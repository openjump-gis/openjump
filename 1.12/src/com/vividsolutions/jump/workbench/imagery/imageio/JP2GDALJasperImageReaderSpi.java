package com.vividsolutions.jump.workbench.imagery.imageio;

import it.geosolutions.imageio.gdalframework.GDALImageReaderSpi;
import it.geosolutions.imageio.stream.input.FileImageInputStreamExt;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

import org.gdal.gdal.gdal;

/**
 * Service provider interface for the jp2k image
 * 
 */
public class JP2GDALJasperImageReaderSpi extends GDALImageReaderSpi {

  private static final Logger LOGGER = Logger
      .getLogger(JP2GDALJasperImageReaderSpi.class.getCanonicalName());

  private static final String[] formatNames = { "jpeg 2000", "JPEG 2000",
      "jpeg2000", "JPEG2000" };

  private static final String[] extensions = { "jp2", "j2k" }; // Should add jpx
                                                               // or jpm

  private static final String[] mimeTypes = { "image/jp2", "image/jpeg2000" };

  static final String version = "1.1";

  static final String description = "Jasper JP2K Image Reader, version " + version;

  static final String readerCN = JP2GDALJasperImageReader.class.getCanonicalName();

  static final String vendorName = "GeoSolutions";

  // writerSpiNames
  static final String[] wSN = { null };

  // StreamMetadataFormatNames and StreamMetadataFormatClassNames
  static final boolean supportsStandardStreamMetadataFormat = false;

  static final String nativeStreamMetadataFormatName = null;

  static final String nativeStreamMetadataFormatClassName = null;

  static final String[] extraStreamMetadataFormatNames = { null };

  static final String[] extraStreamMetadataFormatClassNames = { null };

  // ImageMetadataFormatNames and ImageMetadataFormatClassNames
  static final boolean supportsStandardImageMetadataFormat = false;

  static final String nativeImageMetadataFormatName = null;

  static final String nativeImageMetadataFormatClassName = null;

  static final String[] extraImageMetadataFormatNames = { null };

  static final String[] extraImageMetadataFormatClassNames = { null };

  // static {
  // if (GDALUtilities.isGDALAvailable())
  // IIORegistry.getDefaultInstance().registerServiceProvider(
  // new JP2GDALOpenJPEGImageReaderSpi());
  // }

  public JP2GDALJasperImageReaderSpi() {
    super(
        vendorName,
        version,
        formatNames,
        extensions,
        mimeTypes,
        readerCN, // readerClassName
        new Class[] { File.class, FileImageInputStreamExt.class },
        wSN, // writer Spi Names
        supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
        nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
        extraStreamMetadataFormatClassNames,
        supportsStandardImageMetadataFormat, nativeImageMetadataFormatName,
        nativeImageMetadataFormatClassName, extraImageMetadataFormatNames,
        extraImageMetadataFormatClassNames, 
        Collections.singletonList("JPEG2000"));

    if (LOGGER.isLoggable(Level.FINE))
      LOGGER.fine("JP2GDALJasperImageReaderSpi Constructor");
  }

  /**
   * This method checks if the provided input can be decoded from this SPI
   */
  public boolean canDecodeInput(Object input) throws IOException {
    return super.canDecodeInput(input);
  }

  /**
   * Returns an instance
   * 
   * @see javax.imageio.spi.ImageReaderSpi#createReaderInstance(java.lang.Object)
   */
  public ImageReader createReaderInstance(Object source) throws IOException {
    return new JP2GDALJasperImageReader(this);
  }

  /**
   * @see javax.imageio.spi.IIOServiceProvider#getDescription(java.util.Locale)
   */
  public String getDescription(Locale locale) {
    return description;
  }

}
