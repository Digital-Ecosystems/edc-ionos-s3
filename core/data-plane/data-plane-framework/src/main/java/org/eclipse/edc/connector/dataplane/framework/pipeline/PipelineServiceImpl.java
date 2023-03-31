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

package org.eclipse.edc.connector.dataplane.framework.pipeline;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Default pipeline service implementation.
 */
public class PipelineServiceImpl implements PipelineService {
    private final List<DataSourceFactory> sourceFactories = new ArrayList<>();
    private final List<DataSinkFactory> sinkFactories = new ArrayList<>();
    private final Monitor monitor;

    public PipelineServiceImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return getSourceFactory(request) != null &&
                getSinkFactory(request) != null;
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            // NB: do not include the source type as that can possibly leak internal information
            return Result.failure("Data source not supported for: " + request.getId());
        }

        var sourceValidation = sourceFactory.validate(request);
        if (sourceValidation.failed()) {
            return Result.failure(sourceValidation.getFailureMessages());
        }

        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            // NB: do not include the target type as that can possibly leak internal information
            return Result.failure("Data sink not supported for: " + request.getId());
        }

        var sinkValidation = sinkFactory.validate(request);
        if (sinkValidation.failed()) {
            return Result.failure(sinkValidation.getFailureMessages());
        }

        return Result.success(true);
    }

    @WithSpan
    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            return noSourceFactory(request);
        }
        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            return noSinkFactory(request);
        }
        var source = sourceFactory.createSource(request);
        var sink = sinkFactory.createSink(request);
        monitor.debug(() -> format("Transferring from %s to %s.", source, sink));
        return sink.transfer(source);
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSource source, DataFlowRequest request) {
        var sinkFactory = getSinkFactory(request);
        if (sinkFactory == null) {
            return noSinkFactory(request);
        }
        var sink = sinkFactory.createSink(request);
        monitor.debug(() -> format("Transferring from %s to %s.", source, sink));
        return sink.transfer(source);
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataSink sink, DataFlowRequest request) {
        var sourceFactory = getSourceFactory(request);
        if (sourceFactory == null) {
            return noSourceFactory(request);
        }
        var source = sourceFactory.createSource(request);
        monitor.debug(() -> format("Transferring from %s to %s.", source, sink));
        return sink.transfer(source);
    }

    @Override
    public void registerFactory(DataSourceFactory factory) {
        sourceFactories.add(factory);
    }

    @Override
    public void registerFactory(DataSinkFactory factory) {
        sinkFactories.add(factory);
    }

    @Nullable
    private DataSourceFactory getSourceFactory(DataFlowRequest request) {
        return sourceFactories.stream().filter(s -> s.canHandle(request)).findFirst().orElse(null);
    }

    @Nullable
    private DataSinkFactory getSinkFactory(DataFlowRequest request) {
        return sinkFactories.stream().filter(s -> s.canHandle(request)).findFirst().orElse(null);
    }

    @NotNull
    private CompletableFuture<StatusResult<Void>> noSourceFactory(DataFlowRequest request) {
        return completedFuture(StatusResult.failure(FATAL_ERROR, "Unknown data source type: " + request.getSourceDataAddress().getType()));
    }

    @NotNull
    private CompletableFuture<StatusResult<Void>> noSinkFactory(DataFlowRequest request) {
        return completedFuture(StatusResult.failure(FATAL_ERROR, "Unknown data sink type: " + request.getDestinationDataAddress().getType()));
    }


}
