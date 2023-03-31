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

package org.eclipse.edc.spi.system;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Services for creating instrumented {@link java.util.concurrent.Executor}s, to
 * collect execution metrics when available.
 * <p>
 * The default implementation does not provide any instrumentation. Extension
 * modules can provide implementations of the {@link ExecutorInstrumentation} sub-interface,
 * such as for collecting metrics.
 */
@ExtensionPoint
public interface ExecutorInstrumentation {
    /**
     * Default implementation that does not provide any instrumentation. Extension
     * modules can provide implementations, such as for collecting metrics.
     *
     * @return a default {@link ExecutorInstrumentation} implementation.
     */
    static ExecutorInstrumentation noop() {
        return new ExecutorInstrumentation() {
        };
    }

    /**
     * Instrument a {@link ScheduledExecutorService}.
     *
     * @param target service to instrument.
     * @param name   name used to tag metrics.
     * @return instrumented service.
     */
    default ScheduledExecutorService instrument(ScheduledExecutorService target, String name) {
        return target;
    }

    /**
     * Instrument an {@link ExecutorService}.
     *
     * @param target service to instrument.
     * @param name   name used to tag metrics.
     * @return instrumented service.
     */
    default ExecutorService instrument(ExecutorService target, String name) {
        return target;
    }
}
