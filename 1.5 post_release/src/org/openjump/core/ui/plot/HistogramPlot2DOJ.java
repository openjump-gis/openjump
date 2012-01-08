package org.openjump.core.ui.plot;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.math.plot.*;
import org.math.plot.plots.HistogramPlot2D;
import org.math.plot.render.*;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;

public class HistogramPlot2DOJ extends HistogramPlot2D {

	double[][] XY;

    double width = 0;
    String attrName = ""; 
    PlugInContext context = null;
    Layer layer = null;
    AttributeType type = null;
    private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn = new ZoomToSelectedItemsPlugIn();

    /**
     * 
     * @param n
     * @param c
     * @param _XY
     * @param w
     * @param context
     * @param layer
     * @param attrName
     */
	public HistogramPlot2DOJ(String n, Color c, double[][] _XY, double w, PlugInContext context, Layer layer, String attrName) {
		super(n, c, _XY, w, 0.5, 1);
        //double wcalc = Math.abs(_XY[1][0] - _XY[0][0]); 
        this.width = w;
        XY = _XY;
        this.context = context;
        this.layer = layer;
        this.attrName = attrName;
        FeatureSchema fs = layer.getFeatureCollectionWrapper().getFeatureSchema();
        type = fs.getAttributeType(attrName);
	}        

	public double[] isSelected(int[] screenCoordTest, AbstractDrawer draw) {
		for (int i = 0; i < XY.length; i++) {
			int[] screenCoord = draw.project(XY[i]);

			if ((screenCoord[0] + note_precision > screenCoordTest[0]) && (screenCoord[0] - note_precision < screenCoordTest[0])
					&& (screenCoord[1] + note_precision > screenCoordTest[1]) && (screenCoord[1] - note_precision < screenCoordTest[1])){
			    
                //----- get objects represented by selected Bar ----
                //- calc bounds
                double mean = XY[i][0];
                double lBound = mean - 0.5*this.width;
                double hBound = mean + 0.5*this.width;
                //- check which Object Values are inside range
                java.util.List features = layer.getFeatureCollectionWrapper().getFeatures();    
                Collection coveredFeatures = new ArrayList();
                for (Iterator iter = features.iterator(); iter.hasNext();) {
                    Feature f = (Feature) iter.next();
                    double dval = 0; 
                    Object val = f.getAttribute(this.attrName);
                    if (type == AttributeType.DOUBLE){
                        dval = ((Double)val).doubleValue();
                    }
                    else if (type == AttributeType.INTEGER){
                        dval = ((Integer)val).intValue();
                    }               
                    if(dval < hBound){
                        if(dval > lBound){
                            coveredFeatures.add(f);
                        }
                    }
                }
                //-- flash covered Features
                try{
                    zoomToSelectedItemsPlugIn.flash(
                            FeatureUtil.toGeometries(coveredFeatures), context.getLayerViewPanel());                }
                catch (Throwable t) {
                    context.getWorkbenchContext().getErrorHandler().handleThrowable(t);
                }
                
                return XY[i];
            }
		}
		return null;
	}

}