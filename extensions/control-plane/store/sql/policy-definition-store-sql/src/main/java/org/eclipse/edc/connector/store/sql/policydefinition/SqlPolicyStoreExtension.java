/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.store.sql.policydefinition;

import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.SqlPolicyStoreStatements;
import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Provides(PolicyDefinitionStore.class)
@Extension("SQL policy store")
public class SqlPolicyStoreExtension implements ServiceExtension {

    @Setting(required = true)
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.policy.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private SqlPolicyStoreStatements statements;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var sqlPolicyStore = new SqlPolicyDefinitionStore(dataSourceRegistry, getDataSourceName(context), transactionContext, context.getTypeManager().getMapper(), getStatementImpl());

        context.registerService(PolicyDefinitionStore.class, sqlPolicyStore);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private SqlPolicyStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_SETTING_NAME);
    }
}
