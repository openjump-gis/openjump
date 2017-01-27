package com.vividsolutions.jump.workbench.imagery.geotiff;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.ui.Viewport;

public class RasterPainter
{
  GeoReferencedRaster geoRaster;
  Envelope envModel_viewportCached;
  double scaleCached;
  RenderedOp imgScaled;
  RenderedOp imgWindow;
  RenderedOp imgRescaled;

  // Rescaling parameters
  final static double DEFAULT_RESCALINGCONSTANT = 1;
  final static double DEFAULT_RESCALINGOFFSET = 0;
  double rescalingConstant = DEFAULT_RESCALINGCONSTANT;
  double rescalingOffset = DEFAULT_RESCALINGOFFSET;

//  private final int VIEWPORT_MINX = 0;
//  private final int VIEWPORT_MINY = 0;

  boolean enabled = true;

  public RasterPainter(GeoReferencedRaster geoRaster)
  {
    this.geoRaster = geoRaster;
  }

  public void setRescalingConstant(double x)
  {
    rescalingConstant = x;
  }

  public void setRescalingOffset(double x)
  {
    rescalingOffset = x;
  }

  /**
   * @param image
   * @param constant
   * @param offset
   * @return
   */
  private RenderedOp rescale(RenderedOp image, double constant, double offset)
  {
    // If constant and offset equal to default values, don't do
    // anything, saves time (hopefully).
    if (constant == DEFAULT_RESCALINGCONSTANT
        && offset == DEFAULT_RESCALINGOFFSET)
      return image;
    else
    {
      int bands = imgWindow.getNumBands();
      double[] constants = new double[bands];
      double[] offsets = new double[bands];
      for (int i = 0; i < bands; i++)
      {
        constants[i] = constant;
        offsets[i] = offset;
      }
      ParameterBlock pb = new ParameterBlock();
      pb.addSource(image);
      pb.add(constants);
      pb.add(offsets);
      return JAI.create("rescale", pb, null);
    }
  }

  /**
   * @param image
   * @return
   */
  private RenderedOp rescale(RenderedOp image)
  {
    return rescale(image, rescalingConstant, rescalingOffset);
  }

  /**
   * @param viewport
   * @return @throws
   *         Exception
   */
  private RenderedOp getWindow(Viewport viewport) throws Exception
  {
    // Wat is de gewenste uitsnede in model-coordinaten ?
    Envelope envModel_viewport = viewport.getEnvelopeInModelCoordinates();

    if (envModel_viewportCached == null
        || !envModel_viewportCached.equals(envModel_viewport))
    {
      // Most images perform better with calculateWindow2.
      // Geomedia/USSampleimage.tif perform better with
      // calculateWindow however. calculateWindow2 shows
      // better when panning.
      imgWindow = calculateWindow2(viewport, envModel_viewport);
    }

    return imgWindow;
  }

  private void scaleImage(double scale) throws Exception
  {
    double scaleX = scale * geoRaster.getDblModelUnitsPerRasterUnit_X();
    double scaleY = scale * geoRaster.getDblModelUnitsPerRasterUnit_Y();

    ParameterBlock pb = new ParameterBlock();
    pb.addSource(geoRaster.getImage());
    pb.add((float) scaleX);
    pb.add((float) scaleY);
    pb.add(0f);
    pb.add(0f);
    imgScaled = JAI.create("scale", pb, null);

//    (new TestFrame(imgScaled)).show();
  }

  private RenderedOp calculateWindow2(Viewport viewport,
      Envelope envModel_viewport) throws Exception
  {
    // First, scale the original image if necessary.
    final double scale = viewport.getScale();
    if (scale != scaleCached)
    {
      scaleImage(scale);
      scaleCached = scale;
    }

    // Next, crop the part which is needed out of the
    // scaled image.
    double ratio_cropX = (envModel_viewport.getMinX() - geoRaster.getEnvelope()
        .getMinX())
        / geoRaster.getEnvelope().getWidth();
    double ratio_cropY = (geoRaster.getEnvelope().getMaxY() - envModel_viewport
        .getMaxY())
        / geoRaster.getEnvelope().getHeight();
    double ratio_cropW = envModel_viewport.getWidth()
        / geoRaster.getEnvelope().getWidth();
    double ratio_cropH = envModel_viewport.getHeight()
        / geoRaster.getEnvelope().getHeight();

    float raster_cropX = (int) (ratio_cropX * imgScaled.getWidth());
    float raster_cropY = (int) (ratio_cropY * imgScaled.getHeight());
    float raster_cropW = (int) (ratio_cropW * imgScaled.getWidth());
    float raster_cropH = (int) (ratio_cropH * imgScaled.getHeight());

    float raster_offsetX = 0;
    float raster_offsetY = 0;

    if (raster_cropX < 0)
    {
      raster_offsetX = -raster_cropX;
      raster_cropX = 0;
    }
    if (raster_cropY < 0)
    {
      raster_offsetY = -raster_cropY;
      raster_cropY = 0;
    }
    raster_cropW = Math.min(raster_cropW, imgScaled.getWidth()
        - (int) raster_cropX);
    raster_cropH = Math.min(raster_cropH, imgScaled.getHeight()
        - (int) raster_cropY);

    ParameterBlock pb = new ParameterBlock();
    pb.addSource(imgScaled);
    pb.add(raster_cropX);
    pb.add(raster_cropY);
    pb.add(raster_cropW);
    pb.add(raster_cropH);
    imgWindow = JAI.create("crop", pb, null);

//    imgWindow = imgScaled;

    pb = new ParameterBlock();
    pb.addSource(imgWindow);
    pb.add(raster_offsetX - imgWindow.getMinX());
    pb.add(raster_offsetY - imgWindow.getMinY());
    imgWindow = JAI.create("translate", pb, null);

    return imgWindow;
  }

  /**
   *
   */
  public void paint(Graphics2D g, Viewport viewport)
      throws Exception
  {
    // Get the image for the current viewport.
    RenderedOp imgWindowed = getWindow(viewport);
    if (imgWindowed == null)
      return;

    // Adjust brightness and contrast of this image.
    RenderedOp imgRescaled = rescale(imgWindowed);
    if (imgRescaled == null)
      return;

    g.setComposite(AlphaComposite.SrcOver);
    // The image has been translated and scaled by JAI
    // allready. Just draw it with an identity transformation.
    g.drawRenderedImage(imgRescaled, new AffineTransform());
  }

  /**
   *
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * @return Returns the rescalingConstant.
   */
  public double getRescalingConstant()
  {
    return rescalingConstant;
  }

  /**
   * @return Returns the rescalingOffset.
   */
  public double getRescalingOffset()
  {
    return rescalingOffset;
  }

}