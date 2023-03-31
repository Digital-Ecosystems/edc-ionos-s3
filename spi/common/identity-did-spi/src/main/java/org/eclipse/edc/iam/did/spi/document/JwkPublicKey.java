/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.did.spi.document;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = EllipticCurvePublicKey.class)
public interface JwkPublicKey {
    String getKty();
}
