package org.openjump.core.ui.plugin.datastore.transaction;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import org.apache.log4j.Logger;
import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

import java.util.*;

/**
 * Central class to manage datastore transactions from OpenJUMP.
 * There is only one DataStoreTransactionManager (singleton) for the whole Workbench.
 * This transactionManager register layers with a RWDataStoreDataSource, and each
 * RWDataStoreDataSource record user edits (Evolutions) between two transactions.
 * The commit method of DataStoreTransactionManager call the commit method of each
 * registered RWDataStoreDataSource.
 */
public class DataStoreTransactionManager {

    final Logger LOG = Logger.getLogger(DataStoreTransactionManager.class);
    final String KEY = DataStoreTransactionManager.class.getName();

    private static DataStoreTransactionManager TXMANAGER = new DataStoreTransactionManager();
    private WeakHashMap<Layer,Task> registeredLayers = new WeakHashMap<Layer, Task>();

    /**
     * Get the unique DataStoreTransactionManager.
     * @return JUMPWorkbench's DataStoreTransactionManager
     */
    public static DataStoreTransactionManager getTransactionManager() {
        return TXMANAGER;
    }

    /**
     * Register a new Layer in the DataStoreTransactionManager.
     * Edits happening in this Layer will be recorded in the associated
     * RWDataStoreDataSource.
     * @param layer the layer to be listened to.
     * @param task the task in which the layer is.
     */
    public void registerLayer(Layer layer, Task task) {
        if (!containsLayerManager(task)) {
            task.getLayerManager().addLayerListener(getLayerListener());
        }
        registeredLayers.put(layer, task);
        LOG.info("Register layer '" + layer.getName() + "' (" + task.getName() + ") in the DataStoreTransactionManager");
    }

    private boolean containsLayerManager(Task task) {
        return registeredLayers.containsValue(task);
    }

