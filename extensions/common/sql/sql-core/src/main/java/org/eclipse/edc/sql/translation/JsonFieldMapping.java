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

package org.eclipse.edc.sql.translation;

public class JsonFieldMapping extends TranslationMapping {
    protected final String columnName;

    public JsonFieldMapping(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getStatement(String canonicalPropertyName) {
        var tokens = canonicalPropertyName.split("\\.");

        var statementBuilder = new StringBuilder(columnName);
        int length = tokens.length;
        for (int i = 0; i < length - 1; i++) {
            statementBuilder.append(" -> ");
            statementBuilder.append("'").append(tokens[i]).append("'");
        }

        statementBuilder.append(" ->> ");
        statementBuilder.append("'").append(tokens[length - 1]).append("'");
        return statementBuilder.toString();
    }


}
