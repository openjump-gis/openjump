/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package org.openjump.core.ui.plugin.edittoolbox.cursortools;

import com.vividsolutions.jts.algorithm.MCPointInRing;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.FeatureDrawingUtil;
import com.vividsolutions.jump.workbench.ui.cursortool.NClickTool;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanelProxy;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.WorkbenchContext;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.GridLayout;
import java.awt.Shape;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FillPolygonTool extends NClickTool {

    public static final String AREA_NOT_CLOSED = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.clicked-area-is-not-closed");
    public static final String EXTEND_SEARCH   = I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool.do-you-want-to-extend-search-out-of-the-view");
    
	private FeatureDrawingUtil featureDrawingUtil;
	private WorkbenchContext context;

	public FillPolygonTool(WorkbenchContext context) {
		super(1);
		featureDrawingUtil = new FeatureDrawingUtil(
		    (LayerNamePanelProxy)context.getLayerNamePanel()
		);
		this.context = context;
	}
	
	protected Shape getShape() throws NoninvertibleTransformException {
		//Don't want anything to show up when the user drags. [Jon Aquino]
		return null;
	}

	public Icon getIcon() {
		return new ImageIcon(getClass().getResource("FillPolygon.gif"));
	}
	
	public String getName(){
	    return I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool");
	}

	protected void gestureFinished() throws Exception {
		reportNothingToUndoYet();
        Polygon polygon;;
        if (null != (polygon = getPolygon(true))) {
		    execute(featureDrawingUtil.createAddCommand(
				    polygon, isRollingBackInvalidEdits(), getPanel(), this));
        } else {
            JPanel panel = new JPanel(new GridLayout(2,1));
            panel.add(new JLabel(AREA_NOT_CLOSED));
            panel.add(new JLabel(EXTEND_SEARCH));
            OKCancelDialog dialog = new OKCancelDialog(
                context.getWorkbench().getFrame(),
                I18N.get("org.openjump.core.ui.plugin.edittoolbox.cursortools.FillPolygonTool"), 
                true, panel, null);
            GUIUtil.centreOnWindow(dialog);
            dialog.setVisible(true);
            if (dialog.wasOKPressed() && (null != (polygon = getPolygon(false)))) {
                execute(
			    featureDrawingUtil.createAddCommand(
				    polygon, isRollingBackInvalidEdits(), getPanel(), this));
            } else {
                context.getWorkbench().getFrame().warnUser(AREA_NOT_CLOSED);
            }
        }
	}

	protected Polygon getPolygon(boolean inViewportOnly)
		throws NoninvertibleTransformException {
		Polygonizer polygonizer = new Polygonizer();
		polygonizer.add(getVisibleGeometries(inViewportOnly));
		Collection polys = polygonizer.getPolygons();
		//System.out.println("polys:" + polys);
		Coordinate c = (Coordinate)getCoordinates().get(0);
		for (Object poly : polys) {
		    if (new MCPointInRing((LinearRing)((Polygon)poly).getExteriorRing()).isInside(c)) {
		        return (Polygon)poly;
		    }
		}
		return null;
    }
    
    private Set<Geometry> getVisibleGeometries(boolean inViewportOnly) {
        List layers = context.getLayerManager().getVisibleLayers(false);
        Envelope env = null;
        Set<Geometry> list = new HashSet<Geometry>();
        if (inViewportOnly) {
            env = context.getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
        }
        else {
            env = new Envelope();
            for (Object layer : layers) {
                env.expandToInclude(((Layer)layer).getFeatureCollectionWrapper().getEnvelope());
            }
        }
        for (Object layer : layers) {
            Collection features = ((Layer)layer).getFeatureCollectionWrapper().getFeatures();
            for (Object f : features) {
                Geometry geom = ((Feature)f).getGeometry();
                if (geom.getEnvelopeInternal().intersects(env) && geom.getDimension() > 0) {
                    extractLinearComponents(geom, list);
                }
            }
        }
        //System.out.println("geom:" + list);
        return list;
    }
    
    private void extractLinearComponents(Geometry geom, Set<Geometry> linearComponents) {
        for (int i = 0 ; i < geom.getNumGeometries() ; i++) {
            Geometry g = geom.getGeometryN(i);
            if (g instanceof Polygon) {
                extractLinearComponents((Polygon)g, linearComponents);
            }
            else if (g instanceof LineString) {
                extractLinearComponents((LineString)g, linearComponents);
            }
            else if (g instanceof Point) {
            }
            else extractLinearComponents(g, linearComponents);
        }
    }
    
    private void extractLinearComponents(Polygon poly, Set<Geometry> linearComponents) {
        extractLinearComponents((LineString)poly.getExteriorRing(), linearComponents);
        for (int i = 0 ; i < poly.getNumInteriorRing() ; i++) {
            extractLinearComponents((LineString)poly.getInteriorRingN(i), linearComponents);
        }
    }
    
    private void extractLinearComponents(LineString line, Set<Geometry> linearComponents) {
        Coordinate[] cc = line.getCoordinates();
        for (int i = 1 ; i < cc.length ; i++) {
            LineString ls = line.getFactory().createLineString(new Coordinate[]{cc[i-1], cc[i]});
            ls.normalize();
            linearComponents.add(ls);
        }
    }
    
}
