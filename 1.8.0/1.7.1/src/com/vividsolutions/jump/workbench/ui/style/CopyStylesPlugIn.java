package com.vividsolutions.jump.workbench.ui.style;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.util.Collection;
import javax.swing.ImageIcon;

/**
 * Copies the styles for a layer to a paste buffer
 * @author Martin Davis
 * @version 1.0
 */

public class CopyStylesPlugIn extends AbstractPlugIn {

  static Collection stylesBuffer = null;
  static boolean isScaleDependentRenderingEnabled = false;
  static double minScale = 1.0;
  static double maxScale = 1.0;

  public CopyStylesPlugIn() {
  }

  public String getName() {
    return I18N.get("ui.style.CopyStylesPlugIn.copy-styles");
  }
  
  public ImageIcon getIcon() {
    return IconLoader.icon("Palette_in.gif");
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
    isScaleDependentRenderingEnabled = layer.isScaleDependentRenderingEnabled();
    minScale = layer.getMinScale() == null ? 1.0 : layer.getMinScale();
    maxScale = layer.getMaxScale() == null ? 1.0 : layer.getMaxScale();
    return true;
  }
}