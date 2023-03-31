/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.edc.connector.service.catalog;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceImplTest {

    private final RemoteMessageDispatcherRegistry dispatcher = mock(RemoteMessageDispatcherRegistry.class);

    @Test
    void shouldSendCatalogRequestToDispatcher() {
        var service = new CatalogServiceImpl(dispatcher);
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now())
                .build();
        var catalog = Catalog.Builder.newInstance().id("id").contractOffers(List.of(contractOffer)).build();
        when(dispatcher.send(any(), any(), any())).thenReturn(completedFuture(catalog))
                .thenReturn(completedFuture(Catalog.Builder.newInstance().id("id2").contractOffers(List.of()).build()));

        var future = service.getByProviderUrl("test.provider.url", new QuerySpec());

        assertThat(future).succeedsWithin(1, SECONDS).extracting(Catalog::getContractOffers, InstanceOfAssertFactories.list(ContractOffer.class)).hasSize(1);
        verify(dispatcher, times(1)).send(eq(Catalog.class), isA(CatalogRequest.class), any());
    }
}
