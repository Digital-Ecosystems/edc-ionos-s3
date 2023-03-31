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

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.model.ContractNegotiationDocument;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;

import java.time.Clock;


@Provides({ ContractNegotiationStore.class })
@Extension(value = CosmosContractNegotiationStoreExtension.NAME)
public class CosmosContractNegotiationStoreExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB ContractNegotiation Store";
    @Inject
    private Vault vault;

    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new CosmosContractNegotiationStoreConfig(context);

        var cosmosDbApi = new CosmosDbApiImpl(configuration, clientProvider.createClient(vault, configuration));
        var store = new CosmosContractNegotiationStore(cosmosDbApi, context.getTypeManager(), (RetryPolicy<Object>) context.getService(RetryPolicy.class), configuration.getPartitionKey(), clock);
        context.registerService(ContractNegotiationStore.class, store);

        context.getTypeManager().registerTypes(ContractNegotiationDocument.class);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        if (context.getSetting(configuration.allowSprocAutoUploadSetting(), true)) {
            cosmosDbApi.uploadStoredProcedure("nextForState");
            cosmosDbApi.uploadStoredProcedure("lease");
        }
    }

}

