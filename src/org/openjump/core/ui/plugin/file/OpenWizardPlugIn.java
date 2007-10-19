package org.openjump.core.ui.plugin.file;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.file.open.OpenDataTypePanel;
import org.openjump.core.ui.swing.wizard.WizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class OpenWizardPlugIn extends AbstractThreadedUiPlugIn {

  private static final String KEY = OpenWizardPlugIn.class.getName();

  private OpenDataTypePanel openDataTypePanel;

  private WizardDialog dialog;

  public OpenWizardPlugIn() {
    super(I18N.get(KEY), IconLoader.icon("folder.png"));
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

    Icon icon = getIcon();
    String name = getName();

    FeatureInstaller featureInstaller = new FeatureInstaller(workbenchContext);
    featureInstaller.addMainMenuItem(new String[] {
      MenuNames.FILE
    }, this, 1);

    // Add tool-bar Icon
    WorkbenchToolBar toolBar = frame.getToolBar();
    toolBar.addPlugIn(1, this, icon, enableCheck, workbenchContext);

    // Add layer pop-up menu
    featureInstaller.addPopupMenuItem(frame.getCategoryPopupMenu(), this, name +"{pos:3}",
      false, icon, enableCheck);
  }

  public boolean execute(PlugInContext context) throws Exception {
    Registry registry = workbenchContext.getRegistry();

    dialog = new WizardDialog(context.getWorkbenchFrame(), getName(),
      context.getErrorHandler());
    List<WizardPanel> panels = new ArrayList<WizardPanel>();

    List<WizardGroup> wizards = registry.getEntries(KEY);
    for (WizardGroup wizardGroup : wizards) {
      wizardGroup.initialize(workbenchContext, dialog);
      panels.addAll(wizardGroup.getPanels());
      
    }

    openDataTypePanel = new OpenDataTypePanel(workbenchContext, dialog, wizards);
    panels.add(0, openDataTypePanel);

    dialog.init(panels.toArray(new WizardPanel[panels.size()]));
    dialog.pack();
    dialog.setVisible(true);
    if (dialog.wasFinishPressed()) {
      return true;
    }
    return false;
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    WizardGroup wizard = openDataTypePanel.getSlectedWizardGroup();
    wizard.run(dialog, monitor);
  }
}
