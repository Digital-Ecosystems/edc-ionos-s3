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

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;

import java.util.UUID;

public class TestFunctions {

    public static final String ACCESS_POLICY_ID = "test-ap-id1";
    public static final String CONTRACT_POLICY_ID = "test-cp-id1";

    public static ContractDefinition generateDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicyId(CONTRACT_POLICY_ID)
                .accessPolicyId(ACCESS_POLICY_ID)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("somekey", "someval").build())
                .validity(100)
                .build();
    }

    public static ContractDefinitionDocument generateDocument(String partitionKey) {
        return new ContractDefinitionDocument(generateDefinition(), partitionKey);
    }
}
