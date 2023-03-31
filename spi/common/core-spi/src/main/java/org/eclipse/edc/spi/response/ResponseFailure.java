/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.spi.response;

import org.eclipse.edc.spi.result.Failure;

import java.util.List;

/**
 * Sent when an operation has not completed successfully. The failure type is defined by {@link ResponseStatus}.
 */
public class ResponseFailure extends Failure {
    private final ResponseStatus status;

    public ResponseFailure(ResponseStatus status, List<String> messages) {
        super(messages);
        this.status = status;
    }

    public ResponseStatus status() {
        return status;
    }
}
