/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.transform.IdsTransformerRegistryImpl;
import org.eclipse.edc.protocol.ids.transform.type.contract.ContractOfferToIdsContractOfferTransformer;
import org.eclipse.edc.protocol.ids.transform.type.policy.ActionToIdsActionTransformer;
import org.eclipse.edc.protocol.ids.transform.type.policy.PermissionToIdsPermissionTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartContractOfferSenderTest {

    private MultipartContractOfferSender sender;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var connectorId = IdsId.from("urn:connector:edc").getContent();
        var webhookAddress = "https://webhook";

        var transformerRegistry = new IdsTransformerRegistryImpl();
        transformerRegistry.register(new ContractOfferToIdsContractOfferTransformer());
        transformerRegistry.register(new PermissionToIdsPermissionTransformer());
        transformerRegistry.register(new ActionToIdsActionTransformer());

        objectMapper = IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());

        var senderContext = new SenderDelegateContext(connectorId, objectMapper, transformerRegistry, webhookAddress);
        sender = new MultipartContractOfferSender(senderContext);
    }

    @Test
    void buildMessagePayload_initialRequest_mapPolicyProperties() throws Exception {
        var policy = getPolicy();
        var request = getContractOfferRequest(policy, ContractOfferRequest.Type.INITIAL);

        var result = sender.buildMessagePayload(request);

        var contractRequest = objectMapper.readValue(result, ContractRequest.class);
        assertThat(contractRequest.getProperties())
                .hasSize(2)
                .containsAllEntriesOf(policy.getExtensibleProperties());
    }

    @Test
    void buildMessagePayload_notInitialRequest_mapPolicyProperties() throws Exception {
        var policy = getPolicy();
        var request = getContractOfferRequest(policy, ContractOfferRequest.Type.COUNTER_OFFER);

        var result = sender.buildMessagePayload(request);

        var contractOffer = objectMapper.readValue(result, de.fraunhofer.iais.eis.ContractOffer.class);
        assertThat(contractOffer.getProperties())
                .hasSize(2)
                .containsAllEntriesOf(policy.getExtensibleProperties());
    }

    private ContractOfferRequest getContractOfferRequest(Policy policy, ContractOfferRequest.Type type) {
        return ContractOfferRequest.Builder.newInstance()
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id("contract-offer")
                        .policy(policy)
                        .asset(Asset.Builder.newInstance().id("asset-id").build())
                        .contractStart(ZonedDateTime.now())
                        .contractEnd(ZonedDateTime.now().plusMonths(1))
                        .build())
                .protocol("protocol")
                .connectorId("connector")
                .connectorAddress("https://connector")
                .type(type)
                .build();
    }

    private Policy getPolicy() {
        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return Policy.Builder.newInstance()
                .permission(usePermission)
                .extensibleProperty("key1", "value1")
                .extensibleProperty("key2", "value2")
                .build();
    }
}
