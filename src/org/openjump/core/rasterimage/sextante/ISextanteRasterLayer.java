package org.openjump.core.rasterimage.sextante;

import java.awt.image.DataBuffer;

import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;

import com.vividsolutions.jump.workbench.model.LayerManager;


/**
 * This is the base interface that all raster object have to implement
 * to be able to be used by SEXTANTE algorithms.
 *
 * Instead of implementing this class directly, it is recommended to
 * extend {@link AbstractSextanteRasterLayer}, since it solves some
 * of the most complex methods, such as grid window definition and resampling
 * methods
 * @author Victor Olaya. volaya@unex.es
 *
 */
public interface ISextanteRasterLayer extends ISextanteLayer{

	public static final int RASTER_DATA_TYPE_FLOAT = DataBuffer.TYPE_FLOAT;
	public static final int RASTER_DATA_TYPE_DOUBLE = DataBuffer.TYPE_DOUBLE;
	public static final int RASTER_DATA_TYPE_INT = DataBuffer.TYPE_INT;
	public static final int RASTER_DATA_TYPE_SHORT = DataBuffer.TYPE_SHORT;
	public static final int RASTER_DATA_TYPE_BYTE = DataBuffer.TYPE_BYTE;

	public static final int INTERPOLATION_NearestNeighbour = 0;
	public static final int INTERPOLATION_Bilinear = 1;
	public static final int INTERPOLATION_InverseDistance = 2;
	public static final int INTERPOLATION_BicubicSpline = 3;
	public static final int INTERPOLATION_BSpline = 4;

	/**
	 * Returns the data type of the layer
	 * @return the data type of the layer
	 */
	public int getDataType();

	/**
	 * Returns the grid extent of the query window of this layer.
	 * Each layer can have a query window, which makes it easier to seamlessly
	 * combine several layers.
	 * @return the window grid extent
	 */
	public GridExtent getWindowGridExtent();

	/**
	 * Returns the grid extent of the layer
	 * @return the grid extent of the layer
	 */
	public GridExtent getLayerGridExtent();

	/**
	 * Return the cellsize of the query window. X and Y cellsizes are assumed to be equal
	 * @return the cellsize of the query window
	 */
	public java.awt.Point.Double getWindowCellSize();

	/**
	 * Return the original cellsize of the layer. X and Y cellsizes are assumed to be equal
	 * @return the original cellsize of the layer
	 */
	public java.awt.Point.Double getLayerCellSize();


	public void assign(double dValue);
	public void assign(ISextanteRasterLayer layer);
	public void assignNoData();

	/**
	 * Sets the value at a cell, in the first band of the layer
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param dValue the new value
	 */
	public void setCellValue(int x, int y, double dValue);

	/**
	 * Sets the value at a cell
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param iBand the band (zero-based)
	 * @param dValue the new value
	 */
	public void setCellValue(int x, int y, int iBand, double dValue);

	/**
	 * Adds a value to the current value of a cell in the first
	 * band of the layer
	 * in the first band of the layer
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param dValue the value to sum
	 */
	public void addToCellValue(int x, int y, double dValue);

	/**
	 * Adds a value to the current value of a cell
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param iBand the band (zero-based)
	 * @param dValue the value to sum
	 */
	public void addToCellValue(int x, int y, int iBand, double dValue);

	/**
	 * adds the value of another raster layer to this one.
	 * Both layers must have the same window
	 * This is supposed to be used with monoband layers, so only
	 * the first band of each one is used.
	 */
	public void add(ISextanteRasterLayer layer);

	/**
	 * Multiplies all the values of a layer by a fixed value
	 * @param d the value to multiply
	 */
	public void multiply(double d);

	/**
	 * Gets the no-data value of the layer
	 * @return the no-data value of the layer
	 */
	public double getNoDataValue();

	/**
	 * Sets the no-data value of the layer
	 * @param dNoDataValue the new no-data value
	 */
	public void setNoDataValue(double dNoDataValue);

	/**
	 * Set the value of a cell to the no-data value
	 * @param x the x coordinate(col) of the cell to set to no-data
	 * @param y the y coordinate(row) of the cell to set to no-data
	 * @param iBand the band to set to no-data
	 */
	public void setNoData(int x, int y, int iBand);

	/**
	 * Set the value of a cell in the first band of a layer to the no-data value
	 * @param x the x coordinate(col) of the cell to set to no-data
	 * @param y the y coordinate(row) of the cell to set to no-data
	 */
	public void setNoData(int x, int y);

	/**
	 * Checks if the given value equals the no-data value of the layer
	 * @param dNoDataValue a value to check
	 * @return true if the given value equals the no-data value of the layer
	 */
	public boolean isNoDataValue(double dNoDataValue);

