package org.openjump.core.rasterimage.sextante;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;
import org.openjump.core.rasterimage.sextante.rasterWrappers.GridExtent;

import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.model.LayerManager;

public class OpenJUMPSextanteRasterLayer extends AbstractSextanteRasterLayer{

	private static final double DEFAULT_NO_DATA_VALUE = -99999.;

	private String m_sFilename;
	private String m_sName ="";
	private GridExtent m_LayerExtent;
	private double m_dNoDataValue;
	private Raster m_Raster;

	public void create(RasterImageLayer layer){

		/* [sstein 26 Oct. 2010] - don't use code below because
		 * the raster data should be loaded new from file.
		 * It happened in tests that with the code below data from
		 * another raster, created last, was used instead. 
		 * (e.g. calculated Point-KDE first, and then Line-KDE=> then getting the polygon grid from
		 * the point-KDE raster delivered the Line-KDE raster as poly grid)
		 * 
		m_BaseDataObject = layer;
		//[sstein 2 Aug 2010], changed so we work now with the raster and not the image, which may be scaled for display. 
		//m_Raster = layer.getImage().getData();
		m_Raster = layer.getRasterData();
		//-- end
		m_sName = layer.getName();
		m_sFilename = layer.getImageFileName();
		Envelope env = layer.getEnvelope();
		m_LayerExtent = new GridExtent();
		m_LayerExtent.setXRange(env.getMinX(), env.getMaxX());
		m_LayerExtent.setYRange(env.getMinY(), env.getMaxY());
		m_LayerExtent.setCellSize((env.getMaxX() - env.getMinX())
							/ (double)m_Raster.getWidth());
		m_dNoDataValue = DEFAULT_NO_DATA_VALUE;
		*/
		
		//[sstein 26 Oct. 2010] using the new method instead
		// so I do not need to change the code in all the cases 
		// where #.create(layer) is used
		create(layer, true);
	}

	public void create(RasterImageLayer layer, boolean loadFromFile){

		if (loadFromFile == false){
			m_BaseDataObject = layer;
			//[sstein 2 Aug 2010], changed so we work now with the raster and not the image, which may be scaled for display. 
			//m_Raster = layer.getImage().getData();
			m_Raster = layer.getRasterData();
			//-- end
			m_sName = layer.getName();
			m_sFilename = layer.getImageFileName();
			Envelope env = layer.getEnvelope();
			m_LayerExtent = new GridExtent();
			m_LayerExtent.setXRange(env.getMinX(), env.getMaxX());
			m_LayerExtent.setYRange(env.getMinY(), env.getMaxY());
			m_LayerExtent.setCellSize((env.getMaxX() - env.getMinX())
								/ (double)m_Raster.getWidth());
			m_dNoDataValue = DEFAULT_NO_DATA_VALUE;
		}
		else{	
			RasterImageLayer rasterLayer = new RasterImageLayer(layer.getName(),
					layer.getLayerManager(),
					layer.getImageFileName(),
					null,
					null,
					layer.getEnvelope());
			m_BaseDataObject = rasterLayer;
			m_Raster = rasterLayer.getRasterData();
			//-- end
			m_sName = rasterLayer.getName();
			m_sFilename = rasterLayer.getImageFileName();
			Envelope env = rasterLayer.getEnvelope();
			m_LayerExtent = new GridExtent();
			m_LayerExtent.setXRange(env.getMinX(), env.getMaxX());
			m_LayerExtent.setYRange(env.getMinY(), env.getMaxY());
			m_LayerExtent.setCellSize((env.getMaxX() - env.getMinX())
								/ (double)m_Raster.getWidth());
			m_dNoDataValue = DEFAULT_NO_DATA_VALUE;
		}

	}
	
	public void create(String name, String filename, GridExtent ge,
			int dataType, int numBands, Object crs, LayerManager layerManager) {

		m_Raster = RasterFactory.createBandedRaster(dataType,
								ge.getNX(), ge.getNY(), numBands, null);

		Envelope envelope = new Envelope();
		envelope.init(ge.getXMin(), ge.getXMax(), ge.getYMin(), ge.getYMax());
		ColorModel colorModel = PlanarImage.createColorModel(m_Raster.getSampleModel());
		BufferedImage bufimg = new BufferedImage(colorModel, (WritableRaster) m_Raster, false, null);
		PlanarImage pimage = PlanarImage.wrapRenderedImage(bufimg);
		m_BaseDataObject = new RasterImageLayer(name,
												layerManager,
												filename,
												pimage,
												m_Raster,
												envelope);
		m_sName = name;
		m_sFilename = filename;
		m_LayerExtent = ge;
		m_dNoDataValue = DEFAULT_NO_DATA_VALUE;

	}

