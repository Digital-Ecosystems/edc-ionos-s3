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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;

/**
 * Transitions a transfer process to the {@link TransferProcessStates#DEPROVISIONING DEPROVISIONING} state.
 */
public class DeprovisionRequestHandler extends SingleTransferProcessCommandHandler<DeprovisionRequest> {

    public DeprovisionRequestHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<DeprovisionRequest> getType() {
        return DeprovisionRequest.class;
    }

    @Override
    protected boolean modify(TransferProcess process, DeprovisionRequest command) {
        process.transitionDeprovisioning();
        return true;
    }

    @Override
    protected void postAction(TransferProcess process) {

    }
}
