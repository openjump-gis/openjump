/*******************************************************************************
    GridExtent.java
    Copyright (C) Victor Olaya

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*******************************************************************************/
package org.openjump.core.rasterimage.sextante.rasterWrappers;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.openjump.core.rasterimage.sextante.ISextanteLayer;
import org.openjump.core.rasterimage.sextante.ISextanteRasterLayer;


/**
 * This class defines a grid system (coordinates and cellsize)
 * @author Victor Olaya
 *
 */
public class GridExtent {

	//these values are cell border coordinates, not centered ones
	double m_dXMin;
	double m_dYMin;
	double m_dXMax;
	double m_dYMax;
	double m_dCellSize = 1;
	int m_iNX;
	int m_iNY;

	public GridExtent(){}

	/**
	 * Creates a new grid extent using the extent of a layer
	 * If it is a raster layer, it will also use its cellsize
	 * @param layer a layer
	 */
	public GridExtent (ISextanteLayer layer){

		m_dXMin = layer.getFullExtent().getMinX();
		m_dXMax = layer.getFullExtent().getMaxX();
		m_dYMin = layer.getFullExtent().getMinY();
		m_dYMax = layer.getFullExtent().getMaxY();

		if(layer instanceof ISextanteRasterLayer){
			ISextanteRasterLayer rasterLayer = (ISextanteRasterLayer) layer;
			m_dCellSize = rasterLayer.getLayerGridExtent().getCellSize();
		}

		recalculateNXAndNY();

	}

	/**
	 * Sets a new range for X coordinates. Coordinates are not center
	 * cell ones, but border ones
	 * @param dXMin the minimum x coordinate of the extent.
	 * @param dXMax the maximum x coordinate of the extent
	 */
	public void setXRange(double dXMin, double dXMax){

		m_dXMin = Math.min(dXMin, dXMax);
		m_dXMax = Math.max(dXMin, dXMax);
		recalculateNXAndNY();

	}

	/**
	 * Sets a new range for Y coordinates. Coordinates are not center
	 * cell ones, but border ones
	 * @param dYMin the minimum Y coordinate of the extent.
	 * @param dYMax the maximum Y coordinate of the extent
	 */
	public void setYRange(double dYMin, double dYMax){

		m_dYMin = Math.min(dYMin, dYMax);
		m_dYMax = Math.max(dYMin, dYMax);
		recalculateNXAndNY();

	}

	/**
	 * Returns the cellsize of this extent
	 * @return the cells size of this extent
	 */
	public double getCellSize() {

		return m_dCellSize;

	}

	/**
	 * Sets a new cellsize for this extent
	 * @param cellSize the new cellsize
	 */
	public void setCellSize(double cellSize) {

		m_dCellSize = cellSize;
		recalculateNXAndNY();

	}

	/**
	 * Returns the number of columns in the extent
	 * @return the number of columns
	 */
	public int getNX() {

		return m_iNX;

	}

	/**
	 * Returns the number of rows in the extent
	 * @return the number of rows
	 */
	public int getNY() {

		return m_iNY;

	}

	private void recalculateNXAndNY(){

		m_iNY = (int) Math.floor((m_dYMax - m_dYMin) / m_dCellSize);
		m_iNX = (int) Math.floor((m_dXMax - m_dXMin) / m_dCellSize);
		m_dXMax = m_dXMin + m_dCellSize * m_iNX;
		m_dYMax = m_dYMin + m_dCellSize * m_iNY;

	}

	/**
	 * Return the minimum x coordinate of the extent. This is not the
	 * coordinate of the center of the left-most cell, but the the
	 * coordinate of its left border
	 * @return the minimum x coordinate of the extent
	 */
	public double getXMin() {

		return m_dXMin;

	}

	/**
	 * Return the maximum x coordinate of the extent. This is not the
	 * coordinate of the center of the right-most cell, but the the
	 * coordinate of its right border
	 * @return the maximum x coordinate of the extent
	 */
	public double getXMax() {

		return m_dXMax;

	}

	/**
	 * Return the minimum x coordinateof the extent. This is not the
	 * coordinate of the center of the lower cell, but the the
	 * coordinate of its lower border
	 * @return the minimum y coordinate of the extent
	 */
	public double getYMin() {

		return m_dYMin;

	}

	/**
	 * Return the maximum y coordinate of the extent. This is not the
	 * coordinate of the center of the upper cell, but the the
	 * coordinate of its upper border
	 * @return the maximum x coordinate of the extent
	 */
	public double getYMax() {

		return m_dYMax;

	}

	/**
	 * Returns the real X distance spanned by this extent
	 * @return the real X distance spanned by this extent
	 */
	public double getWidth(){

		return m_dXMax - m_dXMin;

	}

	/**
	 * Returns the real Y distance spanned by this extent
	 * @return the real Y distance spanned by this extent
	 */
	public double getHeight(){

		return m_dYMax - m_dYMin;

	}

	/**
	 * Returns true if the given point falls within the area covered
	 * by this extent
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 * @return whether the given point falls within the area covered
	 * by this extent
	 */
	public boolean contains(double x, double y){

		return (x >= m_dXMin && x <= m_dXMax && y >= m_dYMin && y <= m_dYMax);

	}

