/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.spi.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Define a store that can be used within a state machine
 */
public interface StateEntityStore<T> {

    /**
     * Returns a list of entities that are in a specific state.
     * <p>
     * Implementors MUST handle these requirements: <p>
     * <ul>
     *     <li>
     *         * entities should be fetched from the oldest to the newest, by a timestamp that reports the last state transition on the entity
     *         <p><p>
     *     </li>
     *     <li>
     *         * fetched entities should be leased for a configurable timeout, that will be released after the timeout expires or when the entity will be updated.
     *         This will avoid consecutive fetches in the state machine loop
     *         <p><p>
     *     </li>
     * </ul>
     *
     * @param state The state that the processes of interest should be in.
     * @param max   The maximum amount of result items.
     * @return A list of entities (at most _max_) that are in the desired state.
     */
    @NotNull
    List<T> nextForState(int state, int max);

}
