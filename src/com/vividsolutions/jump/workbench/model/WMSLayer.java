/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.model;

import java.awt.Image;
import java.awt.MediaTracker;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.LayerNameRenderer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.wms.BoundingBox;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.MapRequest;
import com.vividsolutions.wms.WMService;

/**
 * A Layerable that retrieves images from a Web Map Server.
 */
public class WMSLayer extends AbstractLayerable implements Cloneable {

  private String format;

  private List<String> layerNames = new ArrayList<>();

  private String srs;

  private int alpha = 255;

  private WMService service;

  private String wmsVersion = WMService.WMS_1_1_1;

  private Reference oldImage;
  private URL oldURL;

  /**
   * Called by Java2XML
   */
  public WMSLayer() {
    init();
  }

  public WMSLayer(LayerManager layerManager, String serverURL, String srs,
      List<String> layerNames, String format, String version) throws IOException {
    this(layerManager, initializedService(serverURL, version), srs, layerNames,
        format);
  }

  private static WMService initializedService(String serverURL, String version)
      throws IOException {
    WMService initializedService = new WMService(serverURL, version);
    initializedService.initialize();
    return initializedService;
  }

  public WMSLayer(LayerManager layerManager, WMService initializedService,
      String srs, List<String> layerNames, String format) throws IOException {
    this(layerManager, initializedService, srs, layerNames, format,
        initializedService.getVersion());
  }

  public WMSLayer(String title, LayerManager layerManager,
      WMService initializedService, String srs, List<String> layerNames,
      String format) throws IOException {
    this(title, layerManager, initializedService, srs, layerNames, format,
        initializedService.getVersion());
  }

  public WMSLayer(String title, LayerManager layerManager,
      WMService initializedService, String srs, List<String> layerNames,
      String format, String version) {
    super(title, layerManager);
    setService(initializedService);
    setSRS(srs);
    setLayerNames(layerNames);
    setFormat(format);
    setWmsVersion(version);
    init();
  }

  public WMSLayer(LayerManager layerManager, WMService initializedService,
      String srs, List<String> layerNames, String format, String version) {
    this(layerNames.get(0), layerManager, initializedService, srs, layerNames,
        format, version);
  }

  protected void init() {
    getBlackboard().put(RenderingManager.USE_MULTI_RENDERING_THREAD_QUEUE_KEY,
        true);
    getBlackboard().put(LayerNameRenderer.USE_CLOCK_ANIMATION_KEY, true);
  }

  public void setService(WMService service) {
    this.service = service;
    this.serverURL = service.getServerUrl();
  }

  public int getAlpha() {
    return alpha;
  }

  /**
   * @param alpha
   *          0-255 (255 is opaque)
   */
  public void setAlpha(int alpha) {
    this.alpha = alpha;
  }

  public Image createImage(LayerViewPanel panel) throws IOException {

    MapRequest request = createRequest(panel);
    URL newURL = request.getURL();

    Image image;

    // look if last request equals new one.
    // if it does, take the image from the cache.
    if (oldURL == null || !newURL.equals(oldURL) || oldImage == null
        || (image = (Image) oldImage.get()) == null) {
      image = request.getImage();
      MediaTracker mt = new MediaTracker(new JButton());
      mt.addImage(image, 0);

      try {
        mt.waitForID(0);
      } catch (InterruptedException e) {
        Assert.shouldNeverReachHere();
      }
      oldImage = new SoftReference<>(image);
      oldURL = newURL;
    }

    return image;
  }

  private BoundingBox toBoundingBox(String srs, Envelope e) {
    return new BoundingBox(srs, e);
  }

  public MapRequest createRequest(LayerViewPanel panel) throws IOException {
    MapRequest request = getService().createMapRequest();
    request.setBoundingBox(toBoundingBox(srs, panel.getViewport()
        .getEnvelopeInModelCoordinates()));
    request.setFormat(format);
    request.setImageWidth(panel.getWidth());
    request.setImageHeight(panel.getHeight());
    request.setLayerNames(layerNames);
    request.setTransparent(true);

    return request;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void addLayerName(String layerName) {
    layerNames.add(layerName);
  }

  public void setLayerNames(List<String> list) {
    layerNames = list;
  }

  public List<String> getLayerNames() {
    return Collections.unmodifiableList(layerNames);
  }

  public void setSRS(String srs) {
    this.srs = srs;
  }

  public String getSRS() {
    return srs;
  }

  public Object clone() throws java.lang.CloneNotSupportedException {
    WMSLayer clone = (WMSLayer) super.clone();
    clone.layerNames = new ArrayList<>(this.layerNames);

    return clone;
  }

  public void removeAllLayerNames() {
    layerNames.clear();
  }

  private Blackboard blackboard = new Blackboard();

  private String serverURL;

  public Blackboard getBlackboard() {
    return blackboard;
  }

  public WMService getService() throws IOException {
    if (service == null) {
      Assert.isTrue(serverURL != null);
      setService(initializedService(serverURL, wmsVersion));
    }
    return service;
  }

  public String getServerURL() {
    // Called by Java2XML [Jon Aquino 2004-02-23]
    return serverURL;
  }

  public void setServerURL(String serverURL) {
    // Called by Java2XML [Jon Aquino 2004-02-23]
    this.serverURL = serverURL;
  }

  public String getWmsVersion() {
    return wmsVersion;
  }

  public void setWmsVersion(String wmsVersion) {
    this.wmsVersion = wmsVersion;
  }

  public Envelope getEnvelope() {

    Envelope envelope = new Envelope();

    List<String> list = getLayerNames();
    try {
      for (String name : list) {
        MapLayer lyr = getService().getCapabilities().getMapLayerByName(name);
        BoundingBox bb = lyr.getBoundingBox(getSRS());
        if (bb != null
            && bb.getEnvelope().getMinX() < bb.getEnvelope().getMaxX()) {
          envelope.expandToInclude(bb.getEnvelope());
        }
      }
    } catch (IOException e) {
      Logger
          .error(
              "WMSLayer envelope calculation failed."
                  + (Logger.isDebugEnabled() ? "" : " - "
                      + e.getLocalizedMessage()), Logger.isDebugEnabled() ? e
                  : null);
    }
    return envelope;
  }

  public void dispose() {
    // TODO: probably a good idea to remove resources when the layer is closed
    // up
    // dunno what is needed to clean up WMS though, hence leave it for now
  }

}
