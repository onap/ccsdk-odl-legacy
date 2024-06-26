/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.streams.listeners;

import static java.time.Instant.EPOCH;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.yang.gen.v1.instance.identifier.patch.module.rev151121.PatchCont;
import org.opendaylight.yang.gen.v1.instance.identifier.patch.module.rev151121.patch.cont.MyList1;
import org.opendaylight.yang.gen.v1.instance.identifier.patch.module.rev151121.patch.cont.MyList1Builder;
import org.opendaylight.yang.gen.v1.instance.identifier.patch.module.rev151121.patch.cont.MyList1Key;
import org.opendaylight.yang.gen.v1.urn.sal.restconf.event.subscription.rev140708.NotificationOutputTypeGrouping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenerAdapterTest extends AbstractConcurrentDataBrokerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerAdapterTest.class);

    private static final String JSON_NOTIF_LEAVES_CREATE = "/listener-adapter-test/notif-leaves-create.json";
    private static final String JSON_NOTIF_LEAVES_UPDATE =  "/listener-adapter-test/notif-leaves-update.json";
    private static final String JSON_NOTIF_LEAVES_DEL =  "/listener-adapter-test/notif-leaves-del.json";
    private static final String JSON_NOTIF_CREATE = "/listener-adapter-test/notif-create.json";
    private static final String JSON_NOTIF_UPDATE = "/listener-adapter-test/notif-update.json";
    private static final String JSON_NOTIF_DEL = "/listener-adapter-test/notif-del.json";
    private static final String JSON_NOTIF_WITHOUT_DATA_CREATE =
            "/listener-adapter-test/notif-without-data-create.json";
    private static final String JSON_NOTIF_WITHOUT_DATA_UPDATE =
            "/listener-adapter-test/notif-without-data-update.json";
    private static final String JSON_NOTIF_WITHOUT_DATA_DELETE =
            "/listener-adapter-test/notif-without-data-del.json";


    private static final YangInstanceIdentifier PATCH_CONT_YIID =
            YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(PatchCont.QNAME));

    private static EffectiveModelContext schemaContext;

    private DataBroker dataBroker;
    private DOMDataBroker domDataBroker;
    private ControllerContext controllerContext;

    @BeforeClass
    public static void init() {
        schemaContext = YangParserTestUtils.parseYangResource(
                "/instanceidentifier/yang/instance-identifier-patch-module.yang");
    }

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        domDataBroker = getDomBroker();
        controllerContext = TestRestconfUtils.newControllerContext(schemaContext);
    }

    class ListenerAdapterTester extends ListenerAdapter {

        private String lastNotification = null;

        ListenerAdapterTester(final YangInstanceIdentifier path, final String streamName,
                final NotificationOutputTypeGrouping.NotificationOutputType outputType,
                final boolean leafNodesOnly, final boolean skipNotificationData) {
            super(path, streamName, outputType, controllerContext);
            setQueryParams(EPOCH, Optional.empty(), Optional.empty(), leafNodesOnly, skipNotificationData);
        }

        @Override
        protected void post(final Event event) {
            this.lastNotification = event.getData();
        }

        public void assertGot(final String json) throws Exception {
            long start = System.currentTimeMillis();
            while (true) {
                if (lastNotification != null) {
                    break;
                }
                if (System.currentTimeMillis() - start > 1000) {
                    throw new Exception("TIMED OUT waiting for notification with " + json);
                }
                Thread.currentThread();
                Thread.sleep(200);
            }

            this.lastNotification = null;
        }
    }

    static String withFakeDate(final String in) {
        JSONObject doc = new JSONObject(in);
        JSONObject notification = doc.getJSONObject("notification");
        if (notification == null) {
            return in;
        }
        notification.put("eventTime", "someDate");
        return doc.toString();
    }

    private String getNotifJson(final String path) throws IOException, URISyntaxException {
        URL url = getClass().getResource(path);
        byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
        return withFakeDate(new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testJsonNotifsLeaves() throws Exception {
        ListenerAdapterTester adapter = new ListenerAdapterTester(PATCH_CONT_YIID, "Casey",
                NotificationOutputTypeGrouping.NotificationOutputType.JSON, true, false);
        DOMDataTreeChangeService changeService = domDataBroker.getExtensions()
                .getInstance(DOMDataTreeChangeService.class);
        DOMDataTreeIdentifier root = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, PATCH_CONT_YIID);
        changeService.registerDataTreeChangeListener(root, adapter);

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        MyList1Builder builder = new MyList1Builder().setMyLeaf11("Jed").setName("Althea");
        InstanceIdentifier<MyList1> iid = InstanceIdentifier.create(PatchCont.class)
                .child(MyList1.class, new MyList1Key("Althea"));
        writeTransaction.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_LEAVES_CREATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        builder = new MyList1Builder().withKey(new MyList1Key("Althea")).setMyLeaf12("Bertha");
        writeTransaction.mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_LEAVES_UPDATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_LEAVES_DEL));
    }

    @Test
    public void testJsonNotifs() throws Exception {
        ListenerAdapterTester adapter = new ListenerAdapterTester(PATCH_CONT_YIID, "Casey",
                NotificationOutputTypeGrouping.NotificationOutputType.JSON, false, false);
        DOMDataTreeChangeService changeService = domDataBroker.getExtensions()
                .getInstance(DOMDataTreeChangeService.class);
        DOMDataTreeIdentifier root = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, PATCH_CONT_YIID);
        changeService.registerDataTreeChangeListener(root, adapter);

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        MyList1Builder builder = new MyList1Builder().setMyLeaf11("Jed").setName("Althea");
        InstanceIdentifier<MyList1> iid = InstanceIdentifier.create(PatchCont.class)
                .child(MyList1.class, new MyList1Key("Althea"));
        writeTransaction.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_CREATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        builder = new MyList1Builder().withKey(new MyList1Key("Althea")).setMyLeaf12("Bertha");
        writeTransaction.mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_UPDATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_DEL));
    }

    @Test
    public void testJsonNotifsWithoutData() throws Exception {
        ListenerAdapterTester adapter = new ListenerAdapterTester(PATCH_CONT_YIID, "Casey",
                NotificationOutputTypeGrouping.NotificationOutputType.JSON, false, true);
        DOMDataTreeChangeService changeService = domDataBroker.getExtensions()
                .getInstance(DOMDataTreeChangeService.class);
        DOMDataTreeIdentifier root = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, PATCH_CONT_YIID);
        changeService.registerDataTreeChangeListener(root, adapter);

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        MyList1Builder builder = new MyList1Builder().setMyLeaf11("Jed").setName("Althea");
        InstanceIdentifier<MyList1> iid = InstanceIdentifier.create(PatchCont.class)
                .child(MyList1.class, new MyList1Key("Althea"));
        writeTransaction.mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_WITHOUT_DATA_CREATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        builder = new MyList1Builder().withKey(new MyList1Key("Althea")).setMyLeaf12("Bertha");
        writeTransaction.mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_WITHOUT_DATA_UPDATE));

        writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
        writeTransaction.commit();
        adapter.assertGot(getNotifJson(JSON_NOTIF_WITHOUT_DATA_DELETE));
    }
}
