/*******************************************************************************
    GridWrapper
    Copyright (C) Victor Olaya

    Interpolation routines taken from SAGA, by Olaf Conrad

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
 * Abstract class for grip wrappers. Grid wrappers are used
 * to get an easy way of accessing raster layers so different
 * layers (i.e. with different extents and cellsizes) can be combined
 * and analyzed together seamlessly, without having to worry about
 * resampling or adjusting them.
 *
 * @author Victor Olaya
 *
 */

public abstract class GridWrapper {

	public static final int INTERPOLATION_NearestNeighbour = 0;
	public static final int INTERPOLATION_Bilinear = 1;
	public static final int INTERPOLATION_InverseDistance = 2;
	public static final int INTERPOLATION_BicubicSpline = 3;
	public static final int INTERPOLATION_BSpline = 4;

	protected ISextanteRasterLayer m_Layer;

	//this offsets are in cells, not in map units.
	protected int m_iOffsetX;
	protected int m_iOffsetY;

	private int m_iInterpolationMethod = INTERPOLATION_BSpline;
	private double m_dCellSize; //cellsize of the layer, not the window

	protected GridExtent m_WindowExtent;

	//rows and columns of the window
	private int m_iNXLayer;
	private int m_iNYLayer;

	public GridWrapper(ISextanteRasterLayer layer,
					   GridExtent windowExtent){

		m_Layer = layer;
		m_WindowExtent = windowExtent;
		m_dCellSize = layer.getLayerCellSize();
		GridExtent layerExtent = layer.getLayerGridExtent();
		m_iNXLayer = layerExtent.getNX();
		m_iNYLayer = layerExtent.getNY();

	}

	public abstract byte getCellValueAsByte(int x, int y);
	public abstract byte getCellValueAsByte(int x, int y, int band);
	public abstract short getCellValueAsShort(int x, int y);
	public abstract short getCellValueAsShort(int x, int y, int band);
	public abstract int getCellValueAsInt(int x, int y);
	public abstract int getCellValueAsInt(int x, int y, int band);
	public abstract float getCellValueAsFloat(int x, int y);
	public abstract float getCellValueAsFloat(int x, int y, int band);
	public abstract double getCellValueAsDouble(int x, int y);
	public abstract double getCellValueAsDouble(int x, int y, int band);

	protected double getCellValueInLayerCoords(int x, int y, int band) {

		if (isInLayer(x, y, band)){
			return m_Layer.getCellValueInLayerCoords(x, y, band);
		}
		else{
			return getNoDataValue();
		}

	}

	public boolean isNoDataValue (double dValue){

		return (dValue == m_Layer.getNoDataValue());

	}

	public double getNoDataValue(){

		return m_Layer.getNoDataValue();

	}

	public int getNY() {

		return m_WindowExtent.getNY();

	}

	public int getNX() {

		return m_WindowExtent.getNX();

	}

	public double getCellSize(){

		return m_WindowExtent.getCellSize();

	}

	public GridExtent getGridExtent(){

		return m_WindowExtent;

	}

	protected boolean isInLayer(int x, int y, int iBand) {

		return x >= 0 && x < m_iNXLayer && y >= 0 && y < m_iNYLayer && iBand < m_Layer.getBandsCount();

	}

	public double getValueAt(double xPosition, double yPosition, int band){

		int	x, y;
		double	dx, dy;
		double dValue;

		x	= (int) Math.floor(xPosition = (xPosition - m_Layer.getLayerGridExtent().getXMin()) / m_dCellSize);
		y	= (int) Math.floor(yPosition = (m_Layer.getLayerGridExtent().getYMax() - yPosition ) / m_dCellSize);

		dValue = getCellValueInLayerCoords(x,y,band);

		if(	!isNoDataValue(dValue) ){

			dx	= xPosition - x;
			dy	= yPosition - y;

			switch( m_iInterpolationMethod ){
			case INTERPOLATION_NearestNeighbour:
				dValue = getValueNearestNeighbour (x, y, dx, dy, band);
				break;

			case INTERPOLATION_Bilinear:
				dValue	= getValueBiLinear (x, y, dx, dy, band);
				break;

			case INTERPOLATION_InverseDistance:
				dValue	= getValueInverseDistance(x, y, dx, dy, band);
				break;

			case INTERPOLATION_BicubicSpline:
				dValue	= getValueBiCubicSpline (x, y, dx, dy, band);
				break;

			case INTERPOLATION_BSpline:
				dValue	= getValueBSpline (x, y, dx, dy, band);
				break;
			}
		}
		else{
			dValue = getNoDataValue();
		}

		return dValue;
	}

	private double getValueNearestNeighbour(int x, int y, double dx, double dy, int band){

		x	+= (int)(0.5 + dx);
		y	+= (int)(0.5 + dy);

		return getCellValueInLayerCoords(x, y, band);

	}

	private  double getValueBiLinear(int x, int y, double dx, double dy, int band){

		double	z = 0.0, n = 0.0, d;
		double dValue;

		dValue = getCellValueInLayerCoords(x, y, band);
		if (!isNoDataValue(dValue)){
			 d = (1.0 - dx) * (1.0 - dy);
			 z += d * dValue;
			 n += d;
		}

		dValue = getCellValueInLayerCoords(x + 1, y, band);
		if (!isNoDataValue(dValue)){
			 d = (dx) * (1.0 - dy);
			 z += d * dValue;
			 n += d;
		}

		dValue = getCellValueInLayerCoords(x, y + 1, band);
		if (!isNoDataValue(dValue)){
			 d = (1.0 - dx) * (dy);
			 z += d * dValue;
			 n += d;
		}

		dValue = getCellValueInLayerCoords(x + 1, y + 1, band);
		if (!isNoDataValue(dValue)){
			 d = (dx) * (dy);
			 z += d * dValue;
			 n += d;
		}

		if( n > 0.0 ){
			return( z / n );
		}

		return( getNoDataValue() );
	}

