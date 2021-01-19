package com.vividsolutions.jump.workbench.imagery.geotiff;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 * @deprecated replaced by geoimg code, more efficient
 * TODO to be removed in version 2
 */
@Deprecated
public class RasterPainter
{
  GeoReferencedRaster geoRaster;
  Envelope envModel_viewportCached;
  double scaleCached;
  RenderedOp imgScaled;
  RenderedOp imgWindow;
  //RenderedOp imgRescaled;

  // Rescaling parameters
  final static double DEFAULT_RESCALINGCONSTANT = 1;
  final static double DEFAULT_RESCALINGOFFSET = 0;
  double rescalingConstant = DEFAULT_RESCALINGCONSTANT;
  double rescalingOffset = DEFAULT_RESCALINGOFFSET;

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
   * @param image the image to rescale
   * @param constant multiplication factor
   * @param offset offset
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
    // imgScaled is scaled to fit the viewport scale
    final double scale = viewport.getScale();
    if (scale != scaleCached)
    {
      scaleImage(scale);
      scaleCached = scale;
    }

    Envelope vpEnv = envModel_viewport;
    Envelope imEnv = geoRaster.getEnvelope();
    Envelope intersection = imEnv.intersection(vpEnv);
    double cropX = Math.max(vpEnv.getMinX() - imEnv.getMinX(), 0.0)/scale;
    double cropY = Math.max(imEnv.getMaxY() - vpEnv.getMaxY(), 0.0)/scale;
    double offsetX = Math.max(imEnv.getMinX() - vpEnv.getMinX(), 0.0)/scale;
    double offsetY = Math.max(vpEnv.getMinY() - imEnv.getMinY(), 0.0)/scale;

    // Compute the ratio of the image to crop (located on the left of the viewport)
    double ratio_cropX = (envModel_viewport.getMinX() - geoRaster.getEnvelope()
        .getMinX())
        / geoRaster.getEnvelope().getWidth();
    // Compute the ratio of the image to crop (located on the north of the viewport)
    double ratio_cropY = (geoRaster.getEnvelope().getMaxY() - envModel_viewport
        .getMaxY())
        / geoRaster.getEnvelope().getHeight();
    // Compute the ratio between the complete image and the viewport
    double ratio_cropW = envModel_viewport.getWidth()
        / geoRaster.getEnvelope().getWidth();
    double ratio_cropH = envModel_viewport.getHeight()
        / geoRaster.getEnvelope().getHeight();

    // Compute crop parameters, applying ratio on the scaled image
    float raster_cropX = (float) (ratio_cropX * imgScaled.getWidth());
    float raster_cropY = (float) (ratio_cropY * imgScaled.getHeight());
    float raster_cropW = (float) (ratio_cropW * imgScaled.getWidth());
    float raster_cropH = (float) (ratio_cropH * imgScaled.getHeight());

    // Compute the offset to be applied on the scaled image
    // to shift the image of 1/2 pixel in the upper left direction
    double pixelSizeX = geoRaster.getDblModelUnitsPerRasterUnit_X();
    double pixelSizeY = geoRaster.getDblModelUnitsPerRasterUnit_Y();
    float raster_offsetX = (float)(-0.5*pixelSizeX*scale);
    float raster_offsetY = (float)(-0.5*pixelSizeY*scale);

    // left border of the image is on the right of the left border of the viewport
    // image is not cropped, it is shifted to the right
    if (raster_cropX < 0)
    {
      raster_offsetX = raster_offsetX-raster_cropX;
      raster_cropX = 0;
    }
    // upper border of the image is on the bottom of the upper border of the viewport
    // image is not cropped, it is shifted to the south
    if (raster_cropY < 0)
    {
      raster_offsetY = raster_offsetY-raster_cropY;
      raster_cropY = 0;
    }
    // width and height of the scaled image to be displayed
    raster_cropW = Math.min(raster_cropW, imgScaled.getWidth() - raster_cropX);
    raster_cropH = Math.min(raster_cropH, imgScaled.getHeight() - raster_cropY);

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
    // already. Just draw it with an identity transformation.
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