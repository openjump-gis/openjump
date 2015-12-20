package org.openjump.core.ui.plugin.datastore;

import java.util.Collection;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.openjump.core.ui.images.IconLoader;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.RunDatastoreQueryPlugIn;

/**
 * <code>EditDatastoreQueryPlugIn</code> load the query used to create a layer
 * in the RunDatastoreQueryPanel.
 *
 * @author <a href="mailto:michael.michaud@free.fr">Micha&euml;l Michaud</a>
 */
public class EditDataStoreQueryPlugIn extends RunDatastoreQueryPlugIn {

  public static final ImageIcon ICON = IconLoader.icon("arrow_edit_sql.png");

  public static final String SQL_QUERY_KEY = "SQL Query";

  public static final String CONNECTION_DESCRIPTOR_KEY = "Connection Descriptor";

  protected final static int MAIN_COLUMN_WIDTH = 400;

  Map properties;

  public void initialize(PlugInContext context) throws Exception {
    WorkbenchContext workbenchContext = context.getWorkbenchContext();
    EnableCheck enableCheck = createEnableCheck(workbenchContext);
    FeatureInstaller installer = new FeatureInstaller(workbenchContext);
    JPopupMenu popupMenu = workbenchContext.getWorkbench().getFrame()
        .getLayerNamePopupMenu();
    installer.addPopupMenuItem(popupMenu, this,
        new String[] { MenuNames.DATASTORE }, getName(), false, ICON,
        enableCheck);
  }

  public String getName() {
    return I18N
        .get("org.openjump.core.ui.plugin.datastore.EditDataStoreQueryPlugIn.Edit-datastore-query");
  }

  public boolean execute(final PlugInContext context) throws Exception {
    Layer layer = context.getLayerNamePanel().getSelectedLayers()[0];
    properties = layer.getDataSourceQuery().getDataSource().getProperties();
    RunDatastoreQueryPanel panel = (RunDatastoreQueryPanel) panel(context);
    panel.populateConnectionComboBox();

    Object query = properties.get(SQL_QUERY_KEY);
    panel.setQuery(query.toString());

    panel.setLayerName(context.getLayerManager().uniqueLayerName(
        layer.getName()));
    OKCancelDialog dlg = getDialog(context);
    dlg.setVisible(true);
    return dlg.wasOKPressed();
  }

  protected ConnectionPanel createPanel(final PlugInContext context) {

    final ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) properties
        .get(CONNECTION_DESCRIPTOR_KEY);

    RunDatastoreQueryPanel panel = new RunDatastoreQueryPanel(
        context.getWorkbenchContext()) {

      public void populateConnectionComboBox() {
        Collection descriptors = connectionDescriptors();
        if (!descriptors.contains(connectionDescriptor)) {
          descriptors.add(connectionDescriptor);
        }
        getConnectionComboBox().setModel(
            new DefaultComboBoxModel(sortByString(descriptors.toArray())));
        getConnectionComboBox().setSelectedItem(connectionDescriptor);
      }
    };
    return panel;
  }

  /**
   * @param workbenchContext
   * @return an enable check
   */
  public EnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
    final WorkbenchContext wc = workbenchContext;
    EnableCheckFactory enableCheckFactory = new EnableCheckFactory(
        workbenchContext);
    MultiEnableCheck enableCheck = new MultiEnableCheck();
    enableCheck.add(enableCheckFactory
        .createWindowWithLayerManagerMustBeActiveCheck());
    enableCheck.add(enableCheckFactory
        .createExactlyNLayerablesMustBeSelectedCheck(1, Layerable.class));
    enableCheck.add(new EnableCheck() {
      public String check(javax.swing.JComponent component) {
        Layer[] selectedLayers = wc.getLayerNamePanel().getSelectedLayers();

        for (Layer layer : selectedLayers) {
          if (layer.getDataSourceQuery() == null
              || !(layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource)) {
            return I18N
                .get("org.openjump.core.ui.plugin.datastore.EditDataStoreQueryPlugIn.Exactly-one-datastore-query-layer-must-be-selected");
          }
        }
        return null;
      }
    });
    return enableCheck;
  }

}
