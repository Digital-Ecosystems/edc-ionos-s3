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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuleBindingRegistryImpl implements RuleBindingRegistry {
    private static final String DELIMITER = ".";
    private static final String DELIMITED_ALL = "*" + DELIMITER;

    private final Map<String, Set<String>> ruleBindings = new HashMap<>();

    @Override
    public void bind(String ruleType, String scope) {
        ruleBindings.computeIfAbsent(ruleType, k -> new HashSet<>()).add(scope + DELIMITER);
    }

    @Override
    public boolean isInScope(String ruleType, String scope) {
        var boundScopes = ruleBindings.get(ruleType);
        if (boundScopes == null) {
            return false;
        }
        if (boundScopes.contains(DELIMITED_ALL)) {
            return true;
        }
        var delimitedScope = scope + DELIMITER;
        for (String boundScope : boundScopes) {
            if (delimitedScope.startsWith(boundScope)) {
                return true;
            }
        }
        return false;
    }

}
