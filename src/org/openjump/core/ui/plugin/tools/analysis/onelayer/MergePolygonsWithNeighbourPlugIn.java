package org.openjump.core.ui.plugin.tools.analysis.onelayer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import javax.swing.*;
import java.util.*;

public class MergePolygonsWithNeighbourPlugIn extends ThreadedBasePlugIn {

  private String sMergeTwoPolys = "Merge Selected Polygons with Neighbours (v2)";
  private String sFeaturesFromDifferentLayer = "Error: Features from different layers!";
  private String sSidebar = "Merges selected polygons with neighboring polygons, either with the one that is largest of " +
          "all neighbors, or the one with which it has " +
          "the longest common boundary. Note, the function may return multi-polygons if " +
          "the polygons to merge have only one point in common.";
  boolean useArea = true;
  boolean useBorder = false;
  String sUseArea = "merge with neighbor that has the largest area";
  String sUseBoder = "merge with neighbor with the longest common edge";
  String sChoseMergeMethod = "Please chose the merge method:";
  String sMerged = "merged";
  String sSearchingForMergeCandidates = "Searching for merge candidates...";
  String sMergingPolygons = "Merging polygons...";
  boolean useAttribute = false;
  String sUseAttribute = "Use an attribute";
  String sUseAttributeTooltip = "Merge features with same attribute value only";
  String sAttributeToUse = "Attribute to use";
  String attribute = null;
  String layerName;
  final static String sMERGEMETHOD = "MERGE METHOD";

  private MultiInputDialog dialog;

