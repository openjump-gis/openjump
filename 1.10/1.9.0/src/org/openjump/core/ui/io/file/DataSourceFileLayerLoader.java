/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.io.file;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.workbench.plugin.MacroManager;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.plugin.Recordable;
import com.vividsolutions.jump.workbench.plugin.Macro;
import org.openjump.core.ui.util.ExceptionUtil;
import org.openjump.core.ui.util.TaskUtil;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.HTMLFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * The DataSourceFileLayerLoader is an implementation of {@link FileLayerLoader}
 * that wraps an existing file based {@link DataSource} class.
 * 
 * @author Paul Austin
 */
public class DataSourceFileLayerLoader extends AbstractFileLayerLoader implements Recordable {

    private final static String DATASOURCE_CLASSNAME = "DataSourceClassName";
    private final static String URI                  = "Uri";

    // [mmichaud 2014-10-01] add some methods for macro plugin recorder
    private Map<String,Object> parameters;

    public void addParameter(String name, Object value) {
        if (parameters == null) parameters = new HashMap<String, Object>();
        parameters.put(name, value);
    }

    public Object getParameter(String name) {
        if (parameters == null) return null;
        return parameters.get(name);
    }

    public Boolean getBooleanParam(String name) {
        if (parameters == null) return null;
        return (Boolean)parameters.get(name);
    }

    public Integer getIntegerParam(String name) {
        if (parameters == null) return null;
        return (Integer)parameters.get(name);
    }

    public Double getDoubleParam(String name) {
        if (parameters == null) return null;
        return (Double)parameters.get(name);
    }

    public String getStringParam(String name) {
        if (parameters == null) return null;
        return (String)parameters.get(name);
    }

    public void setParameters(Map<String,Object> map) {
        parameters = map;
    }

    public Map<String,Object> getParameters() {
        return parameters;
    }
    // [mmichaud 2014-10-01] end
    
    // this probably clashes with the above, but DataSourceFileLayerLoader is reused for opening files
    // so any "old" parameters lingering during open have a probably detrimental effect on the next open
    public void resetParameters() {
        this.parameters = new HashMap<String, Object>();
    }

  /** The {@link DataSource} class. */
  private Class dataSourceClass;

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

  /** No parameter constuctor for xml persitence.*/
  public DataSourceFileLayerLoader() {}
  public void setContext(WorkbenchContext context) {this.workbenchContext = context;}

  /**
   * Construct a new DataSourceFileLayerLoader.
   * 
   * @param workbenchContext The workbench context.
   * @param dataSourceClass The {@link DataSource} class.
   * @param description The file format name.
   * @param extensions The list of supported extensions.
   */
  public DataSourceFileLayerLoader(WorkbenchContext workbenchContext,
    Class dataSourceClass, String description, List<String> extensions) {
    super(description, extensions);
    this.workbenchContext = workbenchContext;
    this.dataSourceClass = dataSourceClass;
  }

