/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.graalvm.compiler.hotspot.nodes;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_20;
import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_30;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_20;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_30;

import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.hotspot.replacements.ObjectCloneNode;
import org.graalvm.compiler.nodeinfo.NodeCycles;
import org.graalvm.compiler.nodeinfo.NodeSize;
import org.graalvm.compiler.nodes.spi.DefaultNodeCostProvider;
import org.graalvm.compiler.nodes.type.StampTool;

import jdk.vm.ci.meta.ResolvedJavaType;

public abstract class HotSpotNodeCostProvider extends DefaultNodeCostProvider {

    @Override
    public NodeSize size(Node n) {
        if (n instanceof ObjectCloneNode) {
            ResolvedJavaType type = StampTool.typeOrNull(((ObjectCloneNode) n).getObject());
            if (type != null) {
                if (type.isArray()) {
                    return SIZE_30;
                } else {
                    return SIZE_20;
                }
            }
        }
        return super.size(n);
    }

    @Override
    public NodeCycles cycles(Node n) {
        if (n instanceof ObjectCloneNode) {
            ResolvedJavaType type = StampTool.typeOrNull(((ObjectCloneNode) n).getObject());
            if (type != null) {
                if (type.isArray()) {
                    return CYCLES_30;
                } else {
                    return CYCLES_20;
                }
            }
        }
        return super.cycles(n);
    }

}
