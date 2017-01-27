package org.openjump.core.rasterimage.sextante;

import java.util.Arrays;

import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapper;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperInterpolated;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridWrapperNotInterpolated;



/**
 * A convenience class which implements some of the methods
 * of the IRasterLayer interface. Extending this class is recommended
 * instead of implementing the interface directly
 * @author volaya
 *
 */
public abstract class AbstractSextanteRasterLayer implements ISextanteRasterLayer {

	private final static int m_iOffsetX []= {0,  1,  1,  1,  0, -1, -1, -1};
	private final static int m_iOffsetY []= {1,  1,  0, -1, -1, -1,  0,  1};

	private final static double DEG_90_IN_RAD = Math.PI / 180. * 90.;
	private final static double DEG_180_IN_RAD = Math.PI ;
	private final static double DEG_270_IN_RAD = Math.PI / 180. * 270.;

	private GridWrapper m_GridWrapper;

	private double m_dDist[];

	private double _2DX;

	private int[][] m_Histogram;
	private double[] m_dMax;
	private double[] m_dMin;
	private double[] m_dMean;
	private double[] m_dVariance;
	private boolean m_bStatisticsCalculated = false;
	private boolean m_bHistogramCalculated;

	protected Object m_BaseDataObject;

	public Object getBaseDataObject(){

		return m_BaseDataObject;

	}

	public void setInterpolationMethod(int iMethod){

		m_GridWrapper.setInterpolationMethod(iMethod);

	}

	public byte getCellValueAsByte(int x, int y) {

		return m_GridWrapper.getCellValueAsByte(x,y);

	}

	public byte getCellValueAsByte(int x, int y, int band) {

		return m_GridWrapper.getCellValueAsByte(x,y,band);

	}

	public short getCellValueAsShort(int x, int y) {

		return m_GridWrapper.getCellValueAsShort(x,y);

	}

	public short getCellValueAsShort(int x, int y, int band) {

		return m_GridWrapper.getCellValueAsShort(x,y,band);

	}

	public int getCellValueAsInt(int x, int y) {

		return m_GridWrapper.getCellValueAsInt(x,y);

	}

	public int getCellValueAsInt(int x, int y, int band) {

		return m_GridWrapper.getCellValueAsInt(x,y,band);

	}

	public float getCellValueAsFloat(int x, int y) {

		return m_GridWrapper.getCellValueAsFloat(x,y);

	}

	public float getCellValueAsFloat(int x, int y, int band) {

		return m_GridWrapper.getCellValueAsFloat(x,y,band);

	}

	public double getCellValueAsDouble(int x, int y) {

		return m_GridWrapper.getCellValueAsDouble(x,y);

	}

	public double getCellValueAsDouble(int x, int y, int band) {

		return m_GridWrapper.getCellValueAsDouble(x,y,band);

	}

	public double getValueAt(double x, double y){

		return m_GridWrapper.getValueAt(x, y, 0);

	}
	public double getValueAt(double x, double y, int band){

		return m_GridWrapper.getValueAt(x, y, band);

	}

	public boolean isNoDataValue(double dValue){

		return dValue == getNoDataValue();

	}

	public boolean isInWindow(int x, int y){

		if (x < 0 || y < 0)
			return false;

		if (x >= m_GridWrapper.getNX() || y >= m_GridWrapper.getNY())
			return false;

		return true;

	}

	public int getNX(){

		return m_GridWrapper.getNX();

	}

	public int getNY(){

		return m_GridWrapper.getNY();

	}

	public double getWindowCellSize(){

		return m_GridWrapper.getCellSize();

	}

	public GridExtent getWindowGridExtent(){

		return m_GridWrapper.getGridExtent();

	}

	public void assign(double dValue){

		int iBand;
		int x,y;

		for (iBand = 0; iBand < this.getBandsCount(); iBand++){
			for (x = 0; x < getNX(); x++){
				for (y = 0; y < getNY(); y++){
					setCellValue(x,y, iBand, dValue);
				}
			}
		}

	}

	public void assign(ISextanteRasterLayer layer){

		double dValue;

		layer.setWindowExtent(getWindowGridExtent());

		int iNX = layer.getNX();
		int iNY = layer.getNY();

		for (int x = 0; x < iNX; x++){
			for (int y = 0; y < iNY; y++){
				dValue = layer.getCellValueAsDouble(x, y);
				setCellValue(x, y, dValue);
			}
		}

		setNoDataValue(layer.getNoDataValue());

	}


	public void add(ISextanteRasterLayer driver){

		double dValue;

		if (driver.getWindowGridExtent().equals(getWindowGridExtent())){
			for (int x = 0; x < getWindowGridExtent().getNX(); x++){
				for (int y = 0; y < getWindowGridExtent().getNY(); y++){
					dValue = driver.getCellValueAsDouble(x, y)
							+ getCellValueAsDouble(x, y);
					setCellValue(x, y, dValue);
				}
			}
			setNoDataValue(driver.getNoDataValue());
		}

	}

