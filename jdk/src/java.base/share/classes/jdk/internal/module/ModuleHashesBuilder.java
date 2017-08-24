/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.io.PrintStream;
import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import static java.util.stream.Collectors.*;

/**
 * A Builder to compute ModuleHashes from a given configuration
 */
public class ModuleHashesBuilder {
    private final Configuration configuration;
    private final Set<String> hashModuleCandidates;

    /**
     * Constructs a ModuleHashesBuilder that finds the packaged modules
     * from the location of ModuleReference found from the given Configuration.
     *
     * @param config Configuration for building module hashes
     * @param modules the candidate modules to be hashed
     */
    public ModuleHashesBuilder(Configuration config, Set<String> modules) {
        this.configuration = config;
        this.hashModuleCandidates = modules;
    }

    /**
     * Returns a map of a module M to ModuleHashes for the modules
     * that depend upon M directly or indirectly.
     *
     * The key for each entry in the returned map is a module M that has
     * no outgoing edges to any of the candidate modules to be hashed
     * i.e. M is a leaf node in a connected subgraph containing M and
     * other candidate modules from the module graph filtering
     * the outgoing edges from M to non-candidate modules.
     */
    public Map<String, ModuleHashes> computeHashes(Set<String> roots) {
        // build a graph containing the the packaged modules and
        // its transitive dependences matching --hash-modules
        Graph.Builder<String> builder = new Graph.Builder<>();
        Deque<ResolvedModule> deque = new ArrayDeque<>(configuration.modules());
        Set<ResolvedModule> visited = new HashSet<>();
        while (!deque.isEmpty()) {
            ResolvedModule rm = deque.pop();
            if (!visited.contains(rm)) {
                visited.add(rm);
                builder.addNode(rm.name());
                for (ResolvedModule dm : rm.reads()) {
                    if (!visited.contains(dm)) {
                        deque.push(dm);
                    }
                    builder.addEdge(rm.name(), dm.name());
                }
            }
        }

        // each node in a transposed graph is a matching packaged module
        // in which the hash of the modules that depend upon it is recorded
        Graph<String> transposedGraph = builder.build().transpose();

        // traverse the modules in topological order that will identify
        // the modules to record the hashes - it is the first matching
        // module and has not been hashed during the traversal.
        Set<String> mods = new HashSet<>();
        Map<String, ModuleHashes> hashes = new HashMap<>();
        builder.build()
               .orderedNodes()
               .filter(mn -> roots.contains(mn) && !mods.contains(mn))
               .forEach(mn -> {
                   // Compute hashes of the modules that depend on mn directly and
                   // indirectly excluding itself.
                   Set<String> ns = transposedGraph.dfs(mn)
                       .stream()
                       .filter(n -> !n.equals(mn) && hashModuleCandidates.contains(n))
                       .collect(toSet());
                   mods.add(mn);
                   mods.addAll(ns);

                   if (!ns.isEmpty()) {
                       Map<String, Path> moduleToPath = ns.stream()
                           .collect(toMap(Function.identity(), this::moduleToPath));
                       hashes.put(mn, ModuleHashes.generate(moduleToPath, "SHA-256"));
                   }
               });
        return hashes;
    }

    private Path moduleToPath(String name) {
        ResolvedModule rm = configuration.findModule(name).orElseThrow(
            () -> new InternalError("Selected module " + name + " not on module path"));

        URI uri = rm.reference().location().get();
        Path path = Paths.get(uri);
        String fn = path.getFileName().toString();
        if (!fn.endsWith(".jar") && !fn.endsWith(".jmod")) {
            throw new UnsupportedOperationException(path + " is not a modular JAR or jmod file");
        }
        return path;
    }

    /*
     * Utility class
     */
    static class Graph<T> {
        private final Set<T> nodes;
        private final Map<T, Set<T>> edges;

        public Graph(Set<T> nodes, Map<T, Set<T>> edges) {
            this.nodes = Collections.unmodifiableSet(nodes);
            this.edges = Collections.unmodifiableMap(edges);
        }

