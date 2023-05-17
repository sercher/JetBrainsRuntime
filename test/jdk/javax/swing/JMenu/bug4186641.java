/*
 * Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4186641 4242461
  @summary JMenu.getPopupMenuOrigin() protected (not privet) now.
  @key headful
  @run main bug4186641
*/

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;


public class bug4186641 {

    volatile static JFrame fr;

    public static void main(String[] args) throws InterruptedException,
            InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            init();
            if (fr != null) {
                fr.dispose();
            }
        });
    }

    public static void init() {
        class TestJMenu extends JMenu {
            public TestJMenu() {
                super("Test");
            }

            void test() {
                Point testpoint = getPopupMenuOrigin();
            }
        }

        TestJMenu mnu = new TestJMenu();
        fr = new JFrame("bug4186641");
        JMenuBar mb = new JMenuBar();
        fr.setJMenuBar(mb);
        mb.add(mnu);
        JMenuItem mi = new JMenuItem("test");
        mnu.add(mi);
        fr.setSize(100,100);
        fr.setVisible(true);
        mnu.setVisible(true);

        mnu.test();
    }
}
