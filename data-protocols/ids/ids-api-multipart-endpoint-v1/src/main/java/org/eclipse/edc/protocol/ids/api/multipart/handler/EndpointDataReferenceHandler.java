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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformer;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.internalRecipientError;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.messageProcessedNotification;

/**
 * Implementation of the {@link Handler} class for handling of {@link EndpointDataReferenceMessage}.
 * Note that we use the {@link ParticipantUpdateMessage} IDS message to convey the {@link EndpointDataReferenceMessage}.
 */
public class EndpointDataReferenceHandler implements Handler {

    private final Monitor monitor;
    private final IdsId connectorId;
    private final EndpointDataReferenceReceiverRegistry receiverRegistry;
    private final EndpointDataReferenceTransformerRegistry transformerRegistry;
    private final TypeManager typeManager;

    public EndpointDataReferenceHandler(@NotNull Monitor monitor,
                                        @NotNull IdsId connectorId,
                                        @NotNull EndpointDataReferenceReceiverRegistry receiverRegistry,
                                        @NotNull EndpointDataReferenceTransformerRegistry transformerRegistry,
                                        @NotNull TypeManager typeManager) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.receiverRegistry = receiverRegistry;
        this.transformerRegistry = transformerRegistry;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof ParticipantUpdateMessage;
    }

    /**
     * Handling of the request is as follows:
     * - decode {@link EndpointDataReference} from the request payload,
     * - apply a {@link EndpointDataReferenceTransformer} on the previous EDR,
     * - finally apply {@link EndpointDataReferenceReceiver} to the resulting EDR to dispatch it into the consumer environment.
     */
    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        // Read and transform the endpoint data reference from the request payload (using the default object mapper)
        var edr = typeManager.readValue(multipartRequest.getPayload(), EndpointDataReference.class);
        var transformationResult = transformerRegistry.transform(edr);
        if (transformationResult.failed()) {
            monitor.severe("EDR transformation failed: " + String.join(", ", transformationResult.getFailureMessages()));
            return createMultipartResponse(badParameters(multipartRequest.getHeader(), connectorId));
        }

        var transformedEdr = transformationResult.getContent();

        // Apply all endpoint data reference receivers to the endpoint data reference
        var receiveResult = receiverRegistry.receiveAll(transformedEdr).join();
        if (receiveResult.failed()) {
            monitor.severe("EDR dispatch failed: " + String.join(", ", receiveResult.getFailureMessages()));
            return createMultipartResponse(internalRecipientError(multipartRequest.getHeader(), connectorId));
        }

        return createMultipartResponse(messageProcessedNotification(multipartRequest.getHeader(), connectorId));
    }

}
