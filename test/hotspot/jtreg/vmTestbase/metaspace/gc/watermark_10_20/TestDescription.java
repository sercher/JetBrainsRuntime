/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase metaspace/gc/watermark_10_20.
 * VM Testbase keywords: [nonconcurrent, no_cds]
 *
 * @comment Don't run test in configurations where we can't reliably count number of metaspace triggered GCs
 * @requires vm.gc != null | !vm.opt.final.ClassUnloadingWithConcurrentMark
 * @requires vm.gc != "G1" | !vm.opt.final.ClassUnloadingWithConcurrentMark
 * @requires vm.gc != "Z"
 * @library /vmTestbase /test/lib
 * @run main/othervm
 *      -Xmx1g
 *      -Xms150m
 *      -Xlog:gc:gc.log
 *      -XX:MetaspaceSize=5m
 *      -XX:MaxMetaspaceSize=25m
 *      -XX:MinMetaspaceFreeRatio=10
 *      -XX:MaxMetaspaceFreeRatio=20
 *      -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:-UseCompressedOops
 *      metaspace.gc.HighWaterMarkTest
 */

