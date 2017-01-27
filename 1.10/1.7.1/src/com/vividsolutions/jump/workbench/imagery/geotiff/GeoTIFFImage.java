package com.vividsolutions.jump.workbench.imagery.geotiff;

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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * legacy GeoTIFF reader
 */
public class GeoTIFFImage implements ReferencedImage {
  private GeoTIFFRaster gtr;
  private RasterPainter rasterPainter;

  public GeoTIFFImage(String location) throws JUMPException {
    init(location);
  }

  public Envelope getEnvelope() {
    return gtr.getEnvelope();
  }

  private void init(String location) throws JUMPException {
    try {
      gtr = new GeoTIFFRaster(location);
      rasterPainter = new RasterPainter(gtr);
    } catch (Exception ex) {
      gtr = null;
      throw new JUMPException(ex.getMessage());
    }
  }

  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport)
      throws ReferencedImageException {
    try {
      rasterPainter.paint(g, viewport);
    } catch (Exception ex) {
      throw new ReferencedImageException(ex);
    }
  }

  public String getType() {
    return "GeoTiff";
  }

  public String getLoader() {
    return "null";
  }

}