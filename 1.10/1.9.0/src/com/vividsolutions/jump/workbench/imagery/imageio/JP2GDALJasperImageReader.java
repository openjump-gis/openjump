package com.vividsolutions.jump.workbench.imagery.imageio;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JP2GDALJasperImageReader extends EnforcedGDALImageReader {

  private static final Logger LOGGER = Logger
      .getLogger(JP2GDALJasperImageReader.class.getCanonicalName());

  public JP2GDALJasperImageReader(
      JP2GDALJasperImageReaderSpi jp2gdalJasperImageReaderSpi) {
    super(jp2gdalJasperImageReaderSpi);
    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine("JP2GDALJasperImageReader Constructor");
  }

}
