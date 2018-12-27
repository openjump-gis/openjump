package org.openjump.core.ui.plugin.file.open;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.namespace.QName;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.*;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.model.TaskEvent;
import org.openjump.core.model.TaskListener;
import org.openjump.core.rasterimage.ImageAndMetadata;
import org.openjump.core.rasterimage.RasterImageIO;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.ui.plugin.file.FindFile;
import org.openjump.core.ui.plugin.file.OpenProjectPlugIn;
import org.openjump.core.ui.plugin.file.OpenRecentPlugIn;
import org.openjump.core.ui.swing.wizard.AbstractWizardGroup;
import org.openjump.util.metaData.MetaInformationHandler;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;

public class OpenProjectWizard extends AbstractWizardGroup {

    /** The key for the wizard. */
    public static final String KEY = OpenProjectWizard.class.getName();

    public static final String FILE_CHOOSER_DIRECTORY_KEY = KEY
            + " - FILE CHOOSER DIRECTORY";

    private WorkbenchContext workbenchContext;

    private SelectProjectFilesPanel selectProjectPanel;

    private Task sourceTask;

    private Task newTask;

    private File[] files;

    private Envelope savedTaskEnvelope = null;

    private boolean initialized = false;

    /**
     * Construct a new OpenFileWizard.
     * 
     * @param workbenchContext
     *            The workbench context.
     */
    public OpenProjectWizard(final WorkbenchContext workbenchContext) {
        super(I18N.get(KEY), OpenProjectPlugIn.ICON,
                SelectProjectFilesPanel.KEY);
        this.workbenchContext = workbenchContext;
    }

    public OpenProjectWizard(final WorkbenchContext workbenchContext,
            final File[] files) {
        this.workbenchContext = workbenchContext;
        this.files = files;
    }

    @Override
    public void initialize(WorkbenchContext workbenchContext,
            WizardDialog dialog) {
      // init only once
      if (initialized) return;

      selectProjectPanel = new SelectProjectFilesPanel(workbenchContext);
      selectProjectPanel.setDialog(dialog);
      addPanel(selectProjectPanel);

      initialized = true;
    }

    /**
     * Load the files selected in the wizard.
     * 
     * @param monitor
     *            The task monitor.
     * @throws Exception
     *            if an exception occurs during file opening
     */
    @Override
    public void run(WizardDialog dialog, TaskMonitor monitor) throws Exception {
        // local list for internal usage OR let user select via gui
        File[] selectedFiles = (files != null) ? files : selectProjectPanel
                .getSelectedFiles();
        open(selectedFiles, monitor);
    }

    private void open(File[] files, TaskMonitor monitor) throws Exception {
        for (File file : files) {
            open(file, monitor);
        }
    }

    public void open(File file, TaskMonitor monitor) throws Exception {

        // persist last used directory in workbench-state.xml
        Blackboard blackboard = PersistentBlackboardPlugIn
                .get(workbenchContext);
        blackboard.put(FILE_CHOOSER_DIRECTORY_KEY, file.getAbsoluteFile()
                .getParent());

        JUMPWorkbench workbench;
        WorkbenchFrame workbenchFrame = null;
        try (InputStream inputStream = new FileInputStream(file)) {
            workbench = workbenchContext.getWorkbench();
            workbenchFrame = workbench.getFrame();
            PlugInManager plugInManager = workbench.getPlugInManager();
            ClassLoader pluginClassLoader = plugInManager.getClassLoader();
            sourceTask = (Task) new XML2Java(pluginClassLoader).read(
                    inputStream, Task.class);
            initializeDataSources(sourceTask, workbenchFrame.getContext());
            newTask = new Task();
            newTask.setName(GUIUtil.nameWithoutExtension(file));
            newTask.setProjectFile(file);
            newTask.setProperties(sourceTask.getProperties());

            newTask.setTaskWindowLocation(sourceTask.getTaskWindowLocation());
            newTask.setTaskWindowSize(sourceTask.getTaskWindowSize());
            newTask.setMaximized(sourceTask.getMaximized());
            newTask.setSavedViewEnvelope(sourceTask.getSavedViewEnvelope());

            TaskFrame frame = workbenchFrame.addTaskFrame(newTask);
            Dimension size = newTask.getTaskWindowSize();
            if (size != null)
                frame.setSize(size);

            if (newTask.getMaximized())
                frame.setMaximum(true);
            savedTaskEnvelope = newTask.getSavedViewEnvelope();

            LayerManager sourceLayerManager = sourceTask.getLayerManager();
            LayerManager newLayerManager = newTask.getLayerManager();
            CoordinateSystemRegistry crsRegistry = CoordinateSystemRegistry
                    .instance(workbenchContext.getBlackboard());

            workbenchContext.getLayerViewPanel().setDeferLayerEvents(true);

            loadLayers(sourceLayerManager, newLayerManager, crsRegistry,
                    monitor);

            workbenchContext.getLayerViewPanel().setDeferLayerEvents(false);

            OpenRecentPlugIn.get(workbenchContext).addRecentProject(file);

        } catch (ClassNotFoundException e) {
            Logger.error(file.getPath() + " can not be loaded", e);
            workbenchFrame.warnUser("Missing class: " + e.getCause());
        } catch (Exception cause) {
            Exception e = new Exception(I18N.getMessage(KEY
                    + ".could-not-open-project-file-{0}-with-error-{1}",
                    file, cause.getLocalizedMessage()), cause);
            monitor.report(e);
            throw e;
        }
    }

