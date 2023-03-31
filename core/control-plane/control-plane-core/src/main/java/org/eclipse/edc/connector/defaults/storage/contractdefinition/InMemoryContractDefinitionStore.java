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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.connector.defaults.storage.contractdefinition;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.defaults.storage.ReflectionBasedQueryResolver;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The default store implementation used when no extension is configured in a runtime. {@link ContractDefinition}s are
 * stored ephemerally in memory.
 */
public class InMemoryContractDefinitionStore implements ContractDefinitionStore {
    private final Map<String, ContractDefinition> cache = new ConcurrentHashMap<>();
    private final QueryResolver<ContractDefinition> queryResolver = new ReflectionBasedQueryResolver<>(ContractDefinition.class);

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return queryResolver.query(cache.values().stream(), spec);

    }

    @Override
    public ContractDefinition findById(String definitionId) {
        return cache.get(definitionId);
    }


    @Override
    public void save(Collection<ContractDefinition> definitions) {
        definitions.forEach(d -> cache.put(d.getId(), d));
    }

    @Override
    public void save(ContractDefinition definition) {
        cache.put(definition.getId(), definition);
    }

    @Override
    public void update(ContractDefinition definition) {
        save(definition);
    }

    @Override
    public ContractDefinition deleteById(String id) {
        return cache.remove(id);
    }

    @Override
    public void reload() {
        // no-op
    }
}
