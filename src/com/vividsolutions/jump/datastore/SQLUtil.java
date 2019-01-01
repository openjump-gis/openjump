package com.vividsolutions.jump.datastore;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jump.feature.AttributeType;

import java.sql.Types;
import java.text.Normalizer;

/**
 * Utililty class containing methods to manipulate SQL Strings
 */
public class SQLUtil {

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


    /**
     * Returns true if this identifier is quoted.
     */
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
     * Escape single quotes in the given identifier.
     * Replace all single quotes ("'") by double single quotes ("''")
     * @param identifier string identifier to escape
     * @return the identifier with single quotes escaped, or identifier if no string found
     */
    public static String escapeSingleQuote(String identifier) {
        return identifier == null ? null : identifier.replaceAll("'", "''");
    }


    /**
     * Compose a concatenated quoted schema name and table name.
     * @param schemaName unquoted schema name
     * @param tableName unquoted table name
     */
    public static String compose(String schemaName, String tableName) {
        return schemaName == null ?
                "\"" + tableName + "\"" :
                "\"" + schemaName + "\".\"" + tableName + "\"";
    }


    /**
     * Normalize an identifier name (use only lower case)
     * @param name the identifier to normalize
     * @return the name writen in lowercase
     */
    public static String normalize(String name) {
        if (name == null) return null;
        // NFKD is stronger than NFD, for example decompose single charater
        // \u0308 (ffi_ligature into ffi (three letters)
        name = Normalizer.normalize(name, Normalizer.Form.NFKD);
        name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0 ; i < name.length() ; i++) {
            char c = name.charAt(i);
            if(i==0) {
                if (Character.isDigit(c)) sb.append("_").append(Character.toLowerCase(c));
                else if (c < 128 && (Character.isLetter(c) || c == '_')) sb.append(Character.toLowerCase(c));
                else sb.append('_');
            } else {
                if (c < 128 && (Character.isLetterOrDigit(c)) || c == '_') sb.append(Character.toLowerCase(c));
                else sb.append('_');
            }
        }
        return sb.toString();
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
        if (srid > 0) {
            geom.setSRID(srid);
            writer = dimension == 3 ? WRITER3D_SRID : WRITER2D_SRID;
        } else {
            writer = dimension == 3 ? WRITER3D : WRITER2D;
        }
        return writer.write(geom);
    }


    /**
     * Returns OpenJUMP attributeType from sql type and datatype name.
     * dataTypeName is nullable.
     * If dataTypeName = "geometry", binary field will be interpreted into
     * AttributeType.GEOMETRY
     * @param sqlType jdbc sql datatype
     * @param dataTypeName native datatype name
     */
    static public AttributeType getAttributeType(int sqlType, String dataTypeName) {
        if (sqlType == Types.BIGINT)        return AttributeType.LONG;
        // PostGIS geometries are stored as OTHER (type=1111) not BINARY (type=-2)
        if (sqlType == Types.BINARY && dataTypeName != null &&
                dataTypeName.toLowerCase().equals("geometry"))
                                            return AttributeType.GEOMETRY;
        else if (sqlType == Types.BINARY)   return AttributeType.OBJECT;
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
        if (sqlType == Types.OTHER && dataTypeName != null &&
                dataTypeName.toLowerCase().equals("geometry"))
                                            return AttributeType.GEOMETRY;
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



}
