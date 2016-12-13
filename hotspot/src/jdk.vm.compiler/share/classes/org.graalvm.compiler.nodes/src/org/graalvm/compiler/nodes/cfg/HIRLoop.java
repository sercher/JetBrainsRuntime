/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.nodes.cfg;

import org.graalvm.compiler.core.common.LocationIdentity;
import org.graalvm.compiler.core.common.cfg.Loop;
import org.graalvm.compiler.nodes.LoopBeginNode;

public final class HIRLoop extends Loop<Block> {

    private LocationSet killLocations;

    protected HIRLoop(Loop<Block> parent, int index, Block header) {
        super(parent, index, header);
    }

    @Override
    public long numBackedges() {
        return ((LoopBeginNode) getHeader().getBeginNode()).loopEnds().count();
    }

    public LocationSet getKillLocations() {
        if (killLocations == null) {
            killLocations = new LocationSet();
            for (Block b : this.getBlocks()) {
                if (b.getLoop() == this) {
                    killLocations.addAll(b.getKillLocations());
                    if (killLocations.isAny()) {
                        break;
                    }
                }
            }
        }
        for (Loop<Block> child : this.getChildren()) {
            if (killLocations.isAny()) {
                break;
            }
            killLocations.addAll(((HIRLoop) child).getKillLocations());
        }
        return killLocations;
    }

    public boolean canKill(LocationIdentity location) {
        return getKillLocations().contains(location);
    }

    @Override
    public String toString() {
        return super.toString() + " header:" + getHeader().getBeginNode();
    }
}
