/*
 * Copyright 2002-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.awt.event.ComponentEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.WindowEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

import sun.awt.ComponentAccessor;
import sun.awt.SunToolkit;

abstract class XDecoratedPeer extends XWindowPeer {
    private static final Logger log = Logger.getLogger("sun.awt.X11.XDecoratedPeer");
    private static final Logger insLog = Logger.getLogger("sun.awt.X11.insets.XDecoratedPeer");
    private static final Logger focusLog = Logger.getLogger("sun.awt.X11.focus.XDecoratedPeer");
    private final static Logger iconLog = Logger.getLogger("sun.awt.X11.icon.XDecoratedPeer");

    private static XAtom resize_request = new XAtom("_SUN_AWT_RESIZE_REQUEST", false);

    // Set to true when we get the first ConfigureNotify after being
    // reparented - indicates that WM has adopted the top-level.
    boolean configure_seen;
    boolean insets_corrected;

    XIconWindow iconWindow;
    WindowDimensions dimensions;
    XContentWindow content;
    Insets currentInsets;
    XFocusProxyWindow focusProxy;

    XDecoratedPeer(Window target) {
        super(target);
    }

    XDecoratedPeer(XCreateWindowParams params) {
        super(params);
    }

    public long getShell() {
        return window;
    }

    public long getContentWindow() {
        return (content == null) ? window : content.getWindow();
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        if (!resize_request.isInterned()) {
            resize_request.intern(false);
        }
        winAttr.initialFocus = true;

        currentInsets = new Insets(0,0,0,0); // replacemenet for wdata->top, left, bottom, right

        applyGuessedInsets();
        Rectangle bounds = (Rectangle)params.get(BOUNDS);
        dimensions = new WindowDimensions(bounds, getRealInsets(), false);
        params.put(BOUNDS, dimensions.getClientRect());
        insLog.log(Level.FINE, "Initial dimensions {0}", new Object[] { dimensions });

        // Deny default processing of these events on the shell - proxy will take care of
        // them instead
        Long eventMask = (Long)params.get(EVENT_MASK);
        params.add(EVENT_MASK, Long.valueOf(eventMask.longValue() & ~(FocusChangeMask | KeyPressMask | KeyReleaseMask)));
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        // The lines that follow need to be in a postInit, so they
        // happen after the X window is created.
        initResizability();
        updateSizeHints(dimensions);
        content = XContentWindow.createContent(this);
        if (warningWindow != null) {
            warningWindow.toFront();
        }
        focusProxy = createFocusProxy();
    }

    void setIconHints(java.util.List<XIconInfo> icons) {
        if (!XWM.getWM().setNetWMIcon(this, icons)) {
            if (icons.size() > 0) {
                if (iconWindow == null) {
                    iconWindow = new XIconWindow(this);
                }
                iconWindow.setIconImages(icons);
            }
        }
    }

    public void updateMinimumSize() {
        super.updateMinimumSize();
        updateMinSizeHints();
    }


    private void updateMinSizeHints() {
        if (isResizable()) {
            Dimension minimumSize = getTargetMinimumSize();
            if (minimumSize != null) {
                Insets insets = getRealInsets();
                int minWidth = minimumSize.width - insets.left - insets.right;
                int minHeight = minimumSize.height - insets.top - insets.bottom;
                if (minWidth < 0) minWidth = 0;
                if (minHeight < 0) minHeight = 0;
                setSizeHints(XlibWrapper.PMinSize | (isLocationByPlatform()?0:(XlibWrapper.PPosition | XlibWrapper.USPosition)),
                             getX(), getY(), minWidth, minHeight);
                if (isVisible()) {
                    Rectangle bounds = getShellBounds();
                    int nw = (bounds.width < minWidth) ? minWidth : bounds.width;
                    int nh = (bounds.height < minHeight) ? minHeight : bounds.height;
                    if (nw != bounds.width || nh != bounds.height) {
                        setShellSize(new Rectangle(0, 0, nw, nh));
                    }
                }
            } else {
                boolean isMinSizeSet = isMinSizeSet();
                XWM.removeSizeHints(this, XlibWrapper.PMinSize);
                /* Some WMs need remap to redecorate the window */
                if (isMinSizeSet && isShowing() && XWM.needRemap(this)) {
                    /*
                     * Do the re/mapping at the Xlib level.  Since we essentially
                     * work around a WM bug we don't want this hack to be exposed
                     * to Intrinsics (i.e. don't mess with grabs, callbacks etc).
                     */
                    xSetVisible(false);
                    XToolkit.XSync();
                    xSetVisible(true);
                }
            }
        }
    }

    XFocusProxyWindow createFocusProxy() {
        return new XFocusProxyWindow(this);
    }

    protected XAtomList getWMProtocols() {
        XAtomList protocols = super.getWMProtocols();
        protocols.add(wm_delete_window);
        protocols.add(wm_take_focus);
        return protocols;
    }

    public Graphics getGraphics() {
        return getGraphics(content.surfaceData,
                           ComponentAccessor.getForeground(target),
                           ComponentAccessor.getBackground(target),
                           ComponentAccessor.getFont_NoClientCode(target));
    }

    public void setTitle(String title) {
        if (log.isLoggable(Level.FINE)) log.fine("Title is " + title);
        winAttr.title = title;
        updateWMName();
    }

    protected String getWMName() {
        if (winAttr.title == null || winAttr.title.trim().equals("")) {
            return " ";
        } else {
            return winAttr.title;
        }
    }

    void updateWMName() {
        super.updateWMName();
        String name = getWMName();
        XToolkit.awtLock();
        try {
            if (name == null || name.trim().equals("")) {
                name = "Java";
            }
            XAtom iconNameAtom = XAtom.get(XAtom.XA_WM_ICON_NAME);
            iconNameAtom.setProperty(getWindow(), name);
            XAtom netIconNameAtom = XAtom.get("_NET_WM_ICON_NAME");
            netIconNameAtom.setPropertyUTF8(getWindow(), name);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleIconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_ICONIFIED));
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleDeiconify() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_DEICONIFIED));
    }

    public void handleFocusEvent(XEvent xev) {
        super.handleFocusEvent(xev);
        XFocusChangeEvent xfe = xev.get_xfocus();

        // If we somehow received focus events forward it instead to proxy
        // FIXME: Shouldn't we instead check for inferrior?
        focusLog.finer("Received focus event on shell: " + xfe);
//         focusProxy.xRequestFocus();
   }

