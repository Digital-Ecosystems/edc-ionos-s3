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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.web.jersey.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.EdcApiException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static java.util.stream.Collectors.toList;

/**
 * Exception mapper that catches all the `EdcApiException` exceptions, map them to a 4xx response code with a detailed response body
 */
public class EdcApiExceptionMapper implements ExceptionMapper<EdcApiException> {
    private final Map<Class<? extends EdcApiException>, Response.Status> exceptionMap;

    public EdcApiExceptionMapper() {
        exceptionMap = Map.of(
                AuthenticationFailedException.class, UNAUTHORIZED,
                NotAuthorizedException.class, FORBIDDEN,
                InvalidRequestException.class, BAD_REQUEST,
                ObjectNotFoundException.class, NOT_FOUND,
                ObjectConflictException.class, CONFLICT,
                BadGatewayException.class, BAD_GATEWAY
        );
    }

    @Override
    public Response toResponse(EdcApiException exception) {
        var status = exceptionMap.getOrDefault(exception.getClass(), INTERNAL_SERVER_ERROR);

        var errorDetails = exception.getMessages().stream()
                .map(message -> ApiErrorDetail.Builder.newInstance()
                        .message(message)
                        .type(exception.getType())
                        .build()
                )
                .collect(toList());

        return Response.status(status)
                .entity(errorDetails)
                .build();
    }
}
