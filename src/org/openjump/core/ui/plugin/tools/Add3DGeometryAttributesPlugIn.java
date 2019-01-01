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

import com.vividsolutions.jts.geom.*;
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

import java.util.ArrayList;
import java.util.List;

//import org.openjump.core.ui.images.IconLoader;
/**
 * User can add one or several of the following 3d attributes to a layer.
 * <ul>
 * <li>Start point Z</li>
 * <li>End point Z</li>
 * <li>Min Z value<li>
 * <li>Max Z value</li>
 * <li>Mean weighted Z value (weight depends on line length)</li>
 * <li>Total 3d length</li>
 * <li>Minimum signed slope (dz/length)</li>
 * <li>Maximum signed slope (dz/length)</li>
 * <li>Maximum unoriented slope (dz/length)</li>
 * <li>Number of coordinates with NaN value</li>
 * <li>Number of coordinates with negative value</li>
 * <li>Number of coordinates with 0 value</li>
 * <li>Number of coordinates with positive value</li>
 * </ul>
 * @author Micha&euml;l Michaud
 */

public class Add3DGeometryAttributesPlugIn extends AbstractThreadedUiPlugIn{

    private static String LAYER               = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.layer");

    private static String GEOM_ATTRIBUTES     = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.3d-geometry-attributes");

    private static String COMPUTE_ATTRIBUTES  = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.compute-attributes");

    private static String START_Z             = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.start-z");
    private static String ADD_START_Z         = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-start-z");

    private static String END_Z               = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.end-z");
    private static String ADD_END_Z           = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-end-z");

    private static String MIN_Z               = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.min-z");
    private static String ADD_MIN_Z           = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-min-z");

    private static String MAX_Z               = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.max-z");
    private static String ADD_MAX_Z           = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-max-z");

    private static String WEIGHTED_MEAN_Z     = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.weighted-mean-z");
    private static String ADD_WEIGHTED_MEAN_Z = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-weighted-mean-z");

    private static String LENGTH_3D           = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.length-3d");
    private static String ADD_LENGTH_3D       = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-length-3d");

    private static String MAX_DOWN_SLOPE      = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.max-downslope");
    private static String ADD_MAX_DOWNSLOPE   = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-max-downslope");

    private static String MAX_UPSLOPE         = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.max-upslope");
    private static String ADD_MAX_UPSLOPE     = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-max-upslope");

    private static String MAX_SLOPE           = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.max-slope");
    private static String ADD_MAX_SLOPE       = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-max-slope");

    private static String NB_NAN_Z            = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.nb-nan-z");
    private static String ADD_NB_NAN_Z        = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-nb-nan-z");

    private static String NB_NEGATIVE_Z       = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.nb-negative-z");
    private static String ADD_NB_NEGATIVE_Z   = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-nb-negative-z");

    private static String NB_0_Z              = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.nb-0-z");
    private static String ADD_NB_0_Z          = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-nb-0-z");

    private static String NB_POSITIVE_Z       = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.nb-positive-z");
    private static String ADD_NB_POSITIVE_Z   = I18N.get("org.openjump.core.ui.plugin.tools.Add3DGeometryAttributesPlugIn.add-nb-positive-z");
    // 
    // // MORPHOLOGY
    // public static String ADD_ORIENTATION     = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-orientation");
    // // http://iahs.info/hsj/470/hysj_47_06_0921.pdf
    // // http://www.ipublishing.co.in/jggsvol1no12010/voltwo/EIJGGS3022.pdf
    // public static String ADD_GRAVELIUS          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");
    // public static String ADD_MILLER          = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");
    
    // public static String ADD_SINUOSITY       = I18N.get("org.openjump.core.ui.plugin.tools.AddGeometryAttributesPlugIn.add-miller");

    String layer;
    
    private boolean addStartZ         = false;
    private boolean addEndZ           = false;
    private boolean addMinZ           = true;
    private boolean addMaxZ           = true;
    private boolean addWeightedMeanZ  = false;
    private boolean addLength3d       = true;
    private boolean addMaxDownslope   = false;
    private boolean addMaxUpslope     = false;
    private boolean addMaxSlope       = false;
    private boolean addNbNaNZ         = true;
    private boolean addNbNegativeZ    = false;
    private boolean addNb0Z           = false;
    private boolean addNbPositiveZ    = false;
    
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
    
	public void setAddStartZ(boolean addStartZ) {
	    this.addStartZ = addStartZ;
	}
	
	public void setAddEndZ(boolean addEndZ) {
	    this.addEndZ = addEndZ;
	}
	
