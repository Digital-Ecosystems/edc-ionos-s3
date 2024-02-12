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

public interface IonosSettingsSchema {
    String IONOS_ACCESS_KEY = "edc.ionos.access.key";
    String IONOS_SECRET_KEY = "edc.ionos.secret.key";
    String IONOS_ENDPOINT = "edc.ionos.endpoint";
    String IONOS_TOKEN = "edc.ionos.token";
    int IONOS_MAX_FILES_DEFAULT = 1000;
    String IONOS_CHUNK_SIZE = "edc.ionos.chunkSize";
    int IONOS_CHUNK_SIZE_DEFAULT = 1024;
}