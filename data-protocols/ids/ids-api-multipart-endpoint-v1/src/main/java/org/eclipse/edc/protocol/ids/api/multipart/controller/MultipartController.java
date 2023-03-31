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
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements, refactoring
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.TokenFormat;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.protocol.ids.api.multipart.handler.Handler;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.malformedMessage;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.messageTypeNotSupported;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.notAuthenticated;

@Consumes({MediaType.MULTIPART_FORM_DATA})
@Produces({MediaType.MULTIPART_FORM_DATA})
@Path(MultipartController.PATH)
public class MultipartController {

    public static final String PATH = "/data";
    private static final String HEADER = "header";
    private static final String PAYLOAD = "payload";

    private final Monitor monitor;
    private final IdsId connectorId;
    private final List<Handler> multipartHandlers;
    private final ObjectMapper objectMapper;
    private final DynamicAttributeTokenService tokenService;
    private final String idsWebhookAddress;

    public MultipartController(@NotNull Monitor monitor,
                               @NotNull IdsId connectorId,
                               @NotNull ObjectMapper objectMapper,
                               @NotNull DynamicAttributeTokenService tokenService,
                               @NotNull List<Handler> multipartHandlers,
                               @NotNull String idsWebhookAddress) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.objectMapper = objectMapper;
        this.multipartHandlers = multipartHandlers;
        this.tokenService = tokenService;
        this.idsWebhookAddress = idsWebhookAddress;
    }

    /**
     * Processes an incoming IDS multipart request. Validates the message header before passing the
     * request to a handler depending on the message type.
     *
     * @param headerInputStream the multipart header part.
     * @param payload the multipart payload part.
     * @return a multipart response with code 200. In case of error, the multipart header is a
     *         rejection message.
     */
    @POST
    public FormDataMultiPart request(@FormDataParam(HEADER) InputStream headerInputStream,
                                     @FormDataParam(PAYLOAD) String payload) {
        if (headerInputStream == null) {
            return createFormDataMultiPart(malformedMessage(null, connectorId));
        }

        Message header;
        try {
            header = objectMapper.readValue(headerInputStream, Message.class);
        } catch (IOException e) {
            return createFormDataMultiPart(malformedMessage(null, connectorId));
        }

        if (header == null) {
            return createFormDataMultiPart(malformedMessage(null, connectorId));
        }

        // Check if any required header field missing
        if (header.getId() == null || header.getIssuerConnector() == null || header.getSenderAgent() == null) {
            return createFormDataMultiPart(malformedMessage(header, connectorId));
        }

        // Check if DAT present
        var dynamicAttributeToken = header.getSecurityToken();
        if (dynamicAttributeToken == null || dynamicAttributeToken.getTokenValue() == null) {
            monitor.warning("MultipartController: Token is missing in header");
            return createFormDataMultiPart(notAuthenticated(header, connectorId));
        }

        // Validate DAT
        var verificationResult = tokenService
                .verifyDynamicAttributeToken(dynamicAttributeToken, header.getIssuerConnector(), idsWebhookAddress);
        if (verificationResult.failed()) {
            monitor.warning(format("MultipartController: Token validation failed %s", verificationResult.getFailure().getMessages()));
            return createFormDataMultiPart(notAuthenticated(header, connectorId));
        }

        // Build the multipart request
        var claimToken = verificationResult.getContent();
        var multipartRequest = MultipartRequest.Builder.newInstance()
                .header(header)
                .payload(payload)
                .claimToken(claimToken)
                .build();

        var multipartResponse = multipartHandlers.stream()
                .filter(h -> h.canHandle(multipartRequest))
                .findFirst()
                .map(it -> it.handleRequest(multipartRequest))
                .orElseGet(() -> MultipartResponse.Builder.newInstance()
                        .header(messageTypeNotSupported(header, connectorId))
                        .build());

        multipartResponse.setSecurityToken(this::getToken);
        return createFormDataMultiPart(multipartResponse.getHeader(), multipartResponse.getPayload());
    }

    /**
     * Builds a form-data multipart body with the given header and payload.
     *
     * @param header the header.
     * @param payload the payload.
     * @return a multipart body.
     */
    private FormDataMultiPart createFormDataMultiPart(Message header, Object payload) {
        var multiPart = createFormDataMultiPart(header);

        if (payload != null) {
            multiPart.bodyPart(new FormDataBodyPart(PAYLOAD, toJson(payload), MediaType.APPLICATION_JSON_TYPE));
        }

        return multiPart;
    }

    /**
     * Builds a form-data multipart body with the given header.
     *
     * @param header the header.
     * @return a multipart body.
     */
    private FormDataMultiPart createFormDataMultiPart(Message header) {
        var multiPart = new FormDataMultiPart();
        if (header != null) {
            multiPart.bodyPart(new FormDataBodyPart(HEADER, toJson(header), MediaType.APPLICATION_JSON_TYPE));
        }
        return multiPart;
    }

    /**
     * Retrieves an identity token for the given message. Returns a token with value "invalid" if
     * obtaining an identity token fails.
     *
     * @param header the message.
     * @return the token.
     */
    private DynamicAttributeToken getToken(Message header) {
        if (header.getRecipientConnector() != null && !header.getRecipientConnector().isEmpty()) {
            var recipient = header.getRecipientConnector().get(0);
            var tokenResult = tokenService.obtainDynamicAttributeToken(recipient.toString());
            if (tokenResult.succeeded()) {
                return tokenResult.getContent();
            }
        }

        return new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_("invalid")
                .build();
    }

    private byte[] toJson(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
