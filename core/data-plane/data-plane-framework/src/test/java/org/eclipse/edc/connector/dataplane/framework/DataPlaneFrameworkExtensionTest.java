/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.api.client.spi.transferprocess.NoopTransferProcessClient;
import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.dataplane.framework.e2e.EndToEndTest;
import org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneFrameworkExtensionTest {

    TransferService transferService1 = mock(TransferService.class);
    TransferService transferService2 = mock(TransferService.class);
    DataFlowRequest request = EndToEndTest.createRequest("1").build();

    @BeforeEach
    public void setUp(ServiceExtensionContext context) {
        when(transferService1.canHandle(request)).thenReturn(true);
        when(transferService2.canHandle(request)).thenReturn(true);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
        context.registerService(TransferProcessApiClient.class, new NoopTransferProcessClient());
    }

    @Test
    void initialize_registers_PipelineService(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);
        assertThat(context.getService(PipelineService.class)).isInstanceOf(PipelineServiceImpl.class);
    }

    @Test
    void initialize_registers_DataPlaneManager_withDefaultStrategy(ServiceExtensionContext context, ObjectFactory factory) {
        // Act
        validateRequest(context, factory);

        // Assert
        // The default TransferServiceSelectionStrategy will select the first service
        verify(transferService1).validate(request);
        verify(transferService2, never()).validate(request);
    }

    @Test
    void initialize_registers_DataPlaneManager_withInjectedStrategy(ServiceExtensionContext context, ObjectFactory factory) {
        // Arrange
        // Inject a custom TransferServiceSelectionStrategy that will select the second service
        context.registerService(TransferServiceSelectionStrategy.class,
                (request, services) -> services.skip(1).findFirst().orElse(null));

        // Act
        validateRequest(context, factory);

        // Assert
        verify(transferService2).validate(request);
        verify(transferService1, never()).validate(request);
    }

    private void validateRequest(ServiceExtensionContext context, ObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);
        var service = context.getService(TransferServiceRegistry.class);
        service.registerTransferService(transferService1);
        service.registerTransferService(transferService2);
        var m = context.getService(DataPlaneManager.class);
        m.validate(request);
    }
}
