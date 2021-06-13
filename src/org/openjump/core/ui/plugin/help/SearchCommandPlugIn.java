package org.openjump.core.ui.plugin.help;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.SuggestTreeComboBox;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.analysis.GeometryFunction;
import org.openjump.core.rasterimage.RasterImageLayer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * A search toolbar to find an existing command in the menus, submenus and popup
 * menus of OpenJUMP.
 */
public class SearchCommandPlugIn extends AbstractPlugIn {

    static final String VIEW_POPUP = I18N.getInstance().get("ui.MenuNames.LAYERVIEWPANEL_POPUP");
    static final String LAYER_POPUP = I18N.getInstance().get("ui.MenuNames.LAYERNAMEPANEL_LAYER_POPUP");
    static final String CATEGORY_POPUP = I18N.getInstance().get("ui.MenuNames.LAYERNAMEPANEL_CATEGORY_POPUP");

    public void initialize(PlugInContext context) throws Exception {

        context.getFeatureInstaller().addMainMenuPlugin (this,
                new String[]{MenuNames.HELP},
                this.getName() + "...", false,
                IconLoader.icon("search.png"), null);
    }

    public boolean execute(PlugInContext context) throws Exception {
        List<String> commands = new ArrayList();

        // Gather main menu items
        getMenus(context.getWorkbenchFrame().getJMenuBar(), new ArrayList(), commands);

        // Gather popup menu items from LayerNamePanel
        Map<Class<?>,JPopupMenu> map = context.getWorkbenchFrame().getNodeClassToPopupMenuMap();
        getMenus(map.get(Category.class), Arrays.asList(new String[]{CATEGORY_POPUP}), commands);
        getMenus(map.get(Layer.class), Arrays.asList(new String[]{LAYER_POPUP}), commands);
        getMenus(map.get(WMSLayer.class), Arrays.asList(new String[]{LAYER_POPUP}), commands);
        getMenus(map.get(RasterImageLayer.class), Arrays.asList(new String[]{LAYER_POPUP}), commands);

        // Gather popup menu items from LayerViewPanel
        getMenus(LayerViewPanel.popupMenu(), Arrays.asList(new String[]{VIEW_POPUP}), commands);

        // Gather geometry functions
        for (String s : GeometryFunction.getNames()) {
            //print(s);
            commands.add(s + " [" +
                    I18N.getInstance().get("ui.MenuNames.TOOLS") + ">" +
                    I18N.getInstance().get("ui.MenuNames.TOOLS.ANALYSIS") + ">" +
                    I18N.getInstance().get("com.vividsolutions.jump.workbench.ui.plugin.analysis.GeometryFunctionPlugIn") + "]");
        }

        // Gather Toolbox Cursor Tools
        EditingPlugIn editingPlugIn = (EditingPlugIn)context.getWorkbenchContext().getBlackboard().getProperties()
                .get("com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn");
        for (AbstractButton b : editingPlugIn.getToolbox().getButtons()) {
            commands.add(b.getToolTipText() + " [" +
                    I18N.getInstance().get("ui.cursortool.editing.EditingPlugIn.editing-toolbox") + "]");
        }

        // Gather main toolbar tools
        WorkbenchToolBar toolBar = context.getWorkbenchFrame().getToolBar();
        for (Component component : toolBar.getComponents()) {
            if (component instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) component;
                if (b.getToolTipText() != null) {
                    commands.add(b.getToolTipText() + " [" +
                            I18N.getInstance().get("Toolbar") + "]");
                }
            }
        }


        SuggestTreeComboBox stcb = new SuggestTreeComboBox(commands.toArray(new String[0]), 64);
        JDialog dialog = new JDialog(context.getWorkbenchFrame(), getName());
        dialog.add(stcb);
        dialog.setPreferredSize(new java.awt.Dimension(480,64));
        dialog.pack();
        com.vividsolutions.jump.workbench.ui.GUIUtil.centre(dialog, context.getWorkbenchFrame());
        dialog.setVisible(true);
        return true;
    }

    private void getMenus(MenuElement me, List<String> list, List<String> commands) {
        if (me instanceof JMenuBar) {
            for (MenuElement m : me.getSubElements()) {
                getMenus(m, new ArrayList(), commands);
            }
        }
        else if (me instanceof JPopupMenu) {
            for (MenuElement m : me.getSubElements()) {
                getMenus(m, list, commands);
            }
        }
        else if (me instanceof JMenu) {
            List mlist = new ArrayList(list);
            mlist.add(((JMenu) me).getText());
            for (MenuElement m : me.getSubElements()) {
                getMenus(m, mlist, commands);
            }
        }

        else if (me instanceof JMenuItem) {
            commands.add(((JMenuItem) me).getActionCommand() + " " + list.toString().replaceAll(", "," > "));
        }
        else if (me instanceof JCheckBoxMenuItem) {
            commands.add(((JCheckBoxMenuItem)me).getActionCommand() + " " + list.toString().replaceAll(", "," > "));
        }
        else if (me instanceof JRadioButtonMenuItem) {
            commands.add(((JRadioButtonMenuItem)me).getActionCommand() + " " + list.toString().replaceAll(", "," > "));
        }
    }

}
