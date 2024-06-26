/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.netconf.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class TestJsonBodyReader extends AbstractBodyReaderTest {

    private final JsonNormalizedNodeBodyReader jsonBodyReader;
    private static EffectiveModelContext schemaContext;

    private static final QNameModule INSTANCE_IDENTIFIER_MODULE_QNAME = QNameModule.create(
        XMLNamespace.of("instance:identifier:module"), Revision.of("2014-01-17"));

    public TestJsonBodyReader() {
        super(schemaContext, null);
        jsonBodyReader = new JsonNormalizedNodeBodyReader(controllerContext);
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }

    @BeforeClass
    public static void initialization()
            throws Exception {
        final Collection<File> testFiles = TestRestconfUtils.loadFiles("/instanceidentifier/yang");
        testFiles.addAll(TestRestconfUtils.loadFiles("/invoke-rpc"));
        schemaContext = YangParserTestUtils.parseYangFiles(testFiles);
    }

    @Test
    public void moduleDataTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName());
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, false);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/jsondata.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerDataPutTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName()).node(cont1QName);
        final DataSchemaNode dataSchemaNodeOnPath = ((DataNodeContainer) dataSchemaNode).getDataChildByName(cont1QName);
        final String uri = "instance-identifier-module:cont/cont1";
        mockBodyReader(uri, jsonBodyReader, false);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_sub_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNodeOnPath, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName()).node(cont1QName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestJsonBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_sub_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final Module augmentModule = schemaContext.findModules(XMLNamespace.of("augment:module")).iterator().next();
        final QName contAugmentQName = QName.create(augmentModule.getQNameModule(), "cont-augment");
        final AugmentationIdentifier augII = new AugmentationIdentifier(Set.of(contAugmentQName));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(augII).node(contAugmentQName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_augment_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    //FIXME: Uncomment this when JsonParserStream works correctly with case augmentation with choice
    //@Test
    public void moduleSubContainerChoiceAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final Module augmentModule = schemaContext.findModules(XMLNamespace.of("augment:module")).iterator().next();
        final QName augmentChoice1QName = QName.create(augmentModule.getQNameModule(), "augment-choice1");
        final QName augmentChoice2QName = QName.create(augmentChoice1QName, "augment-choice2");
        final QName containerQName = QName.create(augmentChoice1QName, "case-choice-case-container1");
        final AugmentationIdentifier augChoice1II = new AugmentationIdentifier(Set.of(augmentChoice1QName));
        final AugmentationIdentifier augChoice2II = new AugmentationIdentifier(Set.of(augmentChoice2QName));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(augChoice1II).node(augmentChoice1QName).node(augChoice2II).node(augmentChoice2QName)
                .node(containerQName);
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/json/json_augment_choice_container.json");
        final NormalizedNodeContext returnValue = jsonBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void rpcModuleInputTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-test";
        mockBodyReader(uri, jsonBodyReader, true);
        final InputStream inputStream = TestJsonBodyReader.class.getResourceAsStream("/invoke-rpc/json/rpc-input.json");
        final NormalizedNodeContext returnValue = jsonBodyReader.readFrom(null, null, null, mediaType, null,
            inputStream);
        checkNormalizedNodeContextRpc(returnValue);
        final ContainerNode inputNode = (ContainerNode) returnValue.getData();
        final YangInstanceIdentifier yangCont = YangInstanceIdentifier.of(
            QName.create(inputNode.getIdentifier().getNodeType(), "cont"));
        final Optional<DataContainerChild> contDataNode = inputNode.findChildByArg(yangCont.getLastPathArgument());
        assertTrue(contDataNode.isPresent());
        assertTrue(contDataNode.get() instanceof ContainerNode);
        final YangInstanceIdentifier yangleaf = YangInstanceIdentifier.of(
            QName.create(inputNode.getIdentifier().getNodeType(), "lf"));
        final Optional<DataContainerChild> leafDataNode = ((ContainerNode) contDataNode.get())
                .findChildByArg(yangleaf.getLastPathArgument());
        assertTrue(leafDataNode.isPresent());
        assertTrue("lf-test".equalsIgnoreCase(leafDataNode.get().body().toString()));
    }

    private static void checkExpectValueNormalizeNodeContext(final DataSchemaNode dataSchemaNode,
            final NormalizedNodeContext nnContext, final YangInstanceIdentifier dataNodeIdent) {
        assertEquals(dataSchemaNode, nnContext.getInstanceIdentifierContext().getSchemaNode());
        assertEquals(dataNodeIdent, nnContext.getInstanceIdentifierContext().getInstanceIdentifier());
        assertNotNull(NormalizedNodes.findNode(nnContext.getData(), dataNodeIdent));
    }
}
