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
import java.util.List;

import org.geotiff.image.jai.GeoTIFFDescriptor;
import org.geotiff.image.jai.GeoTIFFDirectory;
import org.libtiff.jai.codec.XTIFF;
import org.libtiff.jai.codec.XTIFFField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.util.FileUtil;

public class GeoTIFFRaster extends GeoReferencedRaster
{
  private final String MSG_GENERAL = "This is not a valid GeoTIFF file.";
  String fileName;

  boolean hoPatch = false;

  /**
   * Called by Java2XML
   */
  public GeoTIFFRaster(String imageFileLocation)
      throws Exception
  {
    super(imageFileLocation);
    fileName = imageFileLocation;
    registerWithJAI();
    readRasterfile();
  }

  private void registerWithJAI()
  {
    // Register the GeoTIFF descriptor with JAI.
    GeoTIFFDescriptor.register();
  }

  private void parseGeoTIFFDirectory(GeoTIFFDirectory dir) throws Exception
  {
    // Find the ModelTiePoints field
    XTIFFField fieldModelTiePoints = dir.getField(XTIFF.TIFFTAG_GEO_TIEPOINTS);
    if (fieldModelTiePoints == null)
      throw new Exception("Missing tiepoints-tag in file.\n" + MSG_GENERAL);

    // Get the number of modeltiepoints
//    int numModelTiePoints = fieldModelTiePoints.getCount() / 6;

    // ToDo: alleen numModelTiePoints == 1 ondersteunen.

    // Map the modeltiepoints from raster to model space

    // Read the tiepoints
    setCoorRasterTiff_tiepointLT(new Coordinate(fieldModelTiePoints
        .getAsDouble(0), fieldModelTiePoints.getAsDouble(1), 0));
    setCoorModel_tiepointLT(new Coordinate(fieldModelTiePoints.getAsDouble(3),
        fieldModelTiePoints.getAsDouble(4), 0));
    setEnvelope();
    // Find the ModelPixelScale field
    XTIFFField fieldModelPixelScale = dir
        .getField(XTIFF.TIFFTAG_GEO_PIXEL_SCALE);
    if (fieldModelPixelScale == null)
      throw new Exception("Missing pixelscale-tag in file." + "\n"
          + MSG_GENERAL);

    setDblModelUnitsPerRasterUnit_X(fieldModelPixelScale.getAsDouble(0));
    setDblModelUnitsPerRasterUnit_Y(fieldModelPixelScale.getAsDouble(1));
  }

  /**
   * @return filename of the tiff worldfile
   */
  private String worldFileName()
  {
    int posDot = fileName.lastIndexOf('.');
    if (posDot == -1)
    {
      posDot = fileName.length();
    }
    return fileName.substring(0, posDot) + ".tfw";
  }

  private void parseWorldFile() throws Exception
  {
    // Get the name of the tiff worldfile.
    String name = worldFileName();

    // Read the tags from the tiff worldfile.
    List lines = FileUtil.getContents(name);
    double[] tags = new double[6];
    for (int i = 0; i < 6; i++)
    {
      String line = (String) lines.get(i);
      tags[i] = Double.parseDouble(line);
    }
    setCoorRasterTiff_tiepointLT(new Coordinate(0, 0) );
    setCoorModel_tiepointLT(new Coordinate(0, 0) );
    setAffineTransformation(new AffineTransform(tags));
  }

  protected void readRasterfile() throws Exception
  {
//    ImageCodec originalCodec = ImageCodec.getCodec("tiff");
      super.readRasterfile();

      // Get access to the tags and geokeys.
      // First, get the TIFF directory
      GeoTIFFDirectory dir = (GeoTIFFDirectory) src
          .getProperty("tiff.directory");
      if (dir == null)
        throw new Exception("This is not a (geo)tiff file.");

      try
      {
        // Try to parse any embedded geotiff tags.
        parseGeoTIFFDirectory(dir);
      } catch (Exception E)
      {
        // Embedded geotiff tags have not been found. Try
        // to use a geotiff world file.
        try
        {
          parseWorldFile();
        } catch (Exception e)
        {
          throw new Exception(
              "Neither geotiff tags nor valid worldfile found.\n" + MSG_GENERAL);
        }
      }
  }

}