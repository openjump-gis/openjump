package com.vividsolutions.jump.datastore.spatialite;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;

//import org.sqlite.SQLiteConfig;

/**
 * A driver for supplying {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}s
 */
public class SpatialiteDataStoreDriver extends AbstractSpatialDatabasesDSDriver {

  public final static String JDBC_CLASS = "org.sqlite.JDBC";
  private String spatialiteVersion = "not connected";

  public SpatialiteDataStoreDriver() {
    this.driverName = "Spatialite";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:sqlite:";

    // Panel parameters: adds a file chooser for db name
    paramNames = new String[] { PARAM_DB_File };
    paramClasses = new Class[] { File.class };
    schema = new ParameterListSchema(paramNames, paramClasses);
  }

  /**
   * additionally report spatialite version if connection exists
   */
  public String getVersion() {
    return super.getVersion() + " (Spatialite " + spatialiteVersion + ")";
  }

  /**
   * returns a spatialite JDBC connection with spatialite extension loaded if
   * possible if not, a property will tell so
   * 
   * @param params list of Connection parameters
   * @return a DataStoreConnection
   * @throws Exception if an Exception occurs
   */
  @Override
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception {
    // PARAM_DB_File is a fileChooser: the default mechanism used will store a
    // DefaultAwtShell on OSX. Do not cast to this internal type but gets its
    // toString() method
    // returning the choosen filename.
    String database = params.getParameter(PARAM_DB_File).toString();

    // First checks if the file exists: cannot manage newly created Spatialite
    // files yet:
    // File must exists
    File sqliteFile = new File(database);
    if (!sqliteFile.exists() || !sqliteFile.canRead()) {
      throw new Exception(I18N.getInstance().get(
              "com.vividsolutions.jump.datastore.spatialite.SpatialiteDataStoreDriver.file-does-not-exist",
              database));
    }

    ClassLoader cl = JUMPWorkbench.getInstance().getPlugInManager()
        .getClassLoader();

    // enable loading extensions is mandatory or else mod_spatialite will fail to load
    Class configClazz = cl.loadClass("org.sqlite.SQLiteConfig");
    Method enableMethod = configClazz.getMethod("enableLoadExtension",
        new Class[] { boolean.class });

    Object config = configClazz.newInstance();
    enableMethod.invoke(config, true);

    // this is the code above w/o reflection, KEEP FOR REFERENCE!!!
    // mandatory to load spatialite extension
    // SQLiteConfig config = new SQLiteConfig();
    // config.enableLoadExtension(true);

    Method getPropsMethod = configClazz.getMethod("toProperties");
    Properties props = (Properties) getPropsMethod.invoke(config);

    Connection conn = super.createJdbcConnection(params, props);
    SpatialiteDSConnection dsConn = new SpatialiteDSConnection(conn);

    // memorize spatialite version for displaying later
    this.spatialiteVersion = ((SpatialiteDSMetadata) dsConn.getMetadata())
        .getSpatialiteVersion();
    // report if spatialite could not be loaded
    if (this.spatialiteVersion.isEmpty())
      this.spatialiteVersion = "unavailable";

    return dsConn;
  }

  /** {@inheritDoc} */
  @Override public Icon getConnectedIcon() {
    return IconLoader.icon("ok_spatialite.png");
  }

  /** {@inheritDoc} */
  @Override public Icon getDisconnectedIcon() {
    return GUIUtil.toGrayScale((ImageIcon)getConnectedIcon());
  }

}
