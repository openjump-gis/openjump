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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public abstract class GeoReferencedRaster
{
  protected String imageFileLocation;
  protected RenderedOp src = null;
  Envelope envModel_image;

  // Image enhancement
  double[] min;
  double[] max;

  Coordinate coorRasterTiff_tiepointLT;
  Coordinate coorModel_tiepointLT;

  private double dblModelUnitsPerRasterUnit_X;
  private double dblModelUnitsPerRasterUnit_Y;

  public GeoReferencedRaster(String imageFileLocation)
      throws Exception
  {
    this.imageFileLocation = imageFileLocation;
  }

  /**
   * Basic fetchRasters retrieves a raster from a file. To get a raster from
   * somewhere else, override this method in subclasses.
   */
  protected void fetchRaster() throws Exception
  {
    src = JAI.create("fileload", imageFileLocation);
  }

  protected void readRasterfile() throws Exception
  {
    // ===========================
    // Load the image, any format.
    // ===========================
    fetchRaster();

    // ======================================
    // Image can be distorted, make it square
    // in modelspace.
    // ======================================
    normalize(src);
  }

  /**
   * Convert a coordinate from rasterspace to modelspace.
   *
   * @param coorRaster
   *          coordinate in rasterspace
   * @return coordinate in modelspace
   */
  private Coordinate rasterToModelSpace(Coordinate coorRaster)
  {
    Coordinate coorModel = new Coordinate();

    coorModel.x = coorModel_tiepointLT.x
        + (coorRaster.x - coorRasterTiff_tiepointLT.x)
        * dblModelUnitsPerRasterUnit_X;
    coorModel.y = coorModel_tiepointLT.y
        - (coorRaster.y + coorRasterTiff_tiepointLT.y)
        * dblModelUnitsPerRasterUnit_Y;
    coorModel.z = 0;

    return coorModel;
  }

  /**
   * This method must be overridden if an image is not a square image in
   * modelspace. It should be transformed to make it a square image in
   * modelspace.
   *
   * @param image
   */
  protected void normalize(RenderedOp image)
  {
  }

  /**
   * @return
   */
  public Coordinate getCoorModel_tiepointLT()
  {
    return coorModel_tiepointLT;
  }

  /**
   * @return
   */
  public Coordinate getCoorRasterTiff_tiepointLT()
  {
    return coorRasterTiff_tiepointLT;
  }

  /**
   * @return
   */
  public double getDblModelUnitsPerRasterUnit_X()
  {
    return dblModelUnitsPerRasterUnit_X;
  }

  /**
   * @return
   */
  public double getDblModelUnitsPerRasterUnit_Y()
  {
    return dblModelUnitsPerRasterUnit_Y;
  }

  public RenderedOp getImage() throws Exception
  {
    if (src == null)
      readRasterfile();
    return src;
  }

  void setEnvelope()
  {
//    try
//    {
      Coordinate coorRaster_imageLB = new Coordinate(
          coorRasterTiff_tiepointLT.x, src.getHeight() - 1, 0);
      Coordinate coorRaster_imageRT = new Coordinate(src.getWidth() - 1, 0, 0);
      Coordinate coorModel_imageLB = rasterToModelSpace(coorRaster_imageLB);
      Coordinate coorModel_imageRT = rasterToModelSpace(coorRaster_imageRT);

      envModel_image = new Envelope(coorModel_imageLB, coorModel_imageRT);
  }

  /**
   * @param coordinate
   */
  public void setCoorModel_tiepointLT(Coordinate coordinate)
  {
    coorModel_tiepointLT = coordinate;
    setEnvelope();
  }

  /**
   * @param coordinate
   */
  public void setCoorRasterTiff_tiepointLT(Coordinate coordinate)
  {
    coorRasterTiff_tiepointLT = coordinate;
    //setEnvelope();
  }

  /**
   * @param d
   */
  public void setDblModelUnitsPerRasterUnit_X(double d)
  {
    dblModelUnitsPerRasterUnit_X = d;
    //setEnvelope();
  }

  /**
   * @param d
   */
  public void setDblModelUnitsPerRasterUnit_Y(double d)
  {
    dblModelUnitsPerRasterUnit_Y = d;
    setEnvelope();
  }

  public void setAffineTransformation(AffineTransform transform)
  {
    double scaleX = Math.abs(transform.getScaleX());
    double scaleY = Math.abs(transform.getScaleY());

    setDblModelUnitsPerRasterUnit_X(scaleX);
    setDblModelUnitsPerRasterUnit_Y(scaleY);

    Point2D rasterLT = new Point2D.Double(src.getMinX(), src.getMinY());
    Point2D modelLT = new Point2D.Double();
    transform.transform(rasterLT, modelLT);

    setCoorRasterTiff_tiepointLT(new Coordinate(rasterLT.getX(), rasterLT
        .getY()));
    setCoorModel_tiepointLT(new Coordinate(modelLT.getX(), modelLT.getY()));
  }

  public RenderedOp fullContrast()
  {
    int bands = src.getNumBands();
    double[] constants = new double[bands];
    double[] offsets = new double[bands];
    for (int i = 0; i < bands; i++)
    {
      constants[i] = 1.2 * 255 / (max[i] - min[i]);
      offsets[i] = 255 * min[i] / (min[i] - max[i]);
    }

    ParameterBlock pb = new ParameterBlock();
    pb.addSource(src);
    pb.add(constants);
    pb.add(offsets);
    return JAI.create("rescale", pb, null);
  }

  public Envelope getEnvelope()
  {
    return envModel_image;
  }

  public double[] getMinimumExtreme()
  {
    return min;
  }

  public double[] getMaximumExtreme()
  {
    return max;
  }
}