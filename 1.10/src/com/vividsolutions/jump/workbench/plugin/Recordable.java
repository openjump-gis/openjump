package com.vividsolutions.jump.workbench.plugin;

import java.util.Map;

/**
 * Interface implemented by plugins or loader which can be recorded
 * as part of a macro
 */
public interface Recordable {

    /**
     * Set parameters with which to execute a plugin.
     * @param map a map of parameters
     */
    public void setParameters(Map<String,Object> map);

}
