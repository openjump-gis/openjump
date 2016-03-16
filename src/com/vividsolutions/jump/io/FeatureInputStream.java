package com.vividsolutions.jump.io;

import com.vividsolutions.jump.feature.*;

/**
 * A stream of features from an external source, which
 * may throw exceptions during processing.
 */
public interface FeatureInputStream {

  FeatureSchema getFeatureSchema();

  Feature next() throws Exception;

  boolean hasNext() throws Exception;

  void close() throws Exception;

}