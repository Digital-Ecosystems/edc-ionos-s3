/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.manager;

import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/**
 * Default data manager implementation.
 * <p>
 * This implementation uses a simple bounded queue to support backpressure when the system is overloaded. This should support sufficient performance since data transfers
 * generally do not require low-latency. If low-latency operation becomes a requirement, a concurrent queuing mechanism can be used.
 */
public class DataPlaneManagerImpl implements DataPlaneManager {
    private final AtomicBoolean active = new AtomicBoolean();
    private int queueCapacity = 10000;
    private int workers = 1;
    private long waitTimeout = 100;
    private PipelineService pipelineService;
    private ExecutorInstrumentation executorInstrumentation;
    private Monitor monitor;
    private Telemetry telemetry;
    private BlockingQueue<DataFlowRequest> queue;
    private ExecutorService executorService;
    private DataPlaneStore store;
    private TransferServiceRegistry transferServiceRegistry;

    private TransferProcessApiClient transferProcessClient;

    private DataPlaneManagerImpl() {

    }

    public void start() {
        queue = new ArrayBlockingQueue<>(queueCapacity);
        active.set(true);
        executorService = executorInstrumentation.instrument(Executors.newFixedThreadPool(workers), getClass().getSimpleName());
        for (var i = 0; i < workers; i++) {
            executorService.submit(this::run);
        }
    }

    public void stop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public void forceStop() {
        active.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest dataRequest) {
        var transferService = transferServiceRegistry.resolveTransferService(dataRequest);
        return transferService != null ?
                transferService.validate(dataRequest) :
                Result.failure(format("Cannot find a transfer Service that can handle %s source and %s destination", dataRequest.getSourceDataAddress().getType(), dataRequest.getDestinationDataAddress().getType()));
    }

    @Override
    public void initiateTransfer(DataFlowRequest dataRequest) {
        // store current trace context in entity for request traceability
        DataFlowRequest dataRequestWithTraceContext = dataRequest.toBuilder()
                .traceContext(telemetry.getCurrentTraceContext())
                .build();
        queue.add(dataRequestWithTraceContext);
        store.received(dataRequestWithTraceContext.getProcessId());
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSource source, DataFlowRequest request) {
        return pipelineService.transfer(source, request);
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        return pipelineService.transfer(sink, request);
    }

    @Override
    public State transferState(String processId) {
        return store.getState(processId);
    }

    private void run() {
        while (active.get()) {
            DataFlowRequest request = null;
            try {
                request = queue.poll(waitTimeout, TimeUnit.MILLISECONDS);
                if (request == null) {
                    continue;
                }
                // propagate trace context for request into the current thread
                telemetry.contextPropagationMiddleware(this::processDataFlowRequest).accept(request);

            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Exception e) {
                if (request == null) {
                    monitor.severe("Unable to dequeue data request", e);
                } else {
                    monitor.severe("Error processing data request: " + request.getProcessId(), e);
                    // TODO persist error details
                    store.completed(request.getProcessId());
                }
            }
        }
    }

    private void processDataFlowRequest(DataFlowRequest request) {
        var transferService = transferServiceRegistry.resolveTransferService(request);
        if (transferService == null) {
            // Should not happen since resolving a transferService is part of payload validation
            // TODO persist error details
            store.completed(request.getProcessId());
        } else {
            transferService.transfer(request).whenComplete((result, exception) -> {

                if (request.isTrackable()) {
                    // TODO persist TransferResult or error details
                    store.completed(request.getProcessId());
                }

                onTransferFinished(request, result, exception);


            });
        }
    }

    private void onTransferFinished(DataFlowRequest request, StatusResult<Void> result, Throwable exception) {
        if (exception != null) {
            transferProcessClient.failed(request, exception.getMessage());
        } else if (result.succeeded()) {
            transferProcessClient.completed(request);
        } else {
            transferProcessClient.failed(request, result.getFailureDetail());
        }
    }

    public static class Builder {
        private final DataPlaneManagerImpl manager;

        private Builder() {
            manager = new DataPlaneManagerImpl();
            manager.telemetry = new Telemetry(); // default noop implementation
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder pipelineService(PipelineService pipelineService) {
            manager.pipelineService = pipelineService;
            return this;
        }

        public Builder executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder transferServiceRegistry(TransferServiceRegistry transferServiceRegistry) {
            manager.transferServiceRegistry = transferServiceRegistry;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
            return this;
        }

        public Builder queueCapacity(int capacity) {
            manager.queueCapacity = capacity;
            return this;
        }

        public Builder workers(int workers) {
            manager.workers = workers;
            return this;
        }

        public Builder waitTimeout(long waitTimeout) {
            manager.waitTimeout = waitTimeout;
            return this;
        }

        public Builder store(DataPlaneStore store) {
            manager.store = store;
            return this;
        }

        public Builder transferProcessClient(TransferProcessApiClient transferProcessClient) {
            manager.transferProcessClient = transferProcessClient;
            return this;
        }

        public DataPlaneManagerImpl build() {
            Objects.requireNonNull(manager.transferProcessClient);
            return manager;
        }
    }

}
