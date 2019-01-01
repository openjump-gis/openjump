package org.openjump.core.rasterimage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import java.awt.Point;

/**
 *
 * @author AdL
 */
    
public class Metadata {

    public Metadata(Envelope originalImageEnvelope, Envelope actualEnvelope,
            Point originalSize, Point actualSize,
            double originalCellSize, double actualCellSize, double noDataValue, Stats stats) {
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
    
    public double getOriginalCellSize() {                      
        return originalCellSize;
    }        

    public double getActualCellSize() {                      
        return actualCellSize;
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
    private final double originalCellSize;
    private final double actualCellSize;
    private double noDataValue = -3.40282346639e+038;;
    private final Stats stats;

}
    
