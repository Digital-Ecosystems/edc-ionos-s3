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
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.transform.type.policy.PermissionToIdsPermissionTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionToIdsPermissionTransformerTest {

    private static final String TARGET = "https://target.com";
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    private PermissionToIdsPermissionTransformer transformer;

    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new PermissionToIdsPermissionTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulSimple() {
        Action edcAction = mock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = mock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = mock(de.fraunhofer.iais.eis.Constraint.class);
        Duty edcDuty = mock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsDuty = mock(de.fraunhofer.iais.eis.Duty.class);

        var permission = Permission.Builder.newInstance()
                .target(TARGET).assigner(ASSIGNER).assignee(ASSIGNEE)
                .constraint(edcConstraint)
                .duty(edcDuty)
                .action(edcAction)
                .build();

        var id = IdsId.Builder.newInstance().type(IdsType.PERMISSION).value(permission.hashCode()).build();

        when(context.transform(eq(edcAction), eq(de.fraunhofer.iais.eis.Action.class))).thenReturn(idsAction);
        when(context.transform(eq(edcConstraint), eq(de.fraunhofer.iais.eis.Constraint.class))).thenReturn(idsConstraint);
        when(context.transform(eq(edcDuty), eq(de.fraunhofer.iais.eis.Duty.class))).thenReturn(idsDuty);

        var result = transformer.transform(permission, context);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(id.toUri(), result.getId());
        Assertions.assertEquals(1, result.getAssigner().size());
        Assertions.assertEquals(ASSIGNER_URI, result.getAssigner().get(0));
        Assertions.assertEquals(1, result.getAssignee().size());
        Assertions.assertEquals(ASSIGNEE_URI, result.getAssignee().get(0));
        Assertions.assertEquals(1, result.getAction().size());
        Assertions.assertEquals(idsAction, result.getAction().get(0));
        Assertions.assertEquals(1, result.getConstraint().size());
        Assertions.assertEquals(idsConstraint, result.getConstraint().get(0));
        verify(context).transform(eq(edcAction), eq(de.fraunhofer.iais.eis.Action.class));
        verify(context).transform(eq(edcConstraint), eq(de.fraunhofer.iais.eis.Constraint.class));
        verify(context).transform(eq(edcDuty), eq(de.fraunhofer.iais.eis.Duty.class));
    }
}
