/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vividsolutions.jump.datastore.jdbc;

import java.util.ArrayList;

/**
 * A simple bean to store a query with its list of bound parameters (if any).
 * Suitable to feed JDBCUtil.query with expected query parameters.
 * To execute a query without parameters, pass an empty array of objects
 * @author nicolas Ribot
 */
public class BoundQuery {
  private String query;
  private ArrayList<Object> parameters;

  public BoundQuery(String query, ArrayList<Object> parameters) {
    this.query = query;
    this.parameters = parameters;
  }

  public BoundQuery(String query) {
    this(query, new ArrayList<Object>());
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Object[] getParameters() {
    return parameters.toArray();
  }

  public void setParameters(ArrayList<Object>  parameters) {
    this.parameters = parameters;
  }
  
  public BoundQuery addParameter(Object param) {
    this.parameters.add(param);
    return this;
  }
}