	public void assignNoData() {

		assign(getNoDataValue());

	}

	public void setCellValue(int x, int y, double dValue) {

		setCellValue(x, y, 0, dValue);

	}

	public void setNoData(int x, int y){

		setCellValue(x, y, getNoDataValue());

	}

	public void setNoData(int x, int y, int iBand){

		setCellValue(x, y, iBand, getNoDataValue());

	}

	public void addToCellValue(int x, int y, int iBand, double dValue){

		double dCellValue = getCellValueAsDouble(x, y, iBand);

		if (!isNoDataValue(dCellValue)){
			dCellValue += dValue;
			setCellValue(x, y, iBand, dCellValue);
		}

	}

	public void addToCellValue(int x, int y, double dValue){

		addToCellValue(x, y, 0, dValue);

	}

	public void multiply(double dValue){

		int iBand;
		int x,y;

		for (iBand = 0; iBand < this.getBandsCount(); iBand++){
			for (x = 0; x < getNX(); x++){
				for (y = 0; y < getNY(); y++){
					double dVal = getCellValueAsDouble(x, y, iBand);
					setCellValue(x,y, iBand, dValue * dVal);
				}
			}
		}

	}


	public void setWindowExtent(ISextanteRasterLayer layer){

		GridExtent layerExtent = new GridExtent(layer);

		if (layerExtent.fitsIn(this.getLayerGridExtent())){
			m_GridWrapper = new GridWrapperNotInterpolated(this, layerExtent);
		}
		else{
			m_GridWrapper = new GridWrapperInterpolated(this, layerExtent);
		}

		setConstants();

	}

	public void setWindowExtent(GridExtent extent){


		if (extent.fitsIn(this.getLayerGridExtent())){
			m_GridWrapper = new GridWrapperNotInterpolated(this, extent);
		}
		else{
			m_GridWrapper = new GridWrapperInterpolated(this, extent);
		}

		setConstants();

	}

	public void setFullExtent(){

		m_GridWrapper = new GridWrapperNotInterpolated(this, getLayerGridExtent());

		setConstants();

	}

	///////////////////////////////////Statistical stuff//////////////////////

	private void setConstants(){

		int i;
		double dCellSize = getWindowCellSize();

		m_dDist = new double[8];

	    for (i = 0; i < 8; i++){
	        m_dDist[i] = Math.sqrt ( m_iOffsetX[i] * dCellSize * m_iOffsetX[i] * dCellSize
	                        + m_iOffsetY[i] * dCellSize * m_iOffsetY[i] * dCellSize );
	    }

	    _2DX = dCellSize * 2;

	}

	private void calculateStatistics(){


		int x, y;
		double z;
		int iValues;

		int iBands = getBandsCount();

		m_dMean = new double[iBands];
		m_dVariance = new double[iBands];
		m_dMin = new double[iBands];
		m_dMax = new double[iBands];

		for (int i = 0; i < this.getBandsCount(); i++) {
			m_dMean[i]	= 0.0;
			m_dVariance[i]	= 0.0;
		}

		if (m_GridWrapper == null){
			this.setFullExtent();
		}

		for (int i = 0; i < this.getBandsCount(); i++) {
			iValues	= 0;
			for (y = 0; y < getNY(); y++){
				for (x = 0; x < getNX(); x++){
					z = getCellValueAsDouble(x,y,i);
					if( !isNoDataValue(z))	{
						if( iValues == 0 ){
							m_dMin[i] = m_dMax[i] = z;
						}
						else if( m_dMin[i] > z ){
							m_dMin[i]	= z;
						}
						else if( m_dMax[i] < z ){
							m_dMax[i]	= z;
						}

						m_dMean[i]	+= z;
						m_dVariance[i] += z * z;
						iValues++;
					}
				}
			}

			if( iValues > 0 ){
				m_dMean[i]	/= (double) iValues;
				m_dVariance[i]	= m_dVariance[i] / (double) iValues - m_dMean[i] * m_dMean[i];
			}
		}

		m_bStatisticsCalculated = true;

	}

	private void calculateHistogram(){

		int x,y;
		int iClass;
		double dValue;
		double dRange;

		if (!m_bStatisticsCalculated){
			calculateStatistics();
		}

		int iBands = getBandsCount();

		m_Histogram = new int [iBands][256];

		Arrays.fill(m_Histogram,0);

		for (int i = 0; i < iBands; i++) {
			dRange = m_dMax[i] - m_dMin[i];
			for (y = 0; y < getNY(); y++){
				for (x = 0; x < getNX(); x++){
					dValue = getCellValueAsDouble(x,y,i);
					if( !isNoDataValue(dValue))	{
						iClass = (int) ((dValue - m_dMin[i]) / dRange * 255.);
						m_Histogram[i][iClass]++;
					}
				}
			}
		}

		m_bHistogramCalculated = true;


	}

