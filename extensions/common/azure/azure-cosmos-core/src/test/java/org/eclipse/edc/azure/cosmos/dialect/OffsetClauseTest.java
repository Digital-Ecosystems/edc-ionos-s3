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

package org.eclipse.edc.azure.cosmos.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetClauseTest {

    @Test
    void asString() {
        assertThat(new OffsetClause(null).asString()).isNotNull().isEmpty();
        assertThat(new OffsetClause(14).asString()).isEqualTo("OFFSET 14");
    }
}