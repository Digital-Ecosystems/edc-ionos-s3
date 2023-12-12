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

package com.ionos.edc.dataplane.ionos.s3.validation;

import org.eclipse.edc.connector.dataplane.util.validation.CompositeValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.EmptyValueValidationRule;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Map;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.BLOB_NAME;
import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.BUCKET_NAME;
import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.STORAGE_NAME;
import static org.eclipse.edc.spi.types.domain.DataAddress.SIMPLE_KEY_NAME;


public class IonosSourceDataAddressValidationRule implements ValidationRule<DataAddress> {

    private final CompositeValidationRule<Map<String, String>> mandatoryPropertyValidationRule  = new CompositeValidationRule<>(
            List.of(
                    new EmptyValueValidationRule(SIMPLE_KEY_NAME),
                    new EmptyValueValidationRule(STORAGE_NAME),
                    new EmptyValueValidationRule(BUCKET_NAME),
                    new EmptyValueValidationRule(BLOB_NAME)
            )
    );

    @Override
    public Result<Void> apply(DataAddress dataAddress) {
        return mandatoryPropertyValidationRule.apply(dataAddress.getProperties());
    }
}
