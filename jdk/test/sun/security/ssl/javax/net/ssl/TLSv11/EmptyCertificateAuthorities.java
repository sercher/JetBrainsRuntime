/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4873188
 * @summary Support TLS 1.1
 * @run main/othervm EmptyCertificateAuthorities
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 *
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

public class EmptyCertificateAuthorities {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = false;

    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "/../../../../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf = getSSLServerSF();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);

        // require client authentication.
        sslServerSocket.setNeedClientAuth(true);

        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslIS.read();
        sslOS.write('A');
        sslOS.flush();

        sslSocket.close();
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLSocketFactory sslsf =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);

        // enable TLSv1.1 only
        sslSocket.setEnabledProtocols(new String[] {"TLSv1.1"});

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write('B');
        sslOS.flush();
        sslIS.read();

        sslSocket.close();
    }

    private SSLServerSocketFactory getSSLServerSF() throws Exception {

        char [] password =
            System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
        String keyFilename = System.getProperty("javax.net.ssl.keyStore");

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyFilename), password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        kmf.init(ks, password);

        KeyManager[] kms = kmf.getKeyManagers();
        TrustManager[] tms = new MyX509TM[] {new MyX509TM()};

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kms, tms, null);

        return ctx.getServerSocketFactory();
    }


    static class MyX509TM implements X509TrustManager {
        X509TrustManager tm;

        public void checkClientTrusted(X509Certificate[] chain,
            String authType) throws CertificateException {
            if (tm == null) {
                initialize();
            }
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain,
            String authType) throws CertificateException {
            if (tm == null) {
                initialize();
            }
            tm.checkServerTrusted(chain, authType);
        }

        public X509Certificate[] getAcceptedIssuers() {
            // always return empty array
            return new X509Certificate[0];
        }

        private void initialize() throws CertificateException {
            String passwd =
                System.getProperty("javax.net.ssl.trustStorePassword");
            char [] password = passwd.toCharArray();
            String trustFilename =
                System.getProperty("javax.net.ssl.trustStore");

            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(trustFilename), password);

                TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance("PKIX");
                tmf.init(ks);
                tm = (X509TrustManager)tmf.getTrustManagers()[0];
            } catch (Exception e) {
                throw new CertificateException("Unable to initialize TM");
            }

        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        String keyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + keyStoreFile;
        String trustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                "/" + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new EmptyCertificateAuthorities();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    EmptyCertificateAuthorities() throws Exception {
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            // swallow for now.  Show later
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         * Which side threw the error?
         */
        Exception local;
        Exception remote;
        String whichRemote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
            whichRemote = "server";
        } else {
            remote = clientException;
            local = serverException;
            whichRemote = "client";
        }

        /*
         * If both failed, return the curthread's exception, but also
         * print the remote side Exception
         */
        if ((local != null) && (remote != null)) {
            System.out.println(whichRemote + " also threw:");
            remote.printStackTrace();
            System.out.println();
            throw local;
        }

        if (remote != null) {
            throw remote;
        }

        if (local != null) {
            throw local;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
