/*
 * Copyright 2002-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */


package javax.management.remote;


import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.StringTokenizer;

/**
 * <p>The address of a JMX API connector server.  Instances of this class
 * are immutable.</p>
 *
 * <p>The address is an <em>Abstract Service URL</em> for SLP, as
 * defined in RFC 2609 and amended by RFC 3111.  It must look like
 * this:</p>
 *
 * <blockquote>
 *
 * <code>service:jmx:<em>protocol</em>:<em>sap</em></code>
 *
 * </blockquote>
 *
 * <p>Here, <code><em>protocol</em></code> is the transport
 * protocol to be used to connect to the connector server.  It is
 * a string of one or more ASCII characters, each of which is a
 * letter, a digit, or one of the characters <code>+</code> or
 * <code>-</code>.  The first character must be a letter.
 * Uppercase letters are converted into lowercase ones.</p>
 *
 * <p><code><em>sap</em></code> is the address at which the connector
 * server is found.  This address uses a subset of the syntax defined
 * by RFC 2609 for IP-based protocols.  It is a subset because the
 * <code>user@host</code> syntax is not supported.</p>
 *
 * <p>The other syntaxes defined by RFC 2609 are not currently
 * supported by this class.</p>
 *
 * <p>The supported syntax is:</p>
 *
 * <blockquote>
 *
 * <code>//<em>[host[</em>:<em>port]][url-path]</em></code>
 *
 * </blockquote>
 *
 * <p>Square brackets <code>[]</code> indicate optional parts of
 * the address.  Not all protocols will recognize all optional
 * parts.</p>
 *
 * <p>The <code><em>host</em></code> is a host name, an IPv4 numeric
 * host address, or an IPv6 numeric address enclosed in square
 * brackets.</p>
 *
 * <p>The <code><em>port</em></code> is a decimal port number.  0
 * means a default or anonymous port, depending on the protocol.</p>
 *
 * <p>The <code><em>host</em></code> and <code><em>port</em></code>
 * can be omitted.  The <code><em>port</em></code> cannot be supplied
 * without a <code><em>host</em></code>.</p>
 *
 * <p>The <code><em>url-path</em></code>, if any, begins with a slash
 * (<code>/</code>) or a semicolon (<code>;</code>) and continues to
 * the end of the address.  It can contain attributes using the
 * semicolon syntax specified in RFC 2609.  Those attributes are not
 * parsed by this class and incorrect attribute syntax is not
 * detected.</p>
 *
 * <p>Although it is legal according to RFC 2609 to have a
 * <code><em>url-path</em></code> that begins with a semicolon, not
 * all implementations of SLP allow it, so it is recommended to avoid
 * that syntax.</p>
 *
 * <p>Case is not significant in the initial
 * <code>service:jmx:<em>protocol</em></code> string or in the host
 * part of the address.  Depending on the protocol, case can be
 * significant in the <code><em>url-path</em></code>.</p>
 *
 * @see <a
 * href="ftp://ftp.rfc-editor.org/in-notes/rfc2609.txt">RFC 2609,
 * "Service Templates and <code>Service:</code> Schemes"</a>
 * @see <a
 * href="ftp://ftp.rfc-editor.org/in-notes/rfc3111.txt">RFC 3111,
 * "Service Location Protocol Modifications for IPv6"</a>
 *
 * @since 1.5
 */
public class JMXServiceURL implements Serializable {

    private static final long serialVersionUID = 8173364409860779292L;

