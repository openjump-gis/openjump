package com.vividsolutions.jump.datastore.postgis;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.PrimaryKeyColumn;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class PostgisDSMetadata implements DataStoreMetadata {

  private final WKBReader reader = new WKBReader();

  private PostgisDSConnection conn;

  private Map sridMap = new HashMap();

  public PostgisDSMetadata( PostgisDSConnection conn ) {
    this.conn = conn;
  }

  public String[] getDatasetNames() {
    final List datasetNames = new ArrayList();
    // Spatial tables only.
    JDBCUtil.execute(
        conn.getConnection(),
        "SELECT DISTINCT f_table_schema, f_table_name FROM geometry_columns",
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          String schema = resultSet.getString( 1 );
          String table = resultSet.getString( 2 );
          if ( !schema.equalsIgnoreCase( "public" ) ) {
            table = schema + "." + table;
          }
          datasetNames.add( table );
        }
      }
    } );
    return ( String[] ) datasetNames.toArray(new String[datasetNames.size()]);
  }


  public Envelope getExtents( String datasetName, String attributeName ) {
    
    final Envelope[] e = new Envelope[]{null};
    
    String schema;
    String table;
    if (datasetName.indexOf('.') != -1) {
        String[] parts = datasetName.split("\\.", 2);
        schema = parts[0];
        table = parts[1]; 
    }
    else {
        schema = "public";
        table = datasetName;
    }
    // There are two methods to compute the extent,
    // ST_EstimatedExtent is fast but not precise (last entered data may be excluded
    // until the next ANALYZE)
    // ST_Extent is slow but precise (scan over the whole table)
    String sql1 = "SELECT ST_AsBinary(ST_Estimated_Extent( '" + schema + "', '" + table +"', '" + attributeName + "' ));";
    String sql2 = "SELECT ST_AsBinary(ST_Envelope(ST_Extent(\"" + attributeName + "\"))) FROM \"" + schema + "\".\"" + table + "\";";

    final ResultSetBlock resultSetBlock = new ResultSetBlock() {
        public void yield( ResultSet resultSet ) throws Exception {
            if ( resultSet.next() ) {
                byte[] bytes = ( byte[] ) resultSet.getObject( 1 );
                if ( bytes != null ) {
                    Geometry geom = reader.read( bytes );
                    if ( geom != null ) {
                        e[0] = geom.getEnvelopeInternal();
                    }
                }
            }
        }
    };
    try {
        JDBCUtil.execute(conn.getConnection(), (sql1), resultSetBlock);
        if (e[0] == null || e[0].isNull()) {
            JDBCUtil.execute(conn.getConnection(), (sql2), resultSetBlock);
        }
    } catch (Exception ex1) {
        // If attributeName is indexed but indexed has not been initialized
        // (no VACUUM ANALYZE) ST_Estimated_Extent may throw an exception.
        // In this case, try the second method using ST_Extent
        JDBCUtil.execute(conn.getConnection(), sql2, resultSetBlock);
    }
    return e[0];
  }

  @Deprecated
  public SpatialReferenceSystemID getSRID(String tableName, String colName)
          throws SQLException {
      String key = tableName + "#" + colName;
      if (!sridMap.containsKey(key)) {
          // not in cache, so query it
          String srid = querySRID(tableName, colName);
          sridMap.put(key, new SpatialReferenceSystemID(srid));
      }
      return (SpatialReferenceSystemID)sridMap.get(key);
  }

  @Deprecated
  private String querySRID(String tableName, String colName)
  {
    final StringBuffer srid = new StringBuffer();
    // Changed by Michael Michaud 2010-05-26 (throwed exception for empty tableName)
    // String sql = "SELECT getsrid(" + colName + ") FROM " + tableName + " LIMIT 1";
    String[] tokens = tableName.split("\\.", 2);
    String schema = tokens.length==2?tokens[0]:"public";
    String table = tokens.length==2?tokens[1]:tableName;
    String sql = "SELECT srid FROM geometry_columns where (f_table_schema = '" + schema + "' and f_table_name = '" + table + "')";
    // End of the fix
    JDBCUtil.execute(conn.getConnection(), sql, new ResultSetBlock() {
      public void yield(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
          srid.append(resultSet.getString(1));
        }
      }
    });

    return srid.toString();
  }

  @Deprecated
  public String[] getGeometryAttributeNames( String datasetName ) {
    final List geometryAttributeNames = new ArrayList();
    String sql = "SELECT f_geometry_column FROM geometry_columns "
               + geomColumnMetadataWhereClause( "f_table_schema", "f_table_name", datasetName );
    JDBCUtil.execute(
        conn.getConnection(), sql,
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          geometryAttributeNames.add( resultSet.getString( 1 ) );
        }
      }
    } );
    return ( String[] ) geometryAttributeNames.toArray(new String[geometryAttributeNames.size()]);
  }
  
  public List<GeometryColumn> getGeometryAttributes( String datasetName ) {
    final List<GeometryColumn> geometryAttributes = new ArrayList<GeometryColumn>();
    String sql = "SELECT f_geometry_column, srid, type FROM geometry_columns "
               + geomColumnMetadataWhereClause( "f_table_schema", "f_table_name", datasetName );
    JDBCUtil.execute(
        conn.getConnection(), sql,
        new ResultSetBlock() {
      public void yield( ResultSet resultSet ) throws SQLException {
        while ( resultSet.next() ) {
          geometryAttributes.add(new GeometryColumn(
              resultSet.getString(1),
              resultSet.getInt(2),
              resultSet.getString(3)));
        }
      }
    } );
    return geometryAttributes;
  }

    /**
     * Returns PRIMARY KEY columns of dataset names.
     * @param datasetName name of the table (optionally prefixed by the schema name)
     * @return the list of columns involved in the Primary Key (generally, a single column)
     */
  public List<PrimaryKeyColumn> getPrimaryKeyColumns (String datasetName) {
      final List<PrimaryKeyColumn> identifierColumns = new ArrayList<PrimaryKeyColumn>();
      // query taken from http://www.alberton.info/postgresql_meta_info.html#.UewsFG29b0Q
      String sql =
          "SELECT kcu.column_name, col.data_type" +
          " FROM information_schema.table_constraints tc" +
          " LEFT JOIN information_schema.key_column_usage kcu" +
          " ON tc.constraint_catalog = kcu.constraint_catalog" +
          " AND tc.constraint_schema = kcu.constraint_schema" +
          " AND tc.constraint_name = kcu.constraint_name" +
          " LEFT JOIN information_schema.referential_constraints rc" +
          " ON tc.constraint_catalog = rc.constraint_catalog" +
          " AND tc.constraint_schema = rc.constraint_schema" +
          " AND tc.constraint_name = rc.constraint_name" +
          " LEFT JOIN information_schema.constraint_column_usage ccu" +
          " ON rc.unique_constraint_catalog = ccu.constraint_catalog" +
          " AND rc.unique_constraint_schema = ccu.constraint_schema" +
          " AND rc.unique_constraint_name = ccu.constraint_name" +
          " LEFT JOIN information_schema.columns col" +
          " ON kcu.table_catalog = col.table_catalog" +
          " AND kcu.table_schema = col.table_schema" +
          " AND kcu.table_name = col.table_name" +
          " AND kcu.column_name = col.column_name " +
          geomColumnMetadataWhereClause( "col.table_schema", "col.table_name", datasetName ) +
          " AND tc.constraint_type = 'PRIMARY KEY';";
      JDBCUtil.execute(
              conn.getConnection(), sql,
              new ResultSetBlock() {
                  public void yield( ResultSet resultSet ) throws SQLException {
                      while ( resultSet.next() ) {
                          identifierColumns.add(new PrimaryKeyColumn(
                                  resultSet.getString(1),
                                  resultSet.getString(2)));
                      }
                  }
              } );
      return identifierColumns;
  }


  public String[] getColumnNames( String datasetName ) {
    String sql = "SELECT column_name FROM information_schema.columns "
               + geomColumnMetadataWhereClause( "table_schema", "table_name", datasetName );
    ColumnNameBlock block = new ColumnNameBlock();
    JDBCUtil.execute( conn.getConnection(), sql, block );
    return block.colName;
  }


  private String geomColumnMetadataWhereClause( String schemaCol, String tableCol, String tableName ) {
    // [mmichaud 2011-07-24] Fixed a bug related to tables having common
    // names in public schema and another schema
    int dotPos = tableName.indexOf( "." );
    String schema = "public";
    String table = tableName;
    if (dotPos != -1) {
        schema = tableName.substring( 0, dotPos );
        table = tableName.substring( dotPos + 1 );
    }
    return "WHERE " + schemaCol + " = '" + schema + "'"
          + " AND " + tableCol + " = '" + table + "'";
  }
  
  /**
   * Returns whether column is used by a spatial index (Gist) or not.
   */
  public boolean isIndexed(final String schema, final String table, 
              final String column) throws SQLException {
      String sql = getGeometryIndicesQuery(schema,table,column);
      return conn.getConnection().createStatement().executeQuery(sql).next();
  }

  private static class ColumnNameBlock implements ResultSetBlock {
    List colList = new ArrayList();
    String[] colName;

    public void yield( ResultSet resultSet ) throws SQLException {
      while ( resultSet.next() ) {
        colList.add( resultSet.getString( 1 ) );
      }
      colName = ( String[] ) colList.toArray(new String[colList.size()]);
    }
  }
  
      
    /**
     * Use PostgreSQL metadata to know if schema.table.column is spatially 
     * indexed (or involved in a spatial index).
     *
     * @deprecated initially thought that statistic used by ST_Estimated_Extent was
     * depending on whether a spatial index exists or not. Finally, statistics and
     * index seem to be two independant things
     */
    @Deprecated
    public String getGeometryIndicesQuery(String schema, String table, String column) {
        return "SELECT n.nspname, t.relname, a.attname, c.relname\n" +
               "FROM pg_index i\n" +
               "-- join indexes in pg_class to filter gist indexes (783 and 4000)\n" +
               "JOIN pg_class c ON i.indexrelid = c.oid\n" +
               "-- join tables in pg_class to get the table name\n" +
               "JOIN pg_class t ON i.indrelid = t.oid\n" +
               "-- join tables in pg_tables to get the schema name\n" +
               "JOIN pg_namespace n ON t.relnamespace = n.oid\n" +
               "-- join attributes to check geometry involved in the index\n" +
               "JOIN pg_attribute a ON (a.attrelid = t.oid AND a.attnum = ANY(i.indkey))\n" +
               "-- limit the search in pg_class to index objects\n" +
               "WHERE c.relkind = 'i'\n" +
                   "AND (c.relam = 783 OR c.relam = 4000)\n" +
                   "AND n.nspname = '" + schema + "'\n" +
                   "AND t.relname = '" + table  + "'\n" +
                   "AND a.attname = '" + column + "';";
    }
  
}