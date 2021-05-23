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

import com.vividsolutions.jump.I18N;
import it.geosolutions.imageio.core.CoreCommonImageMetadata;
import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;
import it.geosolutions.imageio.gdalframework.GDALUtilities;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.media.jai.RenderedOp;

import org.libtiff.jai.codec.XTIFF;
import org.libtiff.jai.codec.XTIFFDirectory;
import org.libtiff.jai.codec.XTIFFField;

import com.sun.media.jai.codec.SeekableStream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.graphic.WorldFile;

/**
 * Most georeferencing takes place here.
 * Georeferencing may take place in GeoTiff tags, in GDAL or in a worlfile like
 * tfw or pgw or jgw.
 * In a geotiff, georeferencement may be defined as a transformation matrix or
 * from a tiepoint and scale factors (georeferencement from several tiepoints is
 * not supported).
 * Warning : if georeferencement is defined by a single tiepoint along with a
 * scalex and a scaley factor, the tiepoint may be on the upper-left corner of
 * the upper-left pixel, on the center of the pixel or at any place as long as
 * image coordinate and model coordinate are consistent (see here after for the
 * interpretation of image coordinate)
 *
 * By default, coordinate (0,0) of a geotiff image represents the upper left corner
 * of the upper left pixel. This is called GTRasterTypeGeoKey = RasterPixelIsArea.
 * In this case, upper left corner of the image envelope is the transformation of
 * the (0,0) image coordinate and lower right corner is the image of (w,h).
 *
 * On the other hand, if GTRasterTypeGeoKey = RasterPixelIsPoint, image coordinate
 * (0,0) represents the center of the upper left pixel.
 *
 * World file defines an affine transformation with tx and ty translation parameters
 * defined relatively to the center of the upper left pixel.
 *
 * javadoc added by mmichaud on 2021-05-23
 */
public class GeoReferencedRaster extends GeoRaster {
  private final String MSG_GENERAL = "This is not a valid GeoTIFF file.";
  Envelope envModel_image;
  Envelope envModel_image_backup;

  // PixelType=AREA (default) means that image coordinates
  // refer to the upper left corner of the upper left angle.
  // PixelType=POINT means that image coordinates refer to
  // the center of the upper left pixel
  public enum PixelType {AREA,POINT}
  PixelType pixelType = PixelType.AREA;

  //https://trac.osgeo.org/gdal/ticket/4977
  boolean honourNegativeScaleY = false;

  // Rename upper left image coordinate with more expressive names
  // Remarks in GeoTIFF, rasterULPixelCenter is
  // 0.5, 0.5 in  AREA_OR_POINT=Area (default)
  // 0,   0   in  AREA_OR_POINT=Point
  private Coordinate rasterULPixelCenter;
  private Coordinate modelULPixelCenter;

  private double scaleX;
  // [michaudm 2020-11-16] signed y-scale
  // to be able to handle north-south oriented or south-north oriented images
  private double scaleY;

  /**
   * Called by Java2XML
   */
  public GeoReferencedRaster(String location) throws ReferencedImageException {
    this(location, null);
  }

  public GeoReferencedRaster(String location, Object reader)
      throws ReferencedImageException {
    super(location, reader);
    readRasterfile();
  }

