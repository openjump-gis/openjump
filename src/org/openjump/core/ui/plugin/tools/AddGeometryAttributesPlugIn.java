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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
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
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;


/**
 * User can add one or several of the following geometry attributes to a layer.
 * <ul>
 * <li>X,Y</li>
 * <li>Z</li>
 * <li>Number of points<li>
 * <li>Number of holes</li>
 * <li>Number of components</li>
 * <li>Total length</li>
 * <li>Total area</li>
 * <li>Geometry type</li>
 * <li>Geometry in WKT format</li>
 * </ul>
 * @author Micha&euml;l Michaud
 */

public class AddGeometryAttributesPlugIn extends AbstractThreadedUiPlugIn{

    private static String LAYER               = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.layer");

    private static String X                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.x");
    private static String Y                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.y");
    private static String ADD_XY              = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-xy");

    private static String Z                   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.z");
    private static String ADD_Z               = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-z");

    private static String NB_POINTS           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-points");
    private static String ADD_NB_POINTS       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-points");

    private static String NB_HOLES            = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-of-holes");
    private static String ADD_NB_HOLES        = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-holes");

    private static String NB_COMPONENTS       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.nb-of-components");
    private static String ADD_NB_COMPONENTS   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-nb-of-components");

    private static String LENGTH              = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.length");
    private static String ADD_LENGTH          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-length");

    private static String AREA                = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.area");
    private static String ADD_AREA            = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-area");

    private static String GEOM_TYPE           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.geo-type");
    private static String ADD_GEOMETRY_TYPE   = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-geom-type");

    private static String WKT                 = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.wkt");
    private static String ADD_WKT             = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-wkt");

    private static String POLY_WIDTH          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.poly-width");
    private static String ADD_POLY_WIDTH      = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-poly-width");

    private static String POLY_LENGTH         = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.poly-length");
    private static String ADD_POLY_LENGTH     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-poly-length");

    private static String CIRCULARITY         = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.circularity");
    private static String ADD_CIRCULARITY     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-circularity");

    private static String COMPACITY           = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.compacity");
    private static String ADD_COMPACITY       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-compacity");

    private static String GEOM_ATTRIBUTES     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.geometry-attributes");

    private static String COMPUTE_ATTRIBUTES  = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.compute-attributes");


    String layer;
    
    private boolean addXY = true;
    private boolean addZ  = false;
    private boolean addNbPoints  = false;
    private boolean addNbHoles  = false;
    private boolean addNbComponents  = false;
    private boolean addLength  = false;
    private boolean addArea  = false;
    private boolean addGeometryType  = false;
    private boolean addWKT  = false;
    private boolean addPolyWidth  = false;
    private boolean addPolyLength  = false;
    private boolean addCircularity  = false;
    private boolean addCompacity  = false;
    
