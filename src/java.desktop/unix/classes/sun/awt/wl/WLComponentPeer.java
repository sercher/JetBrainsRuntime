/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.awt.wl;

import static sun.awt.wl.WLToolkit.postEvent;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.util.Objects;
import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.PaintEventDispatcher;
import sun.awt.SunToolkit;
import sun.awt.event.IgnorePaintEvent;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.wl.WLSurfaceData;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;


public class WLComponentPeer implements ComponentPeer
{
    private long nativePtr;
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.wl.WLComponentPeer");
    protected final Component target;
    protected WLGraphicsConfig graphicsConfig;
    protected Color background;
    SurfaceData surfaceData;
    WLRepaintArea paintArea;
    boolean paintPending = false;
    boolean isLayouting = false;
    boolean visible = false;

    int x;
    int y;
    int width;
    int height;
    // used to check if we need to re-create surfaceData.
    int oldWidth = -1;
    int oldHeight = -1;

    static {
        initIDs();
    }

    /**
     * Standard peer constructor, with corresponding Component
     */
    WLComponentPeer(Component target) {
        this.target = target;
        this.background = target.getBackground();
        initGraphicsConfiguration();
        this.surfaceData = graphicsConfig.createSurfaceData(this);
        this.nativePtr = nativeCreateFrame();
        paintArea = new WLRepaintArea();
        Rectangle bounds = target.getBounds();
        x = bounds.x;
        y = bounds.y;
        width = bounds.width;
        height = bounds.height;
        log.info("WLComponentPeer: target=" + target + " x=" + x + " y=" + y +
                 " width=" + width + " height=" + height);
        // TODO
        // setup parent window for target
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    public final void repaint(int x, int y, int width, int height) {
        if (!isVisible() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Graphics g = getGraphics();
        if (g != null) {
            try {
                g.setClip(x, y, width, height);
                if (SunToolkit.isDispatchThreadForAppContext(getTarget())) {
                    paint(g); // The native and target will be painted in place.
                } else {
                    paintPeer(g);
                    postPaintEvent(target, x, y, width, height);
                }
            } finally {
                g.dispose();
            }
        }
    }

    public void postPaintEvent(Component target, int x, int y, int w, int h) {
        PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().
                createPaintEvent(target, x, y, w, h);
        if (event != null) {
            postEvent(event);
        }
    }

    void postPaintEvent() {
        if (isVisible()) {
            PaintEvent pe = new PaintEvent(target, PaintEvent.PAINT,
                    new Rectangle(0, 0, width, height));
            postEvent(pe);
        }
    }

    boolean isVisible() {
        return visible;
    }

    void repaint() {
        repaint(0, 0, getWidth(), getHeight());
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReparentSupported() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isObscured() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDetermineObscurity() {
        throw new UnsupportedOperationException();
    }

    public void focusGained(FocusEvent e) {
        log.info("Not implemented: WLComponentPeer.isObscured()");
    }

    public void focusLost(FocusEvent e) {
        log.info("Not implemented: WLComponentPeer.focusLost(FocusEvent)");
    }

    @Override
    public boolean isFocusable() {
        throw new UnsupportedOperationException();
    }

    public boolean requestFocus(Component lightweightChild, boolean temporary,
                                      boolean focusedWindowChangeAllowed, long time,
                                      FocusEvent.Cause cause)
    {
        log.info("Not implemented: WLComponentPeer.focusLost(FocusEvent)");
        return true;
    }

    protected void wlSetVisible(boolean v) {
        this.visible = v;
        if (this.visible) {
            nativeShowComponent(nativePtr, getParentNativePtr(target), target.getX(), target.getY());
            ((WLSurfaceData)surfaceData).initSurface(this, background != null ? background.getRGB() : 0, target.getWidth(), target.getHeight());
            PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().
                    createPaintEvent(target, 0, 0, target.getWidth(), target.getHeight());
            if (event != null) {
                WLToolkit.postEvent(WLToolkit.targetToAppContext(event.getSource()), event);
            }
        } else {
            nativeHideFrame(nativePtr);
        }
    }

    @Override
    public void setVisible(boolean v) {
        wlSetVisible(v);
    }

    /**
     * @see ComponentPeer
     */
    public void setEnabled(final boolean value) {
        log.info("Not implemented: WLComponentPeer.setEnabled(boolean)");
    }

    @Override
    public void paint(final Graphics g) {
        paintPeer(g);
        target.paint(g);
    }

    void paintPeer(final Graphics g) {
        log.info("Not implemented: WLComponentPeer.paintPeer(Graphics)");
    }

    Graphics getGraphics(SurfaceData surfData, Color afore, Color aback, Font afont) {
        if (surfData == null) return null;

        Color bgColor = aback;
        if (bgColor == null) {
            bgColor = SystemColor.window;
        }
        Color fgColor = afore;
        if (fgColor == null) {
            fgColor = SystemColor.windowText;
        }
        Font font = afont;
        if (font == null) {
            font = new Font(Font.DIALOG, Font.PLAIN, 12);
        }
        return new SunGraphics2D(surfData, fgColor, bgColor, font);
    }

    public Graphics getGraphics() {
        return getGraphics(surfaceData,
                target.getForeground(),
                target.getBackground(),
                target.getFont());
    }

    public Component getTarget() {
        return target;
    }

    public void print(Graphics g) {
        log.info("Not implemented: WLComponentPeer.print(Graphics)");
    }

    public void setBounds(int x, int y, int width, int height, int op) {
        if (this.x != x || this.y != y) {
            WLRobotPeer.setLocationOfWLSurface(getWLSurface(), x, y);
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        validateSurface();
        layout();
    }

    void validateSurface() {
        if ((width != oldWidth) || (height != oldHeight)) {
            doValidateSurface();

            oldWidth = width;
            oldHeight = height;
        }
    }

    final void doValidateSurface() {
        SurfaceData oldData = surfaceData;
        if (oldData != null) {
            surfaceData = graphicsConfig.createSurfaceData(this);
            oldData.invalidate();
        }
    }

    public void coalescePaintEvent(PaintEvent e) {
        Rectangle r = e.getUpdateRect();
        if (!(e instanceof IgnorePaintEvent)) {

            paintArea.add(r, e.getID());
        }
        if (true) {
            switch(e.getID()) {
                case PaintEvent.UPDATE:
                    if (log.isLoggable(Level.INFO)) {
                        log.info("WLCP coalescePaintEvent : UPDATE : add : x = " +
                                r.x + ", y = " + r.y + ", width = " + r.width + ",height = " + r.height);
                    }
                    return;
                case PaintEvent.PAINT:
                    if (log.isLoggable(Level.INFO)) {
                        log.info("WLCP coalescePaintEvent : PAINT : add : x = " +
                                r.x + ", y = " + r.y + ", width = " + r.width + ",height = " + r.height);
                    }
                    return;
            }
        }
    }

    @Override
    public Point getLocationOnScreen() {
        final long wlSurfacePtr = getWLSurface();
        if (wlSurfacePtr != 0) {
            return WLRobotPeer.getLocationOfWLSurface(wlSurfacePtr);
        } else {
            throw new UnsupportedOperationException("getLocationOnScreen() not supported without wayland surface");
        }
    }

    @SuppressWarnings("fallthrough")
    public void handleEvent(AWTEvent e) {
        if ((e instanceof InputEvent) && !((InputEvent) e).isConsumed() && target.isEnabled()) {
            if (e instanceof MouseEvent) {
                if (e instanceof MouseWheelEvent) {
                    log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): MouseWheelEvent");
                } else
                    log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): MouseEvent");
            } else if (e instanceof KeyEvent) {
                log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleF10JavaKeyEvent");
                log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleJavaKeyEvent");
            }
        } else if (e instanceof KeyEvent && !((InputEvent) e).isConsumed()) {
            // even if target is disabled.
            log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleF10JavaKeyEvent");
        } else if (e instanceof InputMethodEvent) {
            log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleJavaInputMethodEvent");
        }

        int id = e.getID();

        switch (id) {
            case PaintEvent.PAINT:
                // Got native painting
                paintPending = false;
                // Fallthrough to next statement
            case PaintEvent.UPDATE:
                log.info("WLComponentPeer.handleEvent(AWTEvent): UPDATE " + this);
                // Skip all painting while layouting and all UPDATEs
                // while waiting for native paint
                if (!isLayouting && !paintPending) {
                    paintArea.paint(target, false);
                }
                return;
            case FocusEvent.FOCUS_LOST:
            case FocusEvent.FOCUS_GAINED:
                log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleJavaFocusEvent");
                break;
            case WindowEvent.WINDOW_LOST_FOCUS:
            case WindowEvent.WINDOW_GAINED_FOCUS:
                log.info("Not implemented: WLComponentPeer.handleEvent(AWTEvent): handleJavaWindowFocusEvent");
                break;
            default:
                break;
        }
    }

    public void beginLayout() {
        // Skip all painting till endLayout
        isLayouting = true;

    }

    public void endLayout() {
        log.info("WLComponentPeer.endLayout(): paintArea.isEmpty() " + paintArea.isEmpty());
        if (!paintPending && !paintArea.isEmpty()
                && !AWTAccessor.getComponentAccessor().getIgnoreRepaint(target))
        {
            // if not waiting for native painting repaint damaged area
            postEvent(new PaintEvent(target, PaintEvent.PAINT,
                    new Rectangle()));
        }
        isLayouting = false;
    }

    public Dimension getMinimumSize() {
        return target.getSize();
    }

    @Override
    public ColorModel getColorModel() {
        if (graphicsConfig != null) {
            return graphicsConfig.getColorModel ();
        }
        else {
            return Toolkit.getDefaultToolkit().getColorModel();
        }
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void layout() {}

    @Override
    public void setBackground(Color c) {
        if (Objects.equals(background, c)) {
            return;
        }
        background = c;
    }

    @Override
    public void setForeground(Color c) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Set foreground to " + c);
        }
        log.info("Not implemented: WLComponentPeer.setForeground(Color)");
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {
        nativeDisposeFrame(nativePtr);
    }

    @Override
    public void setFont(Font f) {
        throw new UnsupportedOperationException();
    }

    public Font getFont() {
        return null;
    }

    public void updateCursorImmediately() {
        log.info("Not implemented: WLComponentPeer.updateCursorImmediately()");
    }

    @Override
    public Image createImage(int width, int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VolatileImage createVolatileImage(int width, int height) {
        throw new UnsupportedOperationException();
    }

    protected void initGraphicsConfiguration() {
        graphicsConfig = (WLGraphicsConfig) target.getGraphicsConfiguration();
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        if (graphicsConfig == null) {
            initGraphicsConfiguration();
        }
        return graphicsConfig;
    }

    @Override
    public boolean handlesWheelScrolling() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createBuffers(int numBuffers, BufferCapabilities caps) throws AWTException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2, BufferCapabilities.FlipContents flipAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Image getBackBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroyBuffers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setZOrder(ComponentPeer above) {
        log.info("Not implemented: WLComponentPeer.setZOrder(ComponentPeer)");
    }

    @Override
    public void applyShape(Region shape) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        throw new UnsupportedOperationException();
    }

    private static native void initIDs();

    protected native long nativeCreateFrame();

    protected native void nativeShowComponent(long ptr, long parentPtr, int x, int y);

    protected native void nativeHideFrame(long ptr);

    protected native void nativeDisposeFrame(long ptr);

    private native long getWLSurface();

    static long getParentNativePtr(Component target) {
        Component parent = target.getParent();
        if (parent == null) return 0;

        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(parent);

        return ((WLComponentPeer)peer).nativePtr;
    }
}