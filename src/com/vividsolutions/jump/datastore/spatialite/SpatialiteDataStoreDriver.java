package com.vividsolutions.jump.datastore.spatialite;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.jdbc.DelegatingDriver;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

//import org.sqlite.SQLiteConfig;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class SpatialiteDataStoreDriver extends AbstractSpatialDatabasesDataStoreDriver {

  public final static String JDBC_CLASS = "org.sqlite.JDBC";
  private static boolean initialized = false;

  public SpatialiteDataStoreDriver() {
    this.driverName = "Spatialite";
    this.jdbcClass = "org.sqlite.JDBC";
    // TODO: prompt for filename
    this.urlPrefix = "jdbc:sqlite:";

    // Panel parameters: adds a file chooser for db name
    paramNames = new String[] { PARAM_DB_File };
    paramClasses = new Class[] { File.class };
    schema = new ParameterListSchema(paramNames, paramClasses);
  }

  /**
   * returns a spatialite JDBC connection with spatialite extension loaded if
   * possible if not, a property will tell so
   * 
   * @param params
   * @return
   * @throws Exception
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
      // TODO: I18N
      throw new Exception("Spatialite file: " + database
          + " does not exist. cannot create connection");
    }

    String url = String.valueOf(new StringBuffer(urlPrefix).append(database));

    ClassLoader cl = JUMPWorkbench.getInstance().getPlugInManager()
        .getClassLoader();

    // we register only once
    if (!initialized) {
      Driver driver = (Driver) cl.loadClass(this.getJdbcClass()).newInstance();
      // DriverManager insists on jdbc drivers loaded with the default
      // classloader, so we wrap our foreign one into a simple wrapper
      // see https://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
      DriverManager.registerDriver(new DelegatingDriver(driver));
      initialized = true;
//      Enumeration<Driver> ds = DriverManager.getDrivers();
//      while (ds.hasMoreElements()) {
//        System.out.println(ds.nextElement());
//      }
    }

    // mandatory to enable loading extensions
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
    Connection conn = DriverManager.getConnection(url, props);

    return new SpatialiteDSConnection(conn);
  }
  
}
