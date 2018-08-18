package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
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
import java.util.concurrent.ThreadLocalRandom;

public class GenerateUniqueRandomIdPlugIn extends AbstractUiPlugIn {

  private static String LAYER        = I18N.get("ui.GenericNames.select-layer");
  private static String ATTRIBUTE    = I18N.get("ui.GenericNames.select-attribute");
  private static String RANDOM       = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandom");

  private static String NO_CANDIDATE = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomNumberPlugIn.no-editable-layer-with-numeric-attribute");
  private static String NON_EMPTY_ATT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.non-empty-attribute");
  private static String OVERWRITE_PROMPT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.overwrite-prompt");

  private Layer layer;
  private String attribute;

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
                if (AttributeTypeFilter.NUMSTRING_FILTER.filter(
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
      JLabel label = new JLabel();
      label.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      label.setText("<html><body>" + NON_EMPTY_ATT + "<br/>" + OVERWRITE_PROMPT + "</body></html>");
      OKCancelDialog okCancelDialog = new OKCancelDialog(dialog, NON_EMPTY_ATT, true,
              label,
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
    computeRandomUniqueValues(context);
    return true;
  }

  private boolean checkAttributeEmpty() {
    for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
      if (f.getAttribute(attribute) != null) return false;
    }
    return true;
  }

  private void computeRandomUniqueValues(PlugInContext context) {
    FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
    AttributeType type = schema.getAttributeType(attribute);
    final Collection<Feature> oldFeatures = new ArrayList<>();
    final Collection<Feature> newFeatures = new ArrayList<>();
    reportNothingToUndoYet(context);
    int fcSize = layer.getFeatureCollectionWrapper().getFeatures().size();
    Number[] numbers = new Number[fcSize];
    for (int i = 1 ; i <= numbers.length ; i++) {
      numbers[i-1] = i;
    }
    numbers = shuffle(numbers);
    int index = 0;
    String format = "%0" + ((int)Math.log10(fcSize)+1) + "d";
    for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
      oldFeatures.add(f.clone(true, true));
      f = f.clone(true, true);
      Number value = numbers[index++];
      if (type == AttributeType.DOUBLE) {
        f.setAttribute(attribute, value.doubleValue());
      } else if (type == AttributeType.INTEGER) {
        f.setAttribute(attribute, value.intValue());
      } else if (type == AttributeType.LONG) {
        f.setAttribute(attribute, value.longValue());
      } else if (type == AttributeType.STRING) {
        f.setAttribute(attribute, String.format(format, value));
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

  // Implementing Fisher-Yates shuffle as described in
  // https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
  private Number[] shuffle(Number[] array) {
    // If running on Java 6 or older, use `new Random()` on RHS here
    Random rnd = ThreadLocalRandom.current();
    for (int i = array.length - 1; i > 0; i--)
    {
      int index = rnd.nextInt(i + 1);
      // Simple swap
      Number a = array[index];
      array[index] = array[i];
      array[i] = a;
    }
    return array;
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    layer = context.getLayerableNamePanel().chooseEditableLayer();
    dialog.addLayerComboBox(LAYER, layer, null,
            AttributeTypeFilter.NUMSTRING_FILTER.filter(context.getLayerManager().getEditableLayers()));
    dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.NUMSTRING_FILTER, null);
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer = dialog.getLayer(LAYER);
    attribute = dialog.getText(ATTRIBUTE);
  }
}
