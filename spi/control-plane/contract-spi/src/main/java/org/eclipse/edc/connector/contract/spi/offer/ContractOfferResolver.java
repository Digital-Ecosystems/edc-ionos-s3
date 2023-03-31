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
 *
 */

package org.eclipse.edc.connector.contract.spi.offer;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * Resolves contract offers.
 */

@ExtensionPoint
public interface ContractOfferResolver {

    /**
     * Resolves contract offers.
     */
    @NotNull
    Stream<ContractOffer> queryContractOffers(ContractOfferQuery query);

}
