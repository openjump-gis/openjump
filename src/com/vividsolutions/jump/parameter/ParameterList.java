package com.vividsolutions.jump.parameter;

import java.util.*;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.LangUtil;

/**
 * A strongly-typed list of parameters for passing to a component
 */
public class ParameterList {
    private ParameterListSchema schema;

    private Map params = new HashMap();

    public ParameterList(ParameterListSchema schema) {
        initialize(schema);
    }

    public ParameterList(ParameterList other) {
        initialize(other.getSchema());
        for (Iterator i = Arrays.asList(other.getSchema().getNames()).iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            setParameter(name, other.getParameter(name));
        }
    }

    protected ParameterList initialize(ParameterListSchema schema) {
        this.schema = schema;
        return this;
    }

    public ParameterListSchema getSchema() {
        return schema;
    }

    public ParameterList setParameter(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public boolean equals(Object obj) {
        return equals((ParameterList) obj);
    }

    private boolean equals(ParameterList other) {
        if (!schema.equals(other.schema)) {
            return false;
        }
        for (Iterator i = params.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (!LangUtil.bothNullOrEqual(params.get(name), other.params
                    .get(name))) {
                return false;
            }
        }
        return true;
    }

    public boolean containsParameter(String name){
      return params.containsKey(name);
    }

    public Object getParameter(String name) {
        return params.get(name);
    }

    public String getParameterString(String name) {
        return (String) params.get(name);
    }

    public int getParameterInt(String name)
    {
      Object value = params.get(name);
      if (value instanceof String)
        return Integer.parseInt((String) value);
      return ((Integer) params.get(name)).intValue();
    }

}