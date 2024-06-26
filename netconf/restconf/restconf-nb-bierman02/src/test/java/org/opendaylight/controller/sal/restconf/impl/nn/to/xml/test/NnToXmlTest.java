/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.xml.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Throwables;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeContext;
import org.opendaylight.netconf.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaAwareBuilders;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.ri.type.BaseTypes;
import org.opendaylight.yangtools.yang.model.ri.type.BitsTypeBuilder;
import org.opendaylight.yangtools.yang.model.ri.type.EnumerationTypeBuilder;
import org.opendaylight.yangtools.yang.model.ri.type.UnionTypeBuilder;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

public class NnToXmlTest extends AbstractBodyReaderTest {
    private static EffectiveModelContext schemaContext;

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;

    public NnToXmlTest() {
        super(schemaContext, null);
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-xml/yang", schemaContext);
    }

    @Test
    public void nnAsYangIdentityrefToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData(null, true);
        nnToXml(normalizedNodeContext, "<lf11 xmlns:x=\"referenced:module\">x:iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithQNamePrefixToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData("prefix", true);
        nnToXml(normalizedNodeContext, "<lf11 xmlns", "=\"referenced:module\">", ":iden</lf11>");
    }

    @Test
    public void nnAsYangIdentityrefWithPrefixToXMLTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareIdrefData("prefix", false);
        nnToXml(normalizedNodeContext, "<lf11>no qname value</lf11>");
    }

    @Test
    public void nnAsYangLeafrefWithPrefixToXMLTest() throws Exception {
        nnToXml(prepareLeafrefData(), "<lfBoolean>true</lfBoolean>", "<lfLfref>true</lfLfref>");
    }

    /**
     * Negative test when leaf of type leafref references to not-existing leaf.
     * {@code VerifyException} is expected.
     */
    @Test
    public void nnAsYangLeafrefWithPrefixToXMLNegativeTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareLeafrefNegativeData();

        final IOException ex = assertThrows(IOException.class, () -> nnToXml(normalizedNodeContext,
            "<not-existing>value</not-existing>", "<lfLfrefNegative>value</lfLfrefnegative>"));
        final Throwable rootCause = Throwables.getRootCause(ex);
        assertThat(rootCause, instanceOf(IllegalArgumentException.class));
        assertEquals("Data tree child (basic:module?revision=2013-12-02)not-existing not present in schema parent "
            + "(basic:module?revision=2013-12-02)cont", rootCause.getMessage());
    }

    @Test
    public void nnAsYangStringToXmlTest() throws Exception {
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.stringType()).deserialize("lfStr value"), "lfStr");
        nnToXml(normalizedNodeContext, "<lfStr>lfStr value</lfStr>");
    }

    @Test
    public void nnAsYangInt8ToXmlTest() throws Exception {
        final String elName = "lfInt8";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.int8Type()).deserialize("14"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">14</" + elName + ">");
    }

    @Test
    public void nnAsYangInt16ToXmlTest() throws Exception {
        final String elName = "lfInt16";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.int16Type()).deserialize("3000"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">3000</" + elName + ">");
    }

    @Test
    public void nnAsYangInt32ToXmlTest() throws Exception {
        final String elName = "lfInt32";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.int32Type()).deserialize("201234"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">201234</" + elName + ">");
    }

    @Test
    public void nnAsYangInt64ToXmlTest() throws Exception {
        final String elName = "lfInt64";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.int64Type()).deserialize("5123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void nnAsYangUint8ToXmlTest() throws Exception {
        final String elName = "lfUint8";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.uint8Type()).deserialize("200"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">200</" + elName + ">");
    }

    @Test
    public void snAsYangUint16ToXmlTest() throws Exception {
        final String elName = "lfUint16";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.uint16Type()).deserialize("4000"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">4000</" + elName + ">");
    }

    @Test
    public void nnAsYangUint32ToXmlTest() throws Exception {
        final String elName = "lfUint32";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.uint32Type()).deserialize("4123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">4123456789</" + elName + ">");
    }

    @Test
    public void snAsYangUint64ToXmlTest() throws Exception {
        final String elName = "lfUint64";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.uint64Type()).deserialize("5123456789"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">5123456789</" + elName + ">");
    }

    @Test
    public void nnAsYangBinaryToXmlTest() throws Exception {
        final String elName = "lfBinary";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.binaryType())
                        .deserialize("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567"),
                elName);
        nnToXml(normalizedNodeContext,
                "<" + elName + ">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567</" + elName + ">");
    }

    @Test
    public void nnAsYangBitsToXmlTest() throws Exception {
        final BitsTypeDefinition.Bit mockBit1 = Mockito.mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit1.getName()).thenReturn("one");
        Mockito.when(mockBit1.getPosition()).thenReturn(Uint32.ONE);
        final BitsTypeDefinition.Bit mockBit2 = Mockito.mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit2.getName()).thenReturn("two");
        Mockito.when(mockBit2.getPosition()).thenReturn(Uint32.TWO);
        final BitsTypeBuilder bitsTypeBuilder = BaseTypes.bitsTypeBuilder(QName.create("foo", "foo"));
        bitsTypeBuilder.addBit(mockBit1);
        bitsTypeBuilder.addBit(mockBit2);

        final String elName = "lfBits";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(bitsTypeBuilder.build()).deserialize("one two"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">one two</" + elName + ">");
    }

    @Test
    public void nnAsYangEnumerationToXmlTest() throws Exception {
        final EnumTypeDefinition.EnumPair mockEnum = Mockito.mock(EnumTypeDefinition.EnumPair.class);
        Mockito.when(mockEnum.getName()).thenReturn("enum2");

        final EnumerationTypeBuilder enumerationTypeBuilder = BaseTypes
                .enumerationTypeBuilder(QName.create("foo", "foo"));
        enumerationTypeBuilder.addEnum(mockEnum);

        final String elName = "lfEnumeration";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(enumerationTypeBuilder.build()).deserialize("enum2"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">enum2</" + elName + ">");
    }

    @Test
    public void nnAsYangEmptyToXmlTest() throws Exception {
        final String elName = "lfEmpty";
        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.emptyType()).deserialize(""), elName);
        nnToXml(normalizedNodeContext, "<" + elName + "/>");
    }

    @Test
    public void nnAsYangBooleanToXmlTest() throws Exception {
        final String elName = "lfBoolean";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(BaseTypes.booleanType()).deserialize("false"), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">false</" + elName + ">");

        normalizedNodeContext = prepareNNC(TypeDefinitionAwareCodec.from(BaseTypes.booleanType()).deserialize("true"),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">true</" + elName + ">");
    }

    @Test
    public void nnAsYangUnionToXmlTest() throws Exception {
        final BitsTypeDefinition.Bit mockBit1 = Mockito.mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit1.getName()).thenReturn("first");
        Mockito.when(mockBit1.getPosition()).thenReturn(Uint32.ONE);
        final BitsTypeDefinition.Bit mockBit2 = Mockito.mock(BitsTypeDefinition.Bit.class);
        Mockito.when(mockBit2.getName()).thenReturn("second");
        Mockito.when(mockBit2.getPosition()).thenReturn(Uint32.TWO);

        final BitsTypeBuilder bitsTypeBuilder = BaseTypes.bitsTypeBuilder(QName.create("foo", "foo"));
        bitsTypeBuilder.addBit(mockBit1);
        bitsTypeBuilder.addBit(mockBit2);

        final UnionTypeBuilder unionTypeBuilder = BaseTypes.unionTypeBuilder(QName.create("foo", "foo"));
        unionTypeBuilder.addType(BaseTypes.int8Type());
        unionTypeBuilder.addType(bitsTypeBuilder.build());
        unionTypeBuilder.addType(BaseTypes.booleanType());
        unionTypeBuilder.addType(BaseTypes.stringType());

        final String elName = "lfUnion";

        // test int8
        final String int8 = "15";
        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                TypeDefinitionAwareCodec.from(unionTypeBuilder.build()).deserialize(int8), elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">15</" + elName + ">");

        // test bits
        final String bits = "first second";
        normalizedNodeContext = prepareNNC(TypeDefinitionAwareCodec.from(unionTypeBuilder.build()).deserialize(bits),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">[first, second]</" + elName + ">");

        // test boolean
        final String bool = "true";
        normalizedNodeContext = prepareNNC(TypeDefinitionAwareCodec.from(unionTypeBuilder.build()).deserialize(bool),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">true</" + elName + ">");

        // test string
        final String s = "Hi!";
        normalizedNodeContext = prepareNNC(TypeDefinitionAwareCodec.from(unionTypeBuilder.build()).deserialize(s),
                elName);
        nnToXml(normalizedNodeContext, "<" + elName + ">Hi!</" + elName + ">");
    }

    private static NormalizedNodeContext prepareNNC(final Object object, final String name) {
        final QName cont = QName.create("basic:module", "2013-12-02", "cont");
        final QName lf = QName.create("basic:module", "2013-12-02", name);

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> contData = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchema);

        final var instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) contSchema, lf.getLocalName());
        final DataSchemaNode schemaLf = instanceLf.get(0).child;

        contData.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue(object).build());

        return new NormalizedNodeContext(
            InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
                contData.build());
    }

    private void nnToXml(final NormalizedNodeContext normalizedNodeContext, final String... xmlRepresentation)
            throws Exception {
        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(normalizedNodeContext, null, null, null, mediaType, null, output);

        for (String element : xmlRepresentation) {
            assertTrue(output.toString().contains(element));
        }
    }

    private static NormalizedNodeContext prepareLeafrefData() {
        final QName cont = QName.create("basic:module", "2013-12-02", "cont");
        final QName lfBoolean = QName.create("basic:module", "2013-12-02", "lfBoolean");
        final QName lfLfref = QName.create("basic:module", "2013-12-02", "lfLfref");

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> contData = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchema);

        var instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) contSchema, lfBoolean.getLocalName());
        DataSchemaNode schemaLf = instanceLf.get(0).child;

        contData.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue(Boolean.TRUE).build());

        instanceLf = ControllerContext.findInstanceDataChildrenByName((DataNodeContainer) contSchema,
                lfLfref.getLocalName());
        schemaLf = instanceLf.get(0).child;

        contData.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue("true").build());

        return new NormalizedNodeContext(
                InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
                contData.build());
    }

    private static NormalizedNodeContext prepareLeafrefNegativeData() {
        final QName cont = QName.create("basic:module", "2013-12-02", "cont");
        final QName lfLfref = QName.create("basic:module", "2013-12-02", "lfLfrefNegative");

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> contData = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchema);

        final var instanceLf = ControllerContext.findInstanceDataChildrenByName((DataNodeContainer)
                contSchema, lfLfref.getLocalName());
        final DataSchemaNode schemaLf = instanceLf.get(0).child;

        contData.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue("value").build());

        return new NormalizedNodeContext(
            InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
            contData.build());
    }

    private static NormalizedNodeContext prepareIdrefData(final String prefix, final boolean valueAsQName) {
        final QName cont = QName.create("basic:module", "2013-12-02", "cont");
        final QName cont1 = QName.create("basic:module", "2013-12-02", "cont1");
        final QName lf11 = QName.create("basic:module", "2013-12-02", "lf11");

        final DataSchemaNode contSchema = schemaContext.getDataChildByName(cont);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> contData = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) contSchema);

        final DataSchemaNode cont1Schema = ((ContainerSchemaNode) contSchema).getDataChildByName(cont1);

        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> cont1Data = SchemaAwareBuilders
                .containerBuilder((ContainerSchemaNode) cont1Schema);

        Object value = null;
        if (valueAsQName) {
            value = QName.create("referenced:module", "2013-12-02", "iden");
        } else {
            value = "no qname value";
        }

        final var instanceLf = ControllerContext
                .findInstanceDataChildrenByName((DataNodeContainer) cont1Schema, lf11.getLocalName());
        final DataSchemaNode schemaLf = instanceLf.get(0).child;

        cont1Data.withChild(SchemaAwareBuilders.leafBuilder((LeafSchemaNode) schemaLf).withValue(value).build());

        contData.withChild(cont1Data.build());

        final NormalizedNodeContext testNormalizedNodeContext = new NormalizedNodeContext(
                InstanceIdentifierContext.ofStack(SchemaInferenceStack.ofDataTreePath(schemaContext, cont)),
                contData.build());
        return testNormalizedNodeContext;
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
