package org.openjump.core.ui.plugin.mousemenu;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.MoveSelectedItemsTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.CopySelectedItemsPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.clipboard.PasteItemsPlugIn;

public class DuplicateItemPlugIn extends AbstractPlugIn {

  public static ImageIcon ICON = IconLoader.icon("ItemDuplicate16.png");

  public DuplicateItemPlugIn() {
  }

  public String getName() {
    return I18N.getInstance().get("org.openjump.core.ui.plugin.mousemenu.DuplicateItemPlugIn");
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
  }

  public boolean execute(PlugInContext context) throws Exception {
    reportNothingToUndoYet(context);

    new CopySelectedItemsPlugIn().execute(context);
    new PasteItemsPlugIn().execute(context);
    context.getLayerViewPanel().setCurrentCursorTool(new MoveSelectedItemsTool(context.getWorkbenchContext()));

    return true;
  }

  public ImageIcon getIcon() {
    return ICON;
  }

  public String getNameWithMnemonic() {
    return StringUtil.replace(getName(), "c", "&c", false);
  }

  public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);

    return new MultiEnableCheck().add(checkFactory.createWindowWithSelectionManagerMustBeActiveCheck())
        .add(checkFactory.createAtLeastNItemsMustBeSelectedCheck(1))
        .add(checkFactory.createExactlyOneSelectedLayerMustBeEditableCheck());
  }

}
