package com.vividsolutions.jump.workbench.ui.plugin;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.*;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;


public class MapToolTipsPlugIn extends AbstractPlugIn {
    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck().add(
            checkFactory
                .createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(new EnableCheck() {
            public String check(JComponent component) {
                ((JCheckBoxMenuItem) component).setSelected(
                    workbenchContext.getLayerViewPanel().getToolTipWriter().isEnabled());
                return null;
            }
        });
    }
    public String getName() {
        //Can't use auto-naming, which produces "Map Tool Tips"; and Unix/Windows
        //CVS issues will occur if I rename MapToolTipsPlugIn to MapTooltipsPlugIn. [Jon Aquino]
		return I18N.get("ui.plugin.MapToolTipsPlugIn.map-tooltips");
	}
    public boolean execute(PlugInContext context) throws Exception {
        context.getLayerViewPanel().getToolTipWriter().setEnabled(
            !context.getLayerViewPanel().getToolTipWriter().isEnabled());
        return true;
    }
}
