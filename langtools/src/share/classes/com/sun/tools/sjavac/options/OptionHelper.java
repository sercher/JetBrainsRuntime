/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.javac.main.CommandLine;
import com.sun.tools.sjavac.Transformer;

/**
 * This class is used to decode sjavac options.
 * See com.sun.tools.sjavac.options.Options for example usage.
 */
public abstract class OptionHelper {

    /** Handle error */
    public abstract void reportError(String msg);

    /** Record a package exclusion pattern */
    public abstract void exclude(String excl);

    /** Record a package inclusion pattern */
    public abstract void include(String incl);

    /** Record a file exclusion */
    public abstract void excludeFile(String exclFile);

    /** Record a file inclusion */
    public abstract void includeFile(String inclFile);

    /** Record a root of sources to be compiled */
    public abstract void sourceRoots(List<Path> path);

    /** Record a suffix + transformer */
    public abstract void addTransformer(String suffix, Transformer tr);

    /** Record a sourcepath to be used */
    public abstract void sourcepath(List<Path> path);

    /** Record a modulepath to be used */
    public abstract void modulepath(List<Path> path);

    /** Record a classpath to be used */
    public abstract void classpath(List<Path> path);

    /** Record the number of cores */
    public abstract void numCores(int parseInt);

    /** Record desired log level */
    public abstract void logLevel(String level);

    /** Record path for reference source list */
    public abstract void compareFoundSources(Path referenceList);

    /** Record a single permitted artifact */
    public abstract void permitArtifact(String f);

    /** Record the fact that unidentified artifacts are permitted */
    public abstract void permitUnidentifiedArtifacts();

    /** Record the fact that sources in the default package are permitted */
    public abstract void permitDefaultPackage();

    /** Record server configuration parameters */
    public abstract void serverConf(String serverConf);

    /** Record server launch configuration parameters */
    public abstract void startServerConf(String serverConf);

    /** Record some arguments to be passed on to javac */
    public abstract void javacArg(String... arg);

    /** Sets the destination directory for the compilation */
    public abstract void destDir(Path dir);

    /** Sets the directory for generated sources */
    public abstract void generatedSourcesDir(Path genSrcDir);

    /** Sets the directory for generated headers */
    public abstract void headerDir(Path dir);

    /** Sets the directory for state and log files generated by sjavac */
    public abstract void stateDir(Path dir);

    /** Sets the implicit policy */
    public abstract void implicit(String policy);


    /**
     * Traverses an array of arguments and performs the appropriate callbacks.
     *
     * @param args the arguments to traverse.
     */
    void traverse(String[] args) {
        try {
            args = CommandLine.parse(args); // Detect @file and load it as a command line.
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Problem reading @"+e.getMessage());
        }
        ArgumentIterator argIter = new ArgumentIterator(Arrays.asList(args));

        nextArg:
        while (argIter.hasNext()) {

            String arg = argIter.next();

            if (arg.startsWith("-")) {
                for (Option opt : Option.values()) {
                    if (opt.processCurrent(argIter, this))
                        continue nextArg;
                }

                javacArg(arg);

                // Does this javac argument take an argument? If so, don't
                // let it pass on to sjavac as a source root directory.
                for (com.sun.tools.javac.main.Option javacOpt : com.sun.tools.javac.main.Option.values()) {
                    if (javacOpt.matches(arg)) {
                        boolean takesArgument = javacOpt.hasArg();
                        boolean separateToken = !arg.contains(":") && !arg.contains("=");
                        if (takesArgument && separateToken)
                            javacArg(argIter.next());
                    }
                }
            } else {
                sourceRoots(Arrays.asList(Paths.get(arg)));
            }
        }
    }
}
