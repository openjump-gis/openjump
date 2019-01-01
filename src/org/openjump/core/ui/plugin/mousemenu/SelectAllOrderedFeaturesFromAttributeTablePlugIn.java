/*
 * Copyright (C) Michaël Michaud.
 */

package org.openjump.core.ui.plugin.mousemenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.OneLayerAttributeTab;
import com.vividsolutions.jump.workbench.ui.plugin.ViewAttributesPlugIn;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Selects all features of the active AttributeTable, preserving order.
 *
 * @author mmichaud
 */
public class SelectAllOrderedFeaturesFromAttributeTablePlugIn extends AbstractPlugIn {


  public SelectAllOrderedFeaturesFromAttributeTablePlugIn() {
    super();
  }

  public void initialize(PlugInContext context) throws Exception {

  }

  public boolean execute(PlugInContext context) throws Exception {

    int count = 0;
    if (context.getActiveInternalFrame() instanceof ViewAttributesPlugIn.ViewAttributesFrame) {
      OneLayerAttributeTab attributeTab = ((ViewAttributesPlugIn.ViewAttributesFrame)context
              .getActiveInternalFrame()).getOneLayerAttributeTab();
      if (attributeTab.getLayer().isSelectable()) {
        Collection<Feature> features = new ArrayList<>();
        attributeTab.getPanel().getTablePanel(attributeTab.getLayer()).getTable().selectAll();
        for (int j = 0; j < attributeTab.getLayerTableModel().getRowCount(); j++) {
          features.add(attributeTab.getLayerTableModel().getFeature(j));
          count++;
        }
        context.getLayerViewPanel().getSelectionManager().unselectItems(attributeTab.getLayer(), features);
        context.getLayerViewPanel().getSelectionManager().getFeatureSelection()
                .selectItems(attributeTab.getLayer(), features);
      }
    }
    context.getWorkbenchFrame().setTimeMessage(
            "" + count + " " +
            I18N.get("org.openjump.core.ui.plugin.mousemenu.SelectAllOrderedFeaturesFromAttributeTablePlugIn.selected-features")
    );
    return true;
  }

}
