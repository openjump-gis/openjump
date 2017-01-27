/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 */
 
package org.openjump.core.ui.plugin.tools;

import java.lang.Object;
import java.util.ArrayList;
import java.util.List;

import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VertexTaggedGeometryDataMapper;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import javax.swing.JCheckBox;


/**
 * Creates a Vorono&iuml; Dialgram using the new Triangulation API of JTS 1.12
 * @author Micha&euml;l Michaud
 */
public class VoronoiDiagramPlugIn extends AbstractThreadedUiPlugIn{

    //public static String VORONOI_DIAGRAM     = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.voronoi-diagram");
    public static String TRIANGULATE         = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.triangulate");
    public static String VORONOI             = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.voronoi");
    public static String SITES_LAYER         = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.sites-layer");
    public static String TRANSFER_ATTRIBUTES = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.transfer-attributes");
    public static String TOLERANCE           = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.tolerance");
    public static String DESCRIPTION         = I18N.get("org.openjump.core.ui.plugin.tools.VoronoiDiagramPlugIn.description");         

	String sitesLayer;
	boolean transferAttributes = false;
	double tolerance = 0.0;
    
    public void initialize(PlugInContext context) throws Exception {
    	    
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuItem(
	    	        this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
	                getName() + "...",
	                false,			//checkbox
	                IconLoader.icon("voronoi.png"),
	                createEnableCheck(context.getWorkbenchContext()));
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    
	public boolean execute(PlugInContext context) throws Exception{
	    this.reportNothingToUndoYet(context);
	        
 		MultiInputDialog dialog = new MultiInputDialog(
	            context.getWorkbenchFrame(), getName(), true);
	        setDialogValues(dialog, context);
	        GUIUtil.centreOnWindow(dialog);
	        dialog.setVisible(true);
	        if (! dialog.wasOKPressed()) { return false; }
	        getDialogValues(dialog);	    
	    return true;
	}
	
	public void setSitesLayer(String sitesLayer) {
	    this.sitesLayer = sitesLayer;
	}
	
	public void setTransferAttributes(boolean transferAttributes) {
	    this.transferAttributes = transferAttributes;
	}
	
	public void setTolerance(double tolerance) {
	    this.tolerance = tolerance;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
	    dialog.setSideBarDescription(DESCRIPTION);
	    if (sitesLayer == null || context.getLayerManager().getLayer(sitesLayer) == null) {
	        sitesLayer = context.getCandidateLayer(0).getName();
	    }
    	dialog.addLayerComboBox(SITES_LAYER, context.getLayerManager().getLayer(sitesLayer), null, context.getLayerManager());
    	dialog.addCheckBox(TRANSFER_ATTRIBUTES, transferAttributes);
    	dialog.addDoubleField(TOLERANCE, tolerance, 12);
    }

	private void getDialogValues(MultiInputDialog dialog) {
	    sitesLayer = dialog.getLayer(SITES_LAYER).getName();
	    transferAttributes = dialog.getBoolean(TRANSFER_ATTRIBUTES);
	    tolerance = dialog.getDouble(TOLERANCE);
    }
    
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.report(TRIANGULATE);
        LayerManager layerManager = context.getLayerManager();
        FeatureCollection inputFC = layerManager.getLayer(sitesLayer).getFeatureCollectionWrapper();
        Geometry sites = getSites(inputFC, transferAttributes);
        VertexTaggedGeometryDataMapper mapper = null;
        if (transferAttributes) {
            mapper = new VertexTaggedGeometryDataMapper();
            mapper.loadSourceGeometries(sites);
        }
        VoronoiDiagramBuilder voronoiBuilder = new VoronoiDiagramBuilder();
        voronoiBuilder.setSites(sites);
        voronoiBuilder.setTolerance(tolerance);
        Envelope env = null;
        if (context.getLayerViewPanel().getFence() != null) {
            env = context.getLayerViewPanel().getFence().getEnvelopeInternal();
        }
        else env = layerManager.getEnvelopeOfAllLayers();
        voronoiBuilder.setClipEnvelope(env);

        Geometry g = voronoiBuilder.getDiagram(new GeometryFactory());
        
        FeatureCollection result = null;
        if (transferAttributes && mapper != null) {
            mapper.transferData(g);
            FeatureSchema schema = inputFC.getFeatureSchema();
            result = new FeatureDataset(schema);
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry cellGeom = g.getGeometryN(i);
                Feature cell = ((Feature)cellGeom.getUserData()).clone(false);
                cellGeom.setUserData(null);
                cell.setGeometry(cellGeom);
                result.add(cell);
            }
        }
        else {
            List geometries = new ArrayList();
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                geometries.add(g.getGeometryN(i));
            }
            result = FeatureDatasetFactory.createFromGeometry(geometries);
        }
        context.getLayerManager().addLayer(StandardCategoryNames.RESULT, sitesLayer+"-"+VORONOI, result); 
    }
    
    private Geometry getSites(FeatureCollection fcSites, boolean transferAttributes) {
        List<Point> sites = new ArrayList<Point>();
        GeometryFactory gf = null;
        if (fcSites.isEmpty()) gf = new GeometryFactory();
        else {
            gf = ((Feature)fcSites.iterator().next()).getGeometry().getFactory();
            for (Object o : fcSites.getFeatures()) {
                addSite(((Feature)o).getGeometry(), sites, gf, o, transferAttributes);
            }
        }
        return gf.createMultiPoint(sites.toArray(new Point[sites.size()]));
    }
    
    private void addSite(Geometry g,
                         List<Point> sites, 
                         GeometryFactory gf,
                         Object o,
                         boolean transferAttributes) {
        if (g instanceof Point) {
            g.setUserData(o);
            sites.add((Point)g);
        }
        else {
            for (Coordinate c : g.getCoordinates()) {
                Point p = gf.createPoint(c);
                p.setUserData(o);
                sites.add(gf.createPoint(c));
            }
        }
    }
	
}
