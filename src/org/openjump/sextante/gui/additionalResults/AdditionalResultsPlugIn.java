package org.openjump.sextante.gui.additionalResults;

import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import org.openjump.sextante.core.ObjectAndDescription;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class AdditionalResultsPlugIn extends AbstractPlugIn {

    private String sName = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result-viewer");
    private static String sWarning = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.List-of-results-is-empty");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        // context.getFeatureInstaller().addMainMenuPlugin(this,
        // new String[] { MenuNames.WINDOW }, getName(), false, getIcon(),
        // getEnableCheck());
        // super.initialize(context);

        // context.getFeatureInstaller().addMainMenuPlugin(this,
        // new String[] { MenuNames.WINDOW }, sName, false,
        // getColorIcon(), getEnableCheck()
        //
        // );

    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        return new MultiEnableCheck().add(resultListMustNotBeEmpy());
    }

    @Override
    public EnableCheck getEnableCheck() {
        return new MultiEnableCheck().add(resultListMustNotBeEmpy());

    }

    public static EnableCheck resultListMustNotBeEmpy() {
        return new EnableCheck() {
            @Override
            public String check(JComponent component) {
                ArrayList<ObjectAndDescription> m_Components = AdditionalResults.m_Components;
                if (!m_Components.isEmpty()) {
                    return null;

                }
                return sWarning;
            }
        };
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        ArrayList<ObjectAndDescription> m_Components = AdditionalResults.m_Components;
        if (m_Components == null || m_Components.size() == 0) {
            JOptionPane.showMessageDialog(null, sWarning, sName,
                    JOptionPane.WARNING_MESSAGE);
            return false;
        } else {

            for (JInternalFrame iFrame : context.getWorkbenchFrame()
                    .getInternalFrames()) {
                if (iFrame instanceof AdditionalResultsFrame) {

                    iFrame.toFront();
                    return true;

                }
            }
            AdditionalResultsFrame additionalResultsFrame = new AdditionalResultsFrame(
                    m_Components);

            context.getWorkbenchFrame()
                    .addInternalFrame(additionalResultsFrame);

        }
        return true;
    }

    public Icon getIcon() {
        ImageIcon icon = new ImageIcon(getClass().getResource(
                "application_view.png"));
        return GUIUtil.toSmallIcon(icon);
    }

    @Override
    public String getName() {
        return sName;
    }

}
