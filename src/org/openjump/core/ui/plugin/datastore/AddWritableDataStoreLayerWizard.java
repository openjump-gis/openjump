package org.openjump.core.ui.plugin.datastore;

import java.awt.Color;
import java.util.Collection;

import javax.swing.*;

import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.plugin.datastore.transaction.DataStoreTransactionManager;
import org.openjump.core.ui.plugin.file.open.ChooseProjectPanel;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

/**
 * Add a wizard to the Open dialog to open database connections with read/write capability.
 */
public class AddWritableDataStoreLayerWizard extends AbstractWizardGroup {


    private static final String KEY = AddWritableDataStoreLayerWizard.class.getName();

    private AddWritableDataStoreLayerWizardPanel dataStoreWizardPanel;

    private DataStoreTransactionManager txManager;

    private WorkbenchContext workbenchContext;

    private ChooseProjectPanel chooseProjectPanel;

    public AddWritableDataStoreLayerWizard(
            String name,
            ImageIcon icon,
            WorkbenchContext workbenchContext,
            DataStoreTransactionManager txManager) {
        super(name, icon,
                AddWritableDataStoreLayerWizardPanel.class.getName());
        this.txManager = txManager;
        this.workbenchContext = workbenchContext;
        dataStoreWizardPanel = new AddWritableDataStoreLayerWizardPanel(workbenchContext);
        addPanel(dataStoreWizardPanel);
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext, dataStoreWizardPanel.getID());
        addPanel(chooseProjectPanel);
    }

    public AddWritableDataStoreLayerWizard(WorkbenchContext workbenchContext,
            DataStoreTransactionManager txManager) {
        super(I18N.get(KEY), IconLoader.icon("database_writable_add.png"),
                AddWritableDataStoreLayerWizardPanel.class.getName());
        this.txManager = txManager;
        this.workbenchContext = workbenchContext;
        dataStoreWizardPanel = new AddWritableDataStoreLayerWizardPanel(workbenchContext);
        addPanel(dataStoreWizardPanel);
        chooseProjectPanel = new ChooseProjectPanel(workbenchContext, dataStoreWizardPanel.getID());
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
            AddWritableDataStoreLayerPanel dataStorePanel = dataStoreWizardPanel.getDataStorePanel();
            if (dataStorePanel.validateInput() == null) {
                final Layer layer = createLayer(dataStorePanel, monitor);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
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
                            //throw NoninvertibleTransformationException;
                            Logger.warn("Exception thrown by AddWritableDataStoreLayerWizard while displaying data in the view", e);
                        }
                        layerManager.addLayerable(categoryName, layer);
                    }
                });
                workbenchContext.getLayerViewPanel().getViewport().update();
            }
            else throw new Exception(dataStorePanel.validateInput());
        } catch (Exception e) {
            monitor.report(e);
            throw e;
        }
    }

    private Layer createLayer(final AddWritableDataStoreLayerPanel panel,
                              TaskMonitor monitor) throws Exception {

        String datasetName = panel.getDatasetName();
        LayerManager layerManager = workbenchContext.getLayerManager();
        Color fillColor = layerManager.generateLayerFillColor();
        FeatureCollection featureCollection = AddNewLayerPlugIn.createBlankFeatureCollection();
        Layer layer = new Layer(datasetName, fillColor, featureCollection,
                layerManager);

        String geometryAttributeName = panel.getGeometryAttributeName();
        String identifierAttributeName = panel.getIdentifierAttributeName();
        String whereClause = panel.getWhereClause();
        int limit = panel.getMaxFeatures();
        ConnectionDescriptor connectionDescriptor = panel.getConnectionDescriptor();
        //boolean caching = panel.isCaching();
        boolean limitedToView = panel.isLimitedToView();
        boolean manageConflicts = panel.isManageConfictsActive();

        WritableDataStoreDataSource ds =
                DataStoreDataSourceFactory.createWritableDataStoreDataSource(
                        connectionDescriptor, datasetName, geometryAttributeName,
                        identifierAttributeName, true,
                        txManager, workbenchContext);
        ds.setMaxFeature(limit);
        ds.setWhereClause(whereClause);
        ds.setLimitedToView(limitedToView);
        ds.setManageConflicts(manageConflicts);
        ds.setWorkbenchContext(workbenchContext);
        ds.setMultiGeometry(panel.getGeometryColumn().getType().toLowerCase().startsWith("multi"));
        ds.setCoordDimension(panel.getGeometryColumn().getCoordDimension());
        ds.setSRID(panel.getGeometryColumn().getSRID());

        DataSourceQuery dsq = new DataSourceQuery(ds, null, datasetName);

        layer.setDataSourceQuery(dsq);

        CoordinateSystemRegistry crsRegistry = CoordinateSystemRegistry.instance(workbenchContext.getBlackboard());
        try {
            layerManager.setFiringEvents(false); // added by michaudm on 2009-04-05
            // TODO : there is currently two different ways to fix the SRID
            // May need refactoring there
            // One is with a CoordinateSystemRegistry stored in the context blackboard
            // Other is with a "Style" which can be persisted with the layer
            load(layer, crsRegistry, monitor);
            SRIDStyle sridStyle = new SRIDStyle();
            sridStyle.setSRID(panel.getGeometryColumn().getSRID());
            layer.addStyle(sridStyle);

            Logger.info("Add layer '" + layer.getName() + "' to '" + layerManager.getTask().getName() +  "' using WritableDataStoreDataSource with :");
            Logger.info("    geometry column    = " + geometryAttributeName);
            Logger.info("    is_multi           = " + panel.getGeometryColumn().getType().toLowerCase().startsWith("multi"));
            Logger.info("    coord_dimension    = " + panel.getGeometryColumn().getCoordDimension());
            Logger.info("    srid               = " + sridStyle.getSRID());
            Logger.info("    external PK column = " + identifierAttributeName);
            Logger.info("    max features       = " + limit);
            Logger.info("    where clause       = " + whereClause);
            Logger.info("    limit to view      = " + limitedToView);

            layerManager.setFiringEvents(true); // added by michaudm on 2009-04-05
        }
        finally {layerManager.setFiringEvents(true);}
        txManager.registerLayer(layer, workbenchContext.getTask());
        return layer;
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
            //dataSource.getProperties().put(WritableDataStoreDataSource.TABLE_CREATED,true);
            return dataSource.installCoordinateSystem(connection.executeQuery(query,
                    monitor), registry);
        } finally {
            connection.close();
        }
    }

}
