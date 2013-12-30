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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.lang.reflect.InvocationTargetException;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.model.Disposable;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.AlphaSetting;

public class GeoImage implements ReferencedImage, Disposable, AlphaSetting {
  private GeoReferencedRaster gtr;
  private int alpha = 255;
  private float last_scale;
  private RenderedOp last_scale_img;
  private Envelope last_img_env;
  private RenderedImage last_rendering;
  private AffineTransform last_transform;
  private Envelope last_vwp_env;
  private BufferedImage full_scale_img;
  private double full_scale;

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
      throw new ReferencedImageException(ex);
    }
  }

  /**
   * actually paint the image to the viewport
   */
  public synchronized void paint(Feature f, java.awt.Graphics2D g, Viewport viewport)
      throws ReferencedImageException {

    try {
      // update image envelope, either use geometry's or image's
      // this allows moving the image via geometry movement
      // we can as well scale in both directions
      // TODO: how can we rotate the image thus achieving full manual
      // georeferencing?
      Envelope envImage = gtr.getEnvelope(f);

      RenderedOp src_img = gtr.getImage();

      ParameterBlock pb;
      // null (for small images) or hints containing a bigger tilecache for this image
      RenderingHints hints = gtr.createCacheRenderingHints();
      // System.out.println("GI img: " + img.getClass().getName() + "@"
      // + Integer.toHexString(img.hashCode()));

      // get current scale
      final float scale = (float) viewport.getScale();
      // get current viewport area
      Envelope envModel_viewport = viewport.getEnvelopeInModelCoordinates();

      // if nothing changed, no reason to rerender the whole shebang
      // this is mainly the case when OJ last and regained focus
      if (last_scale == scale && last_img_env instanceof Envelope
          && last_img_env.equals(envImage) && last_vwp_env instanceof Envelope
          && last_vwp_env.equals(envModel_viewport)
          && last_rendering instanceof RenderedImage
          && last_transform instanceof AffineTransform) {
        draw(g, null);
        return;
      }

//      System.out.println("GI: NO CACHE");
      
      // _the_ renderedop
      RenderedOp img;
      try {
        // reuse a cached version if scale and img_envelope didn't changed
        // speeds up panning, window resizing
        if (last_scale == scale && last_scale_img != null
            && last_img_env instanceof Envelope
            && last_img_env.equals(envImage)) {
          img = last_scale_img;
//          System.out.println("GI: USE SCALE CACHE");
        } else {
//          System.out.println("GI: NO SCALE CACHE");

          // First, scale the original image
          float scaleX = scale * (float) gtr.getDblModelUnitsPerRasterUnit_X();
          float scaleY = scale * (float) gtr.getDblModelUnitsPerRasterUnit_Y();

          // calculate predicted dimensions
          double scaledW = scaleX * src_img.getWidth();
          double scaledH = scaleY * src_img.getHeight();
          // img original dimensions
          double imgW = src_img.getWidth();
          double imgH = src_img.getHeight();
          
          // we don't scale anything resulting below 2x2 pixels
          // simply too small and throws render errors anyway
          if (scaledW < 2 || scaledH < 2)
            return;

          // we cache an overview here for big pictures 
          // speeds up situations when the whole picture is shown
          float scaleX_toUse, scaleY_toUse;
          RenderedImage scale_src_img;
          if ((imgW > 2000 || imgH > 2000) && scaledW < 2000 && scaledH < 2000 ) {
//            System.out.println("GI: USE FULL SCALE CACHE");
            // create a 2000x2000 inmemory version to use for full overviews
            // this is faster than having JAI create it from scratch from big datasets
            if (full_scale_img == null) {
              if (imgW > imgH) {
                full_scale = 1 / (imgW / 2000d);
              } else {
                full_scale = 1 / (imgH / 2000d);
              }
              // subsample average gives a smoothly resized image
              pb = new ParameterBlock();
              pb.addSource(src_img);
              pb.add(full_scale); // x scale factor
              pb.add(full_scale); // y scale factor
              full_scale_img = JAI.create("subsampleaverage", pb, null).getAsBufferedImage();
//              System.out.println("GI full scale img: "
//                  + full_scale_img.getWidth());
            }
            scaleX_toUse = (float) scaleX / (float) full_scale;
            scaleY_toUse = (float) scaleY / (float) full_scale;
            scale_src_img = full_scale_img;
          }
          // scale the original 
          else{
            scaleX_toUse = (float) scaleX;
            scaleY_toUse = (float) scaleY;
            scale_src_img = src_img;
          }

          pb = new ParameterBlock();
          pb.addSource(scale_src_img);
          // subsampling does not work if images are stretched
          // so use slow and qualitative inferior bicubic instead
          // or NOT, to f**g slow, use default interpolation
          if (scaleX > 0.1 || scaleY > 0.1) {
            pb.add(scaleX_toUse);
            pb.add(scaleY_toUse);
            pb.add(0f);
            pb.add(0f);
            // Interpolation interp = Interpolation
            // .getInstance(Interpolation.INTERP_BICUBIC);
            // pb.add(interp); // add interpolation method
            img = JAI.create("scale", pb, hints);
          } else {
            pb.add((double) (scaleX_toUse));
            pb.add((double) (scaleY_toUse));
            img = JAI.create("subsampleaverage", pb, hints);
          }

          // fill scale reuse cache
          last_scale = scale;
          last_scale_img = img;
//          System.out.println("GI scaleimg: " + scaleX+"/"+scaleY+"->"+img.getWidth() + "/"
//              + img.getHeight());
        }
        // cache the latest envelopes
        last_img_env = envImage;
        last_vwp_env = envModel_viewport;

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
        // we ignore a specific error here when we scale, 
        // probably simply too small
        return;
      }

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

      // cache the current rendering here as used in the
      // Nothing-Has-Changed cache above
      last_rendering = img.getAsBufferedImage();
      Rectangle b = img.getBounds();
      last_transform = AffineTransform.getTranslateInstance(b.getX(), b.getY());
      
      // eventually draw the image, let g render the chain
      draw(g, img);

    } catch (Exception ex) {
      throw new ReferencedImageException(ex);
    }
  }

  private void draw(Graphics2D g, RenderedImage img) {
    Composite composite = g.getComposite();
    // setup transparency
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
        1f * alpha / 255));
    // The image has been translated and scaled by JAI
    // already. Just draw it with an identity transformation.
    AffineTransform aft;
    if (img instanceof RenderedImage){
    	aft = new AffineTransform();
    }
    // no img given? paint cached last rendering again
    else{
      img = last_rendering;
      aft = last_transform;
    }
    g.drawRenderedImage(img, aft);
    g.setComposite(composite);
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