  private void parseGeoTIFFDirectory(URI uri) throws ReferencedImageException {
    XTIFFDirectory dir = null;
    InputStream input = null;
    ReferencedImageException re = null;
    try {
      input = createInputStream(uri);
      SeekableStream ss = SeekableStream.wrapInputStream(input, true);
      dir = XTIFFDirectory.create(ss, 0);
    } catch (IllegalArgumentException e) {
      re = new ReferencedImageException("probably no tiff image: "
          + e.getMessage());
    } catch (IOException e) {
      re = new ReferencedImageException("problem accessing tiff image: "
          + e.getMessage());
    } finally {
      // clean up
      disposeInput(input);
      if (re != null)
        throw re;
    }

    // Find the ModelTiePoints field
    XTIFFField fieldModelTiePoints = dir.getField(XTIFF.TIFFTAG_GEO_TIEPOINTS);
    if (fieldModelTiePoints == null) {
      // try to read geotransform (tranformation matrix) information,
      // if tiepoints are not used to georeference this image.
      // These parameters are the same as those in a tfw file.
      XTIFFField fieldModelGeoTransform = dir
          .getField(XTIFF.TIFFTAG_GEO_TRANS_MATRIX);
      if (fieldModelGeoTransform == null) {
        throw new ReferencedImageException(
            "Missing tiepoints-tag and tranformation matrix-tag parameters in file.\n"
                + MSG_GENERAL);
      }
      double[] tags = new double[6];
      // pixel size in x direction (x-scale)
      tags[0] = fieldModelGeoTransform.getAsDouble(0);
      // rotation about y-axis
      tags[1] = fieldModelGeoTransform.getAsDouble(1);
      // rotation about x-axis
      tags[2] = fieldModelGeoTransform.getAsDouble(4);
      // pixel size in the y-direction (y-scale)
      tags[3] = fieldModelGeoTransform.getAsDouble(5);
      // x-ordinate of the center of the upper left pixel
      tags[4] = fieldModelGeoTransform.getAsDouble(3);
      // y-ordinate of the center of the upper left pixel
      tags[5] = fieldModelGeoTransform.getAsDouble(7);
      Logger.debug("gtiff transform: " + Arrays.toString(tags));
      setEnvelope(tags);
    }
    // use the tiepoints as defined
    else if (fieldModelTiePoints.getType() == XTIFFField.TIFF_DOUBLE) {
      // Get the number of modeltiepoints
      // int numModelTiePoints = fieldModelTiePoints.getCount() / 6;
      // ToDo: alleen numModelTiePoints == 1 ondersteunen.
      // Map the modeltiepoints from raster to model space

      // Read the tiepoints
      // imageCoord has not the same meaning in AREA and in POINT mode,
      // but here, we don't mind, imageCoord may represent any point in
      // the image, important thing is that imageCoord and modelCoord
      // represent the same point in the image and in the model
      // coordinate system.
      Coordinate imageCoord = new Coordinate(
              fieldModelTiePoints.getAsDouble(0),
              fieldModelTiePoints.getAsDouble(1));
      Coordinate modelCoord = new Coordinate(
              fieldModelTiePoints.getAsDouble(3),
              fieldModelTiePoints.getAsDouble(4));

      Logger.debug("gtiff tiepoints found : " + Arrays.toString(fieldModelTiePoints.getAsDoubles()));

      // Find the ModelPixelScale field
      XTIFFField fieldModelPixelScale = dir
          .getField(XTIFF.TIFFTAG_GEO_PIXEL_SCALE);
      if (fieldModelPixelScale == null) {
        // TODO: fieldModelTiePoints may contain other GCP that could be exploited to
        //       georeference the image
        throw new ReferencedImageException("Missing pixelscale-tag in file."
            + "\n" + MSG_GENERAL);
      }

      scaleX = fieldModelPixelScale.getAsDouble(0);
      scaleY = fieldModelPixelScale.getAsDouble(1);
      //https://trac.osgeo.org/gdal/ticket/4977
      if (!honourNegativeScaleY) scaleY = - Math.abs(scaleY);
      Logger.debug("gtiff scale : scalex=" + scaleX + ", scaley=" + scaleY);

      // To compute the translation parameters of the transformation, we need
      // to know how a point is converted from image to model coordinate, we
      // don't mind where this point is.
      double tx = modelCoord.x - scaleX * imageCoord.x;
      double ty = modelCoord.y - scaleY * imageCoord.y;
      Logger.debug("translation " + tx + " - " + ty);

      // Now, we want to know the model coordinate of the point precisely
      // located at the center of the upper left pixel.
      // Coordinates of this point is not the same in AREA and in POINT modes
      rasterULPixelCenter = (pixelType == PixelType.AREA) ?
              new Coordinate(0.5,0.5) : new Coordinate(0.0,0.0);
      modelULPixelCenter = new Coordinate(
              getScaleX() * rasterULPixelCenter.x + tx,
              getScaleY() * rasterULPixelCenter.y + ty);
      Logger.debug("model UL pixel center " + modelULPixelCenter);

      setEnvelope();

    }
  }

