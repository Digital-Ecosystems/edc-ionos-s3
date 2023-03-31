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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Sink implementation of the {@link HttpRequestParamsSupplier} which extracts the parameters directly
 * from the data destination address.
 */
public class HttpSinkRequestParamsSupplier extends HttpRequestParamsSupplier {

    private static final String DEFAULT_METHOD = "POST";

    public HttpSinkRequestParamsSupplier(Vault vault, TypeManager typeManager) {
        super(vault, typeManager);
    }

    @Override
    protected boolean extractNonChunkedTransfer(HttpDataAddress address) {
        return address.getNonChunkedTransfer();
    }

    @Override
    @NotNull
    protected DataAddress selectAddress(DataFlowRequest request) {
        return request.getDestinationDataAddress();
    }

    @Override
    @NotNull
    protected String extractMethod(HttpDataAddress address, DataFlowRequest request) {
        return Optional.ofNullable(address.getMethod()).orElse(DEFAULT_METHOD);
    }

    @Override
    protected String extractPath(HttpDataAddress address, DataFlowRequest request) {
        return address.getPath();
    }

    @Override
    @Nullable
    protected String extractQueryParams(HttpDataAddress address, DataFlowRequest request) {
        return null;
    }

    @Override
    protected String extractContentType(HttpDataAddress address, DataFlowRequest request) {
        return address.getContentType();
    }

    @Override
    protected String extractBody(HttpDataAddress address, DataFlowRequest request) {
        return null;
    }
}
