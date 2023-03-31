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

package org.eclipse.edc.transaction.atomikos;

import org.junit.jupiter.api.Test;

class AtomikosTransactionPlatformTest {

    @Test
    void verifyRecovery() throws Exception {
        var platform = new AtomikosTransactionPlatform(TransactionManagerConfiguration.Builder.newInstance().name("test").build());
        platform.recover();

        platform.getTransactionManager().begin();
        platform.getTransactionManager().commit();

        platform.stop();
    }

}