  private void parseGDALMetaData(URI uri) throws ReferencedImageException {

    if (!areGDALClassesAvailable || !GDALUtilities.isGDALAvailable())
      throw new ReferencedImageException("no gdal metadata available because gdal is not properly loaded.");

    // gdal geo info
    List<ImageReaderSpi> readers;
    Exception ex = null;
    Object input;
    try {
      readers = listValidImageIOReaders(uri, GDALImageReaderSpi.class);
      for (ImageReaderSpi readerSpi : readers) {
        input = createInput(uri, readerSpi);
        ImageReader reader = readerSpi.createReaderInstance();
        IIOMetadata metadata = null;
        // try with file or stream
        try {
          reader.setInput(input);
          metadata = reader.getImageMetadata(0);
        } catch (RuntimeException e) {
          Logger.debug("fail " + readerSpi + "/" + input + " -> " + e);
        } finally {
          reader.dispose();
          disposeInput(input);
        }

        if (!(metadata instanceof CoreCommonImageMetadata)) {
          Logger.info("Unexpected error! Metadata should be an instance of the expected class: GDALCommonIIOImageMetadata.");
          continue;
        }

        double[] geoTransform = ((CoreCommonImageMetadata) metadata)
            .getGeoTransformation();

        Logger.debug("successfully retrieved gdal geo metadata: "
            + Arrays.toString(geoTransform));

        // check transform array for validity
        if (geoTransform == null || (geoTransform.length != 6))
          continue;

        // settle for the first result
        double[] tags = new double[6];
        tags[0] = geoTransform[1]; // pixel size in x direction
        tags[1] = geoTransform[4]; // rotation about y-axis
        tags[2] = geoTransform[2]; // rotation about x-axis
        tags[3] = geoTransform[5]; // pixel size in the y-direction
        tags[4] = geoTransform[0]; // x-coordinate of the center of the upper left pixel
        tags[5] = geoTransform[3]; // y-coordinate of the center of the upper left pixel
        setEnvelope(tags);

        // still with us? must have succeeded
        return;
      }
    } catch (IOException e1) {
      ex = e1;
    }

    throw new ReferencedImageException("no gdal metadata retrieved.", ex);
  }

  private void parseWorldFile() throws IOException {
    // Get the name of the tiff worldfile.
    // String name = worldFileName();
    InputStream is = null;
    try {
      is = WorldFile.find(getURI().toString());
      // Read the tags from the tiff worldfile.
      List<String> lines = FileUtil.getContents(is);
      double[] tags = new double[6];
      for (int i = 0; i < 6; i++) {
        String line = lines.get(i);
        tags[i] = Double.parseDouble(line);
      }

      Logger.debug("worldfile: " + Arrays.toString(tags));
      if (pixelType == PixelType.AREA) {
        // For PixelType.AREA, 0,0 image coordinate refers to the UL corner of the UL pixel
        // not its center as for worldfile
        tags[4] = tags[4] - tags[0]*0.5;
        tags[5] = tags[5] - tags[3]*0.5;
      }
      setEnvelope(tags);

    } catch (IOException e) {
      throw e;
    } finally {
      FileUtil.close(is);
    }

  }

  /**
   * initialize the img and try to parse geo infos via (in this order)
   * worldfile, gdal or geotiff
   */
  protected void readRasterfile() throws ReferencedImageException {
    super.readRasterfile();

    URI uri = getURI();

    // Try to find and parse world file.
    try {
      parseWorldFile();
      // still with us? must have succeeded
      Logger.debug("Worldfile geo metadata fetched.");
      return;
    } catch (IOException e) {
      Logger.debug("Worldfile geo metadata unavailable: " + e.getMessage());
    }

    try {
      // Get access to the tags and geokeys.
      // First, try to get the TIFF directory
      // Object dir = src.getProperty("tiff.directory");
      parseGDALMetaData(uri);
      // still with us? must have succeeded
      Logger.debug("GDAL geo metadata fetched.");
      return;
    } catch (ReferencedImageException e) {
      Logger.debug("GDAL geo metadata unavailable: " + e.getMessage());
    }

    try {
      // Get access to the tags and geokeys.
      // First, try to get the TIFF directory
      // Object dir = src.getProperty("tiff.directory");
      parseGeoTIFFDirectory(uri);
      if (envModel_image != null) {
        // still with us? must have succeeded
        Logger.debug("XTIFF geo metadata fetched.");
        return;
      }
    } catch (ReferencedImageException e) {
      Logger.debug("XTIFF geo metadata unavailable: " + e.getMessage());
    }

    Logger.info("No georeference found! Will use default 0,0 placement.");
    JUMPWorkbench.getInstance().getFrame()
        .warnUser(I18N.get(this.getClass().getName() + ".no-georeference-found"));

    // set up a default envelope
    double[] tags = new double[6];
    tags[0] = 1; // pixel size in x direction
    tags[1] = 0; // rotation about y-axis
    tags[2] = 0; // rotation about x-axis
    tags[3] = -1;// pixel size in the y-direction
    tags[4] = 0; // x-coordinate of the center of the upper left pixel
    tags[5] = 0; // y-coordinate of the center of the upper left pixel
    setEnvelope(tags);
  }

