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

package com.ionos.edc.extension.s3.configuration;

import com.ionos.edc.extension.s3.api.S3ConnectorApi;
import com.ionos.edc.extension.s3.api.S3ConnectorApiImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides(S3ConnectorApi.class)
@Extension(value = S3CoreExtension.NAME)
public class S3CoreExtension implements ServiceExtension {
    public static final String NAME = "IonosS3";
    @Setting
    private static final String IONOS_ACCESS_KEY = "edc.ionos.access.key";
    @Setting
    private static final String IONOS_SECRET_KEY = "edc.ionos.secret.key";
    @Setting
    private static final String IONOS_ENDPOINT = "edc.ionos.endpoint";

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
        String accessKey = context.getSetting(IONOS_ACCESS_KEY, "ACCESS");
        String secretKey = context.getSetting(IONOS_SECRET_KEY, "SECRET");
        String endPoint = context.getSetting(IONOS_ENDPOINT, "URL");
        var s3Api = new S3ConnectorApiImpl(endPoint, accessKey, secretKey);
        context.registerService(S3ConnectorApi.class, s3Api);
    }

}
