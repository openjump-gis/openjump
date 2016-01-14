package com.vividsolutions.jump.datastore.jdbc;

import java.sql.*;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Utilities for JDBC.
 *
 * @author Martin Davis
 * @version 1.0
 */
public class JDBCUtil
{
  @Deprecated
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
  
  /**
   * A method using Apache Commons DbUtils to perform preparedStatement queries.
   * A replacement to deprecated @link execute method that takes a preformatted query
   * @param <T> the target type the input ResultSet will be converted to.
   * @param conn
   * @param bQuery the boundQuery to execute
   * @param rsh
   * @param params
   */
  public static void query(Connection conn, BoundQuery bQuery, ResultSetHandler rsh,
      Object... params) {
    QueryRunner run = new QueryRunner();
    try {
      run.query(conn, bQuery.getQuery(), rsh, bQuery.getParameters());
    } catch (SQLException sqle) {
      throw new RuntimeException("Invalid SQL query: "+ bQuery.getQuery() +
          ". Number of bound parameters: " + bQuery.getParameters().length, sqle);
    }
  }
}