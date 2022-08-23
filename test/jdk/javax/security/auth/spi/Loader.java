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

import javax.security.auth.login.LoginContext;
import java.io.File;

/*
 * @test
 * @bug 8047789 8273026
 * @summary auth.login.LoginContext needs to be updated to work with modules
 * @comment shows that the SecondLoginModule is still needed even if it's not in the JAAS login config file
 * @build FirstLoginModule
 * @clean SecondLoginModule
 * @run main/othervm/fail Loader
 * @build SecondLoginModule
 * @run main/othervm Loader
 */
public class Loader {

    public static void main(String[] args) throws Exception {

        System.setProperty("java.security.auth.login.config",
                new File(System.getProperty("test.src"), "sl.conf").toString());
        LoginContext lc = new LoginContext("me");

        lc.login();

    }
}
