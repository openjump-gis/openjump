package com.vividsolutions.jump.workbench.imagery.graphic;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.common.ImageBuilder;
import org.apache.commons.imaging.formats.tiff.TiffContents;
import org.apache.commons.imaging.formats.tiff.TiffDirectory;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImagingParameters;
import org.apache.commons.imaging.formats.tiff.TiffRasterData;
import org.apache.commons.imaging.formats.tiff.TiffRasterStatistics;
import org.apache.commons.imaging.formats.tiff.TiffReader;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldTypeAscii;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldTypeByte;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntry;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntryForRange;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntryForValue;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PhotometricInterpreterFloat;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.openjump.core.rasterimage.TiffTags;
import org.openjump.util.UriUtil;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;

public class CommonsTIFFImage extends CommonsImage {

  public CommonsTIFFImage(String location, WorldFile wf) {
    super(location, wf);
  }

  @Override
  protected void initImage() throws ReferencedImageException {
    BufferedImage image = getImage();
    if (image != null)
      return;

    String uri = getUri();

    try {
      ByteSource byteSource = ByteSource.inputStream(CompressedFile.openFile(uri), UriUtil.getFileName(uri));
      TiffReader tiffReader = new TiffReader(true);
      TiffContents contents = tiffReader.readDirectories(byteSource, true, FormatCompliance.getDefault());
      TiffDirectory directory = contents.directories.get(0);

      if (!directory.hasTiffRasterData()) {
        super.initImage();
        return;
      }

      final TiffImagingParameters params = new TiffImagingParameters();
      final TiffRasterData rasterData = directory.getRasterData(params);

      final List<TiffField> fieldList = directory.getDirectoryEntries();
      
      // fetch nodata value
      Float noData = Float.NaN;
      for (final TiffField tiffField : fieldList) {
        Logger.info("TiffField: " + tiffField.toString());
        // read nodata value
        if (tiffField
            .getTag() == org.apache.commons.imaging.formats.tiff.constants.GdalLibraryTagConstants.EXIF_TAG_GDAL_NO_DATA.tag) {
          try {
            String noDataString = "";
            if (tiffField.getFieldType() instanceof FieldTypeAscii) {
              noDataString = tiffField.getStringValue();
              if (noDataString.equalsIgnoreCase("NaN")) {
                noDataString = "NaN";
              }
            } else if (tiffField.getFieldType() instanceof FieldTypeByte) {
              noDataString = new String(tiffField.getByteArrayValue());
            }
            noData = Float.parseFloat(noDataString);
          } catch (NumberFormatException e) {
            Logger.error(e);
          }
        }
      }

      TiffRasterStatistics simpleStats;
      simpleStats = rasterData.getSimpleStatistics(noData);

      final int w = rasterData.getWidth();
      final int h = rasterData.getHeight();
      float minValue = simpleStats.getMinValue();
      float maxValue = simpleStats.getMaxValue();

      // fetch ramp direction black->white or white->black
      TiffField piField = directory.findField(TiffTagConstants.TIFF_TAG_PHOTOMETRIC_INTERPRETATION);
      // default is black to white ascending
      boolean ascendingGrayTones = (piField == null
          || piField.getIntValue() != TiffTagConstants.PHOTOMETRIC_INTERPRETATION_VALUE_WHITE_IS_ZERO);

      List<PaletteEntry> paletteList = new ArrayList();
      // nodata will be transparent
      paletteList.add(new PaletteEntryForValue(noData, new Color(1f,0f,0f,0f)));
      if (minValue == maxValue)
        paletteList.add(new PaletteEntryForValue(minValue, Color.gray));
      else
        paletteList.add(new PaletteEntryForRange(minValue, maxValue, ascendingGrayTones ? Color.black : Color.white,
            ascendingGrayTones ? Color.white : Color.black));

      PhotometricInterpreterFloat photometricInterpreter = new PhotometricInterpreterFloat(paletteList);

      // this works, but does NOT respect the transparent noData pixel
      //params.setCustomPhotometricInterpreter(photometricInterpreter);
      // BufferedImage bImage = directory.getTiffImage(params);

      // Now construct an ImageBuilder to store the results
      final ImageBuilder builder = new ImageBuilder(w, h, true);
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          final float f = rasterData.getValue(x, y);
          final int argb = photometricInterpreter.mapValueToArgb(f);
          builder.setRgb(x, y, argb);
        }
      }

      final BufferedImage bImage = builder.getBufferedImage();
      setImage(bImage);

    } catch (Exception e) {
      throw new ReferencedImageException(e);
    }
  }

  @Override
  public Envelope getEnvelope() throws ReferencedImageException {
    // TODO implement or reuse TiffDir/Tag georeferencing
    return super.getEnvelope();
  }

}
