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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import com.ionos.edc.extension.s3.connector.ionosapi.TemporaryKey;

import java.io.InputStream;
import java.util.List;

@ExtensionPoint
public interface S3ConnectorApi {

    void createBucket(String bucketName);

    boolean bucketExists(String bucketName);
    
    void uploadObject(String bucketName, String objectName, InputStream  stream);
    
    InputStream getObject(String bucketName, String objectName);

    List<String> listObjects(String bucketName, String objectName);

    TemporaryKey createTemporaryKey();
    
    void deleteTemporaryKey(String accessKey);

}
