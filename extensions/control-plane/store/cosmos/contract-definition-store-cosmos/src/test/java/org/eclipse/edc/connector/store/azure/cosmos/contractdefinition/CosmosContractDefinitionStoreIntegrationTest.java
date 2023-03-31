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
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions.createContractDefinitions;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDefinition;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDocument;
import static org.mockito.Mockito.mock;

@AzureCosmosDbIntegrationTest
class CosmosContractDefinitionStoreIntegrationTest extends ContractDefinitionStoreTestBase {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractDefinitionStore-";
    private static final String TEST_PARTITION_KEY = "test-part-key";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private static TypeManager typeManager;
    private CosmosContractDefinitionStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();
        typeManager = new TypeManager();
        typeManager.registerTypes(ContractDefinition.class, ContractDefinitionDocument.class);

        CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setUp() {
        var containerName = CONTAINER_PREFIX + UUID.randomUUID();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(containerName, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        var cosmosDbApi = new CosmosDbApiImpl(container, true);
        var retryPolicy = RetryPolicy.builder().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS).build();
        store = new CosmosContractDefinitionStore(cosmosDbApi, typeManager, retryPolicy, TEST_PARTITION_KEY, mock(Monitor.class));
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    @Test
    void findAll() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);
        container.createItem(doc2);

