package org.openjump.core.ui.plugin.datastore;

import java.awt.Color;
import java.util.Collection;

import javax.swing.SwingUtilities;

import com.vividsolutions.jump.workbench.Logger;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
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
import java.util.ArrayList;
import java.util.List;

public class AddDataStoreLayerWizard extends AbstractWizardGroup {

  private static final String KEY = AddDataStoreLayerWizard.class.getName();
  private AddDataStoreLayerWizardPanel dataStoreWizardPanel;

  private WorkbenchContext workbenchContext;

  private ChooseProjectPanel chooseProjectPanel;

  public AddDataStoreLayerWizard(WorkbenchContext workbenchContext) {
    super(I18N.get(KEY), IconLoader.icon("database_add.png"),
        AddDataStoreLayerWizardPanel.class.getName());
    this.workbenchContext = workbenchContext;
    dataStoreWizardPanel = new AddDataStoreLayerWizardPanel(workbenchContext);
    addPanel(dataStoreWizardPanel);
    chooseProjectPanel = new ChooseProjectPanel(workbenchContext,
        dataStoreWizardPanel.getID());
    addPanel(chooseProjectPanel);
  }

  public String getFirstId() {
    String firstId = super.getFirstId();
    if (!chooseProjectPanel.hasActiveTaskFrame()
        && chooseProjectPanel.hasTaskFrames()) {
      chooseProjectPanel.setNextID(firstId);
      return chooseProjectPanel.getID();
    } else {
      return firstId;
    }
  }

  public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
    chooseProjectPanel.activateSelectedProject();
    try {
      AddDatastoreLayerPanel dataStorePanel = dataStoreWizardPanel.getDataStorePanel();
      if (dataStorePanel.validateInput() == null) {
        final List<Layer> layers = createLayers(dataStorePanel, monitor);

        // for all selected layers, create a new OJ layer
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            for (final Layer layer : layers) {
              Collection<Category> selectedCategories = workbenchContext
                      .getLayerableNamePanel().getSelectedCategories();
              LayerManager layerManager = workbenchContext.getLayerManager();
              String categoryName = StandardCategoryNames.WORKING;
              if (!selectedCategories.isEmpty()) {
                categoryName = selectedCategories.iterator().next().getName();
              }
              try {
                workbenchContext.getLayerViewPanel().getViewport().update();
              } catch (Exception e) {
                Logger.warn("Exception thrown by AddDataStoreLayerWizard during LayerViewPanel update", e);
                //throw NoninvertibleTransformationException;
              }
              try {
                layerManager.addLayerable(categoryName, layer);
              } catch (Exception e) {
                e.printStackTrace();
                Logger.warn("Exception thrown by AddDataStoreLayerWizard while adding the layer to the LayerManager", e);
              }
            }
          }
        });

        workbenchContext.getLayerViewPanel().getViewport().update();
      } else {
        throw new Exception(dataStorePanel.validateInput());
      }
    } catch (Exception e) {
      monitor.report(e);
      throw e;
    }
  }

  private Layer createLayer(final DataStoreLayer dsLayer,
      ConnectionDescriptor connectionDescriptor, TaskMonitor monitor) throws Exception {

    LayerManager layerManager = workbenchContext.getLayerManager();
    Color fillColor = layerManager.generateLayerFillColor();
    FeatureCollection featureCollection = AddNewLayerPlugIn.createBlankFeatureCollection();
    Layer layer = new Layer(dsLayer.getFullName(), fillColor, featureCollection,
        layerManager);
    
    String geometryAttributeName = dsLayer.getGeoCol().getName();
    String whereClause = dsLayer.getWhereClause();
    int limit = dsLayer.getLimit();
    boolean caching = dsLayer.isCaching();
    DataStoreDataSource ds = new DataStoreDataSource(dsLayer.getFullName(),
        geometryAttributeName, whereClause, limit, connectionDescriptor, caching,
        workbenchContext);

    DataSourceQuery dsq = new DataSourceQuery(ds, null, dsLayer.getFullName());

    layer.setDataSourceQuery(dsq);

    CoordinateSystemRegistry crsRegistry = CoordinateSystemRegistry.instance(workbenchContext.getBlackboard());
    try {
      layerManager.setFiringEvents(false); // added by michaudm on 2009-04-05
      // TODO : there is currently two different ways to fix the SRID
      // May need refactoring there
      // One is with a CoordinateSystemRegistry stored in the context blacboard
      // Other is with a "Style" which can be persisted with the layer
      load(layer, crsRegistry, monitor);
      SRIDStyle sridStyle = new SRIDStyle();
      sridStyle.setSRID(dsLayer.getGeoCol().getSRID());
      layer.addStyle(sridStyle);
      layerManager.setFiringEvents(true); // added by michaudm on 2009-04-05
    } finally {
      layerManager.setFiringEvents(true);
    }
    return layer;
  }

  private List<Layer> createLayers(final AddDatastoreLayerPanel panel,
      TaskMonitor monitor) throws Exception {
    ArrayList<Layer> ret = new ArrayList<>();
    List<DataStoreLayer> dsLayers = panel.getDatasetLayers();
    ConnectionDescriptor connectionDescriptor = panel.getConnectionDescriptor();

    for (DataStoreLayer dsl : dsLayers) {
      ret.add(createLayer(dsl, connectionDescriptor, monitor));
    }
    return ret;

  }

  public static void load(Layer layer, CoordinateSystemRegistry registry,
      TaskMonitor monitor) throws Exception {
    layer.setFeatureCollection(executeQuery(layer.getDataSourceQuery()
        .getQuery(), layer.getDataSourceQuery().getDataSource(), registry,
        monitor));
    layer.setFeatureCollectionModified(false);
  }

  private static FeatureCollection executeQuery(String query,
      DataSource dataSource, CoordinateSystemRegistry registry,
      TaskMonitor monitor) throws Exception {
    Connection connection = dataSource.getConnection();
    try {
      return dataSource.installCoordinateSystem(connection.executeQuery(query,
          monitor), registry);
    } finally {
      connection.close();
    }
  }

}
