package org.openjump.core.ui.plugin.queries;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * SimpleQueryPlugIn is a query editor and processor.
 * It has the following capabilities :
 * <OL><LI>query one or more layers</LI>
 * <LI>attribute queries and spatial queries</LI>
 * <LI>numerical and string functions</LI>
 * <LI>regular expression to find strings</LI>
 * <LI>results as a selection, a table or a new layer</LI></OL>
 * Version 0.2 of the SimpleQueryPlugIn is an adaptation of the original
 * version to the core of OpenJUMP (refactoring, internationalization)
 * @author Micha&euml;l MICHAUD
 * @version 0.2 (16 Oct 2005)
 */ 
public class SimpleQueryPlugIn extends AbstractPlugIn {
    static QueryDialog queryDialog;

    public void initialize(PlugInContext context) throws Exception {

            context.getFeatureInstaller().addMainMenuPlugin (this,
                    new String[]{MenuNames.TOOLS, MenuNames.TOOLS_QUERIES},
                    this.getName() + "...", false,
                    IconLoader.icon("simple_query.png"),
                    getEnableCheck());
    }

    public EnableCheck getEnableCheck() {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance();

        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
                                                      
    public boolean execute(PlugInContext context) throws Exception {
        if (queryDialog==null) {
            queryDialog = new QueryDialog(context);
        }
        else {
            queryDialog.setVisible(true);
            // Refresh layer list in case the user switched to another project
            queryDialog.initComboBoxes();
        }
        return false;
    }
    
    public String getName() {
      return I18N.get("org.openjump.core.ui.plugin.queries.SimpleQuery.menuitem");
    }

}
