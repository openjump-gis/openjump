package com.vividsolutions.jump.datastore.postgis;

import java.sql.*;

import org.postgresql.*;

import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.datastore.*;
import com.vividsolutions.jump.datastore.jdbc.*;

/**
 * Implements the mapping between a result set and
 * a {@link FeatureSchema} and {@link Feature} set.
 *
 * This is a transient worker class, whose lifetime
 * should be no longer than the lifetime of the
 * provided ResultSet
 */
public class PostgisResultSetConverter
{
  private ResultSet rs;
  private FeatureSchema featureSchema;
  private int geometryColIndex = -1;
  private ValueConverter[] mapper;
  private PostgisValueConverterFactory odm;
  private boolean isInitialized = false;

  public PostgisResultSetConverter(Connection conn, ResultSet rs)
  {
    odm = new PostgisValueConverterFactory(conn);
    this.rs = rs;
  }

  public FeatureSchema getFeatureSchema()
      throws SQLException
  {
    init();
    return featureSchema;
  }

  public Feature getFeature()
      throws Exception
  {
    init();
    Feature f = new BasicFeature(featureSchema);
    for (int i = 0; i < mapper.length; i++) {
      f.setAttribute(i, mapper[i].getValue(rs, i + 1));
    }
    return f;
  }

  private void init()
    throws SQLException
  {
    if (isInitialized) return;
    isInitialized = true;

    ResultSetMetaData rsmd = rs.getMetaData();
    int numberOfColumns = rsmd.getColumnCount();
    //String[] columnNames = new String[numberOfColumns];
    //String[] columnTypeNames = new String[numberOfColumns];
    //int[] columnPositions = new int[numberOfColumns];
    mapper = new ValueConverter[numberOfColumns];
    featureSchema = new FeatureSchema();

    for (int i = 0; i < numberOfColumns; i++)
    {
      mapper[i] = odm.getConverter(rsmd, i + 1);
      String colName = rsmd.getColumnName(i + 1);
      // only handles one geometry col for now [MD ?]
      // Convert the first geometry into AttributeType.GEOMETRY and the following ones
      // into AttributeType.STRINGs [mmichaud 2007-05-13]
      if (mapper[i].getType() == AttributeType.GEOMETRY) {
        if (featureSchema.getGeometryIndex() == -1) {
          // fixed by mmichaud using a patch from jaakko [2008-05-21] :
          // use colName instead of "GEOMETRY" for attribute name
          featureSchema.addAttribute(colName, mapper[i].getType());
        }
        else {
          mapper[i] = ValueConverterFactory.STRING_MAPPER;
          featureSchema.addAttribute(colName, AttributeType.STRING);
        }
      }
      else {
          featureSchema.addAttribute(colName, mapper[i].getType());
      }
    }
  }

}