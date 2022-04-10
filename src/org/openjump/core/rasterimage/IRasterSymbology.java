package org.openjump.core.rasterimage;

import java.awt.*;

/**
 * Apply symbology to a Raster.
 */
public interface IRasterSymbology {

  /**
   * Get global image transparency (maybe combined with a per pixel transparency)
   */
  double getTransparency();

  /**
   * Set global image transparency (maybe combined with a per pixel transparency)
   */
  void setTransparency(double transparency);

  /**
   * Get minimum nodata value : values in the interval [minNoDataValue;maxNoDataValue]
   * are interpreted as nodata values and maybe represented as transparent or with
   * a specific color.
   * @return the minimum value of the nodata values interval
   */
  double getMinNoDataValue();

  /**
   * Set minimum nodata value : values in the interval [minNoDataValue;maxNoDataValue]
   * are interpreted as nodata values and maybe represented as transparent or with
   * a specific color.
   */
  void setMinNoDataValue(double minNoDataValue);

  /**
   * Get maximum nodata value : values in the interval [minNoDataValue;maxNoDataValue]
   * are interpreted as nodata values and maybe represented as transparent or with
   * a specific color.
   * @return the maximum value of the nodata values interval
   */
  double getMaxNoDataValue();

  /**
   * Set minimum nodata value : values in the interval [minNoDataValue;maxNoDataValue]
   * are interpreted as nodata values and maybe represented as transparent or with
   * a specific color.
   */
  void setMaxNoDataValue(double maxNoDataValue);

  /**
   * The color to be used for no data values.
   * For multiband images, the result Color of pixel having only one band
   * with nodata is nit defined.
   * @return the Color to be used fo nodata values.
   */
  Color getNoDataColor();

  /**
   * Set the Color to be used for nodata pixels.
   * @param color the Color associated to nodata pixel
   */
  void setNoDataColor(Color color);

  /**
   * @return a String representing the type Symbology.
   */
  String getType();

  /**
   * Set a String representing this specific symbology.
   * @param type
   */
  void setType(String type);

  /**
   * Return symbolized color for a single pixel image data.
   * @param data pixel data as int array
   * @return output Color
   */
  Color getPixelColor(int[] data);
  /**
   * Return symbolized color for a single pixel image data
   * @param data pixel data as int array
   * @return output Color
   */
  Color getPixelColor(double[] data);

  /**
   * Method based on getPixelColor which is implementation specific,
   * and applying global transparency and nodata values processing
   * which is common to all implementations.
   * @param data pixel data to be processed
   * @return the Color to display
   */
  default Color getFinalColor(int[] data) {
    Color color = getPixelColor(data);
    double t = getTransparency();
    if (data.length == 1 && isNoData(data[0])) return getNoDataColor();
    return t == 0 ? color :
        new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha()*(1.0-t)));
  }

  /**
   * Method based on getPixelColor which is implementation specific,
   * and applying global transparency and nodata values processing
   * which is common to all implementations.
   * @param data pixel data to be processed
   * @return the Color to display
   */
  default Color getFinalColor(double[] data) {
    Color color = getPixelColor(data);
    double t = getTransparency();
    if (data.length == 1 && isNoData(data[0])) return getNoDataColor();
    return t == 0 ? color :
        new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha()*(1.0-t)));
  }

  default boolean isNoData(double value) {
    return Double.isNaN(value) ||
        (value == Double.POSITIVE_INFINITY && getMaxNoDataValue() == Double.POSITIVE_INFINITY) ||
        (value == Double.NEGATIVE_INFINITY && getMinNoDataValue() == Double.NEGATIVE_INFINITY) ||
        ((float)value >= (float)getMinNoDataValue() && (float)value <= (float)getMaxNoDataValue());
  }
}
