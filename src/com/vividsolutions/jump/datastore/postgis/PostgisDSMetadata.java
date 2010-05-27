package com.vividsolutions.jump.datastore.postgis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jump.datastore.DataStoreMetadata;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.datastore.jdbc.JDBCUtil;
import com.vividsolutions.jump.datastore.jdbc.ResultSetBlock;



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
    return ( String[] ) datasetNames.toArray( new String[]{} );
  }


  public Envelope getExtents( String datasetName, String attributeName ) {
    final Envelope[] e = new Envelope[]{null};
    //
    // Use find_extent - sometimes estimated_extent was returning null
    //    
    String sql = "";
    //find_extent needs schema and table as separate arguments or it fails with "relation does not exist"
    if(datasetName.indexOf('.') != -1) {
        String[] parts = datasetName.split("\\.", 2);
        sql = "SELECT AsBinary(find_extent( '" + parts[0] + "', '" + parts[1] +"', '" + attributeName + "' ))";
    } else {
        sql = "SELECT AsBinary(find_extent( '" + datasetName + "', '" + attributeName + "' ))";
    }

    JDBCUtil.execute(
        conn.getConnection(), sql,
        new ResultSetBlock() {
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
    } );
        return e[0];
  }

  public SpatialReferenceSystemID getSRID(String tableName, String colName)
          throws SQLException {
      String key = tableName + "#" + colName;
      if (!sridMap.containsKey(key)) {
          // not in cache, so query it
          String srid = querySRID(tableName, colName);
          sridMap.put(key, new SpatialReferenceSystemID(srid));
      }
      SpatialReferenceSystemID srid = (SpatialReferenceSystemID) sridMap
              .get(key);
      return srid;
  }

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
    return ( String[] ) geometryAttributeNames.toArray( new String[]{} );
  }


  public String[] getColumnNames( String datasetName ) {
    String sql = "SELECT column_name FROM information_schema.columns "
               + geomColumnMetadataWhereClause( "table_schema", "table_name", datasetName );
    ColumnNameBlock block = new ColumnNameBlock();
    JDBCUtil.execute( conn.getConnection(), sql, block );
    return block.colName;
  }


  private String geomColumnMetadataWhereClause( String schemaCol, String tableCol, String tableName ) {
    int dotPos = tableName.indexOf( "." );
    return dotPos == -1
                  ? "WHERE lower(" + tableCol + ") = '" + tableName.toLowerCase() + "'"
    : "WHERE lower(" + schemaCol + ") = '"
                  + tableName.substring( 0, dotPos ).toLowerCase()
                  + "' "
                  + " AND lower(" + tableCol + ") = '"
                  + tableName.substring( dotPos + 1 ).toLowerCase() + "'";
  }


  private static class ColumnNameBlock implements ResultSetBlock {
    List colList = new ArrayList();
    String[] colName;

    public void yield( ResultSet resultSet ) throws SQLException {
      while ( resultSet.next() ) {
        colList.add( resultSet.getString( 1 ) );
      }
      colName = ( String[] ) colList.toArray( new String[0] );
    }
  }
}