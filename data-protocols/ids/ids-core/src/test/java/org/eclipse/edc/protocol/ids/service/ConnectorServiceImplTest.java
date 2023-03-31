/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
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

package org.eclipse.edc.protocol.ids.service;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.protocol.ids.spi.domain.connector.SecurityProfile;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.container.DescriptionRequest;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectorServiceImplTest {
    private static final IdsId CONNECTOR_ID = IdsId.from("urn:connector:edc").getContent();
    private static final String CONNECTOR_TITLE = "connectorTitle";
    private static final String CONNECTOR_DESCRIPTION = "connectorDescription";
    private static final SecurityProfile CONNECTOR_SECURITY_PROFILE = SecurityProfile.TRUST_PLUS_SECURITY_PROFILE;
    private static final URI CONNECTOR_ENDPOINT = URI.create("https://example.com/connector/endpoint");
    private static final URI CONNECTOR_MAINTAINER = URI.create("https://example.com/connector/maintainer");
    private static final URI CONNECTOR_CURATOR = URI.create("https://example.com/connector/curator");
    private static final String CONNECTOR_VERSION = "0.0.1-SNAPSHOT";
    private final ConnectorServiceSettings connectorServiceSettings = mock(ConnectorServiceSettings.class);
    private final CatalogService dataCatalogService = mock(CatalogService.class);

    private ConnectorServiceImpl connectorService;

    @BeforeEach
    void setUp() {
        connectorService = new ConnectorServiceImpl(connectorServiceSettings, dataCatalogService);
    }

    @Test
    void getConnector() {
        when(dataCatalogService.getDataCatalog(any())).thenReturn(mock(Catalog.class));
        when(connectorServiceSettings.getId()).thenReturn(CONNECTOR_ID);
        when(connectorServiceSettings.getTitle()).thenReturn(CONNECTOR_TITLE);
        when(connectorServiceSettings.getDescription()).thenReturn(CONNECTOR_DESCRIPTION);
        when(connectorServiceSettings.getSecurityProfile()).thenReturn(CONNECTOR_SECURITY_PROFILE);
        when(connectorServiceSettings.getEndpoint()).thenReturn(CONNECTOR_ENDPOINT);
        when(connectorServiceSettings.getMaintainer()).thenReturn(CONNECTOR_MAINTAINER);
        when(connectorServiceSettings.getCurator()).thenReturn(CONNECTOR_CURATOR);
        var claimToken = ClaimToken.Builder.newInstance().build();
        var descriptionRequest = DescriptionRequest.Builder.newInstance()
                .id(IdsId.from("urn:connector:any").getContent())
                .claimToken(claimToken)
                .querySpec(QuerySpec.none())
                .build();

        var result = connectorService.getConnector(descriptionRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CONNECTOR_ID);
        assertThat(result.getTitle()).isEqualTo(CONNECTOR_TITLE);
        assertThat(result.getDescription()).isEqualTo(CONNECTOR_DESCRIPTION);
        assertThat(result.getSecurityProfile()).isEqualTo(CONNECTOR_SECURITY_PROFILE);
        assertThat(result.getEndpoint()).isEqualTo(CONNECTOR_ENDPOINT);
        assertThat(result.getMaintainer()).isEqualTo(CONNECTOR_MAINTAINER);
        assertThat(result.getCurator()).isEqualTo(CONNECTOR_CURATOR);
        assertThat(result.getConnectorVersion()).isEqualTo(CONNECTOR_VERSION);
        verify(dataCatalogService).getDataCatalog(any());
        verify(connectorServiceSettings).getId();
        verify(connectorServiceSettings).getTitle();
        verify(connectorServiceSettings).getDescription();
        verify(connectorServiceSettings).getSecurityProfile();
        verify(connectorServiceSettings).getEndpoint();
        verify(connectorServiceSettings).getMaintainer();
        verify(connectorServiceSettings).getCurator();
    }

}
