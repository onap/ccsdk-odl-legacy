/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.sal.rest.impl;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.netconf.sal.rest.api.RestconfConstants;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;

/**
 * JAX-RS Provider.
 *
 * @deprecated This class will be replaced by AbstractIdentifierAwareJaxRsProvider in restconf-nb-rfc8040
 */
@Deprecated
public class AbstractIdentifierAwareJaxRsProvider {

    private static final String POST = "POST";

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    private final ControllerContext controllerContext;

    protected AbstractIdentifierAwareJaxRsProvider(final ControllerContext controllerContext) {
        this.controllerContext = controllerContext;
    }

    protected final String getIdentifier() {
        return uriInfo.getPathParameters(false).getFirst(RestconfConstants.IDENTIFIER);
    }

    protected InstanceIdentifierContext getInstanceIdentifierContext() {
        return controllerContext.toInstanceIdentifier(getIdentifier());
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    protected boolean isPost() {
        return POST.equals(request.getMethod());
    }

    protected ControllerContext getControllerContext() {
        return controllerContext;
    }

    Request getRequest() {
        return request;
    }
}
