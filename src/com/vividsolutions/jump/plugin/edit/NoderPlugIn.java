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
package com.vividsolutions.jump.plugin.edit;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.noding.*;
import com.vividsolutions.jts.noding.snapround.MCIndexSnapRounder;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.precision.CoordinatePrecisionReducerFilter;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


/**
 * Noder PlugIn computes intersection nodes in a collection of linear or areal
 * features.</p>
 * Main options are :
 * <ul>
 * <li>Input features : selected features or selected layer</li>
 * <li>Output : new layer or layer update</li>
 * <li>Output intersection points : yes/no</li>
 * <li>What do you want to do with lines : nothing, insert nodes, split lines</li>
 * <li>What do you want to do with polygons : nothing, insert nodes, split polygons</li>
 * <li>Make the process fully robust using snap rounding mode : yes/no</li>
 * <li>Interpolate z on new points</li>
 * </ul>
 *
 * @author Micha&euml;l Michaud
 */
public class NoderPlugIn extends AbstractThreadedUiPlugIn {
    
    private final static String PROCESSED_DATA          = I18N.get("jump.plugin.edit.NoderPlugIn.processed-data");
    private final static String SRC_LAYER               = GenericNames.SOURCE_LAYER;
    private final static String SELECTED_ONLY           = GenericNames.USE_SELECTED_FEATURES_ONLY;
    
    private final static String PROCESSING              = I18N.get("jump.plugin.edit.NoderPlugIn.processing");
    
    private final static String FIND_INTERSECTIONS      = I18N.get("jump.plugin.edit.NoderPlugIn.find-intersections");
    private final static String FIND_DESCRIPTION        = I18N.get("jump.plugin.edit.NoderPlugIn.create-new-layer-with-missing-intersections");
    private final static String LINE_OPTIONS            = I18N.get("jump.plugin.edit.NoderPlugIn.line-options");
    private final static String POLYGON_OPTIONS         = I18N.get("jump.plugin.edit.NoderPlugIn.polygon-options");
                                                        
    private final static String ADVANCED_OPTIONS        = I18N.get("jump.plugin.edit.NoderPlugIn.advanced-options");
    //private final static String NODING_METHOD           = I18N.get("jump.plugin.edit.NoderPlugIn.noding-method");
    private final static String SNAP_ROUNDING           = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding");
    private final static String SNAP_ROUNDING_TOOLTIP   = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding-makes-noding-algorithm-fully-robust");
    private final static String SNAP_ROUNDING_DP        = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding-decimal-places");
    private final static String DECIMAL_DIGITS_TOOLTIP  = I18N.get("jump.plugin.edit.NoderPlugIn.number-of-decimal-digits");
    
    private final static String INTERPOLATE_Z           = I18N.get("jump.plugin.edit.NoderPlugIn.interpolate-z");
    private final static String INTERPOLATED_Z_DP       = I18N.get("jump.plugin.edit.NoderPlugIn.interpolated-z-decimal-places");
    
    private final static String INTERSECTIONS           = I18N.get("jump.plugin.edit.NoderPlugIn.intersections");
    private final static String NODED                   = I18N.get("jump.plugin.edit.NoderPlugIn.noded");
    
    /**
     * Enumeration to choose if elements are processed and if they are only
     * noded or also splitted.
     */
    public static enum Processor {
        
        DO_NOT_PROCESS, NODE, SPLIT;
        
        public String toString() {
            return I18N.get("jump.plugin.edit.NoderPlugIn." + 
                            name().toLowerCase().replaceAll("_","-"));
        }
    }

    private boolean use_selected = false;
    private String layerName;
    private GeometryFactory gf;
    //private FeatureSchema schema_preserve_attributes;
    
    private boolean find_intersections = true;
    private Processor line_processor = Processor.SPLIT;
    private Processor polygon_processor = Processor.NODE;
    
    private boolean snap_rounding = false;
    int snap_rounding_dp = 6;
    
    private boolean interpolate_z = false;
    private int interpolated_z_dp  = 3;
    
    private static final RobustLineIntersector ROBUST_INTERSECTOR = new RobustLineIntersector();

    public NoderPlugIn() { }
  
