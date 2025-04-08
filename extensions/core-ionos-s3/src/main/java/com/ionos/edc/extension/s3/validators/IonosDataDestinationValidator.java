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

package com.ionos.edc.extension.s3.validators;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.Objects;
import java.util.stream.Stream;

import static com.ionos.edc.extension.s3.schema.IonosBucketSchema.BUCKET_NAME;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_KEY_NAME;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;

public class IonosDataDestinationValidator implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress dataAddress) {
        var violations = Stream.of(EDC_DATA_ADDRESS_TYPE_PROPERTY, EDC_DATA_ADDRESS_KEY_NAME, BUCKET_NAME)
                .map(it -> {
                    var value = dataAddress.getStringProperty(it);
                    if (value == null || value.isBlank()) {
                        return Violation.violation("'%s' is a mandatory attribute".formatted(it), it, value);
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