    /**
     * <p>Constructs a <code>JMXServiceURL</code> by parsing a Service URL
     * string.</p>
     *
     * @param serviceURL the URL string to be parsed.
     *
     * @exception NullPointerException if <code>serviceURL</code> is
     * null.
     *
     * @exception MalformedURLException if <code>serviceURL</code>
     * does not conform to the syntax for an Abstract Service URL or
     * if it is not a valid name for a JMX Remote API service.  A
     * <code>JMXServiceURL</code> must begin with the string
     * <code>"service:jmx:"</code> (case-insensitive).  It must not
     * contain any characters that are not printable ASCII characters.
     */
    public JMXServiceURL(String serviceURL) throws MalformedURLException {
        final int serviceURLLength = serviceURL.length();

        /* Check that there are no non-ASCII characters in the URL,
           following RFC 2609.  */
        for (int i = 0; i < serviceURLLength; i++) {
            char c = serviceURL.charAt(i);
            if (c < 32 || c >= 127) {
                throw new MalformedURLException("Service URL contains " +
                                                "non-ASCII character 0x" +
                                                Integer.toHexString(c));
            }
        }

        // Parse the required prefix
        final String requiredPrefix = "service:jmx:";
        final int requiredPrefixLength = requiredPrefix.length();
        if (!serviceURL.regionMatches(true, // ignore case
                                      0,    // serviceURL offset
                                      requiredPrefix,
                                      0,    // requiredPrefix offset
                                      requiredPrefixLength)) {
            throw new MalformedURLException("Service URL must start with " +
                                            requiredPrefix);
        }

        int[] ptr = new int[1];

        // Parse the protocol name
        final int protoStart = requiredPrefixLength;
        final int protoEnd = indexOf(serviceURL, ':', protoStart);
        this.protocol =
            serviceURL.substring(protoStart, protoEnd).toLowerCase();

        if (!serviceURL.regionMatches(protoEnd, "://", 0, 3)) {
            throw new MalformedURLException("Missing \"://\" after " +
                                            "protocol name");
        }

        // Parse the host name
        final int hostStart = protoEnd + 3;
        final int hostEnd;
        if (hostStart < serviceURLLength
            && serviceURL.charAt(hostStart) == '[') {
            hostEnd = serviceURL.indexOf(']', hostStart) + 1;
            if (hostEnd == 0)
                throw new MalformedURLException("Bad host name: [ without ]");
            this.host = serviceURL.substring(hostStart + 1, hostEnd - 1);
            if (!isNumericIPv6Address(this.host)) {
                throw new MalformedURLException("Address inside [...] must " +
                                                "be numeric IPv6 address");
            }
        } else {
            hostEnd =
                indexOfFirstNotInSet(serviceURL, hostNameBitSet, hostStart);
            this.host = serviceURL.substring(hostStart, hostEnd);
        }

        // Parse the port number
        final int portEnd;
        if (hostEnd < serviceURLLength && serviceURL.charAt(hostEnd) == ':') {
            if (this.host.length() == 0) {
                throw new MalformedURLException("Cannot give port number " +
                                                "without host name");
            }
            final int portStart = hostEnd + 1;
            portEnd =
                indexOfFirstNotInSet(serviceURL, numericBitSet, portStart);
            final String portString = serviceURL.substring(portStart, portEnd);
            try {
                this.port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new MalformedURLException("Bad port number: \"" +
                                                portString + "\": " + e);
            }
        } else {
            portEnd = hostEnd;
            this.port = 0;
        }

        // Parse the URL path
        final int urlPathStart = portEnd;
        if (urlPathStart < serviceURLLength)
            this.urlPath = serviceURL.substring(urlPathStart);
        else
            this.urlPath = "";

        validate();
    }

    /**
     * <p>Constructs a <code>JMXServiceURL</code> with the given protocol,
     * host, and port.  This constructor is equivalent to
     * {@link #JMXServiceURL(String, String, int, String)
     * JMXServiceURL(protocol, host, port, null)}.</p>
     *
     * @param protocol the protocol part of the URL.  If null, defaults
     * to <code>jmxmp</code>.
     *
     * @param host the host part of the URL.  If null, defaults to the
     * local host name, as determined by
     * <code>InetAddress.getLocalHost().getHostName()</code>.  If it
     * is a numeric IPv6 address, it can optionally be enclosed in
     * square brackets <code>[]</code>.
     *
     * @param port the port part of the URL.
     *
     * @exception MalformedURLException if one of the parts is
     * syntactically incorrect, or if <code>host</code> is null and it
     * is not possible to find the local host name, or if
     * <code>port</code> is negative.
     */
    public JMXServiceURL(String protocol, String host, int port)
            throws MalformedURLException {
        this(protocol, host, port, null);
    }

