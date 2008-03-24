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
import java.awt.event.*;
import java.awt.peer.ComponentPeer;
import java.awt.image.ColorModel;

import java.lang.ref.WeakReference;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.logging.Level;
import java.util.logging.Logger;

import sun.awt.*;

import sun.awt.image.PixelConverter;

import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

public class XWindow extends XBaseWindow implements X11ComponentPeer {
    private static Logger log = Logger.getLogger("sun.awt.X11.XWindow");
    private static Logger insLog = Logger.getLogger("sun.awt.X11.insets.XWindow");
    private static Logger eventLog = Logger.getLogger("sun.awt.X11.event.XWindow");
    private static final Logger focusLog = Logger.getLogger("sun.awt.X11.focus.XWindow");
    private static Logger keyEventLog = Logger.getLogger("sun.awt.X11.kye.XWindow");
  /* If a motion comes in while a multi-click is pending,
   * allow a smudge factor so that moving the mouse by a small
   * amount does not wipe out the multi-click state variables.
   */
    private final static int AWT_MULTICLICK_SMUDGE = 4;
    // ButtonXXX events stuff
    static int rbutton = 0;
    static int lastX = 0, lastY = 0;
    static long lastTime = 0;
    static long lastButton = 0;
    static WeakReference lastWindowRef = null;
    static int clickCount = 0;

    // used to check if we need to re-create surfaceData.
    int oldWidth = -1;
    int oldHeight = -1;


    protected X11GraphicsConfig graphicsConfig;
    protected AwtGraphicsConfigData graphicsConfigData;

    private boolean reparented;

    XWindow parent;

    Component target;

    private static int JAWT_LOCK_ERROR=0x00000001;
    private static int JAWT_LOCK_CLIP_CHANGED=0x00000002;
    private static int JAWT_LOCK_BOUNDS_CHANGED=0x00000004;
    private static int JAWT_LOCK_SURFACE_CHANGED=0x00000008;
    private int drawState = JAWT_LOCK_CLIP_CHANGED |
    JAWT_LOCK_BOUNDS_CHANGED |
    JAWT_LOCK_SURFACE_CHANGED;

    public static final String TARGET = "target",
        REPARENTED = "reparented"; // whether it is reparented by default

    SurfaceData surfaceData;

    XRepaintArea paintArea;

    // fallback default font object
    private static Font defaultFont;

    static synchronized Font getDefaultFont() {
        if (null == defaultFont) {
            defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        }
        return defaultFont;
    }

    /*
     * Keeps all buttons which were pressed at the time of the last mouse
     * drag until all buttons will be released, contains state as bit masks
     * Button1Mask, Button2Mask, Button3Mask
     */
    private int mouseDragState = 0;

    native int getNativeColor(Color clr, GraphicsConfiguration gc);
    native void getWMInsets(long window, long left, long top, long right, long bottom, long border);
    native long getTopWindow(long window, long rootWin);
    native void getWindowBounds(long window, long x, long y, long width, long height);
    private native static void initIDs();

    private static Field isPostedField;
    static {
        initIDs();
    }

    XWindow(XCreateWindowParams params) {
        super(params);
    }

    XWindow() {
    }

