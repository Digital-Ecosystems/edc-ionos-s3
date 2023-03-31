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

package org.eclipse.edc.connector.dataplane.spi.manager;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore.State;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the execution of data plane requests.
 */
@ExtensionPoint
public interface DataPlaneManager {

    /**
     * Determines if the data flow request is valid and can be processed by this runtime.
     */
    Result<Boolean> validate(DataFlowRequest dataRequest);

    /**
     * Initiates a transfer for the data flow request. This method is non-blocking with respect to processing the request.
     */
    void initiateTransfer(DataFlowRequest dataRequest);

    /**
     * Performs a data transfer using the supplied data source.
     */
    CompletableFuture<StatusResult<Void>> transfer(DataSource source, DataFlowRequest request);

    /**
     * Performs a data transfer using the supplied data sink.
     */
    CompletableFuture<StatusResult<Void>> transfer(DataSink sink, DataFlowRequest request);

    /**
     * Provides transfer state.
     */
    State transferState(String processId);
}
