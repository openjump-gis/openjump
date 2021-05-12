package com.vividsolutions.jump.datastore.mariadb;

import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import javax.swing.*;

public class MysqlDataStoreDriver extends MariadbDataStoreDriver {

  public final static String JDBC_CLASS = "com.mysql.jdbc.Driver";

  public MysqlDataStoreDriver() {
    this.driverName = "MySQL";
    this.jdbcClass = JDBC_CLASS;
    this.urlPrefix = "jdbc:mysql://";
  }

  /** {@inheritDoc} */
  @Override public Icon getConnectedIcon() {
    return IconLoader.icon("dolphin_icon.png");
  }

  /** {@inheritDoc} */
  @Override public Icon getDisconnectedIcon() {
    return GUIUtil.toGrayScale((ImageIcon)getConnectedIcon());
  }

}
