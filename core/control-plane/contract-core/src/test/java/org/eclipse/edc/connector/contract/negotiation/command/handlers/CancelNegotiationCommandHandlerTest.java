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
 *
 */

package org.eclipse.edc.connector.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.command.CancelNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelNegotiationCommandHandlerTest {

    private CancelNegotiationCommandHandler commandHandler;

    private ContractNegotiationStore store;

    @BeforeEach
    public void setUp() {
        store = mock(ContractNegotiationStore.class);
        commandHandler = new CancelNegotiationCommandHandler(store);
    }

    @Test
    void handle_negotiationExists_cancelNegotiation() {
        var negotiationId = "test";
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .updatedAt(12345) // impossible time
                .protocol("test-protocol")
                .build();
        var originalTime = negotiation.getUpdatedAt();
        var command = new CancelNegotiationCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(negotiation);

        commandHandler.handle(command);

        assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
        assertThat(negotiation.getErrorDetail()).isEqualTo("Cancelled");
        assertThat(negotiation.getUpdatedAt()).isNotEqualTo(originalTime);
    }

    @Test
    void handle_negotiationDoesNotExist_throwEdcException() {
        var negotiationId = "test";
        var command = new CancelNegotiationCommand(negotiationId);

        when(store.find(negotiationId)).thenReturn(null);

        assertThatThrownBy(() -> commandHandler.handle(command))
                .isInstanceOf(EdcException.class)
                .hasMessage(format("Could not find ContractNegotiation with ID [%s]", negotiationId));
    }

    @Test
    void getType_returnType() {
        assertThat(commandHandler.getType()).isEqualTo(CancelNegotiationCommand.class);
    }

}
