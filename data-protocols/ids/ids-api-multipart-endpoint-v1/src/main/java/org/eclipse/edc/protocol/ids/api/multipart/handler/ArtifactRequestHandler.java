/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.protocol.ids.spi.types.container.ArtifactRequestMessagePayload;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.inProcessFromServiceResult;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.malformedMessage;
import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;

public class ArtifactRequestHandler implements Handler {
    
    private final IdsId connectorId;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final ContractNegotiationStore contractNegotiationStore;
    private final Vault vault;
    private final TransferProcessService transferProcessService;

    public ArtifactRequestHandler(
            @NotNull Monitor monitor,
            @NotNull IdsId connectorId,
            @NotNull ObjectMapper objectMapper,
            @NotNull ContractNegotiationStore contractNegotiationStore,
            @NotNull Vault vault,
            @NotNull TransferProcessService transferProcessService) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.objectMapper = objectMapper;
        this.contractNegotiationStore = contractNegotiationStore;
        this.vault = vault;
        this.transferProcessService = transferProcessService;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof ArtifactRequestMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        var claimToken = multipartRequest.getClaimToken();
        var message = (ArtifactRequestMessage) multipartRequest.getHeader();

        // Validate request artifact ID
        var artifactUri = message.getRequestedArtifact();
        var artifactResult = IdsId.from(artifactUri.toString());
        if (artifactResult.failed()) {
            monitor.debug("ArtifactRequestHandler: Requested artifact URI is missing.");
            return createMultipartResponse(malformedMessage(multipartRequest.getHeader(), connectorId));
        }

        var artifactIdsId = artifactResult.getContent();
        if (artifactIdsId.getType() != IdsType.ARTIFACT) {
            monitor.debug("ArtifactRequestHandler: Requested artifact URI not of type artifact.");
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        // Validate contract ID
        var contractUri = message.getTransferContract();
        var contractResult = IdsId.from(contractUri.toString());
        if (contractResult.failed()) {
            monitor.debug("ArtifactRequestHandler: Transfer contract URI is missing.");
            return createMultipartResponse(malformedMessage(multipartRequest.getHeader(), connectorId));
        }

        var contractIdsId = contractResult.getContent();
        if (contractIdsId.getType() != IdsType.CONTRACT_AGREEMENT) {
            monitor.debug("ArtifactRequestHandler: Transfer contract URI not of type contract.");
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        // Get contract agreement for received contract ID
        var contractAgreement = contractNegotiationStore.findContractAgreement(contractIdsId.getValue());
        if (contractAgreement == null) {
            monitor.debug(String.format("ArtifactRequestHandler: No contract agreement with id %s found.", contractIdsId.getValue()));
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        // Verify that contract agreement is valid for requested artifact
        if (!artifactIdsId.getValue().equals(contractAgreement.getAssetId())) {
            monitor.debug(String.format("ArtifactRequestHandler: invalid artifact id specified %s for contract: %s", artifactIdsId.getValue(), contractIdsId.getValue()));
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        // Read request payload, which contains the data destination and an optional secret
        ArtifactRequestMessagePayload artifactRequestMessagePayload;
        try {
            artifactRequestMessagePayload = objectMapper.readValue(multipartRequest.getPayload(), ArtifactRequestMessagePayload.class);
        } catch (IOException e) {
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        var dataDestination = artifactRequestMessagePayload.getDataDestination();

        // Read request message properties
        Map<String, String> props = new HashMap<>();
        if (message.getProperties() != null) {
            message.getProperties().forEach((k, v) -> props.put(k, v.toString()));
        }

        // Get webhook address of requesting connector from message properties
        var idsWebhookAddress = Optional.ofNullable(props.remove(IDS_WEBHOOK_ADDRESS_PROPERTY))
                .map(Object::toString)
                .orElse(null);
        if (StringUtils.isNullOrBlank(idsWebhookAddress)) {
            var msg = "Ids webhook address is invalid";
            monitor.debug(String.format("%s: %s", getClass().getSimpleName(), msg));
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        // NB: DO NOT use the asset id provided by the client as that can open aan attack vector where a client references an artifact that
        //     is different from the one specified by the contract

        var dataRequest = DataRequest.Builder.newInstance()
                .id(message.getId().toString())
                .protocol(MessageProtocol.IDS_MULTIPART)
                .dataDestination(dataDestination)
                .connectorId(connectorId.toString())
                .assetId(contractAgreement.getAssetId())
                .contractId(contractAgreement.getId())
                .properties(props)
                .connectorAddress(idsWebhookAddress)
                .build();

        // Initiate a transfer process for the request
        var transferInitiateResult = transferProcessService.initiateTransfer(dataRequest, claimToken);

        // Store secret if process initiated successfully
        if (transferInitiateResult.succeeded() && artifactRequestMessagePayload.getSecret() != null) {
            vault.storeSecret(dataDestination.getKeyName(), artifactRequestMessagePayload.getSecret());
        }

        return createMultipartResponse(inProcessFromServiceResult(transferInitiateResult, multipartRequest.getHeader(), connectorId));
    }
}
