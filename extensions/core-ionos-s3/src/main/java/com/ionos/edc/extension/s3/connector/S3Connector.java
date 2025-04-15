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

package com.ionos.edc.extension.s3.connector;

import com.ionos.edc.extension.s3.api.S3AccessKey;
import com.ionos.edc.extension.s3.types.S3Object;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.io.ByteArrayInputStream;
import java.util.List;

@ExtensionPoint
public interface S3Connector {

    String getDefaultRegionId();

    int getMaxFiles();

    String getEndpoint(String regionId);

    boolean bucketExists(String bucketName, String regionId);

    void createBucket(String bucketName, String regionId);
    
    void uploadObject(String bucketName, String endpoint, String objectName, ByteArrayInputStream stream);

    ByteArrayInputStream getObject(String bucketName, String regionId, String objectName);

    ByteArrayInputStream getObject(String bucketName, String regionId, String objectName, long offset, long length);

    List<S3Object> listObjects(String bucketName, String regionId, String objectName);

    S3AccessKey createAccessKey();

    S3AccessKey retrieveAccessKey(String keyID);

    void deleteAccessKey(String keyID);
}
