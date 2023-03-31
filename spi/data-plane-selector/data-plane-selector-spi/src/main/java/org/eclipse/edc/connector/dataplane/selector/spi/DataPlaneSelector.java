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

package org.eclipse.edc.connector.dataplane.selector.spi;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.RandomSelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link DataPlaneSelector} accepts a certain data request (rather: its source and destination address) and selects a
 * {@link DataPlaneInstance} that can handle that request.
 * It can be thought of as a "software load-balancer" that determines the best-fitting DPF for a particular request.
 */
@FunctionalInterface
public interface DataPlaneSelector {
    /**
     * Computes the best-fit DPF for a given DataRequest. If more than one {@link  DataPlaneInstance} objects fit the requirements,
     * one is selected at random.
     *
     * @return The best-fit {@link DataPlaneInstance}, or null if none was found.
     */
    @Nullable
    default DataPlaneInstance select(DataAddress sourceAddress, DataAddress destinationAddress) {
        return select(sourceAddress, destinationAddress, new RandomSelectionStrategy());
    }

    /**
     * Computes the best-fit DPF for a given DataRequest using the given {@link SelectionStrategy}.
     *
     * @return The best-fit {@link DataPlaneInstance}, or null if none was found.
     */
    @Nullable
    DataPlaneInstance select(DataAddress sourceAddress, DataAddress destinationAddress, SelectionStrategy strategy);
}
