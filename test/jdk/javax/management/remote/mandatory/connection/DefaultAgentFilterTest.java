/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159377 8283093 8299891
 * @library /test/lib
 * @summary Tests ObjectFilter on default agent
 * @author Harsha Wardhana B
 * @modules java.management
 * @build DefaultAgentFilterTest
 * @run main/othervm/timeout=600 -XX:+UsePerfData DefaultAgentFilterTest
 */
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

public class DefaultAgentFilterTest {

    public static class MyTestObject implements Serializable {

        String a;
        int id;
    }

    public interface TestMBean {

        public void op1(HashSet<Object> params);

        public void op2(String s, HashSet<String> params);

        public void op3(MyTestObject obj, String s, HashMap<String, String> param);

        public void setMyAttribute(boolean b);

        public boolean getMyAttribute();

        public void setMyAttribute2(String s);

        public String getMyAttribute2();
    }

    public static class Test extends NotificationBroadcasterSupport implements TestMBean {

        boolean myAttribute;
        String myAttribute2;

        private long sequenceNumber = 1;

        @Override
        public void op1(HashSet<Object> params) {
            System.out.println("Invoked op1");
        }

        @Override
        public void op2(String s, HashSet<String> params) {
            System.out.println("Invoked op2");
        }

        @Override
        public void op3(MyTestObject obj, String s, HashMap<String, String> param) {
            System.out.println("Invoked op3");
        }

        public void setMyAttribute(boolean b) {
            this.myAttribute = b;
            System.out.println("Invoked setMyAttribute");
        }

        public boolean getMyAttribute() {
            System.out.println("Invoked getMyAttribute");
            return myAttribute;
        }

        public void setMyAttribute2(String s) {
            String old = myAttribute2;
            this.myAttribute2 = s;
            Notification n = new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                                                             "String attribute changed", "myAttribute2", "String",
                                                             old, this.myAttribute2);
            sendNotification(n);
            System.out.println("Invoked setMyAttribute2");
        }

        public String getMyAttribute2() {
            System.out.println("Invoked getMyAttribute2");
            return myAttribute2;
        }

