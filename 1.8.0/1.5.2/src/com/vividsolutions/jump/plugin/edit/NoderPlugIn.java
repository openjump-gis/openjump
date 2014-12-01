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
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.precision.CoordinatePrecisionReducerFilter;
import com.vividsolutions.jts.util.*;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

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
    private final static String CREATE_NEW_LAYER        = I18N.get("jump.plugin.edit.NoderPlugIn.create-new-layer");
    private final static String UPDATE_SELECTED_FEATURES= I18N.get("jump.plugin.edit.NoderPlugIn.update-selected-features");
    
    private final static String FIND_INTERSECTIONS      = I18N.get("jump.plugin.edit.NoderPlugIn.find-intersections");
    private final static String FIND_DESCRIPTION        = I18N.get("jump.plugin.edit.NoderPlugIn.create-new-layer-with-missing-intersections");
                                                        
    private final static String LINE_OPTIONS            = I18N.get("jump.plugin.edit.NoderPlugIn.line-options");
    private final static String DO_NOT_PROCESS_LINES    = I18N.get("jump.plugin.edit.NoderPlugIn.do-not-process-lines");
    private final static String NODE_LINES              = I18N.get("jump.plugin.edit.NoderPlugIn.node-lines");
    private final static String SPLIT_LINES             = I18N.get("jump.plugin.edit.NoderPlugIn.split-lines");
                                                        
    private final static String POLYGON_OPTIONS         = I18N.get("jump.plugin.edit.NoderPlugIn.polygon-options");
    private final static String DO_NOT_PROCESS_POLYGONS = I18N.get("jump.plugin.edit.NoderPlugIn.do-not-process-polygons");
    private final static String NODE_POLYGONS           = I18N.get("jump.plugin.edit.NoderPlugIn.node-polygons");
    private final static String SPLIT_POLYGONS          = I18N.get("jump.plugin.edit.NoderPlugIn.split-polygons");
                                                        
    private final static String ADVANCED_OPTIONS        = I18N.get("jump.plugin.edit.NoderPlugIn.advanced-options");
    private final static String NODING_METHOD           = I18N.get("jump.plugin.edit.NoderPlugIn.noding-method");
    private final static String SNAP_ROUNDING           = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding");
    private final static String SNAP_ROUNDING_TOOLTIP   = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding-makes-noding-algorithm-fully-robust");
    private final static String SNAP_ROUNDING_DP        = I18N.get("jump.plugin.edit.NoderPlugIn.snap-rounding-decimal-places");
    private final static String DECIMAL_DIGITS_TOOLTIP  = I18N.get("jump.plugin.edit.NoderPlugIn.number-of-decimal-digits");
    
    private final static String INTERPOLATE_Z           = I18N.get("jump.plugin.edit.NoderPlugIn.interpolate-z");
    private final static String INTERPOLATED_Z_DP       = I18N.get("jump.plugin.edit.NoderPlugIn.interpolated-z-decimal-places");
    
    private final static String INTERSECTIONS           = I18N.get("jump.plugin.edit.NoderPlugIn.intersections");
    private final static String NODED                   = I18N.get("jump.plugin.edit.NoderPlugIn.noded");
    
    private boolean useSelected = false;
    private boolean create_new_layer = true;
    private boolean update_selected_features = false;
    private String layerName;
    private GeometryFactory gf;
    private FeatureSchema schema_preserve_attributes;
    private FeatureSchema schema;
    
    private boolean snap_rounding = false;
    int snap_rounding_dp = 6;
    
    private boolean find_intersections = true;
    private String line_processing = SPLIT_LINES;
    private boolean do_not_process_lines;
    private boolean node_lines = true;
    private boolean split_lines;
    private String polygon_processing = NODE_POLYGONS;
    private boolean do_not_process_polygons;
    private boolean node_polygons = true;
    private boolean split_polygons;
    
    private boolean interpolate_z = false;
    private int interpolated_z_dp  = 3;
    
    private static final RobustLineIntersector ROBUST_INTERSECTOR = new RobustLineIntersector();

    public NoderPlugIn() { }
  
    public void initialize(PlugInContext context) throws Exception {
        	FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    		featureInstaller.addMainMenuItem(
              new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},
              this, new JMenuItem(getName() + "..."),
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

    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.noding-input"));
        
        final Layer layer = context.getLayerManager().getLayer(layerName);
        
        // Feature collection to process and GeometryFactory initialisation
        Collection inputFeatures = getFeaturesToProcess(layer, context);
        if (inputFeatures.isEmpty()) {
            context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-data-to-process"));
            return;
        }
        schema = ((Feature)inputFeatures.iterator().next()).getSchema();
        // Now we are sure there is at least one feature to process
        
        // Create a GeometryFactory consistent with the snap_rounding parameters 
        if (snap_rounding) {
            gf = new GeometryFactory(
                new PrecisionModel(Math.pow(10.0, (double)snap_rounding_dp)));
        }
        else {
            gf = ((Feature)inputFeatures.iterator().next()).getGeometry().getFactory();
        }
        
        monitor.report(I18N.get("jump.plugin.edit.NoderPlugIn.noding"));
        
        // Segments strings are extracted from the input dataset, keeping a
        // link to their parent feature
        List<SegmentString> segmentStrings = 
            Features2SegmentStringsWithData.getSegmentStrings(inputFeatures);
        
        // If inputFeatures contains only 0-dim features, segmentString will be
        // empty and the process should stop !
        if (segmentStrings.isEmpty()) {
            context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-data-to-process"));
            return;
        }
        
        // If the user does not want to split features, find intersections will
        // only find intersections if a vertex is missing (intersections
        // located in the interior of a segment).
        // ==> use IntersectionFinderAdder
        if (find_intersections && (!split_lines) && (!split_polygons)) {
            IntersectionFinderAdder intersector = new IntersectionFinderAdder(ROBUST_INTERSECTOR);
            FeatureCollection nodes = findInteriorIntersections(segmentStrings, intersector);
            context.addLayer(StandardCategoryNames.RESULT, 
                            layerName + " " + INTERSECTIONS, nodes);
        }
        
        // If the user wants to split features (either linestring or polygons), 
        // find intersections will return all intersections located in the 
        // interior of linear elements (including existing vertices as long as
        // they are not a linestring endpoint).
        // ==> use IntersectionAdder
        else if (find_intersections) {
            IntersectionAdder intersector = new IntersectionAdder(ROBUST_INTERSECTOR);
            FeatureCollection nodes = findIntersections(segmentStrings, intersector);
            context.addLayer(StandardCategoryNames.RESULT, 
                            layerName + " " + INTERSECTIONS, nodes);
        }
        
        // If neither process lines nor process polygons is checked, do nothing
        if (do_not_process_lines && do_not_process_polygons) {
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
            FeatureCollection fc = new FeatureDataset(schema);
            final Collection<Feature> updatedFeatures = new ArrayList<Feature>();
            // Node lines and/or polygons and interpolate z as needed
            // note : nodeFeatures method select lines and/or polygons according
            // to node_lines and node_polygons option
            if (node_lines || node_polygons) {
                if (create_new_layer) {
                    fc.addAll(nodeFeatures(geomStructureMap, interpolate_z, interpolated_z_dp));
                }
                else if (update_selected_features) {
                    updatedFeatures.addAll(insertRemove(inputFeatures, geomStructureMap));
                    // commit update happens later
                }
            }
            
            // If split AND interpolation is wanted, we need to
            // - merge noded SegmentStrings for interpolation
            // - interpolate
            // - split again merged strings into SegmentStrings
            if (interpolate_z && (split_lines || split_polygons)) {
                for (Map.Entry<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> entry : geomStructureMap.entrySet()) {
                    Geometry g = entry.getKey().getGeometry();
                    int dim = g.getDimension();
                    if ((dim == 1 && split_lines) || (dim == 2 && split_polygons)) {
                        SegmentStringsWithData2Features.buildGeometry(g, 
                            entry.getValue(), true, interpolated_z_dp);
                    }
                }
            }
            
            // Split lines and/or polygons either with interpolated z or not
            if (split_lines || split_polygons) {
                if (create_new_layer) {
                    if (split_lines) {
                        fc.addAll(splitLines(monitor, nodedSubstring));
                    }
                    if (split_polygons) {
                        STRtree index = indexSegmentStrings(nodedSubstring);
                        fc.addAll(splitPolygons(monitor, geomStructureMap, index));
                    }
                }
                else if (update_selected_features) {
                    insertRemove(inputFeatures, geomStructureMap);
                    if (split_lines) {
                        updatedFeatures.addAll(splitLines(monitor, nodedSubstring));
                    }
                    if (split_polygons) {
                        STRtree index = indexSegmentStrings(nodedSubstring);
                        updatedFeatures.addAll(splitPolygons(monitor, geomStructureMap, index));
                    }
                }
            }
            if (update_selected_features) {
                commitUpdate(context, layer, inputFeatures, updatedFeatures);
            }
            else if (fc.size() > 0) {
                context.addLayer(
                    StandardCategoryNames.RESULT, layerName + " " + NODED, fc
                );
            } else context.getWorkbenchFrame().warnUser(I18N.get("jump.plugin.edit.NoderPlugIn.no-output-data"));
        }
        
        if (monitor.isCancelRequested()) return;
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
    
    private Collection getFeaturesToProcess(Layer lyr, PlugInContext context){
        if (useSelected)
          return context.getLayerViewPanel()
                            .getSelectionManager().getFeaturesWithSelectedItems(lyr);
        return lyr.getFeatureCollectionWrapper().getFeatures();
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
            if ((dim == 1 && node_lines) || (dim == 2 && node_polygons)) {
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
    
    
    private List<Feature> splitLines(TaskMonitor monitor, Collection nodedSubstring) {
        
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
                    Feature feature = (Feature)metadata.getFeature().clone(false);
                    feature.setGeometry(gf.createLineString(cc));
                    list.add(feature);
                }
            }
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
    
    private List<Feature> splitPolygons(TaskMonitor monitor,
            Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap,
            STRtree index) {
    
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
                    Point interiorPoint = g.getInteriorPoint();
                    if (interiorPoint.intersects(geometry)) {
                        // Warning : a robusteness problem found in interiorPoint
                        // function sometimes lying on the boundary
                        if (interiorPoint.intersects(geometry.getBoundary())) continue;
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
        //System.out.println("process " + geometry);
        //System.out.println("process " + geometry.getCoordinates());
        for (Object o : edges) {
            
            SegmentString edge = (SegmentString)o;
            //System.out.println("    compare to " + java.util.Arrays.toString(g.getCoordinates()));
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
    
    private Collection<Feature> insertRemove(Collection inputFeatures,
        Map<Feature,Map<Integer,Map<Integer,List<SegmentString>>>> geomStructureMap) {
        Collection<Feature> newFeatures = new ArrayList<Feature>();
        Collection featuresToRemove = new ArrayList();
        for (Object o : inputFeatures) {
            Feature oldFeature = (Feature)o;
            int dim = oldFeature.getGeometry().getDimension();
            if ((dim == 1 && node_lines) || (dim == 2 && node_polygons)) {
                Feature newFeature = nodeFeature(oldFeature, 
                    geomStructureMap.get(oldFeature), 
                    interpolate_z, interpolated_z_dp);
                newFeatures.add(newFeature);
            } 
            if ((dim == 1 && do_not_process_lines) || (dim == 2 && do_not_process_polygons)) {
                featuresToRemove.add(oldFeature);
            }
        }
        inputFeatures.removeAll(featuresToRemove);
        return newFeatures;
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
        JComboBox addLayerComboBox = dialog.addLayerComboBox(SRC_LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
        final JCheckBox selectedOnlyCB = dialog.addCheckBox(SELECTED_ONLY, useSelected);
        selectedOnlyCB.setEnabled(n > 0);
        
        dialog.addSeparator();
        dialog.addSubTitle(PROCESSING);
        final JRadioButton createNewRB = dialog.addRadioButton(CREATE_NEW_LAYER, "Result", create_new_layer, "");
        final JRadioButton updateRB = dialog.addRadioButton(UPDATE_SELECTED_FEATURES, "Result", update_selected_features, "");
        updateRB.setEnabled(n > 0);
        if (!useSelected) updateRB.setSelected(false);
        selectedOnlyCB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                updateRB.setEnabled(selectedOnlyCB.isSelected());
                if (!selectedOnlyCB.isSelected()) createNewRB.setSelected(true);
            }
        });
        updateRB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (updateRB.isSelected()) {
                    selectedOnlyCB.setSelected(true);
                }
            }
        });        

        dialog.addSeparator();
        dialog.addCheckBox(FIND_INTERSECTIONS, find_intersections, FIND_DESCRIPTION);
        dialog.addComboBox(LINE_OPTIONS, line_processing, 
            Arrays.asList(new String[]{DO_NOT_PROCESS_LINES, NODE_LINES, SPLIT_LINES}), "");
        dialog.addComboBox(POLYGON_OPTIONS, polygon_processing, 
            Arrays.asList(new String[]{DO_NOT_PROCESS_POLYGONS, NODE_POLYGONS, SPLIT_POLYGONS}), "");
        
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
                //dialog.setFieldEnabled(INTERPOLATE_Z, !snapRoundingCB.isSelected());
                //dialog.setFieldEnabled(INTERPOLATED_Z_DP, !snapRoundingCB.isSelected());
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
        useSelected = dialog.getBoolean(SELECTED_ONLY);
        create_new_layer = dialog.getBoolean(CREATE_NEW_LAYER);
        update_selected_features = dialog.getBoolean(UPDATE_SELECTED_FEATURES);
        
        find_intersections = dialog.getBoolean(FIND_INTERSECTIONS);
        
        line_processing = dialog.getText(LINE_OPTIONS);
        do_not_process_lines = line_processing == DO_NOT_PROCESS_LINES;
        node_lines = line_processing == NODE_LINES;
        split_lines = line_processing == SPLIT_LINES;
        
        polygon_processing = dialog.getText(POLYGON_OPTIONS);
        do_not_process_polygons = polygon_processing == DO_NOT_PROCESS_POLYGONS;
        node_polygons = polygon_processing == NODE_POLYGONS;
        split_polygons = polygon_processing == SPLIT_POLYGONS;

        snap_rounding = dialog.getBoolean(SNAP_ROUNDING);
        snap_rounding_dp = dialog.getInteger(SNAP_ROUNDING_DP);        
        interpolate_z = dialog.getBoolean(INTERPOLATE_Z);
        interpolated_z_dp = dialog.getInteger(INTERPOLATED_Z_DP);
    }
  
}
