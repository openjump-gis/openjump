package com.vividsolutions.jump.workbench.ui.plugin.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.ImageIcon;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;
import com.vividsolutions.jump.workbench.ui.plugin.ExportImagePlugIn;

public class CopyImagePlugIn extends ExportImagePlugIn {

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[] { MenuNames.FILE });
  }

  public boolean execute(PlugInContext context) throws Exception {
    Transferable transferable = createTransferable(context);
    if (transferable == null) {
      context
          .getWorkbenchFrame()
          .warnUser(
              I18N.get("ui.plugin.clipboard.CopyImagePlugIn.could-not-copy-the-image-for-some-reason"));
      return false;
    }
    Toolkit.getDefaultToolkit().getSystemClipboard()
        .setContents(transferable, new DummyClipboardOwner());
    return true;
  }

  public static final ImageIcon ICON = IconLoaderFamFam.icon("image_copy.gif");

  private Transferable createTransferable(final PlugInContext context) {
    return new AbstractTransferable(new DataFlavor[] { DataFlavor.imageFlavor }) {
      public Object getTransferData(DataFlavor flavor)
          throws UnsupportedFlavorException, IOException {
        Assert.isTrue(flavor == DataFlavor.imageFlavor);
        return image(context.getLayerViewPanel());
      }
    };
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
    return new MultiEnableCheck().add(checkFactory.createTaskWindowMustBeActiveCheck());
  }
}