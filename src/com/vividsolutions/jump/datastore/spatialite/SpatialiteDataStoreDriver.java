package com.vividsolutions.jump.datastore.spatialite;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.parameter.ParameterList;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import java.io.File;
import org.sqlite.SQLiteConfig;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class SpatialiteDataStoreDriver
    extends SpatialDatabasesDataStoreDriver {
    
    public final static String JDBC_CLASS = "org.sqlite.JDBC";
    
    public SpatialiteDataStoreDriver() {
        this.driverName = "Spatialite";
        this.jdbcClass = "org.sqlite.JDBC";
        //TODO: prompt for filename
        this.urlPrefix = "jdbc:sqlite:";
        
        // Panel parameters: adds a file chooser for db name
        paramNames = new String[]{PARAM_DB_File};
        paramClasses = new Class[]{File.class};
        schema = new ParameterListSchema(paramNames, paramClasses);
    }
    
    /**
     * returns a spatialite JDBC connexion with spatialite extension loaded if possible
     * if not, a property will tell so
     * 
     * @param params
     * @return
     * @throws Exception 
     */
  @Override
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception {
    // PARAM_DB_File is a fileChooser: the default mechanism used will store a
    // DefaultAwtShell on OSX. Do not cast to this internal type but gets its toString() method
    // returning the choosen filename.
    String database = params.getParameter(PARAM_DB_File).toString();
    
    // First checks if the file exists: cannot manage newly created Spatialite files yet:
    // File must exists
    File sqliteFile = new File(database);
    if (!sqliteFile.exists() || !sqliteFile.canRead()) {
      // TODO: I18N
      throw new Exception("Spatialite file: " + database + " does not exist. cannot create connection");
    }
    
    
    SQLiteConfig config = new SQLiteConfig();
    // mandatory to load spatialite extension
    config.enableLoadExtension(true);

    String url
        = String.valueOf(new StringBuffer(urlPrefix).append(database));

    Driver driver = (Driver) Class.forName(this.getJdbcClass()).newInstance();
    DriverManager.registerDriver(driver);

    Connection conn = DriverManager.getConnection(url, config.toProperties());
    
    return new SpatialiteDSConnection(conn);
  }
}
