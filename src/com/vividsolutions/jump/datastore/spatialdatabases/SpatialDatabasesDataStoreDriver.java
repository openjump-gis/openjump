/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.spatialdatabases;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * A driver for supplying {@link SpatialDatabaseDSConnection}s
 */
public class SpatialDatabasesDataStoreDriver
    implements DataStoreDriver {

  public static final String PARAM_Server = "Server";
  public static final String PARAM_Port = "Port";
  public static final String PARAM_Instance = "Database";
  public static final String PARAM_User = "User";
  public static final String PARAM_Password = "Password";

  protected String driverName = null;
  protected String jdbcClass = null;
  protected String urlPrefix = null;

  private static final String[] paramNames = new String[]{
      PARAM_Server,
      PARAM_Port,
      PARAM_Instance,
      PARAM_User,
      PARAM_Password
    };
  private static final Class[] paramClasses = new Class[]{
      String.class,
      Integer.class,
      String.class,
      String.class,
      String.class
    };
  private final ParameterListSchema schema = new ParameterListSchema(paramNames, paramClasses);

  public SpatialDatabasesDataStoreDriver() {
//    this.driverName = "";
//    this.jdbcClass = "";
//    this.urlPrefix = "";
  }

  public String getDriverName() {
    return driverName;
  }

  public String getJdbcClass() {
    return jdbcClass;
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }
  
  

  @Override
  public String getName() {
    return driverName;
  }

  @Override
  public ParameterListSchema getParameterListSchema() {
    return schema;
  }

  @Override
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception {
    String host = params.getParameterString(PARAM_Server);
    int port = params.getParameterInt(PARAM_Port);
    String database = params.getParameterString(PARAM_Instance);
    String user = params.getParameterString(PARAM_User);
    String password = params.getParameterString(PARAM_Password);

    String url
        = String.valueOf(new StringBuffer(urlPrefix).append(host).append(":").append(port).append("/").append(database));

    Driver driver = (Driver) Class.forName(this.getJdbcClass()).newInstance();
    DriverManager.registerDriver(driver);

    // mmichaud 2013-08-27 workaround for ticket #330
    String savePreferIPv4Stack = System.getProperty("java.net.preferIPv4Stack");
    String savePreferIPv6Addresses = System.getProperty("java.net.preferIPv6Addresses");
    System.setProperty("java.net.preferIPv4Stack", "true");
    System.setProperty("java.net.preferIPv6Addresses", "false");

    Connection conn = DriverManager.getConnection(url, user, password);

    if (savePreferIPv4Stack == null) {
      System.getProperties().remove("java.net.preferIPv4Stack");
    } else {
      System.setProperty("java.net.preferIPv4Stack", savePreferIPv4Stack);
    }
    if (savePreferIPv6Addresses == null) {
      System.getProperties().remove("java.net.preferIPv6Addresses");
    } else {
      System.setProperty("java.net.preferIPv6Addresses", savePreferIPv6Addresses);
    }
    return new SpatialDatabasesDSConnection(conn);
  }

  @Override
  public boolean isAdHocQuerySupported() {
    return true;
  }

}
