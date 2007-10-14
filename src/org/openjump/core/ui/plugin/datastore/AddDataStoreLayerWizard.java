package org.openjump.core.ui.plugin.datastore;

import java.awt.Color;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.openjump.core.ui.plugin.file.OpenProjectPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class AddDataStoreLayerWizard extends AbstractWizardGroup {
  private static final String KEY = AddDataStoreLayerWizard.class.getName();

  private AddDataStoreLayerWizardPanel dataStoreWizardPanel;

  private WorkbenchContext workbenchContext;

  public AddDataStoreLayerWizard(WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), IconLoader.icon("Table.gif"),
      AddDataStoreLayerWizardPanel.class.getName());
    this.workbenchContext = workbenchContext;
    dataStoreWizardPanel = new AddDataStoreLayerWizardPanel(workbenchContext);
    addPanel(dataStoreWizardPanel);
  }

  public void run(WizardDialog dialog, TaskMonitor monitor) {
    try {
      AddDatastoreLayerPanel dataStorePanel = dataStoreWizardPanel.getDataStorePanel();
      final Layer layer = createLayer(dataStorePanel, monitor);
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          Collection<Category> selectedCategories = workbenchContext.getLayerNamePanel()
            .getSelectedCategories();
          LayerManager layerManager = workbenchContext.getLayerManager();
          String categoryName = StandardCategoryNames.WORKING;
          if (!selectedCategories.isEmpty()) {
            categoryName = selectedCategories.iterator().next().getName();
          }
          layerManager.addLayerable(categoryName, layer);
        }
      });
    } catch (Exception e) {
      monitor.report(e);
    }
  }

  private Layer createLayer(final AddDatastoreLayerPanel panel,
    TaskMonitor monitor) throws Exception {

    String datasetName = panel.getDatasetName();
    LayerManager layerManager = workbenchContext.getLayerManager();
    Color fillColor = layerManager.generateLayerFillColor();
    FeatureCollection featureCollection = AddNewLayerPlugIn.createBlankFeatureCollection();
    Layer layer = new Layer(datasetName, fillColor, featureCollection,
      layerManager);

    String geometryAttributeName = panel.getGeometryAttributeName();
    String whereClause = panel.getWhereClause();
    ConnectionDescriptor connectionDescriptor = panel.getConnectionDescriptor();
    boolean caching = panel.isCaching();
    DataStoreDataSource ds = new DataStoreDataSource(datasetName,
      geometryAttributeName, whereClause, connectionDescriptor, caching,
      workbenchContext);

    DataSourceQuery dsq = new DataSourceQuery(ds, null, datasetName);

    layer.setDataSourceQuery(dsq);

    CoordinateSystemRegistry crsRegistry = CoordinateSystemRegistry.instance(workbenchContext.getBlackboard());
    OpenProjectPlugIn.load(layer, crsRegistry, monitor);
    return layer;
  }

}