  public Object process(TaskMonitor monitor) throws ClassNotFoundException, URISyntaxException {
      PlugInManager plugInManager = workbenchContext.getWorkbench().getPlugInManager();
      ClassLoader pluginClassLoader = plugInManager.getClassLoader();
      Class datasourceClass = pluginClassLoader.loadClass(getStringParam(DATASOURCE_CLASSNAME));
      DataSource dataSource = (DataSource)LangUtil.newInstance(datasourceClass);
      //Map<String, Object> properties = toProperties(uri, options);
      Map<String, Object> properties = getParameters();
      dataSource.setProperties(properties);
      URI uri = new URI(getStringParam(URI));
      String filename = UriUtil.getFileName(uri);
      String layerName = CompressedFile.createLayerName(uri);
      DataSourceQuery dataSourceQuery = new DataSourceQuery(dataSource, null,
              layerName);
      ArrayList exceptions = new ArrayList();
      monitor.report("Loading " + layerName + "...");

      Connection connection = dataSourceQuery.getDataSource().getConnection();
      try {
          LayerManager layerManager = workbenchContext.getLayerManager();
          layerName = layerManager.uniqueLayerName(layerName);
          FeatureCollection dataset = dataSourceQuery.getDataSource()
                  .installCoordinateSystem(
                          connection.executeQuery(dataSourceQuery.getQuery(), exceptions,
                                  monitor),
                          CoordinateSystemRegistry.instance(workbenchContext.getBlackboard()));
          boolean layer_changed = false;
          if (dataset != null) {
              Layer layer = null;
              for (Feature f : (List<Feature>)dataset.getFeatures()) {

                  // restore referenced image feature, if one
                  Feature img_f = null;
                  FeatureCollection img_fs = new FeatureDataset(ImageryLayerDataset.SCHEMA);
                  if ( ImageryLayerDataset.isImageFeature(f)) {
                      // create an image layer
                      if (layer == null) {
                          layerManager.setFiringEvents(false);
                          layer = ReferencedImageFactoryFileLayerLoader.createLayer(
                                  layerManager, uri);
                          layer.setFeatureCollection(img_fs);
                          layerManager.setFiringEvents(true);
                      }
                      ReferencedImageStyle irs = (ReferencedImageStyle) layer
                              .getStyle(ReferencedImageStyle.class);
                      ImageryLayerDataset ilds = irs.getImageryLayerDataset();

                      // old datasets are converted to new ones, so they must be saved again
                      // signal by setting layer_changed to true
                      layer_changed = layer_changed || ImageryLayerDataset.isOldImageFeature(f);
                      img_f = ReferencedImageFactoryFileLayerLoader.createImageFeature(f, ilds);
                      img_fs.add(img_f);
                  }

              }

              // create a layer to fill in new features
              if (layer == null)
                  layer = new Layer(layerName, layerManager.generateLayerFillColor(),
                          dataset, layerManager);
              Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
              layerManager.addLayerable(category.getName(), layer);
              layer.setName(layerName);
              // make sure compressed files are loaded readonly
              // [ede] disabled via request (jukka,03.2013)
//        if (CompressedFile.isCompressed(UriUtil.getFileName(uri))) {
//            layer.setReadonly(true);
//        }

              layer.setDataSourceQuery(dataSourceQuery);
              layer.setFeatureCollectionModified(layer_changed);
          }
      } finally {
          connection.close();
      }
      // handle exceptions that might have occured
      if (!exceptions.isEmpty()) {
          WorkbenchFrame workbenchFrame = workbenchContext.getWorkbench()
                  .getFrame();
          HTMLFrame outputFrame = workbenchFrame.getOutputFrame();
          outputFrame.createNewDocument();
          ExceptionUtil.reportExceptions(exceptions, dataSourceQuery,
                  workbenchFrame, outputFrame);
          workbenchFrame.warnUser(I18N.get("datasource.LoadDatasetPlugIn.problems-were-encountered"));
          return false;
      }

      return true;
  }

