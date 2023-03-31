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

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.model;

import org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractNegotiationDocumentSerializationTest {

    private final String partitionKey = "test-connector-partition";
    private TypeManager typeManager;

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(ContractNegotiationDocument.class, ContractNegotiationDocument.class);
    }

    @Test
    void testSerialization() {
        var def = TestFunctions.createNegotiation();
        var pk = def.getState();

        var document = new ContractNegotiationDocument(def, partitionKey);

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("wrappedInstance")
                .contains("\"id\":\"" + def.getId() + "\"")
                .contains("\"partitionKey\":\"" + partitionKey + "\"");
    }

    @Test
    void testDeserialization() {
        var def = TestFunctions.createNegotiation();

        var document = new ContractNegotiationDocument(def, partitionKey);
        String json = typeManager.writeValueAsString(document);

        var transferProcessDeserialized = typeManager.readValue(json, ContractNegotiationDocument.class);
        assertThat(transferProcessDeserialized).usingRecursiveComparison().isEqualTo(document);
    }
}
