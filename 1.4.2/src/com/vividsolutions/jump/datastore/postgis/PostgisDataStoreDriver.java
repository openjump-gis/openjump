package com.vividsolutions.jump.datastore.postgis;

import java.sql.*;

import com.vividsolutions.jump.datastore.*;

import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;

import org.postgresql.PGConnection;

/**
 * A driver for supplying {@link PostgisDSConnection}s
 */
public class PostgisDataStoreDriver
    implements DataStoreDriver
{
  public static final String DRIVER_NAME = "PostGIS";
  public static final String JDBC_CLASS = "org.postgresql.Driver";
  public static final String URL_PREFIX = "jdbc:postgresql://";

  public static final String PARAM_Server = "Server";
  public static final String PARAM_Port = "Port";
  public static final String PARAM_Instance = "Database";
  public static final String PARAM_User = "User";
  public static final String PARAM_Password = "Password";

  private static final String[] paramNames = new String[] {
    PARAM_Server,
    PARAM_Port,
    PARAM_Instance,
    PARAM_User,
    PARAM_Password
    };
  private static final Class[] paramClasses = new Class[]
  {
    String.class,
    Integer.class,
    String.class,
    String.class,
    String.class
    };
  private final ParameterListSchema schema = new ParameterListSchema(paramNames, paramClasses);;

  public PostgisDataStoreDriver() {
  }

  public String getName()
  {
    return DRIVER_NAME;
  }
  public ParameterListSchema getParameterListSchema()
  {
    return schema;
  }
  public DataStoreConnection createConnection(ParameterList params)
      throws Exception
  {
    String host = params.getParameterString(PARAM_Server);
    int port = params.getParameterInt(PARAM_Port);
    String database = params.getParameterString(PARAM_Instance);
    String user = params.getParameterString(PARAM_User);
    String password = params.getParameterString(PARAM_Password);

    String url
        = String.valueOf(new StringBuffer(URL_PREFIX).append
        (host).append
        (":").append
        (port).append
        ("/").append(database));

    Driver driver = (Driver) Class.forName(JDBC_CLASS).newInstance();
    DriverManager.registerDriver(driver);

    Connection conn = DriverManager.getConnection(url, user, password);
    return new PostgisDSConnection(conn);
  }
  public boolean isAdHocQuerySupported() {
      return true;
  }

}