package com.vividsolutions.jump.datastore.oracle;

import java.sql.*;
import java.io.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;
import com.vividsolutions.jump.datastore.jdbc.*;
import oracle.jdbc.OracleConnection;
import oracle.sql.STRUCT;
import org.geotools.data.oracle.sdo.GeometryConverter;

/**
 *
 */
public class OracleValueConverterFactory
{
  // should lazily init these
  private final ValueConverter ORA_STRUCT_GEOMETRY_MAPPER = new OracleStructGeometryValueConverter();
    public final ValueConverter WKB_OBJECT_MAPPER   = new OracleValueConverterFactory.WKBObjectValueConverter();

  private final Connection conn;
  private final WKBReader wkbReader = new WKBReader();
  private final WKTReader wktReader = new WKTReader();

  public OracleValueConverterFactory(Connection conn) {
    this.conn = conn;
  }

  public ValueConverter getConverter(ResultSetMetaData rsm, int columnIndex)
      throws SQLException
  {
    String classname = rsm.getColumnClassName(columnIndex);
    String dbTypeName = rsm.getColumnTypeName(columnIndex);

    if (dbTypeName.equalsIgnoreCase("MDSYS.SDO_GEOMETRY")) {
        // WKB is now the normal way to store geometry in PostGIS [mmichaud 2007-05-13]
        return ORA_STRUCT_GEOMETRY_MAPPER;
    }

    // handle the standard types
    ValueConverter stdConverter = ValueConverterFactory.getConverter(rsm, columnIndex);
    if (stdConverter != null)
      return stdConverter;

    // default - can always show it as a string!
    return ValueConverterFactory.STRING_MAPPER;
  }
  

  class OracleStructGeometryValueConverter implements ValueConverter
  {
    public AttributeType getType() { return AttributeType.GEOMETRY; }
    public Object getValue(ResultSet rs, int columnIndex)
        throws IOException, SQLException, ParseException
    {
        Object geometryObject = rs.getObject(columnIndex);
        org.geotools.data.oracle.sdo.GeometryConverter geometryConverter = 
            new org.geotools.data.oracle.sdo.GeometryConverter((OracleConnection) rs.getStatement().getConnection());
        return geometryConverter.asGeometry((STRUCT) geometryObject);
    }
  }

    class WKBObjectValueConverter implements ValueConverter
    {
        public AttributeType getType() { return AttributeType.OBJECT; }
        public Object getValue(ResultSet rs, int columnIndex)
                throws IOException, SQLException, ParseException
        {
            byte[] bytes = rs.getBytes(columnIndex);

            //so rs.getBytes will be one of two things:
            //1. The actual bytes of the WKB if someone did ST_AsBinary
            //2. The bytes of hex representation of the WKB.

            //in the case of #1, according to the WKB spec, the byte value
            //can only be 0 or 1.
            //in the case of #2, it's a hex string, so values range from ascii 0-F
            //use this logic to determine how to process the bytes.

            Geometry geometry = null;
            if(bytes == null || bytes.length <= 0)
            {
                geometry = wktReader.read("GEOMETRYCOLLECTION EMPTY");
            }
            else
            {
                //assume it's the actual bytes (from ST_AsBinary)
                byte[] realWkbBytes = bytes;
                if(bytes[0] >= '0')
                {
                    //ok, it's hex, convert hex string to actual bytes
                    String hexString = new String(bytes);
                    realWkbBytes = WKBReader.hexToBytes(hexString);
                }

                geometry = wkbReader.read(realWkbBytes);
            }

            return geometry;
        }
    }
}