/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter.UNKNOWN_SIZE;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.netconf.sal.rest.api.RestconfNormalizedNodeWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyxmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.api.schema.UserLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.UserMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an experimental iterator over a {@link NormalizedNode}. This is essentially the opposite of a
 * {@link javax.xml.stream.XMLStreamReader} -- unlike instantiating an iterator over the backing data, this
 * encapsulates a {@link NormalizedNodeStreamWriter} and allows us to write multiple nodes.
 *
 * @deprecated This class will be replaced by ParameterAwareNormalizedNodeWriter in restconf-nb-rfc8040
 */
@Deprecated
public class DepthAwareNormalizedNodeWriter implements RestconfNormalizedNodeWriter {
    private final NormalizedNodeStreamWriter writer;
    protected int currentDepth = 0;
    protected final int maxDepth;

    private DepthAwareNormalizedNodeWriter(final NormalizedNodeStreamWriter writer, final int maxDepth) {
        this.writer = requireNonNull(writer);
        this.maxDepth = maxDepth;
    }

    protected final NormalizedNodeStreamWriter getWriter() {
        return writer;
    }

    /**
     * Create a new writer backed by a {@link NormalizedNodeStreamWriter}.
     *
     * @param writer Back-end writer
     * @return A new instance.
     */
    public static DepthAwareNormalizedNodeWriter forStreamWriter(final NormalizedNodeStreamWriter writer,
                                                                 final int maxDepth) {
        return forStreamWriter(writer, true,  maxDepth);
    }

    /**
     * Create a new writer backed by a {@link NormalizedNodeStreamWriter}.
     * Unlike the simple {@link #forStreamWriter(NormalizedNodeStreamWriter, int)}
     * method, this allows the caller to switch off RFC6020 XML compliance, providing better
     * throughput. The reason is that the XML mapping rules in RFC6020 require the encoding
     * to emit leaf nodes which participate in a list's key first and in the order in which
     * they are defined in the key. For JSON, this requirement is completely relaxed and leaves
     * can be ordered in any way we see fit. The former requires a bit of work: first a lookup
     * for each key and then for each emitted node we need to check whether it was already
     * emitted.
     *
     * @param writer Back-end writer
     * @param orderKeyLeaves whether the returned instance should be RFC6020 XML compliant.
     * @return A new instance.
     */
    public static DepthAwareNormalizedNodeWriter forStreamWriter(final NormalizedNodeStreamWriter writer,
                                                                 final boolean orderKeyLeaves, final int maxDepth) {
        return orderKeyLeaves ? new OrderedDepthAwareNormalizedNodeWriter(writer, maxDepth)
                : new DepthAwareNormalizedNodeWriter(writer, maxDepth);
    }

