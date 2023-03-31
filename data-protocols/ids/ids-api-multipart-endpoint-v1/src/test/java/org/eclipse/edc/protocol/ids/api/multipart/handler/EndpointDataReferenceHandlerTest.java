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

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.ParticipantCertificateRevokedMessageBuilder;
import de.fraunhofer.iais.eis.ParticipantUpdateMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EndpointDataReferenceHandler handler;
    private EndpointDataReferenceReceiverRegistry receiverRegistry;
    private EndpointDataReferenceTransformerRegistry transformerRegistry;

    @BeforeEach
    public void setUp() {
        var monitor = mock(Monitor.class);
        var connectorId = IdsId.from("urn:connector:edc").getContent();
        receiverRegistry = mock(EndpointDataReferenceReceiverRegistry.class);
        transformerRegistry = mock(EndpointDataReferenceTransformerRegistry.class);
        var typeManager = new TypeManager();
        handler = new EndpointDataReferenceHandler(monitor, connectorId, receiverRegistry, transformerRegistry, typeManager);
    }

    @Test
    void canHandle_supportedMessage_shouldReturnTrue() throws JsonProcessingException {
        var request = createMultipartRequest(createEndpointDataReference());
        assertThat(handler.canHandle(request)).isTrue();
    }

    @Test
    void canHandle_messageNotSupported_shouldReturnFalse() {
        var request = MultipartRequest.Builder.newInstance()
                .header(new ParticipantCertificateRevokedMessageBuilder().build())
                .claimToken(createClaimToken())
                .build();
        assertThat(handler.canHandle(request)).isFalse();
    }

    @Test
    void handleRequest_success_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var inputEdr = createEndpointDataReference();
        var edrAfterTransformation = createEndpointDataReference();
        var request = createMultipartRequest(inputEdr);

        var edrCapture = ArgumentCaptor.forClass(EndpointDataReference.class);

        when(transformerRegistry.transform(any())).thenReturn(Result.success(edrAfterTransformation));
        when(receiverRegistry.receiveAll(edrAfterTransformation)).thenReturn(CompletableFuture.completedFuture(Result.success()));

        var response = handler.handleRequest(request);

        verify(transformerRegistry, times(1)).transform(edrCapture.capture());

        assertThat(edrCapture.getValue()).satisfies(t -> {
            assertThat(t.getEndpoint()).isEqualTo(inputEdr.getEndpoint());
            assertThat(t.getAuthKey()).isEqualTo(inputEdr.getAuthKey());
            assertThat(t.getAuthCode()).isEqualTo(inputEdr.getAuthCode());
            assertThat(t.getId()).isEqualTo(inputEdr.getId());
            assertThat(t.getProperties()).isEqualTo(inputEdr.getProperties());
        });

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(MessageProcessedNotificationMessage.class));
    }

    @Test
    void handleRequest_transformationFailure_shouldReturnRejectionMessage() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformerRegistry.transform(any())).thenReturn(Result.failure("Test failure"));

        var response = handler.handleRequest(request);

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    @Test
    void handleRequest_receiveFailure_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformerRegistry.transform(any())).thenReturn(Result.success(edr));
        when(receiverRegistry.receiveAll(edr)).thenReturn(CompletableFuture.completedFuture(Result.failure("Test failure")));

        var response = handler.handleRequest(request);

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    private EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("some.endpoint.url")
                .authKey("test-authkey")
                .authCode(UUID.randomUUID().toString())
                .id(UUID.randomUUID().toString())
                .properties(Map.of("key1", UUID.randomUUID().toString()))
                .build();
    }

    private MultipartRequest createMultipartRequest(EndpointDataReference payload) throws JsonProcessingException {
        return MultipartRequest.Builder.newInstance()
                .header(new ParticipantUpdateMessageBuilder().build())
                .payload(MAPPER.writeValueAsString(payload))
                .claimToken(createClaimToken())
                .build();
    }

    private ClaimToken createClaimToken() {
        return ClaimToken.Builder.newInstance().build();
    }
}