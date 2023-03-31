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

package org.eclipse.edc.azure.cosmos;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosStoredProcedure;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CosmosDbApiImpl implements CosmosDbApi {

    private final CosmosItemRequestOptions itemRequestOptions;
    private final CosmosQueryRequestOptions queryRequestOptions;
    private final CosmosContainer container;

    public CosmosDbApiImpl(@NotNull CosmosContainer container, boolean isQueryMetricsEnabled) {
        queryRequestOptions = new CosmosQueryRequestOptions();
        queryRequestOptions.setQueryMetricsEnabled(isQueryMetricsEnabled);
        itemRequestOptions = new CosmosItemRequestOptions();
        this.container = container;
    }

    public CosmosDbApiImpl(@NotNull AbstractCosmosConfig config, CosmosClient client) {
        this(getContainer(config, client), config.isQueryMetricsEnabled());
    }

    private static CosmosContainer getContainer(AbstractCosmosConfig config, CosmosClient client) {
        CosmosDatabase database = getDatabase(client, config.getDbName());
        if (database.readAllContainers().stream().noneMatch(sp -> sp.getId().equals(config.getContainerName()))) {
            throw new EdcException("No CosmosDB container named '" + config.getContainerName() + "' was found in account '" + config.getAccountName() + "'. Please create one, preferably using terraform.");
        }
        return database.getContainer(config.getContainerName());
    }

    private static CosmosDatabase getDatabase(CosmosClient client, String databaseName) {
        var databaseResponse = client.createDatabaseIfNotExists(databaseName);
        return client.getDatabase(databaseResponse.getProperties().getId());
    }

    @Override
    public void saveItem(CosmosDocument<?> item) {
        try {
            // we don't need to supply a partition key, it will be extracted from the CosmosDocument
            CosmosItemResponse<Object> response = container.upsertItem(item, itemRequestOptions);
            handleResponse(response, "Failed to create item");
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void saveItems(Collection<CosmosDocument<?>> definitions) {
        definitions.forEach(this::saveItem);
    }

    @Override
    public Object deleteItem(String id) {

        // we need to query the item first, because delete-by-id requires a partition key, which we might not have available here
        var item = queryItemById(id);
        if (item == null) {
            throw new NotFoundException("An object with the ID " + id + " could not be found!");
        }
        try {
            container.deleteItem(item, itemRequestOptions);
        } catch (CosmosException e) {
            throw new EdcPersistenceException(e);
        }
        return item;
    }

    @Override
    public @Nullable
    Object queryItemById(String id) {
        var query = new SqlQuerySpec("SELECT * FROM c WHERE c.id = @id", new SqlParameter("@id", id));

        try {
            var list = container.queryItems(query, queryRequestOptions, Object.class).stream().collect(Collectors.toList());
            return list.isEmpty() ? null : list.get(0);
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public @Nullable
    Object queryItemById(String id, String partitionKey) {
        CosmosItemResponse<Object> response;
        try {
            response = container.readItem(id, new PartitionKey(partitionKey), itemRequestOptions, Object.class);
        } catch (NotFoundException e) {
            return null;
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
        handleResponse(response, "Failed to query item with id: " + id);
        return response.getItem();
    }

    @Override
    public List<Object> queryAllItems(String partitionKey) {
        try {
            return container.readAllItems(new PartitionKey(partitionKey), queryRequestOptions, Object.class).stream().collect(Collectors.toList());
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public List<Object> queryAllItems() {
        var query = new SqlQuerySpec("SELECT * FROM c");
        try {
            return container.queryItems(query, queryRequestOptions, Object.class).stream().collect(Collectors.toList());
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Stream<Object> queryItems(SqlQuerySpec querySpec) {
        try {
            return container.queryItems(querySpec, queryRequestOptions, Object.class).stream();
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public Stream<Object> queryItems(String query) {
        try {
            return container.queryItems(query, queryRequestOptions, Object.class).stream();
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public String invokeStoredProcedure(String procedureName, String partitionKey, Object... args) {
        var sproc = getStoredProcedure(procedureName);

        List<Object> params = Arrays.asList(args);
        var options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(new PartitionKey(partitionKey));

        CosmosStoredProcedureResponse response = sproc.execute(params, options);

        handleResponse(response.getStatusCode(), "Failed to invoke stored procedure: " + procedureName);
        return response.getResponseAsString();
    }

    @Override
    public void uploadStoredProcedure(String name) {
        // upload stored procedure
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + ".js");
        if (is == null) {
            throw new AssertionError("The input stream referring to the " + name + " file cannot be null!");
        }

        var s = new Scanner(is).useDelimiter("\\A");
        if (!s.hasNext()) {
            throw new IllegalArgumentException("Error loading resource with name " + ".js");
        }
        var body = s.next();
        var props = new CosmosStoredProcedureProperties(name, body);

        var scripts = container.getScripts();
        if (scripts.readAllStoredProcedures().stream().noneMatch(sp -> sp.getId().equals(name))) {
            scripts.createStoredProcedure(props);
        }
    }

    @Override
    public HealthCheckResult get() {

        var response = container.read();
        var success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        return HealthCheckResult.Builder.newInstance()
                .component("CosmosDB " + container.getId())
                .success(success)
                .build();
    }

    private CosmosStoredProcedure getStoredProcedure(String sprocName) {
        return container.getScripts().getStoredProcedure(sprocName);
    }

    private void handleResponse(CosmosItemResponse<?> response, String error) {
        handleResponse(response.getStatusCode(), error);
    }

    private void handleResponse(int code, String error) {
        if (code < 200 || code >= 300) {
            throw new EdcException(error + " (status code: " + code + ")");
        }
    }

}
