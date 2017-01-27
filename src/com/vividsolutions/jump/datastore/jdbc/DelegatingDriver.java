package com.vividsolutions.jump.datastore.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * a jdbc driver wrapper to allow loading the driver with custon classloader
 * from an arbitrary location during runtime. 
 * DatabaseManager.registerDriver() only registers drivers loaded with the
 * system classloader so we trick it into accepting our driver by wrapping it
 * into this one.
 *
 * @see
 *   <a href="https://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location">
 *   How to use a jdbc driver from an arbitrary location</a>
 */
public class DelegatingDriver implements Driver {
  private final Driver driver;

  public DelegatingDriver(Driver driver) {
    if (driver == null) {
      throw new IllegalArgumentException("Driver must not be null.");
    }
    this.driver = driver;
  }

  public Connection connect(String url, Properties info) throws SQLException {
    return driver.connect(url, info);
  }

  public boolean acceptsURL(String url) throws SQLException {
    return driver.acceptsURL(url);
  }

  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
      throws SQLException {
    return driver.getPropertyInfo(url, info);
  }

  public int getMajorVersion() {
    return driver.getMajorVersion();
  }

  public int getMinorVersion() {
    return driver.getMinorVersion();
  }

  public boolean jdbcCompliant() {
    return driver.jdbcCompliant();
  }

  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // we compile w/ java7+ anyway and whilst not _compile_ compatible w/ java6,
    // this is runtime compatible as java6 does not call what it doesn't know
    return driver.getParentLogger();
  }

  /**
   * get wrappee
   * @return
   */
  public Driver getDriver(){
    return driver;
  }

  @Override
  public String toString() {
    return super.toString() + "["+driver.toString()+"]";
  }

}