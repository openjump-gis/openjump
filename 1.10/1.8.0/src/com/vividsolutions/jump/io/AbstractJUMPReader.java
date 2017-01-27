package com.vividsolutions.jump.io;

import com.vividsolutions.jump.feature.FeatureCollection;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Michaël
 * Date: 09/10/13
 * Time: 07:49
 * To change this template use File | Settings | File Templates.
 */
abstract public class AbstractJUMPReader implements JUMPReader {

    private Collection<Exception> exceptions;

    /**
     * Read the specified file using the filename given by the "File" property
     * and any other parameters.
     */
    public abstract FeatureCollection read(DriverProperties dp) throws Exception;

    /**
     * @return exceptions collected during the reading process.
     */
    public Collection<Exception> getExceptions() {
        if (exceptions == null) exceptions = new ArrayList<Exception>();
        return exceptions;
    }

}
