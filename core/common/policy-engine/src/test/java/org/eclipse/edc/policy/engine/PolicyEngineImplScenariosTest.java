/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.policy.model.Operator.IN;

/**
 * Tests key policy enforcement scenarios. Also serves as a blueprint for custom policy functions.
 */
public class PolicyEngineImplScenariosTest {
    private static final String TEST_SCOPE = "test";
    private static final String ABS_SPATIAL_CONSTRAINT = "absoluteSpatialPosition";
    private static final String CONNECTOR_CONSTRAINT = "connector";
    private static final Action USE_ACTION = Action.Builder.newInstance().type("USE").build();

    private PolicyEngineImpl policyEngine;
    private RuleBindingRegistry bindingRegistry;

    /**
     * Demonstrates how to evaluate a simple policy.
     */
    @Test
    void verifyUnrestrictedUse() {
        bindingRegistry.bind(USE_ACTION.getType(), ALL_SCOPES);

        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();


        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, agent).succeeded()).isTrue();
    }

    /**
     * Demonstrates how to use a rule function.
     */
    @Test
    void verifyNoUse() {
        bindingRegistry.bind(USE_ACTION.getType(), ALL_SCOPES);

        var prohibition = Prohibition.Builder.newInstance().action(USE_ACTION).build();

        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        policyEngine.registerFunction(ALL_SCOPES, Prohibition.class, (rule, context) -> rule.getAction().getType().equals(USE_ACTION.getType()));
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, agent).succeeded()).isFalse();
    }

    /**
     * Demonstrates a spatial constraint and how to evaluate policy against agent claims.
     */
    @Test
    void verifySpatialLocation() {
        bindingRegistry.bind(USE_ACTION.getType(), ALL_SCOPES);
        bindingRegistry.bind(ABS_SPATIAL_CONSTRAINT, ALL_SCOPES);

        // function that verifies the EU region
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, ABS_SPATIAL_CONSTRAINT, (operator, value, permission, context) -> {
            var claims = context.getParticipantAgent().getClaims();
            return claims.containsKey("region") && claims.get("region").equals(value);
        });

        var left = new LiteralExpression(ABS_SPATIAL_CONSTRAINT);
        var right = new LiteralExpression("eu");
        var spatialConstraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(IN).rightExpression(right).build();

        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).constraint(spatialConstraint).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, euAgent).succeeded()).isTrue();

        var noRegionAgent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, noRegionAgent).succeeded()).isFalse();
    }

    /**
     * Shows how to handle literal types that are derived from JSON objects.
     */
    @Test
    void verifyConnectorUse() {
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, CONNECTOR_CONSTRAINT, (operator, value, permission, context) -> {
            if (!(value instanceof List)) {
                context.reportProblem("Unsupported right operand type: " + value.getClass().getName());
                return false;
            }
            //noinspection rawtypes
            return ((List) value).contains("connector1");
        });

        var left = new LiteralExpression(CONNECTOR_CONSTRAINT);
        var right = new LiteralExpression(List.of("connector1"));
        var connectorConstraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(IN).rightExpression(right).build();
        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).constraint(connectorConstraint).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, agent).succeeded()).isTrue();
    }

    @BeforeEach
    void setUp() {
        bindingRegistry = new RuleBindingRegistryImpl();
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry));
    }

}
