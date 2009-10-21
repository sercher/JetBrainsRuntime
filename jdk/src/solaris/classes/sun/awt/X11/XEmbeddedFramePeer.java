/*
 * Copyright 2002-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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

package sun.awt.X11;

import java.awt.*;

import java.util.LinkedList;
import java.util.Iterator;

import sun.util.logging.PlatformLogger;

import sun.awt.EmbeddedFrame;
import sun.awt.SunToolkit;

public class XEmbeddedFramePeer extends XFramePeer {

    private static final PlatformLogger xembedLog = PlatformLogger.getLogger("sun.awt.X11.xembed.XEmbeddedFramePeer");

    LinkedList<AWTKeyStroke> strokes;

    XEmbedClientHelper embedder; // Caution - can be null if XEmbed is not supported
    public XEmbeddedFramePeer(EmbeddedFrame target) {
        // Don't specify PARENT_WINDOW param here. Instead we reparent
        // this embedded frame peer to the proper parent window after
        // an XEventDispatcher is registered to handle XEmbed events
        super(new XCreateWindowParams(new Object[] {
            TARGET, target,
            VISIBLE, Boolean.TRUE,
            EMBEDDED, Boolean.TRUE}));
    }

    public void preInit(XCreateWindowParams params) {
        super.preInit(params);
        strokes = new LinkedList<AWTKeyStroke>();
        if (supportsXEmbed()) {
            embedder = new XEmbedClientHelper();
        }
    }
    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        if (embedder != null) {
            // install X11 event dispatcher
            embedder.setClient(this);
            // reparent to XEmbed server
            embedder.install();
        } else if (getParentWindowHandle() != 0) {
            XToolkit.awtLock();
            try {
                XlibWrapper.XReparentWindow(XToolkit.getDisplay(),
                                            getWindow(),
                                            getParentWindowHandle(),
                                            0, 0);
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }

    @Override
    public void dispose() {
        if (embedder != null) {
            // uninstall X11 event dispatcher
            embedder.setClient(null);
        }
        super.dispose();
    }

    public void updateMinimumSize() {
    }

    protected String getWMName() {
        return "JavaEmbeddedFrame";
    }

    final long getParentWindowHandle() {
        return ((XEmbeddedFrame)target).handle;
    }

    boolean supportsXEmbed() {
        return ((EmbeddedFrame)target).supportsXEmbed();
    }

    public boolean requestWindowFocus(long time, boolean timeProvided) {
        // Should check for active state of host application
        if (embedder != null && embedder.isActive()) {
            xembedLog.fine("Requesting focus from embedding host");
            return embedder.requestFocus();
        } else {
            xembedLog.fine("Requesting focus from X");
            return super.requestWindowFocus(time, timeProvided);
        }
    }

    protected void requestInitialFocus() {
        if (embedder != null && supportsXEmbed()) {
            embedder.requestFocus();
        } else {
            super.requestInitialFocus();
        }
    }

    protected boolean isEventDisabled(XEvent e) {
        if (embedder != null && embedder.isActive()) {
            switch (e.get_type()) {
              case XConstants.FocusIn:
              case XConstants.FocusOut:
                  return true;
            }
        }
        return super.isEventDisabled(e);
    }

    public void handleConfigureNotifyEvent(XEvent xev)
    {
        assert (SunToolkit.isAWTLockHeldByCurrentThread());
        XConfigureEvent xe = xev.get_xconfigure();
        if (xembedLog.isLoggable(PlatformLogger.FINE)) {
            xembedLog.fine(xe.toString());
        }

        // fix for 5063031
        // if we use super.handleConfigureNotifyEvent() we would get wrong
        // size and position because embedded frame really is NOT a decorated one
        checkIfOnNewScreen(toGlobal(new Rectangle(xe.get_x(),
                xe.get_y(),
                xe.get_width(),
                xe.get_height())));

        Rectangle oldBounds = getBounds();

        synchronized (getStateLock()) {
            x = xe.get_x();
            y = xe.get_y();
            width = xe.get_width();
            height = xe.get_height();

            dimensions.setClientSize(width, height);
            dimensions.setLocation(x, y);
        }

        if (!getLocation().equals(oldBounds.getLocation())) {
            handleMoved(dimensions);
        }
        reconfigureContentWindow(dimensions);
    }

    protected void traverseOutForward() {
        if (embedder != null && embedder.isActive()) {
            if (embedder.isApplicationActive()) {
                xembedLog.fine("Traversing out Forward");
                embedder.traverseOutForward();
            }
        }
    }

    protected void traverseOutBackward() {
        if (embedder != null && embedder.isActive()) {
            if (embedder.isApplicationActive()) {
                xembedLog.fine("Traversing out Backward");
                embedder.traverseOutBackward();
            }
        }
    }

    // don't use getLocationOnScreen() inherited from XDecoratedPeer
    public Point getLocationOnScreen() {
        XToolkit.awtLock();
        try {
            return toGlobal(0, 0);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    // don't use getBounds() inherited from XDecoratedPeer
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS | NO_EMBEDDED_CHECK);
    }

    public Rectangle getBoundsPrivate() {
        int x = 0, y = 0;
        int w = 0, h = 0;
        XWindowAttributes attr = new XWindowAttributes();

        XToolkit.awtLock();
        try {
            XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                getWindow(), attr.pData);
            x = attr.get_x();
            y = attr.get_y();
            w = attr.get_width();
            h = attr.get_height();
        } finally {
            XToolkit.awtUnlock();
        }
        attr.dispose();

        return new Rectangle(x, y, w, h);
    }
    void registerAccelerator(AWTKeyStroke stroke) {
        if (stroke == null) return;
        strokes.add(stroke);
        if (embedder != null && embedder.isActive()) {
            embedder.registerAccelerator(stroke, strokes.size()-1);
        }
    }

    void unregisterAccelerator(AWTKeyStroke stroke) {
        if (stroke == null) return;
        if (embedder != null && embedder.isActive()) {
            int index = strokes.indexOf(stroke);
            embedder.unregisterAccelerator(index);
        }
    }

    void notifyStarted() {
        // Register accelerators
        if (embedder != null && embedder.isActive()) {
            int i = 0;
            Iterator<AWTKeyStroke> iter = strokes.iterator();
            while (iter.hasNext()) {
                embedder.registerAccelerator(iter.next(), i++);
            }
        }
        // Now we know that the the embedder is an XEmbed server, so we
        // reregister the drop target to enable XDnD protocol support via
        // XEmbed.
        updateDropTarget();
    }
    void notifyStopped() {
        if (embedder != null && embedder.isActive()) {
            for (int i = strokes.size() - 1; i >= 0; i--) {
                embedder.unregisterAccelerator(i);
            }
        }
    }

    long getFocusTargetWindow() {
        return getWindow();
    }

    boolean isXEmbedActive() {
        return embedder != null && embedder.isActive();
    }

    public int getAbsoluteX()
    {
        Point absoluteLoc = XlibUtil.translateCoordinates(getWindow(),
                                                          XToolkit.getDefaultRootWindow(),
                                                          new Point(0, 0));
        return absoluteLoc.x;
    }

    public int getAbsoluteY()
    {
        Point absoluteLoc = XlibUtil.translateCoordinates(getWindow(),
                                                          XToolkit.getDefaultRootWindow(),
                                                          new Point(0, 0));
        return absoluteLoc.y;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    // override XWindowPeer's method to let the embedded frame to block
    // the containing window
    public void setModalBlocked(Dialog blocker, boolean blocked) {
        super.setModalBlocked(blocker, blocked);

        EmbeddedFrame frame = (EmbeddedFrame)target;
        frame.notifyModalBlocked(blocker, blocked);
    }
}
