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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjump.core.ui.util.ExceptionUtil;
import org.openjump.core.ui.util.TaskUtil;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.coordsys.CoordinateSystemRegistry;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
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
public class DataSourceFileLayerLoader extends AbstractFileLayerLoader {
  /** The {@link DataSource} class. */
  private Class dataSourceClass;

  /** The workbench context. */
  private WorkbenchContext workbenchContext;

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

  /**
   * Open the file specified by the URI with the map of option values.
   * 
   * @param monitor The TaskMonitor.
   * @param uri The URI to the file to load.
   * @param options The map of options.
   * @return True if the file could be loaded false otherwise.
   */
  public boolean open(TaskMonitor monitor, URI uri, Map<String, Object> options) {
    DataSource dataSource = (DataSource)LangUtil.newInstance(dataSourceClass);
    Map<String, Object> properties = toProperties(uri, options);
    dataSource.setProperties(properties);
    String filename = UriUtil.getFileName(uri);
    String layerName = filename;
    if (uri.getScheme().equals("zip"))
      layerName = UriUtil.getZipEntryName(uri)+" ("+filename+")";
    DataSourceQuery dataSourceQuery = new DataSourceQuery(dataSource, null,
      filename);
    ArrayList exceptions = new ArrayList();
    monitor.report("Loading " + layerName + "...");

    Connection connection = dataSourceQuery.getDataSource().getConnection();
    try {
      FeatureCollection dataset = dataSourceQuery.getDataSource()
        .installCoordinateSystem(
          connection.executeQuery(dataSourceQuery.getQuery(), exceptions,
            monitor),
          CoordinateSystemRegistry.instance(workbenchContext.getBlackboard()));
      if (dataset != null) {
        LayerManager layerManager = workbenchContext.getLayerManager();
        Layer layer = new Layer(layerName,
          layerManager.generateLayerFillColor(), dataset, layerManager);
        Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
        layerManager.addLayerable(category.getName(), layer);
        layer.setName(layerName);
        // make sure compressed files are loaded readonly
        if (CompressedFile.isCompressed(filename)) {
            layer.setReadonly(true);
        }
        
//        category.add(0, layer);

        layer.setDataSourceQuery(dataSourceQuery);
        layer.setFeatureCollectionModified(false);
      }
    } finally {
      connection.close();
    }
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
