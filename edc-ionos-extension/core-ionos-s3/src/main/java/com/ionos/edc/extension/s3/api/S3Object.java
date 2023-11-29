package com.ionos.edc.extension.s3.api;

public record S3Object(String objectName, long size) {

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
