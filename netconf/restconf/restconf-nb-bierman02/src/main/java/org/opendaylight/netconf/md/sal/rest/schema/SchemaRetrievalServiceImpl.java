/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.md.sal.rest.schema;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.restconf.common.schema.SchemaExportContext;
import org.opendaylight.restconf.common.validation.RestconfValidationUtils;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class SchemaRetrievalServiceImpl implements SchemaRetrievalService {

    private final ControllerContext salContext;

    private static final Splitter SLASH_SPLITTER = Splitter.on("/");
    private static final String MOUNT_ARG = ControllerContext.MOUNT;

    public SchemaRetrievalServiceImpl(final ControllerContext controllerContext) {
        salContext = controllerContext;
    }


    @Override
    public SchemaExportContext getSchema(final String mountAndModule) {
        final SchemaContext schemaContext;
        final Iterable<String> pathComponents = SLASH_SPLITTER.split(mountAndModule);
        final Iterator<String> componentIter = pathComponents.iterator();
        if (!Iterables.contains(pathComponents, MOUNT_ARG)) {
            schemaContext = salContext.getGlobalSchema();
        } else {
            final StringBuilder pathBuilder = new StringBuilder();
            while (componentIter.hasNext()) {
                final String current = componentIter.next();
                // It is argument, not last element.
                if (pathBuilder.length() != 0) {
                    pathBuilder.append("/");
                }
                pathBuilder.append(current);
                if (MOUNT_ARG.equals(current)) {
                    // We stop right at mountpoint, last two arguments should
                    // be module name and revision
                    break;
                }
            }
            schemaContext = getMountSchemaContext(pathBuilder.toString());

        }

        RestconfDocumentedException.throwIf(!componentIter.hasNext(), "Module name must be supplied.",
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        final String moduleName = componentIter.next();
        RestconfDocumentedException.throwIf(!componentIter.hasNext(), "Revision date must be supplied.",
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        final String revisionString = componentIter.next();
        return getExportUsingNameAndRevision(schemaContext, moduleName, revisionString,
                salContext.getYangTextSourceProvider());
    }

    private static SchemaExportContext getExportUsingNameAndRevision(final SchemaContext schemaContext,
             final String moduleName, final String revisionStr,
             final DOMYangTextSourceProvider yangTextSourceProvider) {
        try {
            final Module module = schemaContext.findModule(moduleName, Revision.of(revisionStr)).orElse(null);
            return new SchemaExportContext(
                    schemaContext, RestconfValidationUtils.checkNotNullDocumented(module, moduleName),
                    yangTextSourceProvider);
        } catch (final DateTimeParseException e) {
            throw new RestconfDocumentedException("Supplied revision is not in expected date format YYYY-mm-dd", e);
        }
    }

    private SchemaContext getMountSchemaContext(final String identifier) {
        final InstanceIdentifierContext mountContext = salContext.toMountPointIdentifier(identifier);
        return mountContext.getSchemaContext();
    }
}

