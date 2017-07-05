/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/*
 * @test
 * @summary Check if ActionEvent is triggered by a TrayIcon only when
 *          it is double clicked using mouse button 1 (or single clicked
 *          with button 3 (on Mac OS X))
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library ../../../../lib/testlibrary ../
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main TrayIconMouseTest
 */

public class TrayIconMouseTest {

    TrayIcon icon;
    ExtendedRobot robot;

    boolean actionPerformed = false;
    Object actionLock = new Object();
    static boolean isMacOS = false;

    String caption = "Sample Icon";

    int[] buttonTypes = {
        InputEvent.BUTTON1_MASK,
        InputEvent.BUTTON2_MASK,
        InputEvent.BUTTON3_MASK
    };

    String[] buttonNames = {
        "BUTTON1",
        "BUTTON2",
        "BUTTON3"
    };

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
                isMacOS = true;
            } else if (SystemTrayIconHelper.isOel7()) {
                System.out.println("OEL 7 doesn't support double click in " +
                        "systray. Skipped");
                return;
            }
            new TrayIconMouseTest().doTest();
        }
    }

    TrayIconMouseTest() throws Exception{
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
    }

    void initializeGUI() {

        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), caption);
        icon.addActionListener(event -> {
            actionPerformed = true;
            synchronized (actionLock) {
                try {
                    actionLock.notifyAll();
                } catch (Exception e) {
                }
            }
        });
        try {
            tray.add(icon);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doTest() throws Exception {

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();

        for (int i = 0; i < buttonTypes.length; i++) {
            actionPerformed = false;
            robot.click(buttonTypes[i]);
            robot.waitForIdle(6000);

            if (isMacOS && actionPerformed && i == 2) {

            }else if (isMacOS && i == 2) {
                throw new RuntimeException("FAIL: ActionEvent NOT triggered when " +
                        buttonNames[i] + " is single clicked on Mac OS");
            }else if (actionPerformed) {
                throw new RuntimeException("FAIL: ActionEvent triggered when " +
                        buttonNames[i] + " is single clicked");
            }
        }

        if(!isMacOS) {
            for (int i = 0; i < buttonTypes.length; i++) {
                for (int j = 0; j < buttonTypes.length; j++) {
                    if (j != i) {
                        actionPerformed = false;
                        robot.mousePress(buttonTypes[i]);
                        robot.mousePress(buttonTypes[j]);
                        robot.mouseRelease(buttonTypes[j]);
                        robot.mouseRelease(buttonTypes[i]);

                        robot.waitForIdle();

                        if (actionPerformed)
                            throw new RuntimeException("FAIL: ActionEvent triggered when " +
                                    buttonNames[i] + " and " + buttonNames[j] +
                                    " is clicked and released");
                    }
                }
            }

            for (int i = 0; i < buttonTypes.length; i++) {
                actionPerformed = false;
                robot.mousePress(buttonTypes[i]);
                robot.delay(50);
                robot.mouseRelease(buttonTypes[i]);
                robot.delay(50);
                robot.mousePress(buttonTypes[i]);
                robot.delay(50);
                robot.mouseRelease(buttonTypes[i]);

                if (i == 0) {
                    if (! actionPerformed) {
                        synchronized (actionLock) {
                            try {
                                actionLock.wait(6000);
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (! actionPerformed)
                        throw new RuntimeException("FAIL: ActionEvent not triggered when " +
                                buttonNames[i] + " is double clicked");
                } else {
                    robot.waitForIdle();

                    if (actionPerformed)
                        throw new RuntimeException("FAIL: ActionEvent triggered when " +
                                buttonNames[i] + " is double clicked");
                }
            }
        }
    }
}
