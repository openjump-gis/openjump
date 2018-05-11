package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import static com.vividsolutions.jump.workbench.ui.AttributeTypeFilter.NUMSTRING_FILTER;

/**
 * Update a layer from another layer using a join
 */
public class UpdateWithJoinPlugIn extends AbstractThreadedUiPlugIn {

    private final static String DESCRIPTION = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Description");
    //private final static String CHOOSE_LAYERS = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Choose-layers");
    private final static String REFERENCE_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Reference-layer-to-update");
    private final static String JOIN_LAYER = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Join-layer");
    private final static String REFERENCE_LAYER_EXT_ID = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Reference-layer-ext-id");
    private final static String JOIN_LAYER_ID = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Join-layer-id");
    private final static String LEFT_JOIN = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Left-join");
    private final static String LEFT_JOIN_TT = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Left-join-tooltip");
    private final static String RIGHT_JOIN = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Right-join");
    private final static String RIGHT_JOIN_TT = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Right-join-tooltip");
    //private final static String ADD_ATTRIBUTES = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Add-attributes-create-new-layer");
    //private final static String ADD_ATTRIBUTES_TT = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Add-attributes-tooltip");
    private final static String MAP_ATTRIBUTES = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Map-attributes");
    private final static String DO_NOT_JOIN = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Do-not-join");
    private final static String UNDEFINED_MAPPING = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Attributes-mapping-is-not-defined");
    private final static String JOIN_NOT_UNIQUE = I18N.get("org.openjump.core.ui.plugin.tools.UpdateWithJoinPlugIn.Join-layer-id-is-not-unique");

