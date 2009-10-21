/*
 * Copyright 2003-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
import sun.util.logging.PlatformLogger;

class XWINProtocol extends XProtocol implements XStateProtocol, XLayerProtocol {
    final static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XWINProtocol");

/* Gnome WM spec  */
    XAtom XA_WIN_SUPPORTING_WM_CHECK = XAtom.get("_WIN_SUPPORTING_WM_CHECK");
    XAtom XA_WIN_PROTOCOLS = XAtom.get("_WIN_PROTOCOLS");
    XAtom XA_WIN_STATE = XAtom.get("_WIN_STATE");

    public boolean supportsState(int state) {
        return doStateProtocol();   // TODO - check for Frame constants
    }

    public void setState(XWindowPeer window, int state) {
        if (window.isShowing()) {
            /*
             * Request state transition from a Gnome WM (_WIN protocol) by sending
             * _WIN_STATE ClientMessage to root window.
             */
            long win_state = 0;

            if ( (state & Frame.MAXIMIZED_VERT) != 0) {
                win_state |= WIN_STATE_MAXIMIZED_VERT;
            }
            if ( (state & Frame.MAXIMIZED_HORIZ) != 0) {
                win_state |= WIN_STATE_MAXIMIZED_HORIZ;
            }

            XClientMessageEvent req = new XClientMessageEvent();
            req.set_type(XConstants.ClientMessage);
            req.set_window(window.getWindow());
            req.set_message_type(XA_WIN_STATE.getAtom());
            req.set_format(32);
            req.set_data(0, (WIN_STATE_MAXIMIZED_HORIZ | WIN_STATE_MAXIMIZED_VERT));
            req.set_data(1, win_state);
            if (log.isLoggable(PlatformLogger.FINE)) log.fine("Sending WIN_STATE to root to change the state to " + win_state);
            try {
                XToolkit.awtLock();
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                        XlibWrapper.RootWindow(XToolkit.getDisplay(),
                            window.getScreenNumber()),
                        false,
                        XConstants.SubstructureRedirectMask | XConstants.SubstructureNotifyMask,
                        req.pData);
            }
            finally {
                XToolkit.awtUnlock();
            }
            req.dispose();
        } else {
            /*
             * Specify initial state for a Gnome WM (_WIN protocol) by setting
             * WIN_STATE property on the window to the desired state before
             * mapping it.
             */
            /* Be careful to not wipe out state bits we don't understand */
            long win_state = XA_WIN_STATE.getCard32Property(window);
            long old_win_state = win_state;

            /*
             * In their stupid quest of reinventing every wheel, Gnome WM spec
             * have its own "minimized" hint (instead of using initial state
             * and WM_STATE hints).  This is bogus, but, apparently, some WMs
             * pay attention.
             */
            if ((state & Frame.ICONIFIED) != 0) {
                win_state |= WIN_STATE_MINIMIZED;
            } else {
                win_state &= ~WIN_STATE_MINIMIZED;
            }

            if ((state & Frame.MAXIMIZED_VERT) != 0) {
                win_state |= WIN_STATE_MAXIMIZED_VERT;
            } else {
                win_state &= ~WIN_STATE_MAXIMIZED_VERT;
            }

            if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
                win_state |= WIN_STATE_MAXIMIZED_HORIZ;
            } else {
                win_state &= ~WIN_STATE_MAXIMIZED_HORIZ;
            }
            if ((old_win_state ^ win_state) != 0) {
                if (log.isLoggable(PlatformLogger.FINE)) log.fine("Setting WIN_STATE on " + window + " to change the state to " + win_state);
                XA_WIN_STATE.setCard32Property(window, win_state);
            }
        }
    }

    public int getState(XWindowPeer window) {
        long win_state = XA_WIN_STATE.getCard32Property(window);
        int java_state = Frame.NORMAL;
        if ((win_state & WIN_STATE_MAXIMIZED_VERT) != 0) {
            java_state |= Frame.MAXIMIZED_VERT;
        }
        if ((win_state & WIN_STATE_MAXIMIZED_HORIZ) != 0) {
            java_state |= Frame.MAXIMIZED_HORIZ;
        }
        return java_state;
    }

    public boolean isStateChange(XPropertyEvent e) {
        return doStateProtocol() && e.get_atom() == XA_WIN_STATE.getAtom();
    }

    public void unshadeKludge(XWindowPeer window) {
        long win_state = XA_WIN_STATE.getCard32Property(window);
        if ((win_state & WIN_STATE_SHADED) == 0) {
            return;
        }
        win_state &= ~WIN_STATE_SHADED;
        XA_WIN_STATE.setCard32Property(window, win_state);
    }

    public boolean supportsLayer(int layer) {
        return ((layer == LAYER_ALWAYS_ON_TOP) || (layer == LAYER_NORMAL)) && doLayerProtocol();
    }

    public void setLayer(XWindowPeer window, int layer) {
        if (window.isShowing()) {
            XClientMessageEvent req = new XClientMessageEvent();
            req.set_type(XConstants.ClientMessage);
            req.set_window(window.getWindow());
            req.set_message_type(XA_WIN_LAYER.getAtom());
            req.set_format(32);
            req.set_data(0, layer == LAYER_NORMAL ? WIN_LAYER_NORMAL : WIN_LAYER_ONTOP);
            req.set_data(1, 0);
            req.set_data(2, 0);
            if (log.isLoggable(PlatformLogger.FINE)) log.fine("Setting layer " + layer + " by root message : " + req);
            XToolkit.awtLock();
            try {
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                        XlibWrapper.RootWindow(XToolkit.getDisplay(),
                            window.getScreenNumber()),
                        false,
                        /*XConstants.SubstructureRedirectMask | */XConstants.SubstructureNotifyMask,
                        req.pData);
            }
            finally {
                XToolkit.awtUnlock();
            }
            req.dispose();
        } else {
            if (log.isLoggable(PlatformLogger.FINE)) log.fine("Setting layer property to " + layer);
            XA_WIN_LAYER.setCard32Property(window, layer == LAYER_NORMAL ? WIN_LAYER_NORMAL : WIN_LAYER_ONTOP);
        }
    }

    XAtom XA_WIN_LAYER = XAtom.get("_WIN_LAYER");

