package com.vividsolutions.jump.parameter;

import java.util.*;

/**
 * The schema for a {@link ParameterList}.
 * Parameter names should be case-retentive (i.e. comparisons are case-insensitive,
 * but the case is maintained).
 */
public class ParameterListSchema {

    private String[] paramNames;
    private Class[] paramClasses;
    private Map<String,Integer> name2posMap = new HashMap<>();

    public ParameterListSchema(String[] paramNames, Class[] paramClasses) {
        initialize(paramNames, paramClasses);
    }

    protected ParameterListSchema initialize(String[] paramNames, Class[] paramClasses) {
        this.paramNames = paramNames;
        this.paramClasses = paramClasses;

        for (int i = 0; i < paramNames.length; i++) {
          name2posMap.put(paramNames[i], i);
        }
        return this;
    }

    public String[] getNames()  { return paramNames; }

    public Class[] getClasses()  { return paramClasses; }

    public boolean isValidName(String name) {
        return name2posMap.containsKey(name);
    }
  
    public boolean equals(Object obj) {
        return obj instanceof ParameterListSchema &&
                equals((ParameterListSchema) obj);
    }

    private boolean equals(ParameterListSchema other) {
        if (paramNames.length != other.paramNames.length) { return false; }
        for (int i = 0; i < paramNames.length; i++) {
            if (!paramNames[i].equals(other.paramNames[i])) { return false; }
        }
        for (int i = 0; i < paramNames.length; i++) {
            if (paramClasses[i] != other.paramClasses[i]) { return false; }
        }
        return true;
    }

    public Class getClass(String name) {
        return paramClasses[name2posMap.get(name)];
    }
}