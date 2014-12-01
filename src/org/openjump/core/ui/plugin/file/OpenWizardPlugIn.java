package org.openjump.core.ui.plugin.file;

import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.ImageIcon;

import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.file.open.OpenFileWizard;
import org.openjump.core.ui.swing.wizard.WizardGroup;
import org.openjump.core.ui.swing.wizard.WizardGroupDialog;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class OpenWizardPlugIn extends AbstractThreadedUiPlugIn {

  private static final String KEY = OpenWizardPlugIn.class.getName();

  private WizardGroupDialog dialog;
  private WizardGroup lastWizard;

  private static ImageIcon icon16 = IconLoader
      .icon("fugue/folder-horizontal-open_16.png");
  private static ImageIcon icon20 = IconLoader
      .icon("fugue/folder-horizontal-open_24x20.png");
  
  public OpenWizardPlugIn() {
    super(I18N.get(KEY), icon16);
    this.setShortcutKeys(KeyEvent.VK_O);
    this.setShortcutModifiers(KeyEvent.CTRL_MASK);
  }

  public static void addWizard(final WorkbenchContext workbenchContext,
    final WizardGroup wizard) {
    Registry registry = workbenchContext.getRegistry();
    registry.createEntry(KEY, wizard);
  }

  public void initialize(PlugInContext context) throws Exception {
    super.initialize(context);
    JUMPWorkbench workbench = workbenchContext.getWorkbench();
    WorkbenchFrame frame = workbench.getFrame();

    String name = getName();

    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
    featureInstaller.addMainMenuPlugin(this, new String[] {MenuNames.FILE});

    // Add tool-bar Icon
    WorkbenchToolBar toolBar = frame.getToolBar();
    toolBar.addPlugIn(1, this, icon20, enableCheck, workbenchContext);

    // Add to category pop-up menu
    featureInstaller.addPopupMenuPlugin(frame.getCategoryPopupMenu(), this, name, false, icon16, enableCheck);

    // shortcut
    AbstractPlugIn.registerShortcuts(this);
  }

  public boolean execute(PlugInContext context) throws Exception {
    Registry registry = workbenchContext.getRegistry();

    WorkbenchFrame workbenchFrame = context.getWorkbenchFrame();
    String name = getName();
    if (dialog == null) {
      dialog = new WizardGroupDialog(workbenchContext, workbenchFrame, name);

      List<WizardGroup> wizards = registry.getEntries(KEY);
      lastWizard = wizards.get(0);
      for (WizardGroup wizard : wizards) {
        dialog.addWizard(wizard);
        if (wizard instanceof OpenFileWizard) {
          lastWizard = wizard;
        }
      }

    }
    dialog.setSelectedWizard(lastWizard);
    dialog.pack();
    GUIUtil.centreOnWindow(dialog);
    // [mmichaud 2014-05-01] Setting focusable to false fixes a nasty bug
    // (#359) appeared with java 8 in AddWritableDataStoreLayerPanel and
    // AddDatastoreLayerPanel (dataset popup is closed immediately after
    // opening by an event related to this dialog...)
    dialog.setFocusable(false);
    dialog.setVisible(true);
    lastWizard = dialog.getSelectedWizard();
    if (dialog.wasFinishPressed()) {
      return true;
    }
    return false;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    WizardGroup wizard = dialog.getSelectedWizard();
    wizard.run(dialog, monitor);
  }
}
