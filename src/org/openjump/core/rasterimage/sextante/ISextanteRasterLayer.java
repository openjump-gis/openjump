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

	int RASTER_DATA_TYPE_FLOAT = DataBuffer.TYPE_FLOAT;
	int RASTER_DATA_TYPE_DOUBLE = DataBuffer.TYPE_DOUBLE;
	int RASTER_DATA_TYPE_INT = DataBuffer.TYPE_INT;
	int RASTER_DATA_TYPE_SHORT = DataBuffer.TYPE_SHORT;
	int RASTER_DATA_TYPE_BYTE = DataBuffer.TYPE_BYTE;

	int INTERPOLATION_NearestNeighbour = 0;
	int INTERPOLATION_Bilinear = 1;
	int INTERPOLATION_InverseDistance = 2;
	int INTERPOLATION_BicubicSpline = 3;
	int INTERPOLATION_BSpline = 4;

	/**
	 * Returns the data type of the layer
	 * @return the data type of the layer
	 */
	int getDataType();

	/**
	 * Returns the grid extent of the query window of this layer.
	 * Each layer can have a query window, which makes it easier to seamlessly
	 * combine several layers.
	 * @return the window grid extent
	 */
	GridExtent getWindowGridExtent();

	/**
	 * Returns the grid extent of the layer
	 * @return the grid extent of the layer
	 */
	GridExtent getLayerGridExtent();

	/**
	 * Return the cellsize of the query window. X and Y cellsizes are assumed to be equal
	 * @return the cellsize of the query window
	 */
	java.awt.Point.Double getWindowCellSize();

	/**
	 * Return the original cellsize of the layer. X and Y cellsizes are assumed to be equal
	 * @return the original cellsize of the layer
	 */
	java.awt.Point.Double getLayerCellSize();


	void assign(double dValue);
	void assign(ISextanteRasterLayer layer);
	void assignNoData();

	/**
	 * Sets the value at a cell, in the first band of the layer
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param dValue the new value
	 */
	void setCellValue(int x, int y, double dValue);

	/**
	 * Sets the value at a cell
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param iBand the band (zero-based)
	 * @param dValue the new value
	 */
	void setCellValue(int x, int y, int iBand, double dValue);

	/**
	 * Adds a value to the current value of a cell in the first
	 * band of the layer
	 * in the first band of the layer
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param dValue the value to sum
	 */
	void addToCellValue(int x, int y, double dValue);

	/**
	 * Adds a value to the current value of a cell
	 * @param x the x coordinate (cols)
	 * @param y the y coordinate (rows)
	 * @param iBand the band (zero-based)
	 * @param dValue the value to sum
	 */
	void addToCellValue(int x, int y, int iBand, double dValue);

	/**
	 * adds the value of another raster layer to this one.
	 * Both layers must have the same window
	 * This is supposed to be used with monoband layers, so only
	 * the first band of each one is used.
	 * @param layer the ISextanteRasterLayer to add to this one
	 */
	void add(ISextanteRasterLayer layer);

	/**
	 * Multiplies all the values of a layer by a fixed value
	 * @param d the value to multiply
	 */
	void multiply(double d);

	/**
	 * Gets the no-data value of the layer
	 * @return the no-data value of the layer
	 */
	double getNoDataValue();

	/**
	 * Sets the no-data value of the layer
	 * @param dNoDataValue the new no-data value
	 */
	void setNoDataValue(double dNoDataValue);

	/**
	 * Set the value of a cell to the no-data value
	 * @param x the x coordinate(col) of the cell to set to no-data
	 * @param y the y coordinate(row) of the cell to set to no-data
	 * @param iBand the band to set to no-data
	 */
	void setNoData(int x, int y, int iBand);

	/**
	 * Set the value of a cell in the first band of a layer to the no-data value
	 * @param x the x coordinate(col) of the cell to set to no-data
	 * @param y the y coordinate(row) of the cell to set to no-data
	 */
	void setNoData(int x, int y);

	/**
	 * Checks if the given value equals the no-data value of the layer
	 * @param dNoDataValue a value to check
	 * @return true if the given value equals the no-data value of the layer
	 */
	boolean isNoDataValue(double dNoDataValue);

	byte getCellValueAsByte(int x, int y, int iBand);
	byte getCellValueAsByte(int x, int y) ;
	short getCellValueAsShort(int x, int y, int iBand);
	short getCellValueAsShort(int x, int y) ;
	int getCellValueAsInt(int x, int y, int iBand) ;
	int getCellValueAsInt(int x, int y) ;
	float getCellValueAsFloat(int x, int y, int iBand);
	float getCellValueAsFloat(int x, int y);
	double getCellValueAsDouble(int x, int y, int iBand);
	double getCellValueAsDouble(int x, int y) ;

	/**
	 * Returns the value of a cell in the original image coords, that is,
	 * not using the query window
	 * @param x the x coordinate(col) of the cell
	 * @param y the y coordinate(row) of the cell
	 * @param band the band to be queried
	 * @return the value of a cell in the original image coords.
	 */
	double getCellValueInLayerCoords(int x, int y, int band);

	/**
	 * Returns the value at a given world coordinate.
	 * The current interpolation method is used if the coordinate does not fall
	 * on the exact center of a cell.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param iBand the band to query
	 * @return the value at the given world coordinate
	 */
	double getValueAt(double x, double y, int iBand);

	/**
	 * Returns the value of the first band of this layer, at a given world coordinate.
	 * The current interpolation method is used if the coordinate does not fall
	 * on the exact center of a cell.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the value at the given coordinate. If it falls outside of the layer,
	 * the current no-data value of the layer is returned
	 */
	double getValueAt(double x, double y);

	/**
	 *
	 * @param x the x coordinate(col) of the cell
	 * @param y the y coordinate(row) of the cell
	 * @return true if the given ccell is within the query window extent
	 */
	boolean isInWindow(int x, int y);

	/**
	 * Returns the number of columns in the query window of this layer
	 * @return the number of columns in the query window
	 */
	int getNX();

	/**
	 * Returns the number of rows in the query window of this layer
	 * @return the number of rows in the query window
	 */
	int getNY();

	/**
	 * Returns the number of bands of the layer
	 * @return the number of bands of the layer
	 */
	int getBandsCount();

	/**
	 * Sets the extent of the query window using the full extent of another layer
	 * @param layer the layer from which to take the extent
	 */
	void setWindowExtent(ISextanteRasterLayer layer);

	/**
	 * Sets a new query window extent
	 * @param gridExtent the new grid extent to set fro this layer
	 */
	void setWindowExtent(GridExtent gridExtent);

	/**
	 * Sets the query window extent to the full extent of the layer
	 */
	void setFullExtent();

	/**
	 * Resamples the layer so the new full extent is the same as a given gridExtent
	 * @param gridExtent the reference gridExtent
	 * @param layerManager the LayerManager
	 */
	void fitToGridExtent(GridExtent gridExtent, LayerManager layerManager) ;

	/**
	 * Sets the interpolation method to use for calculating value in points other
	 * than cell centers
	 * @param iMethod method to use for interpolation
	 */
	void setInterpolationMethod(int iMethod);

	/**
	 * Returns the mean value in the first band of the layer
	 * @return the mean value of the first band of this layer
	 */
	double getMeanValue();

	/**
	 * Returns the min value in the first band of the layer
	 * @return the min value of the first band of this layer
	 */
	double getMinValue();

	/**
	 * Returns the max value in the first band of the layer
	 * @return the max value of the first band of this layer
	 */
	double getMaxValue();

	/**
	 * Returns the variance in the first band of the layer
	 * @return the variance of the first band of this layer
	 */
	double getVariance();

	/**
	 * Returns the mean value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the mean value of the given band of this layer
	 */
	double getMeanValue(int iBand);

	/**
	 * Returns the min value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the min value of the given band of this layer
	 */
	double getMinValue(int iBand);

	/**
	 * Returns the max value in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the max value of the given band of this layer
	 */
	double getMaxValue(int iBand);

	/**
	 * Returns the variance in the given band of the layer
	 * @param iBand the index of the band (zero-based)
	 * @return the variance of the given band of this layer
	 */
	double getVariance(int iBand);

	double getSlope(int x, int y);
	double getDistToNeighborInDir(int i);
	double getAspect(int i, int j);
	int getDirToNextDownslopeCell(int x, int y);
	int getDirToNextDownslopeCell(int x, int y, boolean b);

	/**
	 * Returns the histogram of the first band of this layer
	 * @return a histogram of the first band of this layer
	 */
	int[] getHistogram();

	/**
	 * Returns a histogram of a given band of this layer
	 * @param iBand the index of the band (zero-based)
	 * @return a histogram of the given band of this layer
	 */
	int[] getHistogram(int iBand);
	int[] getAccumulatedHistogram();

}
