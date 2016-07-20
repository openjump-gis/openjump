package org.openjump.core.ui.plugin.file;

import java.util.List;

import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.file.save.SaveFileWizard;
import org.openjump.core.ui.swing.wizard.WizardGroup;
import org.openjump.core.ui.swing.wizard.WizardGroupDialog;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class SaveWizardPlugIn extends AbstractThreadedUiPlugIn {

  private static final String KEY = SaveWizardPlugIn.class.getName();
  private static final String LASTWIZARDCLASSNAME = KEY + ".lastwizard";

  private static WizardGroupDialog dialog = null;
  private WizardGroup lastWizard;
  private Blackboard blackboard;

  public SaveWizardPlugIn() {
    super(I18N.get(KEY)+" (experimental)");
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
    blackboard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());

    String name = getName();
    if (!JUMPVersion.getRelease().equalsIgnoreCase("release"))
      FeatureInstaller.getInstance().addMainMenuPlugin(this,
          new String[] { MenuNames.FILE });

    // setWizard(new SaveFileWizard(context));
    // add file wiz
    addWizard(context.getWorkbenchContext(), new SaveFileWizard(context));
  }

  public boolean execute(PlugInContext context) throws Exception {
    Registry registry = workbenchContext.getRegistry();

    WorkbenchFrame workbenchFrame = context.getWorkbenchFrame();
    String name = getName();
    List<WizardGroup> wizards = registry.getEntries(KEY);
    WizardGroup lastwizard = null;
    //dialog = null;
    if (dialog == null) {
      dialog = new WizardGroupDialog(workbenchContext, workbenchFrame, name);

      String lastwizardid = String.valueOf(blackboard.get(LASTWIZARDCLASSNAME));
      for (WizardGroup wizard : wizards) {
        dialog.addWizard(wizard);
        if (wizard.getClass().getName().equals(lastwizardid)) {
          lastWizard = wizard;
        }
      }
    }
    
    // legalize selected layer name (to be used by contained wizards)
    String layerName = workbenchContext.getLayerNamePanel().getSelectedLayers()[0].
        getName().replaceAll("[/:\\\\><\\|]","_");
    dialog.setData(SaveFileWizard.DATAKEY_LAYERNAME, layerName);
    
    // activate initial wizard
    if (lastWizard != null)
      dialog.setSelectedWizard(lastWizard);
    else if (wizards.size() > 0)
      dialog.setSelectedWizard(wizards.get(0));

    dialog.pack();
    GUIUtil.centreOnWindow(dialog);
    // [mmichaud 2014-05-01] Setting focusable to false fixes a nasty bug
    // (#359) appeared with java 8 in AddWritableDataStoreLayerPanel and
    // AddDatastoreLayerPanel (dataset popup is closed immediately after
    // opening by an event related to this dialog...)
    dialog.setFocusable(false);
    dialog.setVisible(true);

    return dialog.wasFinishPressed();
  }

  public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
    WizardGroup wiz = dialog.getSelectedWizard();
    blackboard.put(LASTWIZARDCLASSNAME, wiz.getClass().getName());
    wiz.run(dialog, monitor);
  }

  public EnableCheck getEnableCheck() {
    EnableCheckFactory checkFactory = EnableCheckFactory.getInstance();
    return new MultiEnableCheck().add(
        checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck()).add(
        checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
  }
}
