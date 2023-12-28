/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

 package org.eclipse.edc.sample.extension.transfer;

 import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
 import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
 import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
 import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
 import org.eclipse.edc.policy.model.Action;
 import org.eclipse.edc.policy.model.Permission;
 import org.eclipse.edc.policy.model.Policy;
 import org.eclipse.edc.runtime.metamodel.annotation.Inject;
 import org.eclipse.edc.spi.EdcException;
 import org.eclipse.edc.spi.asset.AssetIndex;
 import org.eclipse.edc.spi.system.ServiceExtension;
 import org.eclipse.edc.spi.system.ServiceExtensionContext;
 import org.eclipse.edc.spi.types.domain.DataAddress;
 import org.eclipse.edc.spi.types.domain.asset.Asset;

 import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.*;
 import static org.eclipse.edc.spi.query.Criterion.criterion;
 public class CloudTransferExtension implements ServiceExtension {
     @Inject
     private AssetIndex assetIndex;
     @Inject
     private PolicyDefinitionStore policyDefinitionStore;
     @Inject
     private ContractDefinitionStore contractDefinitionStore;
 
     @Override
     public String name() {
         return "Cloud-Based Transfer";
     }
 
     @Override
     public void initialize(ServiceExtensionContext context) {
         var policy = createPolicy();
         policyDefinitionStore.create(policy);
 
         registerDataEntries();
         registerContractDefinition(policy.getUid());
     }
 
     public void registerDataEntries() {
         try {
             var dataAddress = DataAddress.Builder.newInstance().type("IonosS3")
                     .property(STORAGE_NAME, "s3-eu-central-1.ionoscloud.com")
                     .property(BUCKET_NAME, "company1")
                     .property(BLOB_NAME, "device1-data.csv")
                     .keyName("device1").build();
             var asset = Asset.Builder.newInstance().id("1").dataAddress(dataAddress).build();
             assetIndex.create(asset);
         } catch (Exception e) {
             throw new EdcException("Error creating Data Entries", e);
         }
     }
 
     public void registerContractDefinition(String policyId) {
         var contractDefinition1 = ContractDefinition.Builder.newInstance().id("1").accessPolicyId(policyId)
                 .contractPolicyId(policyId)
                 .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", "2"))
                 .build();
 
         contractDefinitionStore.save(contractDefinition1);
     }
 
     private PolicyDefinition createPolicy() {
         var usePermission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build())
                 .build();
 
         return PolicyDefinition.Builder.newInstance()
                 .policy(Policy.Builder.newInstance().permission(usePermission).build()).build();
     }
 }
 