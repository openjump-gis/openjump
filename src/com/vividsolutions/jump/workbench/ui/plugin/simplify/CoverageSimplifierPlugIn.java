package com.vividsolutions.jump.workbench.ui.plugin.simplify;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import org.locationtech.jts.coverage.CoverageSimplifier;
import org.locationtech.jts.geom.Geometry;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.List;

public class CoverageSimplifierPlugIn extends AbstractThreadedUiPlugIn {

  private String LAYER;
  private String DESCRIPTION;
  private String CREATE_NEW_LAYER;
  private String CREATE_NEW_LAYER_TOOLTIP;
  private String UPDATE_SOURCE;
  private String UPDATE_SOURCE_TOOLTIP;

  private String SIMPLIFICATION_TOLERANCE;
  private String SIMPLIFICATION_TOLERANCE_TOOLTIP;
  private String USE_ATTRIBUTE;
  private String USE_ATTRIBUTE_TOOLTIP;
  private String ATTRIBUTE;
  private String ATTRIBUTE_TOOLTIP;

  private String OTHER_PARAMETERS;
  private String SMALL_RINGS_REMOVER_COEFFICIENT;
  private String SMALL_RINGS_REMOVER_COEFFICIENT_TOOLTIP;
  private String PRESERVE_OUTER_BOUNDARY;
  private String PRESERVE_OUTER_BOUNDARY_TOOLTIP;
  private String SMOOTHNESS_COEFFICIENT;
  private String SMOOTHNESS_COEFFICIENT_TOOLTIP;

  // Parameter names used for macro persistence
  // Mandatory
  private static final String P_LAYER_NAME               = "LayerName";
  // Optional (default value provided)
  private static final String P_UPDATE_SOURCE            = "UpdateSource";
  private static final String P_SIMPLIFICATION_TOLERANCE = "SimplificationTolerance";
  private static final String P_USE_ATTRIBUTE            = "UseAttribute";
  private static final String P_SIMPLIFICATION_ATTRIBUTE = "SimplificationAttribute";
  private static final String P_SMALL_RINGS_REMOVER_COEFFICIENT  = "SmallRingsRemoverCoefficient";
  private static final String P_PRESERVE_OUTER_BOUNDARY  = "PreserveOuterBoundary";
  private static final String P_SMOOTHNESS_COEFFICIENT   = "SmoothnessCoefficient";

  {
    addParameter(P_LAYER_NAME,              null);
    addParameter(P_UPDATE_SOURCE,           false);
    addParameter(P_SIMPLIFICATION_TOLERANCE,1.0);
    addParameter(P_USE_ATTRIBUTE,           false);
    addParameter(P_SIMPLIFICATION_ATTRIBUTE,null);
    addParameter(P_SMALL_RINGS_REMOVER_COEFFICIENT, 1.0);
    addParameter(P_PRESERVE_OUTER_BOUNDARY, false);
    addParameter(P_SMOOTHNESS_COEFFICIENT,  0.0);
  }

  public CoverageSimplifierPlugIn() {
    super(
        I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn") + "...",
        IconLoader.icon("simplify_coverage_icon.png")
    );
  }


