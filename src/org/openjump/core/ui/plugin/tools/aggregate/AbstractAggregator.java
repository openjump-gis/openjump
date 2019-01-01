package org.openjump.core.ui.plugin.tools.aggregate;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.I18N;

import java.util.*;

/**
 * Basic implementation of Aggregator interface.
 */
abstract class AbstractAggregator<T> implements Aggregator<T> {

    final private AttributeType outputType;
    private boolean ignoreNull;
    private Map<String,Object> parameters;
    private List<T> values;

    AbstractAggregator(AttributeType outputType, boolean ignoreNull, Object...kv) {
        this.outputType = outputType;
        this.ignoreNull = ignoreNull;
        if (kv != null) {
            if (kv.length % 2 == 1) {
                throw new IllegalArgumentException("Aggregator constructor should have " +
                        "an even number of arguments representing successively keys and values");
            }
            this.parameters = new LinkedHashMap<>();
            for (int i = 0 ; i < kv.length/2 ; i += 2) {
                this.parameters.put(kv[i].toString(), kv[i+1]);
            }
        }
        values = new ArrayList<>();
    }

    @Override
    public Set<String> getParameters() {
        return parameters == null ? new HashSet<String>() : parameters.keySet();
    }

    @Override
    public void setParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
        parameters.put(name, value);
    }

    @Override
    public Object getParameter(String name) {
        return parameters == null ? null : parameters.get(name);
    }

    @Override
    public String getName() {
        String simpleName = getClass().getSimpleName();
        return I18N.get(Aggregator.class.getName() + "." + simpleName.substring(simpleName.indexOf('$')+1));
    }

    @Override
    public boolean ignoreNull() {
        return ignoreNull;
    }

    @Override
    public void setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

    @Override
    public void addValue(T object) {
        if (object != null || !ignoreNull) {
            values.add(object);
        }
    }

    @Override
    public List<T> getValues() {
        return values;
    }

    @Override
    public AttributeType getOutputAttributeType() {
        return outputType;
    }

    @Override
    public abstract Object getResult();

    @Override
    public void reset() {
        if (values != null) {
            values.clear();
        }
    }

    @Override
    public abstract Aggregator clone();

    @Override
    public String toString() {
        return getName();
    }

}