	public int[] getHistogram(int iBand){

		if (!m_bHistogramCalculated){
			calculateHistogram();
		}

		return m_Histogram[iBand];

	}

	public int[] getHistogram(){

		return getHistogram(0);

	}

	public int[] getAccumulatedHistogram(int iBand){

		int [] accHistogram = new int[256];

		Arrays.fill(accHistogram, 0);

		if (!m_bHistogramCalculated){
			calculateHistogram();
		}

		for (int i = 1; i < 256; i++) {
			accHistogram[i] = m_Histogram[iBand][i] + accHistogram[i-1];
		}

		return accHistogram;

	}

	public int[] getAccumulatedHistogram(){

		return getAccumulatedHistogram(0);

	}

	public double getMinValue(int iBand){

		if (!m_bStatisticsCalculated){
			calculateStatistics();
		}

		return m_dMin[iBand];

	}

	public double getMaxValue(int iBand){

		if (!m_bStatisticsCalculated){
			calculateStatistics();
		}

		return m_dMax[iBand];

	}

	public double getMeanValue(int iBand){

		if (!m_bStatisticsCalculated){
			calculateStatistics();
		}

		return m_dMean[iBand];

	}

	public double getVariance(int iBand){

		if (!m_bStatisticsCalculated){
			calculateStatistics();
		}

		return m_dVariance[iBand];

	}


	public double getMeanValue(){

		return getMeanValue(0);

	}

	public double getMinValue(){

		return getMinValue(0);

	}

	public double getMaxValue(){

		return getMaxValue(0);

	}

	public double getVariance(){

		return getVariance(0);

	}


	//////////////////////////////Additional methods for DEM analysis//////


	private boolean getSubMatrix3x3(int x, int y, double SubMatrix[]){

		int	i;
		int iDir;
		double	z, z2;

		z = getCellValueAsDouble(x, y);

		if(isNoDataValue(z)){
			return false;
		}
		else{
			//SubMatrix[4]	= 0.0;
			for(i=0; i<4; i++){

				iDir = 2 * i;
				z2 = getCellValueAsDouble(x + m_iOffsetX[iDir], y + m_iOffsetY[iDir]);
				if( !isNoDataValue(z2)){
					SubMatrix[i]	=  z2 - z;
				}
				else{
					z2 = getCellValueAsDouble(x + m_iOffsetX[(iDir + 4) % 8], y + m_iOffsetY[(iDir  + 4) % 8]);
					if( !isNoDataValue(z2)){
						SubMatrix[i]	= z - z2;
					}
					else{
						SubMatrix[i]	= 0.0;
					}
				}
			}

			return true;
		}

	}

	public double getSlope(int x, int y){

		double	zm[], G, H;

		zm = new double[4];

		if( getSubMatrix3x3(x, y, zm) ){
			G	=  (zm[0] - zm[2]) / _2DX;
	        H	=  (zm[1] - zm[3]) / _2DX;
	        return Math.atan(Math.sqrt(G*G + H*H));
		}
		else{
			return m_GridWrapper.getNoDataValue();
		}
	}

	public double getAspect(int x, int y){

		double	zm[], G, H, dAspect;

		zm = new double[4];

		if( getSubMatrix3x3(x, y, zm) ){
			G	=  (zm[0] - zm[2]) / _2DX;
	        H	=  (zm[1] - zm[3]) / _2DX;
			if( G != 0.0 ){
				dAspect = DEG_180_IN_RAD + Math.atan2(H, G);
			}
			else{
				dAspect = H > 0.0 ? DEG_270_IN_RAD : (H < 0.0 ? DEG_90_IN_RAD : -1.0);
			}
			return dAspect;
		}
		else{
			return m_GridWrapper.getNoDataValue();
		}
	}

	public double getDistToNeighborInDir(int iDir){

		return m_dDist[iDir];

	}

	public static double getUnitDistToNeighborInDir(int iDir){

		return( (iDir % 2 != 0) ? Math.sqrt(2.0)  : 1.0 );

	}

	public int getDirToNextDownslopeCell(int x, int y){

		return getDirToNextDownslopeCell(x, y, true);

	}

	public int getDirToNextDownslopeCell(int x, int y, boolean bForceDirToNoDataCell){

		int		i, iDir;
		double	z, z2, dSlope, dMaxSlope;

		z = getCellValueAsDouble(x, y);

		if(isNoDataValue(z)){
			return -1;
		}

		dMaxSlope = 0.0;
		for(iDir=-1, i=0; i<8; i++){
			z2 = getCellValueAsDouble(x + m_iOffsetX[i], y + m_iOffsetY[i]);
			if(isNoDataValue(z2)){
				if (bForceDirToNoDataCell){
					return i;
				}
				else{
					return -1;
				}
			}
			else{
				dSlope	= (z - z2) / getDistToNeighborInDir(i);
				if( dSlope > dMaxSlope ){
					iDir = i;
					dMaxSlope = dSlope;
				}
			}
		}

		return iDir;

	}

	public String toString(){

		return this.getName();

	}

}
