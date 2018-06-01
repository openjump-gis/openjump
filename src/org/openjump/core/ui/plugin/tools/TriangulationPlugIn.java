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

import java.util.ArrayList;
import java.util.List;

import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder;
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;


/**
 * Creates a triangulation using the new Triangulation API of JTS 1.12
 * @author Micha&euml;l Michaud
 */
public class TriangulationPlugIn extends AbstractThreadedUiPlugIn{

    //public static String TRIANGULATION     = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.triangulation");
    public static String TRIANGULATE       = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.triangulate");
    public static String TRIANGULATED      = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.triangulated");
    public static String SITES_LAYER       = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.sites-layer");
    public static String CONSTRAINTS_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.constraints-layer");
    public static String INTERIOR_ONLY     = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.polygon-interior-only");
    public static String TOLERANCE         = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.tolerance");
    public static String DESCRIPTION       = I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.description");
    public static Layer NO_CONSTRAINT      = new Layer(I18N.get("org.openjump.core.ui.plugin.tools.TriangulationPlugIn.no-constraint"),  
                                                Color.BLACK, new FeatureDataset(new FeatureSchema()), new LayerManager());

	String sitesLayer;
	String constraintsLayer = null;
	boolean polygonInteriorOnly = false;
	double tolerance = 0.0;
    
