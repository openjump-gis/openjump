package org.openjump.core.ui.plugin.datastore.postgis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

import java.sql.Types;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * static methods to help formatting sql statements for PostGIS
 * @see org.openjump.core.ui.plugin.datastore.postgis.PostGISConnectionUtil
 * for tools relying on the current connection
 */
public class PostGISQueryUtil {

	private static final WKBWriter WRITER2D = new WKBWriter(2, false);
	private static final WKBWriter WRITER3D = new WKBWriter(3, false);
	private static final WKBWriter WRITER2D_SRID = new WKBWriter(2, true);
	private static final WKBWriter WRITER3D_SRID = new WKBWriter(3, true);
	
	/**
	 * Returns a pair of strings containing unquoted schema and table names
	 * from a full table name. If the fullName contains only one part (table
	 * name), the returned array contains a null element at index 0<br>
	 * Examples :<br>
	 * <ul>
	 * <li>myschema.mytable -> [myschema, mytable]</li>
	 * <li>"MySchema"."MyTable" -> [MySchema, MyTable]</li>
	 * <li>MyTable -> [null, MyTable]</li>
	 * <li>2_table -> [null, 2_table]</li>
	 * </ul>
	 */
	public static String[] splitTableName(String fullName) {

	    if (isQuoted(fullName)) {
	        return splitQuotedTableName(fullName);
	    }
	    int index = fullName.indexOf(".");
	    // no schema
	    if (index == -1) {
	        if (fullName.matches("(?i)^[A-Z_].*")) return new String[]{null, fullName};
	        else return new String[]{null, "\"" + fullName + "\""};
	    }
	    // schema + table name
	    else {
	        String dbSchema = fullName.substring(0, index);
	        String dbTable = fullName.substring(index+1, fullName.length());
            return new String[]{dbSchema, dbTable};
	    }
	}
	
	private static String[] splitQuotedTableName(String fullName) {
	    int index = fullName.indexOf("\".\"");
	    if (index > -1) {
	        return new String[]{
	            unquote(fullName.substring(0, index)),
	            unquote(fullName.substring(index+1, fullName.length()))
	        };
	    }
	    else return new String[]{null, unquote(fullName)};
	}
	
	private static boolean isQuoted(String s) {
	    return s.startsWith("\"") && s.endsWith("\"");
	}
	
	/**
	 * Returns s if s is already quoted (with double-quotes), and a quoted 
	 * version of s otherwise. Returns null if s is null.
	 */
	public static String quote(String s) {
	    if (s == null) return null;
	    if (isQuoted(s)) return s;
	    else return "\"" + s + "\"";
	}
	
	/**
	 * Returns s without initial and final double quotes if any. 
	 * Returns null if s is null.
	 */
	public static String unquote(String s) {
	    if (s == null) return null;
	    if (!isQuoted(s)) return s;
	    else return s.substring(1, s.length()-1);
	}
	
	/**
	 * Compose concatenated quoted schema name and table name.
     * @schemaName unquoted schema name
     * @tableName unquoted table name
	 */
	public static String compose(String schemaName, String tableName) {
	    return schemaName == null ?
	            "\"" + tableName + "\"" :
	            "\"" + schemaName + "\".\"" + tableName + "\"";
	}
    
    /**
     * Returns the CREATE TABLE statement corresponding to this feature schema.
     * The statement includes column names and data types, but neither geometry
     * column nor primary key.
     * @fSchema client feature schema
     * @schemaName unquoted schema name or null to use default schema
     * @tableName unquoted table name
     * @param normalizedColumnNames whether column names must be normalized (lowercased
     *                              and without special characters) or not
     */
    public static String getCreateTableStatement(FeatureSchema fSchema,
            String schemaName, String tableName, boolean normalizedColumnNames) {
        return "CREATE TABLE " + compose(schemaName, tableName) +
               " (" + createColumnList(fSchema, true, false, false, normalizedColumnNames) + ");";
    }


    /**
     * Create statement to add a spatial index on the specified geometry column.
     * The geometry column name must have its final form. Attribute name normalization
     * is the responsability of the calling method.
     * @param schemaName unquoted schema name or null if default schema is used
     * @param tableName unquoted table name
     * @param geometryColumn unquoted geometry column name
     * @return a sql string to add a spatial index
     */
    public static String getAddSpatialIndexStatement(String schemaName, String tableName,
                String geometryColumn) {
        return "CREATE INDEX \"" +
                compose(schemaName, tableName).replaceAll("\"","") + "_" + geometryColumn + "_idx\"\n" +
               "ON " + compose(schemaName, tableName) + " USING GIST ( \"" + geometryColumn + "\" );";
    }
    