    /**
     * <p>Constructs a <code>JMXServiceURL</code> with the given parts.
     *
     * @param protocol the protocol part of the URL.  If null, defaults
     * to <code>jmxmp</code>.
     *
     * @param host the host part of the URL.  If null, defaults to the
     * local host name, as determined by
     * <code>InetAddress.getLocalHost().getHostName()</code>.  If it
     * is a numeric IPv6 address, it can optionally be enclosed in
     * square brackets <code>[]</code>.
     *
     * @param port the port part of the URL.
     *
     * @param urlPath the URL path part of the URL.  If null, defaults to
     * the empty string.
     *
     * @exception MalformedURLException if one of the parts is
     * syntactically incorrect, or if <code>host</code> is null and it
     * is not possible to find the local host name, or if
     * <code>port</code> is negative.
     */
    @ConstructorProperties({"protocol", "host", "port", "URLPath"})
    public JMXServiceURL(String protocol, String host, int port,
                         String urlPath)
            throws MalformedURLException {
        if (protocol == null)
            protocol = "jmxmp";

        if (host == null) {
            InetAddress local;
            try {
                local = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new MalformedURLException("Local host name unknown: " +
                                                e);
            }

            host = local.getHostName();

            /* We might have a hostname that violates DNS naming
               rules, for example that contains an `_'.  While we
               could be strict and throw an exception, this is rather
               user-hostile.  Instead we use its numerical IP address.
               We can only reasonably do this for the host==null case.
               If we're given an explicit host name that is illegal we
               have to reject it.  (Bug 5057532.)  */
            try {
                validateHost(host);
            } catch (MalformedURLException e) {
                if (logger.fineOn()) {
                    logger.fine("JMXServiceURL",
                                "Replacing illegal local host name " +
                                host + " with numeric IP address " +
                                "(see RFC 1034)", e);
                }
                host = local.getHostAddress();
                /* Use the numeric address, which could be either IPv4
                   or IPv6.  validateHost will accept either.  */
            }
        }

        if (host.startsWith("[")) {
            if (!host.endsWith("]")) {
                throw new MalformedURLException("Host starts with [ but " +
                                                "does not end with ]");
            }
            host = host.substring(1, host.length() - 1);
            if (!isNumericIPv6Address(host)) {
                throw new MalformedURLException("Address inside [...] must " +
                                                "be numeric IPv6 address");
            }
            if (host.startsWith("["))
                throw new MalformedURLException("More than one [[...]]");
        }

        this.protocol = protocol.toLowerCase();
        this.host = host;
        this.port = port;

        if (urlPath == null)
            urlPath = "";
        this.urlPath = urlPath;

        validate();
    }

    private void validate() throws MalformedURLException {

        // Check protocol

        final int protoEnd = indexOfFirstNotInSet(protocol, protocolBitSet, 0);
        if (protoEnd == 0 || protoEnd < protocol.length()
            || !alphaBitSet.get(protocol.charAt(0))) {
            throw new MalformedURLException("Missing or invalid protocol " +
                                            "name: \"" + protocol + "\"");
        }

        // Check host

        validateHost();

        // Check port

        if (port < 0)
            throw new MalformedURLException("Bad port: " + port);

        // Check URL path

        if (urlPath.length() > 0) {
            if (!urlPath.startsWith("/") && !urlPath.startsWith(";"))
                throw new MalformedURLException("Bad URL path: " + urlPath);
        }
    }

    private void validateHost() throws MalformedURLException {
        if (host.length() == 0) {
            if (port != 0) {
                throw new MalformedURLException("Cannot give port number " +
                                                "without host name");
            }
            return;
        }

        validateHost(host);
    }