        public Set<T> nodes() {
            return nodes;
        }

        public Map<T, Set<T>> edges() {
            return edges;
        }

        public Set<T> adjacentNodes(T u) {
            return edges.get(u);
        }

        public boolean contains(T u) {
            return nodes.contains(u);
        }

        /**
         * Returns nodes sorted in topological order.
         */
        public Stream<T> orderedNodes() {
            TopoSorter<T> sorter = new TopoSorter<>(this);
            return sorter.result.stream();
        }

        /**
         * Traverse this graph and performs the given action in topological order
         */
        public void ordered(Consumer<T> action) {
            TopoSorter<T> sorter = new TopoSorter<>(this);
            sorter.ordered(action);
        }

        /**
         * Traverses this graph and performs the given action in reverse topological order
         */
        public void reverse(Consumer<T> action) {
            TopoSorter<T> sorter = new TopoSorter<>(this);
            sorter.reverse(action);
        }

        /**
         * Returns a transposed graph from this graph
         */
        public Graph<T> transpose() {
            Builder<T> builder = new Builder<>();
            nodes.stream().forEach(builder::addNode);
            // reverse edges
            edges.keySet().forEach(u -> {
                edges.get(u).stream()
                    .forEach(v -> builder.addEdge(v, u));
            });
            return builder.build();
        }

        /**
         * Returns all nodes reachable from the given root.
         */
        public Set<T> dfs(T root) {
            return dfs(Set.of(root));
        }

        /**
         * Returns all nodes reachable from the given set of roots.
         */
        public Set<T> dfs(Set<T> roots) {
            Deque<T> deque = new LinkedList<>(roots);
            Set<T> visited = new HashSet<>();
            while (!deque.isEmpty()) {
                T u = deque.pop();
                if (!visited.contains(u)) {
                    visited.add(u);
                    if (contains(u)) {
                        adjacentNodes(u).stream()
                            .filter(v -> !visited.contains(v))
                            .forEach(deque::push);
                    }
                }
            }
            return visited;
        }

        public void printGraph(PrintStream out) {
            out.println("graph for " + nodes);
            nodes.stream()
                .forEach(u -> adjacentNodes(u).stream()
                    .forEach(v -> out.format("  %s -> %s%n", u, v)));
        }

        static class Builder<T> {
            final Set<T> nodes = new HashSet<>();
            final Map<T, Set<T>> edges = new HashMap<>();

            public void addNode(T node) {
                if (nodes.contains(node)) {
                    return;
                }
                nodes.add(node);
                edges.computeIfAbsent(node, _e -> new HashSet<>());
            }

            public void addEdge(T u, T v) {
                addNode(u);
                addNode(v);
                edges.get(u).add(v);
            }

            public Graph<T> build() {
                return new Graph<T>(nodes, edges);
            }
        }
    }

    /**
     * Topological sort
     */
    private static class TopoSorter<T> {
        final Deque<T> result = new LinkedList<>();
        final Deque<T> nodes;
        final Graph<T> graph;

        TopoSorter(Graph<T> graph) {
            this.graph = graph;
            this.nodes = new LinkedList<>(graph.nodes);
            sort();
        }

        public void ordered(Consumer<T> action) {
            result.iterator().forEachRemaining(action);
        }

        public void reverse(Consumer<T> action) {
            result.descendingIterator().forEachRemaining(action);
        }

        private void sort() {
            Deque<T> visited = new LinkedList<>();
            Deque<T> done = new LinkedList<>();
            T node;
            while ((node = nodes.poll()) != null) {
                if (!visited.contains(node)) {
                    visit(node, visited, done);
                }
            }
        }

        private void visit(T node, Deque<T> visited, Deque<T> done) {
            if (visited.contains(node)) {
                if (!done.contains(node)) {
                    throw new IllegalArgumentException("Cyclic detected: " +
                        node + " " + graph.edges().get(node));
                }
                return;
            }
            visited.add(node);
            graph.edges().get(node).stream()
                .forEach(x -> visit(x, visited, done));
            done.add(node);
            result.addLast(node);
        }
    }
}
