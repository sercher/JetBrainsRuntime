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
package jdk.vm.ci.hotspot;

import jdk.vm.ci.hotspot.HotSpotVMConfig.CompressEncoding;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;

/**
 * HotSpot specific extension of {@link MemoryAccessProvider}.
 */
public interface HotSpotMemoryAccessProvider extends MemoryAccessProvider {

    JavaConstant readNarrowOopConstant(Constant base, long displacement, CompressEncoding encoding);

    Constant readKlassPointerConstant(Constant base, long displacement);

    Constant readNarrowKlassPointerConstant(Constant base, long displacement, CompressEncoding encoding);

    Constant readMethodPointerConstant(Constant base, long displacement);

    Constant readSymbolConstant(Constant base, long displacement);
}
