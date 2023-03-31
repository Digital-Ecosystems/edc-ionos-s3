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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.sql.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.EdcInjectionException;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class SqlContractNegotiationStoreExtensionTest {

    private SqlContractNegotiationStoreExtension extension;

    @Test
    void initialize(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isInstanceOf(BaseSqlDialectStatements.class);
    }

    @Test
    void initialize_withCustomSqlDialect(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        context.registerService(TransactionContext.class, mock(TransactionContext.class));
        var customSqlDialect = mock(ContractNegotiationStatements.class);
        context.registerService(ContractNegotiationStatements.class, customSqlDialect);

        extension = factory.constructInstance(SqlContractNegotiationStoreExtension.class);

        extension.initialize(context);

        var service = context.getService(ContractNegotiationStore.class);
        assertThat(service).isInstanceOf(SqlContractNegotiationStore.class);
        assertThat(service).extracting("statements").isSameAs(customSqlDialect);
    }

    @Test
    void initialize_missingDataSourceRegistry(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        assertThatThrownBy(() -> factory.constructInstance(SqlContractNegotiationStoreExtension.class))
                .isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void initialize_missingTransactionContext(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TransactionContext.class, mock(TransactionContext.class));

        assertThatThrownBy(() -> factory.constructInstance(SqlContractNegotiationStoreExtension.class))
                .isInstanceOf(EdcInjectionException.class);

    }

}