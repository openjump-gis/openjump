/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.io.datasource;

import java.util.Collection;

import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * A channel of communication with a DataSource.
 */
public interface Connection {

    /**
     * Returns from a DataSource a dataset specified using a query string (the format
     * of which is implementation-dependent). Callers: be sure to call 
     * DataSource#setCoordinateSystem on the returned FeatureCollection.
     * @param query identifies the dataset; may take the form of a SQL statement,
     * a table name, null (if there is only one dataset), or other format
     * @param exceptions a Collection to hold exceptions that occurred (so that processing can continue).
     * @return null if a FeatureCollection could not be created because of a serious
     * problem (indicated in the exceptions)
     */
    FeatureCollection executeQuery(String query, Collection<Throwable> exceptions, TaskMonitor monitor);

    /**
     * Returns from a DataSource a dataset specified using a query string (the format
     * of which is implementation-dependent). If an exception occurs, processing
     * is stopped and the exception thrown. Callers: be sure to call 
     * DataSource#setCoordinateSystem on the returned FeatureCollection.
     */
    FeatureCollection executeQuery(String query, TaskMonitor monitor)
        throws Exception;

    /**
     * Modifies data in the DataSource accordinate to a query string (the format of
     * which is implementation-dependent).
     */
    void executeUpdate(String query, FeatureCollection featureCollection, TaskMonitor monitor)
        throws Exception;

    /**
     * Ends the connection, performing any necessary cleanup.
     */
    void close();
}