  private void setEnvelope(double[] tags) {

    AffineTransform transform = new AffineTransform(tags);

    //We should honour negative scale y, but gdal created plenty of
    //files where sign is not correct.
    //We now have to consider that the scale y sign is not significative
    //and offer an option to honour negative scales
    //https://trac.osgeo.org/gdal/ticket/4977
    //double scaleX = Math.abs(transform.getScaleX());
    //double scaleY = Math.abs(transform.getScaleY());

    scaleX = transform.getScaleX();
    scaleY = transform.getScaleY();

    // To compute the envelope in AreaOrPoint.AREA mode, we need to
    // know that upper left pixel center is at 0.5, 0.5, not 0, 0
    double offset = (pixelType == PixelType.AREA) ? 0.5 : 0.0;
    Point2D rasterLT = new Point2D.Double(src.getMinX()+offset, src.getMinY()+offset);
    Point2D modelLT = new Point2D.Double();
    transform.transform(rasterLT, modelLT);
    Logger.debug("setEnvelope " + rasterLT + " -> " + modelLT);

    rasterULPixelCenter = new Coordinate(rasterLT.getX(), rasterLT.getY());
    modelULPixelCenter = new Coordinate(modelLT.getX(), modelLT.getY());

    setEnvelope();
  }

  void setEnvelope() {

    // Image coordinate of the upper left corner of the envelope
    double ulx = rasterULPixelCenter.x-0.5;
    double uly = rasterULPixelCenter.y-0.5;

    // Bottom left coordinate of the envelope
    Coordinate imageEnvelopeBL = new Coordinate(ulx,uly+src.getHeight());

    // Top right coordinate of the envelope
    Coordinate imageEnvelopeTR = new Coordinate(ulx+src.getWidth(), uly);
    Logger.debug("Image envelope " + ulx + "," + uly + "," + (ulx+src.getWidth()) + "," + (uly+src.getHeight()));

    // Transform envelope corners to the model coordinate system
    Coordinate modelEnvelopeBL = rasterToModelSpace(imageEnvelopeBL);
    Coordinate modelEnvelopeTR = rasterToModelSpace(imageEnvelopeTR);
    Logger.debug("Model envelope " + modelEnvelopeBL.x + "," + modelEnvelopeTR.y + "," + modelEnvelopeTR.x + "," + modelEnvelopeBL.y);

    envModel_image = new Envelope(modelEnvelopeBL, modelEnvelopeTR);

    // backup original envelope
    envModel_image_backup = envModel_image;
  }

  /**
   * Convert a coordinate from rasterspace to modelspace.
   * 
   * @param coorRaster
   *          coordinate in rasterspace
   * @return coordinate in modelspace
   */
  private Coordinate rasterToModelSpace(Coordinate coorRaster) {
    Coordinate coorModel = new Coordinate();
    coorModel.x = modelULPixelCenter.x
        + (coorRaster.x - rasterULPixelCenter.x)
        * scaleX;
    coorModel.y = modelULPixelCenter.y
        + (coorRaster.y - rasterULPixelCenter.y)
        * scaleY;
    coorModel.z = 0;

    return coorModel;
  }

  public Envelope getEnvelope() {
    return envModel_image;
  }

  public Envelope getOriginalEnvelope() {
    return envModel_image_backup;
  }

  /**
   * @return coordinate of left-top corner in the model coordinate system
   */
  public Coordinate getModelULPixelCenter() {
    return modelULPixelCenter;
  }

  /**
   * @return coordinate of left-top corner in the raster coordinate system
   */
  @Deprecated
  public Coordinate getCoorRasterTiff_tiepointLT() {
    //return coorRasterTiff_tiepointLT;
    return rasterULPixelCenter;
  }

  /**
   * @return number of model units per raster unit along X axis
   */
  public double getScaleX() {
    return scaleX;
  }

  /**
   * @return number of model units per raster unit along Y axis
   */
  public double getScaleY() {
    return scaleY;
  }

  public Envelope getEnvelope(Feature f) throws ReferencedImageException {
    // geometry might be modified, if so let's rereference our image ;)
    Geometry g;
    if (f != null && (g = f.getGeometry()) != null) {
      Geometry rasterEnv = (new GeometryFactory())
          .toGeometry(getOriginalEnvelope());
      if (!rasterEnv.equals(g)) {
        Envelope envGeom = g.getEnvelopeInternal();
        // set new scale values
        RenderedOp img = super.getRenderedOp();
        double xUnit = Math.abs(envGeom.getWidth() / img.getWidth());
        scaleX = xUnit;
        double yUnit = Math.abs(envGeom.getHeight() / img.getHeight());
        // y-scale is generally negative (won't work if model y axis is top-down)
        scaleY = -yUnit;
        // assign&return new envelope
        return envModel_image = new Envelope(envGeom);
      }
    }
    return getOriginalEnvelope();
  }

}