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
 *       Daimler TSS GmbH - Initial Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import de.fraunhofer.iais.eis.BinaryOperator;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class OperatorFromIdsBinaryOperatorTransformer implements IdsTypeTransformer<BinaryOperator, Operator> {
    private static final Map<BinaryOperator, Operator> MAPPING = new HashMap<>() {
        {
            put(BinaryOperator.EQUALS, Operator.EQ);
            put(BinaryOperator.EQ, Operator.EQ);
            put(BinaryOperator.GT, Operator.GT);
            put(BinaryOperator.GTEQ, Operator.GEQ);
            put(BinaryOperator.LT, Operator.LT);
            put(BinaryOperator.LTEQ, Operator.LEQ);
            put(BinaryOperator.IN, Operator.IN);
        }
    };

    @Override
    public Class<BinaryOperator> getInputType() {
        return BinaryOperator.class;
    }

    @Override
    public Class<Operator> getOutputType() {
        return Operator.class;
    }

    @Override
    public @Nullable Operator transform(@NotNull BinaryOperator object, @NotNull TransformerContext context) {
        var operator = MAPPING.get(object);
        if (operator == null) {
            context.reportProblem(String.format("cannot transform IDS operator %s", object));
            return null;
        }

        return operator;
    }
}
