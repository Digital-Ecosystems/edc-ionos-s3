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
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.edc.test.system.local;

import com.azure.core.util.BinaryData;
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.junit.testfixtures.MockVault;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.vault.NoopCertificateResolver;
import org.eclipse.edc.spi.system.vault.NoopPrivateKeyResolver;
import org.eclipse.edc.test.system.utils.TransferSimulationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.test.system.local.BlobTransferLocalSimulation.ACCOUNT_ENDPOINT_PROPERTY;
import static org.eclipse.edc.test.system.local.BlobTransferLocalSimulation.ACCOUNT_KEY_PROPERTY;
import static org.eclipse.edc.test.system.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createAsset;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createContractDefinition;
import static org.eclipse.edc.test.system.local.BlobTransferUtils.createPolicy;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_IDS_API;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_IDS_API_PORT;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_PORT;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PATH;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_CONNECTOR_PORT;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_IDS_API;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_IDS_API_PORT;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PORT;
import static org.eclipse.edc.test.system.utils.GatlingUtils.runGatling;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.IDS_PATH;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;

@AzureStorageIntegrationTest
public class BlobTransferIntegrationTest extends AbstractAzureBlobTest {
    private static final Vault CONSUMER_VAULT = new MockVault();
    private static final Vault PROVIDER_VAULT = new MockVault();
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.of(
                    "edc.blobstore.endpoint.template", "http://127.0.0.1:10000/%s",
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "web.http.path", CONSUMER_CONNECTOR_PATH,
                    "web.http.management.port", String.valueOf(CONSUMER_MANAGEMENT_PORT),
                    "web.http.management.path", CONSUMER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(CONSUMER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "ids.webhook.address", CONSUMER_IDS_API));

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-provider",
            "provider",
            Map.of(
                    "edc.blobstore.endpoint.template", "http://127.0.0.1:10000/%s",
                    "edc.test.asset.container.name", PROVIDER_CONTAINER_NAME,
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "web.http.path", PROVIDER_CONNECTOR_PATH,
                    "web.http.management.port", String.valueOf(PROVIDER_MANAGEMENT_PORT),
                    "web.http.management.path", PROVIDER_MANAGEMENT_PATH,
                    "web.http.ids.port", String.valueOf(PROVIDER_IDS_API_PORT),
                    "web.http.ids.path", IDS_PATH,
                    "ids.webhook.address", PROVIDER_IDS_API));


    @BeforeAll
    static void beforeAll() {
        setUpMockVault(consumer, CONSUMER_VAULT);
        setUpMockVault(provider, PROVIDER_VAULT);
    }

    private static void setUpMockVault(EdcRuntimeExtension consumer, Vault vault) {
        consumer.registerServiceMock(Vault.class, vault);
        consumer.registerServiceMock(PrivateKeyResolver.class, new NoopPrivateKeyResolver());
        consumer.registerServiceMock(CertificateResolver.class, new NoopCertificateResolver());
    }

    @Test
    void transferBlob_success() {
        // Arrange
        // Upload a blob with test data on provider blob container (in account1).
        var blobContent = BlobTransferSimulationConfiguration.BLOB_CONTENT;
        createContainer(blobServiceClient1, PROVIDER_CONTAINER_NAME);
        blobServiceClient1.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
                .getBlobClient(PROVIDER_ASSET_FILE)
                .upload(BinaryData.fromString(blobContent));

        // Seed data to provider
        createAsset(account1Name, PROVIDER_CONTAINER_NAME);
        var policyId = createPolicy();
        createContractDefinition(policyId);

        // Write Key to vault
        CONSUMER_VAULT.storeSecret(format("%s-key1", account2Name), account2Key);
        PROVIDER_VAULT.storeSecret(format("%s-key1", account1Name), account1Key);

        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, account2Name);
        System.setProperty(ACCOUNT_KEY_PROPERTY, account2Key);
        System.setProperty(ACCOUNT_ENDPOINT_PROPERTY, TestFunctions.getBlobServiceTestEndpoint(account2Name));
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);
    }

}
