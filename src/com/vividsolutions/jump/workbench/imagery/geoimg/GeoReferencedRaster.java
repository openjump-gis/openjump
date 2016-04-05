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

import it.geosolutions.imageio.core.CoreCommonImageMetadata;
import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.graphic.WorldFile;

public class GeoReferencedRaster extends GeoRaster {
  private final String MSG_GENERAL = "This is not a valid GeoTIFF file.";
  String fileName;

  Envelope envModel_image;
  Envelope envModel_image_backup;

  Coordinate coorRasterTiff_tiepointLT;
  Coordinate coorModel_tiepointLT;

  private double dblModelUnitsPerRasterUnit_X;
  private double dblModelUnitsPerRasterUnit_Y;

  // boolean hoPatch = false;

  /**
   * Called by Java2XML
   * 
   * @throws ReferencedImageException
   */
  public GeoReferencedRaster(String location) throws ReferencedImageException {
    this(location, null);
  }

  public GeoReferencedRaster(String location, Object reader)
      throws ReferencedImageException {
    super(location, reader);
    fileName = imageFileLocation;
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
      re = new ReferencedImageException("problem acessing tiff image: "
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
      tags[0] = fieldModelGeoTransform.getAsDouble(0); // pixel size in x
                                                       // direction
      tags[1] = fieldModelGeoTransform.getAsDouble(1); // rotation about y-axis
      tags[2] = fieldModelGeoTransform.getAsDouble(4); // rotation about x-axis
      tags[3] = fieldModelGeoTransform.getAsDouble(5); // pixel size in the
                                                       // y-direction
      tags[4] = fieldModelGeoTransform.getAsDouble(3); // x-coordinate of the
                                                       // center of the upper
                                                       // left pixel
      tags[5] = fieldModelGeoTransform.getAsDouble(7); // y-coordinate of the
                                                       // center of the upper
                                                       // left pixel
                                                       // setCoorRasterTiff_tiepointLT(new
                                                       // Coordinate(-0.5,
                                                       // -0,5));
      // setCoorModel_tiepointLT(new Coordinate(0, 0));
      // setAffineTransformation(new AffineTransform(tags));
      Logger.debug("gtiff trans: " + Arrays.toString(tags));
      setEnvelope(tags);
    }
    // use the tiepoints as defined
    else {
      // Get the number of modeltiepoints
      // int numModelTiePoints = fieldModelTiePoints.getCount() / 6;
      // ToDo: alleen numModelTiePoints == 1 ondersteunen.
      // Map the modeltiepoints from raster to model space

      // Read the tiepoints
      setCoorRasterTiff_tiepointLT(new Coordinate(
          fieldModelTiePoints.getAsDouble(0),
          fieldModelTiePoints.getAsDouble(1), 0));
      setCoorModel_tiepointLT(new Coordinate(
          fieldModelTiePoints.getAsDouble(3),
          fieldModelTiePoints.getAsDouble(4), 0));
      setEnvelope();
      // Find the ModelPixelScale field
      XTIFFField fieldModelPixelScale = dir
          .getField(XTIFF.TIFFTAG_GEO_PIXEL_SCALE);
      if (fieldModelPixelScale == null) {
        // TODO: fieldModelTiePoints may contains GCP that could be exploited to
        // georeference the image
        throw new ReferencedImageException("Missing pixelscale-tag in file."
            + "\n" + MSG_GENERAL);
      }

      Logger.debug("gtiff tiepoints found.");

      setDblModelUnitsPerRasterUnit_X(fieldModelPixelScale.getAsDouble(0));
      setDblModelUnitsPerRasterUnit_Y(fieldModelPixelScale.getAsDouble(1));

      setEnvelope();

    }
  }

