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
 * @bug 6277246
 * @summary Tests problem with java.beans use of reflection
 * @author Jeff Nisewanger
 */

import java.lang.reflect.Method;
import java.beans.EventHandler;

public class Test6277246 {
    public static void main(String[] args) {
        try {
            System.setSecurityManager(new SecurityManager());
            Class container = Class.forName("java.lang.Class");
            Class parameter = Class.forName("java.lang.String");
            Method method = container.getMethod("forName", parameter);
            Object[] arglist = new Object[] {"sun.misc.BASE64Encoder"};
            EventHandler eh = new EventHandler(Test6277246.class, "forName", "", "forName");
            Object object = eh.invoke(null, method, arglist);
            throw new Error((object != null) ? "test failure" : "test error");
        }
        catch (ClassNotFoundException exception) {
            throw new Error("unexpected exception", exception);
        }
        catch (NoSuchMethodException exception) {
            throw new Error("unexpected exception", exception);
        }
        catch (SecurityException exception) {
            // expected security exception
        }
        catch (RuntimeException exception) {
            throw new Error("unexpected exception", exception);
        }
    }
}