    /**
     * Returns the comma-separated list of attributes included in schema.
     * @param schema the FeatureSchema
     * @param includeSQLDataType if true, each attribute name is immediately
     *        followed by its corresponding sql DataType
     * @param includeGeometry if true, the geometry attribute is included
     * @param includeExternalPK if true, the external primary key is included
     * @param normalizedColumnName whether feature attribute names must be normalized
     *                             (lower case without spacial characters) to specify
     *                             table column names.
     */
    public static String createColumnList(FeatureSchema schema, 
                          boolean includeSQLDataType,
                          boolean includeGeometry,
                          boolean includeExternalPK,
                          boolean normalizedColumnName) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            AttributeType type = schema.getAttributeType(i);
            if (type == AttributeType.GEOMETRY && !includeGeometry) continue;
            if (!includeExternalPK && schema.getExternalPrimaryKeyIndex() == i) continue;
            String name = normalizedColumnName ?
                    normalize(schema.getAttributeName(i))
                    :schema.getAttributeName(i);
            if (0 < count++) sb.append(", ");
            sb.append("\"").append(name).append("\"");
            if (includeSQLDataType) sb.append(" ").append(getSQLType(type));
        }
        return sb.toString();
    }

    public static String escapeApostrophes(String value) {
        return value.replaceAll("'", "''");
    }
    
    /**
     * Returns the sql data type matching this OpenJUMP AttributeType
     */
    public static String getSQLType(AttributeType type) {
        if (type == AttributeType.STRING)   return "varchar";
        if (type == AttributeType.INTEGER)  return "integer";
        if (type == AttributeType.LONG)     return "bigint";
        if (type == AttributeType.DOUBLE)   return "double precision";
        if (type == AttributeType.BOOLEAN)  return "boolean";
        if (type == AttributeType.DATE)     return "timestamp";
        if (type == AttributeType.OBJECT)   return "bytea";
        if (type == AttributeType.GEOMETRY) return "geometry";
        throw new IllegalArgumentException("" + type + " is an unknown AttributeType");
    }
    
    
    /**
     * Returns the OpenJUMP AttributeType matching this sql data type
     */
    public static AttributeType getAttributeType(int sqlType, String sqlName) {
        if (sqlType == Types.BIGINT)     return AttributeType.LONG;
        // PostGIS geometries are stored as OTHER (type=1111) not BINARY (type=-2)
        if (sqlType == Types.BINARY && 
            sqlName.toLowerCase().equals("geometry")) return AttributeType.GEOMETRY;
        else if (sqlType == Types.BINARY)             return AttributeType.OBJECT;
        if (sqlType == Types.BIT)           return AttributeType.BOOLEAN;
        if (sqlType == Types.BLOB)          return AttributeType.OBJECT;
        if (sqlType == Types.BOOLEAN)       return AttributeType.BOOLEAN;
        if (sqlType == Types.CHAR)          return AttributeType.STRING;
        if (sqlType == Types.CLOB)          return AttributeType.STRING;
        if (sqlType == Types.DATALINK)      return AttributeType.OBJECT;
        if (sqlType == Types.DATE)          return AttributeType.DATE;
        if (sqlType == Types.DECIMAL)       return AttributeType.DOUBLE;
        if (sqlType == Types.DISTINCT)      return AttributeType.OBJECT;
        if (sqlType == Types.DOUBLE)        return AttributeType.DOUBLE;
        if (sqlType == Types.FLOAT)         return AttributeType.DOUBLE;
        if (sqlType == Types.INTEGER)       return AttributeType.INTEGER;
        if (sqlType == Types.JAVA_OBJECT)   return AttributeType.OBJECT;
        if (sqlType == Types.LONGNVARCHAR)  return AttributeType.STRING;
        if (sqlType == Types.LONGVARBINARY) return AttributeType.OBJECT;
        if (sqlType == Types.LONGVARCHAR)   return AttributeType.STRING;
        if (sqlType == Types.NCHAR)         return AttributeType.STRING;
        if (sqlType == Types.NCLOB)         return AttributeType.STRING;
        if (sqlType == Types.NULL)          return AttributeType.OBJECT;
        if (sqlType == Types.NUMERIC)       return AttributeType.DOUBLE;
        if (sqlType == Types.NVARCHAR)      return AttributeType.STRING;
        if (sqlType == Types.OTHER && 
            sqlName.toLowerCase().equals("geometry")) return AttributeType.GEOMETRY;
        else if (sqlType == Types.OTHER)    return AttributeType.OBJECT;
        if (sqlType == Types.REAL)          return AttributeType.DOUBLE;
        if (sqlType == Types.REF)           return AttributeType.OBJECT;
        if (sqlType == Types.ROWID)         return AttributeType.INTEGER;
        if (sqlType == Types.SMALLINT)      return AttributeType.INTEGER;
        if (sqlType == Types.SQLXML)        return AttributeType.STRING;
        if (sqlType == Types.STRUCT)        return AttributeType.OBJECT;
        if (sqlType == Types.TIME)          return AttributeType.DATE;
        if (sqlType == Types.TIMESTAMP)     return AttributeType.DATE;
        if (sqlType == Types.TINYINT)       return AttributeType.INTEGER;
        if (sqlType == Types.VARBINARY)     return AttributeType.OBJECT;
        if (sqlType == Types.VARCHAR)       return AttributeType.STRING;
        throw new IllegalArgumentException("" + sqlType + " is an unknown SQLType");
    }
    
    /**
     * Creates the query String to add a GeometryColumn.
     * <p>Note 1 : In PostGIS 2.x, srid=-1 is automatically converted to srid=0 by
     * AddGeometryColumn function.</p>
     * <p>Note 2 : To stay compatible with PostGIS 1.x, last argument of
     * AddGeometryColumn is omitted. As a consequence, geometry type is inserted
     * a the column type rather than a constraint (new default behaviour in 2.x)</p>
     * <p>The geometry column name must have its final form. Attribute name normalization
     * is the responsability of the calling method.</p>
     */
    public static String getAddGeometryColumnStatement(String schemaName, String tableName,
                String geometryColumn, int srid, String geometryType, int dim) {
        if (schemaName == null) {
            return "SELECT AddGeometryColumn('" + tableName + "','" +
                    geometryColumn + "'," +
                    srid + ",'" +
                    geometryType.toUpperCase() + "'," +
                    dim + ");";
        } else {
            return "SELECT AddGeometryColumn('" + schemaName + "','" +
                    tableName + "','" +
                    geometryColumn + "'," +
                    srid + ",'" +
                    geometryType.toUpperCase() + "'," +
                    dim + ");";
        }
    }

    /**
     * Converts the geometry into a byte array in EWKB format
     * @param geom the geometry to convert to a byte array
     * @param hasSrid whether the geometry srid has to be included in the byte array or not
     * @param dimension geometry dimension (2 or 3)
     * @return a byte array containing a EWKB representation of the geometry
     */
    public static byte[] getByteArrayFromGeometry(Geometry geom, boolean hasSrid, int dimension) {
		WKBWriter writer;
		if (hasSrid) {
			writer = dimension==3? WRITER3D_SRID : WRITER2D_SRID;
		}
		else writer = dimension==3? WRITER3D : WRITER2D;
		return writer.write(geom);
	}

    /**
     * Converts the geometry into a byte array in EWKB format
     * @param geom the geometry to convert to a byte array
     * @param srid the srid of the geometry
     * @param dimension geometry dimension (2 or 3)
     * @return a byte array containing a EWKB representation of the geometry
     */
    public static byte[] getByteArrayFromGeometry(Geometry geom, int srid, int dimension) {
        WKBWriter writer;
        geom.setSRID(srid);
        writer = dimension==3? WRITER3D_SRID : WRITER2D_SRID;
        return writer.write(geom);
    }
    
    
    /**
     * Return 3 if coll contains at least one 3d geometry, 2 if coll contains
     * only 2d geometries and defaultDim if coll is empty.
     */
    public static int getGeometryDimension(FeatureCollection coll, int defaultDim) {
        if (coll.size() > 0) {
            // will explore up to 1000 features regularly distributed in the dataset
            // if none of these feature has dim = 3, return 2, else return 3
            int step = 1 + coll.size()/1000;
            int count = 0;
            for (Iterator it = coll.iterator() ; it.hasNext() ; ) {
                if (count%step == 0 && 
                        getGeometryDimension(((Feature)it.next()).getGeometry()) == 3) {
                    return 3;
                }
                count++;
            }
            return 2;
        } else return defaultDim;
    }
    
    
    private static int getGeometryDimension(Geometry g) {
        Coordinate[] cc = g.getCoordinates();
        for (Coordinate c : cc) {
            if (!Double.isNaN(c.z)) return 3;
        }
        return 2;
    }
    
    
    /**
     * Get this FeatureCollection geometry type.
     * Returns defaultType if coll is empty or if coll contains two geometries
     * with different types.
     */
    public static String getGeometryType(FeatureCollection coll, String defaultType) {
        if (coll.size() > 0) {
            Feature f = (Feature)coll.iterator().next();
            String firstGeometryType = f.getGeometry().getGeometryType();
            for (Iterator it = coll.iterator() ; it.hasNext() ; ) {
                f = (Feature)it.next();
                if (!f.getGeometry().getGeometryType().equals(firstGeometryType)) {
                    return defaultType;
                }
            }
            return firstGeometryType;
        }
        else return defaultType;
    }
    
    public static int getSrid(FeatureCollection coll, int defaultSrid) {
        CoordinateSystem cs = coll.getFeatureSchema().getCoordinateSystem();
        if (cs != null) {
            try {
                return cs.getEPSGCode();
            }
            catch(UnsupportedOperationException e) {
                return defaultSrid;
            }
        } else return defaultSrid;
    }

    public static String normalize(String name) {
        if (name == null) return null;
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0 ; i < name.length() ; i++) {
            char c = name.charAt(i);
            if(i==0) {
                if (Character.isLetter(c) || c == '_') sb.append(Character.toLowerCase(c));
                else sb.append('_');
            } else {
                if (Character.isLetterOrDigit(c) || c == '_') sb.append(Character.toLowerCase(c));
                else sb.append('_');
            }
        }
        return sb.toString();
    }

}