    public void initialize(PlugInContext context) throws Exception {
        	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    		featureInstaller.addMainMenuPlugin(this,
              new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},
              getName() + "...", false, null,
              createEnableCheck(context.getWorkbenchContext()), -1);  
    }
  
    public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        return true;
    }
    
    public void setUseSelected(boolean use_selected) {this.use_selected = use_selected;}
    public void setLayerName(String layerName) {this.layerName = layerName;}
    public void setSnapRounding(boolean snap_rounding) {this.snap_rounding = snap_rounding;}
    public void setSnapRoundingDp(int snap_rounding_dp) {this.snap_rounding_dp = snap_rounding_dp;}
    public void setFindIntersections(boolean find_intersections) {this.find_intersections = find_intersections;}
    
    public void setLineProcessor(Processor processor) {this.line_processor = processor;}
    public void setPolygonProcessor(Processor processor) {this.polygon_processor = processor;}
    public void setInterpolateZ(boolean interpolate_z) {this.interpolate_z = interpolate_z;}
    public void setInterpolatedZDp(int interpolated_z_dp) {this.interpolated_z_dp = interpolated_z_dp;}

    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.noding-input"));
        
        // Test if features are selected
        int selectedFeaturesNb = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
        use_selected = selectedFeaturesNb > 0;
        
        // [mmichaud 2012-04-18] main change in input data structure to be able to
        // manage input from a selection of features belonging to several layers.
        Map<Layer,Collection<Feature>> inputFeatures = getFeaturesToProcess(context);
        Map<Layer,Collection<Feature>> outputFeatures = new HashMap<Layer,Collection<Feature>>();
        Collection<Feature> inputAll = new ArrayList<Feature>();
        Map<Feature,Layer> featureToLayer = new HashMap<Feature,Layer>(); 
        for (Layer layer : inputFeatures.keySet()) {
            outputFeatures.put(layer, new ArrayList<Feature>());
            inputAll.addAll(inputFeatures.get(layer));
            for (Feature f : inputFeatures.get(layer)) {
                featureToLayer.put(f,layer);
            }
        }

        // Short-circuit if inputFeatures is empty
        if (inputAll.isEmpty()) {
            context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-data-to-process"));
            return;
        }
        
        // Create a GeometryFactory consistent with the snap_rounding parameters 
        if (snap_rounding) {
            gf = new GeometryFactory(
                new PrecisionModel(Math.pow(10.0, (double)snap_rounding_dp)));
        }
        else {
            gf = (inputAll.iterator().next()).getGeometry().getFactory();
        }
        
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.noding"));
        
        // Segments strings are extracted from the input dataset, keeping a
        // link to their parent feature
        List<SegmentString> segmentStrings = 
            Features2SegmentStringsWithData.getSegmentStrings(inputAll);
        
        // If inputFeatures contains only 0-dim features, segmentString will be
        // empty and the process should stop !
        if (segmentStrings.isEmpty()) {
            context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-data-to-process"));
            return;
        }
        
        
        if (find_intersections) {
            FeatureCollection nodes;
            // If the user does not want to split features, find intersections will
            // only find places where a vertex is missing (intersections located in
            // the interior of a segment)
            // ==> use IntersectionFinderAdder
            //if ((!split_lines) && (!split_polygons)) {
            if (line_processor != Processor.SPLIT && polygon_processor != Processor.SPLIT) {
                IntersectionFinderAdder intersector = new IntersectionFinderAdder(ROBUST_INTERSECTOR);
                nodes = findInteriorIntersections(segmentStrings, intersector);
            }
            // If the user wants to split features (either linestring or polygons), 
            // find intersections will return all intersections located in the 
            // interior of linear elements (including existing vertices as long as
            // they are not a linestring endpoint).
            // ==> use IntersectionAdder
            else {
                IntersectionAdder intersector = new IntersectionAdder(ROBUST_INTERSECTOR);
                nodes = findIntersections(segmentStrings, intersector);
            }
            if (nodes!= null) {
                context.addLayer(StandardCategoryNames.RESULT, layerName + " " + INTERSECTIONS, nodes);
            }
        }
        
        // If neither process lines nor process polygons is checked, do nothing
        // if (do_not_process_lines && do_not_process_polygons) {
        if (line_processor == Processor.DO_NOT_PROCESS && polygon_processor == Processor.DO_NOT_PROCESS) {
        }
        
        // Else, compute nodes and create the geomStructureMap
        else {
            Noder noder = snap_rounding ? 
                          getScaledNoder() :
                          getMCIndexNoder(new IntersectionAdder(ROBUST_INTERSECTOR));
            noder.computeNodes(segmentStrings);
            Collection nodedSubstring = noder.getNodedSubstrings();

            Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> 
                geomStructureMap = SegmentStringsWithData2Features
                                   .getFeature2SegmentStringTreeMap(nodedSubstring);
            FeatureCollection fc = new FeatureDataset(
                inputFeatures.keySet().iterator().next().getFeatureCollectionWrapper().getFeatureSchema());
            //final Collection<Feature> updatedFeatures = new ArrayList<Feature>();

            //if (node_lines || node_polygons) {
            if (line_processor == Processor.NODE || polygon_processor == Processor.NODE) {
                if (!use_selected) {
                    fc.addAll(nodeFeatures(geomStructureMap, interpolate_z, interpolated_z_dp));
                }
                else {
                    insertRemove(inputFeatures, geomStructureMap, outputFeatures);
                }
            }
            
            // If split AND interpolation is wanted, we need to
            // - merge noded SegmentStrings for interpolation
            // - interpolate
            // - split again merged strings into SegmentStrings
            if (interpolate_z && 
                (line_processor == Processor.SPLIT || polygon_processor == Processor.SPLIT)) {
                for (Map.Entry<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> entry : geomStructureMap.entrySet()) {
                    Geometry g = entry.getKey().getGeometry();
                    int dim = g.getDimension();
                    if ((dim == 1 && line_processor == Processor.SPLIT) || 
                        (dim == 2 && polygon_processor == Processor.SPLIT)) {
                        SegmentStringsWithData2Features.buildGeometry(g, 
                            entry.getValue(), true, interpolated_z_dp);
                    }
                }
            }
            
            // Split lines and/or polygons either with interpolated z or not
            if (line_processor == Processor.SPLIT || polygon_processor == Processor.SPLIT) {
                if (!use_selected) {
                    if (line_processor == Processor.SPLIT) {
                        fc.addAll(splitLines(monitor, nodedSubstring, featureToLayer, outputFeatures));
                    }
                    if (polygon_processor == Processor.SPLIT) {
                        STRtree index = indexSegmentStrings(nodedSubstring);
                        fc.addAll(splitPolygons(monitor, geomStructureMap, index, featureToLayer, outputFeatures));
                    }
                }
                else {
                    insertRemove(inputFeatures, geomStructureMap, outputFeatures);
                    if (line_processor == Processor.SPLIT) {
                        splitLines(monitor, nodedSubstring, featureToLayer, outputFeatures);
                    }
                    if (polygon_processor == Processor.SPLIT) {
                        STRtree index = indexSegmentStrings(nodedSubstring);
                        splitPolygons(monitor, geomStructureMap, index, featureToLayer, outputFeatures);
                    }
                }
            }
            if (use_selected) {
                for (Layer layer : inputFeatures.keySet()) {
                    if (layer.isEditable()) {
                        commitUpdate(context, layer, inputFeatures.get(layer), outputFeatures.get(layer));
                        //[mmichaud 2012-04-19] sellect updated features
                        context.getLayerViewPanel().getSelectionManager().getFeatureSelection().unselectItems(layer);
                        context.getLayerViewPanel().getSelectionManager().getFeatureSelection().selectItems(layer, outputFeatures.get(layer));
                    }
                }
            }
            else if (fc.size() > 0) {
                context.addLayer(
                    StandardCategoryNames.RESULT, layerName + " " + NODED, fc
                );
            } else context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-output-data"));
        }
        
        //if (monitor.isCancelRequested()) return;
    }
    
    private Noder getScaledNoder() {
        return new ScaledNoder(new MCIndexSnapRounder(new PrecisionModel(1.0)), 
                               Math.pow(10.0, (double)snap_rounding_dp));
    }
    
    private Noder getMCIndexNoder(SegmentIntersector intersector) {
        MCIndexNoder noder = new MCIndexNoder();
        noder.setSegmentIntersector(intersector);
        //IteratedNoder noder = new IteratedNoder(new PrecisionModel());
        //noder.setMaximumIterations(16);
        return noder;
    }
    
    private Map<Layer,Collection<Feature>> getFeaturesToProcess(PlugInContext context){
        Map<Layer,Collection<Feature>> map = new HashMap<Layer,Collection<Feature>>();
        if (use_selected) {
            Collection<Layer> layers = context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
            for (Layer layer : layers) {
                map.put(layer, context.getLayerViewPanel()
                                      .getSelectionManager()
                                      .getFeaturesWithSelectedItems(layer));
            }
            return map;
        }
        else {
            map.put(context.getLayerManager().getLayer(layerName),
                    context.getLayerManager().getLayer(layerName)
                                             .getFeatureCollectionWrapper()
                                             .getFeatures());
            return map;
        }
    }

    /**
     * Find interior intersections in a collection of linestrings with the
     * MCIndexNoder. Interior intersections lies in the interior of at least one
     * segment.
     * This method is for detection only. It uses a floating point precision
     * model and as a consequence, it is not 100% robust.
     *
     * @param segmentStrings SegmentStrings to process
     * @param intersector SegmentIntersector to collect intersection information
     * @return a collection of nodes missing from the input
     */
    private FeatureCollection findInteriorIntersections(List<SegmentString> segmentStrings,
                                        IntersectionFinderAdder intersector) {
        Noder noder = getMCIndexNoder(intersector);
        noder.computeNodes(segmentStrings);
        List<Geometry> nodes = new ArrayList<Geometry>();
        for (Object node : intersector.getInteriorIntersections()) {
            nodes.add(gf.createPoint((Coordinate)node));
        }
        return FeatureDatasetFactory.createFromGeometry(nodes);
    }
    
    /**
     * Find intersections in a collection of linestrings with the MCIndexNoder. 
     * Intersections include proper intersections, and intersections located on
     * vertices of input LineStrings as long as these vertices are not end
     * points of two LineStrings.
     * This method is for detection only. It uses a floating point precision
     * model and as a consequence, it is not 100% robust.
     *
     * @param segmentStrings SegmentStrings to process
     * @param intersector SegmentIntersector to collect intersection information
     * @return a collection of nodes missing from the input
     */
    private FeatureCollection findIntersections(List<SegmentString> segmentStrings,
                                               IntersectionAdder intersector) {
    
        Noder noder = getMCIndexNoder(intersector);
        noder.computeNodes(segmentStrings);
        Set<Geometry> nodes = new HashSet<Geometry>();
        List<SegmentString> sss = (List<SegmentString>)noder.getNodedSubstrings();
        for (SegmentString ss : sss) {
            SegmentStringData data = (SegmentStringData)ss.getData();
            Geometry g = data.getSourceLineString();
            Coordinate[] ccss = ss.getCoordinates();
            Coordinate[] ccgeom = g.getCoordinates();
            if (!ccss[0].equals(ccgeom[0]) && !ccss[0].equals(ccgeom[ccgeom.length-1])) {
                nodes.add(gf.createPoint(ccss[0]));
            }
            if (!ccss[ccss.length-1].equals(ccgeom[0]) && !ccss[ccss.length-1].equals(ccgeom[ccgeom.length-1])) {
                nodes.add(gf.createPoint(ccss[ccss.length-1]));
            }
        }
        return FeatureDatasetFactory.createFromGeometry(nodes);
    }
        
    private List<Feature> nodeFeatures(
        Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap,
        boolean interpolate, int interpolated_z_dp) {
        List<Feature> list = new ArrayList<Feature>();
        for (Map.Entry<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> entry : geomStructureMap.entrySet()) {
            int dim = entry.getKey().getGeometry().getDimension();
            if ((dim == 1 && line_processor == Processor.NODE) || 
                (dim == 2 && polygon_processor == Processor.NODE)) {
                list.add(nodeFeature(entry.getKey(), entry.getValue(), 
                                     interpolate, interpolated_z_dp));
            }
        }
        return list;
    }
    
    // Build a new Feature from SegmentStrings
    private Feature nodeFeature(Feature feature,
            Map<Integer,Map<Integer,List<SegmentString>>> map,
            boolean interpolate_z, int interpolated_z_dp) {
        if (map == null) return null;
        Geometry g = SegmentStringsWithData2Features
                     .buildGeometry(feature.getGeometry(), map, 
                         interpolate_z, interpolated_z_dp);
        Feature newFeature = feature.clone(false);
        newFeature.setGeometry(g);
        return newFeature;
    }
    
    
    private List<Feature> splitLines(TaskMonitor monitor, 
                                     Collection nodedSubstring, 
                                     Map<Feature,Layer> featureToLayer, 
                                     Map<Layer,Collection<Feature>> outputFeatures) {
        
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.split-lines"));
        int count = 0, total = nodedSubstring.size();
        List<Feature> list = new ArrayList<Feature>();
        for (Object line : nodedSubstring) {
            SegmentString ss = (SegmentString)line;
            Coordinate[] cc = ss.getCoordinates();
            SegmentStringData metadata = (SegmentStringData)ss.getData();
            if (metadata.getFeature().getGeometry() instanceof Lineal) {
                cc = CoordinateArrays.atLeastNCoordinatesOrNothing(2, cc);
                if (cc.length > 1) {
                    Feature feature = metadata.getFeature().clone(false);
                    feature.setGeometry(gf.createLineString(cc));
                    list.add(feature);
                    outputFeatures.get(featureToLayer.get(metadata.getFeature())).add(feature);
                }
            }
            //else if (outputFeatures.get(featureToLayer.get(metadata.getFeature())).contains())
            monitor.report(++count, total, "");
        }
        return list;
    }
    
    private static STRtree indexSegmentStrings(Collection segmentStrings) {
        STRtree index = new STRtree();
        for (Object o : segmentStrings) {
            SegmentString ss = (SegmentString)o;
            index.insert(getEnvelope(ss), ss);
        }
        return index;
    }
    
    private static Envelope getEnvelope(SegmentString segmentString) {
         Envelope env = new Envelope();
         for (Coordinate c : segmentString.getCoordinates()) {
             env.expandToInclude(c);
         }
         return env;
    }
    
    /**
     * @param monitor the task monitor
     * @param geomStructureMap a Map with source features as keys and 
     * hierarchically organized noded segment strings as values
     * @param index index of noded segment strings 
     */
    private List<Feature> splitPolygons(TaskMonitor monitor,
            Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap,
            STRtree index,
            Map<Feature,Layer> featureToLayer, 
            Map<Layer,Collection<Feature>> outputFeatures) {
    
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.split-polygons"));
        int count = 0 , total = geomStructureMap.size();
        List<Feature> list = new ArrayList<Feature>();
        
        for (Map.Entry<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> entry : geomStructureMap.entrySet()) {
            Geometry geometry = entry.getKey().getGeometry();

            if (geometry.getDimension() == 2) {
                
                // If snap_rounding has been requested, segmentStrings have been
                // rounded during the snapping process, so that input geometry 
                // must also be rounded if we want the comparison to be fair.
                if (snap_rounding) {
                    CoordinatePrecisionReducerFilter filter = 
                        new CoordinatePrecisionReducerFilter(gf.getPrecisionModel());
                    geometry = (Geometry)geometry.clone();
                    geometry.apply(filter);
                    geometry.geometryChanged();
                }
                
                Polygonizer polygonizer = new Polygonizer();
                // Building a set will remove duplicates
                // (polygonize does not work with duplicate LineStrings in jts 1.12)
                Set<Geometry> uniqueCandidates = new HashSet<Geometry>();
                Collection<SegmentString> sourceSegmentStrings = new ArrayList<SegmentString>();
                // Add SegmentStrings issued from source geometry
                for (Map<Integer,List<SegmentString>> map : entry.getValue().values()) {
                    for (List<SegmentString> ssList : map.values()) {
                        for (SegmentString ss : ssList) {
                            sourceSegmentStrings.add(ss);
                            Geometry g = gf.createLineString(ss.getCoordinates());
                            uniqueCandidates.add(g.norm());
                        }
                    }
                }
                
                Collection  candidates = index.query(geometry.getEnvelopeInternal());
                // Add SegmentStrings not belonging to source geometry
                for (Object o : candidates) {
                    SegmentString ss = (SegmentString)o;
                    Geometry g = gf.createLineString(ss.getCoordinates());
                    uniqueCandidates.add(g.norm());
                }
                polygonizer.add(uniqueCandidates);
                Collection polys = polygonizer.getPolygons();
                
                for (Object o : polys) {
                    Geometry g = (Geometry)o;
                    if (!geometry.getEnvelopeInternal().contains(g.getEnvelopeInternal())) {
                        continue;
                    }
                    Point interiorPoint = g.getInteriorPoint();
                    // Warning : a robusteness problem found in interiorPoint function
                    // interiorPoint may lie on g boundary
                    // take the interiorPoint of an area computed as the the 
                    // intersection of g and first interiorPoint neighbourhood
                    if (interiorPoint.intersects(g.getBoundary())) {
                        interiorPoint = g.intersection(interiorPoint.buffer(g.getLength()/100.0)).getInteriorPoint();
                    }
                    if (interiorPoint.intersects(geometry)) {

                        Feature newFeature = entry.getKey().clone(false);
                        // Polygonization may have lost some original z, because
                        // Thanks to previous process, all intersection nodes
                        // should now have a z, but polygonization will keep
                        // only one z per node, which may blong to geometry A or
                        // geometry B
                        // resetZpoly will find the z on the source geometry
                        resetZpoly(g, sourceSegmentStrings);
                        newFeature.setGeometry(g);
                        list.add(newFeature);
                        outputFeatures.get(featureToLayer.get(entry.getKey())).add(newFeature);
                    }
                }
            }
            monitor.report(++count, total, "");
        }
        return list;
    }
    
    /** 
     * For SegmentStrings endpoints, transfer z of source SegmentStrings
     * to final geometry.
     */
    private static void resetZpoly(Geometry geometry, Collection edges) {
        
        for (Object o : edges) {
            SegmentString edge = (SegmentString)o;
            int dim = ((SegmentStringData)edge.getData()).getFeature().getGeometry().getDimension();
            if (dim == 2) {
                Coordinate[] cc_edge = edge.getCoordinates();
                Coordinate ci = cc_edge[0];
                Coordinate cf = cc_edge[cc_edge.length-1];
                Coordinate[] cc_g = geometry.getCoordinates();
                for (Coordinate c : cc_g) {
                    if (c.equals(ci)) c.z = ci.z;
                    if (c.equals(cf)) c.z = cf.z;
                }
            }
        }
    }
    
    private Map<Layer,Collection<Feature>> insertRemove(Map<Layer,Collection<Feature>> inputFeatures,
                Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap,
                Map<Layer,Collection<Feature>> outputFeatures) {
        for (Layer layer : inputFeatures.keySet()) {
            if (!layer.isEditable()) continue;
            outputFeatures.put(layer, new ArrayList<Feature>());
            for (Feature oldFeature : inputFeatures.get(layer)) {
                int dim = oldFeature.getGeometry().getDimension();
                if ((dim == 1 && line_processor == Processor.NODE) || 
                    (dim == 2 && polygon_processor == Processor.NODE)) {
                    Feature newFeature = nodeFeature(oldFeature, 
                        geomStructureMap.get(oldFeature), 
                        interpolate_z, interpolated_z_dp);
                    outputFeatures.get(layer).add(newFeature);
                } 
                if ((dim == 1 && line_processor == Processor.DO_NOT_PROCESS) || 
                    (dim == 2 && polygon_processor == Processor.DO_NOT_PROCESS)) {
                    outputFeatures.get(layer).add(oldFeature);
                }
            }
        }
        return outputFeatures;
    }

    
    private void commitUpdate(PlugInContext context, final Layer layer,
                 final Collection inputFeatures, final Collection newFeatures) {
        context.getLayerManager().getUndoableEditReceiver().reportNothingToUndoYet();
        UndoableCommand cmd = new UndoableCommand(getName()) {
            public void execute() {
                layer.getFeatureCollectionWrapper().removeAll(inputFeatures);
                layer.getFeatureCollectionWrapper().addAll(newFeatures);
            }
            
            public void unexecute() {
                layer.getFeatureCollectionWrapper().removeAll(newFeatures);
                layer.getFeatureCollectionWrapper().addAll(inputFeatures);
            }
        };
        boolean exceptionOccurred = true;
		try {
		    cmd.execute();
		    exceptionOccurred = false;
		} 
		finally {
		    if (exceptionOccurred) {
		        context.getLayerManager().getUndoableEditReceiver()
                       .getUndoManager().discardAllEdits();
            }
		}
		context.getLayerManager().getUndoableEditReceiver().receive(cmd.toUndoableEdit());
    }
      
    private void setDialogValues(final MultiInputDialog dialog, PlugInContext context) {
        int n = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems().size();
        dialog.setSideBarImage(IconLoader.icon("Noder.png"));
        dialog.setSideBarDescription(I18N.get("jump.plugin.edit.NoderPlugIn.sidebar-description"));
        dialog.addSubTitle(PROCESSED_DATA);
        final JComboBox addLayerComboBox = dialog.addLayerComboBox(SRC_LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
        // Hide the layer chooser component if some features are selected
        addLayerComboBox.setVisible(n == 0);
        dialog.getLabel(SRC_LAYER).setVisible(n == 0);
        final javax.swing.JLabel selectionLabel = dialog.addLabel(SELECTED_ONLY);
        // Hide the selection JLabel if no feature are selected 
        selectionLabel.setVisible(n > 0);
        
        dialog.addSeparator();
        dialog.addSubTitle(PROCESSING);
        dialog.addCheckBox(FIND_INTERSECTIONS, find_intersections, FIND_DESCRIPTION);
        dialog.addComboBox(LINE_OPTIONS, line_processor, 
            Arrays.asList(Processor.DO_NOT_PROCESS, Processor.NODE, Processor.SPLIT), "");
        dialog.addComboBox(POLYGON_OPTIONS, polygon_processor, 
            Arrays.asList(Processor.DO_NOT_PROCESS, Processor.NODE, Processor.SPLIT), "");
        
        dialog.addSeparator();
        dialog.addSubTitle(ADVANCED_OPTIONS);
        
        final JCheckBox snapRoundingCB = dialog.addCheckBox(SNAP_ROUNDING, snap_rounding, SNAP_ROUNDING_TOOLTIP);
        final JTextField snapRoundingDPTF = dialog.addIntegerField(SNAP_ROUNDING_DP, snap_rounding_dp, 6, DECIMAL_DIGITS_TOOLTIP);
        
        final JCheckBox interpolateZCB = dialog.addCheckBox(INTERPOLATE_Z, interpolate_z);
        final JTextField interpolateZDPTF = dialog.addIntegerField(INTERPOLATED_Z_DP, interpolated_z_dp, 6, DECIMAL_DIGITS_TOOLTIP);
        
        snapRoundingDPTF.setEnabled(snap_rounding);
        snapRoundingCB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dialog.setFieldEnabled(SNAP_ROUNDING_DP, snapRoundingCB.isSelected());
            }
        });

        interpolateZDPTF.setEnabled(interpolate_z);
        interpolateZCB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dialog.setFieldEnabled(INTERPOLATED_Z_DP, interpolateZCB.isSelected());
            }
        });
    }

    private void getDialogValues(MultiInputDialog dialog) {
        Layer layer = dialog.getLayer(SRC_LAYER);
        layerName = layer.getName();
        
        find_intersections = dialog.getBoolean(FIND_INTERSECTIONS);
        line_processor = (Processor)dialog.getComboBox(LINE_OPTIONS).getSelectedItem();
        polygon_processor = (Processor)dialog.getComboBox(POLYGON_OPTIONS).getSelectedItem();

        snap_rounding = dialog.getBoolean(SNAP_ROUNDING);
        snap_rounding_dp = dialog.getInteger(SNAP_ROUNDING_DP);        
        interpolate_z = dialog.getBoolean(INTERPOLATE_Z);
        interpolated_z_dp = dialog.getInteger(INTERPOLATED_Z_DP);
    }
  
}
