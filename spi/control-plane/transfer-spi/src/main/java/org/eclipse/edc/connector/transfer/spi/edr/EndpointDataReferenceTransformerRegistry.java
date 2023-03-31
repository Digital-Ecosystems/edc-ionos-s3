/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.edr;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for {@link EndpointDataReferenceTransformer}.
 */
@ExtensionPoint
public interface EndpointDataReferenceTransformerRegistry {
    /**
     * Adds a new {@link EndpointDataReferenceTransformer} into the registry.
     */
    void registerTransformer(@NotNull EndpointDataReferenceTransformer transformer);

    /**
     * Browse the registry and apply first applicable {@link EndpointDataReferenceTransformer}.
     */
    @NotNull
    Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr);
}
