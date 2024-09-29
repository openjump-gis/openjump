package org.openjump.core.ui.plugin.tools.analysis.onelayer;

import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
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
  private String sTitle;
  private String sSidebar = "Merges selected polygons with neighboring polygons, either with the one that is largest of " +
          "all neighbors, or the one with which it has " +
          "the longest common boundary. Note, the function may return multi-polygons if " +
          "the polygons to merge have only one point in common.";
  boolean useArea = true;
  boolean useBorder = false;
  String sUseArea = "merge with neighbor that has the largest area";
  String sUseBorder = "merge with neighbor with the longest common edge";
  String sChoseMergeMethod = "Please chose the merge method:";
  String sMerged = "merged";
  String sSearchingForMergeCandidates = "Searching for merge candidates...";
  String sMergingPolygons = "Merging polygons...";
  boolean useAttribute = false;
  String sUseAttribute = "Use an attribute";
  String sUseAttributeTooltip = "Merge features with same attribute value only";
  String sAttributeToUse = "Attribute to use";
  String sSkipNullValues = "Skip features with null attribute value";
  String attribute = null;
  boolean skipNullValues = true;
  String layerName;
  String sMinimumArea;
  double minimumArea = 10;
  boolean featuresSelected = false;
  //String sInvalidFeatureMessage = "Features must be valid to be merged";
  final static String sMERGEMETHOD = "MERGE METHOD";

  String sCreateNewLayer = "Create new layer for result";
  boolean createNewLayer = false;

  private MultiInputDialog dialog;

  Comparator<Feature> areaComparator = (o1, o2) -> {
    double diff = o1.getGeometry().getArea() - o2.getGeometry().getArea();
    return diff > 0 ? 1 : diff < 0 ? -1 : 0;
  };

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    sMergeTwoPolys = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merge-Selected-Polygons-with-Neighbours");
    sFeaturesFromDifferentLayer = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.features-from-different-layers");
    sSidebar = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.description");
    sUseArea = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-that-has-the-largest-area");
    sUseBorder = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-with-neighbor-with-the-longest-common-edge");
    sChoseMergeMethod = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Please-chose-the-merge-method");
    sMerged = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merged");
    sSearchingForMergeCandidates = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Searching-for-merge-candidates");
    sMergingPolygons = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Merging-polygons");
    sUseAttribute = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Use-attribute");
    sUseAttributeTooltip = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Use-attribute-tooltip");
    sAttributeToUse = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Attribute");
    sSkipNullValues = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Skip-null-values");
    sMinimumArea = I18N.JUMP.get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.minimum-area");
    sCreateNewLayer = I18N.JUMP.get("ui.plugin.analysis.GeometryFunctionPlugIn.Create-new-layer-for-result");
    //sInvalidFeatureMessage = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.Features-must-be-valid");

    FeatureInstaller featureInstaller = context.getFeatureInstaller();
    featureInstaller.addMainMenuPlugin(
            this,
            new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_GEOMETRY},
            this.getName() + "...",
            false,
            null,
            createEnableCheck(context.getWorkbenchContext())
                .add(context.getCheckFactory().createExactlyNLayersMustBeSelectedCheck(1)));
  }

  public String getName() {
    return sMergeTwoPolys;
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);

    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {

    Layer[] selectedLayers = context.getLayerNamePanel().getSelectedLayers();
    Collection<Layer> layersWithSelectedItems = context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();

    if (selectedLayers.length != 1) {
      throw new Exception(I18N.JUMP.get("com.vividsolutions.jump.workbench.plugin.Exactly-one-layer-must-be-selected"));
    }
    Layer layer = selectedLayers[0];
    if (layersWithSelectedItems.size() > 1 ||
        (layersWithSelectedItems.size() == 1 && !layersWithSelectedItems.contains(layer))) {
      context.getWorkbenchFrame().warnUser(sFeaturesFromDifferentLayer);
      return false;
    }
    featuresSelected = !context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer).isEmpty();
    layerName = layer.getName();
    sTitle = layersWithSelectedItems.isEmpty() ?
        I18N.JUMP.get(
            "org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-small-polygons",
            layerName) :
        I18N.JUMP.get(
            "org.openjump.core.ui.plugin.tools.MergeSelectedPolygonsWithNeighbourPlugIn.merge-selected-polygons",
            layerName);

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

    dialog.addSubTitle(sTitle);
    dialog.addSeparator();

    dialog.setSideBarDescription(sSidebar);
    final String METHODGROUP = sMERGEMETHOD;
    dialog.addLabel(sChoseMergeMethod);
    dialog.addRadioButton(sUseArea, METHODGROUP, this.useArea, sUseArea);
    dialog.addRadioButton(sUseBorder, METHODGROUP, this.useBorder, sUseBorder);

    if (!featuresSelected) {
      JTextField minimumAreaTF = dialog.addDoubleField(sMinimumArea, minimumArea, 12);
      minimumAreaTF.setEnabled(!featuresSelected);
    }

    // Creating a new Layer
    JCheckBox jcbCreateLayer = dialog.addCheckBox(sCreateNewLayer, createNewLayer);
    if (context.getLayerManager().getLayer(layerName).isEditable()) {
      jcbCreateLayer.setVisible(true);
    } else {
      jcbCreateLayer.setVisible(false);
      jcbCreateLayer.setSelected(true);
      dialog.addLabel("<html><b>" + sCreateNewLayer + "</b></html>").setEnabled(false);
    }

    // Using an attribute
    List<String> attributes = AttributeTypeFilter.NO_GEOMETRY_FILTER.filter(context.getLayerManager()
        .getLayer(layerName));
    dialog.addSeparator();
    JCheckBox jcbUseAttribute = dialog.addCheckBox(sUseAttribute, useAttribute);
    JComboBox<String> jcbAttribute = dialog.addComboBox(sAttributeToUse, attribute, attributes, null);
    jcbAttribute.setEnabled(useAttribute);
    JCheckBox jcbSkipNullValues = dialog.addCheckBox(sSkipNullValues, skipNullValues, null);
    jcbSkipNullValues.setEnabled(useAttribute);
    jcbUseAttribute.addActionListener(e -> {
      jcbAttribute.setEnabled(jcbUseAttribute.isSelected());
      jcbSkipNullValues.setEnabled(jcbUseAttribute.isSelected());
      jcbSkipNullValues.setEnabled(jcbUseAttribute.isSelected());
    });
    if (attributes.isEmpty()) {
      jcbUseAttribute.setSelected(false);
      jcbUseAttribute.setEnabled(false);
    }

    GUIUtil.centreOnWindow(dialog);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    this.useArea = dialog.getBoolean(this.sUseArea);
    this.useBorder = dialog.getBoolean(this.sUseBorder);
    this.useAttribute = dialog.getBoolean(sUseAttribute);
    if (useAttribute) {
      attribute = (String)dialog.getComboBox(sAttributeToUse).getSelectedItem();
      skipNullValues = dialog.getBoolean(sSkipNullValues);
    } else attribute = null;
    if (!featuresSelected) {
      minimumArea = dialog.getDouble(sMinimumArea);
    }
    createNewLayer = dialog.getBoolean(sCreateNewLayer);
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

    monitor.allowCancellationRequests();
    monitor.report("Prepare data");
    Layer activeLayer = context.getLayerManager().getLayer(layerName);
    Collection<Feature> selectedFeatures = context.getWorkbenchContext().getLayerViewPanel()
        .getSelectionManager().getFeaturesWithSelectedItems();
    // May contain clones (if createNewLayer is true) or current features
    Collection<Feature> features = new ArrayList<>();
    Collection<Feature> selection = new ArrayList<>();
    for (Feature feature : activeLayer.getFeatureCollectionWrapper().getFeatures()) {
      Feature f = createNewLayer ? feature.clone(true) : feature;
      features.add(f);
      if (selectedFeatures.contains(feature)) selection.add(f);
    }

    Quadtree index = new Quadtree();
    monitor.report("Index data");
    for (Feature feature: features) {
      index.insert(feature.getGeometry().getEnvelopeInternal(), feature);
    }
    Map<Feature,LinkedHashSet<Feature>> genealogy = new HashMap<>();
    ArrayList<Feature> sortedList = new ArrayList<>();
    monitor.report("Merge data");
    if (featuresSelected) {
      sortedList.addAll(selection);
      int total = selection.size();
          sortedList.sort(areaComparator);
      int count = 0;
      while (!sortedList.isEmpty()) {
        monitor.report(count++, total, "merged");
        Feature feature = sortedList.get(0);
        merge(feature, index, sortedList, genealogy);
      }
    } else {
      sortedList.addAll(features);
      sortedList.sort(areaComparator);
      int count = 0;
      int total = (int)sortedList.stream().filter(f -> f.getGeometry().getArea() <= minimumArea).count();
      while (sortedList.size() > 0 && sortedList.get(0).getGeometry().getArea() <= minimumArea) {
        Feature feature = sortedList.get(0);
        Feature merged = merge(feature, index, sortedList, genealogy);
        if (count%10 == 0) {
          long pm = count*1000L/total;
          monitor.report("" + pm/10.0 + "% merged");
        }
        count++;
        if (merged != null && merged.getGeometry().getArea() <= minimumArea) total++;
      }
    }

    if (createNewLayer) {
      monitor.report("Compute result (create new layer)");
      // Iterate through genealogy which maps new features to all ancestors
      for (Map.Entry<Feature, LinkedHashSet<Feature>> entry : genealogy.entrySet()) {
        for (Iterator<Feature> it = entry.getValue().iterator() ; it.hasNext() ;) {
          features.remove(it.next());
        }
        features.add(entry.getKey());
      }
      FeatureCollection dataset = new FeatureDataset(activeLayer.getFeatureCollectionWrapper().getFeatureSchema());
      dataset.addAll(features);
      context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
      context.addLayer(StandardCategoryNames.RESULT, layerName + "-merged", dataset);
    } else {
      context.getWorkbenchContext().getLayerManager().setFiringEvents(false);
      monitor.report("Prepare transaction");
      // Now make the changes within a transaction
      reportNothingToUndoYet(context);
      activeLayer.getLayerManager().getUndoableEditReceiver().startReceiving();
      try {
        EditTransaction transaction = new EditTransaction(
            new ArrayList<>(),
            "MergePolygonWithNeighbour",
            activeLayer,
            true,
            true,
            context.getLayerViewPanel()
        );
        for (Map.Entry<Feature, LinkedHashSet<Feature>> entry : genealogy.entrySet()) {
          while (entry.getValue().size() > 1) {
            Feature f = entry.getValue().iterator().next();
            transaction.deleteFeature(f);
            entry.getValue().remove(f);
          }
          transaction.modifyFeatureGeometry(entry.getValue().iterator().next(), entry.getKey().getGeometry());
        }
        transaction.commit();
        context.getWorkbenchContext().getLayerViewPanel().getSelectionManager().clear();
        context.getWorkbenchContext().getLayerViewPanel().repaint();
      } finally {
        context.getWorkbenchContext().getLayerManager().setFiringEvents(true);
        activeLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
      }
    }
  }

  private Feature merge(Feature feature, SpatialIndex index, ArrayList<Feature> sortedList,
                        Map<Feature,LinkedHashSet<Feature>> genealogy) throws Exception {
    // remove the feature being processed from the index
    sortedList.remove(feature);
    index.remove(feature.getGeometry().getEnvelopeInternal(), feature);
    // find the best candidate to merge to
    List<Feature> candidates = index.query(feature.getGeometry().getEnvelopeInternal());
    Feature bestCandidate = getBestCandidate(feature, candidates);
    if (bestCandidate != null) {
      // Create a new merged feature
      Geometry newGeom = feature.getGeometry().union(bestCandidate.getGeometry());
      Feature newFeature = bestCandidate.clone();
      newFeature.setGeometry(newGeom);

      LinkedHashSet<Feature> children = new LinkedHashSet<>();
      genealogy.put(newFeature, children);

      if (genealogy.containsKey(feature)) children.addAll(genealogy.get(feature));
      else children.add(feature);
      genealogy.remove(feature);

      if (genealogy.containsKey(bestCandidate)) children.addAll(genealogy.get(bestCandidate));
      else children.add(bestCandidate);
      genealogy.remove(bestCandidate);
      sortedList.remove(bestCandidate);
      index.remove(bestCandidate.getGeometry().getEnvelopeInternal(), bestCandidate);
      index.insert(newGeom.getEnvelopeInternal(), newFeature);
      // if there is no preselection of features to be processed but only an area criteria
      // the process is iterative
      if (!this.featuresSelected && this.minimumArea > 0.0) {
        int idx = Collections.binarySearch(sortedList, newFeature, areaComparator);
        if (idx >= 0) sortedList.add(idx, newFeature);
        else sortedList.add((-idx-1), newFeature);
      }
      return newFeature;
    }
    return null;
  }

  private Feature getBestCandidate(Feature feature, List<Feature> candidates) throws Exception {
    if (useArea) return getBestCandidateUsingArea(feature, candidates);
    else if (useBorder) return getBestCandidateUsingBorder(feature, candidates);
    else throw new Exception("No method has been selected to choose the feature to merge to : " +
          "use 'max area' ore use 'common border length'");
  }

  private Feature getBestCandidateUsingArea(Feature feature, List<Feature> candidates) {
    Feature bestCandidate = null;
    double maxArea = 0.0;
    for (Feature candidate : filterCandidates(feature,candidates)) {
      double area = candidate.getGeometry().getArea();
      IntersectionMatrix im = feature.getGeometry().relate(candidate.getGeometry());
      if (im.matches("2********") || im.matches("****1****")) {
        if (area > maxArea) {
          bestCandidate = candidate;
          maxArea = area;
        }
      }
    }
    return bestCandidate;
  }

  private Feature getBestCandidateUsingBorder(Feature feature, List<Feature> candidates) {
    Feature bestCandidate = null;
    double maxLength = 0;
    for (Feature candidate : filterCandidates(feature,candidates)) {
      IntersectionMatrix im = feature.getGeometry().relate(candidate.getGeometry());
      if (im.matches("****1****")) {
        double length = feature.getGeometry().getBoundary()
            .intersection(candidate.getGeometry().getBoundary()).getLength();
        if (length > maxLength) {
          maxLength = length;
          bestCandidate = candidate;
        }
      }
    }
    return bestCandidate;
  }

  private Collection<Feature> filterCandidates(Feature feature, Collection<Feature> candidates) {
    if (attribute != null) {
      List<Feature> list = new ArrayList<>();
      Object ref = feature.getAttribute(attribute);
      if ((ref == null || ref.toString().isEmpty()) && skipNullValues) {
        return list;
      }
      for (Feature c : candidates) {
        if ((ref == null || ref.toString().isEmpty()) && !skipNullValues) {
          if (c.getAttribute(attribute) == null) list.add(c);
        } else if (ref != null && !ref.toString().isEmpty() && c.getAttribute(attribute) != null) {
          if (ref.equals(c.getAttribute(attribute))) list.add(c);
        }
      }
      return list;
    } else return candidates;
  }

}
