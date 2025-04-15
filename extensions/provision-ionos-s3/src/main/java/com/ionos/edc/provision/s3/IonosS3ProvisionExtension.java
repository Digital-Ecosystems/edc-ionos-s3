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
import com.ionos.edc.provision.s3.resource.IonosS3ConsumerResourceDefinitionGenerator;
import com.ionos.edc.provision.s3.resource.IonosS3ProvisionedResource;
import com.ionos.edc.provision.s3.resource.IonosS3ResourceDefinition;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.TYPE;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_ATTEMPTS;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_ATTEMPTS_DEFAULT;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_DELAY;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_KEY_VALIDATION_DELAY_DEFAULT;

@Extension(value = IonosS3ProvisionExtension.NAME)
public class IonosS3ProvisionExtension implements ServiceExtension {

    public static final String NAME = "Ionos Provision";

    private static final String LOG_CONTEXT = "IonosProvisionExtension";

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Inject(required = false)
    private S3Connector s3Connector;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var contextMonitor = monitor.withPrefix("IonosS3ProvisionExtension");

        if (s3Connector == null) {
            contextMonitor.warning("IONOS S3 Connector not loaded, disabling provision extension. You cannot initiate push transfers with dataDestination of type " + TYPE);
            return;
        }

        contextMonitor.debug("Loading configurations");
        var keyValidationAttempts =  context.getSetting(IONOS_KEY_VALIDATION_ATTEMPTS, IONOS_KEY_VALIDATION_ATTEMPTS_DEFAULT);
        var keyValidationDelay =  context.getSetting(IONOS_KEY_VALIDATION_DELAY, IONOS_KEY_VALIDATION_DELAY_DEFAULT);

        contextMonitor.debug("Initializing provisioner");
        var provisionManager = context.getService(ProvisionManager.class);
        var retryPolicy = context.getService(RetryPolicy.class);

        var s3BucketProvisioner = new IonosS3Provisioner(monitor, retryPolicy, s3Connector, keyValidationAttempts, keyValidationDelay);
        provisionManager.register(s3BucketProvisioner);

        contextMonitor.debug("Registering manifest generators");
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerGenerator(new IonosS3ConsumerResourceDefinitionGenerator());

        contextMonitor.debug("Registering types");
        registerTypes(typeManager);

        contextMonitor.info("Provision extension initialized !");
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(IonosS3ProvisionedResource.class, IonosS3ResourceDefinition.class, IonosToken.class);
    }
}
