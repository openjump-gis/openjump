package com.vividsolutions.jump.workbench.model.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * Thread safety is achieved by (1) synchronizing the methods, and (2) creating
 * new Collections in #getFeatures, #query, and #iterator to prevent
 * ConcurrentModificationExceptions.
 */
// Used as the cache for a CachedDynamicFeatureCollection [Jon Aquino
// 2005-03-04]
public class ThreadSafeFeatureCollectionWrapper implements FeatureCollection {

	private FeatureCollection featureCollection;

	public ThreadSafeFeatureCollectionWrapper(
			FeatureCollection featureCollection) {
		this.featureCollection = featureCollection;
	}

	public synchronized FeatureSchema getFeatureSchema() {
		return featureCollection.getFeatureSchema();
	}

	public synchronized Envelope getEnvelope() {
		return featureCollection.getEnvelope();
	}

	public synchronized int size() {
		return featureCollection.size();
	}

	public synchronized boolean isEmpty() {
		return featureCollection.isEmpty();
	}

	public synchronized List getFeatures() {
		return new ArrayList(featureCollection.getFeatures());
	}

	public synchronized Iterator iterator() {
		return new ArrayList(featureCollection.getFeatures()).iterator();
	}

	public synchronized List query(Envelope envelope) {
		return new ArrayList(featureCollection.query(envelope));
	}

	public synchronized void add(Feature feature) {
		featureCollection.add(feature);

	}

	public synchronized void addAll(Collection features) {
		featureCollection.addAll(features);

	}

	public synchronized void removeAll(Collection features) {
		featureCollection.removeAll(features);

	}

	public synchronized void remove(Feature feature) {
		featureCollection.remove(feature);

	}

	public synchronized void clear() {
		featureCollection.clear();

	}

	public synchronized Collection remove(Envelope env) {
		return featureCollection.remove(env);
	}

}