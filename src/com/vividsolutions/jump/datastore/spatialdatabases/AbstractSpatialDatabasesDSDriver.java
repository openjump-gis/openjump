package com.vividsolutions.jump.datastore.spatialdatabases;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.jdbc.DelegatingDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;

/**
 * A driver for supplying {@link SpatialDatabasesDSConnection}s
 */
public abstract class AbstractSpatialDatabasesDSDriver implements
    DataStoreDriver {

  public static final String PARAM_Server = "Server";
  public static final String PARAM_Port = "Port";
  public static final String PARAM_Instance = "Database";
  public static final String PARAM_User = "User";
  public static final String PARAM_Password = "Password";
  // For file based databases eg. Spatialite
  public static final String PARAM_DB_File = "DB file";

  protected String driverName = null;
  protected String jdbcClass = null;
  protected Driver jdbcDriver = null;
  protected String urlPrefix = null;

  protected String[] paramNames = null;
  protected Class[] paramClasses = null;
  protected ParameterListSchema schema = null;

  public AbstractSpatialDatabasesDSDriver() {
    // Nicolas Ribot:
    // paramNames are no more static now they can be overloaded by child classes
    // @link SpatialiteDataStoreDriver for instance
    paramNames = new String[] { PARAM_Server, PARAM_Port, PARAM_Instance,
        PARAM_User, PARAM_Password };
    // Nicolas Ribot: passed protected and not final to allow
    // spatialiteDataStoreDriver to overload it
    paramClasses = new Class[] { String.class, Integer.class, String.class,
        String.class, String.class };
    // Nicolas Ribot: passed protected and not final to allow
    // spatialiteDataStoreDriver to overload it
    schema = new ParameterListSchema(paramNames, paramClasses);
  }

  public Driver getJdbcDriver() {
    if (jdbcDriver == null)
      try {
        initializeJdbcDriver();
      } catch (Exception e) {
        // eat it
      }
    return jdbcDriver;
  }

  public String getJdbcDriverVersion() {
    if (jdbcDriver == null)
      try {
        initializeJdbcDriver();
      } catch (Exception e) {
        return "";
      }
    return jdbcDriver.getMajorVersion() + "." + jdbcDriver.getMinorVersion();
  }

  public String getJdbcClass() {
    return jdbcClass;
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  public String[] getParamNames() {
    return paramNames;
  }

  public Class[] getParamClasses() {
    return paramClasses;
  }

  public ParameterListSchema getSchema() {
    return schema;
  }

  @Override
  public String getName() {
    return driverName;
  }

  @Override
  public String getVersion() {
    return getJdbcClass() + " v" + getJdbcDriverVersion();
  }

  @Override
  public ParameterListSchema getParameterListSchema() {
    return schema;
  }

  protected void initializeJdbcDriver() throws SQLException,
      InstantiationException, IllegalAccessException, ClassNotFoundException {
    // we always use the plugin classloader to find jdbc jars
    // under lib/ext/<subfolder>/
    ClassLoader cl = JUMPWorkbench.getInstance().getPlugInManager()
        .getClassLoader();
    jdbcDriver = (Driver) Class.forName(this.getJdbcClass(), true, cl)
        .newInstance();
    // DriverManager insists on jdbc drivers loaded with the default
    // classloader, so we wrap our foreign one into a simple wrapper
    // see
    // https://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
    DriverManager.registerDriver(new DelegatingDriver(jdbcDriver));
  }

  /**
   * overwrite this method if you have some fancy url scheme and
   * createJdbcConnection() will use that instead of the default implementation
   * 
   * @param params
   * @return
   */
  protected String createJdbcUrl(ParameterList params) {
    String url;
    if (params.containsParameter(PARAM_DB_File)) {
      String database = params.getParameter(PARAM_DB_File).toString();
      url = String.valueOf(new StringBuffer(getUrlPrefix()).append(database));
    } else {
      String host = params.getParameterString(PARAM_Server);
      int port = params.getParameterInt(PARAM_Port);
      String database = params.getParameterString(PARAM_Instance);
      url = String.valueOf(new StringBuffer(getUrlPrefix()).append(host)
          .append(":").append(port).append("/").append(database));
    }

    return url;
  }

  /**
   * convenience wrapper for the
   * {@link #createJdbcConnection(ParameterList, Properties)} below
   * 
   * @param params
   * @return
   * @throws Exception
   */
  protected Connection createJdbcConnection(ParameterList params)
      throws Exception {
    return createJdbcConnection(params, null);
  }

  /**
   * use this method in your implementation to create the actual JDBC connection
   */
  protected Connection createJdbcConnection(ParameterList params,
      Properties connProps) throws Exception {

    String url = createJdbcUrl(params);
    Logger.info("Target database URL : " + url);

    // only register once per driver
    if (jdbcDriver == null)
      initializeJdbcDriver();

    // some helpful debugging output
    // DriverManager.setLogWriter(new java.io.PrintWriter(System.out));
    // java.util.Enumeration<Driver> ds = DriverManager.getDrivers();
    // while (ds.hasMoreElements()) {
    // Driver d = ds.nextElement();
    // System.out.println(d.toString());
    // System.out.println(url);
    // System.out.println(d.acceptsURL(url));
    // }

    // mmichaud 2013-08-27 workaround for ticket #330
    // deactivated on 2016-02-29 as it does not seem to work anymore...
    // String savePreferIPv4Stack = System.getProperty("java.net.preferIPv4Stack");
    // String savePreferIPv6Addresses = System
    //     .getProperty("java.net.preferIPv6Addresses");
    // System.setProperty("java.net.preferIPv4Stack", "true");
    // System.setProperty("java.net.preferIPv6Addresses", "false");

    // workaround a bug in DriverManager.getConnection() when used like this:
    // Connection conn = DriverManager.getConnection(url, user, password);
    // getConnection() blindly connects to each driver and memorizes only
    // the _first_ Exception that occurs. this includes
    // "invalid database address"
    // errors, which is stupid as connect would have only to ask the driver
    // if it supports the given url scheme. funny enough getDriver() does, so
    // we add a bit of code and get the connection ourself w/ the proper driver.
    // finally, as mysql & mariadb both support 'jdbc:mysql' we iterate through
    // all registered drivers now and find exactly the instance of our
    // JDBC_CLASS
    Driver driver = null;
    java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver d = drivers.nextElement();
      if (d instanceof DelegatingDriver)
        d = ((DelegatingDriver) d).getDriver();
      // look for exactly _our_ driver
      if (d.getClass().getName().equals(getJdbcClass())) {
        driver = d;
        break;
      }
    }
    // something went wrong
    if (driver == null)
      throw new JUMPException(getJdbcClass()
          + " is not registered with driver manager.");

    String user = params.getParameterString(PARAM_User);
    String password = params.getParameterString(PARAM_Password);
    Properties info = connProps != null ? new Properties(connProps)
        : new Properties();
    if (user != null) {
      info.put("user", user);
    }
    if (password != null) {
      info.put("password", password);
    }
    Logger.info("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));
    Logger.info("java.net.preferIPv6Addresses="+System.getProperty("java.net.preferIPv6Addresses"));
    Connection conn = driver.connect(url, info);

    // deactivated on 2016-02-29 as it does not seem to work anymore...
    //if (savePreferIPv4Stack == null) {
    //  System.getProperties().remove("java.net.preferIPv4Stack");
    //} else {
    //  System.setProperty("java.net.preferIPv4Stack", savePreferIPv4Stack);
    //}
    //if (savePreferIPv6Addresses == null) {
    //  System.getProperties().remove("java.net.preferIPv6Addresses");
    //} else {
    //  System.setProperty("java.net.preferIPv6Addresses",
    //      savePreferIPv6Addresses);
    //}

    return conn;
  }

  @Override
  public abstract DataStoreConnection createConnection(ParameterList params)
      throws Exception;

  @Override
  public boolean isAdHocQuerySupported() {
    return true;
  }

}