	public void fitToGridExtent(GridExtent ge, LayerManager layerManager) {

		WritableRaster raster = RasterFactory.createBandedRaster(m_Raster.getDataBuffer().getDataType(),
				ge.getNX(), ge.getNY(), m_Raster.getNumBands(), null);

		this.setWindowExtent(ge);
		for (int x = 0; x < ge.getNX(); x++) {
			for (int y = 0; y < ge.getNY(); y++) {
				for (int i = 0; i < getBandsCount(); i++) {
					raster.setSample(x, y, i, this.getCellValueAsDouble(x, y, i));
				}
			}
		}

		Envelope envelope = new Envelope();
		envelope.init(ge.getXMin(), ge.getXMax(), ge.getYMin(), ge.getYMax());
		ColorModel colorModel = PlanarImage.createColorModel(m_Raster.getSampleModel());
		BufferedImage bufimg = new BufferedImage(colorModel, (WritableRaster) m_Raster, false, null);
		PlanarImage pimage = PlanarImage.wrapRenderedImage(bufimg);
		RasterImageLayer imageLayer = new RasterImageLayer(m_sName,
				layerManager,
				m_sFilename,
				pimage,
				m_Raster,
				envelope);

		m_Raster = raster;
		m_BaseDataObject = imageLayer;
		m_LayerExtent = ge;

	}

	public int getBandsCount() {

		if (m_Raster != null){
			return m_Raster.getNumBands();
		}
		else{
			return 0;
		}

	}

	public double getCellValueInLayerCoords(int x, int y, int band) {

		if (m_Raster != null){
			return m_Raster.getSampleDouble(x, y, band);
		}
		else{
			return getNoDataValue();
		}

	}

	public int getDataType() {

		if (m_Raster != null){
			return m_Raster.getDataBuffer().getDataType();
		}
		else{
			return DataBuffer.TYPE_DOUBLE;
		}

	}

	public double getLayerCellSize() {

		if (m_LayerExtent != null){
			return m_LayerExtent.getCellSize();
		}
		else{
			return 0;
		}

	}

	public GridExtent getLayerGridExtent() {

		return m_LayerExtent;

	}

	public double getNoDataValue() {

		return m_dNoDataValue;

	}

	public void setCellValue(int x, int y, int band, double value) {

		if (m_Raster instanceof WritableRaster){
			if (this.getWindowGridExtent().containsCell(x, y)){
				((WritableRaster)m_Raster).setSample(x, y, band, value);
			}
		}

	}

	public void setNoDataValue(double noDataValue) {

		m_dNoDataValue = noDataValue;

	}

	public Object getCRS() {

		return null;

	}

	/**
	 * Returns the extent covered by the layer
	 * @return the extent of the layer
	 */
	public Rectangle2D getFullExtent() {

		if (m_BaseDataObject != null){
			RasterImageLayer layer = (RasterImageLayer) m_BaseDataObject;
			Envelope envelope = layer.getEnvelope();
			return new Rectangle2D.Double(envelope.getMinX(),
					  envelope.getMinY(),
					  envelope.getWidth(),
					  envelope.getHeight());
		}
		else{
			return null;
		}

	}

	public void open() {}

	public void close() {}

	public void postProcess() throws Exception{

		if (m_BaseDataObject != null){

			RasterImageLayer layer = (RasterImageLayer) m_BaseDataObject;

			FileOutputStream tifOut = new FileOutputStream(m_sFilename);
			TIFFEncodeParam param = new TIFFEncodeParam();
			param.setCompression(TIFFEncodeParam.COMPRESSION_NONE);
			TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec.createImageEncoder("tiff", tifOut, param);
			//-- [sstein 2 Aug 2010]
			//BufferedImage image = layer.getImage().getAsBufferedImage();
			ColorModel colorModel = PlanarImage.createColorModel(m_Raster.getSampleModel());
			BufferedImage image = new BufferedImage(colorModel, (WritableRaster) m_Raster, false, null);
			//-- end
			encoder.encode(image);
			tifOut.close();

			/* save geodata: */
			Envelope envelope = layer.getEnvelope();

			WorldFileHandler worldFileHandler = new WorldFileHandler(m_sFilename, false);
			worldFileHandler.writeWorldFile(envelope, image.getWidth(), image.getHeight());

			// Switch RAM mode of the RasterImage
			layer.setImageFileName(m_sFilename);
			layer.setNeedToKeepImage(false);


		}

	}

	public String getFilename() {

		return m_sFilename;

	}

	public String getName() {

		return m_sName;

	}

	public void setName(String sName) {

		m_sName = sName;

		if (m_BaseDataObject != null){
			RasterImageLayer layer = (RasterImageLayer) m_BaseDataObject;
			layer.setName(sName);
		}

	}

}
