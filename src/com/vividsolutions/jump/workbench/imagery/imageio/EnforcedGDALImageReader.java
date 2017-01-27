package com.vividsolutions.jump.workbench.imagery.imageio;

import it.geosolutions.imageio.gdalframework.GDALImageReader;
import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;

import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

public class EnforcedGDALImageReader extends GDALImageReader {

  private static final Logger LOGGER = Logger
      .getLogger(EnforcedGDALImageReader.class.getCanonicalName());

  public EnforcedGDALImageReader(
      GDALImageReaderSpi availableGDALImageReaderSpi) {
    super(availableGDALImageReaderSpi, 0);
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine("AvailableGDALImageReader Constructor");
  }

  @Override
  public void setInput(Object input, boolean seekForwardOnly,
      boolean ignoreMetadata) {

    String skipDriver = "";
    for (int i = 0; i < gdal.GetDriverCount(); i++) {
      Driver d = gdal.GetDriver(i);
      String name = d.getShortName();
      if (!((GDALImageReaderSpi) getOriginatingProvider())
              .getSupportedFormats().contains(name))
        skipDriver += skipDriver.length() > 0 ? " " + name : name;
    }
    gdal.SetConfigOption("GDAL_SKIP", skipDriver.toString());
    gdal.AllRegister();

    super.setInput(input, seekForwardOnly, ignoreMetadata);
  }

}
