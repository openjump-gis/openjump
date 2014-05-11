package com.vividsolutions.jump.datastore;

/**
 * An ad-hoc {@link Query}against a {@link DataStoreConnection}. Uses the
 * query language provided by the datastore (if any). May not be supported by
 * all datastores.
 */
public class AdhocQuery implements Query {

    private String queryString;
    private String primaryKey;

    public AdhocQuery(String queryString) {
        this.queryString = queryString;
    }

    public String getQuery() {
        return queryString;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

}