package com.vividsolutions.jump.io;

import com.vividsolutions.jump.feature.*;

/**
 * Base class for FeatureInputStreamReaders.
 * Handles the details of buffering the stream of features
 * to allow for lookahead.
 * This allows subclasses to implement the simpler semantics
 * of "return null if no more features".
 *
 * Subclasses need to define readNext and close.
 * They also need to set the featureSchema instance variable.
 */
public abstract class BaseFeatureInputStream implements FeatureInputStream {

    private Feature nextFeature = null;

    public abstract FeatureSchema getFeatureSchema();

    public Feature next() throws Exception {
        if (nextFeature == null) {
            return readNext();
        }
        Feature currFeature = nextFeature;
        nextFeature = null;
        return currFeature;
    }

    public boolean hasNext() throws Exception {
        if (nextFeature == null) {
            nextFeature = readNext();
        }
        return nextFeature != null;
    }

  /**
   * Read the next feature, if any.
   *
   * @return the next Feature, or <code>null</code> if there is none
   * @throws Exception if an exception occured while reading next Feature
   */
    protected abstract Feature readNext() throws Exception;

    public abstract void close() throws Exception;
}