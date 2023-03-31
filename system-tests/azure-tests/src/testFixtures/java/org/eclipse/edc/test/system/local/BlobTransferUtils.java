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

package org.eclipse.edc.test.system.local;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_CONNECTOR_MANAGEMENT_URL;
import static org.eclipse.edc.test.system.local.TransferLocalSimulation.PROVIDER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;
import static org.eclipse.edc.test.system.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;

public class BlobTransferUtils {

    private static final String ASSETS_PATH = "/assets";
    private static final String POLICIES_PATH = "/policydefinitions";
    private static final String CONTRACT_DEFINITIONS_PATH = "/contractdefinitions";

    private BlobTransferUtils() {
    }

    public static void createAsset(String accountName, String containerName) {
        var asset = Map.of(
                "asset", Map.of(
                        "id", PROVIDER_ASSET_ID,
                        "properties", Map.of(
                                "asset:prop:name", PROVIDER_ASSET_ID,
                                "asset:prop:contenttype", "text/plain",
                                "asset:prop:version", "1.0",
                                "asset:prop:id", PROVIDER_ASSET_ID,
                                "type", "AzureStorage"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "type", AzureBlobStoreSchema.TYPE,
                                AzureBlobStoreSchema.ACCOUNT_NAME, accountName,
                                AzureBlobStoreSchema.CONTAINER_NAME, containerName,
                                AzureBlobStoreSchema.BLOB_NAME, PROVIDER_ASSET_FILE,
                                "keyName", format("%s-key1", accountName)
                        )
                )
        );

        seedProviderData(ASSETS_PATH, asset);
    }

    @NotNull
    public static String createPolicy() {
        var policy = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .target(PROVIDER_ASSET_ID)
                                .action(Action.Builder.newInstance().type("USE").build())
                                .build())
                        .type(PolicyType.SET)
                        .build())
                .build();

        seedProviderData(POLICIES_PATH, policy);

        return policy.getUid();
    }

    public static void createContractDefinition(String policyId) {

        var criteria = AssetSelectorExpression.Builder.newInstance()
                .constraint("asset:prop:id",
                        "=",
                        PROVIDER_ASSET_ID)
                .build();

        var contractDefinition = Map.of(
                "id", "1",
                "accessPolicyId", policyId,
                "contractPolicyId", policyId,
                "criteria", criteria.getCriteria(),
                "validity", TimeUnit.HOURS.toSeconds(1)
        );

        seedProviderData(CONTRACT_DEFINITIONS_PATH, contractDefinition);
    }

    private static void seedProviderData(String path, Object requestBody) {
        givenProviderBaseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    private static RequestSpecification givenProviderBaseRequest() {
        return given()
                .baseUri(PROVIDER_CONNECTOR_MANAGEMENT_URL + PROVIDER_MANAGEMENT_PATH);
    }
}
