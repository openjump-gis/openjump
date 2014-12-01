package com.vividsolutions.jump.workbench.imagery.imageio;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JP2GDALEcwImageReader extends EnforcedGDALImageReader {

  private static final Logger LOGGER = Logger
      .getLogger(JP2GDALEcwImageReader.class.getCanonicalName());

  public JP2GDALEcwImageReader(
      JP2GDALEcwImageReaderSpi originatingProvider) {
    super(originatingProvider);
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine("JP2GDALOpenJPEGImageReader Constructor");
  }

}
