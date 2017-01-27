package com.vividsolutions.jump.datastore.jdbc;

import java.sql.*;
import com.vividsolutions.jump.feature.*;

/**
 * An interface for objects which can transform columns
 * from ResultSets into JUMP data types
 */
public interface ValueConverter
{
  AttributeType getType();
  Object getValue(ResultSet rs, int column) throws Exception;
}