  public void initialize(PlugInContext context) throws Exception {
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[]{MenuNames.TOOLS,MenuNames.GENERALIZATION}, getName(),
        false, getIcon(), createEnableCheck(context.getWorkbenchContext()));
  }


  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);

    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
  }


  public boolean execute(PlugInContext context) throws Exception {
    LAYER = I18N.getInstance().get("ui.GenericNames.LAYER");
    DESCRIPTION = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.description");
    CREATE_NEW_LAYER = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.create-new-layer");
    CREATE_NEW_LAYER_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.create-new-layer-tooltip");
    UPDATE_SOURCE = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.update-source");
    UPDATE_SOURCE_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.update-source-tooltip");

    SIMPLIFICATION_TOLERANCE = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.simplification-tolerance");
    SIMPLIFICATION_TOLERANCE_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.simplification-tolerance-tooltip");
    USE_ATTRIBUTE = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.use-attribute");
    USE_ATTRIBUTE_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.use-attribute-tooltip");
    ATTRIBUTE = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.attribute");
    ATTRIBUTE_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.attribute-tooltip");

    OTHER_PARAMETERS = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.other-parameters");
    SMALL_RINGS_REMOVER_COEFFICIENT = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.small-rings-remover-coefficient");
    SMALL_RINGS_REMOVER_COEFFICIENT_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.small-ring-remover-coefficient-tooltip");
    PRESERVE_OUTER_BOUNDARY = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.preserve_outer_boundary");
    PRESERVE_OUTER_BOUNDARY_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.preserve_outer_boundary-tooltip");
    SMOOTHNESS_COEFFICIENT = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.smoothness_coefficient");
    SMOOTHNESS_COEFFICIENT_TOOLTIP = I18N.getInstance().get("ui.plugin.simplify.CoverageSimplifierPlugIn.smoothness_coefficient-tooltip");

    MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) {
      return false;
    }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    try {
      Layer layer = context.getLayerManager().getLayer((String)getParameter(P_LAYER_NAME));
      FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
      boolean updateSource = (Boolean)getParameter(P_UPDATE_SOURCE);
      boolean useAttribute = (Boolean)getParameter(P_USE_ATTRIBUTE);
      int attributeIndex = getBooleanParam(P_USE_ATTRIBUTE) ?
          schema.getAttributeIndex(getStringParam(P_SIMPLIFICATION_ATTRIBUTE)) : -1;

      List<Feature> features = layer.getFeatureCollectionWrapper().getFeatures();
      Geometry[] source = new Geometry[features.size()];
      double[] perFeatureTolerance = new double[features.size()];
      for (int i = 0 ; i < features.size() ; i++) {
        source[i] = features.get(i).getGeometry();
        if (useAttribute) {
          Object t = features.get(i).getAttribute(attributeIndex);
          perFeatureTolerance[i] = t == null ? 0.0 : (double)t;
        }
      }
      Geometry[] simplifiedGeometries = useAttribute ? simplify(source, perFeatureTolerance) : simplify(source);

      if (updateSource) {
        updateSourceLayer(monitor, context, layer, simplifiedGeometries);
      } else {
        createNewLayer(monitor, context, layer, simplifiedGeometries);
      }
    } catch(Exception e) {
      throw e;
    }
  }

  private void updateSourceLayer(TaskMonitor monitor, PlugInContext context,
                                 Layer layer, Geometry[] simplifiedGeometries) {
    EditTransaction transaction = new EditTransaction(new LinkedHashSet<>(),
        "CoverageSimplifier", layer, true, true,
        context.getLayerViewPanel().getContext());
    List<Feature> features = layer.getFeatureCollectionWrapper().getFeatures();
    for (int i = 0 ; i < features.size() ; i++) {
      Feature feature = features.get(i);
      transaction.modifyFeatureGeometry(feature, simplifiedGeometries[i]);
    }
    transaction.commit();
  }

  private void createNewLayer(TaskMonitor monitor, PlugInContext context,
                              Layer layer, Geometry[] simplifiedGeometries) {
    FeatureCollection ds = new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema());
    List<Feature> features = layer.getFeatureCollectionWrapper().getFeatures();
    for (int i = 0 ; i < features.size() ; i++) {
      Feature newFeature = features.get(i).clone(false, true);
      newFeature.setGeometry(simplifiedGeometries[i]);
      ds.add(newFeature);
    }
    context.getLayerManager().addLayer(GenericNames.RESULT_LAYER,
        layer.getName()+"_simplified_"+(double)getParameter(P_SIMPLIFICATION_TOLERANCE), ds);
  }

  private Geometry[] simplify(Geometry[] source) {
    CoverageSimplifier simplifier = new CoverageSimplifier(source);
    simplifier.setRemovableRingSizeFactor(getDoubleParam(P_SMALL_RINGS_REMOVER_COEFFICIENT));
    simplifier.setSmoothWeight(getDoubleParam(P_SMOOTHNESS_COEFFICIENT));
    double innerTolerance = getDoubleParam(P_SIMPLIFICATION_TOLERANCE);
    double outerTolerance = getBooleanParam(P_PRESERVE_OUTER_BOUNDARY) ? 0.0 : innerTolerance;
    return simplifier.simplify(innerTolerance, outerTolerance);
  }

  private Geometry[] simplify(Geometry[] source, double[] tolerances) {
    CoverageSimplifier simplifier = new CoverageSimplifier(source);
    simplifier.setRemovableRingSizeFactor(getDoubleParam(P_SMALL_RINGS_REMOVER_COEFFICIENT));
    simplifier.setSmoothWeight(getDoubleParam(P_SMOOTHNESS_COEFFICIENT));
    return simplifier.simplify(tolerances);
  }

  private void setDialogValues(final MultiInputDialog dialog, final PlugInContext context) {
    dialog.setSideBarDescription(DESCRIPTION);
    dialog.setSideBarImage(
        new javax.swing.ImageIcon(IconLoader.image("simplify_coverage.png")
            .getScaledInstance((int)(216.0*0.8), (int)(159.0*0.8), java.awt.Image.SCALE_SMOOTH))
    );
    final JComboBox layerComboBox =
        dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), context.getLayerManager());
    final JRadioButton createNewRadioButton =
        dialog.addRadioButton(CREATE_NEW_LAYER, "new/update",
            !(Boolean)getParameter(P_UPDATE_SOURCE), CREATE_NEW_LAYER_TOOLTIP);
    final JRadioButton updateRadioButton =
        dialog.addRadioButton(UPDATE_SOURCE, "new/update",
            (Boolean)getParameter(P_UPDATE_SOURCE), UPDATE_SOURCE_TOOLTIP);

    dialog.addSubTitle(SIMPLIFICATION_TOLERANCE);
    final JTextField toleranceTextField =
        dialog.addDoubleField(SIMPLIFICATION_TOLERANCE, getDoubleParam(P_SIMPLIFICATION_TOLERANCE),
            10, SIMPLIFICATION_TOLERANCE_TOOLTIP);
    final JCheckBox preserveOuterBoundaryChecBox =
        dialog.addCheckBox(PRESERVE_OUTER_BOUNDARY, getBooleanParam(P_PRESERVE_OUTER_BOUNDARY),
            PRESERVE_OUTER_BOUNDARY_TOOLTIP);
    final JCheckBox useAttributeCheckBox =
        dialog.addCheckBox(USE_ATTRIBUTE, false, USE_ATTRIBUTE_TOOLTIP);
    final JComboBox attributeComboBox =
        dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMERIC_FILTER, ATTRIBUTE_TOOLTIP);
    attributeComboBox.setEnabled(getBooleanParam(P_USE_ATTRIBUTE));

    dialog.addSubTitle(OTHER_PARAMETERS);
    final JTextField smallRingsRemoverCoefficientTextField =
        dialog.addDoubleField(SMALL_RINGS_REMOVER_COEFFICIENT, getDoubleParam(P_SMALL_RINGS_REMOVER_COEFFICIENT),
            10, SMALL_RINGS_REMOVER_COEFFICIENT_TOOLTIP);
    final JTextField smoothnessCoefficientTextField =
        dialog.addDoubleField(SMOOTHNESS_COEFFICIENT, getDoubleParam(P_SMOOTHNESS_COEFFICIENT),
            10, SMOOTHNESS_COEFFICIENT_TOOLTIP);

    layerComboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for (ActionListener listener : layerComboBox.getActionListeners()) {
          // execute other ActionListener methods before this one
          if (listener != this) listener.actionPerformed(e);
        }
        List<String> numericAttributes = AttributeTypeFilter.NUMERIC_FILTER.filter(
            context.getLayerManager().getLayer((String)layerComboBox.getSelectedItem())
        );
        boolean hasNumericAttributes = !numericAttributes.isEmpty();
        useAttributeCheckBox.setEnabled(hasNumericAttributes);
        attributeComboBox.setModel(new DefaultComboBoxModel(numericAttributes.toArray()));
      }
    });
    preserveOuterBoundaryChecBox.addActionListener(e -> {
        String image = preserveOuterBoundaryChecBox.isSelected() ? "simplify_coverage_pob.png" : "simplify_coverage.png";
        dialog.setSideBarImage(
          new javax.swing.ImageIcon(IconLoader.image(image)
              .getScaledInstance((int)(216.0*0.8), (int)(159.0*0.8), java.awt.Image.SCALE_SMOOTH))
        );
    });
    useAttributeCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toleranceTextField.setEnabled(!useAttributeCheckBox.isSelected());
        preserveOuterBoundaryChecBox.setEnabled(!useAttributeCheckBox.isSelected());
        attributeComboBox.setEnabled(useAttributeCheckBox.isSelected());
        setSideBarImage(dialog);
      }
    });
    smoothnessCoefficientTextField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (dialog.getDouble(SMOOTHNESS_COEFFICIENT) < 0)
          smoothnessCoefficientTextField.setText("0.0");
        if (dialog.getDouble(SMOOTHNESS_COEFFICIENT) > 1.0)
          smoothnessCoefficientTextField.setText("1.0");
      }
    });
  }

  private void setSideBarImage(MultiInputDialog dialog) {
    String image = (dialog.getBoolean(PRESERVE_OUTER_BOUNDARY) && !dialog.getBoolean(USE_ATTRIBUTE)) ?
        "simplify_coverage_pob.png" : "simplify_coverage.png";
    dialog.setSideBarImage(
        new javax.swing.ImageIcon(IconLoader.image(image)
            .getScaledInstance((int)(216.0*0.8), (int)(159.0*0.8), java.awt.Image.SCALE_SMOOTH))
    );
  }

  private void getDialogValues(final MultiInputDialog dialog) {

    boolean useAttribute = dialog.getBoolean(USE_ATTRIBUTE);

    addParameter(P_LAYER_NAME, dialog.getLayer(LAYER).getName());
    addParameter(P_UPDATE_SOURCE, dialog.getBoolean(UPDATE_SOURCE));
    addParameter(P_SIMPLIFICATION_TOLERANCE, useAttribute ? 0.0 : dialog.getDouble(SIMPLIFICATION_TOLERANCE));
    addParameter(P_USE_ATTRIBUTE, dialog.getBoolean(USE_ATTRIBUTE));
    addParameter(P_SIMPLIFICATION_ATTRIBUTE, useAttribute ? dialog.getText(ATTRIBUTE) : null);

    addParameter(P_SMALL_RINGS_REMOVER_COEFFICIENT, dialog.getDouble(SMALL_RINGS_REMOVER_COEFFICIENT));
    addParameter(P_PRESERVE_OUTER_BOUNDARY, dialog.getBoolean(PRESERVE_OUTER_BOUNDARY));
    addParameter(P_SMOOTHNESS_COEFFICIENT, dialog.getDouble(SMOOTHNESS_COEFFICIENT));
  }
}
