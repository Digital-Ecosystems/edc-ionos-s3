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
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.connector.contract.spi.offer.store;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Persists {@link ContractDefinition}s.
 */
@ExtensionPoint
public interface ContractDefinitionStore {

    /**
     * Returns all the definitions in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link ContractDefinition} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage
     * implementation.
     */
    @NotNull
    Stream<ContractDefinition> findAll(QuerySpec spec);

    /**
     * Returns the definition with the given id, if it exists.
     *
     * @param definitionId the id.
     * @return the definition with with the given id, or null.
     */
    ContractDefinition findById(String definitionId);

    /**
     * Persists the definitions.
     */
    void save(Collection<ContractDefinition> definitions);

    /**
     * Persists the definition.
     */
    void save(ContractDefinition definition);

    /**
     * Updates the definitions.
     */
    void update(ContractDefinition definition);

    /**
     * Deletes the definition with the given id.
     *
     * @param id A String that represents the {@link ContractDefinition} ID, in most cases this will be a UUID.
     * @return The {@link ContractDefinition} if one was found, or null otherwise.
     * @throws EdcPersistenceException if something goes wrong.
     */
    ContractDefinition deleteById(String id);

    /**
     * Signals the store should reload its internal cache if updates were made. If the implementation does not implement
     * caching, this method will do nothing.
     */
    default void reload() {
    }

    default void accept(ContractDefinition item) {
        save(item);
    }
}
