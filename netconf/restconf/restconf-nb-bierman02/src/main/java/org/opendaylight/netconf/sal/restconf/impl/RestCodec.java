/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.restconf.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.netconf.sal.rest.impl.StringModuleInstanceIdentifierCodec;
import org.opendaylight.restconf.common.util.IdentityValuesDTO;
import org.opendaylight.restconf.common.util.IdentityValuesDTO.IdentityValue;
import org.opendaylight.restconf.common.util.IdentityValuesDTO.Predicate;
import org.opendaylight.restconf.common.util.RestUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.data.api.codec.IllegalArgumentCodec;
import org.opendaylight.yangtools.yang.data.api.codec.InstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RestCodec {

    private static final Logger LOG = LoggerFactory.getLogger(RestCodec.class);

    private RestCodec() {
    }

    // FIXME: IllegalArgumentCodec is not quite accurate
    public static IllegalArgumentCodec<Object, Object> from(final TypeDefinition<?> typeDefinition,
            final DOMMountPoint mountPoint, final ControllerContext controllerContext) {
        return new ObjectCodec(typeDefinition, mountPoint, controllerContext);
    }

    @SuppressWarnings("rawtypes")
    public static final class ObjectCodec implements IllegalArgumentCodec<Object, Object> {

        private static final Logger LOG = LoggerFactory.getLogger(ObjectCodec.class);

        public static final IllegalArgumentCodec LEAFREF_DEFAULT_CODEC = new LeafrefCodecImpl();

        private final ControllerContext controllerContext;
        private final IllegalArgumentCodec instanceIdentifier;
        private final IllegalArgumentCodec identityrefCodec;

        private final TypeDefinition<?> type;

        private ObjectCodec(final TypeDefinition<?> typeDefinition, final DOMMountPoint mountPoint,
                final ControllerContext controllerContext) {
            this.controllerContext = controllerContext;
            type = RestUtil.resolveBaseTypeFrom(typeDefinition);
            if (type instanceof IdentityrefTypeDefinition) {
                identityrefCodec = new IdentityrefCodecImpl(mountPoint, controllerContext);
            } else {
                identityrefCodec = null;
            }
            if (type instanceof InstanceIdentifierTypeDefinition) {
                instanceIdentifier = new InstanceIdentifierCodecImpl(mountPoint, controllerContext);
            } else {
                instanceIdentifier = null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "Legacy code")
        public Object deserialize(final Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    if (input instanceof IdentityValuesDTO) {
                        return identityrefCodec.deserialize(input);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(
                            "Value is not instance of IdentityrefTypeDefinition but is {}. "
                                    + "Therefore NULL is used as translation of  - {}",
                            input == null ? "null" : input.getClass(), String.valueOf(input));
                    }
                    // FIXME: this should be a hard error
                    return null;
                } else if (type instanceof InstanceIdentifierTypeDefinition) {
                    if (input instanceof IdentityValuesDTO) {
                        return instanceIdentifier.deserialize(input);
                    } else {
                        final StringModuleInstanceIdentifierCodec codec = new StringModuleInstanceIdentifierCodec(
                                controllerContext.getGlobalSchema());
                        return codec.deserialize((String) input);
                    }
                } else {
                    final TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> typeAwarecodec =
                            TypeDefinitionAwareCodec.from(type);
                    if (typeAwarecodec != null) {
                        if (input instanceof IdentityValuesDTO) {
                            return typeAwarecodec.deserialize(((IdentityValuesDTO) input).getOriginValue());
                        }
                        return typeAwarecodec.deserialize(String.valueOf(input));
                    } else {
                        LOG.debug("Codec for type \"{}\" is not implemented yet.", type.getQName().getLocalName());
                        // FIXME: this should be a hard error
                        return null;
                    }
                }
            } catch (final ClassCastException e) { // TODO remove this catch when everyone use codecs
                LOG.error("ClassCastException was thrown when codec is invoked with parameter {}", input, e);
                // FIXME: this should be a hard error
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "legacy code")
        public Object serialize(final Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    return identityrefCodec.serialize(input);
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.serialize(input);
                } else if (type instanceof InstanceIdentifierTypeDefinition) {
                    return instanceIdentifier.serialize(input);
                } else {
                    final TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> typeAwarecodec =
                            TypeDefinitionAwareCodec.from(type);
                    if (typeAwarecodec != null) {
                        return typeAwarecodec.serialize(input);
                    } else {
                        LOG.debug("Codec for type \"{}\" is not implemented yet.", type.getQName().getLocalName());
                        return null;
                    }
                }
            } catch (final ClassCastException e) {
                // FIXME: remove this catch when everyone use codecs
                LOG.error("ClassCastException was thrown when codec is invoked with parameter {}", input, e);
                // FIXME: this should be a hard error
                return input;
            }
        }

    }

    public static class IdentityrefCodecImpl implements IdentityrefCodec<IdentityValuesDTO> {
        private static final Logger LOG = LoggerFactory.getLogger(IdentityrefCodecImpl.class);

        private final DOMMountPoint mountPoint;
        private final ControllerContext controllerContext;

        public IdentityrefCodecImpl(final DOMMountPoint mountPoint, final ControllerContext controllerContext) {
            this.mountPoint = mountPoint;
            this.controllerContext = controllerContext;
        }

        @Override
        public IdentityValuesDTO serialize(final QName data) {
            return new IdentityValuesDTO(data.getNamespace().toString(), data.getLocalName(), null, null);
        }

        @Override
        @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "See FIXME below")
        public QName deserialize(final IdentityValuesDTO data) {
            final IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            final Module module = getModuleByNamespace(valueWithNamespace.getNamespace(), mountPoint,
                    controllerContext);
            if (module == null) {
                // FIXME: this should be a hard error
                LOG.info("Module was not found for namespace {}", valueWithNamespace.getNamespace());
                LOG.info("Idenetityref will be translated as NULL for data - {}", String.valueOf(valueWithNamespace));
                return null;
            }

            return QName.create(module.getNamespace(), module.getRevision(), valueWithNamespace.getValue());
        }
    }

    public static class LeafrefCodecImpl implements LeafrefCodec<String> {

        @Override
        public String serialize(final Object data) {
            return String.valueOf(data);
        }

        @Override
        public Object deserialize(final String data) {
            return data;
        }

    }

    public static class InstanceIdentifierCodecImpl implements InstanceIdentifierCodec<IdentityValuesDTO> {
        private static final Logger LOG = LoggerFactory.getLogger(InstanceIdentifierCodecImpl.class);

        private final DOMMountPoint mountPoint;
        private final ControllerContext controllerContext;

        public InstanceIdentifierCodecImpl(final DOMMountPoint mountPoint,
                final ControllerContext controllerContext) {
            this.mountPoint = mountPoint;
            this.controllerContext = controllerContext;
        }

        @Override
        public IdentityValuesDTO serialize(final YangInstanceIdentifier data) {
            final IdentityValuesDTO identityValuesDTO = new IdentityValuesDTO();
            for (final PathArgument pathArgument : data.getPathArguments()) {
                final IdentityValue identityValue = qNameToIdentityValue(pathArgument.getNodeType());
                if (pathArgument instanceof NodeIdentifierWithPredicates && identityValue != null) {
                    final List<Predicate> predicates =
                            keyValuesToPredicateList(((NodeIdentifierWithPredicates) pathArgument).entrySet());
                    identityValue.setPredicates(predicates);
                } else if (pathArgument instanceof NodeWithValue && identityValue != null) {
                    final List<Predicate> predicates = new ArrayList<>();
                    final String value = String.valueOf(((NodeWithValue<?>) pathArgument).getValue());
                    predicates.add(new Predicate(null, value));
                    identityValue.setPredicates(predicates);
                }
                identityValuesDTO.add(identityValue);
            }
            return identityValuesDTO;
        }

        @SuppressFBWarnings(value = { "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "NP_NONNULL_RETURN_VIOLATION" },
                justification = "Unrecognised NullableDecl")
        @Override
        public YangInstanceIdentifier deserialize(final IdentityValuesDTO data) {
            final List<PathArgument> result = new ArrayList<>();
            final IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            final Module module = getModuleByNamespace(valueWithNamespace.getNamespace(), mountPoint,
                    controllerContext);
            if (module == null) {
                LOG.info("Module by namespace '{}' of first node in instance-identifier was not found.",
                        valueWithNamespace.getNamespace());
                LOG.info("Instance-identifier will be translated as NULL for data - {}",
                        String.valueOf(valueWithNamespace.getValue()));
                // FIXME: this should be a hard error
                return null;
            }

            DataNodeContainer parentContainer = module;
            final List<IdentityValue> identities = data.getValuesWithNamespaces();
            for (int i = 0; i < identities.size(); i++) {
                final IdentityValue identityValue = identities.get(i);
                XMLNamespace validNamespace = resolveValidNamespace(identityValue.getNamespace(), mountPoint,
                        controllerContext);
                final var found = ControllerContext.findInstanceDataChildByNameAndNamespace(
                    parentContainer, identityValue.getValue(), validNamespace);
                if (found == null) {
                    LOG.info("'{}' node was not found in {}", identityValue, parentContainer.getChildNodes());
                    LOG.info("Instance-identifier will be translated as NULL for data - {}",
                            String.valueOf(identityValue.getValue()));
                    // FIXME: this should be a hard error
                    return null;
                }
                final DataSchemaNode node = found.child;
                final QName qName = node.getQName();
                PathArgument pathArgument = null;
                if (identityValue.getPredicates().isEmpty()) {
                    pathArgument = new NodeIdentifier(qName);
                } else {
                    if (node instanceof LeafListSchemaNode) { // predicate is value of leaf-list entry
                        final Predicate leafListPredicate = identityValue.getPredicates().get(0);
                        if (!leafListPredicate.isLeafList()) {
                            LOG.info("Predicate's data is not type of leaf-list. It should be in format \".='value'\"");
                            LOG.info("Instance-identifier will be translated as NULL for data - {}",
                                    String.valueOf(identityValue.getValue()));
                            // FIXME: this should be a hard error
                            return null;
                        }
                        pathArgument = new NodeWithValue<>(qName, leafListPredicate.getValue());
                    } else if (node instanceof ListSchemaNode) { // predicates are keys of list
                        final DataNodeContainer listNode = (DataNodeContainer) node;
                        final Map<QName, Object> predicatesMap = new HashMap<>();
                        for (final Predicate predicate : identityValue.getPredicates()) {
                            validNamespace = resolveValidNamespace(predicate.getName().getNamespace(), mountPoint,
                                    controllerContext);
                            final var listKey = ControllerContext
                                    .findInstanceDataChildByNameAndNamespace(listNode, predicate.getName().getValue(),
                                            validNamespace);
                            predicatesMap.put(listKey.child.getQName(), predicate.getValue());
                        }
                        pathArgument = NodeIdentifierWithPredicates.of(qName, predicatesMap);
                    } else {
                        LOG.info("Node {} is not List or Leaf-list.", node);
                        LOG.info("Instance-identifier will be translated as NULL for data - {}",
                                String.valueOf(identityValue.getValue()));
                        // FIXME: this should be a hard error
                        return null;
                    }
                }
                result.add(pathArgument);
                if (i < identities.size() - 1) { // last element in instance-identifier can be other than
                    // DataNodeContainer
                    if (node instanceof DataNodeContainer) {
                        parentContainer = (DataNodeContainer) node;
                    } else {
                        LOG.info("Node {} isn't instance of DataNodeContainer", node);
                        LOG.info("Instance-identifier will be translated as NULL for data - {}",
                                String.valueOf(identityValue.getValue()));
                        // FIXME: this should be a hard error
                        return null;
                    }
                }
            }

            return result.isEmpty() ? null : YangInstanceIdentifier.create(result);
        }

        private static List<Predicate> keyValuesToPredicateList(final Set<Entry<QName, Object>> keyValues) {
            final List<Predicate> result = new ArrayList<>();
            for (final Entry<QName, Object> entry : keyValues) {
                final QName qualifiedName = entry.getKey();
                final Object value = entry.getValue();
                result.add(new Predicate(qNameToIdentityValue(qualifiedName), String.valueOf(value)));
            }
            return result;
        }

        private static IdentityValue qNameToIdentityValue(final QName qualifiedName) {
            if (qualifiedName != null) {
                return new IdentityValue(qualifiedName.getNamespace().toString(), qualifiedName.getLocalName());
            }
            return null;
        }
    }

    private static Module getModuleByNamespace(final String namespace, final DOMMountPoint mountPoint,
            final ControllerContext controllerContext) {
        final XMLNamespace validNamespace = resolveValidNamespace(namespace, mountPoint, controllerContext);

        Module module = null;
        if (mountPoint != null) {
            module = ControllerContext.findModuleByNamespace(mountPoint, validNamespace);
        } else {
            module = controllerContext.findModuleByNamespace(validNamespace);
        }
        if (module == null) {
            LOG.info("Module for namespace {} was not found.", validNamespace);
            return null;
        }
        return module;
    }

    private static XMLNamespace resolveValidNamespace(final String namespace, final DOMMountPoint mountPoint,
            final ControllerContext controllerContext) {
        XMLNamespace validNamespace;
        if (mountPoint != null) {
            validNamespace = ControllerContext.findNamespaceByModuleName(mountPoint, namespace);
        } else {
            validNamespace = controllerContext.findNamespaceByModuleName(namespace);
        }
        if (validNamespace == null) {
            validNamespace = XMLNamespace.of(namespace);
        }

        return validNamespace;
    }
}
