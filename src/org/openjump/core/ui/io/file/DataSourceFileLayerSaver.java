package org.openjump.core.ui.io.file;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjump.util.UriUtil;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class DataSourceFileLayerSaver extends AbstractFileLayerSaver {

  private Class dataSourceClass;
  private PlugInContext context;

  public DataSourceFileLayerSaver(WorkbenchContext workbenchContext,
      Class dataSourceClass, String description, List<String> extensions) {
    super(description, extensions);
    this.context = workbenchContext.createPlugInContext();
    this.dataSourceClass = dataSourceClass;
  }

  public DataSourceFileLayerSaver(WorkbenchContext workbenchContext,
      FileDataSourceQueryChooser fdsqc) {
    this(workbenchContext, fdsqc.getDataSourceClass(), fdsqc.getDescription(),
        Arrays.asList(fdsqc.getExtensions()));
  }

  @Override
  public boolean write(TaskMonitor monitor, URI uri, Map<String, Object> options)
      throws Exception {
    DataSource dataSource = (DataSource) LangUtil.newInstance(dataSourceClass);
    Layer layer = context.getSelectedLayer(0);
    FeatureCollection fc = layer.getFeatureCollectionWrapper();

    monitor.allowCancellationRequests();
    monitor.report("saving " + UriUtil.getFileName(uri));

    if (options == null) {
      options = new HashMap<>();
    }
    options.put(DataSource.URI_KEY, uri);
    options.put(DataSource.FILE_KEY, UriUtil.getFilePath(uri));
    dataSource.setProperties(options);

    Connection connection = dataSource.getConnection();
    try {
      connection.executeUpdate("", fc, monitor);
    } finally {
      connection.close();
    }

    // update the layers dsq
    DataSourceQuery dataSourceQuery = new DataSourceQuery(dataSource, "",
        UriUtil.getFileNameWithoutExtension(uri));
    layer.setDataSourceQuery(dataSourceQuery).setFeatureCollectionModified(
        false);

    return true;
  }

}
