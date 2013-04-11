package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;

/**
 * Plugin for the AdvancedMeasureTool.
 * 
 * @author Matthias Scholz <ms@jammerhund.de>
 * @version 0.1
 */
public class AdvancedMeasurePlugin extends AbstractPlugIn {
    
	AdvancedMeasureTool advancedMeasureTool;

	@Override
	public void initialize(PlugInContext context) throws Exception {
		super.initialize(context);

		advancedMeasureTool = new AdvancedMeasureTool(context.getWorkbenchContext());
		context.getWorkbenchContext().getWorkbench().getFrame().getToolBar().addCursorTool(advancedMeasureTool, advancedMeasureTool.getToolbarButton());
		OptionsDialog.instance(context.getWorkbenchContext().getWorkbench()).addTab(I18N.get("org.openjump.core.ui.plugin.tools.AdvancedMeasurePlugin.OptionPanelTitle"), new AdvancedMeasureOptionsPanel(context.getWorkbenchContext()));
	}

	@Override
	public boolean execute(PlugInContext context) throws Exception {
		try {
			context.getLayerViewPanel().setCurrentCursorTool(advancedMeasureTool);
			return true;
		} catch (Exception e) {
			context.getWorkbenchFrame().warnUser("BIG PROBLEM :-(");
			context.getWorkbenchFrame().getOutputFrame().createNewDocument();
			context.getWorkbenchFrame().getOutputFrame().addText("AdvancedMeasurePlugin Exception:" + e.toString());
			return false;
		}
	}
}