    private void initializeDataSources(Task task, WorkbenchContext context) {
        LayerManager layerManager = task.getLayerManager();
        List<Layer> layers = layerManager.getLayers();
        List<Layer> layersToBeRemoved = new ArrayList<>();
        for (Layer layer : layers) {
            if (layer instanceof LayerView) {
                continue; // no datasource for LayerView
            }
            DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
            DataSource dataSource = dataSourceQuery.getDataSource();
            if (dataSource == null) {
                context.getWorkbench()
                        .getFrame()
                        .warnUser(
                                I18N.getMessage(KEY + ".datasource-not-found",
                                        layer.getName()));
                // context.getWorkbench().getFrame().warnUser("DataSource not found for "
                // + layer.getName());
                layerManager.remove(layer);
                continue;
            }
            if (dataSource instanceof WorkbenchContextReference) {
                try {
                    WorkbenchContextReference workbenchRef = (WorkbenchContextReference) dataSource;
                    workbenchRef.setWorkbenchContext(context);
                } catch (Exception e) {
                    int response = JOptionPane
                            .showConfirmDialog(
                                    workbenchContext.getWorkbench().getFrame(),
                                    "<html>"
                                            + I18N.getMessage(KEY
                                                    + ".opening-datasource-{0}-failed-with-error",
                                                    layer.getName())
                                            + "<br>"
                                            + StringUtil
                                                    .split(e.getLocalizedMessage(),
                                                            80).replaceAll(
                                                            "\n", "<br>")
                                            + "<br>"
                                            + I18N.get(KEY
                                                    + ".click-yes-to-continue-or-no-to-remove-layer")
                                            + "</html>", "OpenJUMP",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE);

                    if (response != JOptionPane.YES_OPTION) {
                        layersToBeRemoved.add(layer);
                    }
                }
            }
        }
        for (Layer layer : layersToBeRemoved)
            layerManager.remove(layer);
    }

