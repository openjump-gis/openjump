package com.vividsolutions.jump.datastore.mariadb;

public class MysqlDataStoreDriver extends MariadbDataStoreDriver {

  public final static String JDBC_CLASS = "com.mysql.jdbc.Driver";

  public MysqlDataStoreDriver() {
    this.driverName = "MySQL";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:mysql://";
  }

}
