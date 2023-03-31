/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.http.webhook;

import org.eclipse.edc.connector.transfer.spi.types.SecretToken;

/**
 * Implementation of the {@link SecretToken} that is based on a simple String.
 * This could be an API key, a JWT or any other format serialized to String (e.g. JSON).
 */
public class SimpleSecretToken implements SecretToken {

    private final String token;

    public SimpleSecretToken(String base64SerializedToken) {
        token = base64SerializedToken;
    }

    @Override
    public long getExpiration() {
        return 0;
    }

}
