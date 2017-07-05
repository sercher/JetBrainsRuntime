/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.dcmd;

import jdk.test.lib.ProcessTools;

import java.util.Arrays;
import java.util.List;

/**
 * Executes Diagnostic Commands on the target VM (specified by pid) using the jcmd tool
 */
public class PidJcmdExecutor extends JcmdExecutor {
    protected final long pid;

    /**
     * Instantiates a new PidJcmdExecutor targeting the current VM
     */
    public PidJcmdExecutor() {
        super();
        try {
            pid = ProcessTools.getProcessId();
        } catch (Exception e) {
            throw new CommandExecutorException("Could not determine own pid", e);
        }
    }

    /**
     * Instantiates a new PidJcmdExecutor targeting the VM indicated by the given pid
     *
     * @param target Pid of the target VM
     */
    public PidJcmdExecutor(String target) {
        super();
        pid = Long.valueOf(target);
    }

    protected List<String> createCommandLine(String cmd) throws CommandExecutorException {
        return Arrays.asList(jcmdBinary, Long.toString(pid), cmd);
    }

}
