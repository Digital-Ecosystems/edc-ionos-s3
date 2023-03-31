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

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.transform.type.policy.ProhibitionToIdsProhibitionTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProhibitionToIdsProhibitionTransformerTest {

    private static final String TARGET = "https://target.com";
    private static final String ASSIGNER = "https://assigner.com";
    private static final String ASSIGNEE = "https://assignee.com";

    private ProhibitionToIdsProhibitionTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ProhibitionToIdsProhibitionTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulSimple() {
        Action edcAction = mock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = mock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = mock(de.fraunhofer.iais.eis.Constraint.class);
        var prohibition = Prohibition.Builder.newInstance()
                .target(TARGET).assignee(ASSIGNEE).assigner(ASSIGNER)
                .constraint(edcConstraint).action(edcAction)
                .build();
        when(context.transform(eq(edcAction), eq(de.fraunhofer.iais.eis.Action.class))).thenReturn(idsAction);
        when(context.transform(eq(edcConstraint), eq(de.fraunhofer.iais.eis.Constraint.class))).thenReturn(idsConstraint);

        var result = transformer.transform(prohibition, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getAssigner().size());
        Assertions.assertEquals(1, result.getAssignee().size());
        Assertions.assertEquals(1, result.getAction().size());
        Assertions.assertEquals(idsAction, result.getAction().get(0));
        Assertions.assertEquals(1, result.getConstraint().size());
        Assertions.assertEquals(idsConstraint, result.getConstraint().get(0));
        verify(context).transform(eq(edcAction), eq(de.fraunhofer.iais.eis.Action.class));
        verify(context).transform(eq(edcConstraint), eq(de.fraunhofer.iais.eis.Constraint.class));
    }
}