    XWindow(long parentWindow, Rectangle bounds) {
        super(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds,
            PARENT_WINDOW, Long.valueOf(parentWindow)}));
    }

    XWindow(Component target, long parentWindow, Rectangle bounds) {
        super(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds,
            PARENT_WINDOW, Long.valueOf(parentWindow),
            TARGET, target}));
    }

    XWindow(Component target, long parentWindow) {
        this(target, parentWindow, target.getBounds());
    }

    XWindow(Component target) {
        this(target, (target.getParent() == null) ? 0 : getParentWindowID(target), target.getBounds());
    }

    XWindow(Object target) {
        this(null, 0, null);
    }

    /* This create is used by the XEmbeddedFramePeer since it has to create the window
       as a child of the netscape window. This netscape window is passed in as wid */
    XWindow(long parentWindow) {
        super(new XCreateWindowParams(new Object[] {
            PARENT_WINDOW, Long.valueOf(parentWindow),
            REPARENTED, Boolean.TRUE,
            EMBEDDED, Boolean.TRUE}));
    }

    protected void initGraphicsConfiguration() {
        graphicsConfig = (X11GraphicsConfig) target.getGraphicsConfiguration();
        graphicsConfigData = new AwtGraphicsConfigData(graphicsConfig.getAData());
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        reparented = Boolean.TRUE.equals(params.get(REPARENTED));

        target = (Component)params.get(TARGET);

        initGraphicsConfiguration();

        AwtGraphicsConfigData gData = getGraphicsConfigurationData();
        X11GraphicsConfig config = (X11GraphicsConfig) getGraphicsConfiguration();
        XVisualInfo visInfo = gData.get_awt_visInfo();
        params.putIfNull(EVENT_MASK, KeyPressMask | KeyReleaseMask
            | FocusChangeMask | ButtonPressMask | ButtonReleaseMask
            | EnterWindowMask | LeaveWindowMask | PointerMotionMask
            | ButtonMotionMask | ExposureMask | StructureNotifyMask);

        if (target != null) {
            params.putIfNull(BOUNDS, target.getBounds());
        } else {
            params.putIfNull(BOUNDS, new Rectangle(0, 0, MIN_SIZE, MIN_SIZE));
        }
        params.putIfNull(BORDER_PIXEL, Long.valueOf(0));
        getColorModel(); // fix 4948833: this call forces the color map to be initialized
        params.putIfNull(COLORMAP, gData.get_awt_cmap());
        params.putIfNull(DEPTH, gData.get_awt_depth());
        params.putIfNull(VISUAL_CLASS, Integer.valueOf((int)XlibWrapper.InputOutput));
        params.putIfNull(VISUAL, visInfo.get_visual());
        params.putIfNull(VALUE_MASK, XlibWrapper.CWBorderPixel | XlibWrapper.CWEventMask | XlibWrapper.CWColormap);
        Long parentWindow = (Long)params.get(PARENT_WINDOW);
        if (parentWindow == null || parentWindow.longValue() == 0) {
            XToolkit.awtLock();
            try {
                int screen = visInfo.get_screen();
                if (screen != -1) {
                    params.add(PARENT_WINDOW, XlibWrapper.RootWindow(XToolkit.getDisplay(), screen));
                } else {
                    params.add(PARENT_WINDOW, XToolkit.getDefaultRootWindow());
                }
            } finally {
                XToolkit.awtUnlock();
            }
        }

        paintArea = new XRepaintArea();
        if (target != null) {
            this.parent = getParentXWindowObject(target.getParent());
        }

        params.putIfNull(BACKING_STORE, XToolkit.getBackingStoreType());
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);

        setWMClass(getWMClass());

        surfaceData = graphicsConfig.createSurfaceData(this);
        Color c;
        if (target != null && (c = target.getBackground()) != null) {
            // We need a version of setBackground that does not call repaint !!
            // and one that does not get overridden. The problem is that in postInit
            // we call setBackground and we dont have all the stuff initialized to
            // do a full paint for most peers. So we cannot call setBackground in postInit.
            // instead we need to call xSetBackground.
            xSetBackground(c);
        }
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        if (graphicsConfig == null) {
            initGraphicsConfiguration();
        }
        return graphicsConfig;
    }

    public AwtGraphicsConfigData getGraphicsConfigurationData() {
        if (graphicsConfigData == null) {
            initGraphicsConfiguration();
        }
        return graphicsConfigData;
    }

    protected String[] getWMClass() {
        return new String[] {XToolkit.getCorrectXIDString(getClass().getName()), XToolkit.getAWTAppClassName()};
    }

    void setReparented(boolean newValue) {
        reparented = newValue;
    }

    boolean isReparented() {
        return reparented;
    }

    static long getParentWindowID(Component target) {

        ComponentPeer peer = target.getParent().getPeer();
        Component temp = target.getParent();
        while (!(peer instanceof XWindow))
        {
            temp = temp.getParent();
            peer = temp.getPeer();
        }

        if (peer != null && peer instanceof XWindow)
            return ((XWindow)peer).getContentWindow();
        else return 0;
    }


    static XWindow getParentXWindowObject(Component target) {
        if (target == null) return null;
        Component temp = target.getParent();
        if (temp == null) return null;
        ComponentPeer peer = temp.getPeer();
        if (peer == null) return null;
        while ((peer != null) && !(peer instanceof XWindow))
        {
            temp = temp.getParent();
            peer = temp.getPeer();
        }
        if (peer != null && peer instanceof XWindow)
            return (XWindow) peer;
        else return null;
    }


    boolean isParentOf(XWindow win) {
        if (!(target instanceof Container) || win == null || win.getTarget() == null) {
            return false;
        }
        Container parent = ComponentAccessor.getParent_NoClientCode(win.target);
        while (parent != null && parent != target) {
            parent = ComponentAccessor.getParent_NoClientCode(parent);
        }
        return (parent == target);
    }

    public Object getTarget() {
        return target;
    }
    public Component getEventSource() {
        return target;
    }

    public ColorModel getColorModel(int transparency) {
        return graphicsConfig.getColorModel (transparency);
    }

    public ColorModel getColorModel() {
        if (graphicsConfig != null) {
            return graphicsConfig.getColorModel ();
        }
        else {
            return XToolkit.getStaticColorModel();
        }
    }

    Graphics getGraphics(SurfaceData surfData, Color afore, Color aback, Font afont) {
        if (surfData == null) return null;

        Component target = (Component) this.target;

        /* Fix for bug 4746122. Color and Font shouldn't be null */
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
            font = XWindow.getDefaultFont();
        }
        return new SunGraphics2D(surfData, fgColor, bgColor, font);
    }

    public Graphics getGraphics() {
        return getGraphics(surfaceData,
                           target.getForeground(),
                           target.getBackground(),
                           target.getFont());
    }

    public FontMetrics getFontMetrics(Font font) {
        return Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    public Rectangle getTargetBounds() {
        return target.getBounds();
    }

    /**
     * Returns true if the event has been handled and should not be
     * posted to Java.
     */
    boolean prePostEvent(AWTEvent e) {
        return false;
    }

    static Method m_sendMessage;
    static void sendEvent(final AWTEvent e) {
        if (isPostedField == null) {
            isPostedField = SunToolkit.getField(AWTEvent.class, "isPosted");
        }
        PeerEvent pe = new PeerEvent(Toolkit.getDefaultToolkit(), new Runnable() {
                public void run() {
                    try {
                        isPostedField.setBoolean(e, true);
                    } catch (IllegalArgumentException e) {
                        assert(false);
                    } catch (IllegalAccessException e) {
                        assert(false);
                    }
                    ((Component)e.getSource()).dispatchEvent(e);
                }
            }, PeerEvent.ULTIMATE_PRIORITY_EVENT);
        if (focusLog.isLoggable(Level.FINER) && (e instanceof FocusEvent)) focusLog.finer("Sending " + e);
        XToolkit.postEvent(XToolkit.targetToAppContext(e.getSource()), pe);
    }


/*
 * Post an event to the event queue.
 */
// NOTE: This method may be called by privileged threads.
//       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    void postEvent(AWTEvent event) {
        XToolkit.postEvent(XToolkit.targetToAppContext(event.getSource()), event);
    }

    static void postEventStatic(AWTEvent event) {
        XToolkit.postEvent(XToolkit.targetToAppContext(event.getSource()), event);
    }

    public void postEventToEventQueue(final AWTEvent event) {
        //fix for 6239938 : Choice drop-down does not disappear when it loses focus, on XToolkit
        if (!prePostEvent(event)) {
            //event hasn't been handled and must be posted to EventQueue
            postEvent(event);
        }
    }

    // overriden in XCanvasPeer
    protected boolean doEraseBackground() {
        return true;
    }

    // We need a version of setBackground that does not call repaint !!
    // and one that does not get overridden. The problem is that in postInit
    // we call setBackground and we dont have all the stuff initialized to
    // do a full paint for most peers. So we cannot call setBackground in postInit.
    final public void xSetBackground(Color c) {
        XToolkit.awtLock();
        try {
            winBackground(c);
            // fix for 6558510: handle sun.awt.noerasebackground flag,
            // see doEraseBackground() and preInit() methods in XCanvasPeer
            if (!doEraseBackground()) {
                return;
            }
            // 6304250: XAWT: Items in choice show a blue border on OpenGL + Solaris10 when background color is set
            // Note: When OGL is enabled, surfaceData.pixelFor() will not
            // return a pixel value appropriate for passing to
            // XSetWindowBackground().  Therefore, we will use the ColorModel
            // for this component in order to calculate a pixel value from
            // the given RGB value.
            ColorModel cm = getColorModel();
            int pixel = PixelConverter.instance.rgbToPixel(c.getRGB(), cm);
            XlibWrapper.XSetWindowBackground(XToolkit.getDisplay(), getContentWindow(), pixel);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    public void setBackground(Color c) {
        xSetBackground(c);
    }

    Color backgroundColor;
    void winBackground(Color c) {
        backgroundColor = c;
    }

    public Color getWinBackground() {
        Color c = null;

        if (backgroundColor != null) {
            c = backgroundColor;
        } else if (parent != null) {
            c = parent.getWinBackground();
        }

        if (c instanceof SystemColor) {
            c = new Color(c.getRGB());
        }

        return c;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public  void repaint(int x,int y, int width, int height) {
        if (!isVisible()) {
            return;
        }
        Graphics g = getGraphics();
        if (g != null) {
            try {
                g.setClip(x,y,width,height);
                paint(g);
            } finally {
                g.dispose();
            }
        }
    }

    public  void repaint() {
        if (!isVisible()) {
            return;
        }
        Graphics g = getGraphics();
        if (g != null) {
            try {
                paint(g);
            } finally {
                g.dispose();
            }
        }
    }

    void paint(Graphics g) {
    }

    //used by Peers to avoid flickering withing paint()
    protected void flush(){
        XToolkit.awtLock();
        try {
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void popup(int x, int y, int width, int height) {
        // TBD: grab the pointer
        xSetBounds(x, y, width, height);
    }

    public void handleExposeEvent(XEvent xev) {
        super.handleExposeEvent(xev);
        XExposeEvent xe = xev.get_xexpose();
        if (isEventDisabled(xev)) {
            return;
        }
        int x = xe.get_x();
        int y = xe.get_y();
        int w = xe.get_width();
        int h = xe.get_height();

        Component target = (Component)getEventSource();

        if (!ComponentAccessor.getIgnoreRepaint(target)
            && ComponentAccessor.getWidth(target) != 0
            && ComponentAccessor.getHeight(target) != 0)
        {
            handleExposeEvent(target, x, y, w, h);
        }
    }

    public void handleExposeEvent(Component target, int x, int y, int w, int h) {
        PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().
            createPaintEvent(target, x, y, w, h);
        if (event != null) {
            postEventToEventQueue(event);
        }
    }

    static int getModifiers(int state, int button, int keyCode) {
        int modifiers = 0;

        if (((state & XlibWrapper.ShiftMask) != 0) ^ (keyCode == KeyEvent.VK_SHIFT)) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (((state & XlibWrapper.ControlMask) != 0) ^ (keyCode == KeyEvent.VK_CONTROL)) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (((state & XToolkit.metaMask) != 0) ^ (keyCode == KeyEvent.VK_META)) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }
        if (((state & XToolkit.altMask) != 0) ^ (keyCode == KeyEvent.VK_ALT)) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (((state & XToolkit.modeSwitchMask) != 0) ^ (keyCode == KeyEvent.VK_ALT_GRAPH)) {
            modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        }
        if (((state & XlibWrapper.Button1Mask) != 0) ^ (button == MouseEvent.BUTTON1)) {
            modifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if (((state & XlibWrapper.Button2Mask) != 0) ^ (button == MouseEvent.BUTTON2)) {
            modifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if (((state & XlibWrapper.Button3Mask) != 0) ^ (button == MouseEvent.BUTTON3)) {
            modifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        return modifiers;
    }

    static int getXModifiers(AWTKeyStroke stroke) {
        int mods = stroke.getModifiers();
        int res = 0;
        if ((mods & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
            res |= XToolkit.ShiftMask;
        }
        if ((mods & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
            res |= XToolkit.ControlMask;
        }
        if ((mods & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
            res |= XToolkit.altMask;
        }
        if ((mods & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
            res |= XToolkit.metaMask;
        }
        if ((mods & (InputEvent.ALT_GRAPH_DOWN_MASK | InputEvent.ALT_GRAPH_MASK)) != 0) {
            res |= XToolkit.modeSwitchMask;
        }
        return res;
    }

    private static int getButtonMask(long mouseButton) {
        if (mouseButton == XlibWrapper.Button1) {
            return XlibWrapper.Button1Mask;
        } else if (mouseButton == XlibWrapper.Button2) {
            return XlibWrapper.Button2Mask;
        } else if (mouseButton == XlibWrapper.Button3) {
            return XlibWrapper.Button3Mask;
        }
        return 0;
    }

    /**
     * Returns true if this event is disabled and shouldn't be passed to Java.
     * Default implementation returns false for all events.
     */
    static int getRightButtonNumber() {
        if (rbutton == 0) { // not initialized yet
            XToolkit.awtLock();
            try {
                rbutton = XlibWrapper.XGetPointerMapping(XToolkit.getDisplay(), XlibWrapper.ibuffer, 3);
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
        return rbutton;
    }

    static int getMouseMovementSmudge() {
        //TODO: It's possible to read corresponding settings
        return AWT_MULTICLICK_SMUDGE;
    }

    public void handleButtonPressRelease(XEvent xev) {
        super.handleButtonPressRelease(xev);
        XButtonEvent xbe = xev.get_xbutton();
        if (isEventDisabled(xev)) {
            return;
        }
        if (eventLog.isLoggable(Level.FINE)) eventLog.fine(xbe.toString());
        long when;
        int modifiers;
        boolean popupTrigger = false;
        int button=0;
        boolean wheel_mouse = false;
        long lbutton = xbe.get_button();
        int type = xev.get_type();
        when = xbe.get_time();
        long jWhen = XToolkit.nowMillisUTC_offset(when);

        int x = xbe.get_x();
        int y = xbe.get_y();
        if (xev.get_xany().get_window() != window) {
            Point localXY = toLocal(xbe.get_x_root(), xbe.get_y_root());
            x = localXY.x;
            y = localXY.y;
        }

        if (type == XlibWrapper.ButtonPress) {
            XWindow lastWindow = (lastWindowRef != null) ? ((XWindow)lastWindowRef.get()):(null);
            /*
               multiclick checking
            */
            if (eventLog.isLoggable(Level.FINEST)) eventLog.finest("lastWindow = " + lastWindow + ", lastButton "
                                                                   + lastButton + ", lastTime " + lastTime + ", multiClickTime "
                                                                   + XToolkit.getMultiClickTime());
            if (lastWindow == this && lastButton == lbutton && (when - lastTime) < XToolkit.getMultiClickTime()) {
                clickCount++;
            } else {
                clickCount = 1;
                lastWindowRef = new WeakReference(this);
                lastButton = lbutton;
                lastX = x;
                lastY = y;
            }
            lastTime = when;


            /*
               Check for popup trigger !!
            */
            if (lbutton == getRightButtonNumber() || lbutton > 2) {
                popupTrigger = true;
            } else {
                popupTrigger = false;
            }
        }

        if (lbutton == XlibWrapper.Button1)
            button = MouseEvent.BUTTON1;
        else if (lbutton ==  XlibWrapper.Button2 )
            button = MouseEvent.BUTTON2;
        else if (lbutton == XlibWrapper.Button3)
            button = MouseEvent.BUTTON3;
        else if (lbutton == XlibWrapper.Button4) {
            button = 4;
            wheel_mouse = true;
        } else if (lbutton == XlibWrapper.Button5) {
            button = 5;
            wheel_mouse = true;
        }

        modifiers = getModifiers(xbe.get_state(),button,0);

        if (!wheel_mouse) {
            MouseEvent me = new MouseEvent((Component)getEventSource(),
                                           type == XlibWrapper.ButtonPress ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                                           jWhen,modifiers, x, y,
                                           xbe.get_x_root(),
                                           xbe.get_y_root(),
                                           clickCount,popupTrigger,button);

            postEventToEventQueue(me);

            if (((mouseDragState & getButtonMask(lbutton)) == 0) && // No up-button in the drag-state
                (type == XlibWrapper.ButtonRelease))
            {
                postEventToEventQueue(me = new MouseEvent((Component)getEventSource(),
                                                     MouseEvent.MOUSE_CLICKED,
                                                     jWhen,
                                                     modifiers,
                                                     x, y,
                                                     xbe.get_x_root(),
                                                     xbe.get_y_root(),
                                                     clickCount,
                                                     false, button));
            }

        }
        else {
            if (xev.get_type() == XlibWrapper.ButtonPress) {
                MouseWheelEvent mwe = new MouseWheelEvent((Component)getEventSource(),MouseEvent.MOUSE_WHEEL, jWhen,
                                                          modifiers,
                                                          x, y,
                                                          xbe.get_x_root(),
                                                          xbe.get_y_root(),
                                                          clickCount,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,
                                                          3,button==4 ?  -1 : 1);
                postEventToEventQueue(mwe);
            }
        }

        mouseDragState &= ~getButtonMask(lbutton); // Exclude the up-button from the drag-state
    }

    public void handleMotionNotify(XEvent xev) {
        super.handleMotionNotify(xev);
        XMotionEvent xme = xev.get_xmotion();
        if (isEventDisabled(xev)) {
            return;
        }

        int mouseKeyState = (xme.get_state() & (Button1Mask | Button2Mask | Button3Mask));
        boolean isDragging = (mouseKeyState != 0);
        int mouseEventType = 0;

        if (isDragging) {
            mouseEventType = MouseEvent.MOUSE_DRAGGED;
        } else {
            mouseEventType = MouseEvent.MOUSE_MOVED;
        }

        /*
           Fix for 6176814 .  Add multiclick checking.
        */
        int x = xme.get_x();
        int y = xme.get_y();
        XWindow lastWindow = (lastWindowRef != null) ? ((XWindow)lastWindowRef.get()):(null);

        if (!(lastWindow == this &&
              (xme.get_time() - lastTime) < XToolkit.getMultiClickTime()  &&
              (Math.abs(lastX - x) < AWT_MULTICLICK_SMUDGE &&
               Math.abs(lastY - y) < AWT_MULTICLICK_SMUDGE))) {
          clickCount = 0;
          lastWindowRef = null;
          mouseDragState = mouseKeyState;
          lastTime = 0;
          lastX = 0;
          lastY = 0;
        }

        long jWhen = XToolkit.nowMillisUTC_offset(xme.get_time());
        int modifiers = getModifiers(xme.get_state(), 0, 0);
        boolean popupTrigger = false;

        Component source = (Component)getEventSource();

        if (xme.get_window() != window) {
            Point localXY = toLocal(xme.get_x_root(), xme.get_y_root());
            x = localXY.x;
            y = localXY.y;
        }
        /* Fix for 5039416.
         * According to canvas.c we shouldn't post any MouseEvent if mouse is dragging and clickCount!=0.
         */
        if ((isDragging && clickCount == 0) || !isDragging) {
            MouseEvent mme = new MouseEvent(source, mouseEventType, jWhen,
                                            modifiers, x, y, xme.get_x_root(), xme.get_y_root(),
                                            clickCount, popupTrigger, MouseEvent.NOBUTTON);
            postEventToEventQueue(mme);
        }
    }


    // REMIND: need to implement looking for disabled events
    public native boolean x11inputMethodLookupString(long event, long [] keysymArray);
    native boolean haveCurrentX11InputMethodInstance();

    public void handleXCrossingEvent(XEvent xev) {
        super.handleXCrossingEvent(xev);
        XCrossingEvent xce = xev.get_xcrossing();

        if (eventLog.isLoggable(Level.FINEST)) eventLog.finest(xce.toString());

        // Skip event If it was caused by a grab
        // This is needed because on displays with focus-follows-mouse on MousePress X system generates
        // two XCrossing events with mode != NormalNotify. First of them notifies that the mouse has left
        // current component. Second one notifies that it has entered into the same component.
        // This looks like the window under the mouse has actually changed and Java handle these  events
        // accordingly. This leads to impossibility to make a double click on Component (6404708)
        XWindowPeer toplevel = getToplevelXWindow();
        if (toplevel != null && !toplevel.isModalBlocked()){
            if (xce.get_mode() != NotifyNormal) {
                // 6404708 : need update cursor in accordance with skipping Leave/EnterNotify event
                // whereas it doesn't need to handled further.
                if (xce.get_type() == EnterNotify) {
                    XAwtState.setComponentMouseEntered(getEventSource());
                    XGlobalCursorManager.nativeUpdateCursor(getEventSource());
                } else { // LeaveNotify:
                    XAwtState.setComponentMouseEntered(null);
                }
                return;
            }
        }
        // X sends XCrossing to all hierarchy so if the edge of child equals to
        // ancestor and mouse enters child, the ancestor will get an event too.
        // From java point the event is bogus as ancestor is obscured, so if
        // the child can get java event itself, we skip it on ancestor.
        long childWnd = xce.get_subwindow();
        if (childWnd != None) {
            XBaseWindow child = XToolkit.windowToXWindow(childWnd);
            if (child != null && child instanceof XWindow &&
                !child.isEventDisabled(xev))
            {
                return;
            }
        }

        // Remember old component with mouse to have the opportunity to send it MOUSE_EXITED.
        final Component compWithMouse = XAwtState.getComponentMouseEntered();
        if (toplevel != null) {
            if(!toplevel.isModalBlocked()){
                if (xce.get_type() == EnterNotify) {
                    // Change XAwtState's component mouse entered to the up-to-date one before requesting
                    // to update the cursor since XAwtState.getComponentMouseEntered() is used when the
                    // cursor is updated (in XGlobalCursorManager.findHeavyweightUnderCursor()).
                    XAwtState.setComponentMouseEntered(getEventSource());
                    XGlobalCursorManager.nativeUpdateCursor(getEventSource());
                } else { // LeaveNotify:
                    XAwtState.setComponentMouseEntered(null);
                }
            } else {
                ((XComponentPeer) ComponentAccessor.getPeer(target))
                    .pSetCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        if (isEventDisabled(xev)) {
            return;
        }

        long jWhen = XToolkit.nowMillisUTC_offset(xce.get_time());
        int modifiers = getModifiers(xce.get_state(),0,0);
        int clickCount = 0;
        boolean popupTrigger = false;
        int x = xce.get_x();
        int y = xce.get_y();
        if (xce.get_window() != window) {
            Point localXY = toLocal(xce.get_x_root(), xce.get_y_root());
            x = localXY.x;
            y = localXY.y;
        }

        // This code tracks boundary crossing and ensures MOUSE_ENTER/EXIT
        // are posted in alternate pairs
        if (compWithMouse != null) {
            MouseEvent me = new MouseEvent(compWithMouse,
                MouseEvent.MOUSE_EXITED, jWhen, modifiers, xce.get_x(),
                xce.get_y(), xce.get_x_root(), xce.get_y_root(), clickCount, popupTrigger,
                MouseEvent.NOBUTTON);
            postEventToEventQueue(me);
            eventLog.finest("Clearing last window ref");
            lastWindowRef = null;
        }
        if (xce.get_type() == EnterNotify) {
            MouseEvent me = new MouseEvent(getEventSource(), MouseEvent.MOUSE_ENTERED,
                jWhen, modifiers, xce.get_x(), xce.get_y(), xce.get_x_root(), xce.get_y_root(), clickCount,
                popupTrigger, MouseEvent.NOBUTTON);
            postEventToEventQueue(me);
        }
    }

    public void doLayout(int x, int y, int width, int height) {}

    public void handleConfigureNotifyEvent(XEvent xev) {
        Rectangle oldBounds = getBounds();

        super.handleConfigureNotifyEvent(xev);
        insLog.log(Level.FINER, "Configure, {0}, event disabled: {1}",
                   new Object[] {xev.get_xconfigure(), isEventDisabled(xev)});
        if (isEventDisabled(xev)) {
            return;
        }

//  if ( Check if it's a resize, a move, or a stacking order change )
//  {
        Rectangle bounds = getBounds();
        if (!bounds.getSize().equals(oldBounds.getSize())) {
            postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_RESIZED));
        }
        if (!bounds.getLocation().equals(oldBounds.getLocation())) {
            postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_MOVED));
        }
//  }
    }

    public void handleMapNotifyEvent(XEvent xev) {
        super.handleMapNotifyEvent(xev);
        log.log(Level.FINE, "Mapped {0}", new Object[] {this});
        if (isEventDisabled(xev)) {
            return;
        }
        ComponentEvent ce;

        ce = new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_SHOWN);
        postEventToEventQueue(ce);
    }

    public void handleUnmapNotifyEvent(XEvent xev) {
        super.handleUnmapNotifyEvent(xev);
        if (isEventDisabled(xev)) {
            return;
        }
        ComponentEvent ce;

        ce = new ComponentEvent(target, ComponentEvent.COMPONENT_HIDDEN);
        postEventToEventQueue(ce);
    }

    private void dumpKeysymArray(XKeyEvent ev) {
        keyEventLog.fine("  "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 0))+
                         "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 1))+
                         "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 2))+
                         "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 3)));
    }
    /**
       Return unicode character or 0 if no correspondent character found.
       Parameter is a keysym basically from keysymdef.h
       XXX: how about vendor keys? Is there some with Unicode value and not in the list?
    */
    char keysymToUnicode( long keysym, int state ) {
        return XKeysym.convertKeysym( keysym, state );
    }
    int keyEventType2Id( int xEventType ) {
        return xEventType == XConstants.KeyPress ? java.awt.event.KeyEvent.KEY_PRESSED :
               xEventType == XConstants.KeyRelease ? java.awt.event.KeyEvent.KEY_RELEASED : 0;
    }
    static private long xkeycodeToKeysym(XKeyEvent ev) {
        return XKeysym.getKeysym( ev );
    }
    void logIncomingKeyEvent(XKeyEvent ev) {
        keyEventLog.fine("--XWindow.java:handleKeyEvent:"+ev);
        dumpKeysymArray(ev);
        keyEventLog.fine("XXXXXXXXXXXXXX javakeycode will be most probably:0x"+ Integer.toHexString(XKeysym.getJavaKeycodeOnly(ev)));
    }
    public void handleKeyPress(XEvent xev) {
        super.handleKeyPress(xev);
        XKeyEvent ev = xev.get_xkey();
        if (eventLog.isLoggable(Level.FINE)) eventLog.fine(ev.toString());
        if (isEventDisabled(xev)) {
            return;
        }
        handleKeyPress(ev);
    }
    // called directly from this package, unlike handleKeyRelease.
    // un-final it if you need to override it in a subclass.
    final void handleKeyPress(XKeyEvent ev) {
        long keysym[] = new long[2];
        char unicodeKey = 0;
        keysym[0] = NoSymbol;

        if (keyEventLog.isLoggable(Level.FINE)) {
            logIncomingKeyEvent( ev );
        }
        if ( //TODO check if there's an active input method instance
             // without calling a native method. Is it necessary though?
            haveCurrentX11InputMethodInstance()) {
            if (x11inputMethodLookupString(ev.pData, keysym)) {
                if (keyEventLog.isLoggable(Level.FINE)) {
                    keyEventLog.fine("--XWindow.java XIM did process event; return; dec keysym processed:"+(keysym[0])+
                                   "; hex keysym processed:"+Long.toHexString(keysym[0])
                                   );
                }
                return;
            }else {
                unicodeKey = keysymToUnicode( keysym[0], ev.get_state() );
                if (keyEventLog.isLoggable(Level.FINE)) {
                    keyEventLog.fine("--XWindow.java XIM did NOT process event, hex keysym:"+Long.toHexString(keysym[0])+"\n"+
                                     "                                         unicode key:"+Integer.toHexString((int)unicodeKey));
                }
            }
        }else  {
            // No input method instance found. For example, there's a Java Input Method.
            // Produce do-it-yourself keysym and perhaps unicode character.
            keysym[0] = xkeycodeToKeysym(ev);
            unicodeKey = keysymToUnicode( keysym[0], ev.get_state() );
            if (keyEventLog.isLoggable(Level.FINE)) {
                keyEventLog.fine("--XWindow.java XIM is absent;             hex keysym:"+Long.toHexString(keysym[0])+"\n"+
                                 "                                         unicode key:"+Integer.toHexString((int)unicodeKey));
            }
        }
        // Keysym should be converted to Unicode, if possible and necessary,
        // and Java KeyEvent keycode should be calculated.
        // For press we should post pressed & typed Java events.
        //
        // Press event might be not processed to this time because
        //  (1) either XIM could not handle it or
        //  (2) it was Latin 1:1 mapping.
        //
        XKeysym.Keysym2JavaKeycode jkc = XKeysym.getJavaKeycode(ev);
        if( jkc == null ) {
            jkc = new XKeysym.Keysym2JavaKeycode(java.awt.event.KeyEvent.VK_UNDEFINED, java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN);
        }
        if (keyEventLog.isLoggable(Level.FINE)) {
            keyEventLog.fine(">>>Fire Event:"+
               (ev.get_type() == XConstants.KeyPress ? "KEY_PRESSED; " : "KEY_RELEASED; ")+
               "jkeycode:decimal="+jkc.getJavaKeycode()+
               ", hex=0x"+Integer.toHexString(jkc.getJavaKeycode())+"; "
            );
        }
        postKeyEvent( java.awt.event.KeyEvent.KEY_PRESSED,
                          ev.get_time(),
                          jkc.getJavaKeycode(),
                          (unicodeKey == 0 ? java.awt.event.KeyEvent.CHAR_UNDEFINED : unicodeKey),
                          jkc.getKeyLocation(),
                          ev.get_state(),ev.getPData(), XKeyEvent.getSize());
        if( unicodeKey > 0 ) {
                keyEventLog.fine("fire _TYPED on "+unicodeKey);
                postKeyEvent( java.awt.event.KeyEvent.KEY_TYPED,
                              ev.get_time(),
                              java.awt.event.KeyEvent.VK_UNDEFINED,
                              unicodeKey,
                              java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN,
                              ev.get_state(),ev.getPData(), XKeyEvent.getSize());
        }


    }

    public void handleKeyRelease(XEvent xev) {
        super.handleKeyRelease(xev);
        XKeyEvent ev = xev.get_xkey();
        if (eventLog.isLoggable(Level.FINE)) eventLog.fine(ev.toString());
        if (isEventDisabled(xev)) {
            return;
        }
        handleKeyRelease(ev);
    }
    // un-private it if you need to call it from elsewhere
    private void handleKeyRelease(XKeyEvent ev) {
        long keysym[] = new long[2];
        char unicodeKey = 0;
        keysym[0] = NoSymbol;

        if (keyEventLog.isLoggable(Level.FINE)) {
            logIncomingKeyEvent( ev );
        }
        // Keysym should be converted to Unicode, if possible and necessary,
        // and Java KeyEvent keycode should be calculated.
        // For release we should post released event.
        //
        XKeysym.Keysym2JavaKeycode jkc = XKeysym.getJavaKeycode(ev);
        if( jkc == null ) {
            jkc = new XKeysym.Keysym2JavaKeycode(java.awt.event.KeyEvent.VK_UNDEFINED, java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN);
        }
        if (keyEventLog.isLoggable(Level.FINE)) {
            keyEventLog.fine(">>>Fire Event:"+
               (ev.get_type() == XConstants.KeyPress ? "KEY_PRESSED; " : "KEY_RELEASED; ")+
               "jkeycode:decimal="+jkc.getJavaKeycode()+
               ", hex=0x"+Integer.toHexString(jkc.getJavaKeycode())+"; "
            );
        }
        // We obtain keysym from IM and derive unicodeKey from it for KeyPress only.
        // We used to cache that value and retrieve it on KeyRelease,
        // but in case for example of a dead key+vowel pair, a vowel after a deadkey
        // might never be cached before.
        // Also, switching between keyboard layouts, we might cache a wrong letter.
        // That's why we use the same procedure as if there was no IM instance: do-it-yourself unicode.
        unicodeKey = keysymToUnicode( xkeycodeToKeysym(ev), ev.get_state() );

        postKeyEvent(  java.awt.event.KeyEvent.KEY_RELEASED,
                          ev.get_time(),
                          jkc.getJavaKeycode(),
                          (unicodeKey == 0 ? java.awt.event.KeyEvent.CHAR_UNDEFINED : unicodeKey),
                          jkc.getKeyLocation(),
                          ev.get_state(),ev.getPData(), XKeyEvent.getSize());

    }

    public void reshape(Rectangle bounds) {
        reshape(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void reshape(int x, int y, int width, int height) {
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        xSetBounds(x, y, width, height);
        // Fixed 6322593, 6304251, 6315137:
        // XWindow's SurfaceData should be invalidated and recreated as part
        // of the process of resizing the window
        // see the evaluation of the bug 6304251 for more information
        validateSurface();
        layout();
    }

    public void layout() {}

    boolean isShowing() {
        return visible;
    }

    boolean isResizable() {
        return true;
    }

    boolean isLocationByPlatform() {
        return false;
    }

    void updateSizeHints() {
        updateSizeHints(x, y, width, height);
    }

    void updateSizeHints(int x, int y, int width, int height) {
        long flags = XlibWrapper.PSize | (isLocationByPlatform() ? 0 : (XlibWrapper.PPosition | XlibWrapper.USPosition));
        if (!isResizable()) {
            log.log(Level.FINER, "Window {0} is not resizable", new Object[] {this});
            flags |= XlibWrapper.PMinSize | XlibWrapper.PMaxSize;
        } else {
            log.log(Level.FINER, "Window {0} is resizable", new Object[] {this});
        }
        setSizeHints(flags, x, y, width, height);
    }

    void updateSizeHints(int x, int y) {
        long flags = isLocationByPlatform() ? 0 : (XlibWrapper.PPosition | XlibWrapper.USPosition);
        if (!isResizable()) {
            log.log(Level.FINER, "Window {0} is not resizable", new Object[] {this});
            flags |= XlibWrapper.PMinSize | XlibWrapper.PMaxSize | XlibWrapper.PSize;
        } else {
            log.log(Level.FINER, "Window {0} is resizable", new Object[] {this});
        }
        setSizeHints(flags, x, y, width, height);
    }

      void validateSurface() {
        if ((width != oldWidth) || (height != oldHeight)) {
            SurfaceData oldData = surfaceData;
            if (oldData != null) {
                surfaceData = graphicsConfig.createSurfaceData(this);
                oldData.invalidate();
            }
            oldWidth = width;
            oldHeight = height;
        }
    }

    public SurfaceData getSurfaceData() {
        return surfaceData;
    }

    public void dispose() {
        SurfaceData oldData = surfaceData;
        surfaceData = null;
        if (oldData != null) {
            oldData.invalidate();
        }
        XToolkit.targetDisposedPeer(target, this);
        destroy();
    }

    public Point getLocationOnScreen() {
        synchronized (target.getTreeLock()) {
            Component comp = target;

            while (comp != null && !(comp instanceof Window)) {
                comp = ComponentAccessor.getParent_NoClientCode(comp);
            }

            // applets, embedded, etc - translate directly
            // XXX: override in subclass?
            if (comp == null || comp instanceof sun.awt.EmbeddedFrame) {
                return toGlobal(0, 0);
            }

            XToolkit.awtLock();
            try {
                Object wpeer = XToolkit.targetToPeer(comp);
                if (wpeer == null
                    || !(wpeer instanceof XDecoratedPeer)
                    || ((XDecoratedPeer)wpeer).configure_seen)
                {
                    return toGlobal(0, 0);
                }

                // wpeer is an XDecoratedPeer not yet fully adopted by WM
                Point pt = toOtherWindow(getContentWindow(),
                                         ((XDecoratedPeer)wpeer).getContentWindow(),
                                         0, 0);

                if (pt == null) {
                    pt = new Point(((XBaseWindow)wpeer).getAbsoluteX(), ((XBaseWindow)wpeer).getAbsoluteY());
                }
                pt.x += comp.getX();
                pt.y += comp.getY();
                return pt;
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }


    static Field bdata;
    static void setBData(KeyEvent e, byte[] data) {
        try {
            if (bdata == null) {
                bdata = SunToolkit.getField(java.awt.AWTEvent.class, "bdata");
            }
            bdata.set(e, data);
        } catch (IllegalAccessException ex) {
            assert false;
        }
    }

    public void postKeyEvent(int id, long when, int keyCode, char keyChar,
        int keyLocation, int state, long event, int eventSize)
    {
        long jWhen = XToolkit.nowMillisUTC_offset(when);
        int modifiers = getModifiers(state, 0, keyCode);
        KeyEvent ke = new KeyEvent((Component)getEventSource(), id, jWhen,
                                   modifiers, keyCode, keyChar, keyLocation);
        if (event != 0) {
            byte[] data = Native.toBytes(event, eventSize);
            setBData(ke, data);
        }
        postEventToEventQueue(ke);
    }

    static native int getAWTKeyCodeForKeySym(int keysym);
    static native int getKeySymForAWTKeyCode(int keycode);
}
