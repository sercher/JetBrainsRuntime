/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Implementation class of HotspotInternalMBean interface.
 *
 * <p> This is designed for internal customer use to create
 * this MBean dynamically from an agent which will then register
 * all internal MBeans to the platform MBeanServer.
 */
public class HotspotInternal
    implements HotspotInternalMBean, MBeanRegistration {

    private static final String HOTSPOT_INTERNAL_MBEAN_NAME =
        "sun.management:type=HotspotInternal";
    private static ObjectName objName = Util.newObjectName(HOTSPOT_INTERNAL_MBEAN_NAME);
    private MBeanServer server = null;

    /**
     * Default constructor that registers all hotspot internal MBeans
     * to the MBeanServer that creates this MBean.
     */
    public HotspotInternal() {
    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws java.lang.Exception {
        // register all internal MBeans when this MBean is instantiated
        // and to be registered in a MBeanServer.
        ManagementFactoryHelper.registerInternalMBeans(server);
        this.server = server;
        return objName;
    }

    public void postRegister(Boolean registrationDone) {};

    public void preDeregister() throws java.lang.Exception {
        // unregister all internal MBeans when this MBean is unregistered.
        ManagementFactoryHelper.unregisterInternalMBeans(server);
    }

    public void postDeregister() {};

}
