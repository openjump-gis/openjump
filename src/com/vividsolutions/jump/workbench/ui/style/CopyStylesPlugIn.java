package com.vividsolutions.jump.workbench.ui.style;

import java.util.Collection;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
/**
 * Copies the styles for a layer to a paste buffer
 * @author Martin Davis
 * @version 1.0
 */

public class CopyStylesPlugIn extends AbstractPlugIn {

  static Collection stylesBuffer = null;

  public CopyStylesPlugIn() {
  }

  public String getName() {
    return I18N.get("ui.style.CopyStylesPlugIn.copy-styles");
  }

  public static MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }

  public boolean execute(PlugInContext context) throws Exception {
  	reportNothingToUndoYet(context);
    final Layer layer = context.getSelectedLayer(0);
    stylesBuffer = layer.cloneStyles();
    return true;
  }
}