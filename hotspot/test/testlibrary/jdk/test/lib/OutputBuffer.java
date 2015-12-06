/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

/**
 * @deprecated This class is deprecated. Use the one from
 *             {@code <root>/test/lib/share/classes/jdk/test/lib/process}
 */
@Deprecated
public class OutputBuffer {
  private final String stdout;
  private final String stderr;

  /**
   * Create an OutputBuffer, a class for storing and managing stdout and stderr
   * results separately
   *
   * @param stdout stdout result
   * @param stderr stderr result
   */
  public OutputBuffer(String stdout, String stderr) {
    this.stdout = stdout;
    this.stderr = stderr;
  }

  /**
   * Returns the stdout result
   *
   * @return stdout result
   */
  public String getStdout() {
    return stdout;
  }

  /**
   * Returns the stderr result
   *
   * @return stderr result
   */
  public String getStderr() {
    return stderr;
  }
}