    public void initialize(PlugInContext context) throws Exception {
    	    
	        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	    	featureInstaller.addMainMenuPlugin(
	    	        this,
	                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
	                getName() + "...", false, null,
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

    public void setLayer(String sitesLayer) {
	    this.layer = sitesLayer;
	}

    public void setAddXY(boolean addXY) {
	    this.addXY = addXY;
	}

    public void setAddZ(boolean addZ) {
	    this.addZ = addZ;
	}

    public void setAddNbPoints(boolean addNbPoints) {
	    this.addNbPoints = addNbPoints;
	}

    public void setAddNbHoles(boolean addNbHoles) {
	    this.addNbHoles = addNbHoles;
	}

    public void setAddNbComponents(boolean addNbComponents) {
	    this.addNbComponents = addNbComponents;
	}

    public void setAddLength(boolean addLength) {
	    this.addLength = addLength;
	}

    public void setAddArea(boolean addArea) {
	    this.addArea = addArea;
	}

    public void setAddPolyWidth(boolean addPolyWidth) {
        this.addPolyWidth = addPolyWidth;
    }

    public void setAddPolyLength(boolean addPolyLength) {
        this.addPolyLength = addPolyLength;
    }

    public void setAddCircularity(boolean addCircularity) {
        this.addCircularity = addCircularity;
    }

    public void setAddCompacity(boolean addCompacity) {
        this.addCompacity = addCompacity;
    }

    public void setAddGeometryType(boolean addGeometryType) {
	    this.addGeometryType = addGeometryType;
	}

    public void setAddWKT(boolean addWKT) {
	    this.addWKT = addWKT;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        layer = context.getCandidateLayer(0).getName();
    	dialog.addLayerComboBox(LAYER, context.getLayerManager().getLayer(layer), null, context.getLayerManager());
    	dialog.addCheckBox(ADD_XY, addXY);
    	dialog.addCheckBox(ADD_Z, addZ);
    	dialog.addCheckBox(ADD_NB_POINTS, addNbPoints);
    	dialog.addCheckBox(ADD_NB_HOLES, addNbHoles);
    	dialog.addCheckBox(ADD_NB_COMPONENTS, addNbComponents);
    	dialog.addCheckBox(ADD_LENGTH, addLength);
    	dialog.addCheckBox(ADD_AREA, addArea);
        dialog.addCheckBox(ADD_POLY_WIDTH, addPolyWidth);
        dialog.addCheckBox(ADD_POLY_LENGTH, addPolyLength);
        dialog.addCheckBox(ADD_CIRCULARITY, addCircularity);
        dialog.addCheckBox(ADD_COMPACITY, addCompacity);
    	dialog.addCheckBox(ADD_GEOMETRY_TYPE, addGeometryType);
    	dialog.addCheckBox(ADD_WKT, addWKT);
    }

	private void getDialogValues(MultiInputDialog dialog) {
	    layer           = dialog.getLayer(LAYER).getName();
	    addXY           = dialog.getBoolean(ADD_XY);
	    addZ            = dialog.getBoolean(ADD_Z);
	    addNbPoints     = dialog.getBoolean(ADD_NB_POINTS);
	    addNbHoles      = dialog.getBoolean(ADD_NB_HOLES);
	    addNbComponents = dialog.getBoolean(ADD_NB_COMPONENTS);
	    addLength       = dialog.getBoolean(ADD_LENGTH);
	    addArea         = dialog.getBoolean(ADD_AREA);
        addPolyWidth    = dialog.getBoolean(ADD_POLY_WIDTH);
        addPolyLength   = dialog.getBoolean(ADD_POLY_LENGTH);
        addCircularity  = dialog.getBoolean(ADD_CIRCULARITY);
        addCompacity    = dialog.getBoolean(ADD_COMPACITY);
	    addGeometryType = dialog.getBoolean(ADD_GEOMETRY_TYPE);
	    addWKT          = dialog.getBoolean(ADD_WKT);
    }
    
	
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.report(COMPUTE_ATTRIBUTES + "...");
        LayerManager layerManager = context.getLayerManager();
        FeatureCollection inputFC = layerManager.getLayer(layer).getFeatureCollectionWrapper();
        FeatureCollection result = new FeatureDataset(getNewSchema(layerManager.getLayer(layer)));
        for (Object o : inputFC.getFeatures()){
            Feature f = (Feature)o;
            Feature bf = new BasicFeature(result.getFeatureSchema());
            Object[] attributes = new Object[result.getFeatureSchema().getAttributeCount()];
            System.arraycopy(f.getAttributes(), 0, attributes, 0, f.getSchema().getAttributeCount());
            bf.setAttributes(attributes);
            setGeometryAttributes(bf);
            result.add(bf);
        }
        context.getLayerManager().addLayer(StandardCategoryNames.RESULT, layer+"-"+GEOM_ATTRIBUTES, result); 
    }
    
    private FeatureSchema getNewSchema(Layer layer) {
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema().clone();
        if (addXY) {
            schema.addAttribute(X, AttributeType.DOUBLE);
            schema.addAttribute(Y, AttributeType.DOUBLE);
        }
        if (addZ)            schema.addAttribute(Z, AttributeType.DOUBLE);
        if (addNbPoints)     schema.addAttribute(NB_POINTS, AttributeType.INTEGER);
        if (addNbHoles)      schema.addAttribute(NB_HOLES, AttributeType.INTEGER);
        if (addNbComponents) schema.addAttribute(NB_COMPONENTS, AttributeType.INTEGER);
        if (addLength)       schema.addAttribute(LENGTH, AttributeType.DOUBLE);
        if (addArea)         schema.addAttribute(AREA, AttributeType.DOUBLE);
        if (addPolyWidth)    schema.addAttribute(POLY_WIDTH, AttributeType.DOUBLE);
        if (addPolyLength)   schema.addAttribute(POLY_LENGTH, AttributeType.DOUBLE);
        if (addCircularity)  schema.addAttribute(CIRCULARITY, AttributeType.DOUBLE);
        if (addCompacity) schema.addAttribute(COMPACITY, AttributeType.STRING);
        if (addGeometryType) schema.addAttribute(GEOM_TYPE, AttributeType.STRING);
        if (addWKT)          schema.addAttribute(WKT, AttributeType.STRING);
        return schema;
    }
    
    private void setGeometryAttributes(Feature f) {
        Geometry g = f.getGeometry();
        if (addXY) {
            f.setAttribute(X, g.getCoordinate().x);
            f.setAttribute(Y, g.getCoordinate().y);
        }
        if (addZ) f.setAttribute(Z, g.getCoordinate().z);
        if (addNbPoints) f.setAttribute(NB_POINTS, g.getCoordinates().length);
        if (addNbHoles) {
            int h = 0;
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry component = g.getGeometryN(i);
                if (component instanceof Polygon) {
                    h += ((Polygon)component).getNumInteriorRing();
                }
            }
            f.setAttribute(NB_HOLES, h);
        }
        if (addNbComponents) f.setAttribute(NB_COMPONENTS, g.getNumGeometries());
        if (addLength) f.setAttribute(LENGTH, g.getLength());
        if (addArea) f.setAttribute(AREA, g.getArea());
        if (addPolyWidth) f.setAttribute(POLY_WIDTH, getPolyWidth(g));
        if (addPolyLength) f.setAttribute(POLY_LENGTH, getPolyLength(g));
        if (addCircularity) f.setAttribute(CIRCULARITY, getCircularity(g));
        if (addCompacity) f.setAttribute(COMPACITY, getCompacity(g));
        if (addGeometryType) f.setAttribute(GEOM_TYPE, g.getGeometryType());
        if (addWKT) f.setAttribute(WKT, g.toString());
    }

