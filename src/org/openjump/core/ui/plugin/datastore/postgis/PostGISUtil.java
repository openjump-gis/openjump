package org.openjump.core.ui.plugin.datastore.postgis;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * static methods to help formatting sql statements for PostGIS
 */
public class PostGISUtil {
    
    //private static final WKBWriter WRITER   = new WKBWriter(2, false);
	//private static final WKBWriter WRITER2D = new WKBWriter(2, false);
	//private static final WKBWriter WRITER3D = new WKBWriter(3, false);
	//private static final WKBWriter WRITER2D_SRID = new WKBWriter(2, true);
	//private static final WKBWriter WRITER3D_SRID = new WKBWriter(3, true);
    //
	////SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
	//
	///**
	// * Returns a two Strings array containing the schema name and the table name
	// * from a full table name. If the fullName contains only one part (table
	// * name), the returned array contains a null element at index 0<br>
	// * Examples :<br>
	// * <ul>
	// * <li>myschema.mytable -> [myschema, mytable]</li>
	// * <li>"MySchema"."MyTable" -> ["MySchema", "MyTable"] (case sensitive)</li>
	// * <li>MyTable -> [null, MyTable]</li>
	// * <li>2_table -> [null, "2_table"]</li>
	// * </ul>
	// */
	//public static String[] divideTableName(String fullName) {
    //
	//    if (isQuoted(fullName)) {
	//        return divideQuotedTableName(fullName);
	//    }
	//    
	//    int index = fullName.indexOf(".");
	//    // no schema
	//    if (index == -1) {
	//        if (fullName.matches("(?i)^[A-Z_].*")) return new String[]{null, fullName};
	//        else return new String[]{null, "\"" + fullName + "\""};
	//    }
	//    // schema + table name
	//    else {
	//        String dbSchema = fullName.substring(0, index);
	//        String dbTable = fullName.substring(index+1, fullName.length());
	//        if (dbSchema.matches("(?i)^[A-Z_].*") && dbTable.matches("(?i)^[A-Z_].*")) {
	//            return new String[]{dbSchema, dbTable};
	//        }
	//        else return new String[]{quote(dbSchema), quote(dbTable)};
	//    }
	//}
	//
	//private static String[] divideQuotedTableName(String fullName) {
	//    int index = fullName.indexOf("\".\"");
	//    if (index > -1) {
	//        return new String[]{
	//            fullName.substring(0, index), 
	//            fullName.substring(index+1, fullName.length())
	//        };
	//    }
	//    else return new String[]{null, fullName};
	//}
	//
	//private static boolean isQuoted(String s) {
	//    return s.startsWith("\"") && s.endsWith("\"");
	//}
	//
	///**
	// * Returns s if s is already quoted (with double-quotes), and a quoted 
	// * version of s otherwise. Returns null if s is null.
	// */
	//public static String quote(String s) {
	//    if (s == null) return null;
	//    if (isQuoted(s)) return s;
	//    else return "\"" + s + "\"";
	//}
	//
	///**
	// * Returns s without initial and final double quotes if any. 
	// * Returns null if s is null.
	// */
	//public static String unquote(String s) {
	//    if (s == null) return null;
	//    if (!isQuoted(s)) return s;
	//    else return s.substring(1, s.length()-1);
	//}
	//
	///**
	// * Compose concatenate dbSchema name and dbTable name without making any
	// * assumption whether names are quoted or not.
	// */
	//public static String compose(String dbSchema, String dbTable) {
	//    return dbSchema == null ? 
	//            "\"" + unquote(dbTable) + "\"" : 
	//            "\"" + unquote(dbSchema) + "\".\"" + unquote(dbTable) + "\"";
	//}
    //
    ///**
    // * Returns the CREATE TABLE statement corresponding to this feature schema.
    // * The statement does not include geometry column.
    // */
    //public static String getCreateTableStatement(FeatureSchema fSchema, String dbSchema, String dbTable) {
    //    return "CREATE TABLE " + compose(dbSchema, dbTable) + 
    //           " (" + createColumnList(fSchema, true, false) + ");";
    //}
    //
    //
    //public static String getAddSpatialIndexStatement(String dbSchema, String dbTable, String geometryColumn) {
    //    return "CREATE INDEX \"" + 
    //        compose(dbSchema, dbTable).replaceAll("\"","") + "_" + geometryColumn + "_idx\"" + 
    //        " ON " + compose(dbSchema, dbTable) + " USING GIST ( \"" + geometryColumn + "\" )"; 
    //}
    //
    ///**
    // * Returns the comma-separated list of attributes included in schema.
    // * @param schema the FeatureSchema
    // * @param incudeDataType if true, each attribute name is immediately 
    // *        followed by its corresponding sql datatype
    // * @param includeGeometry if true, the geometry attribute is included
    // */
    //public static String createColumnList(FeatureSchema schema, 
    //                      boolean includeSQLDataType, boolean includeGeometry) {
    //    StringBuffer sb = new StringBuffer();
    //    int count = 0;
    //    for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
    //        AttributeType type = schema.getAttributeType(i);
    //        if (type == AttributeType.GEOMETRY && !includeGeometry) continue;
    //        String name = schema.getAttributeName(i);
    //        if (0 < count++) sb.append(", ");
    //        sb.append("\"" + name + "\"");
    //        if (includeSQLDataType) sb.append(" ").append(getSQLType(type));
    //    }
    //    return sb.toString();
    //}
    //
    //
    //public static String escapeApostrophes(String value) {
    //    return value.replaceAll("'", "''");
    //}
    //
    ///**
    // * Returns the sql data type matching this OpenJUMP AttributeType
    // */
    //public static String getSQLType(AttributeType type) {
    //    if (type == AttributeType.STRING)   return "varchar";
    //    if (type == AttributeType.INTEGER)  return "integer";
    //    if (type == AttributeType.DOUBLE)   return "double precision";
    //    if (type == AttributeType.DATE)     return "timestamp";
    //    if (type == AttributeType.OBJECT)   return "bytea";
    //    if (type == AttributeType.GEOMETRY) return "geometry";
    //    throw new IllegalArgumentException("" + type + " is an unknown AttributeType");
    //}
    //
    //
    ///**
    // * Returns the OpenJUMP AttributeType matching this sql data type
    // */
    //public static AttributeType getAttributeType(int sqlType, String sqlName) {
    //    if (sqlType == Types.BIGINT)     return AttributeType.INTEGER;
    //    // PostGIS geometries are stored as OTHER (type=1111) not BINARY (type=-2)
    //    if (sqlType == Types.BINARY && 
    //        sqlName.toLowerCase().equals("geometry")) return AttributeType.GEOMETRY;
    //    else if (sqlType == Types.BINARY)             return AttributeType.OBJECT;
    //    if (sqlType == Types.BIT)        return AttributeType.INTEGER;
    //    if (sqlType == Types.BLOB)       return AttributeType.OBJECT;
    //    if (sqlType == Types.BOOLEAN)    return AttributeType.INTEGER;
    //    if (sqlType == Types.CHAR)       return AttributeType.STRING;
    //    if (sqlType == Types.CLOB)       return AttributeType.STRING;
    //    if (sqlType == Types.DATALINK)   return AttributeType.OBJECT;
    //    if (sqlType == Types.DATE)       return AttributeType.DATE;
    //    if (sqlType == Types.DECIMAL)    return AttributeType.DOUBLE;
    //    if (sqlType == Types.DISTINCT)   return AttributeType.OBJECT;
    //    if (sqlType == Types.DOUBLE)     return AttributeType.DOUBLE;
    //    if (sqlType == Types.FLOAT)      return AttributeType.DOUBLE;
    //    if (sqlType == Types.INTEGER)    return AttributeType.INTEGER;
    //    if (sqlType == Types.JAVA_OBJECT)   return AttributeType.OBJECT;
    //    if (sqlType == Types.LONGNVARCHAR)  return AttributeType.STRING;
    //    if (sqlType == Types.LONGVARBINARY) return AttributeType.OBJECT;
    //    if (sqlType == Types.LONGVARCHAR)   return AttributeType.STRING;
    //    if (sqlType == Types.NCHAR)      return AttributeType.STRING;
    //    if (sqlType == Types.NCLOB)      return AttributeType.STRING;
    //    if (sqlType == Types.NULL)       return AttributeType.OBJECT;
    //    if (sqlType == Types.NUMERIC)    return AttributeType.DOUBLE;
    //    if (sqlType == Types.NVARCHAR)   return AttributeType.STRING;
    //    if (sqlType == Types.OTHER && 
    //        sqlName.toLowerCase().equals("geometry")) return AttributeType.GEOMETRY;
    //    else if (sqlType == Types.OTHER) return AttributeType.OBJECT;
    //    if (sqlType == Types.OTHER)      return AttributeType.OBJECT;
    //    if (sqlType == Types.REAL)       return AttributeType.DOUBLE;
    //    if (sqlType == Types.REF)        return AttributeType.OBJECT;
    //    if (sqlType == Types.ROWID)      return AttributeType.INTEGER;
    //    if (sqlType == Types.SMALLINT)   return AttributeType.INTEGER;
    //    if (sqlType == Types.SQLXML)     return AttributeType.STRING;
    //    if (sqlType == Types.STRUCT)     return AttributeType.OBJECT;
    //    if (sqlType == Types.TIME)       return AttributeType.DATE;
    //    if (sqlType == Types.TIMESTAMP)  return AttributeType.DATE;
    //    if (sqlType == Types.TINYINT)    return AttributeType.INTEGER;
    //    if (sqlType == Types.VARBINARY)  return AttributeType.OBJECT;
    //    if (sqlType == Types.VARCHAR)    return AttributeType.STRING;
    //    throw new IllegalArgumentException("" + sqlType + " is an unknown SQLType");
    //}
    //
    ///**
    // * Create the query String to add a GeometryColumn.
    // * Note 1 : In PostGIS 2.x, srid=-1 is automatically converted to srid=0 by
    // * AddGeometryColumn function.
    // * Note 2 : To stay compatible with PostGIS 1.x, last argument of 
    // * AddGeometryColumn is omitted. As a consequence, geometry type is inserted
    // * a the column type rather than a constraint (new default behaviour in 2.x)
    // */
    //public static String getAddGeometryColumnStatement(String dbSchema, String dbTable, 
    //            String geometryColumn, int srid, String geometryType, int dim) {
    //    dbSchema = dbSchema == null ? "" : "'" + unquote(dbSchema) + "'::varchar,";
    //    return "SELECT AddGeometryColumn(" + 
    //            dbSchema + "'" + unquote(dbTable) + "'::varchar,'" + 
    //            geometryColumn + "'::varchar," + 
    //            srid + ",'" + 
    //            geometryType.toUpperCase() + "'::varchar," + 
    //            dim + ");";
    //}
    //
    //
    //public static byte[] getByteArrayFromGeometry(Geometry geom, boolean hasSrid, int dimension) {
	//	WKBWriter writer = WRITER;
	//	if (hasSrid) {
	//		writer = dimension==3? WRITER3D_SRID : WRITER2D_SRID;
	//	}
	//	else writer = dimension==3? WRITER3D : WRITER2D;
	//	return writer.write(geom);
	//}
    //
    //
    //public static int getGeometryDimension(Geometry g) {
    //    Coordinate[] cc = g.getCoordinates();
    //    int d = 2;
    //    for (Coordinate c : cc) {
    //        if (!Double.isNaN(c.z)) return 3;
    //    }
    //    return 2;
    //}
    //
    //
    ///** 
    // * Returns a list of attributes compatibe between postgis table and
    // * featureSchema.
    // */
    //public static String[] compatibleSchemaSubset(java.sql.Connection connection, 
    //                          String dbSchema, String dbTable, FeatureSchema featureSchema) throws SQLException {
    //    DatabaseMetaData metadata = connection.getMetaData();
    //    ResultSet rs = metadata.getColumns(null, unquote(dbSchema), unquote(dbTable), null);
    //    Map<String,AttributeType> map = new HashMap<String,AttributeType>();
    //    while (rs.next()) {
    //        map.put(rs.getString("COLUMN_NAME"),
    //            PostGISUtil.getAttributeType(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME")));
    //    }
    //    List<String> subset = new ArrayList<String>();
    //    for (int i = 0 ; i < featureSchema.getAttributeCount() ; i++) {
    //        String attribute = featureSchema.getAttributeName(i);
    //        AttributeType type = featureSchema.getAttributeType(i);
    //        if (map.containsKey(attribute) && (map.get(attribute)==type)) {
    //            subset.add(attribute);
    //        }
    //    }
    //    return subset.toArray(new String[subset.size()]);
    //}

}