package org.openjump.core.ui.plugin.datastore.postgis2;

import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * A query performed against a PostGIS data source.
 */
public class SaveToPostGISDataSourceQuery extends DataSourceQuery {

    private Map properties = null;

    /**
     * Creates a new query.
     * @param dataSource The data source go be query against.
     * @param query The "sql" of the query.
     * @param name Name of the query.
     */
    public SaveToPostGISDataSourceQuery(DataSource dataSource, String query,
                                        String name) {
        super(dataSource, query, name);
    }

    /**
     * Returns the DataSource for the query.
     */
    public DataSource getDataSource() {
        DataSource ds = super.getDataSource();
        ds.setProperties(properties);
        return(ds);
    }

    /**
     * Property map for the query object.
     * For defined keys see: {@link org.openjump.core.ui.plugin.datastore.postgis.SaveToPostGISDataSource}
     */
    public void setProperties(Map properties) {
        this.properties = new HashMap(properties);
    }
}
