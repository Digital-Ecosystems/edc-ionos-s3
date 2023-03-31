/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation and others
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Daimler TSS GmbH - verify exceptions are re-thrown
 *
 */

package org.eclipse.edc.transaction.local;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transaction.spi.local.LocalTransactionResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LocalTransactionContextTest {
    private LocalTransactionContext transactionContext;
    private LocalTransactionResource dsResource;

    @Test
    void verifyTransaction() {
        // executed a transaction block
        transactionContext.execute(() -> {
        });

        // start and commit should only be called
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).commit();
    }

    @Test
    void verifyJoinNestedTransaction() {
        // executed a nested transaction block
        transactionContext.execute(() -> transactionContext.execute(() -> {
        }));

        // start and commit should only be called once since the nexted trx joins the parent context
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).commit();
    }

    @Test
    void verifyRollbackTransaction() {
        // executed a transaction block
        assertThrows(EdcException.class, () -> transactionContext.execute(() -> {
            throw new RuntimeException();
        }));

        // start and rollback should only be called once
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).rollback();
    }

    @Test
    void verifyRollbackNestedTransaction() {
        // executed a nested transaction block
        assertThrows(EdcException.class, () -> transactionContext.execute(() -> transactionContext.execute(() -> {
            throw new RuntimeException();
        })));

        // start and rollback should only be called once since the nexted trx joins the parent context
        verify(dsResource, times(1)).start();
        verify(dsResource, times(1)).rollback();
    }

    @Test
    void verifyMultipleResourceEnlistmentCommit() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        transactionContext.execute(() -> {
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).commit();
        verify(dsResource2, times(1)).commit();
    }

    @Test
    void verifyMultipleResourceEnlistmentRollback() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        assertThrows(EdcException.class, () -> transactionContext.execute(() -> {
            throw new RuntimeException();
        }));

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).rollback();
        verify(dsResource2, times(1)).rollback();
    }

    @Test
    void verifyMultipleResourceEnlistmentFailureCommit() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        doThrow(new RuntimeException()).when(dsResource).commit();

        transactionContext.execute(() -> {
        });

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).commit();
        verify(dsResource2, times(1)).commit();  // ensure commit was called on resource after the exception was thrown
    }

    @Test
    void verifyMultipleResourceEnlistmentFailureRollback() {
        var dsResource2 = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource2);

        doThrow(new RuntimeException()).when(dsResource).rollback();

        assertThrows(EdcException.class, () -> transactionContext.execute(() -> {
            throw new RuntimeException();
        }));

        verify(dsResource, times(1)).start();
        verify(dsResource2, times(1)).start();
        verify(dsResource, times(1)).rollback();
        verify(dsResource2, times(1)).rollback();  // ensure commit was called on resource after the exception was thrown
    }

    @Test
    void verifySynchronization() {
        var sync = mock(TransactionContext.TransactionSynchronization.class);

        // the sync should be invoked
        transactionContext.execute(() -> transactionContext.registerSynchronization(sync));

        // the sync should be cleared and should not be invoked again
        transactionContext.execute(() -> {
        });

        verify(sync, times(1)).beforeCompletion();
    }

    @BeforeEach
    void setUp() {
        transactionContext = new LocalTransactionContext(mock(Monitor.class));
        dsResource = mock(LocalTransactionResource.class);
        transactionContext.registerResource(dsResource);
    }
}
