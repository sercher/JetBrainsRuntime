/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6521533 6525997 7102282 8198613
 * @summary Verifies that the accelerated codepaths for GradientPaint,
 * LinearGradientPaint, and RadialGradientPaint produce results that are
 * sufficiently close to those produced by the software codepaths.
 * @run main/othervm -Dsun.java2d.uiScale=1 GradientPaints
 * @author campbelc
 */

import java.awt.*;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class GradientPaints extends Canvas {

    private static final int TESTW = 600;
    private static final int TESTH = 500;

    /*
     * We expect slight differences in rendering between the OpenGL and
     * software pipelines due to algorithmic and rounding differences.
     * The purpose of this test is just to make sure that the OGL pipeline
     * is producing results that are "reasonably" consistent with those
     * produced in software, so we will allow +/-TOLERANCE differences
     * in each component.  When comparing the test and reference images,
     * we add up the number of pixels that fall outside this tolerance
     * range and if the sum is larger than some percentage of the total
     * number of pixels.
     *
     * REMIND: Note that we have separate thresholds for linear and radial
     * gradients because the visible differences between OGL and software
     * are more apparent in the radial cases.  In the future we should try
     * to reduce the number of mismatches between the two approaches, but
     * for now the visible differences are slight enough to not cause worry.
     */
    private static final int TOLERANCE = 5;
    private static final int ALLOWED_MISMATCHES_LINEAR =
        (int)(TESTW * TESTH * 0.18);
    private static final int ALLOWED_MISMATCHES_RADIAL =
        (int)(TESTW * TESTH * 0.45);
    private static final int ALLOWED_RENDER_ATTEMPTS = 5;

    private static boolean verbose;

    private static final Color[] COLORS = {
        new Color(0, 0, 0),
        new Color(128, 128, 128),
        new Color(255, 0, 0),
        new Color(255, 255, 0),
        new Color(0, 255, 0),
        new Color(0, 255, 255),
        new Color(128, 0, 255),
        new Color(128, 128, 128),
    };

    private enum PaintType {BASIC, LINEAR, RADIAL}
    private enum XformType {IDENTITY, TRANSLATE, SCALE, SHEAR, ROTATE}
    private static final int[] numStopsArray = {2, 4, 7};
    private static final Object[] hints = {
        RenderingHints.VALUE_ANTIALIAS_OFF,
        RenderingHints.VALUE_ANTIALIAS_ON,
    };

    public void paint(Graphics g) {
        painted.countDown();
    }

    private void testAll(Graphics gscreen,
                         BufferedImage refImg, VolatileImage testImg, GraphicsConfiguration gc)
    {
        Graphics2D gref  = refImg.createGraphics();
        testImg.validate(gc);
        for (PaintType paintType : PaintType.values()) {
            for (CycleMethod cycleMethod : CycleMethod.values()) {
                for (ColorSpaceType colorSpace : ColorSpaceType.values()) {
                    for (XformType xform : XformType.values()) {
                        for (Object aahint : hints) {
                            for (int numStops : numStopsArray) {
                                Paint paint =
                                    makePaint(paintType, cycleMethod,
                                              colorSpace, xform, numStops);
                                String msg =
                                    "type=" + paintType +
                                    " cycleMethod=" + cycleMethod +
                                    " colorSpace=" + colorSpace +
                                    " xformType=" + xform +
                                    " numStops=" + numStops +
                                    " aa=" + aahint;
                                renderTest(gref,  paint, aahint);
                                int allowedMismatches = paintType == PaintType.RADIAL ?
                                        ALLOWED_MISMATCHES_RADIAL : ALLOWED_MISMATCHES_LINEAR;
                                int attempt = 0;
                                while (true) {
                                    Graphics2D gtest = testImg.createGraphics();
                                    renderTest(gtest, paint, aahint);
                                    gscreen.drawImage(testImg, 0, 0, null);
                                    Toolkit.getDefaultToolkit().sync();
                                    gtest.dispose();
                                    BufferedImage snapshot = testImg.getSnapshot();
                                    if (testImg.contentsLost() &&
                                        testImg.validate(gc) != VolatileImage.IMAGE_OK)
                                    {
                                        if (attempt++ >= ALLOWED_RENDER_ATTEMPTS) {
                                            throw new RuntimeException("Cannot render to VI");
                                        }
                                        try {
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        continue;
                                    }
                                    compareImages(refImg, snapshot, allowedMismatches, msg);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        gref.dispose();
    }

    private Paint makePaint(PaintType paintType,
                            CycleMethod cycleMethod,
                            ColorSpaceType colorSpace,
                            XformType xformType, int numStops)
    {
        int startX   = TESTW/6;
        int startY   = TESTH/6;
        int endX     = TESTW/2;
        int endY     = TESTH/2;
        int ctrX     = TESTW/2;
        int ctrY     = TESTH/2;
        int focusX   = ctrX + 20;
        int focusY   = ctrY + 20;
        float radius = 100.0f;
        AffineTransform transform;

        Color[] colors = Arrays.copyOf(COLORS, numStops);
        float[] fractions = new float[colors.length];
        for (int i = 0; i < fractions.length; i++) {
            fractions[i] = ((float)i) / (fractions.length-1);
        }

        transform = switch (xformType) {
            case IDENTITY -> new AffineTransform();
            case TRANSLATE -> AffineTransform.getTranslateInstance(2, 2);
            case SCALE -> AffineTransform.getScaleInstance(1.2, 1.4);
            case SHEAR -> AffineTransform.getShearInstance(0.1, 0.1);
            case ROTATE -> AffineTransform.getRotateInstance(Math.PI / 4,
                    getWidth() >> 1, getHeight() >> 1);
        };

        return switch (paintType) {
            case BASIC -> new GradientPaint(startX, startY, Color.RED,
                    endX, endY, Color.BLUE, (cycleMethod != CycleMethod.NO_CYCLE));
            case LINEAR -> new LinearGradientPaint(new Point2D.Float(startX, startY),
                            new Point2D.Float(endX, endY),
                            fractions, colors,
                            cycleMethod, colorSpace,
                            transform);
            case RADIAL -> new RadialGradientPaint(new Point2D.Float(ctrX, ctrY),
                            radius,
                            new Point2D.Float(focusX, focusY),
                            fractions, colors,
                            cycleMethod, colorSpace,
                            transform);
        };
    }

    private void renderTest(Graphics2D g2d, Paint p, Object aahint) {
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, TESTW, TESTH);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aahint);
        g2d.setPaint(p);
        g2d.fillOval(0, 0, TESTW, TESTH);
    }

    public Dimension getPreferredSize() {
        return new Dimension(TESTW, TESTH);
    }

    private static void compareImages(BufferedImage refImg,
                                      BufferedImage testImg,
                                      int allowedMismatches,
                                      String msg)
    {
        int numMismatches = 0;
        int x1 = 0;
        int y1 = 0;
        int x2 = refImg.getWidth();
        int y2 = refImg.getHeight();

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                Color expected = new Color(refImg.getRGB(x, y));
                Color actual   = new Color(testImg.getRGB(x, y));
                if (!isSameColor(expected, actual)) {
                    numMismatches++;
                }
            }
        }

        if (verbose) {
            System.out.println(msg);
        }
        if (numMismatches > allowedMismatches) {
            try {
                ImageIO.write(refImg,  "png",
                              new File("GradientPaints.ref.png"));
                ImageIO.write(testImg, "png",
                              new File("GradientPaints.cap.png"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!verbose) {
                System.err.println(msg);
            }
            throw new RuntimeException("Test failed: Number of mismatches (" +
                                       numMismatches +
                                       ") exceeds limit (" +
                                       allowedMismatches +
                                       ") with tolerance=" + TOLERANCE);
        }
    }

    private static boolean isSameColor(Color c1, Color c2) {
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();
        int rmin = Math.max(r2- TOLERANCE, 0);
        int gmin = Math.max(g2- TOLERANCE, 0);
        int bmin = Math.max(b2- TOLERANCE, 0);
        int rmax = Math.min(r2+ TOLERANCE, 255);
        int gmax = Math.min(g2+ TOLERANCE, 255);
        int bmax = Math.min(b2+ TOLERANCE, 255);
        return r1 >= rmin && r1 <= rmax &&
                g1 >= gmin && g1 <= gmax &&
                b1 >= bmin && b1 <= bmax;
    }

    static CountDownLatch painted = new CountDownLatch(1);
    static Frame frame = null;

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-verbose")) {
            verbose = true;
        }

        GradientPaints test = new GradientPaints();
        SwingUtilities.invokeLater(() -> {
            frame = new Frame();
            frame.add(test);
            frame.pack();
            frame.setVisible(true);
        });

        // Wait until the component's been painted
        try {
            painted.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed: Interrupted");
        }

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc.getColorModel() instanceof IndexColorModel) {
            System.out.println("IndexColorModel detected: " +
                               "test considered PASSED");
            frame.dispose();
            return;
        }

        BufferedImage refImg =
            new BufferedImage(TESTW, TESTH, BufferedImage.TYPE_INT_RGB);
        VolatileImage testImg = frame.createVolatileImage(TESTW, TESTH);

        try {
            test.testAll(test.getGraphics(), refImg, testImg, gc);
        } finally {
            frame.dispose();
        }
    }
}
