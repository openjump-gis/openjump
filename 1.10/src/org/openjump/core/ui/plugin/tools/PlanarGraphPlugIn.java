package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.workbench.ui.GUIUtil;
import org.openjump.sigle.utilities.geom.FeatureCollectionUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * PlanarGraphPlugIn computes a planar graph from a set of features.
 * The user can choose to produce the nodes, the edges and the faces, or only
 * some of those features.
 * The following relations are kept as edge attributes :<br>
 *     Initial node identifier<br>
 *     Final node identifier<br>
 *     Right face<br>
 *     Left face<br>
 * @author Michael Michaud and Erwan Bocher (2005-06)
 * Internationalized by Stefan Steiniger
 * Comments added by Michael Michaud on 2006-05-01
 */
public class PlanarGraphPlugIn extends ThreadedBasePlugIn {
    
    private final static String EDGE = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Edge");
    private final static String FACE = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Face");
    private final static String NODE = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Node");
    private final static String CATEGORY = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Graph");
    private final static String MAPPING  = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Mapping");
    
    private final static String TITLE               = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Topologic-Analysis");
    private final static String SELECT_LAYER        = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Select-layer-to-analyse");
    private final static String CALCULATE_NODES     = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Calculate-nodes");
    private final static String CALCULATE_FACES     = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Calculate-faces");
    private final static String CALCULATE_RELATIONS = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Calculate-the-relations-arcs-nodes-and-/or-arcs-faces");
    private final static String KEEP_ATTRIBUTES     = I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Keep-attributes");
    
    private final static Integer MINUS_ONE          = -1;
    
    private GeometryFactory gf = new GeometryFactory();
    
    // Do you want to compute nodes
    private static boolean nodeb = true;
    // Do you want to compute faces
    private static boolean faceb = true;
    // Do you want to compute edge/node relations and/or edges/faces relations
    private static boolean relb  = true;
    // Do you want to keep original attributes
    private static boolean attributesb = true;
    
    // Attribute names have not been internationalized
    private static String LEFT_FACE = "LeftFace";
    private static String RIGHT_FACE = "RightFace";
    private static String INITIAL_NODE = "StartNode";
    private static String FINAL_NODE = "EndNode";
    
    private String layerName;
    
    public Collection edges;

    private MultiInputDialog mid;

