/*******************************************************************************
    GridWrapperInterpolated
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
 * A grid wrapper that performs interpolation to calculate
 * cell values. This should be used when the window extent
 * does not 'fit' into the structure (coordinates and cellsize)
 * of the grid.
 *
 * @author Victor Olaya
 *
 */

public class GridWrapperInterpolated extends GridWrapper{


	public GridWrapperInterpolated(ISextanteRasterLayer layer,
			   					GridExtent windowExtent){

		super(layer, windowExtent);

	}


	public byte getCellValueAsByte(int x, int y) {

		return (byte) getCellValue(x, y, 0);

	}

	public byte getCellValueAsByte(int x, int y, int band) {

		return (byte) getCellValue(x, y, band);

	}

	public short getCellValueAsShort(int x, int y) {

		return (short) getCellValue(x, y, 0);

	}

	public short getCellValueAsShort(int x, int y, int band) {

		return (short) getCellValue(x, y, band);

	}

	public int getCellValueAsInt(int x, int y) {

		return (int) getCellValue(x, y, 0);

	}

	public int getCellValueAsInt(int x, int y, int band) {

		return (int) getCellValue(x, y, band);

	}

	public float getCellValueAsFloat(int x, int y) {

		return (float) getCellValue(x, y, 0);

	}

	public float getCellValueAsFloat(int x, int y, int band) {

		return (float) getCellValue(x, y, band);

	}

	public double getCellValueAsDouble(int x, int y) {

		return getCellValue(x, y, 0);

	}

	public double getCellValueAsDouble(int x, int y, int band) {

		return getCellValue(x, y, band);

	}

	private double getCellValue(int x, int y, int band){

		double dX = m_WindowExtent.getXMin() + m_WindowExtent.getCellSize() * (x + 0.5);
		double dY = m_WindowExtent.getYMax() - m_WindowExtent.getCellSize() * (y + 0.5);

		double dValue = getValueAt(dX, dY, band);

		return dValue;

	}


}
