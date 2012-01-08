package com.vividsolutions.jump.datastore;

/**
 * An ad-hoc {@link Query}against a {@link DataStoreConnection}. Uses the
 * query language provided by the datastore (if any). May not be supported by
 * all datastores.
 */
public class AdhocQuery implements Query {
    private String queryString;

    public AdhocQuery(String queryString) {
        this.queryString = queryString;
    }

    public String getQuery() {
        return queryString;
    }

}