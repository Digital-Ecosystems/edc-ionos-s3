/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

import org.eclipse.edc.spi.system.configuration.Config;

/**
 * Contributes configuration to a runtime. Multiple configuration extensions may be loaded in a runtime.
 */
public interface ConfigurationExtension extends BootExtension {

    /**
     * Gets the configuration starting from the root
     *
     * @return A config object
     */
    Config getConfig();
}
