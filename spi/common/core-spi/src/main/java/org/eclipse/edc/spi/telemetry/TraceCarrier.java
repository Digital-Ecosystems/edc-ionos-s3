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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.spi.telemetry;

import java.util.Map;

/**
 * Interface for trace context carrier entities.
 *
 * Use in combination with the various overloads of {@link Telemetry#contextPropagationMiddleware} to propagate the tracing context stored in the entity to the current thread.
 */
public interface TraceCarrier {

    Map<String, String> getTraceContext();

}
