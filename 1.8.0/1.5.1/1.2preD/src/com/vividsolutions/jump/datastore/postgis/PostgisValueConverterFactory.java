package com.vividsolutions.jump.datastore.postgis;

import java.sql.*;
import java.io.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;

import org.postgresql.*;
import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.datastore.jdbc.*;

/**
 *
 */
public class PostgisValueConverterFactory
{
  // should lazily init these
  private final ValueConverter WKT_GEOMETRY_MAPPER = new WKTGeometryValueConverter();
  private final ValueConverter WKB_GEOMETRY_MAPPER = new WKBGeometryValueConverter();

  private final Connection conn;
  private final WKBReader wkbReader = new WKBReader();
  private final WKTReader wktReader = new WKTReader();

  public PostgisValueConverterFactory(Connection conn) {
    this.conn = conn;
  }

  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException
  {
    String classname = rsm.getColumnClassName(columnIndex);
    String dbTypeName = rsm.getColumnTypeName(columnIndex);

    // MD - this is slow - is there a better way?
    if (dbTypeName.equalsIgnoreCase("geometry"))
        // WKB is now the normal way to store geometry in PostGIS [mmichaud 2007-05-13]
        return WKB_GEOMETRY_MAPPER;

    if (dbTypeName.equalsIgnoreCase("bytea"))
        return WKB_GEOMETRY_MAPPER;

    // handle the standard types
    ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
    if (stdConverter != null)
      return stdConverter;

    // default - can always show it as a string!
    return ValueConverterFactory.STRING_MAPPER;
  }

  class WKTGeometryValueConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.GEOMETRY; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException
    {
      Object valObj = rs.getObject(columnIndex);
      String s = valObj.toString();
      Geometry geom = wktReader.read(s);
      return geom;
    }
  }

  class WKBGeometryValueConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.GEOMETRY; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException
    {
      //Object obj = rs.getObject(columnIndex);
      //byte[] bytes = (byte[]) obj;
      byte[] bytes = rs.getBytes(columnIndex);
      Geometry geom = wkbReader.read(bytes);
      return geom;
    }
  }
}