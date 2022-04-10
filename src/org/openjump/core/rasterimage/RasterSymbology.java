package org.openjump.core.rasterimage;

import org.locationtech.jts.util.Assert;

import java.awt.Color;

/**
 *
 * @author Michaël Michaud
 */
abstract public class RasterSymbology implements IRasterSymbology {

    private double transparency = 0;
    private double minNoDataValue = Double.NaN;
    private double maxNoDataValue = Double.NaN;
    private Color noDataColor = new Color(0,0,0,0);
    private String type;
    

    public RasterSymbology() {
    }
    
    public RasterSymbology (double transparency) {
        this.transparency = transparency;
    }

    public RasterSymbology (double transparency, double noDataValue) {
        this.transparency = transparency;
        this.minNoDataValue = noDataValue;
        this.maxNoDataValue = noDataValue;
    }

    public RasterSymbology (double transparency, double noDataValue, Color noDataColor) {
        this.transparency = transparency;
        this.minNoDataValue = noDataValue;
        this.maxNoDataValue = noDataValue;
        this.noDataColor = noDataColor;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public double getTransparency() {
        return transparency;
    }

    @Override
    public void setTransparency(double transparency) {
        this.transparency = transparency;
    }

    @Override
    public double getMinNoDataValue() {
        return minNoDataValue;
    }

    @Override
    public void setMinNoDataValue(double minNoDataValue) {
        this.minNoDataValue = minNoDataValue;
    }

    @Override
    public double getMaxNoDataValue() {
        return maxNoDataValue;
    }

    @Override
    public void setMaxNoDataValue(double maxNoDataValue) {
        this.maxNoDataValue = maxNoDataValue;
    }

    @Override
    public Color getNoDataColor() {
        return noDataColor;
    }

    @Override
    public void setNoDataColor(Color noDataColor) {
        this.noDataColor = noDataColor;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Assert.shouldNeverReachHere();
            return null;
        }
    }
}
