package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
import java.util.Collection;

import javax.swing.SwingUtilities;

import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

public abstract class AbstractAddDatastoreLayerPlugIn extends
        AbstractThreadedUiPlugIn {

    public boolean execute(final PlugInContext context) throws Exception {
        // The user may have added connections using the Connection Manager
        // Toolbox. So refresh the connectionComboBox.
        // [Jon Aquino 2005-03-15]
        panel(context).populateConnectionComboBox();
        OKCancelDialog dlg = getDialog(context);
        dlg.setVisible(true);

        return dlg.wasOKPressed();
    }

    public void run(TaskMonitor monitor, final PlugInContext context)
            throws Exception {
        final Layerable layer = createLayerable(panel(context), monitor,
                context);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Collection selectedCategories = context.getLayerNamePanel()
                        .getSelectedCategories();
                context
                        .getLayerManager()
                        .addLayerable(
                                selectedCategories.isEmpty() ? StandardCategoryNames.WORKING
                                        : selectedCategories.iterator().next()
                                                .toString(), layer);
            }
        });
    }

    protected abstract Layerable createLayerable(ConnectionPanel panel,
            TaskMonitor monitor, PlugInContext context) throws Exception;

    protected OKCancelDialog getDialog(PlugInContext context) {
        if (dialog == null) {
            // Cache the dialog between invocations of this menu item,
            // to preserve the dialog's useful cache of dataset names.
            // [Jon Aquino 2005-03-11]
            dialog = createDialog(context);
        }
        return dialog;
    }

    protected ConnectionPanel panel(PlugInContext context) {
        return (ConnectionPanel) getDialog(context).getCustomComponent();
    }

    private OKCancelDialog dialog;

    private OKCancelDialog createDialog(PlugInContext context) {
        OKCancelDialog dialog = new OKCancelDialog(context.getWorkbenchFrame(),
                getName(), true, createPanel(context),
                new OKCancelDialog.Validator() {
                    public String validateInput(Component component) {
                        return ((ConnectionPanel) component).validateInput();
                    }
                });
        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        return dialog;
    }

    protected abstract ConnectionPanel createPanel(PlugInContext context);
}