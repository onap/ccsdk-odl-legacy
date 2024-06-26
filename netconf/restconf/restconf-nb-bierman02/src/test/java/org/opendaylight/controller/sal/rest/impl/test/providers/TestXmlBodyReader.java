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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeContext;
import org.opendaylight.netconf.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.common.errors.RestconfError;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class TestXmlBodyReader extends AbstractBodyReaderTest {

    private final XmlNormalizedNodeBodyReader xmlBodyReader;
    private static EffectiveModelContext schemaContext;
    private static final QNameModule INSTANCE_IDENTIFIER_MODULE_QNAME = QNameModule.create(
        XMLNamespace.of("instance:identifier:module"), Revision.of("2014-01-17"));

    public TestXmlBodyReader() {
        super(schemaContext, null);
        xmlBodyReader = new XmlNormalizedNodeBodyReader(controllerContext);
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }

    @BeforeClass
    public static void initialization() throws Exception {
        final Collection<File> testFiles = TestRestconfUtils.loadFiles("/instanceidentifier/yang");
        testFiles.addAll(TestRestconfUtils.loadFiles("/invoke-rpc"));
        testFiles.addAll(TestRestconfUtils.loadFiles("/foo-xml-test/yang"));
        schemaContext = YangParserTestUtils.parseYangFiles(testFiles);
    }

    @Test
    public void putXmlTest() throws Exception {
        runXmlTest(false, "foo:top-level-list/key-value");
    }

    @Test
    public void postXmlTest() throws Exception {
        runXmlTest(true, "");
    }

    private void runXmlTest(final boolean isPost, final String path) throws Exception {
        mockBodyReader(path, xmlBodyReader, isPost);
        final InputStream inputStream = TestXmlBodyReader.class.getResourceAsStream("/foo-xml-test/foo.xml");
        final NormalizedNodeContext nnc = xmlBodyReader.readFrom(null, null, null, mediaType, null, inputStream);
        assertNotNull(nnc);

        assertTrue(nnc.getData() instanceof MapEntryNode);
        final MapEntryNode data = (MapEntryNode) nnc.getData();
        assertEquals(2, data.size());
        for (final DataContainerChild child : data.body()) {
            switch (child.getIdentifier().getNodeType().getLocalName()) {
                case "key-leaf":
                    assertEquals("key-value", child.body());
                    break;

                case "ordinary-leaf":
                    assertEquals("leaf-value", child.body());
                    break;
                default:
                    fail();
            }
        }
    }

    @Test
    public void moduleDataTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName());
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, xmlBodyReader, false);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xmldata.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
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
        mockBodyReader(uri, xmlBodyReader, false);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_sub_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
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
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_sub_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
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
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_augment_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void moduleSubContainerChoiceAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode =
                schemaContext.getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final Module augmentModule = schemaContext.findModules(XMLNamespace.of("augment:module")).iterator().next();
        final QName augmentChoice1QName = QName.create(augmentModule.getQNameModule(), "augment-choice1");
        final QName augmentChoice2QName = QName.create(augmentChoice1QName, "augment-choice2");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(new AugmentationIdentifier(Set.of(augmentChoice1QName)))
                .node(augmentChoice1QName)
                // FIXME: DataSchemaTreeNode intepretation seems to have a bug
                //.node(new AugmentationIdentifier(Set.of(augmentChoice2QName)))
                .node(augmentChoice2QName)
                .node(QName.create(augmentChoice1QName, "case-choice-case-container1"));
        final String uri = "instance-identifier-module:cont";
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xml_augment_choice_container.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);
        checkNormalizedNodeContext(returnValue);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, returnValue, dataII);
    }

    @Test
    public void rpcModuleInputTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-test";
        mockBodyReader(uri, xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class.getResourceAsStream("/invoke-rpc/xml/rpc-input.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader.readFrom(null, null, null, mediaType, null,
            inputStream);
        checkNormalizedNodeContextRpc(returnValue);
        final ContainerNode contNode = (ContainerNode) returnValue.getData();
        final Optional<DataContainerChild> contDataNodePotential = contNode.findChildByArg(new NodeIdentifier(
            QName.create(contNode.getIdentifier().getNodeType(), "cont")));
        assertTrue(contDataNodePotential.isPresent());
        final ContainerNode contDataNode = (ContainerNode) contDataNodePotential.get();
        final Optional<DataContainerChild> leafDataNode = contDataNode.findChildByArg(new NodeIdentifier(
            QName.create(contDataNode.getIdentifier().getNodeType(), "lf")));
        assertTrue(leafDataNode.isPresent());
        assertTrue("lf-test".equalsIgnoreCase(leafDataNode.get().body().toString()));
    }

    private static void checkExpectValueNormalizeNodeContext(final DataSchemaNode dataSchemaNode,
            final NormalizedNodeContext nnContext, final YangInstanceIdentifier dataNodeIdent) {
        assertEquals(dataSchemaNode, nnContext.getInstanceIdentifierContext().getSchemaNode());
        assertEquals(dataNodeIdent, nnContext.getInstanceIdentifierContext().getInstanceIdentifier());
        assertNotNull(NormalizedNodes.findNode(nnContext.getData(), dataNodeIdent));
    }

    /**
     * Test when container with the same name is placed in two modules (foo-module and bar-module). Namespace must be
     * used to distinguish between them to find correct one. Check if container was found not only according to its name
     * but also by correct namespace used in payload.
     */
    @Test
    public void findFooContainerUsingNamespaceTest() throws Exception {
        mockBodyReader("", xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xmlDataFindFooContainer.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);

        // check return value
        checkNormalizedNodeContext(returnValue);
        // check if container was found both according to its name and namespace
        assertEquals("Not correct container found, name was ignored",
                "foo-bar-container", returnValue.getData().getIdentifier().getNodeType().getLocalName());
        assertEquals("Not correct container found, namespace was ignored",
                "foo:module", returnValue.getData().getIdentifier().getNodeType().getNamespace().toString());
    }

    /**
     * Test when container with the same name is placed in two modules (foo-module and bar-module). Namespace must be
     * used to distinguish between them to find correct one. Check if container was found not only according to its name
     * but also by correct namespace used in payload.
     */
    @Test
    public void findBarContainerUsingNamespaceTest() throws Exception {
        mockBodyReader("", xmlBodyReader, true);
        final InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/instanceidentifier/xml/xmlDataFindBarContainer.xml");
        final NormalizedNodeContext returnValue = xmlBodyReader
                .readFrom(null, null, null, mediaType, null, inputStream);

        // check return value
        checkNormalizedNodeContext(returnValue);
        // check if container was found both according to its name and namespace
        assertEquals("Not correct container found, name was ignored",
                "foo-bar-container", returnValue.getData().getIdentifier().getNodeType().getLocalName());
        assertEquals("Not correct container found, namespace was ignored",
                "bar:module", returnValue.getData().getIdentifier().getNodeType().getNamespace().toString());
    }

    /**
     * Test PUT operation when message root element is not the same as the last element in request URI.
     * PUT operation message should always start with schema node from URI otherwise exception should be
     * thrown.
     */
    @Test
    public void wrongRootElementTest() throws Exception {
        mockBodyReader("instance-identifier-module:cont", xmlBodyReader, false);
        final InputStream inputStream = TestXmlBodyReader.class.getResourceAsStream(
                "/instanceidentifier/xml/bug7933.xml");
        try {
            xmlBodyReader.readFrom(null, null, null, mediaType, null, inputStream);
            Assert.fail("Test should fail due to malformed PUT operation message");
        } catch (final RestconfDocumentedException exception) {
            final RestconfError restconfError = exception.getErrors().get(0);
            assertEquals(ErrorType.PROTOCOL, restconfError.getErrorType());
            assertEquals(ErrorTag.MALFORMED_MESSAGE, restconfError.getErrorTag());
        }
    }
}
