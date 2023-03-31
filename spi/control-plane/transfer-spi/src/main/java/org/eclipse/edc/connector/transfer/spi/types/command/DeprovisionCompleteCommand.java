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

package org.eclipse.edc.connector.transfer.spi.types.command;

import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;

/**
 * Informs the system that deprovisioning a resource has indeed completed.
 */
public class DeprovisionCompleteCommand extends SingleTransferProcessCommand {
    private final DeprovisionedResource resource;

    public DeprovisionCompleteCommand(String transferProcessId, DeprovisionedResource resource) {
        super(transferProcessId);
        this.resource = resource;
    }

    public DeprovisionedResource getResource() {
        return resource;
    }
}
