package org.openjump.core.ui.plugin.layer;

import javax.swing.JOptionPane;

import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ccordsys.utils.ProjUtils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class ChangeSRIDPlugIn extends AbstractPlugIn {
    public void initialize(PlugInContext context) throws Exception {
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory(
                context.getWorkbenchContext());
        EnableCheck enableCheck = new MultiEnableCheck().add(
                enableCheckFactory
                        .createWindowWithLayerManagerMustBeActiveCheck()).add(
                enableCheckFactory.createExactlyNLayersMustBeSelectedCheck(1));
        new FeatureInstaller(context.getWorkbenchContext()).addMainMenuPlugin(
                this, new String[] { MenuNames.LAYER }, getName() + "...",
                false, null, enableCheck);
    }

    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn.Change-SRID");
    }

    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final Layer layer = context.getSelectedLayer(0);
        final SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
        final int prjSRID = ProjUtils.SRID(layer);
        String input = null;

        input = (String) JOptionPane.showInputDialog(
                context.getWorkbenchFrame(), "SRID:", getName(),
                JOptionPane.PLAIN_MESSAGE, null, null, "" + prjSRID);
        if (input == null) {
            return false;
        }
        final int newSRID = Integer.parseInt(input);
        execute(new UndoableCommand(getName()) {
            public void execute() {
                if (newSRID != prjSRID) {
                    sridStyle.setSRID(newSRID);
                    sridStyle.updateSRIDs(layer);
                } else if (newSRID == prjSRID) {
                    // sridStyle.setSRID(oldSRID);
                    sridStyle.setEnabled(false);
                    layer.removeStyle(sridStyle);
                    layer.fireAppearanceChanged();
                }

            }

            public void unexecute() {
                sridStyle.setSRID(prjSRID);
                sridStyle.updateSRIDs(layer);
            }
        }, context);
        return true;
    }

}
