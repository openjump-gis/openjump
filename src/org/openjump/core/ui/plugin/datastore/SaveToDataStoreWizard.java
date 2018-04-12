package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.SQLUtil;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.famfam.IconLoaderFamFam;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;


/**
 * Wizard to save a dataset to a WritableDataStoreDataSource
 */
public class SaveToDataStoreWizard extends AbstractWizardGroup {

  /** The key for the wizard. */
  public static final String KEY = SaveToDataStoreWizard.class.getName();

  private DataStoreTransactionManager txManager;

  /** The plugin context. */
  private PlugInContext context;

  //private File file;

  public SaveToDataStoreWizard(final PlugInContext context, DataStoreTransactionManager txManager) {
    super(I18N.get(KEY), IconLoaderFamFam.icon("database_save.png"), SaveToDataStorePanel.KEY);
    this.txManager = txManager;
    this.context = context;
  }

  @Override
  public void initialize(WorkbenchContext workbenchContext, WizardDialog dialog) {
//    //for debugging
//    removeAllPanels();
    // already initialized
    if (!getPanels().isEmpty())
      return;

    SaveToDataStorePanel saveToDataStorePanel = new SaveToDataStorePanel(workbenchContext);
    saveToDataStorePanel.setDialog(dialog);
    addPanel(saveToDataStorePanel);
  }


  @Override
  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
    // plugin context is initialized too early, we have to use workbenchContext
    // to get the current selected dataset
    Layer[] layers = context.getWorkbenchContext().getLayerableNamePanel().getSelectedLayers();
    if (layers.length == 0) throw new Exception("No layer has been selected");
    Layer layer = context.getWorkbenchContext().getLayerableNamePanel().getSelectedLayers()[0];
    FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
    String geometryAttributeName = schema.getAttributeName(schema.getGeometryIndex());
    //dialog.setData(geomName);

    WritableDataStoreDataSource writableDS = DataStoreDataSourceFactory.createWritableDataStoreDataSource(
            (ConnectionDescriptor)dialog.getData(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY),
            (String)dialog.getData(WritableDataStoreDataSource.DATASET_NAME_KEY),
            geometryAttributeName,
            WritableDataStoreDataSource.DEFAULT_PK_NAME,
            false,
            "org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager",
            context.getWorkbenchContext());

    writableDS.getProperties().put(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY,
            dialog.getData(WritableDataStoreDataSource.CONNECTION_DESCRIPTOR_KEY));
    writableDS.getProperties().put(WritableDataStoreDataSource.DATASET_NAME_KEY,
            dialog.getData(WritableDataStoreDataSource.DATASET_NAME_KEY));
    writableDS.getProperties().put(WritableDataStoreDataSource.CREATE_PK,
            dialog.getData(WritableDataStoreDataSource.CREATE_PK));
    writableDS.getProperties().put(WritableDataStoreDataSource.GEOM_DIM_KEY,
            dialog.getData(WritableDataStoreDataSource.GEOM_DIM_KEY));
    writableDS.getProperties().put(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY,
            dialog.getData(WritableDataStoreDataSource.NAN_Z_TO_VALUE_KEY));
    writableDS.getProperties().put(WritableDataStoreDataSource.NARROW_GEOMETRY_TYPE_KEY,
            dialog.getData(WritableDataStoreDataSource.NARROW_GEOMETRY_TYPE_KEY));
    writableDS.getProperties().put(WritableDataStoreDataSource.CONVERT_TO_MULTIGEOMETRY_KEY,
            dialog.getData(WritableDataStoreDataSource.CONVERT_TO_MULTIGEOMETRY_KEY));
    if ((boolean)dialog.getData(WritableDataStoreDataSource.CREATE_PK)) {
      writableDS.getProperties().put(WritableDataStoreDataSource.EXTERNAL_PK_KEY,
              WritableDataStoreDataSource.DEFAULT_PK_NAME);
    }

    writableDS.getProperties().put(
            WritableDataStoreDataSource.GEOMETRY_ATTRIBUTE_NAME_KEY,
            (boolean)dialog.getData(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES)?
                      SQLUtil.normalize(schema.getAttributeName(schema.getGeometryIndex()))
                      :schema.getAttributeName(schema.getGeometryIndex()));

    // OpenJUMP has now a better support of Coordinate System at
    // FeatureCollection and FeatureSchema level, but this one is simple
    // and makes it easy to set the SRID the user want before an update
    SRIDStyle sridStyle = (SRIDStyle)layers[0].getStyle(SRIDStyle.class);
    writableDS.getProperties().put(WritableDataStoreDataSource.SRID_KEY, sridStyle.getSRID());
    writableDS.getProperties().put(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES,
            (boolean)dialog.getData(WritableDataStoreDataSource.NORMALIZED_COLUMN_NAMES));
    //}

    writableDS.getProperties().put(WritableDataStoreDataSource.LIMITED_TO_VIEW, false);
    writableDS.getProperties().put(WritableDataStoreDataSource.MAX_FEATURES_KEY, Integer.MAX_VALUE);
    writableDS.getProperties().put(WritableDataStoreDataSource.WHERE_CLAUSE_KEY, "");
    writableDS.getProperties().put(WritableDataStoreDataSource.MANAGE_CONFLICTS, true);

    SaveToDataStoreDataSourceQuery query = new SaveToDataStoreDataSourceQuery(
            writableDS,
            (String)writableDS.getProperties().get(WritableDataStoreDataSource.DATASET_NAME_KEY),
            (String)writableDS.getProperties().get(WritableDataStoreDataSource.DATASET_NAME_KEY));
    query.setProperties(writableDS.getProperties());
    query.getDataSource().getConnection().executeUpdate(
            WritableDataStoreDataSource.DATASET_NAME_KEY,
            layer.getFeatureCollectionWrapper(),
            new DummyTaskMonitor());
    layer.setDataSourceQuery(query);
  }
}
