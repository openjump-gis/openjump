package com.vividsolutions.jump.datastore.jdbc;

import java.sql.*;

/**
 * Utilities for JDBC.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class JDBCUtil
{
  public static void execute(Connection conn, String sql, ResultSetBlock block) {
      try {
          Statement statement = conn.createStatement();
          try {
              ResultSet resultSet = statement.executeQuery(sql);
              try {
                  block.yield(resultSet);
              } finally {
                  resultSet.close();
              }
          } finally {
              statement.close();
          }
      } catch (Exception e) {
          throw new RuntimeException("Invalid query: "+sql, e);
      }
  }
}