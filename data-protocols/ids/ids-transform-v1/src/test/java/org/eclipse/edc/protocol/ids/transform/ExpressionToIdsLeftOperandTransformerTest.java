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

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.protocol.ids.transform.type.policy.ExpressionToIdsLeftOperandTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class ExpressionToIdsLeftOperandTransformerTest {

    private ExpressionToIdsLeftOperandTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ExpressionToIdsLeftOperandTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulMap() {
        var expression = new LiteralExpression("absoluteSpatialPosition");

        var result = transformer.transform(expression, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("absoluteSpatialPosition", result);
    }

}
