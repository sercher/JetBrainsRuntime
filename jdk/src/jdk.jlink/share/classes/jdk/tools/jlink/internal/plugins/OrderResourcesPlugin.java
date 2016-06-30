/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.tools.jlink.internal.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ModuleEntry;
import jdk.tools.jlink.plugin.ModulePool;
import jdk.tools.jlink.plugin.TransformerPlugin;
import jdk.tools.jlink.internal.Utils;

/**
 *
 * Order Resources plugin
 */
public final class OrderResourcesPlugin implements TransformerPlugin {
    public static final String NAME = "order-resources";
    private static final FileSystem JRT_FILE_SYSTEM = Utils.jrtFileSystem();

    private final List<ToIntFunction<String>> filters;
    private final Map<String, Integer> orderedPaths;

    public OrderResourcesPlugin() {
        this.filters = new ArrayList<>();
        this.orderedPaths = new HashMap<>();
    }

    @Override
    public String getName() {
        return NAME;
    }

    static class SortWrapper {
        private final ModuleEntry resource;
        private final int ordinal;

        SortWrapper(ModuleEntry resource, int ordinal) {
            this.resource = resource;
            this.ordinal = ordinal;
        }

        ModuleEntry getResource() {
            return resource;
        }

        String getPath() {
            return resource.getPath();
        }

        int getOrdinal() {
            return ordinal;
        }
    }

    private String stripModule(String path) {
        if (path.startsWith("/")) {
            int index = path.indexOf('/', 1);

            if (index != -1) {
                return path.substring(index + 1);
            }
        }

        return path;
    }

    private int getOrdinal(ModuleEntry resource) {
        String path = resource.getPath();

        Integer value = orderedPaths.get(stripModule(path));

        if (value != null) {
            return value;
        }

        for (ToIntFunction<String> function : filters) {
            int ordinal = function.applyAsInt(path);

            if (ordinal != Integer.MAX_VALUE) {
                return ordinal;
            }
        }

        return Integer.MAX_VALUE;
    }

    private static int compare(SortWrapper wrapper1, SortWrapper wrapper2) {
        int compare = wrapper1.getOrdinal() - wrapper2.getOrdinal();

        if (compare != 0) {
            return compare;
        }

        return wrapper1.getPath().compareTo(wrapper2.getPath());
    }

    @Override
    public void visit(ModulePool in, ModulePool out) {
        in.entries()
                .filter(resource -> resource.getType()
                        .equals(ModuleEntry.Type.CLASS_OR_RESOURCE))
                .map((resource) -> new SortWrapper(resource, getOrdinal(resource)))
                .sorted(OrderResourcesPlugin::compare)
                .forEach((wrapper) -> out.add(wrapper.getResource()));
        in.entries()
                .filter(other -> !other.getType()
                        .equals(ModuleEntry.Type.CLASS_OR_RESOURCE))
                .forEach((other) -> out.add(other));
    }

    @Override
    public Category getType() {
        return Category.SORTER;
    }

    @Override
    public String getDescription() {
        return PluginsResourceBundle.getDescription(NAME);
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public String getArgumentsDescription() {
       return PluginsResourceBundle.getArgument(NAME);
    }

    @Override
    public void configure(Map<String, String> config) {
        List<String> patterns = Utils.parseList(config.get(NAME));
        int ordinal = 0;

        for (String pattern : patterns) {
            if (pattern.startsWith("@")) {
                File file = new File(pattern.substring(1));

                if (file.exists()) {
                    List<String> lines;

                    try {
                        lines = Files.readAllLines(file.toPath());
                    } catch (IOException ex) {
                        throw new PluginException(ex);
                    }

                    for (String line : lines) {
                        if (!line.startsWith("#")) {
                            orderedPaths.put(line + ".class", ordinal++);
                        }
                    }
                }
            } else {
                final int result = ordinal++;
                final PathMatcher matcher = Utils.getPathMatcher(JRT_FILE_SYSTEM, pattern);
                ToIntFunction<String> function = (path)-> matcher.matches(JRT_FILE_SYSTEM.getPath(path)) ? result : Integer.MAX_VALUE;
                filters.add(function);
             }
        }
    }
}
