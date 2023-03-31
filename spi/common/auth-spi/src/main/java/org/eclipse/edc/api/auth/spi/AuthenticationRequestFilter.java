/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.api.auth.spi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.HttpMethod.OPTIONS;

/**
 * Intercepts all requests sent to this resource and authenticates them using an {@link AuthenticationService}. In order
 * to be able to handle CORS requests properly, OPTIONS requests are not validated as their headers usually don't
 * contain credentials.
 */
public class AuthenticationRequestFilter implements ContainerRequestFilter {
    private final AuthenticationService authenticationService;

    public AuthenticationRequestFilter(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var headers = requestContext.getHeaders();

        // OPTIONS requests don't have credentials - do not authenticate
        if (!OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            var isAuthenticated = authenticationService.isAuthenticated(headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (!isAuthenticated) {
                throw new AuthenticationFailedException();
            }
        }
    }
}
