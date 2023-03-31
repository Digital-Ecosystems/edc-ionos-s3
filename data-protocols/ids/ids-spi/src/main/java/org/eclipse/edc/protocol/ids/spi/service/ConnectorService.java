/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.protocol.ids.spi.service;

import org.eclipse.edc.protocol.ids.spi.domain.connector.Connector;
import org.eclipse.edc.protocol.ids.spi.types.container.DescriptionRequest;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.NotNull;


/**
 * The IDS service is able to create IDS compliant descriptions of resources. These descriptions may be used to create a
 * self-description or answer a Description Request Message.
 */
@ExtensionPoint
public interface ConnectorService {

    /**
     * Provides the connector object, which may be used by the IDS self-description of the connector.
     *
     * @return connector description
     */
    @NotNull
    Connector getConnector(@NotNull DescriptionRequest descriptionRequest);
}
