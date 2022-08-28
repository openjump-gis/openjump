package com.vividsolutions.jump.workbench.imagery.graphic;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffContents;
import org.apache.commons.imaging.formats.tiff.TiffDirectory;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImagingParameters;
import org.apache.commons.imaging.formats.tiff.TiffRasterData;
import org.apache.commons.imaging.formats.tiff.TiffRasterStatistics;
import org.apache.commons.imaging.formats.tiff.TiffReader;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntry;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntryForRange;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PaletteEntryForValue;
import org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint.PhotometricInterpreterFloat;
import org.openjump.util.UriUtil;

import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.io.CompressedFile;
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
      ByteSource byteSource = new ByteSourceInputStream(CompressedFile.openFile(uri), UriUtil.getFileName(uri));
      TiffReader tiffReader = new TiffReader(true);
      TiffContents contents = tiffReader.readDirectories(byteSource, true, FormatCompliance.getDefault());
      TiffDirectory directory = contents.directories.get(0);

      if (!directory.hasTiffFloatingPointRasterData()) {
        super.initImage();
        return;
      }

//      PhotometricInterpreterFloat pi = new PhotometricInterpreterFloat(0.0f, 1.0f);
//      HashMap<String, Object> params = new HashMap<>();
//      params.put(TiffConstants.PARAM_KEY_CUSTOM_PHOTOMETRIC_INTERPRETER, pi);
//      directory.getTiffImage(params);
//
//      float maxValue = pi.getMaxFound();
//      float minValue = pi.getMinFound();

      final TiffImagingParameters params = new TiffImagingParameters();
      final TiffRasterData rasterData = directory.getRasterData(params);

      float excludedValue = Float.NaN;
      TiffRasterStatistics simpleStats;
      if (false) { // we can add a custom "No Data" indicator
        simpleStats = rasterData.getSimpleStatistics(9999);
      } else {
        // just gather the standard statistics
        simpleStats = rasterData.getSimpleStatistics();
      }

      float minValue = simpleStats.getMinValue();
      float maxValue = simpleStats.getMaxValue();

//      PhotometricInterpreterFloat grayScale = new PhotometricInterpreterFloat(minValue, maxValue);
//      params = new HashMap<>();
//      params.put(TiffConstants.PARAM_KEY_CUSTOM_PHOTOMETRIC_INTERPRETER, grayScale);
//      BufferedImage bImage = directory.getTiffImage(params);

      TiffField piField = directory.findField(TiffTagConstants.TIFF_TAG_PHOTOMETRIC_INTERPRETATION);
      // default is black to white ascending
      boolean ascendingGrayTones = (piField == null
          || piField.getIntValue() != TiffTagConstants.PHOTOMETRIC_INTERPRETATION_VALUE_WHITE_IS_ZERO);

      List<PaletteEntry> paletteList = new ArrayList();
      if (!Float.isNaN(excludedValue)) {
        // draw the excluded value in red.
        paletteList.add(new PaletteEntryForValue(excludedValue, Color.red));
      }
      paletteList.add(new PaletteEntryForRange(minValue, maxValue, ascendingGrayTones ? Color.black : Color.white,
          ascendingGrayTones ? Color.white : Color.black));
//      paletteList.add(new PaletteEntryForValue(maxValue, Color.white));
      PhotometricInterpreterFloat photometricInterpreter = new PhotometricInterpreterFloat(paletteList);

      params.setCustomPhotometricInterpreter(photometricInterpreter);
      BufferedImage bImage = directory.getTiffImage(params);

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
