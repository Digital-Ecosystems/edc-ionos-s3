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

import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;

/**
 * Converts a {@link Criterion}, which is essentially a select statement, into a {@code Predicate<Asset>}.
 * <p>
 * This is useful when dealing with in-memory collections of objects, here: {@link Asset} where Predicates can be applied
 * efficiently.
 * <p>
 * _Note: other {@link AssetIndex} implementations might have different converters!
 */
class AssetPredicateConverter extends BaseCriterionToPredicateConverter<Asset> {
    @Override
    public <T> T property(String key, Object object) {
        if (object instanceof Asset) {
            var asset = (Asset) object;
            if (asset.getProperties() == null || asset.getProperties().isEmpty()) {
                return null;
            }
            return (T) asset.getProperty(key);
        }
        throw new IllegalArgumentException("Can only handle objects of type " + Asset.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
