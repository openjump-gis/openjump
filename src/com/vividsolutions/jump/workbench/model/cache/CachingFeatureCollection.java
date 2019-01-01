package com.vividsolutions.jump.workbench.model.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.LazyList;
import com.vividsolutions.jump.util.ListWrapper;
import com.vividsolutions.jump.workbench.ui.plugin.AddNewLayerPlugIn;

/**
 * Caches features to prevent unnecessary queries. Useful for wrapping
 * database-backed FeatureCollections. All calls are delegated to the cache,
 * except for calls to query(envelope).iterator() where (1) the envelope is not
 * within the cache envelope, and (2) the call is made in a non-GUI thread.
 */
// The cache is a ThreadSafeFeatureCollection. [Jon Aquino 2005-03-04]
public class CachingFeatureCollection extends FeatureCollectionWrapper {

    private Envelope envelopeOfCompletedCache = new Envelope();

    private FeatureCollection featureCollection;

    private boolean cachingByEnvelope = true;

    public CachingFeatureCollection(final FeatureCollection featureCollection) {
        // Note that this implementation assumes that the feature collection is
        // being viewed by a single LayerViewPanel. This is the common case;
        // however, it is possibile that there could be multiple LayerViewPanels
        // viewing the feature collection i.e. if the user clicks Clone Window.
        // This is not used very often though. [Jon Aquino 2005-03-03]
        super(AddNewLayerPlugIn.createBlankFeatureCollection());
        
        this.featureCollection = featureCollection;
    }

	/**
	 * @see com.vividsolutions.jump.feature.FeatureCollectionWrapper#getEnvelope()
	 */
	public Envelope getEnvelope() {
		try{
			Envelope e = featureCollection.getEnvelope();
			if(e!=null)
				return e;
		}catch(Throwable t){t.printStackTrace();}
		return super.getEnvelope();
	}

	/**
	 * @see com.vividsolutions.jump.feature.FeatureCollectionWrapper#getFeatureSchema()
	 */
	public FeatureSchema getFeatureSchema() {
		return featureCollection.getFeatureSchema();
	}

    public List<Feature> query(final Envelope envelope) {
        // This code achieves its simplicity using two wrappers:
        // LazyList and ListWrapper. [Jon Aquino 2005-03-22]

        // To prevent an unnecessary query of the cached feature collection, use
        // a LazyList to defer the query, as we don't yet know whether the
        // methods are called on or off the GUI thread. In the former case, use
        // the cached feature collection; in the latter case (and only for the
        // #iterator method, as this is the only method used in the
        // LayerRenderer thread), use the live feature collection.
        // [Jon Aquino 2005-03-03]
        final LazyList<Feature> cachedFeatureCollectionQueryResults = new LazyList<>(
                new Block() {
                    public Object yield() {
                        return getCachedFeatureCollection().query(envelope);
                    }
                });
        // Use a ListWrapper to delegate all calls to the cached feature
        // collection, except for calls to #iterator, which may or may not use
        // the cache, depending on whether we are in the GUI thread.
        // [Jon Aquino 2005-03-03]
        return new ListWrapper<Feature>() {
            public Collection<Feature> getCollection() {
                return cachedFeatureCollectionQueryResults;
            }

            public Iterator<Feature> iterator() {
                // Caching criterion 1: envelope check [Jon Aquino 2005-03-22]
                if (cachingByEnvelope
                        && envelopeOfCompletedCache.contains(envelope)) {
                    return super.iterator();
                }
                // Caching criterion 2: GUI-thread check [Jon Aquino 2005-03-22]
                if (SwingUtilities.isEventDispatchThread()) {
                    // Don't do database queries on the GUI thread, as we don't
                    // want the GUI thread to be held up by long operations.
                    // Instead, delegate to the cached feature collection.
                    // [Jon Aquino 2005-03-03]
                    return super.iterator();
                }
                final Iterator iterator = featureCollection.query(envelope)
                        .iterator();
                initializeCacheIfNecessary();
                emptyCache();
                envelopeOfCompletedCache = new Envelope();
                return new Iterator<Feature>() {
                    public void remove() {
                        iterator.remove();
                    }

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Feature next() {
                        Feature nextFeature = (Feature) iterator.next();
                        getCachedFeatureCollection().add(nextFeature);
                        if (!hasNext()) {
                            // Set the cache envelope only when the cache is
                            // complete. [Jon Aquino 2005-03-03]
                            envelopeOfCompletedCache = new Envelope(envelope);
                        }
                        return nextFeature;
                    }

                };
            }
        };
    }

    private boolean initialized = false;

    private void initializeCacheIfNecessary() {
        // The FeatureSchema might not defined until the last minute
        // i.e. until FeatureCollection#query is called [Jon Aquino
        // 2005-03-04]
        // Example: DynamicFeatureCollection [Jon Aquino 2005-03-22]
        if (initialized) {
            return;
        }
        setCachedFeatureCollection(new ThreadSafeFeatureCollectionWrapper(
                new FeatureDataset(featureCollection.getFeatureSchema())));
        initialized = true;
    }

    private FeatureCollection getCachedFeatureCollection() {
        return getFeatureCollection();
    }

    private void setCachedFeatureCollection(
            FeatureCollection cachedFeatureCollection) {
        setFeatureCollection(cachedFeatureCollection);
    }

    /**
     * This setting is ignored if the call to query(envelope).iterator() is made
     * on the GUI thread, because long queries would make the GUI unresponsive.
     * 
     * @param cachingByEnvelope
     *            whether query(envelope).iterator() delegates to the cache if
     *            envelope is within the cache envelope
     */
    public CachingFeatureCollection setCachingByEnvelope(
            boolean cachingByEnvelope) {
        this.cachingByEnvelope = cachingByEnvelope;
        return this;
    }

	public void emptyCache() {
		getCachedFeatureCollection().clear();
        envelopeOfCompletedCache = new Envelope();
	}
}