    private LayerListener getLayerListener() {
        return new LayerAdapter() {
            public void featuresChanged(FeatureEvent e) {
                Layer layer = e.getLayer();
                if (registeredLayers.containsKey(layer)) {
                    DataSource dataSource = layer.getDataSourceQuery().getDataSource();
                    if (dataSource instanceof WritableDataStoreDataSource) {
                        WritableDataStoreDataSource source = (WritableDataStoreDataSource)dataSource;
                        if (e.getType() == FeatureEventType.ADDED) {
                            for (Object feature : e.getFeatures()) {
                                try {
                                    LOG.debug("FeatureEventType: ADDED " + ((Feature)feature).getID());
                                    source.addCreation((Feature)feature);
                                } catch (EvolutionOperationException ex) {
                                    LOG.error("Error during creation of feature " + ((Feature)feature).getID(), ex);
                                }
                            }
                        }
                        else if (e.getType() == FeatureEventType.DELETED) {
                            for (Object feature : e.getFeatures()) {
                                try {
                                    LOG.debug("FeatureEventType: DELETED " + ((Feature)feature).getID());
                                    source.addSuppression((Feature)feature);
                                } catch (EvolutionOperationException ex) {
                                    LOG.error("Error during suppression of feature " + ((Feature)feature).getID(), ex);
                                }
                            }
                        }
                        else if (e.getType() == FeatureEventType.ATTRIBUTES_MODIFIED) {
                            assert e.getFeatures().size() == e.getOldFeatureAttClones().size() :
                                    "There is a problem with the size of FeatureEvent collections";
                            Iterator oldFeatures = e.getOldFeatureAttClones().iterator();
                            for (Object feature : e.getFeatures()) {
                                try {
                                    LOG.debug("FeatureEventType: ATTRIBUTES_MODIFIED " + ((Feature)feature).getID());
                                    source.addModification((Feature)feature, (Feature)oldFeatures.next());
                                } catch (EvolutionOperationException ex) {
                                    LOG.error("Error during modification of feature " + ((Feature)feature).getID(), ex);
                                }
                            }
                        }
                        else if (e.getType() == FeatureEventType.GEOMETRY_MODIFIED) {
                            assert e.getFeatures().size() == e.getOldFeatureClones().size() :
                                    "There is a problem with the size of FeatureEvent collections";
                            Iterator oldFeatures = e.getOldFeatureClones().iterator();
                            for (Object feature : e.getFeatures()) {
                                try {
                                    LOG.debug("FeatureEventType: GEOMETRY_MODIFIED " + ((Feature)feature).getID());
                                    source.addModification((Feature)feature, (Feature)oldFeatures.next());
                                } catch (EvolutionOperationException ex) {
                                    LOG.error("Error during modification of feature " + ((Feature)feature).getID(), ex);
                                }
                            }
                        }
                        else {
                            LOG.error(e.getType() + " is an unknown FeatureEventType");
                        }
                    }
                    else {
                        LOG.error("DataStoreTransactionManager should never contain a reference to a layer which does not use a WritableDataStoreDataSource");
                    }
                }
            }
            public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.REMOVED) {
                    if (e.getLayerable() instanceof Layer && ((Layer) e.getLayerable()).getDataSourceQuery() != null) {
                        if (((Layer) e.getLayerable()).getDataSourceQuery().getDataSource() instanceof WritableDataStoreDataSource) {
                            ((WritableDataStoreDataSource)((Layer) e.getLayerable()).getDataSourceQuery().getDataSource()).getUncommittedEvolutions().clear();
                        }
                    }
                    registeredLayers.remove(e.getLayerable());
                    LOG.info("Unregister layer " + e.getLayerable().getName() + " from the DataStoreTransactionManager");
                }
            }
        };
    }

    /**
     * Get layers registered in the DataStoreTransactionManager.
     */
    public Collection<Layer> getLayers() {
        return registeredLayers.keySet();
    }

    public Task getTask(Layer layer) {
        return registeredLayers.get(layer);
    }

    public Collection<Layer> getLayers(Task task) {
        Collection<Layer> layers = new ArrayList<Layer>();
        for (Map.Entry<Layer,Task> entry : registeredLayers.entrySet()) {
            if (entry.getValue() == task) layers.add(entry.getKey());
        }
        return layers;
    }

    /**
     * Commit all edits performed on this layer since last commit.
     * @param layer
     */
    private boolean commit(Layer layer) throws Exception {
        DataSource source = layer.getDataSourceQuery().getDataSource();
        if (source instanceof WritableDataStoreDataSource) {
            WritableDataStoreDataSource writableSource = (WritableDataStoreDataSource)source;
            // @TODO rework how CREATE_TABLE property is managed

            // CREATE_TABLE should have been turned to off before, but I could not make it work
            // (see also WritableDataStoreDataSource#reloadDataFromDataStore)
            //source.getProperties().put(WritableDataStoreDataSource.CREATE_TABLE, false);
            try {
                LOG.info("Commit layer \"" + layer.getName() + "\"");
                writableSource.getConnection().executeUpdate(null,layer.getFeatureCollectionWrapper(), new DummyTaskMonitor());
            } catch (Exception e) {
                LOG.error("Error occurred while comitting layer \"" + layer.getName() + "\"", e);
                throw e;
            }
            return true;
        }
        return false;
    }

    /**
     * Update this layer from the datasource
     * @param layer
     */
    private int update(TaskFrame taskFrame, Layer layer) {
        DataSource source = layer.getDataSourceQuery().getDataSource();
        if (source instanceof WritableDataStoreDataSource) {
            WritableDataStoreDataSource writableSource = (WritableDataStoreDataSource)source;
            int conflicts = 0;
            try {
                LOG.info("Update layer \"" + layer.getName() + "\"");
                FeatureCollection fc = writableSource.getConnection().executeQuery(null, new DummyTaskMonitor());
                conflicts = manageConflicts(taskFrame, layer, fc);
                layer.getLayerManager().setFiringEvents(false);
                layer.setFeatureCollection(fc);
                LOG.info("" + fc.size() + " features uploaded");
                layer.getLayerManager().setFiringEvents(true);
                LOG.info("" + conflicts + " conflicts detected");
            } catch (Exception e) {
                LOG.error("Error occurred while updating layer \"" + layer.getName() + "\"", e);
                return -1;
            }
            return conflicts;
        }
        return -1;
    }

    /**
     * Update all layers associated to a RWDataStoreDataSource.
     */
    public void update(TaskFrame taskFrame) {
        LOG.info("Update project \"" + taskFrame.getTask().getName() + "\"");
        int total_conflicts = 0;
        boolean no_error = true;
        for (Layer layer : registeredLayers.keySet()) {
            if (taskFrame.getLayerManager().getLayers().contains(layer)) {
                int conflicts = update(taskFrame, layer);
                if (conflicts < 0) no_error = false;
                else total_conflicts += conflicts;
            }
        }
        taskFrame.getLayerViewPanel().getSelectionManager().clear();
        taskFrame.getLayerViewPanel().repaint();
        if (no_error) {
            LOG.info("Project update finished with 0 error and " + total_conflicts + " conflicts");
        }
        else LOG.info("Project update finished with errors");
    }

    private void inspect(TaskFrame taskFrame, Layer layer) {
        DataSource source = layer.getDataSourceQuery().getDataSource();
        if (source instanceof WritableDataStoreDataSource) {
            WritableDataStoreDataSource writableSource = (WritableDataStoreDataSource)source;

            Layer layer_c = taskFrame.getLayerManager().getLayer(layer.getName()+"-uncommitted-creation");
            if (layer_c == null) {
                layer_c = taskFrame.getLayerManager().addLayer("Next-commit", layer.getName()+"-uncommitted-creation",
                        new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema()));
            } else layer_c.getFeatureCollectionWrapper().clear();

            Layer layer_m = taskFrame.getLayerManager().getLayer(layer.getName()+"-uncommitted-modification");
            if (layer_m == null) {
                layer_m = taskFrame.getLayerManager().addLayer("Next-commit", layer.getName()+"-uncommitted-modification",
                        new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema()));
            } else layer_m.getFeatureCollectionWrapper().clear();

            Layer layer_s = taskFrame.getLayerManager().getLayer(layer.getName()+"-uncommitted-suppression");
            if (layer_s == null) {
                layer_s = taskFrame.getLayerManager().addLayer("Next-commit", layer.getName()+"-uncommitted-suppression",
                        new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema()));
            } else layer_s.getFeatureCollectionWrapper().clear();

            for (Evolution evolution : writableSource.getUncommittedEvolutions()) {
                if (evolution.getType() == Evolution.Type.CREATION) {
                    layer_c.getFeatureCollectionWrapper().add(evolution.getNewFeature());
                }
                else if (evolution.getType() == Evolution.Type.MODIFICATION) {
                    layer_m.getFeatureCollectionWrapper().add(evolution.getOldFeature());
                }
                else if (evolution.getType() == Evolution.Type.SUPPRESSION) {
                    layer_s.getFeatureCollectionWrapper().add(evolution.getOldFeature());
                }
                else LOG.error("Tried to inspect an evolution which is neither a creation nor a modification or a suppression");
            }
        }
    }

    public void inspect(TaskFrame taskFrame) {
        for (Layer layer : registeredLayers.keySet()) {
            if (taskFrame.getTask().getLayerManager().getLayers().contains(layer)) {
                inspect(taskFrame, layer);
            }
        }
    }

    /**
     * Commit all edits permformed on all registered layers since last commit.
     */
    public void commit() throws Exception {
        TaskFrame activeFrame = JUMPWorkbench.getInstance().getFrame().getActiveTaskFrame();
        if (activeFrame == null) return;
        LOG.info("Commit evolutions on project \"" + activeFrame.getTask().getName() + "\"");
        boolean no_error = true;
        for (Layer layer : registeredLayers.keySet()) {
            if (activeFrame.getTask().getLayerManager().getLayers().contains(layer)) {
                no_error = commit(layer) && no_error;
            }
        }
        if (no_error) LOG.info("Commit finished without error");
        else LOG.info("Commit finished with error");
        update(activeFrame);
    }


    /**
     * Manage conflicts detected within a freshly updated FeatureCollection
     * @param taskFrame the active TaskFrame
     * @param layer the layer to which the FeatureCollection is attached
     * @param fc a feature collection just queried from the server
     * @return number of features in conflict
     */
    private int manageConflicts(final TaskFrame taskFrame, Layer layer, FeatureCollection fc) {
        // manageConflicts should never be called if its datasource is not a WritableDataStoreDataSource
        assert layer.getDataSourceQuery().getDataSource() instanceof WritableDataStoreDataSource;
        WritableDataStoreDataSource dataSource = (WritableDataStoreDataSource)layer.getDataSourceQuery().getDataSource();

        // get a table of current evolutions in the datasource associated to layer
        Map<Object,Evolution> index = dataSource.getIndexedEvolutions();
        String pk = dataSource.getProperties().get(WritableDataStoreDataSource.EXTERNAL_PK_KEY).toString();
        boolean manageConflicts = (Boolean)dataSource.getProperties().get(WritableDataStoreDataSource.MANAGE_CONFLICTS);
        int conflicts = 0;
        Collection toBeDeleted = new ArrayList();
        for (Object feature : fc.getFeatures()) {
            // For each feature of fc, check if this feature is also in the table of current evolutions
            // If a feature just updated from the server is still in the evolution map, we may have
            // a conflict
            Evolution evo = index.get(((Feature)feature).getAttribute(pk));
            if (evo != null) {
                if (evo.getType()==Evolution.Type.MODIFICATION) {
                    // The database feature has been changed since last commit
                    if (!Arrays.equals(((Feature) feature).getAttributes(), evo.getOldFeature().getAttributes())) {
                        // We must check if the remote change is the same as the local change
                        // If different, we have a conflict and must decide which one must be kept
                        if (!Arrays.equals(((Feature) feature).getAttributes(), evo.getNewFeature().getAttributes())) {
                            LOG.warn("Conflict detected for feature " + evo.getNewFeature().getAttribute(pk));
                            LOG.trace("  - Server: " + feature);
                            LOG.trace("  - Local : " + evo.getNewFeature());
                            conflicts++;
                            if (manageConflicts) {
                                copyLocallyModifiedFeature(taskFrame, layer, evo.getNewFeature());
                            }
                        }
                        // If remote change is the same as local change,
                        // we can remove the change from the evolution table
                        else {
                            dataSource.removeEvolution(evo.getNewFeature().getID());
                            LOG.trace("Eliminate an evolution from evolution stack after detection of a false conflict: "
                                    + evo.getNewFeature().getAttribute(pk));
                        }
                    }
                    else {
                        LOG.trace("Database has not been changed since last transaction : Keep local changes");
                        ((Feature)feature).setAttributes(evo.getNewFeature().getAttributes());
                    }
                }
                if (evo.getType()==Evolution.Type.SUPPRESSION) {
                    if (!Arrays.equals(((Feature) feature).getAttributes(), evo.getOldFeature().getAttributes())) {
                        LOG.warn("Conflict detected for feature " + evo.getNewFeature().getAttribute(pk));
                        LOG.trace("  - The feature has been locally deleted");
                        conflicts++;
                        copyLocallyDeletedFeature(taskFrame, layer, evo.getOldFeature());
                    }
                    // Database feature has not been changed : keep the local change (delete it)
                    else {
                        LOG.trace("Feature " + ((Feature) feature).getAttribute(pk) + " has been locally deleted, don't update it again !");
                        toBeDeleted.add(evo.getOldFeature());
                    }
                }
            }
        }
        fc.removeAll(toBeDeleted);
        for (Evolution evo : dataSource.getUncommittedEvolutions()) {
            if (evo.getType() == Evolution.Type.CREATION) {
                fc.add(evo.getNewFeature());
            }
        }
        return conflicts;
    }

    /**
     * Copy a feature modified locally to the conflict-layer.
     * Used by {@link #manageConflicts(TaskFrame,Layer,FeatureCollection)}.
     * @param taskFrame if conflict-layer does not already exist, it is added to this task
     * @param layer the conflict-layer is named after this layer
     * @param feature local feature to be copied to the conflict layer
     */
    private void copyLocallyModifiedFeature(TaskFrame taskFrame, Layer layer, Feature feature) {
        if (taskFrame != null) {
            Layer layer_m = taskFrame.getLayerManager().getLayer(layer.getName()+"-conflict-modification");
            if (layer_m == null) {
                layer_m = taskFrame.getLayerManager().addLayer("Conflict", layer.getName()+"-conflict-modification",
                        new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema()));
            }
            layer_m.getFeatureCollectionWrapper().add(feature);
        }
    }

    /**
     * Copy a feature deleted locally to the conflict-layer.
     * Used by {@link #manageConflicts(TaskFrame,Layer,FeatureCollection)}.
     * @param taskFrame if conflict-layer does not already exist, it is added to this task
     * @param layer the conflict-layer is named after this layer
     * @param feature local feature to be copied to the conflict layer
     */
    private void copyLocallyDeletedFeature(TaskFrame taskFrame, Layer layer, Feature feature) {
        if (taskFrame != null) {
            Layer layer_m = taskFrame.getLayerManager().getLayer(layer.getName()+"-conflict-suppression");
            if (layer_m == null) {
                layer_m = taskFrame.getLayerManager().addLayer("Conflict", layer.getName()+"-conflict-suppression",
                        new FeatureDataset(layer.getFeatureCollectionWrapper().getFeatureSchema()));
            }
            layer_m.getFeatureCollectionWrapper().add(feature);
        }
    }

}
