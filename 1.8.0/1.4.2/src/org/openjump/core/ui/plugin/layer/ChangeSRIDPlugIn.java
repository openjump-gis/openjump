package org.openjump.core.ui.plugin.layer;

import javax.swing.JOptionPane;

import org.openjump.core.ccordsys.srid.SRIDStyle;

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
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory(context
                .getWorkbenchContext());
        EnableCheck enableCheck = new MultiEnableCheck().add(
                enableCheckFactory
                        .createWindowWithLayerManagerMustBeActiveCheck()).add(
                enableCheckFactory.createExactlyNLayersMustBeSelectedCheck(1));
        new FeatureInstaller(context.getWorkbenchContext())
                .addMainMenuItemWithJava14Fix(this, new String[]{MenuNames.LAYER},
                        getName() + "...", false, null, enableCheck);
//        new FeatureInstaller(context.getWorkbenchContext()).addPopupMenuItem(
//                context.getWorkbenchFrame().getLayerNamePopupMenu(), this,
//                getName() + "...", false, null, enableCheck);
    }
    
    public String getName(){
    	return I18N.get("org.openjump.core.ui.plugin.layer.ChangeSRIDPlugIn.Change-SRID"); 
    }
    
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final Layer layer = context.getSelectedLayer(0);
        final SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
        final int oldSRID = sridStyle.getSRID();
        String input = (String) JOptionPane.showInputDialog(context
                .getWorkbenchFrame(), "SRID:", getName(),
                JOptionPane.PLAIN_MESSAGE, null, null, "" + oldSRID);
        if (input == null) {
            return false;
        }
        final int newSRID = Integer.parseInt(input);
        execute(new UndoableCommand(getName()) {
            public void execute() {
                sridStyle.setSRID(newSRID);
                sridStyle.updateSRIDs(layer);
            }
            public void unexecute() {
                sridStyle.setSRID(oldSRID);
                sridStyle.updateSRIDs(layer);
            }
        }, context);
        return true;
    }
}
