package com.vividsolutions.jump.datastore;

public class DataStoreException
    extends Exception
{

  public DataStoreException(String msg) {
    super(msg);
  }

  public DataStoreException(String msg, Exception cause) {
    super(msg, cause);
  }

  public DataStoreException(Exception cause) {
    super(cause);
  }

}