    private static void validateHost(String h)
            throws MalformedURLException {

        if (isNumericIPv6Address(h)) {
            /* We assume J2SE >= 1.4 here.  Otherwise you can't
               use the address anyway.  We can't call
               InetAddress.getByName without checking for a
               numeric IPv6 address, because we mustn't try to do
               a DNS lookup in case the address is not actually
               numeric.  */
            try {
                InetAddress.getByName(h);
            } catch (Exception e) {
                /* We should really catch UnknownHostException
                   here, but a bug in JDK 1.4 causes it to throw
                   ArrayIndexOutOfBoundsException, e.g. if the
                   string is ":".  */
                MalformedURLException bad =
                    new MalformedURLException("Bad IPv6 address: " + h);
                EnvHelp.initCause(bad, e);
                throw bad;
            }
        } else {
            /* Tiny state machine to check valid host name.  This
               checks the hostname grammar from RFC 1034 (DNS),
               page 11.  A hostname is a dot-separated list of one
               or more labels, where each label consists of
               letters, numbers, or hyphens.  A label cannot begin
               or end with a hyphen.  Empty hostnames are not
               allowed.  Note that numeric IPv4 addresses are a
               special case of this grammar.

               The state is entirely captured by the last
               character seen, with a virtual `.' preceding the
               name.  We represent any alphanumeric character by
               `a'.

               We need a special hack to check, as required by the
               RFC 2609 (SLP) grammar, that the last component of
               the hostname begins with a letter.  Respecting the
               intent of the RFC, we only do this if there is more
               than one component.  If your local hostname begins
               with a digit, we don't reject it.  */
            final int hostLen = h.length();
            char lastc = '.';
            boolean sawDot = false;
            char componentStart = 0;

            loop:
            for (int i = 0; i < hostLen; i++) {
                char c = h.charAt(i);
                boolean isAlphaNumeric = alphaNumericBitSet.get(c);
                if (lastc == '.')
                    componentStart = c;
                if (isAlphaNumeric)
                    lastc = 'a';
                else if (c == '-') {
                    if (lastc == '.')
                        break; // will throw exception
                    lastc = '-';
                } else if (c == '.') {
                    sawDot = true;
                    if (lastc != 'a')
                        break; // will throw exception
                    lastc = '.';
                } else {
                    lastc = '.'; // will throw exception
                    break;
                }
            }

            try {
                if (lastc != 'a')
                    throw randomException;
                if (sawDot && !alphaBitSet.get(componentStart)) {
                    /* Must be a numeric IPv4 address.  In addition to
                       the explicitly-thrown exceptions, we can get
                       NoSuchElementException from the calls to
                       tok.nextToken and NumberFormatException from
                       the call to Integer.parseInt.  Using exceptions
                       for control flow this way is a bit evil but it
                       does simplify things enormously.  */
                    StringTokenizer tok = new StringTokenizer(h, ".", true);
                    for (int i = 0; i < 4; i++) {
                        String ns = tok.nextToken();
                        int n = Integer.parseInt(ns);
                        if (n < 0 || n > 255)
                            throw randomException;
                        if (i < 3 && !tok.nextToken().equals("."))
                            throw randomException;
                    }
                    if (tok.hasMoreTokens())
                        throw randomException;
                }
            } catch (Exception e) {
                throw new MalformedURLException("Bad host: \"" + h + "\"");
            }
        }
    }

    private static final Exception randomException = new Exception();


    /**
     * <p>The protocol part of the Service URL.
     *
     * @return the protocol part of the Service URL.  This is never null.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * <p>The host part of the Service URL.  If the Service URL was
     * constructed with the constructor that takes a URL string
     * parameter, the result is the substring specifying the host in
     * that URL.  If the Service URL was constructed with a
     * constructor that takes a separate host parameter, the result is
     * the string that was specified.  If that string was null, the
     * result is
     * <code>InetAddress.getLocalHost().getHostName()</code>.</p>
     *
     * <p>In either case, if the host was specified using the
     * <code>[...]</code> syntax for numeric IPv6 addresses, the
     * square brackets are not included in the return value here.</p>
     *
     * @return the host part of the Service URL.  This is never null.
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>The port of the Service URL.  If no port was
     * specified, the returned value is 0.</p>
     *
     * @return the port of the Service URL, or 0 if none.
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>The URL Path part of the Service URL.  This is an empty
     * string, or a string beginning with a slash (<code>/</code>), or
     * a string beginning with a semicolon (<code>;</code>).
     *
     * @return the URL Path part of the Service URL.  This is never
     * null.
     */
    public String getURLPath() {
        return urlPath;
    }

