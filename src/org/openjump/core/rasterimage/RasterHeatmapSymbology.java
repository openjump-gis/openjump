package org.openjump.core.rasterimage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RasterHeatmapSymbology extends RasterSymbology {

  private static final String HEATMAP = "HEATMAP";

  List<Color> colors = new ArrayList<>();        // Color for each band (may have transparency)
  List<Integer> thresholds = new ArrayList<>();  // values under threshold are totally transparent
  // max value for the band (currently, only 8bit band is correctly managed)
  double max = 255;


  // Used by Java2XML
  public RasterHeatmapSymbology() {}

  public RasterHeatmapSymbology(List<Color> colors, List<Integer> thresholds, double max) {
    this.colors = colors;
    this.thresholds = thresholds;
    this.max = max;
  }

  @Override
  public String getType() {
    return HEATMAP;
  }

  public List<Color> getColors() {
    return colors;
  }

  public void setColors(List<Color> colors) {
    this.colors = colors;
  }

  // Used by XML2java
  public void addColor(Color color) {
    colors.add(color);
  }

  public List<Integer> getThresholds() {
    return thresholds;
  }

  public void setThresholds(List<Integer> thresholds) {
    this.thresholds = thresholds;
  }

  // Used by XML2java
  public void addThreshold(Integer threshold) {
    thresholds.add(threshold);
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  @Override public Color getPixelColor(int[] values) {
    Color[] cc = new Color[values.length];
    for (int i = 0 ; i < values.length ; i++) {
      cc[i] = getColor(colors.get(i), thresholds.get(i), values[i]);
    }
    return mixColors(cc);
  }

  @Override public Color getPixelColor(double[] values) {
    Color[] cc = new Color[values.length];
    for (int i = 0 ; i < values.length ; i++) {
      cc[i] = getColor(colors.get(i), thresholds.get(i), values[i]);
    }
    return mixColors(cc);
  }


  /**
   * Apply transparency to the color
   * @param color input Color
   * @param threshold transparency is set from 1 to 0 between threshold and max
   *                  and pixel is totally transparent below threshold.
   * @param value predicted value for a particular band
   * @return new new Color with transparency
   */
  Color getColor(Color color, int threshold, int value) {
    int alpha = (int) (255f * Math.max(0.0, (float)(value-threshold)) / Math.max(0.0, (float)(max-threshold)));
    return new Color(
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        alpha
    );
  }

  /**
   * Apply transparency to the color
   * @param color input Color
   * @param threshold transparency is set from 1 to 0 between threshold and max
   *                  and pixel is totally transparent below threshold.
   * @param value predicted value for a particular band
   * @return new new Color with transparency
   */
  Color getColor(Color color, int threshold, double value) {
    //int alpha = (int) (255f * Math.max(0.0, (float)(value-threshold)) / Math.max(0.0, (float)(max-threshold)));
    double normalizedThreshold = threshold/256f;
    double normalizedValue = value/max;
    double normalizedAlpha = Math.max(0.0, normalizedValue-normalizedThreshold) / Math.max(0.0, 1.0-normalizedThreshold);
    return new Color(
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        (int)(normalizedAlpha*256f)
    );
  }


  Color mixColors(Color... colors) {
    float Ar = colors[0].getRed()/255f;
    float Ag = colors[0].getGreen()/255f;
    float Ab = colors[0].getBlue()/255f;
    float Aa = colors[0].getAlpha()/255f;
    for (Color color : colors) {
      float Br = color.getRed()/255f;
      float Bg = color.getGreen()/255f;
      float Bb = color.getBlue()/255f;
      float Ba = color.getAlpha()/255f;
      Ar = (Ar*Aa + Br*Ba*(1f-Aa));
      Ag = (Ag*Aa + Bg*Ba*(1f-Aa));
      Ab = (Ab*Aa + Bb*Ba*(1f-Aa));
      Aa = Aa + Ba*(1f - Aa);
    }
    return new Color((int)(Ar*255), (int)(Ag*255), (int)(Ab*255), (int)((Aa*255)));
  }




}
