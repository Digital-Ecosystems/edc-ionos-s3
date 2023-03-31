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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;

/**
 * An implementation backed by the Atomikos transaction manager.
 */
public class AtomikosTransactionContext implements TransactionContext {
    private final Monitor monitor;
    private TransactionManager transactionManager;

    public AtomikosTransactionContext(Monitor monitor) {
        this.monitor = monitor;
    }

    public void initialize(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void execute(TransactionBlock block) {
        execute((ResultTransactionBlock<Void>) () -> {
            block.execute();
            return null;
        });
    }

    @Override
    public void registerSynchronization(TransactionSynchronization sync) {
        if (transactionManager == null) {
            throw new EdcException("Transaction context was not initialized");
        }
        try {
            var transaction = transactionManager.getTransaction();
            if (transaction == null) {
                throw new EdcException("A transaction is not active");
            }
            transaction.registerSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    sync.beforeCompletion();
                }

                @Override
                public void afterCompletion(int i) {

                }
            });
        } catch (SystemException | RollbackException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> T execute(ResultTransactionBlock<T> block) {
        var startedTransaction = false;
        Transaction transaction = null;
        try {
            transaction = transactionManager.getTransaction();
            if (transaction == null) {
                transactionManager.begin();
                transaction = transactionManager.getTransaction();
                startedTransaction = true;
            }

            return block.execute();

        } catch (Exception e) {
            try {
                if (transaction != null) {
                    transaction.setRollbackOnly();
                }
            } catch (SystemException ex) {
                monitor.severe("Error setting rollback", ex);
            }
            throw new EdcException(e);
        } finally {
            if (startedTransaction) {
                try {
                    var status = transactionManager.getStatus();
                    if (STATUS_ACTIVE == status) {
                        transactionManager.commit();
                    } else if (STATUS_MARKED_ROLLBACK == status) {
                        transactionManager.rollback();
                    }
                } catch (HeuristicRollbackException | SystemException | HeuristicMixedException | RollbackException e) {
                    monitor.severe("Transaction error", e);
                }
            }
        }
    }
}
