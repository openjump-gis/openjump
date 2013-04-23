package com.vividsolutions.jump.datastore.postgis;

import java.sql.*;

import org.postgresql.PGConnection;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.AdhocQuery;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.Query;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.io.FeatureInputStream;

/**
 */
public class PostgisDSConnection
    implements DataStoreConnection
{
  private PostgisDSMetadata dbMetadata;
  private Connection connection;

  public PostgisDSConnection(Connection conn) {
    connection = conn;
    dbMetadata = new PostgisDSMetadata(this);
  }

  public Connection getConnection()
  {
    return connection;
  }

  public DataStoreMetadata getMetadata()
  {
    return dbMetadata;
  }

    public FeatureInputStream execute(Query query) throws Exception {
        if (query instanceof FilterQuery) {
            try {
                return executeFilterQuery((FilterQuery) query);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        if (query instanceof AdhocQuery) {
            return executeAdhocQuery((AdhocQuery) query);
        }
        throw new IllegalArgumentException(I18N.get(this.getClass().getName()+".unsupported-query-type"));
    }

  /**
   * Executes a filter query.
   *
   * The SRID is optional for queries - it will be determined automatically
   * from the table metadata if not supplied.
   *
   * @param query the query to execute
   * @return the results of the query
 * @throws SQLException
   */
  public FeatureInputStream executeFilterQuery(FilterQuery query) throws SQLException
  {
    SpatialReferenceSystemID srid = dbMetadata.getSRID(query.getDatasetName(), query.getGeometryAttributeName());
    String[] colNames = dbMetadata.getColumnNames(query.getDatasetName());

    PostgisSQLBuilder builder = new PostgisSQLBuilder(srid, colNames);
    String queryString = builder.getSQL(query);

    PostgisFeatureInputStream ifs = new PostgisFeatureInputStream(connection, queryString);
    return ifs;
  }

  public FeatureInputStream executeAdhocQuery(AdhocQuery query) throws Exception
  {
    String queryString = query.getQuery();
    PostgisFeatureInputStream ifs = new PostgisFeatureInputStream(connection, queryString);
    if (ifs.getFeatureSchema().getGeometryIndex() < 0) {
        throw new Exception(I18N.get(this.getClass().getName()+".resultset-must-have-a-geometry-column"));
    }
    return ifs;
  }


  public void close()
      throws DataStoreException
  {
    try {
      connection.close();
    }
    catch (Exception ex) { throw new DataStoreException(ex); }
  }

  public boolean isClosed() throws DataStoreException {
    try {
        return connection.isClosed();
    } catch (SQLException e) {
        throw new DataStoreException(e);
    }
  }

}