    @SuppressWarnings("deprecation")
    private void loadLayers(LayerManager sourceLayerManager,
            LayerManager newLayerManager, CoordinateSystemRegistry registry,
            TaskMonitor monitor) throws Exception {
        JUMPWorkbench workbench = workbenchContext.getWorkbench();
        WorkbenchFrame workbenchFrame = workbench.getFrame();
        FindFile findFile = new FindFile(workbenchFrame);
        boolean displayDialog = true;

        String oldProjectPath = sourceTask.getProperty(new QName(
                Task.PROJECT_FILE_KEY));
        boolean updateResources = false;
        boolean updateOnlyMissingResources = false;
        File oldProjectFile = null;
        if (oldProjectPath != null && !oldProjectPath.equals("")) {
            oldProjectFile = new File(oldProjectPath);
            if (!oldProjectFile.equals(newTask.getProjectFile())) {
                JCheckBox checkbox = new JCheckBox(
                        I18N.get("ui.plugin.OpenProjectPlugIn.Only-for-missing-resources"));
                String message = I18N
                        .get("ui.plugin.OpenProjectPlugIn."
                                + "The-project-has-been-moved-Do-you-want-to-update-paths-below-the-project-folder");
                Object[] params = { message, checkbox };
                int answer = JOptionPane.showConfirmDialog(workbenchFrame,
                        params, "OpenJUMP", JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    updateResources = true;
                    if (checkbox.isSelected())
                        updateOnlyMissingResources = true;
                }
            }
        }

        try {
            List<Category> categories = sourceLayerManager.getCategories();
            for (Category sourceLayerCategory : categories) {
                newLayerManager.addCategory(sourceLayerCategory.getName());

                // LayerManager#addLayerable adds layerables to the top. So
                // reverse the order.
                ArrayList<Layerable> layerables = new ArrayList<>(
                        sourceLayerCategory.getLayerables());
                Collections.reverse(layerables);

                for (Layerable layerable : layerables) {
                    if (monitor != null) {
                        monitor.report(I18N
                                .get("ui.plugin.OpenProjectPlugIn.loading")
                                + " " + layerable.getName());
                    }
                    layerable.setLayerManager(newLayerManager);

                    if (layerable instanceof LayerView) {
                        //layerable.setLayerManager(newLayerManager);
                    }
                    else if (layerable instanceof Layer) {
                        Layer layer = (Layer) layerable;
                        File layerFile = getLayerFileProperty(layer);
                        if (!updateOnlyMissingResources || !layerFile.exists()) {
                            if (updateResources
                                    && layerFile != null
                                    && isLocatedBellow(
                                            oldProjectFile.getParentFile(),
                                            layerFile)) {
                                File newLayerFile = updateResourcePath(
                                        oldProjectFile,
                                        newTask.getProjectFile(), layerFile);
                                setLayerFileProperty(layer, newLayerFile);
                            }
                        }
                        try {
                            load(layer, registry, monitor);
                        } catch (FileNotFoundException ex) {
                            if (displayDialog) {
                                displayDialog = false;

                                int response = JOptionPane
                                        .showConfirmDialog(
                                                workbenchFrame,
                                                I18N.get("ui.plugin.OpenProjectPlugIn.At-least-one-file-in-the-task-could-not-be-found")
                                                        + "\n"
                                                        + I18N.get("ui.plugin.OpenProjectPlugIn.Do-you-want-to-locate-it-and-continue-loading-the-task"),
                                                "OpenJUMP",
                                                JOptionPane.YES_NO_OPTION);

                                if (response != JOptionPane.YES_OPTION) {
                                    break;
                                }
                            }

                            DataSourceQuery dataSourceQuery = layer
                                    .getDataSourceQuery();
                            DataSource dataSource = dataSourceQuery
                                    .getDataSource();
                            Map<String,Object> properties = dataSource.getProperties();
                            if (properties.get(DataSource.FILE_KEY) != null) {
                                String fname = properties.get(
                                        DataSource.FILE_KEY).toString();
                                String filename = findFile.getFileName(fname);
                                if (filename.length() > 0) {
                                    // set the new source for this layer
                                    properties.put(DataSource.FILE_KEY,
                                            filename);
                                    dataSource.setProperties(properties);
                                    load(layer, registry, monitor);
                                } else {
                                    break;
                                }
                            }
                        }
                    } else if (layerable instanceof RasterImageLayer) {

                        RasterImageLayer rasterImageLayer = (RasterImageLayer) layerable;
                        loadRasterImageLayer(workbenchContext,
                                rasterImageLayer,
                                rasterImageLayer.getSymbology(),
                                sourceLayerCategory);
                        continue;
                    }

                    newLayerManager.addLayerable(sourceLayerCategory.getName(),
                            layerable);
                }
            }
            // fire TaskListener's
            for (TaskListener taskListener : workbenchFrame.getTaskListeners()) {
                taskListener.taskLoaded(new TaskEvent(this, newLayerManager.getTask()));
            }
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (savedTaskEnvelope == null)
                            workbenchContext.getLayerViewPanel().getViewport()
                                    .zoomToFullExtent();
                        else
                            workbenchContext.getLayerViewPanel().getViewport()
                                    .zoom(savedTaskEnvelope);
                    } catch (Exception ex) {
                        Logger.error("Error finalizing OpenProjectWizard#loadLayers", ex);
                    }
                }
            });
        }
    }

    public static void load(Layer layer, CoordinateSystemRegistry registry,
            TaskMonitor monitor) throws Exception {
        DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
        String query = dataSourceQuery.getQuery();
        DataSource dataSource = dataSourceQuery.getDataSource();
        FeatureCollection features = executeQuery(query, dataSource, registry,
                monitor);
        layer.setFeatureCollection(features);
        layer.setFeatureCollectionModified(false);
    }

    public static void loadRasterImageLayer(WorkbenchContext context,
            RasterImageLayer ril, RasterSymbology symbology, Category category)
            throws Exception {

        RasterImageIO rasterImageIO = new RasterImageIO();
        Point point = RasterImageIO.getImageDimensions(ril.getImageFileName());
        Envelope env = RasterImageIO.getGeoReferencing(ril.getImageFileName(),
                true, point);

        Viewport viewport = context.getLayerViewPanel().getViewport();
        Resolution requestedRes = RasterImageIO
                .calcRequestedResolution(viewport);
        ImageAndMetadata imageAndMetadata = rasterImageIO.loadImage(context,
                ril.getImageFileName(), null,
                viewport.getEnvelopeInModelCoordinates(), requestedRes);

        ril = new RasterImageLayer(ril.getName(), context.getLayerManager(),
                ril.getImageFileName(), imageAndMetadata.getImage(), env);
        // [Giuseppe Aruta 2017/11/13] Since raster file is reloaded we
        // detect raster and SRS info and store them as metadata.
        try {
            ril.setSRSInfo(ProjUtils.getSRSInfoFromLayerSource(ril));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        Raster raster = ril.getRasterData(null);
        MetaInformationHandler mih = new MetaInformationHandler(ril);

        mih.addMetaInformation(I18N.get("file-name"), "");
        mih.addMetaInformation(I18N.get("resolution"), raster.getWidth()
                + " (px) x " + raster.getHeight() + " (px)");
        mih.addMetaInformation(I18N.get("real-world-width"), ril
                .getWholeImageEnvelope().getWidth());
        mih.addMetaInformation(I18N.get("real-world-height"), ril
                .getWholeImageEnvelope().getHeight());
        mih.addMetaInformation("srid", ril.getSRSInfo().getCode());
        if (ril.getSRSInfo().getCode().equals("0")) {
            mih.addMetaInformation("srid-location", "");
        } else {
            mih.addMetaInformation("srid-location", ril.getSRSInfo()
                    .getSource());
        }

        // ###################################
        context.getLayerManager().addLayerable(category.getName(), ril);

        if (symbology != null) {
            ril.setSymbology(symbology);
        }

    }

    private static FeatureCollection executeQuery(String query,
            DataSource dataSource, CoordinateSystemRegistry registry,
            TaskMonitor monitor) throws Exception {
        Connection connection = dataSource.getConnection();
        try {
            FeatureCollection features = connection
                    .executeQuery(query, monitor);
            return dataSource.installCoordinateSystem(features, registry);
        } finally {
            connection.close();
        }
    }

    private File updateResourcePath(File oldProjectFile, File newProjectFile,
            File layerFile) {
        String oldParent = oldProjectFile.getParentFile().getAbsolutePath();
        String newParent = newProjectFile.getParentFile().getAbsolutePath();
        String child = layerFile.getAbsolutePath();
        String relativePath = child.substring(oldParent.length() + 1);
        return new File(newParent, relativePath);
    }

    private boolean isLocatedBellow(File parentDir, File layerFile) {
        if (layerFile == null)
            return false;
        for (File layerParent = layerFile.getParentFile(); layerParent != null; layerParent = layerParent
                .getParentFile()) {
            if (layerParent.equals(parentDir))
                return true;
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private File getLayerFileProperty(Layer layer) {
        DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
        DataSource dataSource = dataSourceQuery.getDataSource();
        Map properties = dataSource.getProperties();
        File layerFile = null;
        if (properties.get(DataSource.FILE_KEY) != null) {
            layerFile = new File(properties.get(DataSource.FILE_KEY).toString());
        }
        return layerFile;
    }

    @SuppressWarnings("deprecation")
    private void setLayerFileProperty(Layer layer, File file) {
        DataSourceQuery dataSourceQuery = layer.getDataSourceQuery();
        DataSource dataSource = dataSourceQuery.getDataSource();
        Map<String,Object> properties = dataSource.getProperties();
        properties.put(DataSource.FILE_KEY, file.getAbsolutePath());
    }

}
