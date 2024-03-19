/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.restconf.rev131019;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.DatastoreIdentifier;

/**
 **/
public class DatastoreIdentifierBuilder {

    public static DatastoreIdentifier getDefaultInstance(final String defaultValue) {
        QName qName = QName.create(defaultValue);
        return DatastoreIdentifier.of(qName);
    }

}
