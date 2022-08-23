/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

/**
 * @test TestAlwaysPretouch
 * @requires vm.gc.Epsilon
 * @summary Test that pre-touch works
 *
 * @run main/othervm -Xms64m -Xmx256m
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 *
 * @run main/othervm -Xms64m -Xmx256m -XX:-AlwaysPreTouch
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 *
 * @run main/othervm -Xms64m -Xmx256m -XX:+AlwaysPreTouch
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 *
 * @run main/othervm -Xmx256m
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 *
 * @run main/othervm -Xmx256m -XX:-AlwaysPreTouch
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 *
 * @run main/othervm -Xmx256m -XX:+AlwaysPreTouch
 *                   -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
 *                   gc.epsilon.TestAlwaysPretouch
 */

package gc.epsilon;

public class TestAlwaysPretouch {
  public static void main(String[] args) throws Exception {
    // everything should happen before entry point
  }
}
