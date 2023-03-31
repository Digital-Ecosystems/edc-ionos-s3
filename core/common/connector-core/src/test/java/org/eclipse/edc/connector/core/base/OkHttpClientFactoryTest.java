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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.core.base;

import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OkHttpClientFactoryTest {

    private static final String HTTP_URL = "http://localhost:11111";
    private static final String HTTPS_URL = "https://localhost:11111";
    private final Monitor monitor = mock(Monitor.class);
    private final EventListener eventListener = mock(EventListener.class);

    @Test
    void shouldPrintLogIfHttpsNotEnforced() {
        var context = createContextWithConfig(emptyMap());

        var okHttpClient = OkHttpClientFactory.create(context, eventListener)
                .newBuilder().addInterceptor(dummySuccessfulResponse())
                .build();

        assertThatCode(() -> call(okHttpClient, HTTP_URL)).doesNotThrowAnyException();
        assertThatCode(() -> call(okHttpClient, HTTPS_URL)).doesNotThrowAnyException();
        verify(monitor).info(argThat(messageContains("HTTPS enforcement")));
    }

    @Test
    void shouldEnforceHttpsCalls() {
        var config = Map.of("edc.http.enforce-https", "true");
        var context = createContextWithConfig(config);

        var okHttpClient = OkHttpClientFactory.create(context, eventListener)
                .newBuilder().addInterceptor(dummySuccessfulResponse())
                .build();

        assertThatThrownBy(() -> call(okHttpClient, HTTP_URL)).isInstanceOf(EdcException.class);
        assertThatCode(() -> call(okHttpClient, HTTPS_URL)).doesNotThrowAnyException();
        verify(monitor, never()).info(argThat(messageContains("HTTPS enforcement")));
    }

    @NotNull
    private Interceptor dummySuccessfulResponse() {
        return it -> new Response.Builder()
                .code(200)
                .message("any")
                .body(ResponseBody.create("", MediaType.get("text/html")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build())
                .build();
    }

    private void call(OkHttpClient okHttpClient, String url) throws IOException {
        okHttpClient.newCall(new Request.Builder().url(url).build()).execute().close();
    }

    @NotNull
    private DefaultServiceExtensionContext createContextWithConfig(Map<String, String> config) {
        var context = new DefaultServiceExtensionContext(mock(TypeManager.class), monitor, mock(Telemetry.class), List.of(() -> ConfigFactory.fromMap(config)));
        context.initialize();
        return context;
    }

    @NotNull
    private ArgumentMatcher<String> messageContains(String string) {
        return message -> message.contains(string);
    }

}