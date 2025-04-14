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

package com.ionos.edc.dataplane.ionos.s3;

import com.ionos.edc.dataplane.ionos.s3.datasink.IonosDataSinkFactory;
import com.ionos.edc.dataplane.ionos.s3.datasource.IonosDataSourceFactory;
import com.ionos.edc.extension.s3.connector.S3Connector;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.TYPE;

@Extension(value = IonosS3DataPlaneExtension.NAME)
public class IonosS3DataPlaneExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Ionos S3 Storage";
    @Inject
    private PipelineService pipelineService;
    
    @Inject(required = false)
    private S3Connector s3Connector;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;
    
    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var contextMonitor = monitor.withPrefix("IonosS3DataPlaneExtension");

        if (s3Connector == null) {
            contextMonitor.warning("IONOS S3 Connector not loaded, disabling dataSource factory. You cannot receive push transfers to Assets with dataAddress of type " + TYPE);
        } else {
            contextMonitor.debug("Initializing dataSource factory");

            var dataSourceFactory = new IonosDataSourceFactory(s3Connector);
            pipelineService.registerFactory(dataSourceFactory);
        }

        contextMonitor.debug("Initializing dataSink factory");
        var dataSinkFactory = new IonosDataSinkFactory(executorContainer.getExecutorService(), monitor, vault, typeManager);
        pipelineService.registerFactory(dataSinkFactory);

        contextMonitor.info("DataPlane extension initialized !");
    }
}
