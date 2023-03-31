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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleFunction;
import org.eclipse.edc.policy.evaluator.PolicyEvaluator;
import org.eclipse.edc.policy.evaluator.RuleProblem;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

/**
 * Default implementation of the policy engine.
 */
public class PolicyEngineImpl implements PolicyEngine {
    private static final String ALL_SCOPES_DELIMITED = ALL_SCOPES + ".";

    private final ScopeFilter scopeFilter;

    private final Map<String, List<ConstraintFunctionEntry<Rule>>> constraintFunctions = new TreeMap<>();
    private final Map<String, List<RuleFunctionEntry<Rule>>> ruleFunctions = new TreeMap<>();

    private final List<BiFunction<Policy, PolicyContext, Boolean>> preValidators = new ArrayList<>();
    private final List<BiFunction<Policy, PolicyContext, Boolean>> postValidators = new ArrayList<>();

    public PolicyEngineImpl(ScopeFilter scopeFilter) {
        this.scopeFilter = scopeFilter;
    }

    @Override
    public Policy filter(Policy policy, String scope) {
        return scopeFilter.applyScope(policy, scope);
    }

    @Override
    public Result<Policy> evaluate(String scope, Policy policy, ParticipantAgent agent) {
        return evaluate(scope, policy, agent, new HashMap<>());
    }

    @Override
    public Result<Policy> evaluate(String scope, Policy policy, ParticipantAgent agent, Map<Class, Object> contextInformation) {
        var context = new PolicyContextImpl(agent, contextInformation);

        for (BiFunction<Policy, PolicyContext, Boolean> validator : preValidators) {
            if (!validator.apply(policy, context)) {
                return Result.failure(context.hasProblems() ? context.getProblems() : List.of("Pre-validator failed: " + validator.getClass().getName()));
            }
        }

        var evalBuilder = PolicyEvaluator.Builder.newInstance();

        final var delimitedScope = scope + ".";

        ruleFunctions.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(entry -> entry.getValue().stream()).forEach(entry -> {
            if (Duty.class.isAssignableFrom(entry.type)) {
                evalBuilder.dutyRuleFunction((rule) -> entry.function.evaluate(rule, context));
            } else if (Permission.class.isAssignableFrom(entry.type)) {
                evalBuilder.permissionRuleFunction((rule) -> entry.function.evaluate(rule, context));
            } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                evalBuilder.prohibitionRuleFunction((rule) -> entry.function.evaluate(rule, context));
            }
        });

        constraintFunctions.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(entry -> entry.getValue().stream()).forEach(entry -> {
            if (Duty.class.isAssignableFrom(entry.type)) {
                evalBuilder.dutyFunction(entry.key, (operator, value, duty) -> entry.function.evaluate(operator, value, duty, context));
            } else if (Permission.class.isAssignableFrom(entry.type)) {
                evalBuilder.permissionFunction(entry.key, (operator, value, permission) -> entry.function.evaluate(operator, value, permission, context));
            } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                evalBuilder.prohibitionFunction(entry.key, (operator, value, prohibition) -> entry.function.evaluate(operator, value, prohibition, context));
            }
        });

        var evaluator = evalBuilder.build();

        var filteredPolicy = scopeFilter.applyScope(policy, scope);

        var result = evaluator.evaluate(filteredPolicy);

        if (result.valid()) {
            for (BiFunction<Policy, PolicyContext, Boolean> validator : postValidators) {
                if (!validator.apply(policy, context)) {
                    return Result.failure(context.hasProblems() ? context.getProblems() : List.of("Post-validator failed: " + validator.getClass().getName()));
                }
            }

            updateContextInformation(context, contextInformation);
            return Result.success(policy);
        } else {
            updateContextInformation(context, contextInformation);
            return Result.failure(result.getProblems().stream().map(RuleProblem::getDescription).collect(toList()));
        }
    }

    /**
     * Updates the initially supplied context data from the policy context.
     *
     * @param context the policy context.
     * @param contextInformation the initial context data.
     */
    private void updateContextInformation(PolicyContext context, Map<Class, Object> contextInformation) {
        contextInformation.keySet().forEach(key -> contextInformation.put(key, context.getContextData(key)));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Rule> void registerFunction(String scope, Class<R> type, String key, AtomicConstraintFunction<R> function) {
        constraintFunctions.computeIfAbsent(scope + ".", k -> new ArrayList<>()).add(new ConstraintFunctionEntry(type, key, function));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends Rule> void registerFunction(String scope, Class<R> type, RuleFunction<R> function) {
        ruleFunctions.computeIfAbsent(scope + ".", k -> new ArrayList<>()).add(new RuleFunctionEntry(type, function));
    }

    @Override
    public void registerPreValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {
        preValidators.add(validator);
    }

    @Override
    public void registerPostValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {
        postValidators.add(validator);
    }

    private boolean scopeFilter(String entry, String scope) {
        return ALL_SCOPES_DELIMITED.equals(entry) || scope.startsWith(entry);
    }

    private static class ConstraintFunctionEntry<R extends Rule> {
        Class<R> type;
        String key;
        AtomicConstraintFunction<R> function;

        ConstraintFunctionEntry(Class<R> type, String key, AtomicConstraintFunction<R> function) {
            this.type = type;
            this.key = key;
            this.function = function;
        }
    }

    private static class RuleFunctionEntry<R extends Rule> {
        Class<R> type;
        RuleFunction<R> function;

        RuleFunctionEntry(Class<R> type, RuleFunction<R> function) {
            this.type = type;
            this.function = function;
        }
    }

}
