package com.vividsolutions.jump.datastore.jdbc;

import java.sql.*;

/**
 * A block of code to execute on a result set.
 *
 * @author Martin Davis
 * @version 1.0
 */

public interface ResultSetBlock
{
    void yield(ResultSet resultSet) throws Exception;
}
