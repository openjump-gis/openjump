package org.openjump.core.ui.plugin.raster;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.*;
import org.openjump.core.rasterimage.IRasterSymbology;
import org.openjump.core.rasterimage.RasterImageLayer;

import java.util.Collection;

public class PasteRasterStylePlugIn extends AbstractPlugIn {

  //public void initialize(PlugInContext context) throws Exception {
  //  super.initialize(context);
  //  final EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(context.getWorkbenchContext());
  //  context.getFeatureInstaller().addMainMenuPlugin(this, new String[] { MenuNames.RASTER },
  //      getName(), false, null,
  //      new MultiEnableCheck()
  //          .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
  //          .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class)));
  //}

  @Override
  public String getName() {
    return I18N.JUMP.get(getClass().getName());
  }

  @Override
  public boolean execute(PlugInContext context) throws Exception {
    Collection<RasterImageLayer> layers = context.getLayerNamePanel().selectedNodes(RasterImageLayer.class);
    if (layers.size() > 0) {
      for (RasterImageLayer layer : layers) {
        IRasterSymbology symbology = (IRasterSymbology) CopyRasterStylePlugIn.cache.get().clone();
        layer.setSymbology(symbology);
      }
      return true;
    } else {
      return false;
    }
  }

  public MultiEnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    final EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
    return new MultiEnableCheck()
        .add(checkFactory.createWindowWithAssociatedTaskFrameMustBeActiveCheck())
        .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(1, RasterImageLayer.class));
  }
}
