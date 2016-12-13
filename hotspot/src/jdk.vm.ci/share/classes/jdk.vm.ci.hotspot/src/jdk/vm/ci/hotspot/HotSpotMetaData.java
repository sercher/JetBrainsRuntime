/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;

import jdk.vm.ci.code.TargetDescription;

public class HotSpotMetaData {
    private byte[] pcDescBytes;
    private byte[] scopesDescBytes;
    private byte[] relocBytes;
    private byte[] exceptionBytes;
    private byte[] oopMaps;
    private String[] metadata;

    public HotSpotMetaData(TargetDescription target, HotSpotCompiledCode compiledMethod) {
        // Assign the fields default values...
        pcDescBytes = new byte[0];
        scopesDescBytes = new byte[0];
        relocBytes = new byte[0];
        exceptionBytes = new byte[0];
        oopMaps = new byte[0];
        metadata = new String[0];
        // ...some of them will be overwritten by the VM:
        runtime().getCompilerToVM().getMetadata(target, compiledMethod, this);
    }

    public byte[] pcDescBytes() {
        return pcDescBytes;
    }

    public byte[] scopesDescBytes() {
        return scopesDescBytes;
    }

    public byte[] relocBytes() {
        return relocBytes;
    }

    public byte[] exceptionBytes() {
        return exceptionBytes;
    }

    public byte[] oopMaps() {
        return oopMaps;
    }

    public String[] metadataEntries() {
        return metadata;
    }
}