    /**
     * <p>The string representation of this Service URL.  If the value
     * returned by this method is supplied to the
     * <code>JMXServiceURL</code> constructor, the resultant object is
     * equal to this one.</p>
     *
     * <p>The <code><em>host</em></code> part of the returned string
     * is the value returned by {@link #getHost()}.  If that value
     * specifies a numeric IPv6 address, it is surrounded by square
     * brackets <code>[]</code>.</p>
     *
     * <p>The <code><em>port</em></code> part of the returned string
     * is the value returned by {@link #getPort()} in its shortest
     * decimal form.  If the value is zero, it is omitted.</p>
     *
     * @return the string representation of this Service URL.
     */
    public String toString() {
        /* We don't bother synchronizing the access to toString.  At worst,
           n threads will independently compute and store the same value.  */
        if (toString != null)
            return toString;
        StringBuilder buf = new StringBuilder("service:jmx:");
        buf.append(getProtocol()).append("://");
        final String getHost = getHost();
        if (isNumericIPv6Address(getHost))
            buf.append('[').append(getHost).append(']');
        else
            buf.append(getHost);
        final int getPort = getPort();
        if (getPort != 0)
            buf.append(':').append(getPort);
        buf.append(getURLPath());
        toString = buf.toString();
        return toString;
    }

    /**
     * <p>Indicates whether some other object is equal to this one.
     * This method returns true if and only if <code>obj</code> is an
     * instance of <code>JMXServiceURL</code> whose {@link
     * #getProtocol()}, {@link #getHost()}, {@link #getPort()}, and
     * {@link #getURLPath()} methods return the same values as for
     * this object.  The values for {@link #getProtocol()} and {@link
     * #getHost()} can differ in case without affecting equality.
     *
     * @param obj the reference object with which to compare.
     *
     * @return <code>true</code> if this object is the same as the
     * <code>obj</code> argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof JMXServiceURL))
            return false;
        JMXServiceURL u = (JMXServiceURL) obj;
        return
            (u.getProtocol().equalsIgnoreCase(getProtocol()) &&
             u.getHost().equalsIgnoreCase(getHost()) &&
             u.getPort() == getPort() &&
             u.getURLPath().equals(getURLPath()));
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /* True if this string, assumed to be a valid argument to
     * InetAddress.getByName, is a numeric IPv6 address.
     */
    private static boolean isNumericIPv6Address(String s) {
        // address contains colon if and only if it's a numeric IPv6 address
        return (s.indexOf(':') >= 0);
    }

    // like String.indexOf but returns string length not -1 if not present
    private static int indexOf(String s, char c, int fromIndex) {
        int index = s.indexOf(c, fromIndex);
        if (index < 0)
            return s.length();
        else
            return index;
    }

    private static int indexOfFirstNotInSet(String s, BitSet set,
                                            int fromIndex) {
        final int slen = s.length();
        int i = fromIndex;
        while (true) {
            if (i >= slen)
                break;
            char c = s.charAt(i);
            if (c >= 128)
                break; // not ASCII
            if (!set.get(c))
                break;
            i++;
        }
        return i;
    }

    private final static BitSet alphaBitSet = new BitSet(128);
    private final static BitSet numericBitSet = new BitSet(128);
    private final static BitSet alphaNumericBitSet = new BitSet(128);
    private final static BitSet protocolBitSet = new BitSet(128);
    private final static BitSet hostNameBitSet = new BitSet(128);
    static {
        /* J2SE 1.4 adds lots of handy methods to BitSet that would
           allow us to simplify here, e.g. by not writing loops, but
           we want to work on J2SE 1.3 too.  */

        for (char c = '0'; c <= '9'; c++)
            numericBitSet.set(c);

        for (char c = 'A'; c <= 'Z'; c++)
            alphaBitSet.set(c);
        for (char c = 'a'; c <= 'z'; c++)
            alphaBitSet.set(c);

        alphaNumericBitSet.or(alphaBitSet);
        alphaNumericBitSet.or(numericBitSet);

        protocolBitSet.or(alphaNumericBitSet);
        protocolBitSet.set('+');
        protocolBitSet.set('-');

        hostNameBitSet.or(alphaNumericBitSet);
        hostNameBitSet.set('-');
        hostNameBitSet.set('.');
    }

    private static void addCharsToBitSet(BitSet set, String chars) {
        for (int i = 0; i < chars.length(); i++)
            set.set(chars.charAt(i));
    }

    /**
     * The value returned by {@link #getProtocol()}.
     */
    private final String protocol;

    /**
     * The value returned by {@link #getHost()}.
     */
    private final String host;

    /**
     * The value returned by {@link #getPort()}.
     */
    private final int port;

    /**
     * The value returned by {@link #getURLPath()}.
     */
    private final String urlPath;

    /**
     * Cached result of {@link #toString()}.
     */
    private transient String toString;

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc", "JMXServiceURL");
}
