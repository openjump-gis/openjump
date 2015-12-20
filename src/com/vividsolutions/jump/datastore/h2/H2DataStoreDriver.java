package com.vividsolutions.jump.datastore.h2;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;

import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.jdbc.DelegatingDriver;
import com.vividsolutions.jump.datastore.spatialdatabases.AbstractSpatialDatabasesDataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

/**
 * A driver for supplying
 * {@link com.vividsolutions.jump.datastore.spatialdatabases.SpatialDatabasesDSConnection}
 * s
 */
public class H2DataStoreDriver extends AbstractSpatialDatabasesDataStoreDriver {

  public final static String JDBC_CLASS = "org.h2.Driver";

  public H2DataStoreDriver() {
    this.driverName = "H2GIS";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:h2:";

    paramNames = new String[]{
            PARAM_Instance,
            PARAM_User,
            PARAM_Password
    };
    paramClasses = new Class[]{
            String.class,
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
    Connection conn = createJdbcConnection(params);
    return new H2DSConnection(conn);
  }

  protected Connection createJdbcConnection(ParameterList params) throws Exception {
    String database = params.getParameterString(PARAM_Instance);
    String user = params.getParameterString(PARAM_User);
    String password = params.getParameterString(PARAM_Password);

    String url = String.valueOf(new StringBuffer(urlPrefix).append(database));

    // only register once per driver
    if (!this.registered) {
      // we always use the plugin classloader to find jdbc jars
      // under lib/ext/<subfolder>/
      ClassLoader cl = JUMPWorkbench.getInstance().getPlugInManager()
              .getClassLoader();
      Driver driver = (Driver) Class.forName(this.getJdbcClass(), true, cl)
              .newInstance();
      // DriverManager insists on jdbc drivers loaded with the default
      // classloader, so we wrap our foreign one into a simple wrapper
      // see
      // https://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
      DriverManager.registerDriver(new DelegatingDriver(driver));
      this.registered = true;
    }

    // some helpful debugging output
    // DriverManager.setLogWriter(new PrintWriter(System.out));
    // Enumeration<Driver> ds = DriverManager.getDrivers();
    // while (ds.hasMoreElements()) {
    // Driver d = ds.nextElement();
    // System.out.println(d);
    // System.out.println(url);
    // System.out.println(d.acceptsURL(url));
    // }

    // workaround a bug in DriverManager.getConnection() when used like this:
    //   Connection conn = DriverManager.getConnection(url, user, password);
    // getConnection() blindly connects to each driver and memorizes only
    // the _first_ Exception that occurs. this includes "invalid database address"
    // errors, which is stupid as connect would have only to ask the driver
    // if it supports the given url scheme. funny enough getDriver() does, so
    // we add a bit of code and get the connection ourself w/ the proper driver.
    Driver d = DriverManager.getDriver(url);
    java.util.Properties info = new java.util.Properties();
    if (user != null) {
      info.put("user", user);
    }
    if (password != null) {
      info.put("password", password);
    }
    Connection conn = d.connect(url, info);

    return conn;
  }
}
