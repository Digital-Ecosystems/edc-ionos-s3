/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.listener;

import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessCancelled;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessDeprovisioningRequested;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessEnded;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessFailed;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessInitiated;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessProvisioningRequested;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessRequested;

import java.time.Clock;

/**
 * Listener responsible for creating and publishing events regarding TransferProcess state changes
 */
public class TransferProcessEventListener implements TransferProcessListener {
    private final EventRouter eventRouter;
    private final Clock clock;

    public TransferProcessEventListener(EventRouter eventRouter, Clock clock) {
        this.eventRouter = eventRouter;
        this.clock = clock;
    }

    @Override
    public void initiated(TransferProcess process) {
        var event = TransferProcessInitiated.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void provisioningRequested(TransferProcess process) {
        var event = TransferProcessProvisioningRequested.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void provisioned(TransferProcess process) {
        var event = TransferProcessProvisioned.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void requested(TransferProcess process) {
        var event = TransferProcessRequested.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void completed(TransferProcess process) {
        var event = TransferProcessCompleted.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deprovisioningRequested(TransferProcess process) {
        var event = TransferProcessDeprovisioningRequested.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deprovisioned(TransferProcess process) {
        var event = TransferProcessDeprovisioned.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void ended(TransferProcess process) {
        var event = TransferProcessEnded.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void cancelled(TransferProcess process) {
        var event = TransferProcessCancelled.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void failed(TransferProcess process) {
        var event = TransferProcessFailed.Builder.newInstance()
                .transferProcessId(process.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }
}
