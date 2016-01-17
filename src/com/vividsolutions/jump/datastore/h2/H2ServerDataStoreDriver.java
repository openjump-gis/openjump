package com.vividsolutions.jump.datastore.h2;

import java.sql.Connection;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDSDriver;
import com.vividsolutions.jump.parameter.ParameterList;

/**
 * A driver for supplying
 * {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}
 * s
 */
public class H2ServerDataStoreDriver extends
    AbstractSpatialDatabasesDSDriver {

  public final static String JDBC_CLASS = "org.h2.Driver";

  public H2ServerDataStoreDriver() {
    this.driverName = "H2GIS Server";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:h2:tcp://";
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
}