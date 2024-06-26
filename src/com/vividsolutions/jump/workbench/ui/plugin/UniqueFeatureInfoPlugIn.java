package com.vividsolutions.jump.workbench.ui.plugin;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Iterator;

public class UniqueFeatureInfoPlugIn extends AbstractPlugIn {

  private static EnableCheck checker = null;

  public UniqueFeatureInfoPlugIn() {
    this.setShortcutKeys(KeyEvent.VK_I);
    this.setShortcutModifiers(KeyEvent.ALT_MASK);
  }

  public static final ImageIcon ICON = IconLoader.icon("information_16x16.png");

  public static EnableCheck createEnableCheck(
      WorkbenchContext workbenchContext) {
    if (checker == null) {
      EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
      checker = new MultiEnableCheck()
          .add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
          .add(checkFactory.createWindowWithLayerManagerMustBeActiveCheck())
          .add(
              checkFactory
                  .createWindowWithAssociatedTaskFrameMustBeActiveCheck())
          .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1));
    }
    return checker;
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);
    // Deactivate synchronization
    //Don't pass in TaskFrame as LayerManagerProxy, because the TaskFrame may
    //be closed and thus the LayerManagerProxy may return null. [Jon Aquino]
    TaskFrame taskFrame = ((TaskFrameProxy) context.getActiveInternalFrame()).getTaskFrame();
    InfoFrame infoFrame = taskFrame.getInfoFrame();
    SelectionManager selectionManager =
        ((SelectionManagerProxy) context.getActiveInternalFrame()).getSelectionManager();
    infoFrame.getModel().clear();

    // TODO should be nice to extend FeatureInfoPlugIn to non-layer layerables
    for (Iterator<Layer> i = context.getLayerManager().iterator(Layer.class); i.hasNext(); ) {
      Layer layer = i.next();

      if (selectionManager.getFeaturesWithSelectedItems(layer).isEmpty()) {
        continue;
      }
      //infoFrame.getModel().remove(layer);
      infoFrame.getModel().add(layer, selectionManager.getFeaturesWithSelectedItems(layer));
    }
    infoFrame.surface();

    return true;
  }
}
