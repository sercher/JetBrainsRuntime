/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
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
  @test
  @bug 6852111
  @summary Unhandled 'spurious wakeup' in java.awt.EventQueue.invokeAndWait()
  @author dmitry.cherepanov@sun.com: area=awt.event
  @run main InvocationEventTest
*/

/**
 * InvocationEventTest.java
 *
 * summary: Tests new isDispatched method of the InvocationEvent class
 */

import java.awt.*;
import java.awt.event.*;

public class InvocationEventTest
{
    public static void main(String []s) throws Exception
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Runnable runnable = new Runnable() {
            public void run() {
            }
        };
        Object lock = new Object();
        InvocationEvent event = new InvocationEvent(tk, runnable, lock, true);

        if (event.isDispatched()) {
            throw new RuntimeException(" Initially, the event shouldn't be dispatched ");
        }

        synchronized(lock) {
            tk.getSystemEventQueue().postEvent(event);
            while(!event.isDispatched()) {
                lock.wait();
            }
        }

        if(!event.isDispatched()) {
            throw new RuntimeException(" Finally, the event should be dispatched ");
        }
    }
}
