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

import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.SqlPolicyStoreStatements;
import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

public class PolicyMapping extends TranslationMapping {
    public PolicyMapping(SqlPolicyStoreStatements statements) {
        add("permissions", new JsonFieldMapping(PostgresDialectStatements.PERMISSIONS_ALIAS));
        add("prohibitions", new JsonFieldMapping(PostgresDialectStatements.PROHIBITIONS_ALIAS));
        add("obligations", new JsonFieldMapping(PostgresDialectStatements.OBLIGATIONS_ALIAS));
        add("extensibleProperties", new JsonFieldMapping(PostgresDialectStatements.EXT_PROPERTIES_ALIAS));
        add("inheritsFrom", statements.getInheritsFromColumn());
        add("assigner", statements.getAssignerColumn());
        add("assignee", statements.getAssigneeColumn());
        add("target", statements.getTargetColumn());
        add("type", statements.getTypeColumn());
    }
}
