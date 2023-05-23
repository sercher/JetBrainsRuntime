/*
 * Copyright (c) 2004, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4962718
 * @summary Propertychange Listener not fired by inheritPopupMenu and Popupmenu properties
 * @key headful
 * @run main bug4962718
*/

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class bug4962718 {
    static volatile boolean popupWasSet = false;
    static volatile boolean inheritWasSet = false;
    static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("bug4962718");
                JButton button = new JButton("For test");
                JPopupMenu popup = new JPopupMenu();

                button.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equals("inheritsPopupMenu")) {
                            inheritWasSet = true;
                        } else if( evt.getPropertyName().
                                  equals("componentPopupMenu")) {
                            popupWasSet = true;
                        }
                    }
                });

                frame.add(button);
                button.setInheritsPopupMenu(true);
                button.setInheritsPopupMenu(false);
                button.setComponentPopupMenu(popup);
                button.setComponentPopupMenu(null);
                frame.pack();
                frame.setVisible(true);
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            if (!inheritWasSet) {
                throw new RuntimeException("Test failed, inheritsPopupMenu " +
                                   " property change listener was not called");
            }
            if (!popupWasSet) {
                throw new RuntimeException("Test failed, componentPopupMenu " +
                                    " property change listener was not called");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }
}

