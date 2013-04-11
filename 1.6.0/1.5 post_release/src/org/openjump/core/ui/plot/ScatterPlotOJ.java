package org.openjump.core.ui.plot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.math.plot.plots.ScatterPlot;
import org.math.plot.render.AbstractDrawer;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomToSelectedItemsPlugIn;

public class ScatterPlotOJ extends ScatterPlot {

	double[][] XY;
    int[] pointId;
    PlugInContext context = null;
    Layer layer = null;
    
    private ZoomToSelectedItemsPlugIn zoomToSelectedItemsPlugIn = new ZoomToSelectedItemsPlugIn();
    
    /**
     * [sstein] constructor for use in O-JUMP
     * @param n
     * @param c
     * @param _XY
     * @param featureID
     */
    public ScatterPlotOJ(String n, Color c, double[][] _XY, int[] featureID, PlugInContext context, Layer layer) {
        super(n, c, AbstractDrawer.ROUND_DOT, AbstractDrawer.DEFAULT_DOT_RADIUS, _XY);
        this.pointId =  featureID;
        XY = _XY;
        this.context = context;
        this.layer = layer;
    }

	public double[] isSelected(int[] screenCoordTest, AbstractDrawer draw) {
		for (int i = 0; i < XY.length; i++) {
			int[] screenCoord = draw.project(XY[i]);

			if ((screenCoord[0] + note_precision > screenCoordTest[0]) && (screenCoord[0] - note_precision < screenCoordTest[0])
					&& (screenCoord[1] + note_precision > screenCoordTest[1]) && (screenCoord[1] - note_precision < screenCoordTest[1])){
                //System.out.println("fid of selected point: " + pointId[i]);
                List features = layer.getFeatureCollectionWrapper().getFeatures();
                Feature selFeature = null;
                for (Iterator iter = features.iterator(); iter.hasNext();) {
                    Feature f = (Feature) iter.next();
                    if ( pointId[i] == f.getID()){
                        selFeature = f;
                    }
                }
                //context.getLayerViewPanel().getSelectionManager().clear();               
                //context.getLayerViewPanel().getSelectionManager().getFeatureSelection().selectItems(layer, selFeature);
                Collection clickedFeatures = new ArrayList();
                clickedFeatures.add(selFeature);
                try{
                    zoomToSelectedItemsPlugIn.flash(
                            FeatureUtil.toGeometries(clickedFeatures), context.getLayerViewPanel());                }
                catch (Throwable t) {
                    context.getWorkbenchContext().getErrorHandler().handleThrowable(t);
                }

                //-- [sstein] modified to print FID as label
                //   but this results with problems in AbstractDrawer.drawCoordinate()
                //   since it will try to set the axes using the FID as well.
                /*
                double[] vals;
                if (pointId !=null){
                    vals = new double[3];
                    vals[0] = XY[i][0];
                    vals[1] = XY[i][1];                
                    vals[2] = pointId[i];
                }
                else{
                  vals = XY[i];                       
                }
                return vals;
                */
                return XY[i];
            }
		}
		return null;
	}
}