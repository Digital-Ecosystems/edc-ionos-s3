/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.offer;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionService.CATALOGING_SCOPE;
import static org.eclipse.edc.spi.asset.AssetSelectorExpression.SELECT_ALL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {

    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore definitionStore = mock(ContractDefinitionStore.class);

    private ContractDefinitionServiceImpl definitionService;

    @BeforeEach
    void setUp() {
        definitionService = new ContractDefinitionServiceImpl(mock(Monitor.class), definitionStore, policyEngine, policyStore);
    }

    @Test
    void definitionsFor_verifySatisfiesPolicies() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var def = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(policyStore.findById(any())).thenReturn(def);
        when(policyEngine.evaluate(CATALOGING_SCOPE, def.getPolicy(), agent)).thenReturn(Result.success(def.getPolicy()));
        when(definitionStore.findAll(any())).thenReturn(Stream.of(createContractDefinition()));

        var definitions = definitionService.definitionsFor(agent);

        assertThat(definitions).hasSize(1);
        verify(policyEngine, atLeastOnce()).evaluate(CATALOGING_SCOPE, def.getPolicy(), agent);
        verify(definitionStore).findAll(any());
    }

    @Test
    void definitionsFor_verifyDoesNotSatisfyAccessPolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var definition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id("access").build();
        when(policyStore.findById(any())).thenReturn(definition);
        var contractDefinition = createContractDefinition();
        when(policyEngine.evaluate(any(), any(), any())).thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var result = definitionService.definitionsFor(agent);

        assertThat(result).isEmpty();
        verify(policyEngine, atLeastOnce()).evaluate(CATALOGING_SCOPE, definition.getPolicy(), agent);
        verify(definitionStore).findAll(any());
    }

    @Test
    void definitionsFor_verifyPoliciesNotFound() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();
        when(policyStore.findById(any())).thenReturn(null);
        when(policyEngine.evaluate(CATALOGING_SCOPE, policy, agent)).thenReturn(Result.success(policy));
        when(definitionStore.findAll(QuerySpec.max())).thenReturn(Stream.of(createContractDefinition()));

        var definitions = definitionService.definitionsFor(agent);

        assertThat(definitions).isEmpty();
        verify(policyEngine, never()).evaluate(any(), any(), any());
    }

    @Test
    void definitionFor_found() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var definition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        var contractDefinition = createContractDefinition();
        when(policyStore.findById(any())).thenReturn(definition);
        when(policyEngine.evaluate(eq(CATALOGING_SCOPE), isA(Policy.class), isA(ParticipantAgent.class))).thenReturn(Result.success(definition.getPolicy()));
        when(definitionStore.findById("1")).thenReturn(contractDefinition);

        var result = definitionService.definitionFor(agent, "1");

        assertThat(result).isNotNull();
        verify(policyEngine, atLeastOnce()).evaluate(CATALOGING_SCOPE, definition.getPolicy(), agent);
    }

    @Test
    void definitionFor_notFound() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        when(definitionStore.findById(any())).thenReturn(null);

        var result = definitionService.definitionFor(agent, "nodefinition");

        assertThat(result).isNull();
        verify(policyEngine, never()).evaluate(any(), any(), any());
    }

    private static ContractDefinition createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract")
                .selectorExpression(SELECT_ALL)
                .validity(100)
                .build();
    }
}
