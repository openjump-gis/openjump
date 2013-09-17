package com.vividsolutions.jump.workbench.imagery.geoimg;

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
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;
import java.lang.reflect.InvocationTargetException;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.model.Disposable;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.AlphaSetting;

public class GeoImage implements ReferencedImage, Disposable, AlphaSetting {
  private GeoReferencedRaster gtr;
  int alpha = 255;

  public GeoImage(String location, Object reader) throws JUMPException {
    init(location, reader);
  }

  public Envelope getEnvelope() {
    return gtr.getEnvelope();
  }

  private void init(String location, Object reader)
      throws ReferencedImageException {
    try {
      gtr = new GeoReferencedRaster(location, reader);
    } catch (Exception ex) {
      dispose();
      String causemsg = ex.getCause() != null ? "\n"
          + ex.getCause().getMessage() : "";
      throw new ReferencedImageException(ex.getMessage() + causemsg, ex);
    }
  }

  public void paint(Feature f, java.awt.Graphics2D g, Viewport viewport)
      throws ReferencedImageException {
    try {
      // update image envelope, either use geometry's or image's
      // this allows moving the image via geometry movement
      // we can as well scale in both directions
      // TODO: how can we rotate the image thus achieving full manual
      // georeferencing?
      Geometry geom = f.getGeometry();
      Envelope envImage = gtr.getEnvelope(f);

      RenderedOp img = gtr.getImage();
      ParameterBlock pb;

      // First, scale the original image
      final float scale = (float) viewport.getScale();
      float scaleX = scale * (float) gtr.getDblModelUnitsPerRasterUnit_X();
      float scaleY = scale * (float) gtr.getDblModelUnitsPerRasterUnit_Y();

      pb = new ParameterBlock();
      pb.addSource(img);
      pb.add((float) scaleX);
      pb.add((float) scaleY);
      pb.add(0f);
      pb.add(0f);
      // System.out.println("SCALE: " + scaleX + "/" + scaleY);
      img = JAI.create("scale", pb, null);
      try {
        // System.out.println("SCALEdim: "+img.getHeight()+"/"+img.getWidth());
        // execute the JAI descriptor to enforce the exception below AND
        // simply do not draw too small images
        if (img.getWidth() < 1 || img.getHeight() < 1)
          return;
      } catch (Exception e) {
        Throwable cause;
        if (!e.getClass().getName().endsWith("ImagingException"))
          throw e;
        if (!((cause = e.getCause()) instanceof Throwable && cause instanceof InvocationTargetException))
          throw e;
        if (!((cause = cause.getCause()) instanceof Throwable && cause instanceof IllegalArgumentException))
          throw e;
        // we ignore a specific error here when we scale, probably simply too small
        return;
      }

      Envelope envModel_viewport = viewport.getEnvelopeInModelCoordinates();

      // Next, crop the part which is needed out of the scaled image.
      double ratio_cropX = (envModel_viewport.getMinX() - envImage.getMinX())
          / envImage.getWidth();
      double ratio_cropY = (envImage.getMaxY() - envModel_viewport.getMaxY())
          / envImage.getHeight();
      double ratio_cropW = envModel_viewport.getWidth() / envImage.getWidth();
      double ratio_cropH = envModel_viewport.getHeight() / envImage.getHeight();

      float raster_cropX = (int) (ratio_cropX * img.getWidth());
      float raster_cropY = (int) (ratio_cropY * img.getHeight());
      float raster_cropW = (int) (ratio_cropW * img.getWidth());
      float raster_cropH = (int) (ratio_cropH * img.getHeight());

      float raster_offsetX = 0;
      float raster_offsetY = 0;

      if (raster_cropX < 0) {
        raster_offsetX = -raster_cropX;
        raster_cropX = 0;
      }
      if (raster_cropY < 0) {
        raster_offsetY = -raster_cropY;
        raster_cropY = 0;
      }
      raster_cropW = Math
          .min(raster_cropW, img.getWidth() - (int) raster_cropX);
      raster_cropH = Math.min(raster_cropH, img.getHeight()
          - (int) raster_cropY);

      pb = new ParameterBlock();
      pb.addSource(img);
      pb.add(raster_cropX);
      pb.add(raster_cropY);
      pb.add(raster_cropW);
      pb.add(raster_cropH);
      img = JAI.create("crop", pb, null);

      // move the image to the model coordinates
      pb = new ParameterBlock();
      pb.addSource(img);
      pb.add(raster_offsetX - img.getMinX());
      pb.add(raster_offsetY - img.getMinY());
      img = JAI.create("translate", pb, null);

      // // Make your transparent mask
      // pb = new ParameterBlock();
      // pb.add(new Float(gtr.getImage().getWidth()));
      // pb.add(new Float(gtr.getImage().getHeight()));
      // pb.add(new Byte[] { (byte) alpha }); // Sample value. 0 = transparent,
      // 255 = opaque
      //
      // RenderedOp mask = JAI.create("constant", pb);
      //
      // // Combine with your existing image
      // pb = new ParameterBlock();
      // pb.addSource(img);
      // pb.addSource(mask);
      // img = JAI.create("bandmerge", pb);

      Composite composite = g.getComposite();
      // g.setComposite(AlphaComposite.SrcOver);
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
          1f * alpha / 255));
      // The image has been translated and scaled by JAI
      // already. Just draw it with an identity transformation.
      g.drawRenderedImage(img, new AffineTransform());
      g.setComposite(composite);
    } catch (Exception ex) {
      throw new ReferencedImageException(ex);
    }
  }

  public String getType() {
    return gtr != null ? gtr.getType() : "";
  }

  public String getLoader() {
    return gtr != null ? gtr.getLoader() : "";
  }

  public void dispose() {
    if (gtr != null)
      gtr.dispose();
  }

  public int getAlpha() {
    return alpha;
  }

  public void setAlpha(int alpha) {
    this.alpha = alpha;
  }

}