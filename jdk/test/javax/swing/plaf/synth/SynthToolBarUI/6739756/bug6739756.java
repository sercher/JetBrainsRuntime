/*
 * Copyright 2010 Sun Microsystems, Inc.  All Rights Reserved.
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

/**
 * @test
 * @bug 6739756
 * @author Alexander Potochkin
 * @summary JToolBar leaves space for non-visible items under Nimbus L&F
 * @run main bug6739756
 */

import javax.swing.*;
import java.awt.*;

public class bug6739756 {

    public static void main(String[] args) throws Exception {
        try {
           UIManager.setLookAndFeel(
                   "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JToolBar tb = new JToolBar();
                Dimension preferredSize = tb.getPreferredSize();
                JButton button = new JButton("Test");
                button.setVisible(false);
                tb.add(button);
                if (!preferredSize.equals(tb.getPreferredSize())) {
                    throw new RuntimeException("Toolbar's preferredSize is wrong");
                }
            }
        });
    }
}
