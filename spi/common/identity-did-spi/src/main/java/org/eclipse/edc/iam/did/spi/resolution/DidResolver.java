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

package org.eclipse.edc.iam.did.spi.resolution;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves a DID against an external resolver service.
 */
public interface DidResolver {

    /**
     * Returns the DID method this resolver supports.
     */
    @NotNull
    String getMethod();

    /**
     * Resolves the DID document.
     */
    @NotNull
    Result<DidDocument> resolve(String didKey);

}
