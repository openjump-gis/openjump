package com.vividsolutions.jump.datastore.mariadb;

public class MysqlMariadbDataStoreDriver extends MariadbDataStoreDriver {

  public MysqlMariadbDataStoreDriver() {
    this.driverName = "MySQL (MariaDB)";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:mysql://";
  }

}