	public void setAddMinZ(boolean addMinZ) {
	    this.addMinZ = addMinZ;
	}
	
	public void setAddMaxZ(boolean addMaxZ) {
	    this.addMaxZ = addMaxZ;
	}
	
	public void setAddWeightedMeanZ(boolean addWeightedMeanZ) {
	    this.addWeightedMeanZ = addWeightedMeanZ;
	}
    
	public void setAddLength3d(boolean addLength3d) {
	    this.addLength3d = addLength3d;
	}
	
	public void setAddMaxDownSlope(boolean addMaxDownslope) {
	    this.addMaxDownslope = addMaxDownslope;
	}
	
	public void setAddMaxUpslope(boolean addMaxUpslope) {
	    this.addMaxUpslope = addMaxUpslope;
	}
	
	public void setAddMaxSlope(boolean addMaxSlope) {
	    this.addMaxSlope = addMaxSlope;
	}
	
	public void setAddNbNaNZ(boolean addNbNaNZ) {
	    this.addNbNaNZ = addNbNaNZ;
	}
	
	public void setAddNbNegativeZ(boolean addNbNegativeZ) {
	    this.addNbNegativeZ = addNbNegativeZ;
	}
	
	public void setAddNb0Z(boolean addNb0Z) {
	    this.addNb0Z = addNb0Z;
	}
	
