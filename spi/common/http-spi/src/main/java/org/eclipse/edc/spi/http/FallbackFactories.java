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

package org.eclipse.edc.spi.http;

import dev.failsafe.Fallback;
import dev.failsafe.event.ExecutionAttemptedEvent;
import dev.failsafe.function.CheckedFunction;
import okhttp3.Response;

import static java.lang.String.format;

/**
 * A set of global defined {@link FallbackFactory} that can be used with {@link EdcHttpClient}
 */
public interface FallbackFactories {

    /**
     * Verifies that the response is successful, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory statusMustBeSuccessful() {
        return request -> {
            CheckedFunction<ExecutionAttemptedEvent<? extends Response>, Exception> exceptionSupplier = event -> {
                var response = event.getLastResult();
                if (response == null) {
                    return new EdcHttpClientException(event.getLastException().getMessage());
                } else {
                    return new EdcHttpClientException(format("Server response to %s was not successful but was %s: %s", request, response.code(), response.body().string()));
                }
            };
            return Fallback.builderOfException(exceptionSupplier)
                    .handleResultIf(r -> !r.isSuccessful())
                    .build();
        };
    }

    /**
     * Verifies that the response has a specific status, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory statusMustBe(int status) {
        return request -> {
            CheckedFunction<ExecutionAttemptedEvent<? extends Response>, Exception> exceptionSupplier = event -> {
                var response = event.getLastResult();
                if (response == null) {
                    return new EdcHttpClientException(event.getLastException().getMessage());
                } else {
                    return new EdcHttpClientException(format("Server response to %s was not %s but was %s: %s", request, status, response.code(), response.body().string()));
                }
            };
            return Fallback.builderOfException(exceptionSupplier)
                    .handleResultIf(r -> r.code() != status)
                    .build();
        };
    }
}
