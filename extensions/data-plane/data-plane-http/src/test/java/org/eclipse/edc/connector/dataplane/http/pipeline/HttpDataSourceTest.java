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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpDataSourceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String requestId;
    private String url;

    @BeforeEach
    public void setUp() {
        requestId = UUID.randomUUID().toString();
        url = "http://some.test.url/";
    }

    @Test
    void verifyCallSuccess() throws IOException {
        var json = MAPPER.writeValueAsString(Map.of("key1", "Value1"));
        var responseBody = ResponseBody.create(json, MediaType.parse("application/json"));

        var interceptor = new CustomInterceptor(200, responseBody, "Test message");
        var params = mock(HttpRequestParams.class);
        var request = new Request.Builder().url(url).get().build();
        var source = defaultBuilder(interceptor).params(params).build();

        when(params.toRequest()).thenReturn(request);

        var parts = source.openPartStream().collect(Collectors.toList());

        var interceptedRequest = interceptor.getInterceptedRequest();
        assertThat(interceptedRequest).isEqualTo(request);
        assertThat(parts).hasSize(1);
        var part = parts.get(0);
        try (var is = part.openStream()) {
            assertThat(new String(is.readAllBytes())).isEqualTo(json);
        }

        verify(params).toRequest();
    }

    @Test
    void verifyExceptionIsThrownIfCallFailed() {
        var message = "Test message";
        var body = "Test body";
        var interceptor = new CustomInterceptor(400, ResponseBody.create(body, MediaType.parse("text/plain")), message);
        var params = mock(HttpRequestParams.class);
        var request = new Request.Builder().url(url).get().build();
        var source = defaultBuilder(interceptor).params(params).build();

        when(params.toRequest()).thenReturn(request);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(source::openPartStream)
                .withMessage("Received code transferring HTTP data for request %s: %d - %s. %s", requestId, 400, message, body);

        verify(params).toRequest();
    }

    private HttpDataSource.Builder defaultBuilder(Interceptor interceptor) {
        var httpClient = testHttpClient(interceptor);
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .name("test-name")
                .monitor(mock(Monitor.class))
                .requestId(requestId);
    }

    static final class CustomInterceptor implements Interceptor {
        private final List<Request> requests = new ArrayList<>();
        private final int statusCode;
        private final ResponseBody responseBody;
        private final String message;

        CustomInterceptor(int statusCode, ResponseBody responseBody, String message) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.message = message;
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) {
            requests.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1)
                    .code(statusCode)
                    .body(responseBody)
                    .message(message)
                    .build();
        }

        public Request getInterceptedRequest() {
            return requests.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No request intercepted"));
        }
    }
}
