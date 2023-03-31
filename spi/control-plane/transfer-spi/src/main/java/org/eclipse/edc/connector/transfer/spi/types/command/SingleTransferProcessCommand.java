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

package org.eclipse.edc.connector.transfer.spi.types.command;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.command.Command;

/**
 * Specialization of the {@link Command} interface, that is useful in situations where
 * a single {@link TransferProcess} is
 * operated on.
 */
public class SingleTransferProcessCommand extends TransferProcessCommand {
    protected final String transferProcessId;

    public SingleTransferProcessCommand(String transferProcessId) {
        super();
        this.transferProcessId = transferProcessId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }
}
