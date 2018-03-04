package org.openjump.core.ui.plugin.tools.aggregate;

import com.vividsolutions.jump.feature.AttributeType;

import java.util.List;
import java.util.Set;

/**
 * An aggregator accumulate individual values with addValue method and
 * return a synthetic value with getResult method.
 */
public interface Aggregator<T> {

    String SEPARATOR_NAME = "Separator";

    /**
     * Create a new Aggregator with the same attributes as this one.
     */
    Aggregator clone();

    /**
     * Returns true if this aggregator must ignore null values.
     */
    boolean ignoreNull();

    /**
     * Change the way this aggregator process null values.
     */
    void setIgnoreNull(boolean ignoreNull);

    /**
     * Return parameter names used by this aggregator
     * @return the set of parameters
     */
    Set<String> getParameters();

    /**
     * Set a parameter value for this aggregator.
     * @param name the name of the aggregator
     * @param value the new value of the parameter
     */
    void setParameter(String name, Object value);

    /**
     * Returns parameter value for name parameter.
     * @param name the name of the parameter
     */
    Object getParameter(String name);

    /**
     * Returns the name of this Aggregator.
     */
    String getName();

    /**
     * @Return the AttributeType of the aggregated value.
     */
    AttributeType getOutputAttributeType();

    /**
     * Adds a new value to the aggregator.
     * @param value value to be added
     */
    void addValue(T value);

    /**
     * @return the values accumulated by this aggregator.
     */
    List<T> getValues();

    /**
     * @return the aggregated value.
     */
    Object getResult();

}
