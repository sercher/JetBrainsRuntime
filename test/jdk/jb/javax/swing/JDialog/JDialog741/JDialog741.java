/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.applet.Applet;
import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JDialog741 extends Applet {

    private static JFrame jFrame;
    private static Window windowAncestor;
    private static JDialog modalBlocker;

    public void start() {

        System.setProperty("jbre.popupwindow.settype", "true");

        jFrame = new JFrame("Wrong popup z-order");
        jFrame.setSize(200, 200);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel jPanel = new JPanel();
        jPanel.setPreferredSize(new Dimension(200, 200));

        Popup popup = PopupFactory.getSharedInstance().getPopup(jFrame, jPanel, 100, 100);
        windowAncestor = SwingUtilities.getWindowAncestor(jPanel);
        ((RootPaneContainer) windowAncestor).getRootPane().putClientProperty("SIMPLE_WINDOW", true);
        windowAncestor.setFocusable(true);
        windowAncestor.setFocusableWindowState(true);
        windowAncestor.setAutoRequestFocus(true);

        jFrame.setVisible(true);
        popup.show();


        modalBlocker = new JDialog(windowAncestor, "Modal Blocker");
        modalBlocker.setModal(true);
        modalBlocker.setSize(new Dimension(200, 200));
        modalBlocker.setLocation(200, 200);
        modalBlocker.addWindowListener(new JDialog741Listener());
        modalBlocker.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        modalBlocker.setVisible(true);
    }

    static class JDialog741Listener extends DialogListener {

        private static String printStream(String msg, InputStream stream) throws IOException {

            String result = "";
            int count = stream.available();
            if (count > 0) {
                byte[] b = new byte[count];
                stream.read(b);
                System.out.println("========= " + msg + " ========");
                result = new String(b);
                System.out.print(result);
                System.out.println("======================================");
            }
            return result;
        }

        @Override
        public void windowOpened(WindowEvent windowEvent) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String anotherAppMsg = "";
            boolean appFailed = false;

            try {
                String javaPath = System.getProperty("java.home", "");
                String command = javaPath + File.separator + "bin" +
                        File.separator + "java -cp " + System.getProperty("test.classes", ".") +
                        " AnotherApp";

                Process process = Runtime.getRuntime().exec(command);
                int returnCode = process.waitFor();

                printStream("AnotherApp System.out", process.getInputStream());

                String serr = printStream("AnotherApp System.err", process.getErrorStream());

                if (!serr.isEmpty()) {
                    String[] lines = serr.split("\\n");
                    for (String s : lines) {
                        if (s.contains("RuntimeException")) {
                            anotherAppMsg = s.split(":")[1];
                        }
                    }
                    appFailed = true;
                }

                System.out.println("return code: " + returnCode);

                jFrame.dispose();
                windowAncestor.dispose();
                modalBlocker.dispose();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            if (appFailed && !anotherAppMsg.isEmpty())
                throw new RuntimeException(anotherAppMsg);
            else
                throw new RuntimeException("AnotherApp failed");

        }
    }

}

class AnotherApp {

    private static Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e1) {
            e1.printStackTrace();
        }
    }

    private static JFrame jFrame = new JFrame("Another application");

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            jFrame.setSize(500, 500);
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jFrame.addWindowListener(new AnotherAppListener());
            jFrame.setVisible(true);
        });
    }

    private static boolean checkImage(Container window, Dimension shotSize, int x, int y, int maxWidth, int maxHeight) {

        boolean result = true;

        System.out.println("checking for expectedX: " + x + "; expectedY: " + y);
        System.out.println("              maxWidth: " + maxWidth + ";  maxHeight: " + maxHeight);

        Rectangle captureRect = new Rectangle(window.getLocationOnScreen(), shotSize);
        BufferedImage screenImage = robot.createScreenCapture(captureRect);

        try {
            ImageIO.write(screenImage, "bmp", new File("test741" + window.getName() + ".bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int rgb;
        int expectedRGB = screenImage.getRGB(x, y) & 0x00FFFFFF;

        System.out.println("  expected rgb value: " + Integer.toHexString(expectedRGB));
        for (int col = 1; col < maxWidth; col++) {
            for (int row = 1; row < maxHeight; row++) {
                // remove transparance
                rgb = screenImage.getRGB(col, row) & 0x00FFFFFF;

                result = (expectedRGB == rgb);
                if (expectedRGB != rgb) {
                    System.out.println("at row: " + row + "; col: " + col);
                    System.out.println("  expected rgb value: " + Integer.toHexString(expectedRGB));
                    System.out.println("    actual rgb value: " + Integer.toHexString(rgb));
                    break;
                }
            }
            if (!result) break;
        }

        return result;
    }

    static class AnotherAppListener extends DialogListener {

        @Override
        public void windowOpened(WindowEvent windowEvent) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Dimension shotSize;

            Container contentPane = jFrame.getContentPane();
            shotSize = contentPane.getSize();
            int expectedX = (int) (shotSize.getWidth() * 3 / 4);
            int expectedY = (int) (shotSize.getHeight() / 4);
            int maxWidth = (int) (shotSize.getWidth());
            int maxHeight = (int) (shotSize.getHeight());

            boolean failed = !checkImage(contentPane, shotSize, expectedX, expectedY, maxWidth, maxHeight);

            jFrame.dispose();
            if (failed) throw new RuntimeException("AnotherApp must be above all windows");
        }
    }
}

abstract class DialogListener implements WindowListener {

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