  public void initialize(PlugInContext context) throws Exception {

    sMergeTwoPolys = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merge-Selected-Polygons-with-Neighbours");
    sFeaturesFromDifferentLayer = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.features-from-different-layers");
    sSidebar = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.description");
    sUseArea = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-that-has-the-largest-area");
    sUseBoder = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-with-the-longest-common-edge");
    sChoseMergeMethod = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Please-chose-the-merge-method");
    sMerged = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merged");
    sSearchingForMergeCandidates = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Searching-for-merge-candidates");
    sMergingPolygons = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merging-polygons");
    sUseAttribute = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Use-attribute");
    sUseAttributeTooltip = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Use-attribute-tooltip");
    sAttributeToUse = I18N.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Attribute");

    FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
    featureInstaller.addMainMenuPlugin(
            this,                //exe
            new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},  //menu path
            this.getName() + "...", //name methode .getName recieved by AbstractPlugIn
            false,      //checkbox
            null,      //icon
            createEnableCheck(context.getWorkbenchContext())); //enable check
  }

  public String getName() {
    return sMergeTwoPolys;
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

    return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))
            .add(checkFactory.createSelectedItemsLayersMustBeEditableCheck());
  }

  public boolean execute(PlugInContext context) throws Exception {

    Collection<Layer> layers = context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
    if (layers.size() != 1) {
      context.getWorkbenchFrame().warnUser(sFeaturesFromDifferentLayer);
      return false;
    }
    layerName = layers.iterator().next().getName();

    initDialog(context);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) {
      return false;
    } else {
      this.getDialogValues(dialog);
    }
    return true;
  }

  private void initDialog(PlugInContext context) {

    dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
    dialog.setSideBarDescription(sSidebar);
    final String METHODGROUP = sMERGEMETHOD;
    dialog.addLabel(sChoseMergeMethod);
    dialog.addRadioButton(sUseArea, METHODGROUP, this.useArea, sUseArea);
    dialog.addRadioButton(sUseBoder, METHODGROUP, this.useBorder, sUseBoder);
    JCheckBox jcbUseAttribute = dialog.addCheckBox(sUseAttribute, useAttribute);
    List<String> attributes = AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(context.getLayerManager()
            .getLayer(layerName));
    jcbUseAttribute.setEnabled(!attributes.isEmpty());
    if (!jcbUseAttribute.isEnabled()) jcbUseAttribute.setSelected(false);
    JComboBox<String> jcbAttribute = dialog.addComboBox(sAttributeToUse, attribute, attributes, null);
    jcbAttribute.setEnabled(jcbUseAttribute.isSelected());
    jcbUseAttribute.addActionListener(e -> jcbAttribute.setEnabled(jcbUseAttribute.isSelected()));
    GUIUtil.centreOnWindow(dialog);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    this.useArea = dialog.getBoolean(this.sUseArea);
    this.useBorder = dialog.getBoolean(this.sUseBoder);
    if (dialog.getBoolean(sUseAttribute)) attribute = (String)dialog.getComboBox(sAttributeToUse).getSelectedItem();
    else attribute = null;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

    monitor.allowCancellationRequests();
    context.getWorkbenchContext().getLayerManager().setFiringEvents(false);

    Layer activeLayer = context.getLayerManager().getLayer(layerName);

    FeatureCollection fc = activeLayer.getFeatureCollectionWrapper();
    STRtree index = new STRtree();
    monitor.report("Indexing...");
    for (Feature f : fc.getFeatures()) {
      index.insert(f.getGeometry().getEnvelopeInternal(), f);
    }

    Collection<Feature> selection = context.getWorkbenchContext().getLayerViewPanel()
            .getSelectionManager().getFeaturesWithSelectedItems();

    Map<Integer,Set<Integer>> graph = getPolygonGraph(index, selection, monitor);
    // ids of a all features which may change
    Set<Integer> ids = new HashSet<>();
    for (Map.Entry<Integer,Set<Integer>> entry : graph.entrySet()) {
      ids.add(entry.getKey());
      ids.addAll(entry.getValue());
    }
    // Preserve original geometries
    Map<Integer,Feature> currentFeatures = new HashMap<>();
    Map<Integer, Geometry> sourceGeometries = new HashMap<>();
    for (Feature f : fc.getFeatures()) {
      if (ids.contains(f.getID())) currentFeatures.put(f.getID(), f);
      if (ids.contains(f.getID())) sourceGeometries.put(f.getID(), (Geometry)f.getGeometry().clone());
    }

    monitor.report(sMergingPolygons);
    int count = 0;
    for (Feature f : selection) {
      Feature target = useArea ? chooseMaxAreaNeighbour(f.getID(), graph, currentFeatures)
              : chooseLongestBoundaryNeighbour(f.getID(), graph, currentFeatures);
      monitor.report(++count, selection.size(), sMerged);
      if (target != null) {
        merge(f.getID(), target.getID(), graph, currentFeatures);
        //fc.remove(f);
      }
    }
    monitor.report("Prepare transaction");
    // Preserve new calculated geometries and restore original one as
    // because the graph calculation was not transactional (it is difficult
    // to make it transactional beacause of its iterative nature)
    Map<Integer,Geometry> newGeometries = new HashMap<>();
    ids.clear();
    for (Feature f : selection) ids.add(f.getID());
    for (int i : currentFeatures.keySet()) {
      if (ids.contains(i) && !graph.containsKey(i)) newGeometries.put(i, null);
      else newGeometries.put(i, currentFeatures.get(i).getGeometry());
      currentFeatures.get(i).setGeometry(sourceGeometries.get(i));
    }

    // Now make the changes within a transaction

    reportNothingToUndoYet(context);
    activeLayer.getLayerManager().getUndoableEditReceiver().startReceiving();
    try {
      EditTransaction transaction = new EditTransaction(
            new ArrayList(),
            "MergePolygonWithNeighbour",
            activeLayer,
            true,
            true,
            context.getLayerViewPanel()
      );
      for (int i : currentFeatures.keySet()) {
        if (!graph.containsKey(i))
        if (ids.contains(i)) {
          transaction.deleteFeature(currentFeatures.get(i));
        } else {
          transaction.modifyFeatureGeometry(currentFeatures.get(i), newGeometries.get(i));
        }
      }
      transaction.commit();
      context.getWorkbenchContext().getLayerManager().setFiringEvents(true);
      context.getWorkbenchContext().getLayerViewPanel().repaint();
      transaction.commit();
      activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
    }
    finally {
      activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
    }
  }

  // Create a map containing relations between each selected feature and adjacent features
  private Map<Integer,Set<Integer>> getPolygonGraph(SpatialIndex index,
                                                    Collection<Feature> selection,
                                                    TaskMonitor monitor) {

    Map<Integer,Set<Integer>> graph = new HashMap<>();
    monitor.report("Building the graph");
    int count = 0;
    int total = selection.size();
    for (Feature f : selection) {
      monitor.report(++count, total, "polygons");
      if (f.getGeometry().getDimension() != 2) continue;
      List<Feature> candidates = index.query(f.getGeometry().getEnvelopeInternal());
      int fid = f.getID();
      Set<Integer> neighbours = graph.get(fid);
      if (neighbours == null) neighbours = new HashSet<>();
      for (Feature candidate : candidates) {
        int cid = candidate.getID();
        if (cid == fid) continue;
        if (candidate.getGeometry().getDimension() != 2) continue;
        if (attribute != null && !Objects.equals(f.getAttribute(attribute), candidate.getAttribute(attribute))) continue;
        // Non, ça fait des trous !
        //if (c.getGeometry().getArea() < f.getGeometry().getArea()) continue;
        if (f.getGeometry().intersects(candidate.getGeometry())) {
        //if (f.getGeometry().relate(c.getGeometry(),"****1****")) {
          neighbours.add(candidate.getID());
        }
      }
      graph.put(fid, neighbours);
    }
    return graph;
  }

  private boolean merge(int srcId, int dstId, Map<Integer,Set<Integer>> graph, Map<Integer,Feature> currentFeatures) {
    Feature src = currentFeatures.get(srcId);
    Feature dst = currentFeatures.get(dstId);
    dst.setGeometry(dst.getGeometry().union(src.getGeometry()));
    Set<Integer> newDestSet = graph.get(dstId);
    if (newDestSet != null) newDestSet.remove(srcId);
    // Neighbours of src
    for (Integer id : graph.get(srcId)) {
      if (id != srcId && id != dstId) {
        if (newDestSet != null) newDestSet.add(id);
      }
      if (graph.containsKey(id)) graph.get(id).remove(srcId);
    }
    if (graph.containsKey(dst.getID())) {
      for (Integer id : graph.get(dstId)) {
        if (id != srcId && id != dstId) {
          if (newDestSet != null) newDestSet.add(id);
        }
      }
    }
    graph.remove(srcId);
    return true;
  }

  private Feature chooseMaxAreaNeighbour(int fid, Map<Integer,Set<Integer>> graph,
                                         Map<Integer,Feature> currentFeatures) {
    Set<Integer> candidates = graph.get(fid);
    Feature src = currentFeatures.get(fid);
    double srcArea = src.getGeometry().getArea();
    double max = 0;
    Feature selected = null;
    for (Integer cid : candidates) {
      Feature candidate = currentFeatures.get(cid);
      if (cid == fid) continue;
      double area = candidate.getGeometry().getArea();
      //if (area < srcArea) continue;
      if (area > max) {
        max = area;
        selected = candidate;
      }
    }
    return selected;
  }

  private Feature chooseLongestBoundaryNeighbour(int fid, Map<Integer,Set<Integer>> graph,
                                                 Map<Integer,Feature> currentFeatures) {
    Set<Integer> candidates = graph.get(fid);
    Feature src = currentFeatures.get(fid);
    double srcArea = src.getGeometry().getArea();
    double max = 0;
    Feature selected = null;
    if (candidates != null) {
      for (Integer cid : candidates) {
        Feature candidate = currentFeatures.get(cid);
        if (cid == fid) continue;
        //if (candidate.getGeometry().getArea() < srcArea) continue;
        double length = candidate.getGeometry().intersection(src.getGeometry()).getLength();
        if (length > max) {
          max = length;
          selected = candidate;
        }
      }
    } else {
      System.out.println("L'objet " + fid + " n'est plus dans le graph");
    }
    return selected;
  }

}
