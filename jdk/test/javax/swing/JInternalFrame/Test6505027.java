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
 * @test
 * @bug 6505027
 * @summary Tests focus problem inside internal frame
 * @author Sergey Malenkov
 */

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class Test6505027 implements Runnable {

    private static final boolean INTERNAL = true;
    private static final boolean TERMINATE = true;

    private static final int WIDTH = 450;
    private static final int HEIGHT = 200;
    private static final int OFFSET = 10;
    private static final long PAUSE = 2048L;

    private static final String[] COLUMNS = { "Size", "Shape" }; // NON-NLS
    private static final String[] ITEMS = { "a", "b", "c", "d" }; // NON-NLS
    private static final String KEY = "terminateEditOnFocusLost"; // NON-NLS

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Test6505027());

        Component component = null;
        while (component == null) {
            try {
                Thread.sleep(PAUSE);
            }
            catch (InterruptedException exception) {
                // ignore interrupted exception
            }
            component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        }
        if (!component.getClass().equals(JComboBox.class)) {
            throw new Error("unexpected focus owner: " + component);
        }
        SwingUtilities.getWindowAncestor(component).dispose();
    }

    private JTable table;
    private Point point;

    public void run() {
        if (this.table == null) {
            JFrame main = new JFrame();
            main.setSize(WIDTH + OFFSET * 3, HEIGHT + OFFSET * 5);
            main.setLocationRelativeTo(null);
            main.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            main.setVisible(true);

            Container container = main;
            if (INTERNAL) {
                JInternalFrame frame = new JInternalFrame();
                frame.setBounds(OFFSET, OFFSET, WIDTH, HEIGHT);
                frame.setVisible(true);

                JDesktopPane desktop = new JDesktopPane();
                desktop.add(frame, new Integer(1));

                container.add(desktop);
                container = frame;
            }
            this.table = new JTable(new DefaultTableModel(COLUMNS, 2));
            if (TERMINATE) {
                this.table.putClientProperty(KEY, Boolean.TRUE);
            }
            TableColumn column = this.table.getColumn(COLUMNS[1]);
            column.setCellEditor(new DefaultCellEditor(new JComboBox(ITEMS)));

            container.add(BorderLayout.NORTH, new JTextField());
            container.add(BorderLayout.CENTER, new JScrollPane(this.table));

            SwingUtilities.invokeLater(this);
        }
        else if (this.point == null) {
            this.point = this.table.getCellRect(1, 1, false).getLocation();
            SwingUtilities.convertPointToScreen(this.point, this.table);
            SwingUtilities.invokeLater(this);
        }
        else {
            try {
                Robot robot = new Robot();
                robot.mouseMove(this.point.x + 1, this.point.y + 1);
                robot.mousePress(InputEvent.BUTTON1_MASK);
            }
            catch (AWTException exception) {
                throw new Error("unexpected exception", exception);
            }
        }
    }
}
