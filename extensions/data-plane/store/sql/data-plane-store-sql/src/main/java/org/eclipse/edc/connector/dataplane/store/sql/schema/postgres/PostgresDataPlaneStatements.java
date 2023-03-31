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

package org.eclipse.edc.connector.dataplane.store.sql.schema.postgres;

import org.eclipse.edc.connector.dataplane.store.sql.schema.BaseSqlDataPlaneStatements;
import org.eclipse.edc.sql.dialect.PostgresDialect;

public class PostgresDataPlaneStatements extends BaseSqlDataPlaneStatements {


    @Override
    public String getFormatAsJsonOperator() {
        return PostgresDialect.getJsonCastOperator();
    }
}
