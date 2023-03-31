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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContractOfferDescriptionValidationTest {

    private Validator validator;

    private static Policy policy() {
        return Policy.Builder.newInstance().build();
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(value = InvalidPropertiesProvider.class)
    void validate_invalidProperties(String offerId, String assetId, Policy policy) {
        var desc = ContractOfferDescription.Builder.newInstance()
                .offerId(offerId)
                .assetId(assetId)
                .policy(policy)
                .build();

        assertThat(validator.validate(desc)).hasSize(1);
    }

    @Test
    void validate_validProperties() {
        var desc = ContractOfferDescription.Builder.newInstance()
                .offerId("offer")
                .assetId("asset")
                .policy(policy())
                .build();

        assertThat(validator.validate(desc)).isEmpty();
    }

    private static class InvalidPropertiesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("", "asset", policy()),
                    Arguments.of("offer", "", policy()),
                    Arguments.of("offer", "asset", null)
            );
        }
    }
}
