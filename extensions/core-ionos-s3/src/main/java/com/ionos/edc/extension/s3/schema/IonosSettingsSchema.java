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
    String IONOS_REGION = "edc.ionos.endpoint.region";
    String IONOS_TOKEN = "edc.ionos.token";
    String IONOS_KEY_VALIDATION_ATTEMPTS = "edc.ionos.key.validation.attempts";
    String IONOS_KEY_VALIDATION_DELAY = "edc.ionos.key.validation.delay";
    String IONOS_MAX_FILES = "edc.ionos.max.files";

    String IONOS_REGION_DEFAULT = "de";
    int IONOS_MAX_FILES_DEFAULT = 1000;
    int IONOS_KEY_VALIDATION_ATTEMPTS_DEFAULT = 10;
    long IONOS_KEY_VALIDATION_DELAY_DEFAULT = 3000;
}