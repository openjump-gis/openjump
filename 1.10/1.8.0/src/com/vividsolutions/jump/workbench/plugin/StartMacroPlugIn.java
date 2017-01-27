package com.vividsolutions.jump.workbench.plugin;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import javax.swing.*;

/**
 * Start recording a sequence of plugin execution
 * @author Micha&euml;l Michaud
 */
public class StartMacroPlugIn extends AbstractPlugIn implements MacroManager {

    public StartMacroPlugIn() {
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(
                this,
                new String[]{MenuNames.CUSTOMIZE, "Macro"},
                getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()), -1);
        if (context.getWorkbenchContext().getBlackboard().get(MACRO_STARTED) == null) {
            context.getWorkbenchContext().getBlackboard().put(MACRO_STARTED, Boolean.FALSE);
        }
        if (context.getWorkbenchContext().getBlackboard().get(MACRO_RUNNING) == null) {
            context.getWorkbenchContext().getBlackboard().put(MACRO_RUNNING, Boolean.FALSE);
        }
    }

    public static EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        return new EnableCheck() {
            public String check(JComponent component) {
                Object obj = workbenchContext.getBlackboard().get(MACRO_STARTED);
                if (obj != null && obj.equals(Boolean.TRUE)) return "Macro recording is already on";
                else return null;
            }
        };
    }

    public boolean execute(PlugInContext context) throws Exception {
        context.getWorkbenchContext().getBlackboard().put(MACRO_STARTED, true);
        System.out.println("MacroStarted: " + context.getWorkbenchContext().getBlackboard().get(MACRO_STARTED));
        context.getWorkbenchContext().getBlackboard().put(MACRO, new Macro());
        return true;
    }
}
