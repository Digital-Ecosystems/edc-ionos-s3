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
 *       Fraunhofer Insitute for Software and Systems Engineering
 *
 */

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintBuilder;
import org.eclipse.edc.protocol.ids.transform.type.policy.ConstraintFromIdsConstraintTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConstraintFromIdsConstraintTransformerTest {

    private static final URI CONSTRAINT_ID = URI.create("https://constraint.com");

    private ConstraintFromIdsConstraintTransformer transformer;

    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ConstraintFromIdsConstraintTransformer();
        idsConstraint = new IdsConstraintBuilder(CONSTRAINT_ID)
                .leftOperand("PURPOSE")
                .operator(BinaryOperator.EQ)
                .rightOperand(new RdfResource("hello"))
                .build();
        context = mock(TransformerContext.class);
    }

    @Test
    void testTransform() {
        var expectedLeftExpression = new LiteralExpression("left");
        var expectedRightExpression = new LiteralExpression("right");

        when(context.transform(any(String.class), eq(Expression.class)))
                .thenReturn(expectedLeftExpression);
        when(context.transform(any(BinaryOperator.class), eq(Operator.class)))
                .thenReturn(Operator.EQ);
        when(context.transform(any(RdfResource.class), eq(Expression.class)))
                .thenReturn(expectedRightExpression);

        var result = (AtomicConstraint) transformer.transform(idsConstraint, context);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getLeftExpression());
        Assertions.assertEquals(result.getLeftExpression(), expectedLeftExpression);
        Assertions.assertNotNull(result.getOperator());
        Assertions.assertNotNull(result.getRightExpression());
        Assertions.assertEquals(result.getRightExpression(), expectedRightExpression);
        verify(context).transform(any(String.class), eq(Expression.class));
        verify(context).transform(any(BinaryOperator.class), eq(Operator.class));
        verify(context).transform(any(RdfResource.class), eq(Expression.class));
    }

}