	public byte getCellValueAsByte(int x, int y, int iBand);
	public byte getCellValueAsByte(int x, int y) ;
	public short getCellValueAsShort(int x, int y, int iBand);
	public short getCellValueAsShort(int x, int y) ;
	public int getCellValueAsInt(int x, int y, int iBand) ;
	public int getCellValueAsInt(int x, int y) ;
	public float getCellValueAsFloat(int x, int y, int iBand);
	public float getCellValueAsFloat(int x, int y);
	public double getCellValueAsDouble(int x, int y, int iBand);
	public double getCellValueAsDouble(int x, int y) ;

	/**
	 * Returns the value of a cell in the original image coords, that is,
	 * not using the query window
	 * @param x the x coordinate(col) of the cell
	 * @param y the y coordinate(row) of the cell
	 * @param band the band to be queried
	 * @return the value of a cell in the original image coords.
	 */
	public double getCellValueInLayerCoords(int x, int y, int band);

	/**
	 * Returns the value at a given world coordinate.
	 * The current interpolation method is used if the coordinate does not fall
	 * on the exact center of a cell.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param iBand the band to query
	 * @return the value at the given world coordinate
	 */
	public double getValueAt(double x, double y, int iBand);

	/**
	 * Returns the value of the first band of this layer, at a given world coordinate.
	 * The current interpolation method is used if the coordinate does not fall
	 * on the exact center of a cell.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the value at the given coordinate. If it falls outside of the layer,
	 * the current no-data value of the layer is returned
	 */
	public double getValueAt(double x, double y);

	/**
	 *
	 * @param x the x coordinate(col) of the cell
	 * @param y the y coordinate(row) of the cell
	 * @return true if the given ccell is within the query window extent
	 */
	public boolean isInWindow(int x, int y);

	/**
	 * Returns the number of columns in the query window of this layer
	 * @return the number of columns in the query window
	 */
	public int getNX();

	/**
	 * Returns the number of rows in the query window of this layer
	 * @return the number of rows in the query window
	 */
	public int getNY();

	/**
	 * Returns the number of bands of the layer
	 * @return the number of bands of the layer
	 */
	public int getBandsCount();

	/**
	 * Sets the extent of the query window using the full extent of another layer
	 * @param layer the layer from which to take the extent
	 */
	public void setWindowExtent(ISextanteRasterLayer layer);

	/**
	 * Sets a new query window extent
	 * @param gridExtent the new grid extent to set fro this layer
	 */
	public void setWindowExtent(GridExtent gridExtent);

	/**
	 * Sets the query window extent to the full extent of the layer
	 */
	public void setFullExtent();

	/**
	 * Resamples the layer so the new full extent is the same as a given gridExtent
	 * @param gridExtent the reference gridExtent
	 */
	public void fitToGridExtent(GridExtent gridExtent, LayerManager layerManager) ;

	/**
	 * Sets the interpolation method to use for calculating value in points other
	 * than cell centers
	 * @param iMethod
	 */
	public void setInterpolationMethod(int iMethod);

	/**
	 * Returns the mean value in the first band of the layer
	 * @return the mean value of the first band of this layer
	 */
	public double getMeanValue();

	/**
	 * Returns the min value in the first band of the layer
	 * @return the min value of the first band of this layer
	 */
	public double getMinValue();

	/**
	 * Returns the max value in the first band of the layer
	 * @return the max value of the first band of this layer
	 */
	public double getMaxValue();

	/**
	 * Returns the variance in the first band of the layer
	 * @return the variance of the first band of this layer
	 */
	public double getVariance();

	/**
	 * Returns the mean value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the mean value of the given band of this layer
	 */
	public double getMeanValue(int iBand);

	/**
	 * Returns the min value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the min value of the given band of this layer
	 */
	public double getMinValue(int iBand);

	/**
	 * Returns the max value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the max value of the given band of this layer
	 */
	public double getMaxValue(int iBand);

	/**
	 * Returns the variance in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the variance of the given band of this layer
	 */
	public double getVariance(int iBand);

	public double getSlope(int x, int y);
	public double getDistToNeighborInDir(int i);
	public double getAspect(int i, int j);
	public int getDirToNextDownslopeCell(int x, int y);
	public int getDirToNextDownslopeCell(int x, int y, boolean b);

	/**
	 * Returns the histogram of the first band of this layer
	 * @return a histogram of the first band of this layer
	 */
	public int[] getHistogram();

	/**
	 * Returns a histogram of a given band of this layer
	 * @param iBand the index of the band (zero-based)
	 * @return a histogram of the given band of this layer
	 */
	public int[] getHistogram(int iBand);
	public int[] getAccumulatedHistogram();

}
