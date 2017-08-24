/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.xml.internal.messaging.saaj.soap.impl;

import com.sun.xml.internal.messaging.saaj.soap.SOAPDocumentImpl;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Objects;

/**
 * Node list wrapper, finding SOAP elements automatically when possible.
 *
 * @author Roman Grigoriadi
 */
public class NodeListImpl implements NodeList {

    private final SOAPDocumentImpl soapDocument;

    private final NodeList nodeList;

    public NodeListImpl(SOAPDocumentImpl soapDocument, NodeList nodeList) {
        Objects.requireNonNull(soapDocument);
        Objects.requireNonNull(soapDocument);
        this.soapDocument = soapDocument;
        this.nodeList = nodeList;
    }

    @Override
    public Node item(int index) {
        return soapDocument.findIfPresent(nodeList.item(index));
    }

    @Override
    public int getLength() {
        return nodeList.getLength();
    }
}
