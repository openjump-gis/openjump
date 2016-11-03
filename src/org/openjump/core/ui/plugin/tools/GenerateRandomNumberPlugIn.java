package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Plugin to fill an attribute with randomly generated numbers
 */
public class GenerateRandomNumberPlugIn extends AbstractUiPlugIn {

    private static String LAYER        = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.layer");
    private static String ATTRIBUTE    = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.attribute");
    private static String ATTRIBUTE_TT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.attribute-tooltip");
    private static String RANDOM       = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.random-generators");
    private static String MIN          = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.min-value");
    private static String MIN_TT       = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.min-value-tooltip");
    private static String MAX          = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.max-value");
    private static String MAX_TT       = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.max-value-tooltip");

    private static String NO_CANDIDATE  = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.no-editable-layer-with-numeric-attribute");

    private Layer layer;
    private String attribute;
    private double min, max;

    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(
                this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES, RANDOM},
                getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))
                .add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        if (AttributeTypeFilter.NUMERIC_FILTER.filter(
                                workbenchContext.getLayerManager().getEditableLayers()).size() == 0) {
                            return NO_CANDIDATE;
                        }
                        return null;
                    }
                });
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
        boolean empty = checkAttributeEmpty();
        if (!empty) {
            OKCancelDialog okCancelDialog = new OKCancelDialog(dialog, "Attribute column is not empty", true,
                    new JLabel("Attribute column is not empty. Do you want to overwrite it ?"),
                    new OKCancelDialog.Validator() {
                        @Override
                        public String validateInput(Component component) {
                            return null;
                        }
                    });
            okCancelDialog.setVisible(true);
            if (!okCancelDialog.wasOKPressed()) {
                return false;
            }
        }
        computeRandomValues(context);
        return true;
    }

    private boolean checkAttributeEmpty() {
        for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
            if (f.getAttribute(attribute) != null) return false;
        }
        return true;
    }

    private void computeRandomValues(PlugInContext context) {
        Random rdm = new Random();
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        AttributeType type = schema.getAttributeType(attribute);
        final Collection<Feature> oldFeatures = new ArrayList<>();
        final Collection<Feature> newFeatures = new ArrayList<>();
        reportNothingToUndoYet(context);
        for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
            oldFeatures.add(f.clone(true, true));
            f = f.clone(true, true);
            if (type == AttributeType.DOUBLE) {
                f.setAttribute(attribute, min + rdm.nextDouble()*(max-min));
            } else if (type == AttributeType.INTEGER) {
                f.setAttribute(attribute, (int)min + rdm.nextInt((int)(max-min)));
            } else if (type == AttributeType.LONG) {
                f.setAttribute(attribute, (long)min + (long)rdm.nextInt((int)(max-min)));
            }
            newFeatures.add(f);
        }
        context.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                    new UndoableCommand(I18N.get(AutoAssignAttributePlugIn.class.getName())) {

                        public void execute() {
                            Iterator<Feature> newFeatIterator = newFeatures.iterator();
                            for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
                                f.setAttribute(attribute, newFeatIterator.next().getAttribute(attribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(newFeatures,
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, oldFeatures);
                        }

                        public void unexecute() {
                            Iterator<Feature> oldFeatIterator = oldFeatures.iterator();
                            for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
                                f.setAttribute(attribute, oldFeatIterator.next().getAttribute(attribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(
                                    layer.getFeatureCollectionWrapper().getFeatures(),
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, newFeatures);
                        }
                    };
            command.execute();
            layer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
        } finally {
            layer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        layer = context.getLayerableNamePanel().chooseEditableLayer();
        dialog.addLayerComboBox(LAYER, layer, context.getLayerManager());
        dialog.addLayerComboBox(LAYER, layer, null,
                AttributeTypeFilter.NUMERIC_FILTER.filter(context.getLayerManager().getEditableLayers()));
        dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMERIC_FILTER, ATTRIBUTE_TT);
        dialog.addDoubleField(MIN, 0, 12, MIN_TT);
        dialog.addDoubleField(MAX, 0, 12, MAX_TT);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER);
        attribute = dialog.getText(ATTRIBUTE);
        min = dialog.getDouble(MIN);
        max = dialog.getDouble(MAX);
    }
}
