package org.openjump.core.ui.plugin.file;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;
import org.openjump.core.ui.plugin.datastore.SaveToDataStoreWizard;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;
import org.openjump.core.ui.plugin.file.save.SaveToFileWizard;
import org.openjump.core.ui.swing.wizard.WizardGroup;
import org.openjump.core.ui.swing.wizard.WizardGroupDialog;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class SaveWizardPlugIn extends AbstractThreadedUiPlugIn {

  private static final String KEY = SaveWizardPlugIn.class.getName();
  private static final String LASTWIZARDCLASSNAME = KEY + ".lastwizard";
  public static final String DATAKEY_SIMPLIFIED_LAYERNAME = KEY
      + ".simplified-layername";
  public static final String DATAKEY_SELECTED_LAYERABLES = KEY
      + ".selected-layerables";

  private static WizardGroupDialog dialog = null;
  private WizardGroup lastWizard;
  private Blackboard blackboard;

  public SaveWizardPlugIn() {
    super(I18N.get(KEY) + " (experimental)");
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

//    String name = getName();
//    if (!JUMPVersion.getRelease().equalsIgnoreCase("release"))
//      FeatureInstaller.getInstance().addMainMenuPlugin(this,
//          new String[] { MenuNames.FILE });

    // add each wiz one by one
    addWizard(context.getWorkbenchContext(), new SaveToFileWizard(context));
    // datastores are experimental for now
    if (!JUMPVersion.getRelease().equalsIgnoreCase("release"))
      addWizard(context.getWorkbenchContext(),
              new SaveToDataStoreWizard(context,
                      DataStoreTransactionManager.getTransactionManager()));
  }

  public boolean execute(PlugInContext pluginContext) throws Exception {
    Registry registry = workbenchContext.getRegistry();

    List<WizardGroup> wizards = registry.getEntries(KEY);
    WizardGroup lastwizard = null;
    //dialog = null;
    if (dialog == null) {
      WorkbenchFrame workbenchFrame = pluginContext.getWorkbenchFrame();
      String name = getName();
      dialog = new WizardGroupDialog(workbenchContext, workbenchFrame, name);

      String lastwizardid = String.valueOf(blackboard.get(LASTWIZARDCLASSNAME));
      for (WizardGroup wizard : wizards) {
        dialog.addWizard(wizard);
        if (wizard.getClass().getName().equals(lastwizardid)) {
          lastWizard = wizard;
        }
      }
    }

    // fetch the selected layer from the plugincontext
    Collection<Layerable> layers = pluginContext.getSelectedLayerables();
    // save it to dialog context for use in subsequent wizards
    dialog.setData(DATAKEY_SELECTED_LAYERABLES, layers);

    // legalize selected layer's name (to be used by contained wizards)
    Layerable layer = layers.iterator().next();
    String layerName = layer.getName().replaceAll("[/:\\\\><\\|]", "_");
    dialog.setData(DATAKEY_SIMPLIFIED_LAYERNAME, layerName);

    // activate initial wizard
    if (lastWizard != null)
      dialog.setSelectedWizard(lastWizard);
    // or activate first
    else if (dialog.getWizardCount() > 0)
      dialog.setSelectedWizard(dialog.getWizardAt(0));

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

  @Override
  public Icon getIcon() {
    return IconLoader.icon("disk_dots.png");
  }

  @Override
  public String getName() {
    return I18N.get("com.vividsolutions.jump.workbench.datasource.SaveDatasetAsPlugIn") +" (testing)";
  }
}
