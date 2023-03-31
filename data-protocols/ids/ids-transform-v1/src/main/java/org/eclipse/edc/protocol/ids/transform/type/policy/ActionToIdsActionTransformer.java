/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial API and Implementation
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionToIdsActionTransformer implements IdsTypeTransformer<Action, de.fraunhofer.iais.eis.Action> {

    @Override
    public Class<Action> getInputType() {
        return Action.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Action> getOutputType() {
        return de.fraunhofer.iais.eis.Action.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Action transform(@NotNull Action object, @NotNull TransformerContext context) {
        var type = object.getType();

        de.fraunhofer.iais.eis.Action idsAction;
        try {
            idsAction = de.fraunhofer.iais.eis.Action.valueOf(type);
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("Encountered undefined action type: %s", type));
            idsAction = null;
        }

        return idsAction;
    }
}