    public void initialize(PlugInContext context) throws Exception {
    	    
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuPlugin(
	    	        this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
	                getName() + "...",
	                false,			//checkbox
	                IconLoader.icon("triangulation.png"),
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
	
	public void setConstraintsLayer(String constraintsLayer) {
	    this.constraintsLayer = constraintsLayer;
	}
	
	public void setPolygonInteriorOnly(boolean polygonInteriorOnly) {
	    this.polygonInteriorOnly = polygonInteriorOnly;
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
    	final JComboBox jcbConstraint = dialog.addLayerComboBox(CONSTRAINTS_LAYER, NO_CONSTRAINT, null, getConstraintCandidateLayers(context));
    	final JCheckBox jcbInteriordialog = dialog.addCheckBox(INTERIOR_ONLY, polygonInteriorOnly);
    	jcbInteriordialog.setEnabled(jcbConstraint.getSelectedItem() != NO_CONSTRAINT);
    	jcbConstraint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jcbInteriordialog.setEnabled(jcbConstraint.getSelectedItem() != NO_CONSTRAINT) ;
            }
        });
    	dialog.addDoubleField(TOLERANCE, tolerance, 12);
    }

	private void getDialogValues(MultiInputDialog dialog) {
	    sitesLayer = dialog.getLayer(SITES_LAYER).getName();
	    constraintsLayer = dialog.getLayer(CONSTRAINTS_LAYER) != NO_CONSTRAINT ?
	                       dialog.getLayer(CONSTRAINTS_LAYER).getName() : null;
	    polygonInteriorOnly = dialog.getBoolean(INTERIOR_ONLY);
	    tolerance = dialog.getDouble(TOLERANCE);
    }
	
    private List<Layer> getConstraintCandidateLayers(PlugInContext context) {
        Layer[] layers = context.getLayerManager().getLayers().toArray(new Layer[0]);
        List<Layer> linearLayers = new ArrayList<>();
        linearLayers.add(NO_CONSTRAINT);
        for (Layer layer : layers) {
            FeatureCollection fc = layer.getFeatureCollectionWrapper();
            if (!fc.isEmpty()) {
                if ((fc.iterator().next()).getGeometry().getDimension() > 0) {
                    linearLayers.add(layer);
                }
            } 
        }
        return linearLayers;
    }
    
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.report(TRIANGULATE);
        Geometry sites = getSites(context
                .getLayerManager().getLayer(sitesLayer).getFeatureCollectionWrapper());
        ConformingDelaunayTriangulationBuilder triangulationBuilder = new ConformingDelaunayTriangulationBuilder();
        triangulationBuilder.setSites(sites);
        triangulationBuilder.setTolerance(tolerance);
        if (constraintsLayer != null) {
            triangulationBuilder.setConstraints(getConstraints(context
                .getLayerManager().getLayer(constraintsLayer).getFeatureCollectionWrapper()));
        }
        Geometry g = triangulationBuilder.getTriangles(new GeometryFactory());
        List<Geometry> geometries = new ArrayList<>();
        for (int i = 0 ; i < g.getNumGeometries() ; i++) {
            geometries.add(g.getGeometryN(i));
        }
        if (polygonInteriorOnly && constraintsLayer != null) {
            geometries = removeTrianglesOutOfPolygons(geometries, 
                getPolygons(context.getLayerManager().getLayer(constraintsLayer).getFeatureCollectionWrapper()));
        }
        FeatureCollection result = FeatureDatasetFactory.createFromGeometry(geometries);
        context.getLayerManager().addLayer(StandardCategoryNames.RESULT, sitesLayer+"-"+TRIANGULATED, result); 
    }
    
    private List<Geometry> removeTrianglesOutOfPolygons(List<Geometry> geometries, List<Polygon> polygons) {
        STRtree index = new STRtree();
        List<Geometry> result = new ArrayList<>();
        for (Polygon p : polygons) {
            index.insert(p.getEnvelopeInternal(), p);
        }
        for (Geometry g : geometries) {
            Point p = g.getInteriorPoint();
            List candidates = index.query(p.getEnvelopeInternal());
            for (Object o : candidates) {
                if (((Geometry)o).contains(p)) {
                    result.add(g);
                    break;
                }
            }
        }
        return result;
    }
    
    private Geometry getSites(FeatureCollection fcSites) {
        List<Point> sites = new ArrayList<>();
        GeometryFactory gf;
        if (fcSites.isEmpty()) gf = new GeometryFactory();
        else {
            gf = (fcSites.iterator().next()).getGeometry().getFactory();
            for (Object o : fcSites.getFeatures()) {
                addSite(((Feature)o).getGeometry(), sites, gf);
            }
        }
        return gf.createMultiPoint(sites.toArray(new Point[sites.size()]));
    }
    
    private void addSite(Geometry g, List<Point> sites, GeometryFactory gf) {
        if (g instanceof Point) sites.add((Point)g);
        else {
            for (Coordinate c : g.getCoordinates()) {
                sites.add(gf.createPoint(c));
            }
        }
    }
    
    private Geometry getConstraints(FeatureCollection fcConstraints) {
        List<LineString> constraints = new ArrayList<>();
        GeometryFactory gf;
        if (fcConstraints.isEmpty()) gf = new GeometryFactory();
        else {
            gf = (fcConstraints.iterator().next()).getGeometry().getFactory();
            for (Object o : fcConstraints.getFeatures()) {
                addConstraints(((Feature)o).getGeometry(), constraints, gf);
            }
        }
        return gf.createMultiLineString(constraints.toArray(new LineString[constraints.size()]));
    }
    
    private void addConstraints(Geometry g, List<LineString> constraints, GeometryFactory gf) {
        if (g instanceof GeometryCollection) {
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                addConstraints(g.getGeometryN(i), constraints, gf);
            }
        }
        else if (g instanceof Polygon) {
            constraints.add(((Polygon)g).getExteriorRing());
            for (int i = 0 ; i < ((Polygon)g).getNumInteriorRing() ; i++) {
                constraints.add(((Polygon)g).getInteriorRingN(i));
            }
        }
        else if (g instanceof LineString) {
            constraints.add((LineString)g);
        }
    }
    
    private List<Polygon> getPolygons(FeatureCollection fcConstraints) {
        List<Polygon> polygons = new ArrayList<>();
        GeometryFactory gf;
        if (fcConstraints.isEmpty()) gf = new GeometryFactory();
        else {
            gf = (fcConstraints.iterator().next()).getGeometry().getFactory();
            for (Object o : fcConstraints.getFeatures()) {
                addPolygons(((Feature)o).getGeometry(), polygons, gf);
            }
        }
        return polygons;
    }
    
    private void addPolygons(Geometry g, List<Polygon> polygons, GeometryFactory gf) {
        if (g instanceof GeometryCollection) {
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                addPolygons(g.getGeometryN(i), polygons, gf);
            }
        }
        else if (g instanceof Polygon) {
            polygons.add((Polygon)g);
        }
    }
	
}
