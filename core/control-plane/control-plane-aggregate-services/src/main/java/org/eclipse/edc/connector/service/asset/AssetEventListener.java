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

package org.eclipse.edc.connector.service.asset;

import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.asset.AssetCreated;
import org.eclipse.edc.spi.event.asset.AssetDeleted;
import org.eclipse.edc.spi.observe.asset.AssetListener;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.time.Clock;

/**
 * Listener responsible for creating and publishing events regarding Asset state changes
 */
public class AssetEventListener implements AssetListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public AssetEventListener(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(Asset asset) {
        var event = AssetCreated.Builder.newInstance()
                .assetId(asset.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deleted(Asset asset) {
        var event = AssetDeleted.Builder.newInstance()
                .assetId(asset.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }
}
