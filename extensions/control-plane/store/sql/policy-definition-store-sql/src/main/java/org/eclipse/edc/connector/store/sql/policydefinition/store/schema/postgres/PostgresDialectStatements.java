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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.sql.policydefinition.store.schema.postgres;

import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static org.eclipse.edc.sql.dialect.PostgresDialect.getSelectFromJsonArrayTemplate;

/**
 * Statements and clauses specific to the Postgres dialect, such as JSON operators and functions.
 */
public class PostgresDialectStatements extends BaseSqlDialectStatements {
    // the aliases MUST be different from the actual column names, to avoid name clashes
    public static final String PROHIBITIONS_ALIAS = "pro";
    public static final String PERMISSIONS_ALIAS = "perm";
    public static final String OBLIGATIONS_ALIAS = "oblig";
    public static final String EXT_PROPERTIES_ALIAS = "extprop";

    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        if (querySpec.containsAnyLeftOperand("policy.prohibitions")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), getProhibitionsColumn(), PROHIBITIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new PolicyDefinitionMapping(this));
        } else if (querySpec.containsAnyLeftOperand("policy.permissions")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), getPermissionsColumn(), PERMISSIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new PolicyDefinitionMapping(this));
        } else if (querySpec.containsAnyLeftOperand("policy.obligations")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), getDutiesColumn(), OBLIGATIONS_ALIAS);
            return new SqlQueryStatement(select, querySpec, new PolicyDefinitionMapping(this));
        } else if (querySpec.containsAnyLeftOperand("policy.extensibleProperties")) {
            var select = getSelectFromJsonArrayTemplate(getSelectTemplate(), getExtensiblePropertiesColumn(), EXT_PROPERTIES_ALIAS);
            return new SqlQueryStatement(select, querySpec, new PolicyDefinitionMapping(this));
        } else {
            return super.createQuery(querySpec);
        }
    }

}