  private void parseGDALMetaData(URI uri) throws ReferencedImageException {

    // if (!GDALUtilities.isGDALAvailable())
    // throw new
    // ReferencedImageException("no gdal metadata available because gdal is not properly loaded.");

    // gdal geo info
    List<ImageReaderSpi> readers;
    Exception ex = null;
    Object input = null;
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
        } catch (IllegalArgumentException e) {
          Logger.debug("fail " + readerSpi + "/" + input + " -> " + e);
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
        tags[4] = geoTransform[0]; // x-coordinate of the center of the upper
                                   // left pixel
        tags[5] = geoTransform[3]; // y-coordinate of the center of the upper
                                   // left pixel
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
      is = WorldFile.find(fileName);
      // Read the tags from the tiff worldfile.
      List lines = FileUtil.getContents(is);
      double[] tags = new double[6];
      for (int i = 0; i < 6; i++) {
        String line = (String) lines.get(i);
        tags[i] = Double.parseDouble(line);
      }

      Logger.debug("wf: " + Arrays.toString(tags));

      setEnvelope(tags);

    } catch (IOException e) {
      throw e;
    } finally {
      FileUtil.close(is);
    }

  }

  protected void readRasterfile() throws ReferencedImageException {
    super.readRasterfile();

    URI uri;
    try {
      uri = new URI(imageFileLocation);
    } catch (URISyntaxException e) {
      throw new ReferencedImageException(e);
    }

    // Try to find and parse world file.
    try {
      parseWorldFile();
      // still with us? must have succeeded
      Logger.debug("Worldfile geo metadata fetched.");
      return;
    } catch (IOException e) {
      Logger.debug("Worldfile geo metadata unavailable: " + e.getMessage());
    }

    //if (false)
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
      // still with us? must have succeeded
      Logger.debug("XTIFF geo metadata fetched.");
      return;
    } catch (ReferencedImageException e) {
      Logger.debug("XTIFF geo metadata unavailable: " + e.getMessage());
    }

    double[] tags = new double[6];
    tags[0] = 1; // pixel size in x
                 // direction
    tags[1] = 0; // rotation about y-axis
    tags[2] = 0; // rotation about x-axis
    tags[3] = -1;// pixel size in the
                 // y-direction
    tags[4] = 0; // x-coordinate of the
                 // center of the upper
                 // left pixel
    tags[5] = 0; // y-coordinate of the
                 // center of the upper
                 // left pixel
    setEnvelope(tags);

    Logger.info("No georeference found! Will use default 0,0 placement.");
    JUMPWorkbench.getInstance().getFrame()
        .warnUser(this.getClass().getName() + ".no-geo-reference-found");

  }

  private void setEnvelope(double[] tags) {
    setCoorRasterTiff_tiepointLT(new Coordinate(-0.5, -0.5));
    setCoorModel_tiepointLT(new Coordinate(0, 0));
    AffineTransform transform = new AffineTransform(tags);

    double scaleX = Math.abs(transform.getScaleX());
    double scaleY = Math.abs(transform.getScaleY());

    setDblModelUnitsPerRasterUnit_X(scaleX);
    setDblModelUnitsPerRasterUnit_Y(scaleY);

    Point2D rasterLT = new Point2D.Double(src.getMinX(), src.getMinY());
    Point2D modelLT = new Point2D.Double();
    transform.transform(rasterLT, modelLT);

    setCoorRasterTiff_tiepointLT(new Coordinate(rasterLT.getX(),
        rasterLT.getY()));
    setCoorModel_tiepointLT(new Coordinate(modelLT.getX(), modelLT.getY()));

    setEnvelope();
  }

  void setEnvelope() {
    Coordinate coorRaster_imageLB = new Coordinate(coorRasterTiff_tiepointLT.x,
        src.getHeight(), 0);
    Coordinate coorRaster_imageRT = new Coordinate(src.getWidth(), 0, 0);
    Coordinate coorModel_imageLB = rasterToModelSpace(coorRaster_imageLB);
    Coordinate coorModel_imageRT = rasterToModelSpace(coorRaster_imageRT);

    envModel_image = new Envelope(coorModel_imageLB, coorModel_imageRT);

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

    coorModel.x = coorModel_tiepointLT.x
        + (coorRaster.x - coorRasterTiff_tiepointLT.x)
        * dblModelUnitsPerRasterUnit_X;
    coorModel.y = coorModel_tiepointLT.y
        - (coorRaster.y + coorRasterTiff_tiepointLT.y)
        * dblModelUnitsPerRasterUnit_Y;
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
   * @param coordinate
   */
  private void setCoorModel_tiepointLT(Coordinate coordinate) {
    coorModel_tiepointLT = coordinate;
    // setEnvelope();
  }

  /**
   * @param coordinate
   */
  private void setCoorRasterTiff_tiepointLT(Coordinate coordinate) {
    coorRasterTiff_tiepointLT = coordinate;
    // setEnvelope();
  }

  /**
   * @param d
   */
  private void setDblModelUnitsPerRasterUnit_X(double d) {
    dblModelUnitsPerRasterUnit_X = d;
    // setEnvelope();
  }

  /**
   * @param d
   */
  private void setDblModelUnitsPerRasterUnit_Y(double d) {
    dblModelUnitsPerRasterUnit_Y = d;
    // setEnvelope();
  }

  /**
   * @return coordinate of left-top corner in the model coordinate system
   */
  public Coordinate getCoorModel_tiepointLT() {
    return coorModel_tiepointLT;
  }

  /**
   * @return coordinate of left-top corner in the raster coordinate system
   */
  public Coordinate getCoorRasterTiff_tiepointLT() {
    return coorRasterTiff_tiepointLT;
  }

  /**
   * @return number of model units per raster unit along X axis
   */
  public double getDblModelUnitsPerRasterUnit_X() {
    return dblModelUnitsPerRasterUnit_X;
  }

  /**
   * @return number of model units per raster unit along Y axis
   */
  public double getDblModelUnitsPerRasterUnit_Y() {
    return dblModelUnitsPerRasterUnit_Y;
  }

  public Envelope getEnvelope(Feature f) throws ReferencedImageException {
    // geometry might be modified, if so let's rereference our image ;)
    Geometry g;
    if (f instanceof Feature && (g = f.getGeometry()) != null) {
      Geometry rasterEnv = (new GeometryFactory())
          .toGeometry(getOriginalEnvelope());
      if (!rasterEnv.equals(g)) {
        Envelope envGeom = g.getEnvelopeInternal();
        // set new scale values
        RenderedOp img = super.getImage();
        double xUnit = Math.abs(envGeom.getWidth() / img.getWidth());
        setDblModelUnitsPerRasterUnit_X(xUnit);
        double yUnit = Math.abs(envGeom.getHeight() / img.getHeight());
        setDblModelUnitsPerRasterUnit_Y(yUnit);
        // assign&return new envelope
        return envModel_image = new Envelope(envGeom);
      }
    }
    return getOriginalEnvelope();
  }

}