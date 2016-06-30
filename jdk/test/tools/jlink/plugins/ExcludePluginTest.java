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

/*
 * @test
 * @summary Test exclude plugin
 * @author Jean-Francois Denise
 * @modules jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 * @run main ExcludePluginTest
 */

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import jdk.tools.jlink.internal.ModulePoolImpl;

import jdk.tools.jlink.internal.plugins.ExcludePlugin;
import jdk.tools.jlink.plugin.ModuleEntry;
import jdk.tools.jlink.plugin.ModulePool;

public class ExcludePluginTest {

    public static void main(String[] args) throws Exception {
        new ExcludePluginTest().test();
    }

    public void test() throws Exception {
        check("**.jcov", "/num/toto.jcov", true);
        check("**.jcov", "/toto.jcov/", true);
        check("**.jcov", "/toto.jcov/tutu/tata", false);
        check("/java.base/*.jcov", "/java.base/toto.jcov", true);
        check("/java.base/toto.jcov", "/tjava.base/iti.jcov", false);
        check("/java.base/*/toto.jcov", "/java.base/toto.jcov", false);
        check("/java.base/*/toto.jcov", "/java.base/tutu/toto.jcov", true);
        check("**/java.base/*/toto.jcov", "/tutu/java.base/tutu/toto.jcov", true);
        check("/META-INF/**", "/META-INF/services/  MyProvider ", true);
        check("/META-INF/**", "/META-INF/services/MyProvider", true);
        check("**/META-INF", "/ META-INF/services/MyProvider", false);
        check("**/META-INF/**", "/java.base//META-INF/services/MyProvider", true);
        check("/java.base/*/Toto$Titi.class", "/java.base/tutu/Toto$Titi.class", true);
        check("/**$**.class", "/java.base/tutu/Toto$Titi.class", true);
        check("**$**.class", "/java.base/tutu/Toto$Titi.class", true);

        // Excluded resource list in a file
        File order = new File("resources.exc");
        order.createNewFile();
        Files.write(order.toPath(), "**.jcov".getBytes());
        check("@" + order.getAbsolutePath(), "/num/toto.jcov", true);
    }

    public void check(String s, String sample, boolean exclude) throws Exception {
        Map<String, String> prop = new HashMap<>();
        prop.put(ExcludePlugin.NAME, s);
        ExcludePlugin excludePlugin = new ExcludePlugin();
        excludePlugin.configure(prop);
        ModulePool resources = new ModulePoolImpl();
        ModuleEntry resource = ModuleEntry.create(sample, new byte[0]);
        resources.add(resource);
        ModulePool result = new ModulePoolImpl();
        excludePlugin.visit(resources, result);
        if (exclude) {
            if (result.contains(resource)) {
                throw new AssertionError(sample + " should be excluded by " + s);
            }
        } else {
            if (!result.contains(resource)) {
                throw new AssertionError(sample + " shouldn't be excluded by " + s);
            }
        }
    }
}
