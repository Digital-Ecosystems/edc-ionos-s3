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

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetPredicateConverterTest {

    private AssetPredicateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AssetPredicateConverter();
    }

    @Test
    void convert_nameEquals() {
        var criterion = new Criterion(Asset.PROPERTY_NAME, "=", "test-asset");
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .build();
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(asset)).isTrue();
    }

    @Test
    void convert_versionEquals() {
        var criterion = new Criterion(Asset.PROPERTY_VERSION, "=", "6.9");
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .version("6.9")
                .build();
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(asset)).isTrue();
    }

    @Test
    void convert_anotherPropertyEquals() {
        var criterion = new Criterion("test-property", "=", "somevalue");
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .version("6.9")
                .property("test-property", "somevalue")
                .build();
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(asset)).isTrue();
    }

    @Test
    void convert_operatorIn() {
        var asset = Asset.Builder.newInstance()
                .name("bob")
                .version("6.9")
                .property("test-property", "somevalue")
                .build();
        var criterion = new Criterion(Asset.PROPERTY_NAME, "in", List.of("bob", "alice"));
        var pred = converter.convert(criterion);
        assertThat(pred).isNotNull().accepts(asset);

    }

    @Test
    void convert_invalidOperator() {
        var criterion = new Criterion("name", "GREATER_THAN", "(bob, alice)");
        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator [GREATER_THAN] is not supported by this converter!");

    }

}