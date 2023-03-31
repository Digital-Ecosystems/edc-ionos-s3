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

package org.eclipse.edc.protocol.ids.spi.transform;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

/**
 * Dispatches to {@link IdsTypeTransformer}s to bidirectionally convert between IDS and EDC types.
 * This is a marker interface to restrict to ids transformer types
 */
@ExtensionPoint
public interface IdsTransformerRegistry extends TypeTransformerRegistry<IdsTypeTransformer<?, ?>> {

}
