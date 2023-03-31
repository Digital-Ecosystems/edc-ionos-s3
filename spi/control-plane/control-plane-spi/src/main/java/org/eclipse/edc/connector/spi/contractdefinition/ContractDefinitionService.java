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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.spi.contractdefinition;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.stream.Stream;

/**
 * Service that permits actions and queries on ContractDefinition entity.
 */
public interface ContractDefinitionService {

    /**
     * Returns a contract definition by its id
     *
     * @param contractDefinitionId id of the contract definition
     * @return the contract definition, null if it's not found
     */
    ContractDefinition findById(String contractDefinitionId);

    /**
     * Query contract definitions
     *
     * @param query request
     * @return the collection of contract definitions that match the query
     */
    ServiceResult<Stream<ContractDefinition>> query(QuerySpec query);

    /**
     * Create a contract definition with its related data address. If a definition with the same id exists, returns
     * CONFLICT failure.
     *
     * @param contractDefinition the contract definition
     * @return successful result if the contract definition is created correctly, failure otherwise
     */
    ServiceResult<ContractDefinition> create(ContractDefinition contractDefinition);

    /**
     * Delete a contract definition. If the definition is already referenced by a contract agreement, returns CONFLICT
     * failure. If the definition does not exist, returns NOT_FOUND failure.
     *
     * @param contractDefinitionId the id of the contract definition to be deleted
     * @return successful result if the contract definition is deleted correctly, failure otherwise
     */
    ServiceResult<ContractDefinition> delete(String contractDefinitionId);
}
