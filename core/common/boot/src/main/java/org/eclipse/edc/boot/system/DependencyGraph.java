/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.util.CyclicDependencyException;
import org.eclipse.edc.boot.util.TopologicalSort;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.EdcInjectionException;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.system.injection.InjectionPoint;
import org.eclipse.edc.spi.system.injection.InjectionPointScanner;
import org.eclipse.edc.spi.system.injection.ProviderMethod;
import org.eclipse.edc.spi.system.injection.ProviderMethodScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;


/**
 * Converts an unsorted list of {@link ServiceExtension} instances into a directed graph based on dependency direction, i.e.
 * which extension depends on which other extension.
 */
public class DependencyGraph {
    private final InjectionPointScanner injectionPointScanner = new InjectionPointScanner();
    private final ServiceExtensionContext context;

    public DependencyGraph(ServiceExtensionContext context) {
        this.context = context;
    }

    /**
     * Sorts all {@link ServiceExtension} implementors, that were found on the classpath, according to their dependencies.
     * Depending Extensions (i.e. those who <em>express</em> a dependency) are sorted first, providing extensions (i.e. those
     * who provide a dependency) are sorted last.
     *
     * @param loadedExtensions A list of {@link ServiceExtension} instances that were picked up by the {@link ServiceLocator}
     * @return A list of {@link InjectionContainer}s that are sorted topologically according to their dependencies.
     * @throws CyclicDependencyException when there is a dependency cycle
     * @see TopologicalSort
     * @see InjectionContainer
     */
    public List<InjectionContainer<ServiceExtension>> of(List<ServiceExtension> loadedExtensions) {
        var extensions = sortByType(loadedExtensions);
        var dependencyMap = createDependencyMap(extensions);

        var sort = new TopologicalSort<ServiceExtension>();

        // check if all injected fields are satisfied, collect missing ones and throw exception otherwise
        var unsatisfiedInjectionPoints = new ArrayList<InjectionPoint<ServiceExtension>>();
        var injectionPoints = extensions.stream()
                .flatMap(ext -> getInjectedFields(ext).stream()
                        .peek(injectionPoint -> {
                            if (!canResolve(dependencyMap, injectionPoint.getType())) {
                                if (injectionPoint.isRequired()) {
                                    unsatisfiedInjectionPoints.add(injectionPoint);
                                }
                            } else {
                                // get() would return null, if the feature is already in the context's service list
                                ofNullable(dependencyMap.get(injectionPoint.getType()))
                                        .ifPresent(l -> l.stream()
                                                .filter(d -> !Objects.equals(d, ext)) // remove dependencies onto oneself
                                                .forEach(provider -> sort.addDependency(ext, provider)));
                            }
                        })
                )
                .collect(Collectors.toList());

        //throw an exception if still unsatisfied links
        if (!unsatisfiedInjectionPoints.isEmpty()) {
            var string = "The following injected fields were not provided:\n";
            string += unsatisfiedInjectionPoints.stream().map(InjectionPoint::toString).collect(Collectors.joining("\n"));
            throw new EdcInjectionException(string);
        }

        //check that all the @Required features are there
        var unsatisfiedRequirements = new ArrayList<String>();
        extensions.forEach(ext -> {
            var features = getRequiredFeatures(ext.getClass());
            features.forEach(feature -> {
                var dependencies = dependencyMap.get(feature);
                if (dependencies == null) {
                    unsatisfiedRequirements.add(feature.getName());
                } else {
                    dependencies.forEach(dependency -> sort.addDependency(ext, dependency));
                }
            });
        });

        if (!unsatisfiedRequirements.isEmpty()) {
            var string = String.format("The following @Require'd features were not provided: [%s]", String.join(", ", unsatisfiedRequirements));
            throw new EdcException(string);
        }

        sort.sort(extensions);

        // todo: should the list of InjectionContainers be generated directly by the flatmap?
        // convert the sorted list of extensions into an equally sorted list of InjectionContainers
        return extensions.stream()
                .map(se -> new InjectionContainer<>(se, injectionPoints.stream().filter(ip -> ip.getInstance() == se).collect(Collectors.toSet())))
                .collect(Collectors.toList());
    }

    private boolean canResolve(Map<Class<?>, List<ServiceExtension>> dependencyMap, Class<?> featureName) {
        var providers = dependencyMap.get(featureName);
        if (providers != null) {
            return true;
        } else {
            // attempt to interpret the feature name as class name, instantiate it and see if the context has that service
            return context.hasService(featureName);
        }
    }

    private Map<Class<?>, List<ServiceExtension>> createDependencyMap(List<ServiceExtension> extensions) {
        Map<Class<?>, List<ServiceExtension>> dependencyMap = new HashMap<>();
        extensions.forEach(ext -> getDefaultProvidedFeatures(ext).forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));
        extensions.forEach(ext -> getProvidedFeatures(ext).forEach(feature -> dependencyMap.computeIfAbsent(feature, k -> new ArrayList<>()).add(ext)));
        return dependencyMap;
    }

    private Set<Class<?>> getRequiredFeatures(Class<?> clazz) {
        var requiresAnnotation = clazz.getAnnotation(Requires.class);
        if (requiresAnnotation != null) {
            var features = requiresAnnotation.value();
            return Stream.of(features).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * Obtains all features a specific extension requires as strings
     */
    private Set<Class<?>> getProvidedFeatures(ServiceExtension ext) {
        var allProvides = new HashSet<Class<?>>();

        // check all @Provides
        var providesAnnotation = ext.getClass().getAnnotation(Provides.class);
        if (providesAnnotation != null) {
            var featureStrings = Arrays.stream(providesAnnotation.value()).collect(Collectors.toSet());
            allProvides.addAll(featureStrings);
        }
        // check all @Provider methods
        allProvides.addAll(new ProviderMethodScanner(ext).nonDefaultProviders().stream().map(ProviderMethod::getReturnType).collect(Collectors.toSet()));
        return allProvides;
    }

    private Set<Class<?>> getDefaultProvidedFeatures(ServiceExtension ext) {
        return new ProviderMethodScanner(ext).defaultProviders().stream()
                .map(ProviderMethod::getReturnType)
                .collect(Collectors.toSet());
    }

    /**
     * Handles core-, transfer- and contract-extensions and inserts them at the beginning of the list so that
     * explicit @Requires annotations are not necessary
     */
    private List<ServiceExtension> sortByType(List<ServiceExtension> loadedExtensions) {
        var baseDependencies = loadedExtensions.stream().filter(e -> e.getClass().getAnnotation(BaseExtension.class) != null).collect(Collectors.toList());
        if (baseDependencies.isEmpty()) {
            throw new EdcException("No base dependencies were found on the classpath. Please add the \"core:common:connector-core\" module to your classpath!");
        }

        return loadedExtensions.stream().sorted(new ServiceExtensionComparator()).collect(Collectors.toList());
    }

    /**
     * Obtains all features a specific extension provides as strings
     */
    private Set<InjectionPoint<ServiceExtension>> getInjectedFields(ServiceExtension ext) {
        // initialize with legacy list
        return injectionPointScanner.getInjectionPoints(ext);
    }

    private static class ServiceExtensionComparator implements Comparator<ServiceExtension> {
        @Override
        public int compare(ServiceExtension o1, ServiceExtension o2) {
            return orderFor(o1.getClass()).compareTo(orderFor(o2.getClass()));
        }

        private Integer orderFor(Class<? extends ServiceExtension> class1) {
            return class1.getAnnotation(BaseExtension.class) != null
                    ? 0 : class1.getAnnotation(CoreExtension.class) != null
                    ? 1 : 2;
        }
    }
}
