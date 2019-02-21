package com.vividsolutions.wms;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.cursortool.FeatureInfoTool;

/**
 * implements a {@link WMSRequest} specifically tailored for the
 * {@link FeatureInfoTool} to retrieve info about a {@link WMSLayer}
 */
public class FeatureInfoRequest extends AbstractWMSRequest {

  private WMSLayer wmsLayer;
  private Point2D point;
  private BoundingBox bbox;
  private int height, width;

  public FeatureInfoRequest(WMSLayer layer) throws IOException, KeyManagementException, NoSuchAlgorithmException {
    super(layer.getService());
    this.wmsLayer = layer;
  }

  public void setPoint(Point2D point) {
    this.point = point;
  }

  public void setBbox(Envelope bbox) {
    this.bbox = new BoundingBox(wmsLayer.getSRS(), bbox);
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  @Override
  public URL getURL() throws MalformedURLException, KeyManagementException, NoSuchAlgorithmException {
    String featInfoUrl = service.getCapabilities().getFeatureInfoURL();

    if (featInfoUrl.contains("?")) {
      if (!featInfoUrl.endsWith("?"))
        featInfoUrl += "&";
    } else {
      featInfoUrl += "?";
    }

    if (WMService.WMS_1_0_0.equals(version)) {
      featInfoUrl += "REQUEST=feature_info&WMTVER=1.0.0";
    } else if (WMService.WMS_1_1_0.equals(version)
        || WMService.WMS_1_1_1.equals(version)
        || WMService.WMS_1_3_0.equals(version)) {
      featInfoUrl += "REQUEST=GetFeatureInfo&SERVICE=WMS&VERSION=" + version;
    }

    String names = getWmsLayerNames(wmsLayer);
    featInfoUrl += "&QUERY_LAYERS=" + names + "&LAYERS=" + names;
    if (WMService.WMS_1_3_0.equals(version)) {
      featInfoUrl += "&CRS=" + wmsLayer.getSRS() + "&I=" + (int) point.getX()
          + "&J=" + (int) point.getY();
    } else {
      featInfoUrl += "&SRS=" + wmsLayer.getSRS() + "&X=" + (int) point.getX()
          + "&Y=" + (int) point.getY();
    }

    featInfoUrl += "&WIDTH=" + width + "&HEIGHT=" + height + "&STYLES="
        + "&FORMAT=" + wmsLayer.getFormat();

    // copied over from MapRequest
    if (bbox != null) {
      featInfoUrl += "&" + bbox.getBBox(version);
    }

    if (!WMService.WMS_1_0_0.equals(version)) {
      try {
        featInfoUrl += "&INFO_FORMAT="
            + wmsLayer.getService().getCapabilities().getInfoFormat();
      } catch (IOException e) {
        featInfoUrl += "&INFO_FORMAT=text/plain";
      }
    }

    featInfoUrl = featInfoUrl.concat("&FEATURE_COUNT=10 ");

    Logger.trace(featInfoUrl);
    return new URL(featInfoUrl);
  }

  private String getWmsLayerNames(WMSLayer selLayer) {
    int i;
    String names = "";
    List<String> layerNames = selLayer.getLayerNames();
    for (i = 0; i < layerNames.size(); ++i) {
      String name = (String) layerNames.get(i);
      try {
        name = URLEncoder.encode(name, "UTF-8");
      } catch (Exception ignored) {
      }
      names += name;
      if (i < layerNames.size() - 1) {
        names += ",";
      }
    }

    return names;
  }
}
