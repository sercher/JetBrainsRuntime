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

package jdk.net;

import java.net.*;
import java.io.IOException;
import java.io.FileDescriptor;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import sun.net.ExtendedOptionsImpl;

/**
 * Defines static methods to set and get socket options defined by the
 * {@link java.net.SocketOption} interface. All of the standard options defined
 * by {@link java.net.Socket}, {@link java.net.ServerSocket}, and
 * {@link java.net.DatagramSocket} can be set this way, as well as additional
 * or platform specific options supported by each socket type.
 * <p>
 * The {@link #supportedOptions(Class)} method can be called to determine
 * the complete set of options available (per socket type) on the
 * current system.
 * <p>
 * When a security manager is installed, some non-standard socket options
 * may require a security permission before being set or get.
 * The details are specified in {@link ExtendedSocketOptions}. No permission
 * is required for {@link java.net.StandardSocketOptions}.
 *
 * @see java.nio.channels.NetworkChannel
 */
@jdk.Exported
public class Sockets {

    private static final HashMap<Class<?>,Set<SocketOption<?>>>
        options = new HashMap<>();

    static {
        initOptionSets();
    }

    private Sockets() {}

    /**
     * Sets the value of a socket option on a {@link java.net.Socket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option. May be null for some
     *              options.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs, or socket is closed.
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @throws NullPointerException if name is null
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> void setOption(Socket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a {@link java.net.Socket}
     *
     * @param s the socket
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @throws NullPointerException if name is null
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> T getOption(Socket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Sets the value of a socket option on a {@link java.net.ServerSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> void setOption(ServerSocket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a {@link java.net.ServerSocket}
     *
     * @param s the socket
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> T getOption(ServerSocket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Sets the value of a socket option on a {@link java.net.DatagramSocket}
     * or {@link java.net.MulticastSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> void setOption(DatagramSocket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a
     * {@link java.net.DatagramSocket} or {@link java.net.MulticastSocket}
     *
     * @param s the socket
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @see java.net.StandardSocketOptions
     */
    public static <T> T getOption(DatagramSocket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Returns a set of {@link java.net.SocketOption}s supported by the
     * given socket type. This set may include standard options and also
     * non standard extended options.
     *
     * @param socketType the type of java.net socket
     *
     * @throws IllegalArgumentException if socketType is not a valid
     *         socket type from the java.net package.
     */
    public static Set<SocketOption<?>> supportedOptions(Class<?> socketType) {
        Set<SocketOption<?>> set = options.get(socketType);
        if (set == null) {
            throw new IllegalArgumentException("unknown socket type");
        }
        return set;
    }

    private static void checkValueType(Object value, Class<?> type) {
        if (!type.isAssignableFrom(value.getClass())) {
            String s = "Found: " + value.getClass().toString() + " Expected: "
                        + type.toString();
            throw new IllegalArgumentException(s);
        }
    }

    private static void initOptionSets() {
        boolean flowsupported = ExtendedOptionsImpl.flowSupported();

        // Socket

        Set<SocketOption<?>> set = new HashSet<>();
        set.add(StandardSocketOptions.SO_KEEPALIVE);
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        set.add(StandardSocketOptions.SO_LINGER);
        set.add(StandardSocketOptions.IP_TOS);
        set.add(StandardSocketOptions.TCP_NODELAY);
        if (flowsupported) {
            set.add(ExtendedSocketOptions.SO_FLOW_SLA);
        }
        set = Collections.unmodifiableSet(set);
        options.put(Socket.class, set);

        // ServerSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        set.add(StandardSocketOptions.IP_TOS);
        set = Collections.unmodifiableSet(set);
        options.put(ServerSocket.class, set);

        // DatagramSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        set.add(StandardSocketOptions.IP_TOS);
        if (flowsupported) {
            set.add(ExtendedSocketOptions.SO_FLOW_SLA);
        }
        set = Collections.unmodifiableSet(set);
        options.put(DatagramSocket.class, set);

        // MulticastSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        set.add(StandardSocketOptions.IP_TOS);
        set.add(StandardSocketOptions.IP_MULTICAST_IF);
        set.add(StandardSocketOptions.IP_MULTICAST_TTL);
        set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
        if (flowsupported) {
            set.add(ExtendedSocketOptions.SO_FLOW_SLA);
        }
        set = Collections.unmodifiableSet(set);
        options.put(MulticastSocket.class, set);
    }
}
