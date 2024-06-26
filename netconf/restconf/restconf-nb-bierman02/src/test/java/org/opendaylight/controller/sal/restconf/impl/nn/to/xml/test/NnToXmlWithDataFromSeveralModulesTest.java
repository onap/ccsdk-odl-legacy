/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
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
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaAwareBuilders;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

public class NnToXmlWithDataFromSeveralModulesTest extends
        AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static EffectiveModelContext schemaContext;

    public NnToXmlWithDataFromSeveralModulesTest() {
        super(schemaContext, null);
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/nn-to-xml/data-of-several-modules/yang", schemaContext);
    }

    @Test
    public void dataFromSeveralModulesToXmlTest()
            throws WebApplicationException, IOException, URISyntaxException {
        final NormalizedNodeContext normalizedNodeContext = prepareNormalizedNodeContext();
        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                mediaType, null, output);

        final String outputString = output.toString();
        // data
        assertTrue(outputString
                .contains(
                        "<data xmlns=" + '"'
                                + "urn:ietf:params:xml:ns:netconf:base:1.0"
                                + '"' + '>'));
        // cont m2
        assertTrue(outputString.contains(
                "<cont_m2 xmlns=" + '"' + "module:two" + '"' + '>'));
        assertTrue(outputString.contains("<lf1_m2>lf1 m2 value</lf1_m2>"));
        assertTrue(outputString.contains("<contB_m2/>"));
        assertTrue(outputString.contains("</cont_m2>"));

        // cont m1
        assertTrue(outputString.contains(
                "<cont_m1 xmlns=" + '"' + "module:one" + '"' + '>'));
        assertTrue(outputString.contains("<contB_m1/>"));
        assertTrue(outputString.contains("<lf1_m1>lf1 m1 value</lf1_m1>"));
        assertTrue(outputString.contains("</cont_m1>"));

        // end
        assertTrue(output.toString().contains("</data>"));
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    private static NormalizedNodeContext prepareNormalizedNodeContext() {
        final String rev = "2014-01-17";

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataContSchemaContNode = SchemaAwareBuilders
                .containerBuilder(schemaContext);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> modul1 = buildContBuilderMod1(
                "module:one", rev, "cont_m1", "contB_m1", "lf1_m1",
                "lf1 m1 value");
        dataContSchemaContNode.withChild(modul1.build());

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> modul2 = buildContBuilderMod1(
                "module:two", rev, "cont_m2", "contB_m2", "lf1_m2",
                "lf1 m2 value");
        dataContSchemaContNode.withChild(modul2.build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                InstanceIdentifierContext.ofLocalRoot(schemaContext),
                dataContSchemaContNode.build());

        return testNormalizedNodeContext;
    }

    private static DataContainerNodeBuilder<NodeIdentifier, ContainerNode> buildContBuilderMod1(
            final String uri, final String rev, final String cont, final String contB, final String lf1,
            final String lf1Value) {
        final QName contQname = QName.create(uri, rev, cont);
        final QName contBQname = QName.create(uri, rev, contB);
        final QName lf1Qname = QName.create(contQname, lf1);

        final DataSchemaNode contSchemaNode = schemaContext
                .getDataChildByName(contQname);
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataContainerNodeAttrBuilder = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchemaNode);

        Preconditions.checkState(contSchemaNode instanceof ContainerSchemaNode);
        final var instanceLf1_m1 = ControllerContext.findInstanceDataChildrenByName(
                (DataNodeContainer) contSchemaNode, lf1Qname.getLocalName());
        final DataSchemaNode schemaLf1_m1 = instanceLf1_m1.get(0).child;

        dataContainerNodeAttrBuilder.withChild(SchemaAwareBuilders
                .leafBuilder((LeafSchemaNode) schemaLf1_m1)
                .withValue(lf1Value).build());

        final DataSchemaNode contBSchemaNode = ((ContainerSchemaNode) contSchemaNode)
                .getDataChildByName(contBQname);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataContainerB = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contBSchemaNode);

        return dataContainerNodeAttrBuilder.withChild(dataContainerB.build());
    }
}
