package com.vividsolutions.jump.workbench.plugin;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

import javax.swing.*;
import java.io.*;

/**
 * Stop recording a macro.
 * @author Micha&euml;l Michaud
 */
public class StopMacroPlugIn extends AbstractPlugIn implements MacroManager {

    private static final String MACRO_NAME = I18N.get("com.vividsolutions.jump.workbench.plugin.macro-name");
    private static final String MACRO_NAME_TOOLTIP = I18N.get("com.vividsolutions.jump.workbench.plugin.macro-name-tooltip");

    public StopMacroPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.CUSTOMIZE, "Macro"},
                getName() + "...", false, null,
                null, -1);
        new File("lib/ext/macro/").mkdir();
    }

    public static EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        return new EnableCheck() {
            public String check(JComponent component) {
                Object obj = workbenchContext.getBlackboard().get(StartMacroPlugIn.MACRO_STARTED);
                if (obj == null || obj.equals(Boolean.FALSE)) return "Macro recording is already off";
                else return null;
            }
        };
    }

    public boolean execute(PlugInContext context) throws Exception {
        try {
            if (context.getWorkbenchContext().getBlackboard().get(MACRO) != null) {
                context.getWorkbenchContext().getBlackboard().put(StartMacroPlugIn.MACRO_STARTED, false);
                Macro processes = (Macro) context.getWorkbenchContext().getBlackboard().get(MACRO);
                final MultiInputDialog dialog = new MultiInputDialog(
                        context.getWorkbenchFrame(), getName(), true);
                context.getWorkbenchContext().getBlackboard().put(MACRO, null);
                dialog.addTextField(MACRO_NAME, "Macro name", 16, new EnableCheck[]{
                        new EnableCheck() {
                            public String check(JComponent component) {
                                if(!dialog.getText(MACRO_NAME).matches("[^\\?%*:|\"<>\\.]+")) {
                                    return dialog.getText(MACRO_NAME) + " is not a valid macro name";
                                }
                                return null;
                            }
                        }
                }, MACRO_NAME_TOOLTIP);
                GUIUtil.centreOnWindow(dialog);
                dialog.setVisible(true);
                if (dialog.wasOKPressed()) {
                    String name = dialog.getText(MACRO_NAME);
                    Java2XML java2XML = new Java2XML();
                    File file = new File("lib/ext/macro/" + name + ".ojm");
                    file.getParentFile().mkdirs();
                    new Java2XML().write(processes, "processes", new File("lib/ext/macro/" + name + ".ojm"));
                    return true;
                }
                else {
                    return false;
                }
            } else {
                return false;
            }
        } catch(IOException e) {
            throw e;
        }
    }

}
