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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.PATH;
import static org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;

/**
 * Source implementation of the {@link HttpRequestParamsSupplier} which proxies (or filter, depending
 * on the data source address configuration) the parameters from the incoming request.
 */
public class HttpSourceRequestParamsSupplier extends HttpRequestParamsSupplier {

    private static final String DEFAULT_METHOD = "GET";

    public HttpSourceRequestParamsSupplier(Vault vault, TypeManager typeManager) {
        super(vault, typeManager);
    }

    @Override
    protected boolean extractNonChunkedTransfer(HttpDataAddress address) {
        return false;
    }

    @Override
    protected @NotNull DataAddress selectAddress(DataFlowRequest request) {
        return request.getSourceDataAddress();
    }

    @Override
    protected @NotNull String extractMethod(HttpDataAddress address, DataFlowRequest request) {
        if (Boolean.parseBoolean(address.getProxyMethod())) {
            return Optional.ofNullable(request.getProperties().get(METHOD))
                    .orElseThrow(() -> new EdcException("Missing http method for request: " + request.getId()));
        }
        return DEFAULT_METHOD;
    }

    @Override
    protected @Nullable String extractPath(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyPath()) ? request.getProperties().get(PATH) : null;
    }

    @Override
    protected @Nullable String extractQueryParams(HttpDataAddress address, DataFlowRequest request) {
        var queryParams = Stream.of(address.getQueryParams(), getRequestQueryParams(address, request))
                .filter(s -> !StringUtils.isNullOrBlank(s))
                .collect(Collectors.joining("&"));
        return !queryParams.isEmpty() ? queryParams : null;
    }

    @Override
    @Nullable
    protected String extractContentType(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyBody()) ? request.getProperties().get(MEDIA_TYPE) : null;
    }

    @Override
    @Nullable
    protected String extractBody(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyBody()) ? request.getProperties().get(BODY) : null;
    }

    @Nullable
    private String getRequestQueryParams(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyQueryParams()) ? request.getProperties().get(QUERY_PARAMS) : null;
    }
}
