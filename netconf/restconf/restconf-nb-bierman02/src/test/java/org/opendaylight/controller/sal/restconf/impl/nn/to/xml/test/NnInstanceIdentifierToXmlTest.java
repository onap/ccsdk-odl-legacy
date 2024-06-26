/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaAwareBuilders;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

public class NnInstanceIdentifierToXmlTest extends AbstractBodyReaderTest {
    private static EffectiveModelContext schemaContext;

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter = new NormalizedNodeXmlBodyWriter();

    public NnInstanceIdentifierToXmlTest() {
        super(schemaContext, null);
    }

    @BeforeClass
    public static void initialization() throws URISyntaxException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang", schemaContext);
    }

    @Test
    public void nnAsYangInstanceIdentifierAugmentLeafList() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareNNCLeafList();

        final OutputStream output = new ByteArrayOutputStream();

        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null, mediaType, null, output);

        assertNotNull(output);

        final String outputJson = output.toString();

        assertTrue(outputJson.contains("<cont xmlns="));
        assertTrue(outputJson.contains(
                '"' + "instance:identifier:module" + '"'));
        assertTrue(outputJson.contains(">"));

        assertTrue(outputJson.contains("<cont1>"));

        assertTrue(outputJson.contains("<lf11 xmlns="));
        assertTrue(outputJson.contains(
                '"' + "augment:module:leaf:list" + '"'));
        assertTrue(outputJson.contains(">"));
        assertTrue(outputJson.contains("/instanceidentifier/"));
        assertTrue(outputJson.contains("</lf11>"));

        assertTrue(outputJson.contains("<lflst11 xmlns="));
        assertTrue(outputJson.contains(
                '"' + "augment:module:leaf:list" + '"'));
        assertTrue(outputJson.contains(">"));
        assertTrue(outputJson.contains("lflst11 value"));
        assertTrue(outputJson.contains("</lflst11>"));

        assertTrue(outputJson.contains("</cont1>"));
        assertTrue(outputJson.contains("</cont>"));
    }

    private static NormalizedNodeContext prepareNNCLeafList() throws URISyntaxException {
        final QName cont = QName.create("instance:identifier:module", "2014-01-17",
                "cont");
        final QName cont1 = QName.create("instance:identifier:module", "2014-01-17",
                "cont1");
        final QName lflst11 = QName.create("augment:module:leaf:list", "2014-01-17",
                "lflst11");
        final QName lf11 = QName.create("augment:module:leaf:list", "2014-01-17",
                "lf11");

        final DataSchemaNode schemaCont = schemaContext.getDataChildByName(cont);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataCont = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) schemaCont);

        final DataSchemaNode schemaCont1 = ((ContainerSchemaNode) schemaCont).getDataChildByName(cont1);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataCont1 = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) schemaCont1);

        final var instanceLfLst11 = ControllerContext.findInstanceDataChildrenByName(
                (DataNodeContainer) schemaCont1, lflst11.getLocalName());

        final DataSchemaNode lfLst11Schema = instanceLfLst11.get(0).child;
        final ListNodeBuilder<Object, SystemLeafSetNode<Object>> lfLst11Data = SchemaAwareBuilders
                .leafSetBuilder((LeafListSchemaNode) lfLst11Schema);

        lfLst11Data.withChild(SchemaAwareBuilders.leafSetEntryBuilder((LeafListSchemaNode) lfLst11Schema)
                .withValue("lflst11 value").build());
        dataCont1.withChild(lfLst11Data.build());

        final var instanceLf11 = ControllerContext.findInstanceDataChildrenByName(
                (DataNodeContainer) schemaCont1, lf11.getLocalName());
        final DataSchemaNode lf11Schema = instanceLf11.get(0).child;

        dataCont1.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) lf11Schema)
                .withValue("/instanceidentifier/").build());
        dataCont.withChild(dataCont1.build());

        return new NormalizedNodeContext(
            InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
            dataCont.build());
    }

    @Test
    public void nnAsYangInstanceIdentifierAugment() throws Exception {

        final NormalizedNodeContext normalizedNodeContext = preparNNC();
        final OutputStream output = new ByteArrayOutputStream();

        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null,
                mediaType, null, output);

        assertNotNull(output);

        final String outputJson = output.toString();

        assertTrue(outputJson.contains("<cont xmlns="));
        assertTrue(outputJson.contains(
                '"' + "instance:identifier:module" + '"'));
        assertTrue(outputJson.contains(">"));

        assertTrue(outputJson.contains("<cont1>"));

        assertTrue(outputJson.contains("<lst11 xmlns="));
        assertTrue(outputJson.contains('"' + "augment:module" + '"'));
        assertTrue(outputJson.contains(">"));

        assertTrue(outputJson.contains(
                "<keyvalue111>keyvalue111</keyvalue111>"));
        assertTrue(outputJson.contains(
                "<keyvalue112>keyvalue112</keyvalue112>"));

        assertTrue(outputJson.contains("<lf111 xmlns="));
        assertTrue(outputJson.contains(
                '"' + "augment:augment:module" + '"'));
        assertTrue(outputJson.contains(">/cont/cont1/lf12</lf111>"));

        assertTrue(outputJson.contains("<lf112 xmlns="));
        assertTrue(outputJson.contains(
                '"' + "augment:augment:module" + '"'));
        assertTrue(outputJson.contains(">lf12 value</lf112>"));

        assertTrue(outputJson.contains("</lst11></cont1></cont>"));
    }

    private static NormalizedNodeContext preparNNC() {
        final QName cont = QName.create("instance:identifier:module", "2014-01-17",
                "cont");
        final QName cont1 = QName.create("instance:identifier:module", "2014-01-17",
                "cont1");
        final QName lst11 = QName.create("augment:module", "2014-01-17", "lst11");
        final QName lf11 = QName.create("augment:augment:module", "2014-01-17",
                "lf111");
        final QName lf12 = QName.create("augment:augment:module", "2014-01-17",
                "lf112");
        final QName keyvalue111 = QName.create("augment:module", "2014-01-17",
                "keyvalue111");
        final QName keyvalue112 = QName.create("augment:module", "2014-01-17",
                "keyvalue112");

        final DataSchemaNode schemaCont = schemaContext.getDataChildByName(cont);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataCont = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) schemaCont);

        final DataSchemaNode schemaCont1 = ((ContainerSchemaNode) schemaCont)
                .getDataChildByName(cont1);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> dataCont1 = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) schemaCont1);

        final var instanceLst11 = ControllerContext.findInstanceDataChildrenByName(
                (DataNodeContainer) schemaCont1, lst11.getLocalName());
        final DataSchemaNode lst11Schema = instanceLst11.get(0).child;

        final CollectionNodeBuilder<MapEntryNode, SystemMapNode> dataLst11 = SchemaAwareBuilders
                .mapBuilder((ListSchemaNode) lst11Schema);

        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> dataLst11Vaule = SchemaAwareBuilders
                .mapEntryBuilder((ListSchemaNode) lst11Schema);

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, keyvalue111, dataLst11, "keyvalue111"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, keyvalue112, dataLst11, "keyvalue112"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, lf11, dataLst11, "/cont/cont1/lf12"));

        dataLst11Vaule.withChild(buildLeaf(lst11Schema, lf12, dataLst11, "lf12 value"));

        dataLst11.withChild(dataLst11Vaule.build());

        dataCont1.withChild(dataLst11.build());
        dataCont.withChild(dataCont1.build());

        return new NormalizedNodeContext(
            InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
            dataCont.build());
    }

    private static DataContainerChild buildLeaf(final DataSchemaNode lst11Schema, final QName qname,
            final CollectionNodeBuilder<MapEntryNode, SystemMapNode> dataLst11, final Object value) {

        final var instanceLf = ControllerContext.findInstanceDataChildrenByName(
            (DataNodeContainer) lst11Schema, qname.getLocalName());
        final DataSchemaNode schemaLf = instanceLf.get(0).child;

        return SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue(value).build();
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
