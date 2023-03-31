/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.transaction.spi;


import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Implementations execute code within a transactional boundary. A {@code TransactionContext} provides a consistent programming model for local and global (e.g. JTA) transaction
 * infrastructure. Specifically, client code executes transactional code in the same way whether the context is backed by a local resource such as a single JDBC connection pool
 * or a JTA transaction manager with enlisted resources.
 * <p>
 * Implementations must support joining existing transactions. Nested executions will therefore join the transactional context of their parent.
 * <p>
 * Transactional semantics will vary by implementation. For example, an implementation may only support atomicity when a single resource is enlisted in a transaction.
 */
@ExtensionPoint
public interface TransactionContext {

    /**
     * Executes the code within a transaction.
     */
    void execute(TransactionBlock block);

    /**
     * Executes the code within a transaction producing a result
     */
    <T> T execute(ResultTransactionBlock<T> block);

    /**
     * Registers a synchronization that will be called before a transaction commits or is rolled back.
     */
    void registerSynchronization(TransactionSynchronization sync);

    /**
     * Defines a block of transactional code.
     */
    @FunctionalInterface
    interface TransactionBlock {

        /**
         * Executes the transaction block.
         */
        void execute();

    }

    /**
     * Defines a block of transactional code producing a result
     */
    @FunctionalInterface
    interface ResultTransactionBlock<T> {
        T execute();
    }

    /**
     * Implementations receive callbacks before a transaction commits or is rolled back.
     */
    @FunctionalInterface
    interface TransactionSynchronization {
        void beforeCompletion();
    }
}
