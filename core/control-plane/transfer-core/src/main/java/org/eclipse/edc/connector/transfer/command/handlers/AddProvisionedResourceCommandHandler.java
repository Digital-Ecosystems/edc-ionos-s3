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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.process.ProvisionCallbackDelegate;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.command.AddProvisionedResourceCommand;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.List;

/**
 * Processes a {@link AddProvisionedResourceCommand} by delegating it to the {@link TransferProcessManager}.
 * <p>
 * This class exists to avoid coupling the TPM to the command handler registry.
 */
public class AddProvisionedResourceCommandHandler implements CommandHandler<AddProvisionedResourceCommand> {
    private final ProvisionCallbackDelegate delegate;

    public AddProvisionedResourceCommandHandler(ProvisionCallbackDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(AddProvisionedResourceCommand command) {
        delegate.handleProvisionResult(command.getTransferProcessId(), List.of(StatusResult.success(command.getProvisionResponse())));
    }

    @Override
    public Class<AddProvisionedResourceCommand> getType() {
        return AddProvisionedResourceCommand.class;
    }

}
