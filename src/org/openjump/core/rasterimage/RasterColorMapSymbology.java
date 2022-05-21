package org.openjump.core.rasterimage;

import org.openjump.core.rasterimage.styler.ColorMapEntry;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class RasterColorMapSymbology extends RasterSymbology {

  private TreeMap<Double, Color> colorTreeMap;
  private double transparency = 0;

  public RasterColorMapSymbology() {
  }

  public RasterColorMapSymbology (String colorMapType) {
    super();
    super.setType(colorMapType);
    colorTreeMap = new TreeMap<>();
  }

  public void addColorMapEntry(double upperValue, Color color) {
    colorTreeMap.put(upperValue, color);
  }

  private ColorMapEntry getColorMapEntry(double cellValue) {
    if(colorTreeMap.floorEntry(cellValue) != null) {
      return new ColorMapEntry(
          colorTreeMap.floorEntry(cellValue).getKey(),
          colorTreeMap.floorEntry(cellValue).getValue());
    }
    return null;
  }

  private ColorMapEntry getNextColorMapEntry(double cellValue) {
    if(colorTreeMap.higherEntry(cellValue) != null) {
      return new ColorMapEntry(
          colorTreeMap.higherEntry(cellValue).getKey(),
          colorTreeMap.higherEntry(cellValue).getValue());
    }
    return null;
  }

  public TreeMap<Double, Color> getColorTreeMap() {
    return colorTreeMap;
  }

  public void setColorTreeMap(TreeMap<Double, Color> colorTreeMap) {
    this.colorTreeMap = colorTreeMap;
  }

  public ColorMapEntry[] getColorMapEntries() {

    ColorMapEntry[] colorMapEntries = new ColorMapEntry[getColorTreeMap().size()];
    int pos = 0;
    for(Map.Entry<Double,Color> colorMapEntry : getColorTreeMap().entrySet()) {
      colorMapEntries[pos] = new ColorMapEntry(colorMapEntry.getKey(), colorMapEntry.getValue());
      pos++;
    }
    return colorMapEntries;

  }

  public double getTransparency() {
    return transparency;
  }

  public void setTransparency(double transparency) {
    this.transparency = transparency;
  }

  @Override public Color getPixelColor(double[] data) {
    return getColor(clampValue(data[0]));
  }

  @Override public Color getPixelColor(int[] data) {
    return getColor(clampValue(data[0]));
  }

  private double clampValue(double value) {

    // 2022-05-21 : Don't try to interpolate noDataValue in the ColorTreeMap
    if (isNoData(value) && getNoDataColor() != null) {
      return value;
    }
    // If symbology min value is higher than raster min value
    // the value becomes equal to the symbology min value
    Double[] symbologyClassLimits =  getColorTreeMap().keySet().toArray(new Double[0]);
    double newMinValue = symbologyClassLimits[0];
    if(isNoData(newMinValue)) {
      newMinValue = symbologyClassLimits[1];
    }

    if(!this.isNoData(value) && value < newMinValue) {
      return newMinValue;
    } else {
      return value;
    }
  }

  public Color getColor(int value) {
    return getColor((double)value);
  }


  public Color getColor(double value) {

    // 2022-05-21 : special handling of noData values
    if (isNoData(value)) {
        return getNoDataColor();
    }

    if(getType().equals(TYPE_RAMP)) {

      ColorMapEntry downColorMapEntry = getColorMapEntry(value);
      ColorMapEntry upColorMapEntry = getNextColorMapEntry(value);

      if(downColorMapEntry != null && upColorMapEntry == null) {
        return downColorMapEntry.getColor();
      } else if(downColorMapEntry == null && upColorMapEntry != null) {
        return upColorMapEntry.getColor();
      } else if(downColorMapEntry != null && upColorMapEntry != null) {

        if(downColorMapEntry.getColor() == null) {
          return null;
        } else if(upColorMapEntry.getColor() == null) {
          return downColorMapEntry.getColor();
        } else {
          double distDown = value - downColorMapEntry.getUpperValue();
          double distUp = upColorMapEntry.getUpperValue()- value;

          double relDist = distDown / (distUp + distDown);

          return calculateNewColor(
              downColorMapEntry.getColor(),
              upColorMapEntry.getColor(),
              relDist);
        }
      } else {
        return null;
      }

    } else if(getType().equals(TYPE_INTERVALS)) {

      ColorMapEntry downColorMapEntry = getColorMapEntry(value);
      if(downColorMapEntry == null) {
        return null;
      }
      return downColorMapEntry.getColor();

    } else if(getType().equals(TYPE_SINGLE)) {

      ColorMapEntry downColorMapEntry = getColorMapEntry(value);
      if(downColorMapEntry == null) {
        return null;
      }
      return downColorMapEntry.getColor();


    } else {
      return null;
    }

  }

  private static Color calculateNewColor (Color downColor, Color upColor, double relDist) {
    int red = (int) Math.round((upColor.getRed() - downColor.getRed()) * relDist + downColor.getRed());
    int green = (int) Math.round((upColor.getGreen() - downColor.getGreen()) * relDist + downColor.getGreen());
    int blue = (int) Math.round((upColor.getBlue() - downColor.getBlue()) * relDist + downColor.getBlue());

    return new Color(red, green, blue);
  }


  public static final String TYPE_RAMP = "RAMP";
  public static final String TYPE_INTERVALS = "INTERVALS";
  public static final String TYPE_SINGLE = "SINGLE";

}
