/*******************************************************************************
    GridWrapperNotInterpolated
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

import org.openjump.core.rasterimage.sextante.ISextanteRasterLayer;

/**
 * A grid wrapper that does not perform interpolation to
 * calculate cell values. This should be used when the window
 * extent 'fits' into the structure (coordinates and cellsize)
 * of the grid, so it is faster than using a grid wrapper with
 * interpolation
 *
 * Upon construction, cellsizes are not checked, so they are assumed
 * to be equal. Use a QueryableGridWindow to safely create a GridWrapper
 * better than instantiating this class directly.
 *
 * @author Victor Olaya
 *
 */

public class GridWrapperNotInterpolated extends GridWrapper{


	public GridWrapperNotInterpolated(ISextanteRasterLayer layer,
			  						GridExtent windowExtent){

		super(layer, windowExtent);

		calculateOffsets();

	}

	protected void calculateOffsets(){

//		double dMinX, dMaxY;
//		int iWindowMinX, iWindowMinY;

		GridExtent layerExtent = m_Layer.getLayerGridExtent();

		m_iOffsetX = (int) ((m_WindowExtent.getXMin() - layerExtent.getXMin() )
				/ m_WindowExtent.getCellSize());
		m_iOffsetY = (int) ((layerExtent.getYMax() - m_WindowExtent.getYMax() )
				 / m_WindowExtent.getCellSize());

//		dMinX = Math.min(Math.max(m_WindowExtent.getXMin(), layerExtent.getXMin()), layerExtent.getXMax());
//		//dMinY = Math.min(Math.max(m_WindowExtent.getYMin(), layerExtent.getYMin()), layerExtent.getYMax());
//		dMaxY = Math.max(Math.min(m_WindowExtent.getYMax(), layerExtent.getYMax()), layerExtent.getYMin());
//
//		m_iMinX = (int) Math.floor((dMinX - layerExtent.getXMin()) / m_WindowExtent.getCellSize());
//		m_iMinY = (int) Math.floor((layerExtent.getYMax() - dMaxY) / m_WindowExtent.getCellSize());
//
//		m_iOffsetX = m_iMinX - iWindowMinX;
//		m_iOffsetY = m_iMinY - iWindowMinY;

	}


	public byte getCellValueAsByte(int x, int y) {

		return (byte) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, 0);

	}

	public byte getCellValueAsByte(int x, int y, int band) {

		return (byte) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, band);

	}

	public short getCellValueAsShort(int x, int y) {

		return (short) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, 0);

	}

	public short getCellValueAsShort(int x, int y, int band) {

		return (short) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, band);

	}

	public int getCellValueAsInt(int x, int y) {

		return (int) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, 0);

	}

	public int getCellValueAsInt(int x, int y, int band) {

		return (int) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, band);

	}

	public float getCellValueAsFloat(int x, int y) {

		return (float) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, 0);

	}

	public float getCellValueAsFloat(int x, int y, int band) {

		return (float) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, band);

	}

	public double getCellValueAsDouble(int x, int y) {

		return (double) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, 0);

	}

	public double getCellValueAsDouble(int x, int y, int band) {

		return (double) getCellValueInLayerCoords(x + m_iOffsetX, y +  m_iOffsetY, band);

	}

}