/* _WIN_STATE bits */
    final static int WIN_STATE_STICKY          =(1<<0); /* everyone knows sticky            */
    final static int WIN_STATE_MINIMIZED       =(1<<1); /* Reserved - definition is unclear */
    final static int WIN_STATE_MAXIMIZED_VERT  =(1<<2); /* window in maximized V state      */
    final static int WIN_STATE_MAXIMIZED_HORIZ =(1<<3); /* window in maximized H state      */
    final static int WIN_STATE_HIDDEN          =(1<<4); /* not on taskbar but window visible*/
    final static int WIN_STATE_SHADED          =(1<<5); /* shaded (MacOS / Afterstep style) */
/* _WIN_LAYER values */
    final static int WIN_LAYER_ONTOP = 6;
    final static int WIN_LAYER_NORMAL = 4;

    long WinWindow = 0;
    boolean supportChecked = false;
    void detect() {
        if (supportChecked) {
            return;
        }
        WinWindow = checkAnchor(XA_WIN_SUPPORTING_WM_CHECK, XAtom.XA_CARDINAL);
        supportChecked = true;
        if (log.isLoggable(PlatformLogger.FINE)) log.fine("### " + this + " is active: " + (WinWindow != 0));
    }

    boolean active() {
        detect();
        return WinWindow != 0;
    }
    boolean doStateProtocol() {
        boolean res = active() && checkProtocol(XA_WIN_PROTOCOLS, XA_WIN_STATE);
        if (log.isLoggable(PlatformLogger.FINE)) log.fine("### " + this + " supports state: " + res);
        return res;
    }

    boolean doLayerProtocol() {
        boolean res = active() && checkProtocol(XA_WIN_PROTOCOLS, XA_WIN_LAYER);
        if (log.isLoggable(PlatformLogger.FINE)) log.fine("### " + this + " supports layer: " + res);
        return res;
    }
}
