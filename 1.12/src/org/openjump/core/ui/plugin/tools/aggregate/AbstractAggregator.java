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

    public Set<String> getParameters() {
        return parameters == null ? new HashSet<String>() : parameters.keySet();
    }

    public void setParameter(String name, Object value) {
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
        parameters.put(name, value);
    }

    public Object getParameter(String name) {
        return parameters == null ? null : parameters.get(name);
    }

    public String getName() {
        String simpleName = getClass().getSimpleName();
        return I18N.get(Aggregator.class.getName() + "." + simpleName.substring(simpleName.indexOf('$')+1));
    }

    public boolean ignoreNull() {
        return ignoreNull;
    }

    public void setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

    public void addValue(T object) {
        if (object != null || !ignoreNull) {
            values.add(object);
        }
    }

    public List<T> getValues() {
        return values;
    }

    public AttributeType getOutputAttributeType() {
        return outputType;
    }

    public abstract Object getResult();

    public abstract Aggregator clone();

    public String toString() {
        return getName();
    }

}
