/*
 *  Copyright (c) 2022 IONOS
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      IONOS
 *
 */

package com.ionos.edc.provision.s3;

import com.ionos.edc.extension.s3.connector.S3Connector;
import com.ionos.edc.extension.s3.types.IonosToken;
import com.ionos.edc.provision.s3.bucket.IonosS3ConsumerResourceDefinitionGenerator;
import com.ionos.edc.provision.s3.bucket.IonosS3ProvisionedResource;
import com.ionos.edc.provision.s3.bucket.IonosS3Provisioner;
import com.ionos.edc.provision.s3.bucket.IonosS3ResourceDefinition;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_ATTEMPTS;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_ATTEMPTS_DEFAULT;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_DELAY;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_DELAY_DEFAULT;

@Extension(value = IonosProvisionExtension.NAME)
public class IonosProvisionExtension implements ServiceExtension {

    public static final String NAME = "Ionos Provision";

    @Inject
    private Vault vault;
    @Inject
    private TypeManager typeManager;
    @Inject
    private S3Connector clientApi;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var keyValidationAttempts =  context.getSetting(IONOS_KEY_VALIDATION_ATTEMPTS, IONOS_KEY_VALIDATION_ATTEMPTS_DEFAULT);
        var keyValidationDelay =  context.getSetting(IONOS_KEY_VALIDATION_DELAY, IONOS_KEY_VALIDATION_DELAY_DEFAULT);

        monitor.debug("IonosProvisionExtension" + "provisionManager");
        var provisionManager = context.getService(ProvisionManager.class);

        monitor.debug("IonosProvisionExtension" + "retryPolicy");
        var retryPolicy = context.getService(RetryPolicy.class);

        monitor.debug("IonosProvisionExtension" + "s3BucketProvisioner");
        var s3BucketProvisioner = new IonosS3Provisioner(monitor, retryPolicy, clientApi, keyValidationAttempts, keyValidationDelay);
        provisionManager.register(s3BucketProvisioner);

        monitor.debug("IonosProvisionExtension" + "manifestGenerator");
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerGenerator(new IonosS3ConsumerResourceDefinitionGenerator());

        monitor.debug("IonosProvisionExtension" + "registerTypes");
        registerTypes(typeManager);
    }

    @Override
    public void shutdown() {
        ServiceExtension.super.shutdown();
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(IonosS3ProvisionedResource.class, IonosS3ResourceDefinition.class, IonosToken.class);
    }
}
