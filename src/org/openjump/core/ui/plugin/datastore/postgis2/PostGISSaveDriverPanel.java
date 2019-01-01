package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;
import org.openjump.core.ui.plugin.datastore.DataStoreSaveDriverPanel;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 * Panel containing user interface elements to save a layer into a PostGIS table.
 */
public class PostGISSaveDriverPanel extends DataStoreSaveDriverPanel {

    public PostGISSaveDriverPanel(PlugInContext context) {
        super(context);
    }

}
