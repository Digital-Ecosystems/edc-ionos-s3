/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.Objects;

/**
 * Implementation of a {@link DataPlaneClient} that uses a local {@link DataPlaneManager},
 * i.e. one that runs in the same JVM as the control plane.
 */
public class EmbeddedDataPlaneClient implements DataPlaneClient {

    private final DataPlaneManager dataPlaneManager;

    public EmbeddedDataPlaneClient(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = Objects.requireNonNull(dataPlaneManager, "Data plane manager");
    }

    @WithSpan
    @Override
    public StatusResult<Void> transfer(DataFlowRequest request) {
        var result = dataPlaneManager.validate(request);
        if (result.failed()) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, String.join(", ", result.getFailureMessages()));
        }
        dataPlaneManager.initiateTransfer(request);
        return StatusResult.success();
    }
}