	private double getValueInverseDistance(int x, int y, double dx, double dy, int band){

		double	z = 0.0, n = 0.0, d;
		double dValue;

		if( dx > 0.0 || dy > 0.0 ){

			dValue = getCellValueInLayerCoords(x, y, band);
			if (!isNoDataValue(dValue)){
				d = 1.0 / Math.sqrt(dx*dx + dy*dy);
				z += d * dValue;
				n += d;
			}

			dValue = getCellValueInLayerCoords(x + 1, y, band);
			if (!isNoDataValue(dValue)){
				d = 1.0 / Math.sqrt((1.0-dx)*(1.0-dx) + dy*dy);
				z += d * dValue;
				n += d;
			}

			dValue = getCellValueInLayerCoords(x, y + 1, band);
			if (!isNoDataValue(dValue)){
				d = 1.0 / Math.sqrt(dx*dx + (1.0-dy)*(1.0-dy));
				z += d * dValue;
				n += d;
			}

			dValue = getCellValueInLayerCoords(x + 1, y + 1, band);
			if (!isNoDataValue(dValue)){
				d = 1.0 / Math.sqrt((1.0-dx)*(1.0-dx) + (1.0-dy)*(1.0-dy));
				z += d * dValue;
				n += d;
			}

			if( n > 0.0 )
			{
				return( z / n );
			}
		}
		else{
			return getCellValueInLayerCoords(x, y, band) ;
		}

		return( getNoDataValue());
	}

	private double getValueBiCubicSpline(int x, int y, double dx, double dy, int band){

		int		i;
		double	a0, a2, a3, b1, b2, b3, c[], z_xy[][];

		c = new double[4];
		z_xy = new double[4][4];

		if( get4x4Submatrix(x, y, z_xy, band) ){

			for(i=0; i<4; i++){
				a0		= z_xy[0][i] - z_xy[1][i];
				a2		= z_xy[2][i] - z_xy[1][i];
				a3		= z_xy[3][i] - z_xy[1][i];

				b1		= -a0 / 3.0 + a2       - a3 / 6.0;
				b2		=  a0 / 2.0 + a2 / 2.0;
				b3		= -a0 / 6.0 - a2 / 2.0 + a3 / 6.0;

				c[i]	= z_xy[1][i] + b1 * dx + b2 * dx*dx + b3 * dx*dx*dx;
			}

			a0		= c[0] - c[1];
			a2		= c[2] - c[1];
			a3		= c[3] - c[1];

			b1		= -a0 / 3.0 + a2       - a3 / 6.0;
			b2		=  a0 / 2.0 + a2 / 2.0;
			b3		= -a0 / 6.0 - a2 / 2.0 + a3 / 6.0;

			return( c[1] + b1 * dy + b2 * dy*dy + b3 * dy*dy*dy );
		}

		return( getValueBiLinear(x, y, dx, dy, band) );
	}

	private double getValueBSpline(int x, int y, double dx, double dy, int band){

		int		i, ix, iy;
		double	z, px, py, Rx[], Ry[], z_xy[][];

		Rx = new double[4];
		Ry = new double[4];
		z_xy = new double [4][4];

		if( get4x4Submatrix(x, y, z_xy, band) ){
			for(i=0, px=-1.0-dx, py=-1.0-dy; i<4; i++, px++, py++){
				Rx[i]	= 0.0;
				Ry[i]	= 0.0;

				if( (z = px + 2.0) > 0.0 )
					Rx[i]	+=        z*z*z;
				if( (z = px + 1.0) > 0.0 )
					Rx[i]	+= -4.0 * z*z*z;
				if( (z = px + 0.0) > 0.0 )
					Rx[i]	+=  6.0 * z*z*z;
				if( (z = px - 1.0) > 0.0 )
					Rx[i]	+= -4.0 * z*z*z;
				if( (z = py + 2.0) > 0.0 )
					Ry[i]	+=        z*z*z;
				if( (z = py + 1.0) > 0.0 )
					Ry[i]	+= -4.0 * z*z*z;
				if( (z = py + 0.0) > 0.0 )
					Ry[i]	+=  6.0 * z*z*z;
				if( (z = py - 1.0) > 0.0 )
					Ry[i]	+= -4.0 * z*z*z;

				Rx[i]	/= 6.0;
				Ry[i]	/= 6.0;
			}

			for(iy=0, z=0.0; iy<4; iy++){
				for(ix=0; ix<4; ix++){
					z	+= z_xy[ix][iy] * Rx[ix] * Ry[iy];
				}
			}

			return( z );
		}

		return( getValueBiLinear(x, y, dx, dy, band) );
	}

	private boolean get4x4Submatrix(int x, int y, double z_xy[][], int band){

		int	ix, iy, px, py;
		double dValue;

		for(iy=0, py=y-1; iy<4; iy++, py++){
			for(ix=0, px=x-1; ix<4; ix++, px++){
				dValue = getCellValueInLayerCoords(px, py , band);
				if (isNoDataValue(dValue)){
					return false;
				}
				else{
					z_xy[ix][iy] = dValue;
				}
			}
		}

		return( true );
	}

	public void setInterpolationMethod(int iMethod){

		m_iInterpolationMethod = iMethod;

	}

}
