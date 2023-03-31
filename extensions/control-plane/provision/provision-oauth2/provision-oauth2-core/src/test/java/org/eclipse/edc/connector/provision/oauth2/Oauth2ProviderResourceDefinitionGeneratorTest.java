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
 *       ZF Friedrichshafen AG - unit tests for canGenerate
 *
 */

package org.eclipse.edc.connector.provision.oauth2;

import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class Oauth2ProviderResourceDefinitionGeneratorTest extends AbstractOauth2DataAddressValidationTest {

    private final ProviderResourceDefinitionGenerator generator = new Oauth2ProviderResourceDefinitionGenerator();

    @Test
    void returnDefinitionIfTypeIsHttpDataAndOauth2ParametersArePresent() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                .build();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .destinationType("any")
                .build();

        var definition = generator.generate(dataRequest, dataAddress, simplePolicy());

        assertThat(definition).isNotNull().asInstanceOf(type(Oauth2ResourceDefinition.class))
                .satisfies(d -> {
                    assertThat(d.getClientId()).isEqualTo("aClientId");
                    assertThat(d.getClientSecret()).isEqualTo("aSecret");
                    assertThat(d.getTokenUrl()).isEqualTo("aTokenUrl");
                });
    }

    @Test
    void generate_noDataRequestAsParameter() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                .build();
        assertThatNullPointerException().isThrownBy(() -> generator.generate(null, dataAddress, simplePolicy()));
    }

    @Test
    void generate_noDataAddressAsParameter() {
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .destinationType("any")
                .build();

        assertThatNullPointerException().isThrownBy(() -> generator.generate(dataRequest, null, simplePolicy()));
    }

    @Test
    void generate_noPolicyAsParameter() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .property(Oauth2DataAddressSchema.CLIENT_ID, "aClientId")
                .property(Oauth2DataAddressSchema.CLIENT_SECRET, "aSecret")
                .property(Oauth2DataAddressSchema.TOKEN_URL, "aTokenUrl")
                .build();
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .destinationType("any")
                .build();

        assertThatNullPointerException().isThrownBy(() -> generator.generate(dataRequest, dataAddress, null));
    }

    @Override
    protected boolean test(DataAddress address) {
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .destinationType("any")
                .build();
        return generator.canGenerate(dataRequest, address, simplePolicy());
    }

    private Policy simplePolicy() {
        return Policy.Builder.newInstance().build();
    }
}