	/**
	 * Returns true if the given extents matches the grid defined by this
	 * grid extent (has same size and cell boundaries match)
	 * @param extent
	 * @return whether the passed extent matches fits into this extent
	 */
	public boolean fitsIn(GridExtent extent){

		boolean bFitsX, bFitsY;
		double dOffset;
		double dOffsetCols;
		double dOffsetRows;
		final double MIN_DIF = 0.00001;

		if (extent.getCellSize() != this.getCellSize()){
			return false;
		}
		dOffset = Math.abs(extent.getXMin() - this.getXMin());
		dOffsetCols = dOffset / this.getCellSize();
		bFitsX = (dOffsetCols - Math.floor(dOffsetCols + 0.5) < MIN_DIF);

		dOffset = Math.abs(extent.getYMax() - this.getYMax());
		dOffsetRows = dOffset / this.getCellSize();
		bFitsY = (Math.abs(dOffsetRows - Math.floor(dOffsetRows + 0.5)) < MIN_DIF);

		return bFitsX && bFitsY;

	}

	/**
	 * Returns true if this extent has them same characteristics as a given one
	 * @param extent
	 * @return whether this extent equals the given extent
	 */
	public boolean equals(GridExtent extent){

		return m_dXMin == extent.getXMin()
			&& m_dXMax == extent.getXMax()
			&& m_dYMin == extent.getYMin()
			&& m_dYMax == extent.getYMax()
			&& m_dCellSize == extent.getCellSize();

	}

	/**
	 * Modifies this extent to incorporate another one into its
	 * boundaries
	 * @param extent the extent to add
	 */
	public void addExtent(GridExtent extent){

		m_dXMin = Math.min(extent.getXMin(), m_dXMin);
		m_dXMax = Math.max(extent.getXMax(), m_dXMax);
		m_dYMin = Math.min(extent.getYMin(), m_dYMin);
		m_dYMax = Math.max(extent.getYMax(), m_dYMax);
		m_dCellSize = Math.min(extent.getCellSize(), m_dCellSize);
		recalculateNXAndNY();

	}

	/**
	 * Converts a world coordinate to grid coordinates
	 * @param pt a point in world coordinates
	 * @return a grid cell with coordinates of the given point in
	 * grid coordinates referred to this grid extent
	 */
	public GridCell getGridCoordsFromWorldCoords(Point2D pt){

		int x = (int)Math.floor((pt.getX() - m_dXMin) / m_dCellSize);
		int y = (int)Math.floor((m_dYMax - pt.getY()) / m_dCellSize);

		GridCell cell = new GridCell(x, y, 0.0);

		return cell;

	}

	/**
	 * Converts a world coordinate to grid coordinates
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 * @return a grid cell representing the given point in
	 * grid coordinates referred to this grid extent
	 */
	public GridCell getGridCoordsFromWorldCoords(double x, double y){

		return getGridCoordsFromWorldCoords(new Point2D.Double(x,y));

	}

	/**
	/**
	 * Converts a grid cell into a world coordinate representing
	 * the center of that cell
	 * @param cell the cell to convert
	 * @return a point representing the given cell in world coordinates
	 */
	public Point2D getWorldCoordsFromGridCoords(GridCell cell){

		double x = m_dXMin + (cell.getX() + 0.5) * m_dCellSize;
		double y = m_dYMax - (cell.getY() + 0.5) * m_dCellSize;

		Point2D pt = new Point2D.Double(x, y);

		return pt;

	}

	/**
	 * Converts a grid cell into a world coordinate representing
	 * the center of that cell
	 * @param x the x coordinate (col) of the cell
	 * @param y the y coordinate (row) of the cell
	 * @return a point representing the given cell in world coordinates
	 */
	public Point2D getWorldCoordsFromGridCoords(int x, int y){

		return getWorldCoordsFromGridCoords(new GridCell(x, y, 0));

	}

	public String toString(){

		String s = Double.toString(m_dXMin) + ", "
					+ Double.toString(m_dYMin) + ", "
					+ Double.toString(m_dXMax) + ", "
					+ Double.toString(m_dYMax) + ", "
					+ Double.toString(m_dCellSize);

		return s;

	}

//	public String asCommandLineParameters(){
//
//		String s = "\"" + Double.toString(m_dXMin) + "\", "
//					+ "\"" + Double.toString(m_dYMin) + "\", "
//					+ "\"" + Double.toString(m_dXMax) + "\", "
//					+ "\"" + Double.toString(m_dYMax) + "\", "
//					+ "\"" + Double.toString(m_dCellSize) + "\"";
//
//		return s;
//
//	}

	/**
	 * Enlarges this grid extent one cell in each direction
	 */
	public void enlargeOneCell() {

		m_dYMin = m_dYMin - m_dCellSize;
		m_dXMin = m_dXMin - m_dCellSize;
		m_dXMax = m_dXMax + m_dCellSize;
		m_dYMax = m_dYMax + m_dCellSize;
		this.recalculateNXAndNY();

	}

	/**
	 * Returns this extent as a Java Rectangle2D
	 * @return the extent of this grid extent as a Jave Rectangle2D
	 */
	public Rectangle2D getAsRectangle2D(){

		Rectangle2D rect = new Rectangle2D.Double();
		rect.setRect(m_dXMin, m_dYMin, m_dXMax - m_dXMin, m_dYMax - m_dYMin);

		return rect;

	}

	/**
	 * Returns true if the cell is within the limits of this
	 * grid extent
	 * @param x the x coordinate (col) of the cell
	 * @param y the y coordinate (row) of the cell
	 * @return whether the cell is within the limits of this
	 * grid extent
	 */
	public boolean containsCell(int x, int y) {

		return x >= 0 && x < m_iNX && y >= 0 && y < m_iNY;

	}


}
