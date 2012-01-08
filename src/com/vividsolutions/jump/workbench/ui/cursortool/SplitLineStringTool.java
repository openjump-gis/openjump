package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.snap.SnapManager;

public class SplitLineStringTool extends AbstractClickSelectedLineStringsTool {

	Coordinate snapCoord = null;		
	
    public String getName() {
        return I18N.get("com.vividsolutions.jump.workbench.ui.cursortool.SplitLineStringTool.Split-LinesStrings");
    }
    
	protected void gestureFinished(Collection nearbyLineStringFeatures)
			throws NoninvertibleTransformException {
		Feature closestFeature = closest(nearbyLineStringFeatures,
				getModelClickPoint());
		LineString lineString = (LineString) closestFeature.getGeometry();
		if (CollectionUtil.list(lineString.getStartPoint().getCoordinate(),
				lineString.getEndPoint().getCoordinate()).contains(
				DistanceOp.closestPoints(lineString, getModelClickPoint())[0])) {
			getWorkbench().getFrame().warnUser(
					NO_SELECTED_LINESTRINGS_HERE_MESSAGE);
			return;
		}
		if (!layer(closestFeature, layerToSpecifiedFeaturesMap()).isEditable()) {
			warnLayerNotEditable(layer(closestFeature,
					layerToSpecifiedFeaturesMap()));
			return;
		}
		//-- [sstein] replaced Model destination by point to snap
		if (this.snapCoord != null){
			split(closestFeature, this.snapCoord, layer(closestFeature,
					layerToSpecifiedFeaturesMap()));
			}
		else{
			split(closestFeature, getModelDestination(), layer(closestFeature,
					layerToSpecifiedFeaturesMap()));
		}		
	}

	private void split(Feature feature, Coordinate coordinate, Layer layer) {
			new SplitLineStringsOp(Color.blue).addSplit(feature, coordinate, layer,
					false).execute(getName(), isRollingBackInvalidEdits(),
					getPanel());
	}

	private Feature closest(Collection features, Point point) {
		Feature closestFeature = null;
		this.snapCoord = null;
		//-- [sstein: 20.11.2005] : make snap working
		double tol = SnapManager.getToleranceInPixels(this.getWorkbench().getBlackboard()) / this.getPanel().getViewport().getScale();
		//double closestDistance = Double.MAX_VALUE;
		double closestDistance = tol;
		for (Iterator i = features.iterator(); i.hasNext();) {
			Feature feature = (Feature) i.next();
			double distance = feature.getGeometry().distance(point);
			if (distance < closestDistance) {
				closestFeature = feature; 				
				closestDistance = distance;
			}
		}
		//-- [sstein]: get snap point
		this.snapCoord = this.getVertexToSnap(closestFeature.getGeometry(), point, tol); 
		return closestFeature;
	}

	public Icon getIcon() {
		return IconLoader.icon("SplitLinestring.gif");
	}
	
	//[sstein: 20.11.2005] added
	private Coordinate getVertexToSnap(Geometry g, Point p, double tolerance){
		Coordinate coord = null;
		if (g instanceof LineString){
			LineString ls = (LineString)g;
			double minDist = tolerance;			
			for(int i=0; i < ls.getNumPoints(); i++){
				Point pT = ls.getPointN(i);
				double dist = pT.distance(p);
				if (dist < minDist){
					minDist = dist;
					coord = (Coordinate)pT.getCoordinate().clone();
				}
			}
		}
		return coord;
	}
}