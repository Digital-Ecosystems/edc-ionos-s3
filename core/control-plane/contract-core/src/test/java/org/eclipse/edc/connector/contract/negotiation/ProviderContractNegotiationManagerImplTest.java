/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.negotiation;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ERROR;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderContractNegotiationManagerImplTest {

    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final SendRetryManager sendRetryManager = mock(SendRetryManager.class);
    private final ContractNegotiationListener listener = mock(ContractNegotiationListener.class);
    private ProviderContractNegotiationManagerImpl negotiationManager;

    @BeforeEach
    void setUp() {
        CommandQueue<ContractNegotiationCommand> queue = mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        CommandRunner<ContractNegotiationCommand> commandRunner = mock(CommandRunner.class);

        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);
        negotiationManager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock(Monitor.class))
                .commandQueue(queue)
                .commandRunner(commandRunner)
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();
    }

    @Test
    void requestedConfirmOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validateInitialOffer(token, contractOffer)).thenReturn(Result.success(contractOffer));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = negotiationManager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(n ->
                n.getState() == ContractNegotiationStates.CONFIRMING.code() &&
                        n.getCounterPartyId().equals(request.getConnectorId()) &&
                        n.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                        n.getProtocol().equals(request.getProtocol()) &&
                        n.getCorrelationId().equals(request.getCorrelationId()) &&
                        n.getContractOffers().size() == 1 &&
                        n.getLastContractOffer().equals(contractOffer)
        ));
        verify(validationService).validateInitialOffer(token, contractOffer);
    }

    @Test
    void requestedDeclineOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validateInitialOffer(token, contractOffer)).thenReturn(Result.failure("error"));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = negotiationManager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(n ->
                n.getState() == ContractNegotiationStates.DECLINING.code() &&
                        n.getCounterPartyId().equals(request.getConnectorId()) &&
                        n.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                        n.getProtocol().equals(request.getProtocol()) &&
                        n.getCorrelationId().equals(request.getCorrelationId()) &&
                        n.getContractOffers().size() == 1 &&
                        n.getLastContractOffer().equals(contractOffer)
        ));
        verify(validationService).validateInitialOffer(token, contractOffer);
    }

    @Test
    void declined() {
        var negotiation = createContractNegotiation();
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(store.findForCorrelationId(negotiation.getCorrelationId())).thenReturn(negotiation);
        var token = ClaimToken.Builder.newInstance().build();

        var result = negotiationManager.declined(token, negotiation.getCorrelationId());

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(n ->
                n.getState() == ContractNegotiationStates.DECLINED.code()
        ));
        verify(listener).declined(any());
    }

    @Test
    void providerOffering_shouldSendOfferAndTransitionOffered() {
        var negotiation = contractNegotiationBuilder().state(PROVIDER_OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(PROVIDER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == PROVIDER_OFFERED.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
            verify(listener).offered(any());
        });
    }

    @Test
    void providerOffering_shouldTransitionOfferingIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(PROVIDER_OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(PROVIDER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == PROVIDER_OFFERING.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
        });
    }

    @Test
    void providerOffering_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(PROVIDER_OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(PROVIDER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
            verify(listener).failed(any());
        });
    }

    @Test
    void declining_shouldSendRejectionAndTransitionDeclined() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == DECLINED.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
            verify(listener).declined(any());
        });
    }

    @Test
    void declining_shouldTransitionDecliningIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == DECLINING.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
        });
    }

    @Test
    void declining_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
            verify(listener).failed(any());
        });
    }

    @Test
    void confirming_shouldSendAgreementAndTransitionConfirmed() {
        var negotiation = contractNegotiationBuilder()
                .state(CONFIRMING.code())
                .contractOffer(contractOffer())
                .contractAgreement(contractAgreementBuilder().policy(Policy.Builder.newInstance().build()).build())
                .build();
        when(store.nextForState(eq(CONFIRMING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id("policyId").build());

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONFIRMED.code()));
            verify(dispatcherRegistry, only()).send(any(), isA(ContractAgreementRequest.class), any());
            verify(listener).confirmed(any());
        });
    }

    @Test
    void confirming_shouldSendNewAgreementAndTransitionConfirmed() {
        var negotiation = contractNegotiationBuilder()
                .state(CONFIRMING.code())
                .contractOffer(contractOffer())
                .build();
        when(store.nextForState(eq(CONFIRMING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONFIRMED.code()));
            verify(dispatcherRegistry, only()).send(any(), isA(ContractAgreementRequest.class), any());
            verify(listener).confirmed(any());
        });
    }

    @Test
    void confirming_shouldTransitionConfirmingIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONFIRMING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONFIRMING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONFIRMING.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
        });
    }

    @Test
    void confirming_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONFIRMING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONFIRMING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any(), any());
            verify(listener).failed(any());
        });
    }

    @NotNull
    private ContractNegotiation createContractNegotiation() {
        return contractNegotiationBuilder()
                .contractOffer(contractOffer())
                .build();
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId("correlationId")
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(400)
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractAgreement.Builder contractAgreementBuilder() {
        return ContractAgreement.Builder.newInstance()
                .id(ContractId.createContractId(UUID.randomUUID().toString()))
                .providerAgentId("any")
                .consumerAgentId("any")
                .assetId("default")
                .policy(Policy.Builder.newInstance().build());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ContractId.createContractId("1"))
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("assetId").build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now())
                .build();
    }

}
