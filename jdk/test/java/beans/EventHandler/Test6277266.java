/*
 * Copyright 2005-2009 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 6277266
 * @summary Tests access control issue in EventHandler
 * @author Jeff Nisewanger
 */

import java.beans.EventHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import javax.swing.SwingUtilities;

public class Test6277266 {
    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());
        try {
            SwingUtilities.invokeAndWait(
                    (Runnable) Proxy.newProxyInstance(
                            null,
                            new Class[] {Runnable.class},
                            new EventHandler(
                                    Test6277266.class,
                                    "getProtectionDomain",
                                    null,
                                    null
                            )
                    )
            );
            throw new Error("SecurityException expected");
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof SecurityException){
                return; // expected security exception
            }
            throw new Error("unexpected exception", exception);
        } catch (InterruptedException exception) {
            throw new Error("unexpected exception", exception);
        }
    }
}
