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

package com.ionos.edc.extension.s3.api;

import com.ionos.edc.extension.s3.connector.ionosapi.S3AccessKey;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.io.ByteArrayInputStream;
import java.util.List;

@ExtensionPoint
public interface S3ConnectorApi {

    void createBucket(String bucketName);

    boolean bucketExists(String bucketName);
    
    void uploadObject(String bucketName, String objectName, ByteArrayInputStream stream);

    ByteArrayInputStream getObject(String bucketName, String objectName);

    ByteArrayInputStream getObject(String bucketName, String objectName, long offset, long length);

    List<S3Object> listObjects(String bucketName, String objectName);

    S3AccessKey createAccessKey();

    S3AccessKey retrieveAccessKey(String keyID);

    void deleteAccessKey(String keyID);

}