    private Double getPolyWidth(Geometry g) {
        if (g.getDimension() == 2) {
            double length = g.getLength();
            double area = g.getArea();
            double val = (( length * length ) / 4.0 )-( 4.0 * area );
            if (val >= 0.0) {
                //calcul normal sur surface allongée
                return (((length / 2.0) - Math.sqrt(val)) / 2.0);
            } else {
                //diamètre du disque de même surface, sur une surface ramassée
                return 2.0 * Math.sqrt(area / Math.PI);
            }
        } else return null;
    }

    private Double getPolyLength(Geometry g) {
        if (g.getDimension() == 2) {
            double length = g.getLength();
            double area = g.getArea();
            double val = (( length * length ) / 4.0 )-( 4.0 * area );
            if (val >= 0.0) {
                //calcul normal sur surface allongée
                return area / (((length / 2.0) - Math.sqrt(val)) / 2.0);
            } else {
                //diamètre du disque de même surface, sur une surface ramassée
                return 2.0 * Math.sqrt(area / Math.PI);
            }
        } else return null;
    }

    // Ratio between the geometry area and the area of a circle having the same perimeter
    // = 1 if the olygonis a circle
    // = 0 if the polygon is completely flat
    private Double getCircularity(Geometry g) {
        if (g.getDimension() == 2) {
            if (g.isEmpty()) return 0.0;
            double length = g.getLength();
            return 4 * Math.PI * g.getArea() / length / length;
        } else return null;
    }

    // Ratio between the perimeter of the geometry and the perimeter of the circle with the same perimeter
    // Not bounded value :
    // = 1 (circle)
    // = positive infinity (flat)
    private Double getCompacity(Geometry g) {
        if (g.getDimension() == 2) {
            double area = g.getArea();
            return area == 0 ? 0 : g.getLength() / (2*Math.sqrt(Math.PI*area));
        } else return null;
    }

}
