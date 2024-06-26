/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class InstanceIdentifierTypeLeafTest {

    @Test
    public void stringToInstanceIdentifierTest() throws Exception {
        final EffectiveModelContext schemaContext =
                YangParserTestUtils.parseYangFiles(TestRestconfUtils.loadFiles("/instanceidentifier"));
        ControllerContext controllerContext = TestRestconfUtils.newControllerContext(schemaContext);
        final InstanceIdentifierContext instanceIdentifier =
                controllerContext.toInstanceIdentifier(
                        "/iid-value-module:cont-iid/iid-list/%2Fiid-value-module%3Acont-iid%2Fiid-value-module%3A"
                                + "values-iid%5Biid-value-module:value-iid='value'%5D");
        final YangInstanceIdentifier yiD = instanceIdentifier.getInstanceIdentifier();
        assertNotNull(yiD);
        final PathArgument lastPathArgument = yiD.getLastPathArgument();
        assertTrue(lastPathArgument.getNodeType().getNamespace().toString().equals("iid:value:module"));
        assertTrue(lastPathArgument.getNodeType().getLocalName().equals("iid-list"));

        final NodeIdentifierWithPredicates list = (NodeIdentifierWithPredicates) lastPathArgument;
        final YangInstanceIdentifier value = (YangInstanceIdentifier) list.getValue(
            QName.create(lastPathArgument.getNodeType(), "iid-leaf"));
        final PathArgument lastPathArgumentOfValue = value.getLastPathArgument();
        assertTrue(lastPathArgumentOfValue.getNodeType().getNamespace().toString().equals("iid:value:module"));
        assertTrue(lastPathArgumentOfValue.getNodeType().getLocalName().equals("values-iid"));

        final NodeIdentifierWithPredicates valueList = (NodeIdentifierWithPredicates) lastPathArgumentOfValue;
        final String valueIid = (String) valueList.getValue(
                QName.create(lastPathArgumentOfValue.getNodeType(), "value-iid"));
        assertEquals("value", valueIid);
    }

}
