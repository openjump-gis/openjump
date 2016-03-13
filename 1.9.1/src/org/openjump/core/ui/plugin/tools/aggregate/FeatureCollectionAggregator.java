package org.openjump.core.ui.plugin.tools.aggregate;

import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.I18N;

import java.util.*;

/**
 * Creates a new FeatureDataset from a source FeatureCollection,
 * <ul>
 *     <li>a list of attributes to be used as the key</li>
 *     <li>a list of aggregator function to aggregate attribute values which are not part of the key</li>
 * </ul>
 *
 */
public class FeatureCollectionAggregator {

    private final static String KEY = FeatureCollectionAggregator.class.getName();

    private FeatureCollection fc;
    private List<String> keyAttributes;
    List<AttributeAggregator> aggregators;

    public FeatureCollectionAggregator(FeatureCollection fc, List<String> keyAttributes, List<AttributeAggregator> aggregators)
            throws AggregatorException {
        this.fc = fc;
        this.keyAttributes = keyAttributes;
        this.aggregators = aggregators;
        // Check validity of input attribute names
        for (int i = 0 ; i < keyAttributes.size() ; i++) {
            if (!fc.getFeatureSchema().hasAttribute(keyAttributes.get(i))) {
                throw new AggregatorException(I18N.getMessage(KEY + ".attribute-does-not-exists", keyAttributes.get(i)));
            }
        }
        for (AttributeAggregator aggregator : aggregators) {
            if (!fc.getFeatureSchema().hasAttribute(aggregator.getInputName())) {
                throw new AggregatorException(I18N.getMessage(KEY + ".attribute-does-not-exists", aggregator.getInputName()));
            }
        }
    }

    /**
     * Returns a FeatureCollection where attributes defines by aggregators are
     * aggregated on features having the same key.
     * Feature's key are defined by one or more attributes from the source
     * featureCollection.
     * @return
     */
    public FeatureCollection getAggregatedFeatureCollection() {
        Map<Key,List<AttributeAggregator>> map = new HashMap<Key, List<AttributeAggregator>>();
        // Add attributes values to features with teh same key
        for (Object object : fc.getFeatures()) {
            Feature feature = (Feature)object;
            Key key = new Key(feature, keyAttributes);
            List<AttributeAggregator> featureAggregators = map.get(key);
            if (featureAggregators == null) {
                featureAggregators = new ArrayList<AttributeAggregator>();
                for (AttributeAggregator agg : aggregators) {

                    featureAggregators.add(new AttributeAggregator(
                            agg.getInputName(),
                            agg.getAggregator().clone(),
                            agg.getOutputName()));
                }
                map.put(key, featureAggregators);
            }
            for (AttributeAggregator agg : featureAggregators) {
                agg.getAggregator().addValue(feature.getAttribute(agg.getInputName()));
            }
        }

        FeatureSchema newSchema = getFeatureSchema();
        FeatureCollection result = new FeatureDataset(newSchema);
        for (Map.Entry<Key,List<AttributeAggregator>> entry : map.entrySet()) {
            Feature feature = new BasicFeature(newSchema);
            for (String keyAtt : keyAttributes) {
                feature.setAttribute(keyAtt, entry.getKey().map.get(keyAtt));
            }
            for (AttributeAggregator agg : entry.getValue()) {
                feature.setAttribute(agg.getOutputName(), agg.getAggregator().getResult());
            }
            result.add(feature);
        }
        return result;
    }

    private FeatureSchema getFeatureSchema() {
        FeatureSchema oldSchema = fc.getFeatureSchema();
        FeatureSchema newSchema = new FeatureSchema();
        for (int i = 0 ; i < keyAttributes.size() ; i++) {
            newSchema.addAttribute(keyAttributes.get(i),
                    oldSchema.getAttributeType(oldSchema.getAttributeIndex(keyAttributes.get(i))));
        }
        for (AttributeAggregator agg : aggregators) {
            newSchema.addAttribute(agg.getOutputName(), agg.getAggregator().getOutputAttributeType());
        }
        return newSchema;
    }

    public static class Key {

        private Map<String,Object> map = new HashMap<String, Object>();

        Key(Feature feature, List<String> attributes) {
            for (String name : attributes) {
                map.put(name, feature.getAttribute(name));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return map.equals(key.map);
        }

        @Override
        public int hashCode() {
            return map != null ? map.hashCode() : 0;
        }

    }

    public static class AggregatorException extends Exception {
        public AggregatorException(String message) {
            super(message);
        }
    }

}
