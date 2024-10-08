/*
 *  Copyright (c) 2023 IONOS
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

package com.ionos.edc.extension.s3.types;

public record S3Object(String objectName, long size) {

    public boolean isDirectory() {
        return objectName.endsWith("/");
    }

    public boolean isRootObject(String blobName) {
        return (objectName.equals(blobName) ||  objectName.equals(blobName + "/"));
    }

    public String shortObjectName(String blobName) {
        if (isRootObject(blobName))
            return objectName;

        var shortObjectName = objectName.replaceFirst(blobName, "");

        if (shortObjectName.indexOf("/") == 0)
            return shortObjectName.replaceFirst("/", "");
        else
            return shortObjectName;
    }

}