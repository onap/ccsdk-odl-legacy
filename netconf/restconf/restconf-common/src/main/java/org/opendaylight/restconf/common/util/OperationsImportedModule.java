/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.common.util;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.UnresolvedQName.Unqualified;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DataTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ExtensionEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.FeatureEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IdentityEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SubmoduleEffectiveStatement;

final class OperationsImportedModule extends AbstractOperationsModule {
    private final Module original;

    OperationsImportedModule(final Module original) {
        this.original = requireNonNull(original);
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public QNameModule getQNameModule() {
        return original.getQNameModule();
    }

    @Override
    public String getPrefix() {
        return original.getPrefix();
    }

    @Override
    public Collection<DataSchemaNode> getChildNodes() {
        return List.of();
    }

    @Override
    public DataSchemaNode dataChildByName(final QName name) {
        return null;
    }

    @Override
    public List<EffectiveStatement<?, ?>> effectiveSubstatements() {
        return List.of();
    }

    @Override
    public @NonNull Optional<ExtensionEffectiveStatement> findExtension(@NonNull QName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findExtension'");
    }

    @Override
    public @NonNull Optional<FeatureEffectiveStatement> findFeature(@NonNull QName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findFeature'");
    }

    @Override
    public @NonNull Optional<IdentityEffectiveStatement> findIdentity(@NonNull QName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findIdentity'");
    }

    @Override
    public @NonNull Optional<SubmoduleEffectiveStatement> findSubmodule(@NonNull Unqualified arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findSubmodule'");
    }

    @Override
    public @NonNull Collection<DataTreeEffectiveStatement<?>> dataTreeNodes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'dataTreeNodes'");
    }

    @Override
    public @NonNull Optional<DataTreeEffectiveStatement<?>> findDataTreeNode(@NonNull QName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findDataTreeNode'");
    }

    @Override
    public @NonNull Optional<SchemaTreeEffectiveStatement<?>> findSchemaTreeNode(@NonNull QName arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findSchemaTreeNode'");
    }

    @Override
    public @NonNull Collection<SchemaTreeEffectiveStatement<?>> schemaTreeNodes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'schemaTreeNodes'");
    }

    @Override
    public @NonNull Unqualified argument() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'argument'");
    }

    @Override
    public @NonNull Optional<String> findNamespacePrefix(@NonNull QNameModule arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findNamespacePrefix'");
    }

    @Override
    public @NonNull Optional<ModuleEffectiveStatement> findReachableModule(@NonNull String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findReachableModule'");
    }

    @Override
    public Collection<Entry<QNameModule, String>> namespacePrefixes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'namespacePrefixes'");
    }

    @Override
    public @NonNull Collection<Entry<String, ModuleEffectiveStatement>> reachableModules() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reachableModules'");
    }
}