/***************************************************************************************
 *                             I N S E T S   C O D E
 **************************************************************************************/

    protected boolean isInitialReshape() {
        return false;
    }

    Insets difference(Insets i1, Insets i2) {
        return new Insets(i1.top-i2.top, i1.left - i2.left, i1.bottom-i2.bottom, i1.right-i2.right);
    }

    void add(Insets i1, Insets i2) {
        i1.left += i2.left;
        i1.top += i2.top;
        i1.right += i2.right;
        i1.bottom += i2.bottom;
    }
    boolean isNull(Insets i) {
        return (i == null) || ((i.left | i.top | i.right | i.bottom) == 0);
    }
    Insets copy(Insets i) {
        return new Insets(i.top, i.left, i.bottom, i.right);
    }

    long reparent_serial = 0;

    public void handleReparentNotifyEvent(XEvent xev) {
        XReparentEvent  xe = xev.get_xreparent();
        if (insLog.isLoggable(Level.FINE)) insLog.fine(xe.toString());
        reparent_serial = xe.get_serial();
        XToolkit.awtLock();
        try {
            long root = XlibWrapper.RootWindow(XToolkit.getDisplay(), getScreenNumber());

            if (isEmbedded()) {
                setReparented(true);
                insets_corrected = true;
                return;
            }
            Component t = (Component)target;
            if (getDecorations() == XWindowAttributesData.AWT_DECOR_NONE) {
                setReparented(true);
                insets_corrected = true;
                reshape(dimensions, SET_SIZE, false);
            } else if (xe.get_parent() == root) {
                configure_seen = false;
                insets_corrected = false;

                /*
                 * We can be repareted to root for two reasons:
                 *   . setVisible(false)
                 *   . WM exited
                 */
                if (isVisible()) { /* WM exited */
                    /* Work around 4775545 */
                    XWM.getWM().unshadeKludge(this);
                    insLog.fine("- WM exited");
                } else {
                    insLog.fine(" - reparent due to hide");
                }
            } else { /* reparented to WM frame, figure out our insets */
                setReparented(true);
                insets_corrected = false;

                // Check if we have insets provided by the WM
                Insets correctWM = getWMSetInsets(null);
                if (correctWM != null) {
                    insLog.log(Level.FINER, "wm-provided insets {0}", new Object[]{correctWM});
                    // If these insets are equal to our current insets - no actions are necessary
                    Insets dimInsets = dimensions.getInsets();
                    if (correctWM.equals(dimInsets)) {
                        insLog.finer("Insets are the same as estimated - no additional reshapes necessary");
                        no_reparent_artifacts = true;
                        insets_corrected = true;
                        applyGuessedInsets();
                        return;
                    }
                } else {
                    correctWM = XWM.getWM().getInsets(this, xe.get_window(), xe.get_parent());

                    if (correctWM != null) {
                        insLog.log(Level.FINER, "correctWM {0}", new Object[] {correctWM});
                    } else {
                        insLog.log(Level.FINER, "correctWM insets are not available, waiting for configureNotify");
                    }
                }

                if (correctWM != null) {
                    handleCorrectInsets(correctWM);
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    protected void handleCorrectInsets(Insets correctWM) {
        XToolkit.awtLock();
        try {
            /*
             * Ok, now see if we need adjust window size because
             * initial insets were wrong (most likely they were).
             */
            Insets correction = difference(correctWM, currentInsets);
            insLog.log(Level.FINEST, "Corrention {0}", new Object[] {correction});
            if (!isNull(correction)) {
                /*
                 * Actual insets account for menubar/warning label,
                 * so we can't assign directly but must adjust them.
                 */
                add(currentInsets, correction);
                applyGuessedInsets();

                //Fix for 6318109: PIT: Min Size is not honored properly when a
                //smaller size is specified in setSize(), XToolkit
                //update minimum size hints
                updateMinSizeHints();

                /*
                 * If this window has been sized by a pack() we need
                 * to keep the interior geometry intact.  Since pack()
                 * computed width and height with wrong insets, we
                 * must adjust the target dimensions appropriately.
                 */
            }
            if (insLog.isLoggable(Level.FINER)) insLog.finer("Dimensions before reparent: " + dimensions);

            dimensions.setInsets(getRealInsets());
            insets_corrected = true;

            if (isMaximized()) {
                return;
            }

            if ((getHints().get_flags() & (USPosition | PPosition)) != 0) {
                reshape(dimensions, SET_BOUNDS, false);
            } else {
                reshape(dimensions, SET_SIZE, false);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void handleMoved(WindowDimensions dims) {
        Point loc = dims.getLocation();
        ComponentAccessor.setX((Component)target, loc.x);
        ComponentAccessor.setY((Component)target, loc.y);
        postEvent(new ComponentEvent(target, ComponentEvent.COMPONENT_MOVED));
    }


    protected Insets guessInsets() {
        if (isEmbedded()) {
            return new Insets(0, 0, 0, 0);
        } else {
            if (currentInsets.top > 0) {
                /* insets were set on wdata by System Properties */
                return copy(currentInsets);
            } else {
                Insets res = getWMSetInsets(null);
                if (res == null) {
                    res = XWM.getWM().guessInsets(this);
                }
                return res;
            }
        }
    }

    private void applyGuessedInsets() {
        Insets guessed = guessInsets();
        currentInsets = copy(guessed);
        insets = copy(currentInsets);
    }

    public void revalidate() {
        XToolkit.executeOnEventHandlerThread(target, new Runnable() {
                public void run() {
                    target.invalidate();
                    target.validate();
                }
            });
    }

    Insets getRealInsets() {
        if (isNull(insets)) {
            applyGuessedInsets();
        }
        return insets;
    }

    public Insets getInsets() {
        Insets in = copy(getRealInsets());
        in.top += getMenuBarHeight() + getWarningWindowHeight();
        if (insLog.isLoggable(Level.FINEST)) insLog.log(Level.FINEST, "Get insets returns {0}", new Object[] {in});
        return in;
    }

    boolean gravityBug() {
        return XWM.configureGravityBuggy();
    }

    // The height of area used to display current active input method
    int getInputMethodHeight() {
        return 0;
    }

    void updateSizeHints(WindowDimensions dims) {
        Rectangle rec = dims.getClientRect();
        checkShellRect(rec);
        updateSizeHints(rec.x, rec.y, rec.width, rec.height);
    }

    void updateSizeHints() {
        updateSizeHints(dimensions);
    }

    // Coordinates are that of the target
    // Called only on Toolkit thread
    public void reshape(WindowDimensions newDimensions, int op,
                        boolean userReshape)
    {
        if (insLog.isLoggable(Level.FINE)) {
            insLog.fine("Reshaping " + this + " to " + newDimensions + " op " + op + " user reshape " + userReshape);
        }
        if (userReshape) {
            // We handle only userReshape == true cases. It means that
            // if the window manager or any other part of the windowing
            // system sets inappropriate size for this window, we can
            // do nothing but accept it.
            Rectangle reqBounds = newDimensions.getBounds();
            Rectangle newBounds = constrainBounds(reqBounds.x, reqBounds.y, reqBounds.width, reqBounds.height);
            newDimensions = new WindowDimensions(newBounds, newDimensions.getInsets(), newDimensions.isClientSizeSet());
        }
        XToolkit.awtLock();
        try {
            if (!isReparented() || !isVisible()) {
                insLog.log(Level.FINE, "- not reparented({0}) or not visible({1}), default reshape",
                           new Object[] {Boolean.valueOf(isReparented()), Boolean.valueOf(visible)});

                // Fix for 6323293.
                // This actually is needed to preserve compatibility with previous releases -
                // some of licensees are expecting componentMoved event on invisible one while
                // its location changes.
                Point oldLocation = getLocation();

                Point newLocation = new Point(ComponentAccessor.getX((Component)target),
                                              ComponentAccessor.getY((Component)target));

                if (!newLocation.equals(oldLocation)) {
                    handleMoved(newDimensions);
                }

                dimensions = new WindowDimensions(newDimensions);
                updateSizeHints(dimensions);
                Rectangle client = dimensions.getClientRect();
                checkShellRect(client);
                setShellBounds(client);
                if (content != null &&
                    !content.getSize().equals(newDimensions.getSize()))
                {
                    reconfigureContentWindow(newDimensions);
                }
                return;
            }

            int wm = XWM.getWMID();
            updateChildrenSizes();
            applyGuessedInsets();

            Rectangle shellRect = newDimensions.getClientRect();

            if (gravityBug()) {
                Insets in = newDimensions.getInsets();
                shellRect.translate(in.left, in.top);
            }

            if ((op & NO_EMBEDDED_CHECK) == 0 && isEmbedded()) {
                shellRect.setLocation(0, 0);
            }

            checkShellRectSize(shellRect);
            if (!isEmbedded()) {
                checkShellRectPos(shellRect);
            }

            op = op & ~NO_EMBEDDED_CHECK;

            if (op == SET_LOCATION) {
                setShellPosition(shellRect);
            } else if (isResizable()) {
                if (op == SET_BOUNDS) {
                    setShellBounds(shellRect);
                } else {
                    setShellSize(shellRect);
                }
            } else {
                XWM.setShellNotResizable(this, newDimensions, shellRect, true);
                if (op == SET_BOUNDS) {
                    setShellPosition(shellRect);
                }
            }

            reconfigureContentWindow(newDimensions);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * @param x, y, width, heith - dimensions of the window with insets
     */
    private void reshape(int x, int y, int width, int height, int operation,
                         boolean userReshape)
    {
        Rectangle newRec;
        boolean setClient = false;
        WindowDimensions dims = new WindowDimensions(dimensions);
        switch (operation & (~NO_EMBEDDED_CHECK)) {
          case SET_LOCATION:
              // Set location always sets bounds location. However, until the window is mapped we
              // should use client coordinates
              dims.setLocation(x, y);
              break;
          case SET_SIZE:
              // Set size sets bounds size. However, until the window is mapped we
              // should use client coordinates
              dims.setSize(width, height);
              break;
          case SET_CLIENT_SIZE: {
              // Sets client rect size. Width and height contain insets.
              Insets in = currentInsets;
              width -= in.left+in.right;
              height -= in.top+in.bottom;
              dims.setClientSize(width, height);
              break;
          }
          case SET_BOUNDS:
          default:
              dims.setLocation(x, y);
              dims.setSize(width, height);
              break;
        }
        if (insLog.isLoggable(Level.FINE)) insLog.log(Level.FINE, "For the operation {0} new dimensions are {1}",
                                                      new Object[] {operationToString(operation), dims});

        reshape(dims, operation, userReshape);
    }

    // This method gets overriden in XFramePeer & XDialogPeer.
    abstract boolean isTargetUndecorated();

    @Override
    Rectangle constrainBounds(int x, int y, int width, int height) {
        // We don't restrict the setBounds() operation if the code is trusted.
        if (!hasWarningWindow()) {
            return new Rectangle(x, y, width, height);
        }

        // If it's undecorated or is not currently visible,
        // apply the same constraints as for the Window.
        if (!isVisible() || isTargetUndecorated()) {
            return super.constrainBounds(x, y, width, height);
        }

        // If it's visible & decorated, constraint the size only
        int newX = x;
        int newY = y;
        int newW = width;
        int newH = height;

        GraphicsConfiguration gc = ((Window)target).getGraphicsConfiguration();
        Rectangle sB = gc.getBounds();
        Insets sIn = ((Window)target).getToolkit().getScreenInsets(gc);

        Rectangle curBounds = getBounds();

        int maxW = Math.max(sB.width - sIn.left - sIn.right, curBounds.width);
        int maxH = Math.max(sB.height - sIn.top - sIn.bottom, curBounds.height);

        // First make sure the size is withing the visible part of the screen
        if (newW > maxW) {
            newW = maxW;
        }

        if (newH > maxH) {
            newH = maxH;
        }

        return new Rectangle(newX, newY, newW, newH);
    }

    /**
     * @see java.awt.peer.ComponentPeer#setBounds
     */
    public void setBounds(int x, int y, int width, int height, int op) {
        // TODO: Rewrite with WindowDimensions
        reshape(x, y, width, height, op, true);
        validateSurface();
    }

    // Coordinates are that of the shell
    void reconfigureContentWindow(WindowDimensions dims) {
        if (content == null) {
            insLog.fine("WARNING: Content window is null");
            return;
        }
        content.setContentBounds(dims);
    }

    boolean no_reparent_artifacts = false;
    public void handleConfigureNotifyEvent(XEvent xev) {
        assert (SunToolkit.isAWTLockHeldByCurrentThread());
        XConfigureEvent xe = xev.get_xconfigure();
        insLog.log(Level.FINE, "Configure notify {0}", new Object[] {xe});

        // XXX: should really only consider synthetic events, but
        if (isReparented()) {
            configure_seen = true;
        }

        if (!isMaximized()
            && (xe.get_serial() == reparent_serial || xe.get_window() != getShell())
            && !no_reparent_artifacts)
        {
            insLog.fine("- reparent artifact, skipping");
            return;
        }
        no_reparent_artifacts = false;

        /**
         * When there is a WM we receive some CN before being visible and after.
         * We should skip all CN which are before being visible, because we assume
         * the gravity is in action while it is not yet.
         *
         * When there is no WM we receive CN only _before_ being visible.
         * We should process these CNs.
         */
        if (!isVisible() && XWM.getWMID() != XWM.NO_WM) {
            insLog.fine(" - not visible, skipping");
            return;
        }

        /*
         * Some window managers configure before we are reparented and
         * the send event flag is set! ugh... (Enlighetenment for one,
         * possibly MWM as well).  If we haven't been reparented yet
         * this is just the WM shuffling us into position.  Ignore
         * it!!!! or we wind up in a bogus location.
         */
        int runningWM = XWM.getWMID();
        if (insLog.isLoggable(Level.FINE)) {
            insLog.log(Level.FINE, "reparented={0}, visible={1}, WM={2}, decorations={3}",
                    new Object[] {isReparented(), isVisible(), runningWM, getDecorations()});
        }
        if (!isReparented() && isVisible() && runningWM != XWM.NO_WM
                &&  !XWM.isNonReparentingWM()
                && getDecorations() != XWindowAttributesData.AWT_DECOR_NONE) {
            insLog.fine("- visible but not reparented, skipping");
            return;
        }
        //Last chance to correct insets
        if (!insets_corrected && getDecorations() != XWindowAttributesData.AWT_DECOR_NONE) {
            long parent = XlibUtil.getParentWindow(window);
            Insets correctWM = (parent != -1) ? XWM.getWM().getInsets(this, window, parent) : null;
            if (insLog.isLoggable(Level.FINER)) {
                if (correctWM != null) {
                    insLog.finer("Configure notify - insets : " + correctWM);
                } else {
                    insLog.finer("Configure notify - insets are still not available");
                }
            }
            if (correctWM != null) {
                handleCorrectInsets(correctWM);
            } else {
                //Only one attempt to correct insets is made (to lower risk)
                //if insets are still not available we simply set the flag
                insets_corrected = true;
            }
        }

        updateChildrenSizes();

        // Bounds of the window
        Rectangle targetBounds = new Rectangle(ComponentAccessor.getX((Component)target),
                ComponentAccessor.getY((Component)target),
                ComponentAccessor.getWidth((Component)target),
                ComponentAccessor.getHeight((Component)target));

        Point newLocation = targetBounds.getLocation();
        if (xe.get_send_event() || runningWM == XWM.NO_WM || XWM.isNonReparentingWM()) {
            // Location, Client size + insets
            newLocation = new Point(xe.get_x() - currentInsets.left, xe.get_y() - currentInsets.top);
        } else {
            // CDE/MWM/Metacity/Sawfish bug: if shell is resized using
            // top or left border, we don't receive synthetic
            // ConfigureNotify, only the one from X with zero
            // coordinates.  This is the workaround to get real
            // location, 6261336
            switch (XWM.getWMID()) {
                case XWM.CDE_WM:
                case XWM.MOTIF_WM:
                case XWM.METACITY_WM:
                case XWM.SAWFISH_WM:
                {
                    Point xlocation = queryXLocation();
                    if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "New X location: {0}", new Object[]{xlocation});
                    if (xlocation != null) {
                        newLocation = xlocation;
                    }
                    break;
                }
                default:
                    break;
            }
        }

        WindowDimensions newDimensions =
                new WindowDimensions(newLocation,
                new Dimension(xe.get_width(), xe.get_height()),
                copy(currentInsets),
                true);

        insLog.log(Level.FINER, "Insets are {0}, new dimensions {1}",
                new Object[] {currentInsets, newDimensions});

        checkIfOnNewScreen(newDimensions.getBounds());

        Point oldLocation = getLocation();
        dimensions = newDimensions;
        if (!newLocation.equals(oldLocation)) {
            handleMoved(newDimensions);
        }
        reconfigureContentWindow(newDimensions);
        updateChildrenSizes();
    }

    private void checkShellRectSize(Rectangle shellRect) {
        if (shellRect.width < 0) {
            shellRect.width = 1;
        }
        if (shellRect.height < 0) {
            shellRect.height = 1;
        }
    }

    private void checkShellRectPos(Rectangle shellRect) {
        int wm = XWM.getWMID();
        if (wm == XWM.MOTIF_WM || wm == XWM.CDE_WM) {
            if (shellRect.x == 0 && shellRect.y == 0) {
                shellRect.x = shellRect.y = 1;
            }
        }
    }

    private void checkShellRect(Rectangle shellRect) {
        checkShellRectSize(shellRect);
        checkShellRectPos(shellRect);
    }

    public void setShellBounds(Rectangle rec) {
        if (insLog.isLoggable(Level.FINE)) insLog.fine("Setting shell bounds on " +
                                                       this + " to " + rec);
        XToolkit.awtLock();
        try {
            updateSizeHints(rec.x, rec.y, rec.width, rec.height);
            XlibWrapper.XResizeWindow(XToolkit.getDisplay(), getShell(), rec.width, rec.height);
            XlibWrapper.XMoveWindow(XToolkit.getDisplay(), getShell(), rec.x, rec.y);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }
    public void setShellSize(Rectangle rec) {
        if (insLog.isLoggable(Level.FINE)) insLog.fine("Setting shell size on " +
                                                       this + " to " + rec);
        XToolkit.awtLock();
        try {
            updateSizeHints(rec.x, rec.y, rec.width, rec.height);
            XlibWrapper.XResizeWindow(XToolkit.getDisplay(), getShell(), rec.width, rec.height);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }
    public void setShellPosition(Rectangle rec) {
        if (insLog.isLoggable(Level.FINE)) insLog.fine("Setting shell position on " +
                                                       this + " to " + rec);
        XToolkit.awtLock();
        try {
            updateSizeHints(rec.x, rec.y, rec.width, rec.height);
            XlibWrapper.XMoveWindow(XToolkit.getDisplay(), getShell(), rec.x, rec.y);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    void initResizability() {
        setResizable(winAttr.initialResizability);
    }
    public void setResizable(boolean resizable) {
        int fs = winAttr.functions;
        if (!isResizable() && resizable) {
            insets = currentInsets = new Insets(0, 0, 0, 0);
            resetWMSetInsets();
            if (!isEmbedded()) {
                setReparented(false);
            }
            winAttr.isResizable = resizable;
            if ((fs & MWM_FUNC_ALL) != 0) {
                fs &= ~(MWM_FUNC_RESIZE | MWM_FUNC_MAXIMIZE);
            } else {
                fs |= (MWM_FUNC_RESIZE | MWM_FUNC_MAXIMIZE);
            }
            winAttr.functions = fs;
            XWM.setShellResizable(this);
        } else if (isResizable() && !resizable) {
            insets = currentInsets = new Insets(0, 0, 0, 0);
            resetWMSetInsets();
            if (!isEmbedded()) {
                setReparented(false);
            }
            winAttr.isResizable = resizable;
            if ((fs & MWM_FUNC_ALL) != 0) {
                fs |= (MWM_FUNC_RESIZE | MWM_FUNC_MAXIMIZE);
            } else {
                fs &= ~(MWM_FUNC_RESIZE | MWM_FUNC_MAXIMIZE);
            }
            winAttr.functions = fs;
            XWM.setShellNotResizable(this, dimensions, dimensions.getBounds(), false);
        }
    }

    Rectangle getShellBounds() {
        return dimensions.getClientRect();
    }

    public Rectangle getBounds() {
        return dimensions.getBounds();
    }

    public Dimension getSize() {
        return dimensions.getSize();
    }

    public int getX() {
        return dimensions.getLocation().x;
    }

    public int getY() {
        return dimensions.getLocation().y;
    }

    public Point getLocation() {
        return dimensions.getLocation();
    }

    public int getAbsoluteX() {
        // NOTE: returning this peer's location which is shell location
        return dimensions.getScreenBounds().x;
    }

    public int getAbsoluteY() {
        // NOTE: returning this peer's location which is shell location
        return dimensions.getScreenBounds().y;
    }

    public int getWidth() {
        return getSize().width;
    }

    public int getHeight() {
        return getSize().height;
    }

    final public WindowDimensions getDimensions() {
        return dimensions;
    }

    public Point getLocationOnScreen() {
        XToolkit.awtLock();
        try {
            if (configure_seen) {
                return toGlobal(0,0);
            } else {
                Point location = target.getLocation();
                if (insLog.isLoggable(Level.FINE))
                    insLog.log(Level.FINE, "getLocationOnScreen {0} not reparented: {1} ",
                               new Object[] {this, location});
                return location;
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }


/***************************************************************************************
 *              END            OF             I N S E T S   C O D E
 **************************************************************************************/

    protected boolean isEventDisabled(XEvent e) {
        switch (e.get_type()) {
            // Do not generate MOVED/RESIZED events since we generate them by ourselves
          case ConfigureNotify:
              return true;
          case EnterNotify:
          case LeaveNotify:
              // Disable crossing event on outer borders of Frame so
              // we receive only one set of cross notifications(first set is from content window)
              return true;
          default:
              return super.isEventDisabled(e);
        }
    }

    int getDecorations() {
        return winAttr.decorations;
    }

    int getFunctions() {
        return winAttr.functions;
    }

    public void setVisible(boolean vis) {
        log.log(Level.FINER, "Setting {0} to visible {1}", new Object[] {this, Boolean.valueOf(vis)});
        if (vis && !isVisible()) {
            XWM.setShellDecor(this);
            super.setVisible(vis);
            if (winAttr.isResizable) {
                //Fix for 4320050: Minimum size for java.awt.Frame is not being enforced.
                //We need to update frame's minimum size, not to reset it
                XWM.removeSizeHints(this, XlibWrapper.PMaxSize);
                updateMinimumSize();
            }
        } else {
            super.setVisible(vis);
        }
    }

    protected void suppressWmTakeFocus(boolean doSuppress) {
        XAtomList protocols = getWMProtocols();
        if (doSuppress) {
            protocols.remove(wm_take_focus);
        } else {
            protocols.add(wm_take_focus);
        }
        wm_protocols.setAtomListProperty(this, protocols);
    }

    public void dispose() {
        if (content != null) {
            content.destroy();
        }
        focusProxy.destroy();

        if (iconWindow != null) {
            iconWindow.destroy();
        }

        super.dispose();
    }

    public void handleClientMessage(XEvent xev) {
        super.handleClientMessage(xev);
        XClientMessageEvent cl = xev.get_xclient();
        if ((wm_protocols != null) && (cl.get_message_type() == wm_protocols.getAtom())) {
            if (cl.get_data(0) == wm_delete_window.getAtom()) {
                handleQuit();
            } else if (cl.get_data(0) == wm_take_focus.getAtom()) {
                handleWmTakeFocus(cl);
            }
        } else if (cl.get_message_type() == resize_request.getAtom()) {
            reshape((int)cl.get_data(0), (int)cl.get_data(1),
                    (int)cl.get_data(2), (int)cl.get_data(3),
                    (int)cl.get_data(4), true);
        }
    }

    private void handleWmTakeFocus(XClientMessageEvent cl) {
        focusLog.log(Level.FINE, "WM_TAKE_FOCUS on {0}", new Object[]{this});
        requestWindowFocus(cl.get_data(1), true);
    }

    /**
     * Requests focus to this decorated top-level by requesting X input focus
     * to the shell window.
     */
    protected void requestXFocus(long time, boolean timeProvided) {
        // We have proxied focus mechanism - instead of shell the focus is held
        // by "proxy" - invisible mapped window. When we want to set X input focus to
        // toplevel set it on proxy instead.
        if (focusProxy == null) {
            if (focusLog.isLoggable(Level.FINE)) focusLog.warning("Focus proxy is null for " + this);
        } else {
            if (focusLog.isLoggable(Level.FINE)) focusLog.fine("Requesting focus to proxy: " + focusProxy);
            if (timeProvided) {
                focusProxy.xRequestFocus(time);
            } else {
                focusProxy.xRequestFocus();
            }
        }
    }

    XFocusProxyWindow getFocusProxy() {
        return focusProxy;
    }

    public void handleQuit() {
        postEvent(new WindowEvent((Window)target, WindowEvent.WINDOW_CLOSING));
    }

    final void dumpMe() {
        System.err.println(">>> Peer: " + x + ", " + y + ", " + width + ", " + height);
    }

    final void dumpTarget() {
        int getWidth = ComponentAccessor.getWidth((Component)target);
        int getHeight = ComponentAccessor.getHeight((Component)target);
        int getTargetX = ComponentAccessor.getX((Component)target);
        int getTargetY = ComponentAccessor.getY((Component)target);
        System.err.println(">>> Target: " + getTargetX + ", " + getTargetY + ", " + getWidth + ", " + getHeight);
    }

    final void dumpShell() {
        dumpWindow("Shell", getShell());
    }
    final void dumpContent() {
        dumpWindow("Content", getContentWindow());
    }
    final void dumpParent() {
        long parent = XlibUtil.getParentWindow(getShell());
        if (parent != 0)
        {
            dumpWindow("Parent", parent);
        }
        else
        {
            System.err.println(">>> NO PARENT");
        }
    }

    final void dumpWindow(String id, long window) {
        XWindowAttributes pattr = new XWindowAttributes();
        try {
            XToolkit.awtLock();
            try {
                int status =
                    XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                     window, pattr.pData);
            }
            finally {
                XToolkit.awtUnlock();
            }
            System.err.println(">>>> " + id + ": " + pattr.get_x()
                               + ", " + pattr.get_y() + ", " + pattr.get_width()
                               + ", " + pattr.get_height());
        } finally {
            pattr.dispose();
        }
    }

    final void dumpAll() {
        dumpTarget();
        dumpMe();
        dumpParent();
        dumpShell();
        dumpContent();
    }

    boolean isMaximized() {
        return false;
    }

    boolean isOverrideRedirect() {
        return false;
    }

    public boolean requestWindowFocus(long time, boolean timeProvided) {
        focusLog.fine("Request for decorated window focus");
        // If this is Frame or Dialog we can't assure focus request success - but we still can try
        // If this is Window and its owner Frame is active we can be sure request succedded.
        Window win = (Window)target;
        Window focusedWindow = XKeyboardFocusManagerPeer.getCurrentNativeFocusedWindow();
        Window activeWindow = XWindowPeer.getDecoratedOwner(focusedWindow);

        focusLog.log(Level.FINER, "Current window is: active={0}, focused={1}",
                     new Object[]{ Boolean.valueOf(win == activeWindow),
                                   Boolean.valueOf(win == focusedWindow)});

        XWindowPeer toFocus = this;
        while (toFocus.nextTransientFor != null) {
            toFocus = toFocus.nextTransientFor;
        }

        if (this == toFocus) {
            if (focusAllowedFor()) {
                if (win == activeWindow && win != focusedWindow) {
                    // Happens when focus is on window child
                    focusLog.fine("Focus is on child window - transfering it back");
                    handleWindowFocusInSync(-1);
                } else {
                    focusLog.fine("Requesting focus to this window");
                    if (timeProvided) {
                        requestXFocus(time);
                    } else {
                        requestXFocus();
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        else if (toFocus.focusAllowedFor()) {
            focusLog.fine("Requesting focus to " + toFocus);
            if (timeProvided) {
                toFocus.requestXFocus(time);
            } else {
                toFocus.requestXFocus();
            }
            return false;
        }
        else
        {
            // This might change when WM will have property to determine focus policy.
            // Right now, because policy is unknown we can't be sure we succedded
            return false;
        }
    }

    XWindowPeer actualFocusedWindow = null;
    void setActualFocusedWindow(XWindowPeer actualFocusedWindow) {
        synchronized(getStateLock()) {
            this.actualFocusedWindow = actualFocusedWindow;
        }
    }

    boolean requestWindowFocus(XWindowPeer actualFocusedWindow,
                               long time, boolean timeProvided)
    {
        setActualFocusedWindow(actualFocusedWindow);
        return requestWindowFocus(time, timeProvided);
    }
    public void handleWindowFocusIn(long serial) {
        if (null == actualFocusedWindow) {
            super.handleWindowFocusIn(serial);
        } else {
            /*
             * Fix for 6314575.
             * If this is a result of clicking on one of the Frame's component
             * then 'actualFocusedWindow' shouldn't be focused. A decision of focusing
             * it or not should be made after the appropriate Java mouse event (if any)
             * is handled by the component where 'actualFocusedWindow' value may be reset.
             *
             * The fix is based on the empiric fact consisting in that the component
             * receives native mouse event nearly at the same time the Frame receives
             * WM_TAKE_FOCUS (when FocusIn is generated via XSetInputFocus call) but
             * definetely before the Frame gets FocusIn event (when this method is called).
             */
            postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    XWindowPeer fw = null;
                    synchronized (getStateLock()) {
                        fw = actualFocusedWindow;
                        actualFocusedWindow = null;
                        if (null == fw || !fw.isVisible() || !fw.isFocusableWindow()) {
                            fw = XDecoratedPeer.this;
                        }
                    }
                    fw.handleWindowFocusIn_Dispatch();
                }
            }));
        }
    }

    public void handleWindowFocusOut(Window oppositeWindow, long serial) {
        Window actualFocusedWindow = XKeyboardFocusManagerPeer.getCurrentNativeFocusedWindow();

        // If the actual focused window is not this decorated window then retain it.
        if (actualFocusedWindow != null && actualFocusedWindow != target) {
            Window owner = XWindowPeer.getDecoratedOwner(actualFocusedWindow);

            if (owner != null && owner == target) {
                setActualFocusedWindow((XWindowPeer) ComponentAccessor.getPeer(actualFocusedWindow));
            }
        }
        super.handleWindowFocusOut(oppositeWindow, serial);
    }

    private Point queryXLocation()
    {
        return XlibUtil.translateCoordinates(
            getContentWindow(),
            XlibWrapper.RootWindow(XToolkit.getDisplay(), getScreenNumber()),
            new Point(0, 0));
    }
}
