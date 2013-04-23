/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.opengl;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import sun.awt.CGraphicsConfig;
import sun.awt.CGraphicsDevice;
import sun.awt.image.OffScreenImage;
import sun.awt.image.SunVolatileImage;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;
import sun.java2d.Surface;
import sun.java2d.SurfaceData;
import sun.java2d.opengl.OGLContext.OGLContextCaps;
import sun.java2d.pipe.hw.AccelSurface;
import sun.java2d.pipe.hw.AccelTypedVolatileImage;
import sun.java2d.pipe.hw.ContextCapabilities;
import static sun.java2d.opengl.OGLSurfaceData.*;
import static sun.java2d.opengl.OGLContext.OGLContextCaps.*;
import sun.java2d.pipe.hw.AccelDeviceEventListener;
import sun.java2d.pipe.hw.AccelDeviceEventNotifier;

import sun.lwawt.LWComponentPeer;
import sun.lwawt.macosx.CPlatformView;

public final class CGLGraphicsConfig extends CGraphicsConfig
    implements OGLGraphicsConfig
{
    //private static final int kOpenGLSwapInterval =
    // RuntimeOptions.getCurrentOptions().OpenGLSwapInterval;
    private static final int kOpenGLSwapInterval = 0; // TODO
    private static boolean cglAvailable;
    private static ImageCapabilities imageCaps = new CGLImageCaps();

    private int pixfmt;
    private BufferCapabilities bufferCaps;
    private long pConfigInfo;
    private ContextCapabilities oglCaps;
    private OGLContext context;
    private final Object disposerReferent = new Object();
    private static native boolean initCGL();
    private static native long getCGLConfigInfo(int displayID, int visualnum,
                                                int swapInterval);
    private static native int getOGLCapabilities(long configInfo);

    static {
        cglAvailable = initCGL();
    }

    private CGLGraphicsConfig(CGraphicsDevice device, int pixfmt,
                                long configInfo, ContextCapabilities oglCaps)
    {
        super(device);

        this.pixfmt = pixfmt;
        this.pConfigInfo = configInfo;
        this.oglCaps = oglCaps;
        context = new OGLContext(OGLRenderQueue.getInstance(), this);

        // add a record to the Disposer so that we destroy the native
        // CGLGraphicsConfigInfo data when this object goes away
        Disposer.addRecord(disposerReferent,
                           new CGLGCDisposerRecord(pConfigInfo));
    }

    @Override
    public Object getProxyKey() {
        return this;
    }

    @Override
    public SurfaceData createManagedSurface(int w, int h, int transparency) {
        return CGLSurfaceData.createData(this, w, h,
                                         getColorModel(transparency),
                                         null,
                                         OGLSurfaceData.TEXTURE);
    }

    public static CGLGraphicsConfig getConfig(CGraphicsDevice device,
                                              int pixfmt)
    {
        if (!cglAvailable) {
            return null;
        }

        long cfginfo = 0;
        final String ids[] = new String[1];
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            // getCGLConfigInfo() creates and destroys temporary
            // surfaces/contexts, so we should first invalidate the current
            // Java-level context and flush the queue...
            OGLContext.invalidateCurrentContext();

            cfginfo = getCGLConfigInfo(device.getCGDisplayID(), pixfmt,
                                       kOpenGLSwapInterval);
            if (cfginfo != 0L) {
                OGLContext.setScratchSurface(cfginfo);
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        ids[0] = OGLContext.getOGLIdString();
                    }
                });
            }
        } finally {
            rq.unlock();
        }
        if (cfginfo == 0) {
            return null;
        }

        int oglCaps = getOGLCapabilities(cfginfo);
        ContextCapabilities caps = new OGLContextCaps(oglCaps, ids[0]);

        return new CGLGraphicsConfig(device, pixfmt, cfginfo, caps);
    }

    public static boolean isCGLAvailable() {
        return cglAvailable;
    }

    /**
     * Returns true if the provided capability bit is present for this config.
     * See OGLContext.java for a list of supported capabilities.
     */
    @Override
    public boolean isCapPresent(int cap) {
        return ((oglCaps.getCaps() & cap) != 0);
    }

    @Override
    public long getNativeConfigInfo() {
        return pConfigInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.pipe.hw.BufferedContextProvider#getContext
     */
    @Override
    public OGLContext getContext() {
        return context;
    }

    @Override
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        WritableRaster
            raster = model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(),
                                 null);
    }

    @Override
    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
        case Transparency.OPAQUE:
            // REMIND: once the ColorModel spec is changed, this should be
            //         an opaque premultiplied DCM...
            return new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        case Transparency.BITMASK:
            return new DirectColorModel(25, 0xff0000, 0xff00, 0xff, 0x1000000);
        case Transparency.TRANSLUCENT:
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            return new DirectColorModel(cs, 32,
                                        0xff0000, 0xff00, 0xff, 0xff000000,
                                        true, DataBuffer.TYPE_INT);
        default:
            return null;
        }
    }

    public boolean isDoubleBuffered() {
        return isCapPresent(CAPS_DOUBLEBUFFERED);
    }

    private static class CGLGCDisposerRecord implements DisposerRecord {
        private long pCfgInfo;
        public CGLGCDisposerRecord(long pCfgInfo) {
            this.pCfgInfo = pCfgInfo;
        }
        public void dispose() {
            if (pCfgInfo != 0) {
                OGLRenderQueue.disposeGraphicsConfig(pCfgInfo);
                pCfgInfo = 0;
            }
        }
    }

    // TODO: CGraphicsConfig doesn't implement displayChanged() yet
    //@Override
    public synchronized void displayChanged() {
        //super.displayChanged();

        // the context could hold a reference to a CGLSurfaceData, which in
        // turn has a reference back to this CGLGraphicsConfig, so in order
        // for this instance to be disposed we need to break the connection
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            OGLContext.invalidateCurrentContext();
        } finally {
            rq.unlock();
        }

        updateTotalDisplayBounds();
    }

    @Override
    public String toString() {
        int displayID = getDevice().getCGDisplayID();
        return ("CGLGraphicsConfig[dev="+displayID+",pixfmt="+pixfmt+"]");
    }

    @Override
    public SurfaceData createSurfaceData(CPlatformView pView) {
        return CGLSurfaceData.createData(pView);
    }

    @Override
    public SurfaceData createSurfaceData(CGLLayer layer) {
        return CGLSurfaceData.createData(layer);
    }

    @Override
    public Image createAcceleratedImage(Component target,
                                        int width, int height)
    {
        ColorModel model = getColorModel(Transparency.OPAQUE);
        WritableRaster wr = model.createCompatibleWritableRaster(width, height);
        return new OffScreenImage(target, model, wr,
                                  model.isAlphaPremultiplied());
    }

    @Override
    public void assertOperationSupported(final int numBuffers,
                                         final BufferCapabilities caps)
            throws AWTException {
        // Assume this method is never called with numBuffers != 2, as 0 is
        // unsupported, and 1 corresponds to a SingleBufferStrategy which
        // doesn't depend on the peer. Screen is considered as a separate
        // "buffer".
        if (numBuffers != 2) {
            throw new AWTException("Only double buffering is supported");
        }
        final BufferCapabilities configCaps = getBufferCapabilities();
        if (!configCaps.isPageFlipping()) {
            throw new AWTException("Page flipping is not supported");
        }
        if (caps.getFlipContents() == BufferCapabilities.FlipContents.PRIOR) {
            throw new AWTException("FlipContents.PRIOR is not supported");
        }
    }

    @Override
    public Image createBackBuffer(final LWComponentPeer<?, ?> peer) {
        final Rectangle r = peer.getBounds();
        // It is possible for the component to have size 0x0, adjust it to
        // be at least 1x1 to avoid IAE
        final int w = Math.max(1, r.width);
        final int h = Math.max(1, r.height);
        final int transparency = peer.isTranslucent() ? Transparency.TRANSLUCENT
                                                      : Transparency.OPAQUE;
        return new SunVolatileImage(this, w, h, transparency, null);
    }

    @Override
    public void destroyBackBuffer(final Image backBuffer) {
        if (backBuffer != null) {
            backBuffer.flush();
        }
    }

    @Override
    public void flip(final LWComponentPeer<?, ?> peer, final Image backBuffer,
                     final int x1, final int y1, final int x2, final int y2,
                     final BufferCapabilities.FlipContents flipAction) {
        final Graphics g = peer.getGraphics();
        try {
            g.drawImage(backBuffer, x1, y1, x2, y2, x1, y1, x2, y2, null);
        } finally {
            g.dispose();
        }
        if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
            final Graphics2D bg = (Graphics2D) backBuffer.getGraphics();
            try {
                bg.setBackground(peer.getBackground());
                bg.clearRect(0, 0, backBuffer.getWidth(null),
                             backBuffer.getHeight(null));
            } finally {
                bg.dispose();
            }
        }
    }

    private static class CGLBufferCaps extends BufferCapabilities {
        public CGLBufferCaps(boolean dblBuf) {
            super(imageCaps, imageCaps,
                  dblBuf ? FlipContents.UNDEFINED : null);
        }
    }

    @Override
    public BufferCapabilities getBufferCapabilities() {
        if (bufferCaps == null) {
            bufferCaps = new CGLBufferCaps(isDoubleBuffered());
        }
        return bufferCaps;
    }

    private static class CGLImageCaps extends ImageCapabilities {
        private CGLImageCaps() {
            super(true);
        }
        public boolean isTrueVolatile() {
            return true;
        }
    }

    @Override
    public ImageCapabilities getImageCapabilities() {
        return imageCaps;
    }

    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height,
                                                       int transparency,
                                                       int type) {
        if (type == FLIP_BACKBUFFER || type == WINDOW || type == UNDEFINED ||
            transparency == Transparency.BITMASK)
        {
            return null;
        }

        if (type == FBOBJECT) {
            if (!isCapPresent(CAPS_EXT_FBOBJECT)) {
                return null;
            }
        } else if (type == PBUFFER) {
            boolean isOpaque = transparency == Transparency.OPAQUE;
            if (!isOpaque && !isCapPresent(CAPS_STORED_ALPHA)) {
                return null;
            }
        }

        SunVolatileImage vi = new AccelTypedVolatileImage(this, width, height,
                                                          transparency, type);
        Surface sd = vi.getDestSurface();
        if (!(sd instanceof AccelSurface) ||
            ((AccelSurface)sd).getType() != type)
        {
            vi.flush();
            vi = null;
        }

        return vi;
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.pipe.hw.AccelGraphicsConfig#getContextCapabilities
     */
    @Override
    public ContextCapabilities getContextCapabilities() {
        return oglCaps;
    }

    @Override
    public void addDeviceEventListener(AccelDeviceEventListener l) {
        int displayID = getDevice().getCGDisplayID();
        AccelDeviceEventNotifier.addListener(l, displayID);
    }

    @Override
    public void removeDeviceEventListener(AccelDeviceEventListener l) {
        AccelDeviceEventNotifier.removeListener(l);
    }

    private static final Rectangle totalDisplayBounds = new Rectangle();

    private static void updateTotalDisplayBounds() {
        synchronized (totalDisplayBounds) {
            Rectangle virtualBounds = new Rectangle();
            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                for (GraphicsConfiguration gc : gd.getConfigurations()) {
                    virtualBounds = virtualBounds.union(gc.getBounds());
                }
            }
            totalDisplayBounds.setBounds(virtualBounds);
        }
    }

    // 7160609: GL still fails to create a square texture of this size,
    //          so we use this value to cap the total display bounds.
    native private static int getMaxTextureSize();

    @Override
    public int getMaxTextureWidth() {
        //Temporary disable this logic and use some magic constrain.
        /*
         int width;

         synchronized (totalDisplayBounds) {
         if (totalDisplayBounds.width == 0) {
         updateTotalDisplayBounds();
         }
         width = totalDisplayBounds.width;
         }

         return Math.min(width, getMaxTextureSize());
         */
        return getMaxTextureSize() / (getDevice().getScaleFactor() * 2);
    }

    @Override
    public int getMaxTextureHeight() {
        //Temporary disable this logic and use some magic constrain.
        /*
         int height;

         synchronized (totalDisplayBounds) {
         if (totalDisplayBounds.height == 0) {
         updateTotalDisplayBounds();
         }
         height = totalDisplayBounds.height;
         }

         return Math.min(height, getMaxTextureSize());
         */
        return getMaxTextureSize() / (getDevice().getScaleFactor() * 2);
    }
}