	public void setAddNbPositiveZ(boolean addNbPositiveZ) {
	    this.addNbPositiveZ = addNbPositiveZ;
	}
	
    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        layer = context.getCandidateLayer(0).getName();
    	dialog.addLayerComboBox(LAYER, context.getLayerManager().getLayer(layer), null, context.getLayerManager());
    	dialog.addSeparator();
    	dialog.addCheckBox(ADD_START_Z, addStartZ);
    	dialog.addCheckBox(ADD_END_Z, addEndZ);
    	dialog.addCheckBox(ADD_MIN_Z, addMinZ);
    	dialog.addCheckBox(ADD_MAX_Z, addMaxZ);
    	dialog.addCheckBox(ADD_WEIGHTED_MEAN_Z, addWeightedMeanZ);
    	dialog.addSeparator();
    	dialog.addCheckBox(ADD_LENGTH_3D, addLength3d);
    	dialog.addCheckBox(ADD_MAX_DOWNSLOPE, addMaxDownslope);
    	dialog.addCheckBox(ADD_MAX_UPSLOPE, addMaxUpslope);
    	dialog.addCheckBox(ADD_MAX_SLOPE, addMaxSlope);
    	dialog.addSeparator();
    	dialog.addCheckBox(ADD_NB_NAN_Z, addNbNaNZ);
    	dialog.addCheckBox(ADD_NB_NEGATIVE_Z, addNbNegativeZ);
    	dialog.addCheckBox(ADD_NB_0_Z, addNb0Z);
    	dialog.addCheckBox(ADD_NB_POSITIVE_Z, addNbPositiveZ);
    }

	private void getDialogValues(MultiInputDialog dialog) {
	    layer             = dialog.getLayer(LAYER).getName();
	    
	    addStartZ         = dialog.getBoolean(ADD_START_Z);
	    addEndZ           = dialog.getBoolean(ADD_END_Z);
	    addMinZ           = dialog.getBoolean(ADD_MIN_Z);
	    addMaxZ           = dialog.getBoolean(ADD_MAX_Z);
	    addWeightedMeanZ  = dialog.getBoolean(ADD_WEIGHTED_MEAN_Z);
	    
	    addLength3d       = dialog.getBoolean(ADD_LENGTH_3D);
	    addMaxDownslope   = dialog.getBoolean(ADD_MAX_DOWNSLOPE);
	    addMaxUpslope     = dialog.getBoolean(ADD_MAX_UPSLOPE);
	    addMaxSlope       = dialog.getBoolean(ADD_MAX_SLOPE);
	    
	    addNbNaNZ         = dialog.getBoolean(ADD_NB_NAN_Z);
	    addNbNegativeZ    = dialog.getBoolean(ADD_NB_NEGATIVE_Z);
	    addNb0Z           = dialog.getBoolean(ADD_NB_0_Z);
	    addNbPositiveZ    = dialog.getBoolean(ADD_NB_POSITIVE_Z);
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
        
        if (addStartZ)         schema.addAttribute(START_Z, AttributeType.DOUBLE);
        if (addEndZ)           schema.addAttribute(END_Z, AttributeType.DOUBLE);
        if (addMinZ)           schema.addAttribute(MIN_Z, AttributeType.DOUBLE);
        if (addMaxZ)           schema.addAttribute(MAX_Z, AttributeType.DOUBLE);
        if (addWeightedMeanZ)  schema.addAttribute(WEIGHTED_MEAN_Z, AttributeType.DOUBLE);
        
        if (addLength3d)       schema.addAttribute(LENGTH_3D, AttributeType.DOUBLE);
        if (addMaxDownslope)   schema.addAttribute(MAX_DOWN_SLOPE, AttributeType.DOUBLE);
        if (addMaxUpslope)     schema.addAttribute(MAX_UPSLOPE, AttributeType.DOUBLE);
        if (addMaxSlope)       schema.addAttribute(MAX_SLOPE, AttributeType.DOUBLE);
        
        if (addNbNaNZ)         schema.addAttribute(NB_NAN_Z, AttributeType.INTEGER);
        if (addNbNegativeZ)    schema.addAttribute(NB_NEGATIVE_Z, AttributeType.INTEGER);
        if (addNb0Z)           schema.addAttribute(NB_0_Z, AttributeType.INTEGER);
        if (addNbPositiveZ)    schema.addAttribute(NB_POSITIVE_Z, AttributeType.INTEGER);
        return schema;
    }
    
    private void setGeometryAttributes(Feature f) {
        Geometry g = f.getGeometry();
        Coordinate[] cc = g.getCoordinates();
        
        // Attributes with direct access
        //double startZ=Double.NaN, endZ=Double.NaN;
        if (addStartZ) f.setAttribute(START_Z, cc[0].z);
        if (addEndZ) f.setAttribute(END_Z, cc[cc.length-1].z);
        
        // Attributes requiring a scan of Coordinate array
        double minZ=Double.NaN, maxZ=Double.NaN;
        int nbNaNZ=0, nbNegativeZ=0, nb0Z=0, nbPositiveZ=0;
        for (Coordinate c : cc) {
            if (Double.isNaN(c.z)) {
                nbNaNZ++;
            }
            else {
                if (Double.isNaN(minZ) || c.z < minZ) minZ = c.z;
                if (Double.isNaN(maxZ) || c.z > maxZ) maxZ = c.z;
                if (c.z < 0.0) nbNegativeZ++;
                if (c.z == 0.0) nb0Z++;
                if (c.z > 0.0) nbPositiveZ++;
            }
        }
        if (addMinZ) f.setAttribute(MIN_Z, minZ);
        if (addMaxZ) f.setAttribute(MAX_Z, maxZ);
        if (addNbNaNZ) f.setAttribute(NB_NAN_Z, nbNaNZ);
        if (addNbNegativeZ) f.setAttribute(NB_NEGATIVE_Z, nbNegativeZ);
        if (addNb0Z) f.setAttribute(NB_0_Z, nb0Z);
        if (addNbPositiveZ) f.setAttribute(NB_POSITIVE_Z, nbPositiveZ);
        
        // Attributes requiring a scan of each linear component
        double weightedMeanZ = Double.NaN;
        double length3d = 0;
        double maxDownslope = Double.NaN;
        double maxUpslope = Double.NaN;
        double maxSlope = Double.NaN;
        int dim = g.getDimension();
        if (dim == 0) {
            int nbNonNullZ = 0;
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry component = g.getGeometryN(i);
                Coordinate c = component.getCoordinate();
                // Point or Multipoint
                if (!Double.isNaN(c.z)) {
                    if (Double.isNaN(weightedMeanZ)) weightedMeanZ = c.z;
                    else weightedMeanZ += c.z;
                    nbNonNullZ++;
                }
            }
            weightedMeanZ = weightedMeanZ/nbNonNullZ;
        }
        else if (dim > 0) {
            double weightedLength2d = 0;
            for (int i = 0 ; i < g.getNumGeometries() ; i++) {
                Geometry component = g.getGeometryN(i);
                if (component.getDimension() == 0) continue;
                // We first collect sublinestrings having initial and a final z
                List<ZBoundedSubLineString> list = new ArrayList<>();
                if (component instanceof LineString) {
                    getZBoundedSubLineStrings((LineString)component, list);
                }
                else if (component instanceof Polygon) {
                    Polygon polygon = (Polygon)component;
                    getZBoundedSubLineStrings(polygon.getExteriorRing(), list);
                    for (int j = 0 ; j < polygon.getNumInteriorRing() ; j++) {
                        getZBoundedSubLineStrings(polygon.getInteriorRingN(j), list);
                    }
                }
                for (ZBoundedSubLineString line : list) {
                    // sum of 3d length
                    length3d += line.getLength3d();
                    // sum of valid weighted Z
                    double wZ = line.getWeightedZ();
                    double slope = line.getSlope();
                    if (!Double.isNaN(wZ)) {
                        if (Double.isNaN(weightedMeanZ)) weightedMeanZ = wZ;
                        else weightedMeanZ += wZ;
                        weightedLength2d += line.getLength2d();
                    }
                    if (Double.isNaN(maxDownslope)) maxDownslope = slope;
                    else {
                        if (!Double.isNaN(slope) && slope < maxDownslope) maxDownslope = slope;
                    }
                    if (Double.isNaN(maxUpslope)) maxUpslope = slope;
                    else {
                        if (!Double.isNaN(slope) && slope > maxUpslope) maxUpslope = slope;
                    }
                    if (Double.isNaN(maxSlope)) maxSlope = Math.abs(slope);
                    else {
                        if (!Double.isNaN(slope) && Math.abs(slope) > maxSlope) maxSlope = Math.abs(slope);
                    }
                }
            }
            weightedMeanZ = weightedMeanZ/weightedLength2d;
        }
        if (maxDownslope > 0) maxDownslope = Double.NaN;
        if (maxUpslope < 0) maxUpslope = Double.NaN;
        if (addLength3d) f.setAttribute(LENGTH_3D, length3d);
        if (addWeightedMeanZ) f.setAttribute(WEIGHTED_MEAN_Z, weightedMeanZ);
        if (addMaxDownslope) f.setAttribute(MAX_DOWN_SLOPE, 100*maxDownslope);
        if (addMaxUpslope) f.setAttribute(MAX_UPSLOPE, 100*maxUpslope);
        if (addMaxSlope) f.setAttribute(MAX_SLOPE, 100*maxSlope);
    }
    
    private void getZBoundedSubLineStrings(LineString line, List<ZBoundedSubLineString> list) {
        Coordinate[] cc = line.getCoordinates();
        double subLineLength = 0.0;
        double prevZ = cc[0].z;
        for (int i = 1 ; i < cc.length ; i++) {
            subLineLength += cc[i-1].distance(cc[i]);
            Coordinate c = cc[i];
            if (!Double.isNaN(c.z) || i == (cc.length-1)) {
                list.add(new ZBoundedSubLineString(prevZ, c.z, subLineLength));
                subLineLength = 0;
                prevZ = c.z;
            }
        }
    }
    
    /**
     * Inner Class representing a sub-linestring where each end points is either 
     * <ul>
     * <li>an endpoint of the main LineString</li>
     * <li>an interior point with a z value</li>
     * </ul>
     * Z characteristics as 3D length or slopes are computed on
     * ZBoundedSubLineString
     */
    private static class ZBoundedSubLineString {
        
        double startZ;
        double endZ;
        double length2d;
        double dz = Double.NaN;

        private ZBoundedSubLineString(double startZ, double endZ, double length2d) {
            this.startZ = startZ;
            this.endZ = endZ;
            this.length2d = length2d;
            if (!Double.isNaN(startZ) && !Double.isNaN(endZ)) dz = endZ-startZ;
        }

        private double getLength2d() {
            return length2d;
        }
        
        /** 
         * Return the length3d of this substring if both start and end points
         * have a z, length2d elseif.
         */
        private double getLength3d() {
            return Double.isNaN(dz) ? length2d : Math.sqrt(length2d*length2d + dz*dz);
        }
        
        /**
        * Return :
        * <ul>
        * <li>mean z * length2d if both ends of the linestring have a z</li>
        * <li>z * length2d if only one end has a z</li>
        * <li>NaN if substring end points don't have z values
        * </ul>
        */
        private double getWeightedZ() {
            if (Double.isNaN(dz)) {
                if (!Double.isNaN(startZ)) return startZ * length2d;
                if (!Double.isNaN(endZ)) return endZ * length2d;
                return Double.NaN;
            }
            else return (startZ+(dz/2.0)) * length2d;
        }
        
        /**
         * Return the signed slope of the sublinestring (dz/length2d) or NaN if 
         * it contains only one or z value.
         */
        private double getSlope() {
            if (!Double.isNaN(dz)) {
                if (length2d > 0) return dz/length2d;
                else if (dz > 0) return Double.POSITIVE_INFINITY;
                else if (dz < 0) return Double.NEGATIVE_INFINITY;
                else return 0.0;
            }
            else return Double.NaN;
        }
    }
	
}
