/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerPullTransferProxyResolverImplTest {

    private DataPlaneSelectorClient selectorClient;
    private ConsumerPullTransferProxyResolverImpl resolver;

    @BeforeEach
    public void setUp() {
        selectorClient = mock(DataPlaneSelectorClient.class);
        resolver = new ConsumerPullTransferProxyResolverImpl(selectorClient, UUID.randomUUID().toString());
    }

    @Test
    void verifyResolveSuccess() {
        var address = DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
        var proxyUrl = "test.proxy.url";
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .property("publicApiUrl", proxyUrl)
                .build();

        var srcAddressCaptor = ArgumentCaptor.forClass(DataAddress.class);
        var destAddressCaptor = ArgumentCaptor.forClass(DataAddress.class);

        when(selectorClient.find(srcAddressCaptor.capture(), destAddressCaptor.capture(), ArgumentCaptor.forClass(String.class).capture())).thenReturn(instance);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(proxyUrl);

        assertThat(srcAddressCaptor.getValue()).isEqualTo(address);
        assertThat(destAddressCaptor.getValue().getType()).isEqualTo("HttpProxy");
    }

    @Test
    void verifyFailedResultReturnedIfDataPlaneResolutionFails() {
        var address = DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();

        when(selectorClient.find(any(), any(), any())).thenReturn(null);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anySatisfy(s -> assertThat(s).contains(address.getType()));
    }

    @Test
    void verifyFailedResultReturnedIfDataPlaneInstanceDoesNotContainPublicApiUrl() {
        var address = DataAddress.Builder.newInstance().type(UUID.randomUUID().toString()).build();
        var instance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://some.test.url")
                .build();

        when(selectorClient.find(any(), any(), any())).thenReturn(instance);

        var result = resolver.resolveProxyUrl(address);

        verify(selectorClient).find(any(), any(), any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).contains("Missing property `publicApiUrl` in DataPlaneInstance");
    }
}