    private Layer referenceLayer = null;
    private Layer joinLayer = null;
    private String referenceLayerExtId = "";
    private String joinLayerId = "";
    private boolean left = true;
    private boolean right = false;
    private boolean add_attributes = false;
    private boolean attributesChoosen;
    private Map<String, String> attributesMapping = new HashMap<>();

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES},
                this.getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()), -1);
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(2));
    }

    public boolean execute(PlugInContext context) throws Exception {
        attributesChoosen = false;
        MultiInputDialog dialog = initDialog(context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        } else {
            if (!attributesChoosen) {
                throw new Exception(UNDEFINED_MAPPING);
            } else {
                this.getDialogValues(dialog);
            }
        }
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {

        monitor.allowCancellationRequests();

        reportNothingToUndoYet(context);

        FeatureCollection joinFC = joinLayer.getFeatureCollectionWrapper();
        int joinLayerAttributeIndex = joinFC.getFeatureSchema().getAttributeIndex(joinLayerId);
        Map<String,Feature> joinMap = new HashMap<>();

        for (Feature f : joinFC.getFeatures()) {
            Object joinId = f.getAttribute(joinLayerAttributeIndex);
            if (joinId == null) continue;
            // If joinMap already contains this key, join is ambiguous, an exception is thrown
            if (joinMap.get(joinId.toString()) != null) {
                throw new Exception(JOIN_NOT_UNIQUE);
            }
            joinMap.put(joinId.toString(), f);
        }

        if (add_attributes) {
            //createNewLayer(monitor, context, joinMap);
        } else {
            updateLayer(monitor, context, joinMap);
        }

    }

    //private void createNewLayer(TaskMonitor monitor, PlugInContext context, Map<String,Feature> joinMap) {}

    private void updateLayer(TaskMonitor monitor, PlugInContext context, Map<String,Feature> joinMap) {

        final Map<Integer,Feature> oldFeatures = new HashMap<>(); // old versions of modified features
        final Map<Integer,Feature> newFeatures = new HashMap<>(); // new version of modified features
        final List<Feature> added = new ArrayList<>();            // features added to baseLayer
        final List<Feature> removed = new ArrayList<>();          // features removed from baseLayer

        for (Feature f : referenceLayer.getFeatureCollectionWrapper().getFeatures()) {

            Object extKey = f.getAttribute(referenceLayerExtId);
            if (extKey == null) {
                if (!left) {
                    removed.add(f);
                }
            } else {
                Feature joinedFeature = joinMap.get(extKey.toString());
                if (joinedFeature == null) { // no join
                    if (!left) {
                        removed.add(f);
                    }
                    continue;                             // f is kept (left join)
                }
                // extKey is not null and key exists in joinLayer
                Feature newFeature = f.clone(true);
                boolean modified = false;
                for (int i = 0 ; i < f.getSchema().getAttributeCount() ; i++) {
                    String name = f.getSchema().getAttributeName(i);
                    if (name.equals(referenceLayerExtId)) {
                        continue;   // do not update attribute used for the join
                    }
                    String joinName = attributesMapping.get(name);
                    if (joinName.equals(DO_NOT_JOIN)) {
                        continue;   // do not update if no join is defined
                    }
                    int joinIndex = joinedFeature.getSchema().getAttributeIndex(joinName);
                    if (Objects.equals(f.getAttribute(name), joinedFeature.getAttribute(joinIndex))) {
                        continue;   // do not update if
                    }
                    AttributeType type = f.getSchema().getAttributeType(i);
                    if (type == AttributeType.STRING) newFeature.setAttribute(i, joinedFeature.getString(joinIndex));
                    else if (type == AttributeType.DOUBLE) newFeature.setAttribute(i, joinedFeature.getDouble(joinIndex));
                    else if (type == AttributeType.INTEGER) newFeature.setAttribute(i, joinedFeature.getInteger(joinIndex));
                    else if (type == AttributeType.GEOMETRY) newFeature.setGeometry(joinedFeature.getGeometry());
                    else newFeature.setAttribute(i, joinedFeature.getAttribute(joinIndex));
                    modified = true;
                }
                if (modified) {
                    oldFeatures.put(f.getID(), f.clone(true));
                    newFeatures.put(f.getID(), newFeature);
                }
            }
        }
        if (right) {
            for (Feature f : joinLayer.getFeatureCollectionWrapper().getFeatures()) {
                Object key = f.getAttribute(referenceLayerExtId);
                if (key != null && joinMap.containsKey(key.toString())) continue;
                Feature bf = new BasicFeature(referenceLayer.getFeatureCollectionWrapper().getFeatureSchema());
                bf.setGeometry(f.getGeometry());
                for (int i = 0 ; i < bf.getSchema().getAttributeCount() ; i++) {
                    String name = bf.getSchema().getAttributeName(i);
                    String joinName = attributesMapping.get(name);
                    if (name.equals(referenceLayerExtId)) joinName = joinLayerId;
                    if (joinName.equals(DO_NOT_JOIN)) continue;
                    int joinIndex = f.getSchema().getAttributeIndex(joinName);
                    if (f.getAttribute(joinIndex) == null) continue;
                    AttributeType type = bf.getSchema().getAttributeType(i);
                    if (type == AttributeType.STRING) bf.setAttribute(i, f.getString(joinIndex));
                    else if (type == AttributeType.DOUBLE) bf.setAttribute(i, f.getDouble(joinIndex));
                    else if (type == AttributeType.INTEGER) bf.setAttribute(i, f.getInteger(joinIndex));
                    else if (type == AttributeType.GEOMETRY) bf.setGeometry(f.getGeometry());
                    else bf.setAttribute(i, f.getAttribute(joinIndex));
                }
                added.add(bf);
            }
        }

        referenceLayer.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                    new UndoableCommand(I18N.get(AutoAssignAttributePlugIn.class.getName())) {
                        public void execute() {
                            for (Feature f : referenceLayer.getFeatureCollectionWrapper().getFeatures()) {
                                Feature newFeature = newFeatures.get(f.getID());
                                if (newFeature != null) {
                                    f.setAttributes(newFeature.getAttributes());
                                }
                            }
                            for (Feature f : removed) {
                                referenceLayer.getFeatureCollectionWrapper().remove(f);
                            }
                            for (Feature f : added) {
                                referenceLayer.getFeatureCollectionWrapper().add(f);
                            }
                            referenceLayer.getLayerManager().fireFeaturesAttChanged(newFeatures.values(),
                                    FeatureEventType.ATTRIBUTES_MODIFIED, referenceLayer, oldFeatures.values());
                            referenceLayer.getLayerManager().fireGeometryModified(newFeatures.values(),
                                    referenceLayer, oldFeatures.values());
                            referenceLayer.getLayerManager().fireFeaturesChanged(added,
                                    FeatureEventType.ADDED, referenceLayer);
                            referenceLayer.getLayerManager().fireFeaturesChanged(removed,
                                    FeatureEventType.DELETED, referenceLayer);
                        }
                        public void unexecute() {
                            for (Feature f : referenceLayer.getFeatureCollectionWrapper().getFeatures()) {
                                Feature oldFeature = oldFeatures.get(f.getID());
                                if (oldFeature != null) {
                                    f.setAttributes(oldFeature.getAttributes());
                                }
                            }
                            for (Feature f : removed) {
                                referenceLayer.getFeatureCollectionWrapper().add(f);
                            }
                            for (Feature f : added) {
                                referenceLayer.getFeatureCollectionWrapper().remove(f);
                            }
                            referenceLayer.getLayerManager().fireFeaturesAttChanged(oldFeatures.values(),
                                    FeatureEventType.ATTRIBUTES_MODIFIED, referenceLayer, newFeatures.values());
                            referenceLayer.getLayerManager().fireGeometryModified(oldFeatures.values(),
                                    referenceLayer, newFeatures.values());
                            referenceLayer.getLayerManager().fireFeaturesChanged(removed,
                                    FeatureEventType.ADDED, referenceLayer);
                            referenceLayer.getLayerManager().fireFeaturesChanged(added,
                                    FeatureEventType.DELETED, referenceLayer);
                        }
                    };
            command.execute();
            referenceLayer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
        } finally {
            referenceLayer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
    }

    private MultiInputDialog initDialog(final PlugInContext context) {

        final MultiInputDialog dialog = new MultiInputDialog(context.getWorkbenchFrame(), this.getName(), true);
        dialog.setSideBarDescription(DESCRIPTION);

        if (referenceLayer == null || !context.getLayerManager().getLayers().contains(referenceLayer)) {
            referenceLayer = context.getCandidateLayer(0);
        }
        final JComboBox<Layer> jcbBaseLayer = dialog.addLayerComboBox(REFERENCE_LAYER, referenceLayer, null, context.getLayerManager());

        List<String> baseLayerAttributeList = NUMSTRING_FILTER.filter(referenceLayer);
        String valBaseAttribute = baseLayerAttributeList.size() > 0 ? baseLayerAttributeList.get(0) : null;
        final JComboBox<String> jcbBaseLayerId = dialog.addComboBox(REFERENCE_LAYER_EXT_ID, valBaseAttribute, baseLayerAttributeList, REFERENCE_LAYER_EXT_ID);
        jcbBaseLayerId.setEnabled(baseLayerAttributeList.size() > 0);

        dialog.addSeparator(); //----

        if (joinLayer == null || !context.getLayerManager().getLayers().contains(joinLayer)) {
            joinLayer = context.getCandidateLayer(1);
        }
        final JComboBox<Layer> jcbJoinLayer = dialog.addLayerComboBox(JOIN_LAYER, joinLayer, null, context.getLayerManager());

        List<String> joinLayerAttributeList = NUMSTRING_FILTER.filter(joinLayer);
        String valJoinAttribute = joinLayerAttributeList.size() > 0 ? joinLayerAttributeList.get(0) : null;
        final JComboBox<String> jcbJoinLayerId = dialog.addComboBox(JOIN_LAYER_ID, valJoinAttribute, joinLayerAttributeList, JOIN_LAYER_ID);
        jcbJoinLayerId.setEnabled(joinLayerAttributeList.size() > 0);

        dialog.addSeparator(); //----

        dialog.addCheckBox(LEFT_JOIN, left, LEFT_JOIN_TT);
        dialog.addCheckBox(RIGHT_JOIN, right, RIGHT_JOIN_TT);
        //dialog.addCheckBox(ADD_ATTRIBUTES, add_attributes, ADD_ATTRIBUTES_TT);

        jcbBaseLayer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jcbBaseLayer.getSelectedItem() == referenceLayer) return;
                attributesChoosen = false;
                referenceLayer = (Layer)jcbBaseLayer.getSelectedItem();
                List<String> baseLayerAttributeList = NUMSTRING_FILTER.filter(referenceLayer);
                jcbBaseLayerId.setModel(new DefaultComboBoxModel<>(baseLayerAttributeList.toArray(new String[0])));
                jcbBaseLayerId.setEnabled(baseLayerAttributeList.size() > 0);
            }
        });

        jcbBaseLayerId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jcbBaseLayerId.getSelectedItem().equals(referenceLayerExtId)) attributesChoosen = false;
            }
        });

        jcbJoinLayer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(jcbJoinLayer.getSelectedItem() == joinLayer) return;
                attributesChoosen = false;
                joinLayer = (Layer)jcbJoinLayer.getSelectedItem();
                List<String> joinLayerAttributeList = NUMSTRING_FILTER.filter(joinLayer);
                jcbJoinLayerId.setModel(new DefaultComboBoxModel<>(joinLayerAttributeList.toArray(new String[0])));
                jcbJoinLayerId.setEnabled(joinLayerAttributeList.size() > 0);
            }
        });

        jcbJoinLayerId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!jcbJoinLayerId.getSelectedItem().equals(joinLayerId)) attributesChoosen = false;
            }
        });

        JButton mapAttributesButton = dialog.addButton(MAP_ATTRIBUTES);
        mapAttributesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MultiInputDialog mid = initMappingDialog(context.getWorkbenchFrame(),
                        dialog.getLayer(REFERENCE_LAYER),
                        dialog.getLayer(JOIN_LAYER),
                        dialog.getText(REFERENCE_LAYER_EXT_ID));
                GUIUtil.centreOnWindow(mid);
                mid.setVisible(true);
                if (mid.wasOKPressed()) {
                    getMapping(mid);
                    attributesChoosen = true;
                }
            }
        });

        return dialog;
    }


    private void getDialogValues(MultiInputDialog dialog) {
        this.referenceLayer = dialog.getLayer(REFERENCE_LAYER);
        this.joinLayer = dialog.getLayer(JOIN_LAYER);
        this.referenceLayerExtId = dialog.getText(REFERENCE_LAYER_EXT_ID);
        this.joinLayerId = dialog.getText(JOIN_LAYER_ID);
        this.left = dialog.getBoolean(LEFT_JOIN);
        this.right = dialog.getBoolean(RIGHT_JOIN);
    }

    // MultiInputDialog to set the mapping between base attributes and join attributes
    private MultiInputDialog initMappingDialog(JFrame parent, Layer baseLayer, Layer joinLayer, String baseId) {
        // Creates a MultiInputDialog box with a JScrollPane to handle long list of attributes
        MultiInputDialog dialog = new MultiInputDialog(parent, "", true) {
            protected void setMainComponent() {
                currentPanel = new JPanel(new GridBagLayout());
                mainComponent = new JScrollPane(currentPanel);
                Border mainComponentBorder = BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                );
                currentPanel.setBorder(mainComponentBorder);
            }
        };
        dialog.setPreferredSize(new Dimension(480,320));
        // For each attribute of baseLayer, creates a list of joinLayer attributes candidates
        for (String name : baseLayer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeNames()) {
            if (name.equals(baseId)) continue;
            AttributeType type = baseLayer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeType(name);
            AttributeTypeFilter filter = AttributeTypeFilter.ALL_FILTER;
            if (type != AttributeType.STRING) {
                if (type == AttributeType.GEOMETRY) filter = AttributeTypeFilter.GEOMETRY_FILTER;
                if (type == AttributeType.DOUBLE) filter = AttributeTypeFilter.DOUBLE_FILTER;
                if (type == AttributeType.INTEGER) filter = AttributeTypeFilter.NUMERIC_FILTER;
                if (type == AttributeType.LONG) filter = AttributeTypeFilter.NUMERIC_FILTER;
                if (type == AttributeType.DOUBLE) filter = AttributeTypeFilter.NUMERIC_FILTER;
                if (type == AttributeType.DATE) filter = AttributeTypeFilter.DATE_FILTER;
                if (type == AttributeType.BOOLEAN) filter = AttributeTypeFilter.BOOLEAN_FILTER;
            }
            List<String> candidates = filter.filter(joinLayer);
            candidates.add(0, DO_NOT_JOIN);
            candidates.remove(joinLayerId);
            dialog.addComboBox(name,
                    candidates.contains(name) && type != AttributeType.GEOMETRY ?
                            name :
                            DO_NOT_JOIN,
                    candidates, "");
        }
        return dialog;
    }

    // Get mapping between base attributes and join attributes
    private void getMapping(MultiInputDialog dialog) {
        FeatureSchema schema = referenceLayer.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            if (!dialog.getText(schema.getAttributeName(i)).equals("")) {
                attributesMapping.put(schema.getAttributeName(i), dialog.getText(schema.getAttributeName(i)));
            }
        }
    }
}
