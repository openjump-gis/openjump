package com.vividsolutions.jump.workbench.ui.style;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

/**
 * Pastes the styles from the internal style paste buffer to a layer
 * @author Martin Davis
 * @version 1.0
 */

public class PasteStylesPlugIn extends AbstractPlugIn {

  public static MultiEnableCheck createEnableCheck(
      final WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1));
  }


  public PasteStylesPlugIn() {
  }

  public String getName() {
  	return I18N.get("ui.style.PasteStylesPlugIn.paste-styles");
  }

  public boolean execute(PlugInContext context) throws Exception
  {
    if (CopyStylesPlugIn.stylesBuffer == null)
      return false;
    Layer[] selectedLayers = context.getSelectedLayers();
    for (int i = 0; i < selectedLayers.length; i++) {
      pasteStyles(selectedLayers[i]);
    }
    return true;
  }

  private void pasteStyles(Layer layer)
  {
    layer.setStyles(CopyStylesPlugIn.stylesBuffer);
  }
}