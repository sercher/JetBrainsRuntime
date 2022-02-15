package sun.java2d.wl;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import sun.awt.wl.WLComponentPeer;
import sun.awt.wl.WLFramePeer;
import sun.awt.wl.WLGraphicsConfig;
import sun.java2d.SurfaceData;
import sun.java2d.loops.SurfaceType;

public class WLSurfaceData extends SurfaceData {

  private final WLComponentPeer peer;
  private final WLGraphicsConfig graphicsConfig;
  private final int depth;

  public native void initSurface(WLComponentPeer peer, int rgb, int width, int height);
  protected native void initOps(WLComponentPeer peer, WLGraphicsConfig gc, int depth);

  protected WLSurfaceData(WLComponentPeer peer,
                          WLGraphicsConfig gc,
                          SurfaceType sType,
                          ColorModel cm)
  {
    super(sType, cm);
    this.peer = peer;
    this.graphicsConfig = gc;
    this.depth = cm.getPixelSize();
    initOps(peer, graphicsConfig, depth);
  }

  /**
   * Method for instantiating a Window SurfaceData
   */
  public static WLSurfaceData createData(WLComponentPeer peer) {
    WLGraphicsConfig gc = getGC(peer);
    return new WLSurfaceData(peer, gc, gc.getSurfaceType(), peer.getColorModel());
  }


  public static WLGraphicsConfig getGC(WLComponentPeer peer) {
    if (peer != null) {
      return (WLGraphicsConfig) peer.getGraphicsConfiguration();
    } else {
      GraphicsEnvironment env =
          GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gd = env.getDefaultScreenDevice();
      return (WLGraphicsConfig)gd.getDefaultConfiguration();
    }
  }
  @Override
  public SurfaceData getReplacement() {
    return null;
  }

  @Override
  public GraphicsConfiguration getDeviceConfiguration() {
    return null;
  }

  @Override
  public Raster getRaster(int x, int y, int w, int h) {
    return null;
  }

  @Override
  public Rectangle getBounds() {
    Rectangle r = peer.getTarget().getBounds();
    r.x = r.y = 0;
    return r;
  }

  @Override
  public Object getDestination() {
    return peer.getTarget();
  }

}
