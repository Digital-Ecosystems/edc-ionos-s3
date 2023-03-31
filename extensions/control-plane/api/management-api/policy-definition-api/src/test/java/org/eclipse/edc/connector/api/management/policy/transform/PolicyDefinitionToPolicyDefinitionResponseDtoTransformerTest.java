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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.policy.transform;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyDefinitionToPolicyDefinitionResponseDtoTransformerTest {

    private final PolicyDefinitionToPolicyDefinitionResponseDtoTransformer transformer = new PolicyDefinitionToPolicyDefinitionResponseDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractDefinition = PolicyDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .createdAt(10L)
                .build();

        var dto = transformer.transform(contractDefinition, context);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(contractDefinition.getId());
        assertThat(dto.getPolicy()).isEqualTo(contractDefinition.getPolicy());
        assertThat(dto.getCreatedAt()).isEqualTo(10L);
    }

}
