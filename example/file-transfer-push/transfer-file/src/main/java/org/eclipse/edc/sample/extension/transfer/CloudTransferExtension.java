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
 import org.eclipse.edc.spi.asset.AssetIndex;
 import org.eclipse.edc.spi.system.ServiceExtension;
 import org.eclipse.edc.spi.system.ServiceExtensionContext;
 import org.eclipse.edc.spi.types.domain.DataAddress;
 import org.eclipse.edc.spi.types.domain.asset.Asset;

 import java.util.UUID;

 import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.*;
 import static org.eclipse.edc.spi.query.Criterion.criterion;

 public class CloudTransferExtension implements ServiceExtension {

     public static final String ASSET_BUCKET_NAME = "edc.ionos.examples.provider.bucketName";
     public static final String ASSET_BLOB_NAME = "edc.ionos.examples.provider.blobName";
     public static final String ASSET_FILTER_INCLUDES = "edc.ionos.examples.provider.filter.includes";
     public static final String ASSET_FILES_EXCLUDES = "edc.ionos.examples.provider.filter.excludes";

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
         var bucketName = context.getSetting(ASSET_BUCKET_NAME, "company1");
         var blobName = context.getSetting(ASSET_BLOB_NAME, "device1-data.csv");
         var filterIncludes = context.getSetting(ASSET_FILTER_INCLUDES, null);
         var filterExcludes = context.getSetting(ASSET_FILES_EXCLUDES, null);

         var policy = createPolicy();
         registerDataEntries(bucketName, blobName, filterIncludes, filterExcludes);
         registerContractDefinition(policy.getUid());
     }

     private PolicyDefinition createPolicy() {
         var usePermission = Permission.Builder.newInstance()
                 .action(Action.Builder.newInstance().type("USE").build())
                 .build();

         var policy = PolicyDefinition.Builder.newInstance()
                 .policy(Policy.Builder.newInstance().permission(usePermission).build())
                 .build();

         policyDefinitionStore.create(policy);
         return policy;
     }

     public void registerDataEntries(String bucketName, String blobName, String filterIncludes, String filterExcludes) {
         var dataAddressBuilder = DataAddress.Builder.newInstance().type("IonosS3")
                 .property(STORAGE_NAME, "s3-eu-central-1.ionoscloud.com")
                 .property(BUCKET_NAME, bucketName)
                 .property(BLOB_NAME, blobName)
                 .keyName(UUID.randomUUID().toString());
         if (filterIncludes != null) {
             dataAddressBuilder.property(FILTER_INCLUDES, filterIncludes);
         }
         if (filterExcludes != null) {
             dataAddressBuilder.property(FILTER_EXCLUDES, filterExcludes);
         }
         var dataAddress = dataAddressBuilder.build();

         var asset = Asset.Builder.newInstance()
                 .id("asset-1")
                 .dataAddress(dataAddress)
                 .build();
         assetIndex.create(asset);
     }
 
     public void registerContractDefinition(String policyId) {
         var contractDefinition = ContractDefinition.Builder.newInstance()
                 .id("contract-1")
                 .accessPolicyId(policyId)
                 .contractPolicyId(policyId)
                 .assetsSelectorCriterion(criterion(Asset.PROPERTY_ID, "=", "asset-1"))
                 .build();

         contractDefinitionStore.save(contractDefinition);
     }

 }