        @Override
        public MBeanNotificationInfo[] getNotificationInfo() {
            System.out.println("Invoked getNotificationInfo");
            String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
            String name = AttributeChangeNotification.class.getName();
            String description = "An attribute of this MBean has changed";
            MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
            return new MBeanNotificationInfo[] {info};
        }
    }

    private static class TestAppRun implements AutoCloseable {

        private Process p;
        private final ProcessBuilder pb;
        private final String name;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private volatile long pid = -1;

        public TestAppRun(ProcessBuilder pb, String name) {
            this.pb = pb;
            this.name = name;
        }

        public synchronized void start() throws Exception {
            if (started.compareAndSet(false, true)) {
                try {
                    AtomicBoolean error = new AtomicBoolean(false);
                    AtomicBoolean bindError = new AtomicBoolean(false);
                    // The predicate below tries to recognise failures.  On a port clash, it sees e.g.
                    // Error: Exception thrown by the agent : java.rmi.server.ExportException: Port already in use: 46481; nested exception is:
                    // ...and will never see "main enter" from TestApp.
                    p = ProcessTools.startProcess(
                            TEST_APP_NAME + "{" + name + "}",
                            pb,
                            (line) -> {
                                if (line.contains("Exception")) {
                                    error.set(true);
                                    bindError.set(line.toLowerCase().contains("port already in use"));
                                    return true; // On Exception, app will never start
                                }
                                return line.contains("main enter");
                            },
                            60, TimeUnit.SECONDS);
                    if (bindError.get()) {
                        throw new BindException("Process could not be started");
                    } else if (error.get()) {
                        throw new RuntimeException();
                    }
                    pid = p.pid();
                } catch (Exception ex) {
                    if (p != null) {
                        p.destroy();
                        p.waitFor();
                    }
                    throw ex;
                }
            }
        }

        public long getPid() {
            return pid;
        }

        public synchronized void stop()
                throws IOException, InterruptedException {
            if (started.compareAndSet(true, false)) {
                p.getOutputStream().write(0);
                p.getOutputStream().flush();
                int ec = p.waitFor();
                if (ec != 0) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Test application '").append(name);
                    msg.append("' failed with exit code: ");
                    msg.append(ec);
                    System.err.println(msg);
                }
            }
        }

        @Override
        public void close() throws Exception {
            stop();
        }
    }

    private static final String TEST_APP_NAME = "TestApp";
    private static final int FREE_PORT_ATTEMPTS = 10;

    private static void testDefaultAgent(String propertyFile) throws Exception {
        testDefaultAgent(propertyFile, null, true /* test operations */);
    }

    private static void testDefaultAgent(String propertyFile, boolean testOperations) throws Exception {
        testDefaultAgent(propertyFile, null, testOperations);
    }

    private static void testDefaultAgent(String propertyFile, String additionalArgument, boolean testOperations) throws Exception {
        for (int i = 1; i <= FREE_PORT_ATTEMPTS; i++) {
            int port = Utils.getFreePort();
            System.out.println("Attempting testDefaultAgent("
                               + (propertyFile != null ? propertyFile : "no properties")
                               + " testOperations=" + testOperations
                               + ") with port: " + port);
            try {
                testDefaultAgent(propertyFile, additionalArgument, port, testOperations);
                break;  // return succesfully
            } catch (BindException b) {
                // Retry with new port.  Throw if last iteration:
                if (i == FREE_PORT_ATTEMPTS) {
                    throw(b);
                }
            }
        }
    }

    /**
      * Run the test app and connect. Test MBean Operations if the boolean testOperations is true, otherwise
      * test other usages (Attributes, Query)
      */
    private static void testDefaultAgent(String propertyFile, String additionalArgument, int port, boolean testOperations) throws Exception {
        List<String> pbArgs = new ArrayList<>(Arrays.asList(
                "-cp",
                System.getProperty("test.class.path"),
                "-XX:+UsePerfData"
        ));
        // JMX config arguments, using propertyFile if non-null:
        pbArgs.add("-Dcom.sun.management.jmxremote.port=" + port);
        pbArgs.add("-Dcom.sun.management.jmxremote.authenticate=false");
        pbArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
        if (propertyFile != null) {
            String propFile = System.getProperty("test.src") + File.separator + propertyFile;
            pbArgs.add("-Dcom.sun.management.config.file=" + propFile);
        }
        if (additionalArgument != null) {
            pbArgs.add(additionalArgument);
        }
        pbArgs.add(TEST_APP_NAME);

        ProcessBuilder pb = ProcessTools.createLimitedTestJavaProcessBuilder(
                pbArgs.toArray(new String[pbArgs.size()])
        );

        try (TestAppRun s = new TestAppRun(pb, DefaultAgentFilterTest.class.getSimpleName())) {
            s.start();
            JMXServiceURL url = testConnect(port);
            if (testOperations) {
                testMBeanOperations(url);
            } else {
                testMBeanOtherClasses(url);
            }
        }
    }

    private static JMXServiceURL testConnect(int port) throws Exception {
        EOFException lastException = null;
        JMXServiceURL url = null;
        // factor adjusted timeout (5 seconds) for the RMI to become available
        long timeout = System.currentTimeMillis() + Utils.adjustTimeout(5000);
        do {
            lastException = null;
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                String[] relist = registry.list();
                for (int i = 0; i < relist.length; ++i) {
                    System.out.println("Got registry: " + relist[i]);
                }
                String jmxUrlStr = String.format(
                        "service:jmx:rmi:///jndi/rmi://localhost:%d/jmxrmi",
                        port);
                url = new JMXServiceURL(jmxUrlStr);

                try (JMXConnector c = JMXConnectorFactory.connect(url, null)) {
                    MBeanServerConnection conn = c.getMBeanServerConnection();
                    ObjectName name = new ObjectName("jtreg:type=Test");
                    conn.createMBean(Test.class.getName(), name);
                }
            } catch (Exception ex) {
                if (ex instanceof EOFException) {
                    lastException = (EOFException) ex;
                    System.out.println("Error establishing RMI connection. Retrying in 500ms.");
                    Thread.sleep(500);
                } else {
                    throw ex;
                }
            }
        } while (lastException != null && System.currentTimeMillis() < timeout);
        if (lastException != null) {
            throw lastException;
        }
        return url;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("---" + DefaultAgentFilterTest.class.getName() + "-main: starting ...");

        try {
            // Properties file filter blocks DefaultAgentFilterTest$MyTestObject
            testDefaultAgent("mgmt1.properties");
            System.out.println("----\tTest FAILED !!");
            throw new RuntimeException("---" + DefaultAgentFilterTest.class.getName() + " - No exception reported");
        } catch (Exception ex) {
            if (ex instanceof InvalidClassException) {
                System.out.println("----\tTest PASSED !!");
            } else if (ex instanceof UnmarshalException
                    && ((UnmarshalException) ex).getCause() instanceof InvalidClassException) {
                System.out.println("----\tTest PASSED !!");
            } else {
                System.out.println(ex);
                System.out.println("----\tTest FAILED !!");
                throw ex;
            }
        }
        try {
            // filter non-existent class
            testDefaultAgent("mgmt2.properties");
            System.out.println("----\tTest PASSED !!");
        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("----\tTest FAILED !!");
            throw ex;
        }
        try {
            // Use default filter, should fail with: java.io.InvalidClassException: filter status: REJECTED
            testDefaultAgent(null /* no properties file */);
            throw new RuntimeException("---" + DefaultAgentFilterTest.class.getName() + " - No exception reported");
        } catch (Exception ex) {
            System.out.println(ex);
            if (ex instanceof InvalidClassException) {
                System.out.println("----\tTest PASSED: expected InvalidClassException received.");
            } else {
                System.out.println("----\tTest FAILED: unexpected Exception");
                throw ex;
            }
        }
        try {
            // Test Attributes and Query work by default:
            testDefaultAgent(null /* no properties file */,  false /* not testing operations */);
        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("----\tTest FAILED !!");
            throw ex;
        }
        try {
            // Add custom filter on command-line.
            testDefaultAgent(null, "-Dcom.sun.management.jmxremote.serial.filter.pattern=\"java.lang.*;java.math.BigInteger;"
                             + "java.math.BigDecimal;java.util.*;javax.management.openmbean.*;javax.management.ObjectName;"
                             + "java.rmi.MarshalledObject;javax.security.auth.Subject;DefaultAgentFilterTest$MyTestObject;!*\"",
                             true /* test operations */);
            System.out.println("----\tTest PASSED: with custom filter on command-line");
        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("----\tTest FAILED: unexpected Exception");
            throw ex;
        }
        System.out.println("---" + DefaultAgentFilterTest.class.getName() + "-main: finished ...");
    }

    private static void testMBeanOperations(JMXServiceURL serverUrl) throws Exception {
        Map<String, Object> clientEnv = new HashMap<>(1);
        ObjectName name = new ObjectName("jtreg:type=Test");
        try (JMXConnector client = JMXConnectorFactory.connect(serverUrl, clientEnv)) {
            MBeanServerConnection conn = client.getMBeanServerConnection();

            HashSet<String> set = new HashSet<>();
            set.add("test1");
            set.add("test2");

            String a = "A";

            Object[] params1 = {set};
            String[] sig1 = {HashSet.class.getName()};
            conn.invoke(name, "op1", params1, sig1);

            Object[] params2 = {a, set};
            String[] sig2 = {String.class.getName(), HashSet.class.getName()};
            conn.invoke(name, "op2", params2, sig2);

            HashMap<String, String> map = new HashMap<>();
            map.put("a", "A");
            map.put("b", "B");

            Object[] params3 = {new MyTestObject(), a, map};
            String[] sig3 = {MyTestObject.class.getName(), String.class.getName(),
                HashMap.class.getName()};
            conn.invoke(name, "op3", params3, sig3);

            System.out.println("Done testMBeanOperations");
        }
    }

    private static void testMBeanOtherClasses(JMXServiceURL serverUrl) throws Exception {
        Map<String, Object> clientEnv = new HashMap<>(1);
        ObjectName name = new ObjectName("jtreg:type=Test");
        try (JMXConnector client = JMXConnectorFactory.connect(serverUrl, clientEnv)) {
            MBeanServerConnection conn = client.getMBeanServerConnection();

            // Set operations send types to the server and can conflict with the filter.
            // Attribute set operations require javax.management.Attribute.
            conn.setAttribute(name, new Attribute("MyAttribute", Boolean.TRUE));
            boolean b = (Boolean) conn.getAttribute(name, "MyAttribute");
            if (b != true) {
                throw new RuntimeException("Attribute not as expected, got: " + b);
            }
            conn.setAttribute(name, new Attribute("MyAttribute2", "my string value"));
            String s = (String) conn.getAttribute(name, "MyAttribute2");
            if (!s.equals("my string value")) {
                throw new RuntimeException("Attribute not as expected, got: " + s);
            }

            // Use javax.management.AttributeList:
            AttributeList attrs = conn.getAttributes(name, new String [] { "MyAttribute", "MyAttribute2" });
            attrs = conn.setAttributes(name, attrs); // Setting fails if filter too restrictive

            // Sending a Query uses several classes from javax.management:
            QueryExp exp = Query.isInstanceOf(Query.value("notImportantClassName"));
            Set<ObjectInstance> queryResult = conn.queryMBeans(name, exp);

            // Notification:
            conn.addNotificationListener(name, new NotificationListener() {
                public void handleNotification(Notification notification, Object handback) {
                    System.out.println("Received notification: " +  notification);
                } }, null,  null);
            // Trigger a Notification:
            conn.setAttribute(name, new Attribute("MyAttribute2", "my new string value"));
            // Receiving the AttributeChangeNotification is not truly important for this test:
            try { Thread.sleep(5000); } catch (InterruptedException ie) { }

            System.out.println("Done testMBeanOtherClasses");
        }
    }
}

class TestApp {

    private static void doSomething() throws IOException {
        int r = System.in.read();
        System.out.println("read: " + r);
    }

    public static void main(String args[]) throws Exception {
        System.out.println("main enter");
        System.out.flush();
        doSomething();
        System.out.println("main exit");
    }
}
