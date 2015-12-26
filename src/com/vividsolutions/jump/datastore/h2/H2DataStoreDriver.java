package com.vividsolutions.jump.datastore.h2;

import java.io.File;
import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;

/**
 * A driver for supplying
 * {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}
 * s
 */
public class H2DataStoreDriver extends AbstractSpatialDatabasesDSDriver {

  public final static String JDBC_CLASS = "org.h2.Driver";

  public H2DataStoreDriver() {
    this.driverName = "H2GIS";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:h2:";

    paramNames = new String[]{
            PARAM_DB_File,
            PARAM_User,
            PARAM_Password
    };
    paramClasses = new Class[]{
            File.class,
            String.class,
            String.class
    };
    schema = new ParameterListSchema(paramNames, paramClasses);
  }

  /**
   * returns the right type of DataStoreConnection
   * 
   * @param params
   * @return
   * @throws Exception
   */
  @Override
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception {
    Connection conn = super.createJdbcConnection(params);
    return new H2DSConnection(conn);
  }

  @Override
  protected String createJdbcUrl(ParameterList params) {
    // PARAM_DB_File is a fileChooser: the default mechanism used will store a
    // DefaultAwtShell on OSX. Do not cast to this internal type but gets its
    // toString() method
    // returning the choosen filename.
    String database = params.getParameter(PARAM_DB_File).toString();
    // remove possibly existing file extension
    database = database.replaceAll("(?i)\\.\\w+\\.db$", "");
    // only open already existing database files, _don't_ create
    return getUrlPrefix() + database + ";IFEXISTS=TRUE;";
  }

}
