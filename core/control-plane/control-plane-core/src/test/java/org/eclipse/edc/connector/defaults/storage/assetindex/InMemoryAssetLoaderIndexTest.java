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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.defaults.storage.assetindex;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.asset.AssetEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAssetLoaderIndexTest {

    private AssetIndex assetLoader;

    @Test
    void accept() {
        var asset = createAsset("test-asset", UUID.randomUUID().toString());
        var dataAddress = createDataAddress(asset);
        assetLoader.accept(asset, dataAddress);

        Assertions.assertThat(((InMemoryAssetIndex) assetLoader).getAssets()).hasSize(1);
        assertThat(((InMemoryAssetIndex) assetLoader).getDataAddresses()).hasSize(1);
    }

    @Test
    void accept_illegalParams() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        assertThatThrownBy(() -> assetLoader.accept(null, dataAddress)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> assetLoader.accept(createAsset("testasset", "testid"), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void accept_exists() {
        var asset = createAsset("test-asset", UUID.randomUUID().toString());
        var dataAddress = createDataAddress(asset);
        assetLoader.accept(asset, dataAddress);
        DataAddress dataAddress1 = createDataAddress(asset);
        assetLoader.accept(asset, dataAddress1);

        //assert that this replaces the previous data address
        assertThat(((InMemoryAssetIndex) assetLoader).getAssets()).hasSize(1).containsValue(asset);
        assertThat(((InMemoryAssetIndex) assetLoader).getDataAddresses()).hasSize(1).containsValue(dataAddress1);

    }

    @Test
    void acceptAll() {
        var asset1 = createAsset("asset1", "id1");
        var asset2 = createAsset("asset2", "id2");

        var address1 = createDataAddress(asset1);
        var address2 = createDataAddress(asset2);

        List.of(new AssetEntry(asset1, address1), new AssetEntry(asset2, address2))
                .forEach(entry -> assetLoader.accept(entry));

        assertThat(((InMemoryAssetIndex) assetLoader).getAssets()).hasSize(2);
        assertThat(((InMemoryAssetIndex) assetLoader).getDataAddresses()).hasSize(2);

    }

    @Test
    void acceptAll_oneExists_shouldOverwrite() {
        var asset1 = createAsset("asset1", "id1");

        var address1 = createDataAddress(asset1);
        var address2 = createDataAddress(asset1);

        List.of(new AssetEntry(asset1, address1), new AssetEntry(asset1, address2))
                .forEach(entry -> assetLoader.accept(entry));

        // only one address/asset combo should exist
        assertThat(((InMemoryAssetIndex) assetLoader).getAssets()).hasSize(1);
        assertThat(((InMemoryAssetIndex) assetLoader).getDataAddresses()).hasSize(1).containsValue(address2).containsKeys(asset1.getId());

    }

    @BeforeEach
    void setup() {
        assetLoader = new InMemoryAssetIndex();
    }

    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").contentType("type").build();
    }

    private DataAddress createDataAddress(Asset asset) {
        return DataAddress.Builder.newInstance()
                .type(asset.getContentType())
                .keyName("test-keyname")
                .build();
    }
}
