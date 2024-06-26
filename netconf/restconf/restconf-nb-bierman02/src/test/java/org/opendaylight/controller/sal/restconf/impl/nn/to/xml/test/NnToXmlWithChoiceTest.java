/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeContext;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaAwareBuilders;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

public class NnToXmlWithChoiceTest extends AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static EffectiveModelContext schemaContext;

    public NnToXmlWithChoiceTest() {
        super(schemaContext, null);
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-xml/choice", schemaContext);
    }

    @Test
    public void cnSnToXmlWithYangChoice() throws Exception {
        NormalizedNodeContext normalizedNodeContext = prepareNNC("lf1",
                "String data1");
        OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        assertTrue(output.toString().contains("<lf1>String data1</lf1>"));

        normalizedNodeContext = prepareNNC("lf2", "String data2");
        output = new ByteArrayOutputStream();

        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                    mediaType, null, output);
        assertTrue(output.toString().contains("<lf2>String data2</lf2>"));
    }

    private static NormalizedNodeContext prepareNNC(final String name, final Object value) {

        final QName contQname = QName.create("module:with:choice", "2013-12-18",
                "cont");
        final QName lf = QName.create("module:with:choice", "2013-12-18", name);
        final QName choA = QName.create("module:with:choice", "2013-12-18", "choA");

        final DataSchemaNode contSchemaNode = schemaContext
                .getDataChildByName(contQname);
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        final DataSchemaNode choiceSchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(choA);
        assertTrue(choiceSchemaNode instanceof ChoiceSchemaNode);

        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> dataChoice = SchemaAwareBuilders
                .choiceBuilder((ChoiceSchemaNode) choiceSchemaNode);

        final var instanceLf = ControllerContext
                .findInstanceDataChildrenByName(
                        (DataNodeContainer) contSchemaNode, lf.getLocalName());
        final DataSchemaNode schemaLf = instanceLf.get(0).child;

        dataChoice.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf)
                .withValue(value).build());

        dataContainerNodeAttrBuilder.withChild(dataChoice.build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, contQname)),
                dataContainerNodeAttrBuilder.build());

        return testNormalizedNodeContext;
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
