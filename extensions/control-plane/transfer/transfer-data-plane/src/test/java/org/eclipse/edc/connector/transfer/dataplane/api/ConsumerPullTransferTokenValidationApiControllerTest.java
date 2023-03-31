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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.api;

import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerPullTransferTokenValidationApiControllerTest {

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final DataEncrypter encrypterMock = mock(DataEncrypter.class);
    private final TokenValidationService tokenValidationServiceMock = mock(TokenValidationService.class);

    private ConsumerPullTransferTokenValidationApiController controller;

    @BeforeEach
    void setUp() {
        controller = new ConsumerPullTransferTokenValidationApiController(tokenValidationServiceMock, encrypterMock, TYPE_MANAGER);
    }

    @Test
    void verifyValidateSuccess() {
        var token = UUID.randomUUID().toString();
        var encryptedDataAddress = UUID.randomUUID().toString();
        var decryptedDataAddress = DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
        var claims = ClaimToken.Builder.newInstance()
                .claims(Map.of(
                                "key1", "value1",
                                DATA_ADDRESS, encryptedDataAddress
                        )
                )
                .build();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(encrypterMock.decrypt(encryptedDataAddress)).thenReturn(TYPE_MANAGER.writeValueAsString(decryptedDataAddress));

        var responseAddress = controller.validate(token);
        assertThat(responseAddress.getType()).isEqualTo(decryptedDataAddress.getType());

        verify(tokenValidationServiceMock).validate(anyString());
        verify(encrypterMock).decrypt(anyString());
    }

    @Test
    void verifyTokenValidationFailureThrowsException() {
        var token = UUID.randomUUID().toString();
        var errorMsg = UUID.randomUUID().toString();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.failure(errorMsg));

        assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(() -> controller.validate(token));

        verify(encrypterMock, never()).decrypt(anyString());
    }

    @Test
    void verifyMissingAddressThrowsException() {
        var token = UUID.randomUUID().toString();
        var claims = ClaimToken.Builder.newInstance()
                .claims(Map.of("key1", "value1"))
                .build();

        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> controller.validate(token));

        verify(encrypterMock, never()).decrypt(anyString());
    }
}