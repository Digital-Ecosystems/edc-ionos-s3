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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.defaults.storage.assetindex;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryDataAddressResolverTest {
    private InMemoryAssetIndex resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryAssetIndex();
    }

    @Test
    void resolveForAsset() {
        var id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        var address = createDataAddress(testAsset);
        resolver.accept(testAsset, address);

        assertThat(resolver.resolveForAsset(testAsset.getId())).isEqualTo(address);
    }

    @Test
    void resolveForAsset_assetNull_raisesException() {
        var id = UUID.randomUUID().toString();
        var testAsset = createAsset("foobar", id);
        var address = createDataAddress(testAsset);
        resolver.accept(testAsset, address);

        assertThatThrownBy(() -> resolver.resolveForAsset(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveForAsset_whenAssetDeleted_raisesException() {
        var testAsset = createAsset("foobar", UUID.randomUUID().toString());
        var address = createDataAddress(testAsset);
        resolver.accept(testAsset, address);
        resolver.deleteById(testAsset.getId());

        assertThat(resolver.resolveForAsset(testAsset.getId())).isNull();
    }

    private Asset createAsset(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").contentType("type").build();
    }

    private DataAddress createDataAddress(Asset asset) {
        return DataAddress.Builder.newInstance()
                .keyName("test-keyname")
                .type(asset.getContentType())
                .build();
    }
}
