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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.edr;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a transformation operation for a {@link EndpointDataReference}.
 */
public interface EndpointDataReferenceTransformer {
    /**
     * Returns true if current {@link EndpointDataReferenceTransformer} can operate on this {@link EndpointDataReference}.
     */
    boolean canHandle(@NotNull EndpointDataReference edr);

    /**
     * Apply transformation on {@link EndpointDataReference} if it can handle it.
     */
    Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr);
}
