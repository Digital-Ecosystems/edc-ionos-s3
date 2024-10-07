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

package com.ionos.edc.extension.s3;

import com.ionos.edc.extension.s3.connector.S3Connector;
import com.ionos.edc.extension.s3.connector.S3ConnectorImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_ACCESS_KEY;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_REGION;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_SECRET_KEY;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_TOKEN;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_MAX_FILES;
import static com.ionos.edc.extension.s3.schema.IonosSettingsSchema.IONOS_MAX_FILES_DEFAULT;

@Provides(S3Connector.class)
@Extension(value = S3CoreExtension.NAME)
public class S3CoreExtension implements ServiceExtension {

    public static final String NAME = "IonosS3";
    
    @Inject
    private Vault vault;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var accessKey = vault.resolveSecret(IONOS_ACCESS_KEY);
        var secretKey = vault.resolveSecret(IONOS_SECRET_KEY);
        var region = vault.resolveSecret(IONOS_REGION);
        var token =  vault.resolveSecret(IONOS_TOKEN);

        if(accessKey == null || secretKey  == null || region ==null || token == null) {
              monitor.warning("Couldn't connect or the vault didn't return values, falling back to ConfigMap Configuration");
        	  accessKey = context.getSetting(IONOS_ACCESS_KEY, null);
              secretKey = context.getSetting(IONOS_SECRET_KEY, null);
              region = context.getSetting(IONOS_REGION, null);
              token = context.getSetting(IONOS_TOKEN, null);
        }

        var maxFiles =  Integer.valueOf(context.getSetting(IONOS_MAX_FILES, IONOS_MAX_FILES_DEFAULT));

        var s3Connector = new S3ConnectorImpl(region, accessKey, secretKey, token, maxFiles);
        context.registerService(S3Connector.class, s3Connector);
    }
}
