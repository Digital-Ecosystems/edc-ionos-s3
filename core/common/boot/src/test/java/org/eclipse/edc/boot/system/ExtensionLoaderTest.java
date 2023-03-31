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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.boot.system;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.eclipse.edc.boot.system.testextensions.ProviderDefaultServicesExtension;
import org.eclipse.edc.boot.system.testextensions.ProviderExtension;
import org.eclipse.edc.boot.util.CyclicDependencyException;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.monitor.MultiplexingMonitor;
import org.eclipse.edc.spi.system.MonitorExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.EdcInjectionException;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtensionLoaderTest {

    private final ServiceLocator serviceLocator = mock(ServiceLocator.class);
    private final ServiceExtension coreExtension = new TestCoreExtension();
    private final ExtensionLoader loader = new ExtensionLoader(serviceLocator);
    private final ServiceExtensionContext context = mock(ServiceExtensionContext.class);

    @BeforeAll
    public static void setup() {
        // mock default open telemetry
        GlobalOpenTelemetry.set(mock(OpenTelemetry.class));
    }

    @Test
    void loadMonitor_whenSingleMonitorExtension() {
        var mockedMonitor = mock(Monitor.class);
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mockedMonitor);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertEquals(mockedMonitor, monitor);
    }

    @Test
    void loadMonitor_whenMultipleMonitorExtensions() {
        var exts = new ArrayList<MonitorExtension>();
        exts.add(() -> mock(Monitor.class));
        exts.add(ConsoleMonitor::new);

        var monitor = ExtensionLoader.loadMonitor(exts);

        assertTrue(monitor instanceof MultiplexingMonitor);
    }

    @Test
    void loadMonitor_whenNoMonitorExtension() {
        var monitor = ExtensionLoader.loadMonitor(new ArrayList<>());

        assertTrue(monitor instanceof ConsoleMonitor);
    }

    @Test
    void selectOpenTelemetryImpl_whenNoOpenTelemetry() {
        var openTelemetry = ExtensionLoader.selectOpenTelemetryImpl(emptyList());

        assertThat(openTelemetry).isEqualTo(GlobalOpenTelemetry.get());
    }

    @Test
    void selectOpenTelemetryImpl_whenSingleOpenTelemetry() {
        var customOpenTelemetry = mock(OpenTelemetry.class);

        var openTelemetry = ExtensionLoader.selectOpenTelemetryImpl(List.of(customOpenTelemetry));

        assertThat(openTelemetry).isSameAs(customOpenTelemetry);
    }

    @Test
    void selectOpenTelemetryImpl_whenSeveralOpenTelemetry() {
        var customOpenTelemetry1 = mock(OpenTelemetry.class);
        var customOpenTelemetry2 = mock(OpenTelemetry.class);

        Exception thrown = assertThrows(IllegalStateException.class,
                () -> ExtensionLoader.selectOpenTelemetryImpl(List.of(customOpenTelemetry1, customOpenTelemetry2)));
        assertEquals(thrown.getMessage(), "Found 2 OpenTelemetry implementations. Please provide only one OpenTelemetry service provider.");
    }

    @Test
    @DisplayName("No dependencies between service extensions")
    void loadServiceExtensions_noDependencies() {

        var service1 = new ServiceExtension() {
        };
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, coreExtension));

        var list = loader.loadServiceExtensions(context);

        assertThat(list).hasSize(2);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).contains(service1);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Locating two service extensions for the same service class ")
    void loadServiceExtensions_whenMultipleServices() {
        var service1 = new ServiceExtension() {
        };
        var service2 = new ServiceExtension() {
        };

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, service2, coreExtension));

        var list = loader.loadServiceExtensions(context);
        assertThat(list).hasSize(3);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(service1, service2, coreExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A DEFAULT service extension depends on a PRIMORDIAL one")
    void loadServiceExtensions_withBackwardsDependency() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();
        var providing = new ProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(providing, depending, someExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providing, depending, someExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }


    @Test
    @DisplayName("A service extension has a dependency on another one of the same loading stage")
    void loadServiceExtensions_withEqualDependency() {
        var depending = new DependingExtension() {
        };
        var coreService = new SomeExtension() {
        };

        var thirdService = new ServiceExtension() {
        };

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, thirdService, coreService, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(coreService, depending, thirdService, coreExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Two service extensions have a circular dependency")
    void loadServiceExtensions_withCircularDependency() {
        var s1 = new TestProvidingExtension2();
        var s2 = new TestProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A service extension has an unsatisfied dependency")
    void loadServiceExtensions_dependencyNotSatisfied() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, someExtension, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(EdcException.class).hasMessageContaining("The following injected fields were not provided:\nField \"someService\" of type ");
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Services extensions are sorted by dependency order")
    void loadServiceExtensions_dependenciesAreSorted() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();


        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, providingExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Should throw exception when no core dependency found")
    void loadServiceExtensions_noCoreDependencyShouldThrowException() {
        var depending = new DependingExtension();
        var coreService = new SomeExtension();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(EdcException.class);
    }

    @Test
    @DisplayName("Requires annotation influences ordering")
    void loadServiceExtensions_withAnnotation() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();
        var annotatedExtension = new AnnotatedExtension();
        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, annotatedExtension, providingExtension, coreExtension));

        var services = loader.loadServiceExtensions(context);

        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending, annotatedExtension);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Requires annotation not satisfied")
    void loadServiceExtensions_withAnnotation_notSatisfied() {
        var annotatedExtension = new AnnotatedExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(annotatedExtension, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isNotInstanceOf(EdcInjectionException.class).isInstanceOf(EdcException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features work")
    void loadServiceExtensions_withMixedInjectAndAnnotation() {
        var providingExtension = new ProvidingExtension(); // provides SomeObject
        var anotherProvidingExt = new AnotherProvidingExtension(); //provides AnotherObject
        var mixedAnnotation = new MixedAnnotation();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(mixedAnnotation, providingExtension, coreExtension, anotherProvidingExt));

        var services = loader.loadServiceExtensions(context);
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, anotherProvidingExt, mixedAnnotation);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features introducing circular dependency")
    void loadServiceExtensions_withMixedInjectAndAnnotation_withCircDependency() {
        var s1 = new TestProvidingExtension3();
        var s2 = new TestProvidingExtension();

        when(serviceLocator.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> loader.loadServiceExtensions(context)).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocator).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("bootServiceExtensions - Should invoke default provider")
    void bootServiceExtensions_withSingleDefaultProvider() {
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var defaultProvider = (ProviderDefaultServicesExtension) Mockito.spy(TestFunctions.createProviderExtension(true));
        when(defaultProvider.testObject()).thenCallRealMethod();


        var context = new DefaultServiceExtensionContext(new TypeManager(), mock(Monitor.class), null, List.of());

        var list = TestFunctions.createInjectionContainers(TestFunctions.createList(defaultProvider, dependentExtension), context);

        ExtensionLoader.bootServiceExtensions(list, context);

        verify(defaultProvider, times(1)).testObject();
    }

    @Test
    @DisplayName("bootServiceExtensions - Should only invoke non-default provider")
    void bootServiceExtensions_withSingleDefaultProvider_andNonDefault() {
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var defaultProvider = (ProviderDefaultServicesExtension) Mockito.spy(TestFunctions.createProviderExtension(true));

        var nonDefaultProvider = (ProviderExtension) Mockito.spy(TestFunctions.createProviderExtension(false));
        when(nonDefaultProvider.testObject()).thenCallRealMethod();

        var context = new DefaultServiceExtensionContext(new TypeManager(), mock(Monitor.class), null, List.of());

        var list = TestFunctions.createInjectionContainers(TestFunctions.createList(defaultProvider, dependentExtension, nonDefaultProvider), context);

        ExtensionLoader.bootServiceExtensions(list, context);

        verify(defaultProvider, never()).testObject();
        verify(nonDefaultProvider, times(1)).testObject();
    }

    @Test
    @DisplayName("bootServiceExtensions - Should invoke non-default provider")
    void bootServiceExtensions_withNonDefault() {
        var dependentExtension = TestFunctions.createDependentExtension(true);

        var nonDefaultProvider = (ProviderExtension) Mockito.spy(TestFunctions.createProviderExtension(false));
        when(nonDefaultProvider.testObject()).thenCallRealMethod();

        var context = new DefaultServiceExtensionContext(new TypeManager(), mock(Monitor.class), null, List.of());

        var list = TestFunctions.createInjectionContainers(TestFunctions.createList(dependentExtension, nonDefaultProvider), context);

        ExtensionLoader.bootServiceExtensions(list, context);

        verify(nonDefaultProvider, times(1)).testObject();
    }

    @Test
    @DisplayName("bootServiceExtensions - Should invoke default provider for optional dependency")
    void bootServiceExtensions_withOptionalDependency_onlyDefault() {
        var dependentExtension = TestFunctions.createDependentExtension(false);

        var defaultProvider = (ProviderDefaultServicesExtension) Mockito.spy(TestFunctions.createProviderExtension(true));
        when(defaultProvider.testObject()).thenCallRealMethod();

        var context = new DefaultServiceExtensionContext(new TypeManager(), mock(Monitor.class), null, List.of());

        var list = TestFunctions.createInjectionContainers(TestFunctions.createList(dependentExtension, defaultProvider), context);

        ExtensionLoader.bootServiceExtensions(list, context);

        verify(defaultProvider, times(1)).testObject();
        assertThat(context.getService(TestObject.class)).isNotNull();
    }

    @Test
    @DisplayName("bootServiceExtensions - Should invoke non-default provider with optional dependency")
    void bootServiceExtensions_withOptionalDependency_defaultAndNonDefault() {
        var dependentExtension = TestFunctions.createDependentExtension(false);

        var defaultProvider = (ProviderDefaultServicesExtension) Mockito.spy(TestFunctions.createProviderExtension(true));
        when(defaultProvider.testObject()).thenCallRealMethod();

        var provider = (ProviderExtension) Mockito.spy(TestFunctions.createProviderExtension(false));
        when(provider.testObject()).thenCallRealMethod();

        var context = new DefaultServiceExtensionContext(new TypeManager(), mock(Monitor.class), null, List.of());

        var list = TestFunctions.createInjectionContainers(TestFunctions.createList(dependentExtension, defaultProvider, provider), context);

        ExtensionLoader.bootServiceExtensions(list, context);

        verify(defaultProvider, never()).testObject();
        verify(provider, times(1)).testObject();
        assertThat(context.getService(TestObject.class)).isNotNull();
    }

    @SafeVarargs
    private <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    private static class DependingExtension implements ServiceExtension {
        @Inject
        private SomeObject someService;
    }

    private static class SomeExtension implements ServiceExtension {
    }

    @Provides({ SomeObject.class })
    private static class ProvidingExtension implements ServiceExtension {
    }

    @Provides(AnotherObject.class)
    private static class AnotherProvidingExtension implements ServiceExtension {
    }

    @Provides({ SomeObject.class })
    private static class TestProvidingExtension implements ServiceExtension {
        @Inject
        AnotherObject obj;
    }

    @Provides({ AnotherObject.class })
    private static class TestProvidingExtension2 implements ServiceExtension {
        @Inject
        SomeObject obj;
    }

    @Provides({ AnotherObject.class })
    @Requires({ SomeObject.class })
    private static class TestProvidingExtension3 implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class AnnotatedExtension implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class MixedAnnotation implements ServiceExtension {
        @Inject
        private AnotherObject obj;
    }

    private static class SomeObject {
    }

    private static class AnotherObject {
    }

    @BaseExtension
    private static class TestCoreExtension implements ServiceExtension {

    }
}