    /**
     * Iterate over the provided {@link NormalizedNode} and emit write
     * events to the encapsulated {@link NormalizedNodeStreamWriter}.
     *
     * @param node Node
     * @return DepthAwareNormalizedNodeWriter
     * @throws IOException when thrown from the backing writer.
     */
    @Override
    public final DepthAwareNormalizedNodeWriter write(final NormalizedNode node) throws IOException {
        if (wasProcessedAsCompositeNode(node)) {
            return this;
        }

        if (wasProcessAsSimpleNode(node)) {
            return this;
        }

        throw new IllegalStateException("It wasn't possible to serialize node " + node);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    /**
     * Emit a best guess of a hint for a particular set of children. It evaluates the
     * iterable to see if the size can be easily gotten to. If it is, we hint at the
     * real number of child nodes. Otherwise we emit UNKNOWN_SIZE.
     *
     * @param children Child nodes
     * @return Best estimate of the collection size required to hold all the children.
     */
    static final int childSizeHint(final Iterable<?> children) {
        return children instanceof Collection ? ((Collection<?>) children).size() : UNKNOWN_SIZE;
    }

    private boolean wasProcessAsSimpleNode(final NormalizedNode node) throws IOException {
        if (node instanceof LeafSetEntryNode) {
            if (currentDepth < maxDepth) {
                final LeafSetEntryNode<?> nodeAsLeafList = (LeafSetEntryNode<?>) node;
                writer.startLeafSetEntryNode(nodeAsLeafList.getIdentifier());
                writer.scalarValue(nodeAsLeafList.body());
                writer.endNode();
            }
            return true;
        } else if (node instanceof LeafNode) {
            final LeafNode<?> nodeAsLeaf = (LeafNode<?>)node;
            writer.startLeafNode(nodeAsLeaf.getIdentifier());
            writer.scalarValue(nodeAsLeaf.body());
            writer.endNode();
            return true;
        } else if (node instanceof AnyxmlNode) {
            final AnyxmlNode<?> anyxmlNode = (AnyxmlNode<?>)node;
            final Class<?> objectModel = anyxmlNode.bodyObjectModel();
            if (writer.startAnyxmlNode(anyxmlNode.getIdentifier(), objectModel)) {
                if (DOMSource.class.isAssignableFrom(objectModel)) {
                    writer.domSourceValue((DOMSource) anyxmlNode.body());
                } else {
                    writer.scalarValue(anyxmlNode.body());
                }
                writer.endNode();
            }
            return true;
        }

        return false;
    }

    /**
     * Emit events for all children and then emit an endNode() event.
     *
     * @param children Child iterable
     * @return True
     * @throws IOException when the writer reports it
     */
    protected final boolean writeChildren(final Iterable<? extends NormalizedNode> children) throws IOException {
        if (currentDepth < maxDepth) {
            for (final NormalizedNode child : children) {
                write(child);
            }
        }
        writer.endNode();
        return true;
    }

    protected boolean writeMapEntryChildren(final MapEntryNode mapEntryNode) throws IOException {
        if (currentDepth < maxDepth) {
            writeChildren(mapEntryNode.body());
        } else if (currentDepth == maxDepth) {
            writeOnlyKeys(mapEntryNode.getIdentifier().entrySet());
        }
        return true;
    }

    private void writeOnlyKeys(final Set<Entry<QName, Object>> entries) throws IOException {
        for (final Entry<QName, Object> entry : entries) {
            writer.startLeafNode(new NodeIdentifier(entry.getKey()));
            writer.scalarValue(entry.getValue());
            writer.endNode();
        }
        writer.endNode();
    }

    protected boolean writeMapEntryNode(final MapEntryNode node) throws IOException {
        writer.startMapEntryNode(node.getIdentifier(), childSizeHint(node.body()));
        currentDepth++;
        writeMapEntryChildren(node);
        currentDepth--;
        return true;
    }

    private boolean wasProcessedAsCompositeNode(final NormalizedNode node) throws IOException {
        boolean processedAsCompositeNode = false;
        if (node instanceof ContainerNode) {
            final ContainerNode n = (ContainerNode) node;
            writer.startContainerNode(n.getIdentifier(), childSizeHint(n.body()));
            currentDepth++;
            processedAsCompositeNode = writeChildren(n.body());
            currentDepth--;
        } else if (node instanceof MapEntryNode) {
            processedAsCompositeNode = writeMapEntryNode((MapEntryNode) node);
        } else if (node instanceof UnkeyedListEntryNode) {
            final UnkeyedListEntryNode n = (UnkeyedListEntryNode) node;
            writer.startUnkeyedListItem(n.getIdentifier(), childSizeHint(n.body()));
            currentDepth++;
            processedAsCompositeNode = writeChildren(n.body());
            currentDepth--;
        } else if (node instanceof ChoiceNode) {
            final ChoiceNode n = (ChoiceNode) node;
            writer.startChoiceNode(n.getIdentifier(), childSizeHint(n.body()));
            processedAsCompositeNode = writeChildren(n.body());
        } else if (node instanceof AugmentationNode) {
            final AugmentationNode n = (AugmentationNode) node;
            writer.startAugmentationNode(n.getIdentifier());
            processedAsCompositeNode = writeChildren(n.body());
        } else if (node instanceof UnkeyedListNode) {
            final UnkeyedListNode n = (UnkeyedListNode) node;
            writer.startUnkeyedList(n.getIdentifier(), childSizeHint(n.body()));
            processedAsCompositeNode = writeChildren(n.body());
        } else if (node instanceof UserMapNode) {
            final UserMapNode n = (UserMapNode) node;
            writer.startOrderedMapNode(n.getIdentifier(), childSizeHint(n.body()));
            processedAsCompositeNode = writeChildren(n.body());
        } else if (node instanceof MapNode) {
            final MapNode n = (MapNode) node;
            writer.startMapNode(n.getIdentifier(), childSizeHint(n.body()));
            processedAsCompositeNode = writeChildren(n.body());
        } else if (node instanceof LeafSetNode) {
            final LeafSetNode<?> n = (LeafSetNode<?>) node;
            if (node instanceof UserLeafSetNode) {
                writer.startOrderedLeafSet(n.getIdentifier(), childSizeHint(n.body()));
            } else {
                writer.startLeafSet(n.getIdentifier(), childSizeHint(n.body()));
            }
            currentDepth++;
            processedAsCompositeNode = writeChildren(n.body());
            currentDepth--;
        }

        return processedAsCompositeNode;
    }

    private static final class OrderedDepthAwareNormalizedNodeWriter extends DepthAwareNormalizedNodeWriter {
        private static final Logger LOG = LoggerFactory.getLogger(OrderedDepthAwareNormalizedNodeWriter.class);

        OrderedDepthAwareNormalizedNodeWriter(final NormalizedNodeStreamWriter writer, final int maxDepth) {
            super(writer, maxDepth);
        }

        @Override
        protected boolean writeMapEntryNode(final MapEntryNode node) throws IOException {
            final NormalizedNodeStreamWriter writer = getWriter();
            writer.startMapEntryNode(node.getIdentifier(), childSizeHint(node.body()));

            final Set<QName> qnames = node.getIdentifier().keySet();
            // Write out all the key children
            for (final QName qname : qnames) {
                final Optional<? extends NormalizedNode> child = node.findChildByArg(new NodeIdentifier(qname));
                if (child.isPresent()) {
                    write(child.get());
                } else {
                    LOG.info("No child for key element {} found", qname);
                }
            }

            // Write all the rest
            currentDepth++;
            final boolean result = writeChildren(Iterables.filter(node.body(), input -> {
                if (input instanceof AugmentationNode) {
                    return true;
                }
                if (!qnames.contains(input.getIdentifier().getNodeType())) {
                    return true;
                }

                LOG.debug("Skipping key child {}", input);
                return false;
            }));
            currentDepth--;
            return result;
        }
    }
}
