/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.base;

import dev.failsafe.RetryPolicy;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.http.FallbackFactory;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static dev.failsafe.okhttp.FailsafeCall.with;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class EdcHttpClientImpl implements EdcHttpClient {
    
    private final OkHttpClient okHttpClient;
    private final RetryPolicy<Response> retryPolicy;
    private final Monitor monitor;

    public EdcHttpClientImpl(OkHttpClient okHttpClient, RetryPolicy<Response> retryPolicy, Monitor monitor) {
        this.okHttpClient = okHttpClient;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    @Override
    public Response execute(Request request) throws IOException {
        var call = okHttpClient.newCall(request);
        return with(retryPolicy).compose(call).execute();
    }

    @Override
    public <T> Result<T> execute(Request request, Function<Response, Result<T>> mappingFunction) {
        return execute(request, emptyList(), mappingFunction);
    }

    @Override
    public <T> Result<T> execute(Request request, List<FallbackFactory> fallbacks, Function<Response, Result<T>> mappingFunction) {
        var call = okHttpClient.newCall(request);
        var builder = with(retryPolicy);
        fallbacks.stream().map(it -> it.create(request)).forEach(builder::compose);

        try (var response = builder.compose(call).execute()) {
            return mappingFunction.apply(response);
        } catch (Throwable e) {
            monitor.severe("HTTP client exception caught for request " + request, e);
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(Request request, Function<Response, T> mappingFunction) {
        var call = okHttpClient.newCall(request);
        return with(retryPolicy).compose(call)
                .executeAsync()
                .thenApply(response -> {
                    try (response) {
                        return mappingFunction.apply(response);
                    }
                });
    }

    @Override
    public EdcHttpClient withDns(String dnsServer) {
        var url = requireNonNull(HttpUrl.get(dnsServer));

        var dns = new DnsOverHttps.Builder()
                .client(okHttpClient)
                .url(url)
                .includeIPv6(false)
                .build();

        return new EdcHttpClientImpl(okHttpClient.newBuilder().dns(dns).build(), retryPolicy, monitor);
    }

}
