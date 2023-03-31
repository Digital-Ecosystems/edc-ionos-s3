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

package org.eclipse.edc.spi.system.health;

import java.util.function.Supplier;

/**
 * Implementations contribute to determine if a runtime is ready to process requests.
 */
public interface ReadinessProvider extends Supplier<HealthCheckResult> {
}
