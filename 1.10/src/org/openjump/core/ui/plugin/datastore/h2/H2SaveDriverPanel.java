package org.openjump.core.ui.plugin.datastore.h2;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.datastore.DataStoreSaveDriverPanel;

/**
 * Panel containing user interface elements to save a layer into a PostGIS table.
 */
public class H2SaveDriverPanel extends DataStoreSaveDriverPanel {

    public H2SaveDriverPanel(PlugInContext context) {
        super(context);
    }

}
