package com.vividsolutions.jump.workbench.imagery.imageio;

import it.geosolutions.imageio.gdalframework.GDALImageReader;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JP2GDALOpenJPEGImageReader extends EnforcedGDALImageReader {

  private static final Logger LOGGER = Logger
      .getLogger(JP2GDALOpenJPEGImageReader.class.getCanonicalName());

  public JP2GDALOpenJPEGImageReader(
      JP2GDALOpenJPEGImageReaderSpi originatingProvider) {
    super(originatingProvider);
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine("JP2GDALOpenJPEGImageReader Constructor");
  }

}
