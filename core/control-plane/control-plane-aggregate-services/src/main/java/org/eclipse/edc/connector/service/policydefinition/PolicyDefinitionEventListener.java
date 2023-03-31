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

package org.eclipse.edc.connector.service.policydefinition;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionListener;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionCreated;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionDeleted;

import java.time.Clock;

/**
 * Listener responsible for creating and publishing events regarding PolicyDefinition state changes
 */
public class PolicyDefinitionEventListener implements PolicyDefinitionListener {
    private final Clock clock;
    private final EventRouter eventRouter;

    public PolicyDefinitionEventListener(Clock clock, EventRouter eventRouter) {
        this.clock = clock;
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(PolicyDefinition policyDefinition) {
        var event = PolicyDefinitionCreated.Builder.newInstance()
                .policyDefinitionId(policyDefinition.getUid())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deleted(PolicyDefinition policyDefinition) {
        var event = PolicyDefinitionDeleted.Builder.newInstance()
                .policyDefinitionId(policyDefinition.getUid())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }
}
