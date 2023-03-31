/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex.model;

import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetDocumentSerializationTest {

    private TypeManager typeManager;

    private static Asset createAsset() {
        return Asset.Builder.newInstance()
                .id("id-test")
                .name("node-test")
                .contentType("application/json")
                .version("123")
                .property("foo", "bar")
                .build();
    }

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
    }

    @Test
    void testSerialization() {
        var asset = createAsset();

        var document = new AssetDocument(asset, "partitionkey-test", DataAddress.Builder.newInstance().type("type").build());

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("\"partitionKey\":\"partitionkey-test\"")
                .contains("\"asset_prop_id\":\"id-test\"")
                .contains("\"asset_prop_name\":\"node-test\"")
                .contains("\"asset_prop_version\":\"123\"")
                .contains("\"asset_prop_contenttype\":\"application/json\"")
                .contains("\"foo\":\"bar\"")
                .contains("\"wrappedInstance\":")
                .contains("\"id\":\"id-test\"");
    }

    @Test
    void testDeserialization() {
        var asset = createAsset();

        var document = new AssetDocument(asset, "partitionkey-test", DataAddress.Builder.newInstance()
                .type("testtype").build());
        String json = typeManager.writeValueAsString(document);

        var deserialized = typeManager.readValue(json, AssetDocument.class);
        assertThat(deserialized.getWrappedInstance()).usingRecursiveComparison().isEqualTo(document.getWrappedInstance());
    }
}
