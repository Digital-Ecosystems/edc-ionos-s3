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

package com.ionos.edc.extension.s3.schema;

import org.eclipse.edc.spi.types.domain.transfer.FlowType;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public interface IonosBucketSchema {
    String TYPE = "IonosS3";

    String REGION_ID = EDC_NAMESPACE + "region";
    String BUCKET_NAME = EDC_NAMESPACE + "bucketName";
    String BLOB_NAME = EDC_NAMESPACE + "blobName";
    String PATH = EDC_NAMESPACE + "path";
    String FILTER_INCLUDES = EDC_NAMESPACE + "filter.includes";
    String FILTER_EXCLUDES = EDC_NAMESPACE + "filter.excludes";

    String ENDPOINT = EDC_NAMESPACE + "endpoint";
    String MAX_FILES = EDC_NAMESPACE + "maxFiles";

    String PUSH_TRANSFER_TYPE = TYPE + "-" + FlowType.PUSH;
}