  /**
   * Open the file specified by the URI with the map of option values.
   * 
   * @param monitor The TaskMonitor.
   * @param uri The URI to the file to load.
   * @param options The map of options.
   * @return True if the file could be loaded false otherwise.
   */
  public boolean open(TaskMonitor monitor, URI uri, Map<String, Object> options) {
      //reset old parameters away
      resetParameters();
      addParameter(DATASOURCE_CLASSNAME, dataSourceClass.getName());
      addParameter(URI, uri.toString());
      Map<String,Object> properties = toProperties(uri, options);
      for (String key : properties.keySet()) {
          addParameter(key, properties.get(key));
      }
      try {
          //return (Boolean)process(monitor);
          boolean ret = (Boolean)process(monitor);
          if (workbenchContext.getBlackboard().get(MacroManager.MACRO_STARTED, false)) {
              DataSourceFileLayerLoader clone = new DataSourceFileLayerLoader();
              clone.setParameters(this.getParameters());
              ((Macro)workbenchContext.getBlackboard().get("Macro")).addProcess(clone);
          }
          return ret;
      } catch(Exception e) {
          e.printStackTrace();
          return false;
      }
      /*
    DataSource dataSource = (DataSource)LangUtil.newInstance(dataSourceClass);
    Map<String, Object> properties = toProperties(uri, options);
    dataSource.setProperties(properties);
    String filename = UriUtil.getFileName(uri);
    String layerName = CompressedFile.createLayerName(uri);
    DataSourceQuery dataSourceQuery = new DataSourceQuery(dataSource, null,
      layerName);
    ArrayList exceptions = new ArrayList();
    monitor.report("Loading " + layerName + "...");

    Connection connection = dataSourceQuery.getDataSource().getConnection();
    try {
      LayerManager layerManager = workbenchContext.getLayerManager();
      layerName = layerManager.uniqueLayerName(layerName);
      FeatureCollection dataset = dataSourceQuery.getDataSource()
        .installCoordinateSystem(
          connection.executeQuery(dataSourceQuery.getQuery(), exceptions,
            monitor),
          CoordinateSystemRegistry.instance(workbenchContext.getBlackboard()));
      boolean layer_changed = false;
      if (dataset != null) {
        Layer layer = null;
        for (Feature f : (List<Feature>)dataset.getFeatures()) {

          // restore referenced image feature, if one
          Feature img_f = null;
          FeatureCollection img_fs = new FeatureDataset(ImageryLayerDataset.SCHEMA);
          if ( ImageryLayerDataset.isImageFeature(f)) {
            // create an image layer
            if (layer == null) {
              layerManager.setFiringEvents(false);
              layer = ReferencedImageFactoryFileLayerLoader.createLayer(
                  layerManager, uri);
              layer.setFeatureCollection(img_fs);
              layerManager.setFiringEvents(true);
            }
            ReferencedImageStyle irs = (ReferencedImageStyle) layer
                .getStyle(ReferencedImageStyle.class);
            ImageryLayerDataset ilds = irs.getImageryLayerDataset();
            
            // old datasets are converted to new ones, so they must be saved again
            // signal by setting layer_changed to true
            layer_changed = layer_changed || ImageryLayerDataset.isOldImageFeature(f);
            img_f = ReferencedImageFactoryFileLayerLoader.createImageFeature(f, ilds);
            img_fs.add(img_f);
          }
          
        }
        
        // create a layer to fill in new features
        if (layer == null)
          layer = new Layer(layerName, layerManager.generateLayerFillColor(),
              dataset, layerManager);
        Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
        layerManager.addLayerable(category.getName(), layer);
        layer.setName(layerName);
        // make sure compressed files are loaded readonly 
        // [ede] disabled via request (jukka,03.2013)
//        if (CompressedFile.isCompressed(UriUtil.getFileName(uri))) {
//            layer.setReadonly(true);
//        }

        layer.setDataSourceQuery(dataSourceQuery);
        layer.setFeatureCollectionModified(layer_changed);
      }
    } finally {
      connection.close();
    }
    // handle exceptions that might have occured
    if (!exceptions.isEmpty()) {
      WorkbenchFrame workbenchFrame = workbenchContext.getWorkbench()
        .getFrame();
      HTMLFrame outputFrame = workbenchFrame.getOutputFrame();
      outputFrame.createNewDocument();
      ExceptionUtil.reportExceptions(exceptions, dataSourceQuery,
        workbenchFrame, outputFrame);
      workbenchFrame.warnUser(I18N.get("datasource.LoadDatasetPlugIn.problems-were-encountered"));
      return false;
    }

    return true;
    */
  }

  /**
   * Convert the URI and map of options for the data source. If the URI is a ZIP
   * uri the File option will be set to the ZIP file name and the CompressedFile
   * set to the entry in the ZIP file.
   * 
   * @param uri The URI to the file.
   * @param options The selected options.
   * @return The options.
   */
  protected Map<String, Object> toProperties(URI uri,
    Map<String, Object> options) {
    Map<String, Object> properties = new HashMap<String, Object>();
    File file;
    // zip:// applies to all archives like *.zip,*.tgz ...
    if (uri.getScheme().equals("zip")) {
      file = UriUtil.getZipFile(uri);
      String compressedFile = UriUtil.getZipEntryName(uri);
      properties.put("CompressedFile", compressedFile);
    } else {
      file = new File(uri);
    }
    String filePath = file.getAbsolutePath();
    properties.put(DataSource.FILE_KEY, filePath);
    properties.putAll(options);
    return properties;
  }
}
