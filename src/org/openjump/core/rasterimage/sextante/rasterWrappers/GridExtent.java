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
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterFactory;

import org.openjump.core.rasterimage.sextante.ISextanteLayer;
import org.openjump.core.rasterimage.sextante.ISextanteRasterLayer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;


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
	double m_dCellSizeX = 1;
    double m_dCellSizeY = 1;
	int m_iNX;
	int m_iNY;
	Envelope m_Envelope;
	//Added Raster to bypass java hip space on building  a double array of cell values
	//if cols*rows is big (large rasters)
    WritableRaster m_Raster;

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
			m_dCellSizeX = rasterLayer.getLayerGridExtent().getCellSize().x;
                        m_dCellSizeY = rasterLayer.getLayerGridExtent().getCellSize().y;
		}

		recalculateNXAndNY();

	}

	
	/**
	 * Creates a new grid extent using the extents of a set of layers.<br>
	 * This extend must contains all the input layers.
	 * If they the input layers are  raster layers, the cell size will be added by the first one
	 * @param 
	 */
	
	public  GridExtent (ISextanteRasterLayer[] rasterImageLayers) {
		m_dCellSizeX = rasterImageLayers[1].getLayerGridExtent().getCellSize().x;
        	m_dCellSizeY = rasterImageLayers[1].getLayerGridExtent().getCellSize().y;
		int i;
		int numImages = rasterImageLayers.length;
		boolean eastGreaterThanWest = true;
		boolean northGreaterThanSouth = true;
		final double[][] imageData = new double[numImages][6];
		double north = Double.NEGATIVE_INFINITY;
		double  south = Double.POSITIVE_INFINITY;
		double east = Double.NEGATIVE_INFINITY;
		double west = Double.POSITIVE_INFINITY;
		// retrieve info from each image.
		for (i = 0; i < numImages; i++) {
			final Rectangle2D env = rasterImageLayers[i].getFullExtent();
			imageData[i][0] = env.getMaxY();
			imageData[i][1] = env.getMinY();
			imageData[i][2] = env.getMaxX();
			imageData[i][3] = env.getMinX();
			imageData[i][4] = rasterImageLayers[i].getLayerGridExtent().getCellSize().x;
			imageData[i][5] = rasterImageLayers[i].getLayerGridExtent().getCellSize().y;
			if (i == 0) {
				if (imageData[i][0] < imageData[i][1]) {
					northGreaterThanSouth = false;
					north = Double.POSITIVE_INFINITY;
					south = Double.NEGATIVE_INFINITY;
				}
				if (imageData[i][2] < imageData[i][3]) {
					eastGreaterThanWest = false;
					east = Double.POSITIVE_INFINITY;
					west = Double.NEGATIVE_INFINITY;
				}
			}
			if (northGreaterThanSouth) {
				if (imageData[i][0] > north) {
					north = imageData[i][0];
				}
				if (imageData[i][1] < south) {
					south = imageData[i][1];
				}
			} else {
				if (imageData[i][0] < north) {
					north = imageData[i][0];
				}
				if (imageData[i][1] > south) {
					south = imageData[i][1];
				}
			}
			if (eastGreaterThanWest) {
				if (imageData[i][2] > east) {
					east = imageData[i][2];
				}
				if (imageData[i][3] < west) {
					west = imageData[i][3];
				}
			} else {
				if (imageData[i][2] < east) {
					east = imageData[i][2];
				}
				if (imageData[i][3] > west) {
					west = imageData[i][3];
				}
			}
			if (imageData[i][4] < m_dCellSizeX) {
				m_dCellSizeX = imageData[i][4];
			}
			if (imageData[i][5] < m_dCellSizeY) {
				m_dCellSizeY = imageData[i][4];
			}
			 
		}
		 setXRange(west, east);
		 setYRange(south, north);
		 setCellSize(m_dCellSizeX, m_dCellSizeY);
		 recalculateNXAndNY();
	}
	
	
	
	
	/**
	 * Sets a new range for X coordinates. Coordinates are not center
	 * cell ones, but border ones. Note, the CellSize needs to be set first
	 * before this method is used.
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
	 * cell ones, but border ones. Note, the CellSize needs to be set first
	 * before this method is used.
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
	public java.awt.Point.Double getCellSize() {

		return new java.awt.Point.Double(m_dCellSizeX, m_dCellSizeY);

	}
        
	public void setCellSize(double cellSizeX, double cellSizeY) {

		m_dCellSizeX = cellSizeX;
        m_dCellSizeY = cellSizeY;
		recalculateNXAndNY();

	}

	/**
	 * Method to build a GridExtent with a defined Raster
	 * 
	 * @param cellSizeX cell size along X
	 * @param cellSizeY cell size along Y
	 * @param envelope envelope of this GridExtent
	 * @param nodata nodata value
	 */

	   public void setValuesAndRaster(double cellSizeX, double cellSizeY, Envelope envelope, double nodata) {
		   m_dCellSizeX = cellSizeX;
		   m_dCellSizeY = cellSizeY;	
		   m_dXMin = Math.min(envelope.getMinX(), envelope.getMaxX());
		   m_dXMax = Math.max(envelope.getMinX(), envelope.getMaxX());
		   m_dYMin = Math.min(envelope.getMinY(), envelope.getMaxY());
		   m_dYMax = Math.max(envelope.getMinY(), envelope.getMaxY());
		   recalculateNXAndNY();
		   m_iNX = this.getNX();
		   m_iNY = this.getNY();
		   m_Raster = RasterFactory.createBandedRaster(
	                DataBuffer.TYPE_FLOAT, m_iNX , m_iNY , 1, null);
	       for (int i = 0; i < m_iNX; i++) {
	            for (int j = 0; j < m_iNY; j++) {
	            	m_Raster.setSample(i, j, 0, nodata);
	            }
	        }	            
		}
	 
	

		
	 	public WritableRaster getRaster() {
	 		return m_Raster;
	 	}
	 
	 	/**
		 * Returns the extension of Grid as org.locationtech.jts.geom.Envelope
		 * @return Envelope
		 */
	 	public Envelope getEnvelope() {
	 		return new Envelope(m_dXMin,m_dXMax,m_dYMin, m_dYMax );
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

		m_iNY = (int) Math.round((m_dYMax - m_dYMin) / m_dCellSizeY);
		m_iNX = (int) Math.round((m_dXMax - m_dXMin) / m_dCellSizeX);
		m_dXMax = m_dXMin + m_dCellSizeX * m_iNX;
		m_dYMax = m_dYMin + m_dCellSizeY * m_iNY;
		//Recalculating we get the Envelope, useful when a raster is saved as
		//layer
		m_Envelope = new Envelope(m_dXMin,m_dXMax,m_dYMin, m_dYMax );

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
	 * @param extent another gridExtent to compare this gridExtent to
	 * @return whether the passed extent matches fits into this extent
	 */
	public boolean fitsIn(GridExtent extent){

		boolean bFitsX, bFitsY;
		double dOffset;
		double dOffsetCols;
		double dOffsetRows;
		final double MIN_DIF = 0.00001;

		if (extent.getCellSize().x != this.getCellSize().x){
			return false;
		}
                if (extent.getCellSize().y != this.getCellSize().y){
			return false;
		}
		dOffset = Math.abs(extent.getXMin() - this.getXMin());
		dOffsetCols = dOffset / this.getCellSize().x;
		bFitsX = (dOffsetCols - Math.floor(dOffsetCols + 0.5) < MIN_DIF);

		dOffset = Math.abs(extent.getYMax() - this.getYMax());
		dOffsetRows = dOffset / this.getCellSize().y;
		bFitsY = (Math.abs(dOffsetRows - Math.floor(dOffsetRows + 0.5)) < MIN_DIF);

		return bFitsX && bFitsY;

	}

	/**
	 * Returns true if this extent has them same characteristics as a given one
	 * @param extent another gridExtent to compare this gridExtent to
	 * @return whether this extent equals the given extent
	 */
	public boolean equals(GridExtent extent){

		return m_dXMin == extent.getXMin()
			&& m_dXMax == extent.getXMax()
			&& m_dYMin == extent.getYMin()
			&& m_dYMax == extent.getYMax()
			&& m_dCellSizeX == extent.getCellSize().x
                        && m_dCellSizeY == extent.getCellSize().y;

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
		m_dCellSizeX = Math.min(extent.getCellSize().x, m_dCellSizeX);
                m_dCellSizeY = Math.min(extent.getCellSize().y, m_dCellSizeY);
		recalculateNXAndNY();

	}

	/**
	 * Converts a world coordinate to grid coordinates
	 * @param pt a point in world coordinates
	 * @return a grid cell with coordinates of the given point in
	 * grid coordinates referred to this grid extent
	 */
	public GridCell getGridCoordsFromWorldCoords(Point2D pt){

		int x = (int)Math.floor((pt.getX() - m_dXMin) / m_dCellSizeX);
		int y = (int)Math.floor((m_dYMax - pt.getY()) / m_dCellSizeY);

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

		double x = m_dXMin + (cell.getX() + 0.5) * m_dCellSizeX;
		double y = m_dYMax - (cell.getY() + 0.5) * m_dCellSizeY;

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

	/**
	 * Convert World coordinates to grid coordinates
	 * @param coordinate the model Coordinate to convert to image Coordinate
	 * @return java.awt.Point
	 */
	 public  java.awt.Point getGridCoordsFromWorldCoords(Coordinate coordinate){
	     int x = (int)Math.floor((coordinate.x-m_dXMin) / m_dCellSizeX);
	    // int y = (int)Math.floor((m_Extent.getYMax()-coord.y) / CellSize);
	     int y = (int)Math.floor((coordinate.y-m_dYMin) / m_dCellSizeY);
	     return new java.awt.Point(x, y);
	     
	 }
	
	
	
        @Override
	public String toString(){

		String s = "" + m_dXMin + ", " + m_dYMin + ", "
									+ m_dXMax + ", " + m_dYMax + ", "
									+ m_dCellSizeX + ", " + m_dCellSizeY;

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

		m_dYMin = m_dYMin - m_dCellSizeY;
		m_dXMin = m_dXMin - m_dCellSizeX;
		m_dXMax = m_dXMax + m_dCellSizeX;
		m_dYMax = m_dYMax + m_dCellSizeY;
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
