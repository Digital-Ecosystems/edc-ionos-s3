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

package org.eclipse.edc.connector.store.azure.cosmos.policydefinition;

import com.azure.cosmos.implementation.NotFoundException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.store.azure.cosmos.policydefinition.model.PolicyDocument;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;

/**
 * Implementation of the {@link PolicyDefinitionStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) always hit the cache, while write operations affect both the cache
 * AND the database.
 */
public class CosmosPolicyDefinitionStore implements PolicyDefinitionStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final String partitionKey;
    private final Monitor monitor;

    public CosmosPolicyDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String partitionKey, Monitor monitor) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        this.monitor = monitor;
    }

    @Override
    public PolicyDefinition findById(String policyId) {
        var policyDefinition = cosmosDbApi.queryItemById(policyId);
        return policyDefinition != null ? convert(policyDefinition) : null;
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec spec) {
        var statement = new SqlStatement<>(PolicyDocument.class);
        var query = statement.where(spec.getFilterExpression())
                .offset(spec.getOffset())
                .limit(spec.getLimit())
                .orderBy(spec.getSortField(), spec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::convert);
    }

    @Override
    public void save(PolicyDefinition policy) {
        with(retryPolicy).run(() -> cosmosDbApi.saveItem(convertToDocument(policy)));
    }

    @Override
    public @Nullable PolicyDefinition deleteById(String policyId) {
        try {
            var deletedItem = cosmosDbApi.deleteItem(policyId);
            if (deletedItem == null) {
                return null;
            }
            return convert(deletedItem);
        } catch (NotFoundException e) {
            monitor.debug(() -> String.format("PolicyDefinition with id %s not found", policyId));
            return null;
        }
    }

    @Override
    public void reload() {
    }

    @NotNull
    private PolicyDocument convertToDocument(PolicyDefinition policy) {
        return new PolicyDocument(policy, partitionKey);
    }


    private PolicyDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, PolicyDocument.class).getWrappedInstance();
    }
}