    /**
     * Calculations take place here
     */
    public void run(TaskMonitor monitor, PlugInContext context)
        throws Exception {
        
        // Faces FeatureCollection declaration
        FeatureCollection fcFace;
    
        // Getting options from the dialog
        Layer layer = mid.getLayer(SELECT_LAYER);
        FeatureCollection fcSource = layer.getFeatureCollectionWrapper();
        layerName = layer.getName();
        nodeb = mid.getBoolean(CALCULATE_NODES);
        faceb = mid.getBoolean(CALCULATE_FACES);
        relb = mid.getBoolean(CALCULATE_RELATIONS);
        attributesb = mid.getBoolean(KEEP_ATTRIBUTES); 
        
        // Get linear elements from all geometries in the layer
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Searching-for-linear-elements"));
        List list = getLines(fcSource);
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Number-of-found-elements") + ": " + list.size());
        
        // Union the lines (unioning is the most expensive operation)
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Generate-layer-of-arcs"));
        FeatureCollection fcEdge = createEdgeLayer(
            layer.getFeatureCollectionWrapper(), nodeb, faceb, relb);
        if (fcEdge.size() > 0) {
            context.getLayerManager().addLayer(CATEGORY, layerName + "_" + EDGE, fcEdge);
        } else {context.getWorkbenchFrame().warnUser("No edge found");}
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Arc-layer-generated"));
        
        // Create the node Layer
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Create-nodes"));
        if (nodeb) {
            FeatureCollection fcNode = createNodeLayer(fcEdge, relb);
            if (fcNode.size() > 0) {
                context.getLayerManager().addLayer(CATEGORY, layerName + "_" + NODE, fcNode);
            } else {context.getWorkbenchFrame().warnUser("No node found");}
        }
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Layer-with-nodes-generated"));
        
        // Create face Layer from edges with Polygonizer
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Create-faces"));
        //if (faceb) {
        //    fcFace = createFaceLayer(fcEdge, context, relb);
        //}
        monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Layer-of-faces-generated"));
    
        //Erwan aout 2005
        // Here, one process the result dataset to get attributes from source layer
        // Attributes are transferred if the primitive is included in the source feature
        // If the source layer is made of polygons, attributes are transferred to faces
        // If the source layer is made of linestrings, attributes are transferred to edges
       
        if (faceb){
            fcFace = createFaceLayer(fcEdge, relb);
            Feature fWithin = null;
            AttributeMapping mapping;
            
            if (attributesb) {
                // Use mapping to get the attributes
                mapping = new AttributeMapping(new FeatureSchema(), new FeatureSchema());
                List<Feature> aFeatures = new ArrayList<>();
                monitor.report(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Transfer-of-attributes"));
                if (FeatureCollectionUtil.getFeatureCollectionDimension(fcSource)==2){
                    mapping = new AttributeMapping(fcSource.getFeatureSchema(), fcFace.getFeatureSchema());
                    aFeatures = fcFace.getFeatures();
                }
                else if (FeatureCollectionUtil.getFeatureCollectionDimension(fcSource)==1) {
                    mapping = new AttributeMapping(fcSource.getFeatureSchema(), fcFace.getFeatureSchema());
                    aFeatures = fcEdge.getFeatures();
                }
                else {context.getWorkbenchFrame().warnUser(
                    I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Cannot-transfer-attributes"));
                }
                        
                FeatureDataset fcRecup = new FeatureDataset(mapping.createSchema("GEOMETRY"));
                IndexedFeatureCollection indexedB = new IndexedFeatureCollection(fcSource);
            
                for (Feature aFeature : aFeatures) {
                    Feature feature = new BasicFeature(fcRecup.getFeatureSchema());
                    int nbFeatureWithin = 0;
                    for (Iterator j = indexedB.query(aFeature.getGeometry().getEnvelopeInternal()).iterator();
                        j.hasNext() && !monitor.isCancelRequested();) {
                        
                        Feature bFeature = (Feature) j.next();
                        if (aFeature.getGeometry().within(bFeature.getGeometry())) {
                            nbFeatureWithin++;
                            fWithin = bFeature;
                        }
                    }
                    // Attributes are copied if the resulting geometry is contained
                    // in one source geometry
                    if (nbFeatureWithin == 1 && attributesb) {
                        mapping.transferAttributes(fWithin, aFeature, feature);
                    }
                    // Resulting geometry is cloned
                    feature.setGeometry((Geometry) aFeature.getGeometry().clone()); 
                    fcRecup.add(feature);
                }
                if (fcRecup.size() > 0) {
                    context.getLayerManager().addLayer(CATEGORY, layerName + "_" + MAPPING, fcRecup);
                }
            }
            context.getLayerManager().addLayer(CATEGORY, layerName + "_" + FACE, fcFace);
        }
    }         

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY, MenuNames.CONVERT},
                this.getName(), false, null, 
                new MultiEnableCheck()
                        .add(new EnableCheckFactory(context.getWorkbenchContext()).createTaskWindowMustBeActiveCheck())
                        .add(new EnableCheckFactory(context.getWorkbenchContext()).createAtLeastNLayersMustExistCheck(1))
                ); 
    }
   
    public boolean execute(PlugInContext context) throws Exception {
        initDialog(context);
        mid.setVisible(true);
        mid.wasOKPressed();
        return mid.wasOKPressed();
    }

    public String getName(){
    	return I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.Planar-Graph") + "...";	
    }
    
    private void initDialog(PlugInContext context) {

        mid = new MultiInputDialog(context.getWorkbenchFrame(), TITLE, true);
        Layer src_layer;
        if (layerName == null || context.getLayerManager().getLayer(layerName) == null) {
            src_layer = context.getCandidateLayer(0);
        } 
        else {
            src_layer = context.getLayerManager().getLayer(layerName);
        }
        mid.addLayerComboBox(SELECT_LAYER, src_layer, context.getLayerManager());
        mid.addLabel(I18N.get("org.openjump.sigle.plugin.PlanarGraphPlugIn.The-layer-of-arcs-is-always-generated"));
        mid.addCheckBox(CALCULATE_NODES, nodeb);
        mid.addCheckBox(CALCULATE_FACES, faceb);
        mid.addCheckBox(CALCULATE_RELATIONS, relb);
        mid.addCheckBox(KEEP_ATTRIBUTES, attributesb);
        mid.pack();
        GUIUtil.centreOnWindow(mid);
    }

    // ************************************************
    // extract lines from a feature collection
    // ************************************************
    private List getLines(FeatureCollection fc) {
        List linesList = new ArrayList();
        LinearComponentExtracter filter = new LinearComponentExtracter(linesList);
        for (Feature feature : fc.getFeatures()) {
            Geometry g = feature.getGeometry();
            g.apply(filter);
        }
        return linesList;
    }
    
    // ************************************************
    // Create edge layer
    // ************************************************
    // public (used in external plugin) TODO such public method should not be directly in the PlugIn
    public FeatureCollection createEdgeLayer(FeatureCollection fc,
        boolean nodeb, boolean faceb, boolean relations) {
        // Schema edge
        FeatureSchema fsEdge = new FeatureSchema();
          fsEdge.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
          fsEdge.addAttribute("ID", AttributeType.INTEGER);
          // Edge - Node relation
          if (nodeb && relations) {
              fsEdge.addAttribute(INITIAL_NODE, AttributeType.INTEGER);
              fsEdge.addAttribute(FINAL_NODE, AttributeType.INTEGER);
          }
          // Edge - Face relation
          if (faceb && relations) {
              fsEdge.addAttribute(RIGHT_FACE, AttributeType.INTEGER);
              fsEdge.addAttribute(LEFT_FACE, AttributeType.INTEGER);
          }
        FeatureDataset fcEdge = new FeatureDataset(fsEdge);
        
        // Get linear elements from all geometries in the layer
        List list = getLines(fc);
        
        // Union the lines (unioning is the most expensive operation)
        // fixed on 2008-11-10 by mmichaud, using the new UnaryUnionOp
        Geometry gc = new UnaryUnionOp(list).union();
        if (!(gc instanceof GeometryCollection)) {
            gc = gf.createGeometryCollection(new Geometry[]{gc});
        }
        
        // Create the edge layer by merging lines between 3+ order nodes
        // (Merged lines are multilines)
        LineMerger lineMerger = new LineMerger();
        for (int i = 0 ; i < gc.getNumGeometries() ; i++) {
            lineMerger.add(gc.getGeometryN(i));
        }
        edges = lineMerger.getMergedLineStrings();
        int no = 0;
        for (Iterator it = edges.iterator() ; it.hasNext() ;) {
            Feature f = new BasicFeature(fsEdge);
            f.setGeometry((Geometry)it.next());
            f.setAttribute("ID", ++no);
            fcEdge.add(f);
        }
        return fcEdge;
    }
    
    // ************************************************
    // Create node layer
    // ************************************************
    // public (used in external plugin) TODO such public method should not be directly in the PlugIn
    public FeatureCollection createNodeLayer(FeatureCollection fcEdge, boolean relations) {
        FeatureSchema fsNode = new FeatureSchema();
        fsNode.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fsNode.addAttribute("ID", AttributeType.INTEGER);
        FeatureDataset fcNode = new FeatureDataset(fsNode);
        
        // Create the node Layer
        Map<Coordinate,Geometry> gNodes = new HashMap<>();
        for (Iterator it = edges.iterator() ; it.hasNext() ;) {
            Coordinate[] cc = ((Geometry)it.next()).getCoordinates();
            gNodes.put(cc[0], gf.createPoint(cc[0]));
            gNodes.put(cc[cc.length-1], gf.createPoint(cc[cc.length-1]));
        }
        int no = 0;
        Map<Coordinate,Feature> fNodes = new HashMap<>();
        for (Geometry point : gNodes.values()) {
            Feature f = new BasicFeature(fsNode);
            f.setGeometry(point);
            f.setAttribute("ID", ++no);
            fNodes.put(f.getGeometry().getCoordinate(), f);
            fcNode.add(f);
        }
        //context.getLayerManager().addLayer(CATEGORY, layerName + "_" + NODE, fcNode);
        
        // Compute the relation between edges and nodes
        if (relations) {
            for (Feature f : fcEdge.getFeatures()) {
                Coordinate[] cc = f.getGeometry().getCoordinates();
                f.setAttribute(INITIAL_NODE, (fNodes.get(cc[0])).getAttribute("ID"));
                f.setAttribute(FINAL_NODE, (fNodes.get(cc[cc.length-1])).getAttribute("ID"));
            }
        }
        return fcNode;
    }
    
    // ************************************************
    // Create face layer
    // ************************************************
    // public (used in external plugin) TODO such public method should not be directly in the PlugIn
    public FeatureCollection createFaceLayer(FeatureCollection fcEdge, boolean relations) {
        // Create the face layer
        FeatureSchema fsFace = new FeatureSchema();
        fsFace.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fsFace.addAttribute("ID", AttributeType.INTEGER);
        FeatureCollection fcFace = new FeatureDataset(fsFace);
        
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(edges);
        int no = 0;
        for (Iterator it = polygonizer.getPolygons().iterator() ; it.hasNext() ;) {
            Feature f = new BasicFeature(fsFace);
            Geometry face = (Geometry)it.next();
            face.normalize();    // add on 2007-08-11
            f.setGeometry(face);
            f.setAttribute("ID", ++no);
            fcFace.add(f);
        }
        //context.getLayerManager().addLayer("Graph", layerName+"_Face", fcFace);
        
        // write face identifiers into edges
        // if there is no face on the side of an edge, id number is -1
        if(relations) {
            for (Feature edge : fcEdge.getFeatures()) {
                // Fix added on 2007-07-09 [mmichaud]
                edge.setAttribute(RIGHT_FACE, MINUS_ONE);
                edge.setAttribute(LEFT_FACE, MINUS_ONE);
                Geometry g1 = edge.getGeometry();
                List list = fcFace.query(g1.getEnvelopeInternal());
                for (int i = 0 ; i < list.size() ; i++) {
                    Feature face = (Feature)list.get(i);
                    labelEdge(edge, face);
                }
            }
        }
        return fcFace;
    }
    
    private void labelEdge(Feature edge, Feature face) {
        IntersectionMatrix im = edge.getGeometry().relate(face.getGeometry());
        // intersection between boundaries has dimension 1
        if (im.matches("*1*******")) {
            Coordinate c0 = edge.getGeometry().getCoordinates()[0];
            Coordinate c1 = edge.getGeometry().getCoordinates()[1];
            if (hasSameOrientation(c0, c1, face.getGeometry())) {
                edge.setAttribute(RIGHT_FACE, face.getAttribute("ID"));
            }
            else {
                edge.setAttribute(LEFT_FACE, face.getAttribute("ID"));
            }
        }
        // intersection between the line and the polygon interior has dimension 1
        else if (im.matches("1********")) {
            edge.setAttribute(RIGHT_FACE, face.getAttribute("ID"));
            edge.setAttribute(LEFT_FACE, face.getAttribute("ID"));
        }
        // intersection between the line and the polygon exterior has dimension 1
        // else if (im.matches("F********")) {}
    }
    
    // Returns true if c0-c1 has the same orientation as g boundary
    // Returns false else if
    private boolean hasSameOrientation(Coordinate c0, Coordinate c1, Geometry g) {
        assert g instanceof Polygon;
        assert !c0.equals(c1);
        
        int index0 = -1;
        int index1 = -1;
        
        LineString ring = ((Polygon)g).getExteriorRing();
        Coordinate[] cc = ring.getCoordinates();
        for (int i = 0 ; i < cc.length ; i++) {
            if (index0 == -1 && cc[i].equals(c0)) index0 = i;
            if (index1 == -1 && cc[i].equals(c1)) index1 = i;
            if (index0 > -1 && index1 > -1) {
                if (Math.abs(index1-index0) == 1) return index1 > index0;
                else return index0 > index1;
            }
        }
        for (int i = 0 ; i < ((Polygon)g).getNumInteriorRing() ; i++) {
            LineString hole = ((Polygon)g).getInteriorRingN(i);
            index0 = -1;
            index1 = -1;
            cc = hole.getCoordinates();
            for (int j = 0 ; j < cc.length ; j++) {
                if (index0 == -1 && cc[j].equals(c0)) index0 = j;
                if (index1 == -1 && cc[j].equals(c1)) index1 = j;
                if (index0 > -1 && index1 > -1) {
                    if (Math.abs(index1-index0) == 1) return index1 > index0;
                    else return index0 > index1;
                }
            }
        }
        return false;
    }
    
}


