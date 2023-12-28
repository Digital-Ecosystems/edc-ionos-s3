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

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Objects;
import java.util.stream.Stream;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.*;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class IonosSourceDataAddressValidationRule implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        var violations = Stream.of(STORAGE_NAME, BUCKET_NAME, BLOB_NAME)
                .map(it -> {
                    var value = dataAddress.getStringProperty(it);
                    if (value == null || value.isBlank()) {
                        return violation("'%s' is a mandatory attribute".formatted(it), it, value);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violations);
    }
}