        store.reload();
        assertThat(store.findAll(QuerySpec.max())).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
    }

    @Test
    void findAll_noReload() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.max())).hasSize(2);
    }

    @Test
    void findAll_emptyResult() {
        assertThat(store.findAll(QuerySpec.max())).isNotNull().isEmpty();
    }

    @Test
    void findById() {
        var doc = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc);

        var result = store.findById(doc.getId());

        assertThat(result).isNotNull().isEqualTo(doc.getWrappedInstance());
    }

    @Test
    void findById_invalidId() {
        assertThat(store.findById("invalid-id")).isNull();
    }

    @Test
    void save() {
        var definition = generateDefinition();
        store.save(definition);

        var actual = container.readAllItems(new PartitionKey(TEST_PARTITION_KEY), Object.class);

        assertThat(actual).hasSize(1).map(this::convert).first().isEqualTo(definition);
    }

    @Test
    void save_exists_shouldUpdate() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);

        var defToAdd = doc1.getWrappedInstance();

        //modify a single field
        defToAdd.getSelectorExpression().getCriteria().add(new Criterion("anotherkey", "isGreaterThan", "anotherValue"));

        store.save(defToAdd);
        var actual = container.readAllItems(new PartitionKey(doc1.getPartitionKey()), Object.class);
        assertThat(actual).hasSize(1);
        var first = convert(actual.stream().findFirst().get());
        assertThat(first.getSelectorExpression().getCriteria()).hasSize(2).anySatisfy(criterion -> {
            assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherkey");
            assertThat(criterion.getOperator()).isNotEqualTo("isGreaterThan");
            assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherValue");
        }); //we modified that earlier

    }

    @Test
    void saveAll() {
        var def1 = generateDefinition();
        var def2 = generateDefinition();
        var def3 = generateDefinition();

        store.save(List.of(def1, def2, def3));

        var allItems = container.readAllItems(new PartitionKey(TEST_PARTITION_KEY), Object.class);
        assertThat(allItems).hasSize(3);
        var allDefs = allItems.stream().map(this::convert);
        assertThat(allDefs).containsExactlyInAnyOrder(def1, def2, def3);
    }

    @Test
    void save_delete_find_shouldNotExist() {
        var def1 = generateDefinition();
        store.save(def1);
        assertThat(store.findAll(QuerySpec.max())).containsOnly(def1);

        store.deleteById(def1.getId());

        assertThat(store.findAll(QuerySpec.max())).doesNotContain(def1);
    }

    @Test
    void update() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);

        var definition = doc1.getWrappedInstance();
        //modify the object
        definition.getSelectorExpression().getCriteria().add(new Criterion("anotherKey", "NOT EQUAL", "anotherVal"));
        store.update(definition);

        var updatedDefinition = convert(container.readItem(doc1.getId(), new PartitionKey(doc1.getPartitionKey()), Object.class).getItem());
        assertThat(updatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(updatedDefinition.getSelectorExpression().getCriteria()).hasSize(2)
                .anySatisfy(criterion -> {
                    assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherKey");
                    assertThat(criterion.getOperator()).isNotEqualTo("NOT EQUAL");
                    assertThat(criterion.getOperandLeft()).isNotEqualTo("anotherValue");
                }); //we modified that earlier
    }

    @Test
    void update_notExists() {
        var document = generateDocument(TEST_PARTITION_KEY);
        var definition = document.getWrappedInstance();
        //modify the object - should insert
        store.update(definition);

        var updatedDefinition = convert(container.readItem(document.getId(), new PartitionKey(document.getPartitionKey()), Object.class).getItem());
        assertThat(updatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(updatedDefinition.getSelectorExpression().getCriteria()).hasSize(1);
    }

    @Test
    void delete() {
        var document = generateDocument(TEST_PARTITION_KEY);
        container.createItem(document);

        var contractDefinition = convert(document);
        var deletedContractDefinition = store.deleteById(document.getId());
        assertThat(deletedContractDefinition).isEqualTo(contractDefinition);

        assertThat(container.readAllItems(new PartitionKey(document.getPartitionKey()), Object.class)).isEmpty();
    }

    @Test
    void findAll_noQuerySpec() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);

        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(2).extracting(ContractDefinition::getId).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
    }

    @Test
    void findAll_verifyPaging() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(ContractDefinitionDocument::getId).collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4).extracting(ContractDefinition::getId).isSubsetOf(all);

    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(ContractDefinitionDocument::getId).collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(40).build())).hasSize(7).extracting(ContractDefinition::getId).isSubsetOf(all);
    }

    @Test
    void findAll_verifyFiltering() {
        var documents = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).collect(Collectors.toList());

        var expectedId = documents.get(3).getId();

        var query = QuerySpec.Builder.newInstance().filter("id=" + expectedId).build();
        assertThat(store.findAll(query)).extracting(ContractDefinition::getId).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_unsuccessfulFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something = other").build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void findAll_verifySorting() {

        IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).forEach(d -> container.createItem(d));

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.findAll(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractDefinition::getId));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.findAll(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    // Override the base test since Cosmos returns documents where the property subject of ORDER BY does not exist
    @Test
    void findAll_verifySorting_invalidProperty() {

        var ids = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(ContractDefinitionDocument::getId).collect(Collectors.toList());


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = store.findAll(query).collect(Collectors.toList());
        assertThat(all).hasSize(10);
    }

    @Test
    void verify_readWriteFindAll() {
        // add an object
        var def = generateDefinition();
        store.save(def);
        assertThat(store.findAll(QuerySpec.max())).containsExactly(def);

        // modify the object
        var modifiedDef = ContractDefinition.Builder.newInstance().id(def.getId())
                .contractPolicyId("test-cp-id-new")
                .accessPolicyId("test-ap-id-new")
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("somekey", "someval").build())
                .validity(TimeUnit.HOURS.toSeconds(1))
                .build();

        store.update(modifiedDef);

        // re-read
        var all = store.findAll(QuerySpec.Builder.newInstance().filter("contractPolicyId=test-cp-id-new").build()).collect(Collectors.toList());

        assertThat(all).hasSize(1).containsExactly(modifiedDef);

    }

    @Test
    @Disabled("Cosmos does not support yet matching a JSON")
    void find_queryBySelectorExpression_entireCriterion() throws JsonProcessingException {
        var definitionsExpected = createContractDefinitions(20);
        definitionsExpected.get(0).getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "test-asset"));
        var def5 = definitionsExpected.get(5);
        def5.getSelectorExpression().getCriteria().add(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));
        getContractDefinitionStore().save(definitionsExpected);

        var json = new ObjectMapper().writeValueAsString(new Criterion(Asset.PROPERTY_ID, "=", "foobar-asset"));

        var spec = QuerySpec.Builder.newInstance()
                .filter("selectorExpression.criteria = " + json)
                .build();

        assertThat(getContractDefinitionStore().findAll(spec)).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(def5);
    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return store;
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsCollectionIndexQuery() {
        return true;
    }

    @Override
    protected boolean supportsSortOrder() {
        return true;
    }

    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
