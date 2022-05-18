package org.openjump.core.rasterimage;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.openjump.core.ccordsys.utils.SRSInfo;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 *
 * @author AdL
 */
    
public class Metadata {

    public static double DEFAULT_NODATA_VALUE = -3.40282346639e+038;

    public Metadata(Envelope originalImageEnvelope, Envelope actualEnvelope,
                    Point originalSize, Point actualSize,
                    Resolution originalCellSize, Resolution actualCellSize,
                    double noDataValue, Stats stats) {
        this.originalImageEnvelope = originalImageEnvelope;
        this.actualEnvelope = actualEnvelope;
        this.originalSize = originalSize;
        this.actualSize = actualSize;
        this.originalCellSize = originalCellSize;
        this.actualCellSize = actualCellSize;
        this.noDataValue = noDataValue;
        this.stats = stats;
    }

    public Envelope getOriginalImageEnvelope() {
        return originalImageEnvelope;
    }
    
    public Envelope getActualEnvelope() {
        return actualEnvelope;
    }

    public Coordinate getOriginalImageLowerLeftCoord() {
        return new Coordinate(originalImageEnvelope.getMinX(), originalImageEnvelope.getMinY());
    }

    public Coordinate getActualLowerLeftCoord() {
        return new Coordinate(actualEnvelope.getMinX(), actualEnvelope.getMinY());
    }
    
    public Point getOriginalSize() {
        return originalSize;
    }

    public Point getActualSize() {
        return actualSize;
    }
    
    public Resolution getOriginalCellSize() {
        return new Resolution(
            getOriginalImageEnvelope().getWidth() / getOriginalSize().x,
            getOriginalImageEnvelope().getHeight() / getOriginalSize().y
        );
    }

    public Resolution getActualCellSize() {
        return new Resolution(
            getActualEnvelope().getWidth() / getActualSize().x,
            getActualEnvelope().getHeight() / getActualSize().y
        );
    }
    
    public double getNoDataValue() {
        return noDataValue;
    }

    public Stats getStats() {
        return stats;
    }

    private final Envelope originalImageEnvelope;
    private final Envelope actualEnvelope;
    private final Point originalSize;
    private final Point actualSize;
    private final Resolution originalCellSize;
    private final Resolution actualCellSize;
    private double noDataValue = DEFAULT_NODATA_VALUE;
    private final Stats stats;

}
    
