/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.netconf.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeContext;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.netconf.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.netconf.sal.rest.impl.WriterParameters;
import org.opendaylight.netconf.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.netconf.sal.restconf.impl.QueryParametersParser;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CutDataToCorrectDepthTest extends JerseyTest {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyTest.class);

    private static NormalizedNode depth1Cont;
    private static NormalizedNode depth2Cont1;
    private NormalizedNode globalPayload;
    private static EffectiveModelContext schemaContextModules;

    private final ControllerContext controllerContext =
            TestRestconfUtils.newControllerContext(schemaContextModules, null);

    @Path("/")
    public class RestImpl {

        @GET
        @Path("/config/{identifier:.+}")
        @Produces({ "application/json", "application/xml" })
        public NormalizedNodeContext getData(@Encoded @PathParam("identifier") final String identifier,
                                             @Context final UriInfo uriInfo) {

            final InstanceIdentifierContext iiWithData = controllerContext.toInstanceIdentifier(identifier);

            NormalizedNode data = null;
            if (identifier.equals("nested-module:depth1-cont/depth2-cont1")) {
                data = depth2Cont1;
            } else if (identifier.equals("nested-module:depth1-cont")) {
                data = depth1Cont;
            }

            final WriterParameters writerParameters = QueryParametersParser.parseWriterParameters(uriInfo);
            return new NormalizedNodeContext(iiWithData, data, writerParameters);
        }

        @GET
        @Path("/operational/{identifier:.+}")
        @Produces({ "application/json", "application/xml" })
        public NormalizedNodeContext getDataOperational(@Encoded @PathParam("identifier") final String identifier,
                                                        @Context final UriInfo uriInfo) {
            return getData(identifier, uriInfo);
        }

        @PUT
        @Path("/config/{identifier:.+}")
        @Consumes({ "application/json", "application/xml" })
        public void normalizedData(@Encoded @PathParam("identifier") final String identifier,
                                   final NormalizedNodeContext payload) throws InterruptedException {
            LOG.info("Payload: {}.", payload);
            LOG.info("Instance identifier of payload: {}.",
                    payload.getInstanceIdentifierContext().getInstanceIdentifier());
            LOG.info("Data of payload: {}.", payload.getData());
            globalPayload = payload.getData();
        }

        @PUT
        @Path("/operational/{identifier:.+}")
        @Consumes({ "application/json", "application/xml" })
        public void normalizedDataOperational(@Encoded @PathParam("identifier") final String identifier,
                                              final NormalizedNodeContext payload) throws InterruptedException {
            normalizedData(identifier, payload);
        }
    }

    @BeforeClass
    public static void initialize() throws FileNotFoundException, ReactorException {
        schemaContextModules = TestUtils.loadSchemaContext("/modules");
        final Module module = TestUtils.findModule(schemaContextModules.getModules(), "nested-module");
        assertNotNull(module);

        final UnkeyedListNode listAsUnkeyedList = unkeyedList(
                "depth2-cont1",
                unkeyedEntry("depth2-cont1",
                        container("depth3-cont1",
                            container("depth4-cont1", leaf("depth5-leaf1", "depth5-leaf1-value")),
                            leaf("depth4-leaf1", "depth4-leaf1-value")), leaf("depth3-leaf1", "depth3-leaf1-value")));

        final MapNode listAsMap = mapNode(
                "depth2-list2",
                mapEntryNode("depth2-list2", 2, leaf("depth3-lf1-key", "depth3-lf1-key-value"),
                        leaf("depth3-lf2-key", "depth3-lf2-key-value"), leaf("depth3-lf3", "depth3-lf3-value")));

        depth1Cont = container(
                "depth1-cont",
                listAsUnkeyedList,
                listAsMap,
                leafList("depth2-lfLst1", "depth2-lflst1-value1", "depth2-lflst1-value2", "depth2-lflst1-value3"),
                container(
                        "depth2-cont2",
                        container("depth3-cont2",
                            container("depth4-cont2", leaf("depth5-leaf2", "depth5-leaf2-value")),
                            leaf("depth4-leaf2", "depth4-leaf2-value")), leaf("depth3-leaf2", "depth3-leaf2-value")),
                leaf("depth2-leaf1", "depth2-leaf1-value"));

        depth2Cont1 = listAsUnkeyedList;
    }

    // TODO: These tests should be fixed/rewriten because they fail randomly due to data not being de-serialized
    // properly in readers
    //@Test
    public void getDataWithUriDepthParameterTest() throws WebApplicationException, IOException {
        getDataWithUriDepthParameter("application/json");
        getDataWithUriDepthParameter("application/xml");
    }

    public void getDataWithUriDepthParameter(final String mediaType) throws WebApplicationException, IOException {
        Response response;

        // Test config with depth 1
        response = target("/config/nested-module:depth1-cont").queryParam("depth", "1").request(mediaType)
                .get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth1());

        // Test config with depth 2
        response = target("/config/nested-module:depth1-cont").queryParam("depth", "2").request(mediaType)
                .get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth2());

        // Test config with depth 3
        response = target("/config/nested-module:depth1-cont").queryParam("depth", "3").request(mediaType)
                .get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth3());

        // Test config with depth 4
        response = target("/config/nested-module:depth1-cont").queryParam("depth", "4").request(mediaType)
                .get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth4());

        // Test config with depth 5
        response = target("/config/nested-module:depth1-cont").queryParam("depth", "5").request(mediaType)
                .get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth5());

        // Test config with depth unbounded

        response = target("/config/nested-module:depth1-cont").queryParam("depth", "unbounded")
                .request(mediaType).get();
        txtDataToNormalizedNode(response, mediaType, "/config/nested-module:depth1-cont");
        verifyResponse(nodeDataDepth5());
    }

    private void txtDataToNormalizedNode(final Response response, final String mediaType, final String uri) {
        final String responseStr = response.readEntity(String.class);
        LOG.info("Response entity message: {}.", responseStr);
        target(uri).request(mediaType).put(Entity.entity(responseStr, mediaType));
    }

    private void verifyResponse(final NormalizedNode nodeData) throws WebApplicationException, IOException {
        assertNotNull(globalPayload);
        assertEquals(globalPayload, nodeData);
        globalPayload = null;
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(new RestImpl());
        resourceConfig.registerClasses(XmlNormalizedNodeBodyReader.class, NormalizedNodeXmlBodyWriter.class,
                JsonNormalizedNodeBodyReader.class, NormalizedNodeJsonBodyWriter.class,
                RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    private static LeafNode<?> leaf(final String localName, final Object value) {
        return Builders.leafBuilder().withNodeIdentifier(toIdentifier(localName)).withValue(value).build();
    }

    private static ContainerNode container(final String localName, final DataContainerChild... children) {
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> containerBuilder =
                Builders.containerBuilder();
        for (final DataContainerChild child : children) {
            containerBuilder.withChild(child);
        }
        containerBuilder.withNodeIdentifier(toIdentifier(localName));
        return containerBuilder.build();
    }

    private static UnkeyedListNode unkeyedList(
            final String localName,
            final UnkeyedListEntryNode... entryNodes) {
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> builder = Builders.unkeyedListBuilder();
        final NodeIdentifier identifier = toIdentifier(localName);
        builder.withNodeIdentifier(identifier);
        for (final UnkeyedListEntryNode unkeyedListEntryNode : entryNodes) {
            builder.withChild(unkeyedListEntryNode);
        }
        return builder.build();
    }

    private static UnkeyedListEntryNode unkeyedEntry(final String localName, final DataContainerChild... children) {
        final DataContainerNodeBuilder<NodeIdentifier, UnkeyedListEntryNode> builder =
                Builders.unkeyedListEntryBuilder();
        builder.withNodeIdentifier(toIdentifier(localName));
        for (final DataContainerChild child : children) {
            builder.withChild(child);
        }
        return builder.build();
    }

    private static MapNode mapNode(final String localName, final MapEntryNode... entryNodes) {
        final CollectionNodeBuilder<MapEntryNode, SystemMapNode> builder = Builders.mapBuilder();
        builder.withNodeIdentifier(toIdentifier(localName));
        for (final MapEntryNode mapEntryNode : entryNodes) {
            builder.withChild(mapEntryNode);
        }
        return builder.build();
    }

    private static MapEntryNode mapEntryNode(final String localName, final int keysNumber,
                                             final DataContainerChild... children) {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder =
                Builders.mapEntryBuilder();
        final Map<QName, Object> keys = new HashMap<>();
        for (int i = 0; i < keysNumber; i++) {
            keys.put(children[i].getIdentifier().getNodeType(), children[i].body());
        }
        builder.withNodeIdentifier(toIdentifier(localName, keys));

        for (final DataContainerChild child : children) {
            builder.withChild(child);
        }
        return builder.build();
    }

    private static LeafSetNode<?> leafList(final String localName, final String... children) {
        final ListNodeBuilder<Object, SystemLeafSetNode<Object>> builder = Builders.leafSetBuilder();
        builder.withNodeIdentifier(toIdentifier(localName));
        for (final String child : children) {
            builder.withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(toIdentifier(localName, child))
                    .withValue(child).build());
        }
        return builder.build();
    }

    private static NodeIdentifier toIdentifier(final String localName) {
        return new NodeIdentifier(QName.create("urn:nested:module", "2014-06-03", localName));
    }

    private static NodeIdentifierWithPredicates toIdentifier(final String localName, final Map<QName, Object> keys) {
        return NodeIdentifierWithPredicates.of(QName.create("urn:nested:module", "2014-06-03", localName), keys);
    }

    private static NodeWithValue<?> toIdentifier(final String localName, final Object value) {
        return new NodeWithValue<>(QName.create("urn:nested:module", "2014-06-03", localName), value);
    }

    private static UnkeyedListEntryNode nodeDataDepth3Operational() {
        return unkeyedEntry("depth2-cont1",
                container("depth3-cont1", container("depth4-cont1"), leaf("depth4-leaf1", "depth4-leaf1-value")),
                leaf("depth3-leaf1", "depth3-leaf1-value"));
    }

    private static ContainerNode nodeDataDepth5() {
        return container(
                "depth1-cont",
                unkeyedList(
                        "depth2-cont1",
                        unkeyedEntry("depth2-cont1",
                                container("depth3-cont1",
                                        container("depth4-cont1", leaf("depth5-leaf1", "depth5-leaf1-value")),
                                        leaf("depth4-leaf1", "depth4-leaf1-value")),
                                leaf("depth3-leaf1", "depth3-leaf1-value"))),
                mapNode("depth2-list2",
                        mapEntryNode("depth2-list2", 2, leaf("depth3-lf1-key", "depth3-lf1-key-value"),
                            leaf("depth3-lf2-key", "depth3-lf2-key-value"), leaf("depth3-lf3", "depth3-lf3-value"))),
                leafList("depth2-lfLst1", "depth2-lflst1-value1", "depth2-lflst1-value2", "depth2-lflst1-value3"),
                container(
                        "depth2-cont2",
                        container("depth3-cont2",
                            container("depth4-cont2", leaf("depth5-leaf2", "depth5-leaf2-value")),
                            leaf("depth4-leaf2", "depth4-leaf2-value")), leaf("depth3-leaf2", "depth3-leaf2-value")),
                leaf("depth2-leaf1", "depth2-leaf1-value"));
    }

    private static ContainerNode nodeDataDepth4() {
        return container(
                "depth1-cont",
                unkeyedList("depth2-cont1", nodeDataDepth3Operational()),
                mapNode("depth2-list2",
                        mapEntryNode("depth2-list2", 2, leaf("depth3-lf1-key", "depth3-lf1-key-value"),
                            leaf("depth3-lf2-key", "depth3-lf2-key-value"), leaf("depth3-lf3", "depth3-lf3-value"))),
                leafList("depth2-lfLst1", "depth2-lflst1-value1", "depth2-lflst1-value2", "depth2-lflst1-value3"),
                container(
                    "depth2-cont2",
                    container("depth3-cont2", container("depth4-cont2"), leaf("depth4-leaf2", "depth4-leaf2-value")),
                    leaf("depth3-leaf2", "depth3-leaf2-value")), leaf("depth2-leaf1", "depth2-leaf1-value"));
    }

    private static ContainerNode nodeDataDepth3() {
        return container(
            "depth1-cont",
            unkeyedList("depth2-cont1",
                unkeyedEntry("depth2-cont1", container("depth3-cont1"), leaf("depth3-leaf1", "depth3-leaf1-value"))),
            mapNode("depth2-list2",
                    mapEntryNode("depth2-list2", 2, leaf("depth3-lf1-key", "depth3-lf1-key-value"),
                        leaf("depth3-lf2-key", "depth3-lf2-key-value"), leaf("depth3-lf3", "depth3-lf3-value"))),
            leafList("depth2-lfLst1", "depth2-lflst1-value1", "depth2-lflst1-value2", "depth2-lflst1-value3"),
            container("depth2-cont2", container("depth3-cont2"), leaf("depth3-leaf2", "depth3-leaf2-value")),
            leaf("depth2-leaf1", "depth2-leaf1-value"));
    }

    private static ContainerNode nodeDataDepth2() {
        return container(
                "depth1-cont",
                unkeyedList("depth2-cont1", unkeyedEntry("depth2-cont1")),
                mapNode("depth2-list2",
                        mapEntryNode("depth2-list2", 2, leaf("depth3-lf1-key", "depth3-lf1-key-value"),
                                leaf("depth3-lf2-key", "depth3-lf2-key-value"))), container("depth2-cont2"),
//                leafList("depth2-lfLst1"),
                leaf("depth2-leaf1", "depth2-leaf1-value"));
    }

    private static ContainerNode nodeDataDepth1() {
        return container("depth1-cont");
    }
}
