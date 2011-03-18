package org.openjump.core.ui.plugin.raster.color;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import javax.media.jai.PlanarImage;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.color.ColorGenerator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.WorkbenchContext;

/**
 * @author plouy_p
 * 
 *  
 */
/**
 * 15 sept. 2005
 *
 * @author  Paul PLOUY
 * 			Laboratoire RESO - 	université de Rennes 2
 * 			FRANCE
 * 
 * 			modified by Stefan Steiniger (perriger@gmx.de)
 * 
 */
public class RasterColorEditor {

    private RasterImageLayer layer;
	private	ColorGenerator	colorGenerator;

    public RasterColorEditor(RasterImageLayer Rlayer) {
        setRasterLayer(Rlayer);
    }

    public void setRasterLayer(RasterImageLayer Rlayer) {
        this.layer = Rlayer;
    }

    public RasterImageLayer getRasterImageLayer() {
        return layer;
    }

    /*
     * This method apply a new set of Colors to the RasterLayer It has to read
     * the raster data, build new categories, create a new GridCoverage and
     * apply it to the RasterLayer
     */
    public void changeColors(WorkbenchContext context, Color[] colors,
            Color noDataColor, float min, float max) throws NoninvertibleTransformException {
    
        if (colors == null || colors.length == 0) {
            layer.setNeedToKeepImage(false);
            layer.flushImages(true);
            layer.setEnvelope(layer.getEnvelope());
            context.getLayerViewPanel().getViewport().update();
            return;
        }
    	colorGenerator = new ColorGenerator(35, colors);
        Raster raster = layer.getRasterData();
        
        /**
         * TODO: make the stuff below work. Not sure how, becasue
         * the three GeoTools classes have a lot of dependencies...
         * so one should use the geotools lib directly???
         */

        /*
        final String path;
        final Unit unit = null;
        final Category[] categories;
        final CoordinateReferenceSystem crs;
        final Rectangle2D bounds;

		
        Category[] categories = new Category[] {
                new Category("val1", color1, new NumberRange(1, 255),
                        new NumberRange(min, max)),
                new Category("val2", noDataColor, 0) };
	
        GridSampleDimension GSD = new GridSampleDimension(categories, null);
        GSD = GSD.geophysics(true);
		
        
        int width = image.getData().getWidth();
        int height = image.getData().getHeight();
        
        WritableRaster data = RasterFactory.createBandedRaster(
                java.awt.image.DataBuffer.TYPE_FLOAT, width, height, 1, null);
                WritableRaster oldData = (WritableRaster) image.getData();
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                data.setSample(j, i, 0, oldData.getSampleFloat(j, i, 0));
            }
        }
		*/
        //OpenJUMPSextanteRasterLayer ojraster = new OpenJUMPSextanteRasterLayer();
        //ojraster.create(layer);
        //double rasterMaxValue = ojraster.getMaxValue();
        //double rasterMinValue = ojraster.getMinValue();
        int width = raster.getWidth();
        int height = raster.getHeight();
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		int numOfSteps = colorGenerator.getSteps();
		
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                float value = raster.getSampleFloat(j, i, 0);
                
				if (value == Double.POSITIVE_INFINITY) {
					newImage.setRGB(j, i, Color.TRANSLUCENT);
				} else {
					int intColor = (int) ((value - min)
							/ (max - min) * (numOfSteps-1));

					/* black color indicates that value is out of the min/max area: */
					if (intColor >= numOfSteps || intColor < 0) {
						newImage.setRGB(j, i, Color.BLACK.getRGB());
					} else {
						Color newColor = colorGenerator.getColor(intColor);
						if (newColor==null) {
							//newImage.setRGB(j, i, Color.BLACK.getRGB());
						}
						
						newImage.setRGB(j, i, newColor.getRGB());
					}
				}

            }
        }
       
        /**
         * TODO: make this work
         */
        /*
        BufferedImage BufImage = new BufferedImage(GSD.getColorModel(), data,
                false, null);
          */
        
        PlanarImage pimage = PlanarImage.wrapRenderedImage(newImage);
        /*
        System.out.println("databuffer: " + newImage.getRaster().getDataBuffer()
                + "samplemodel: " + newImage.getRaster().getSampleModel());
		*/
		layer.setNeedToKeepImage(true);
        layer.setImage(pimage);
        layer.setEnvelope(layer.getEnvelope());
        context.getLayerViewPanel().getViewport().update();
    }

}
