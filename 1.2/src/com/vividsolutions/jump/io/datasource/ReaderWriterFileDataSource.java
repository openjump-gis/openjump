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

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.io.JUMPWriter;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * Adapts the old JUMP I/O API (Readers and Writers) to the new JUMP I/O API
 * (DataSources).
 */
public class ReaderWriterFileDataSource extends DataSource {
    protected JUMPReader reader;
    protected JUMPWriter writer;
    
    public ReaderWriterFileDataSource(JUMPReader reader, JUMPWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }   

    public Connection getConnection() {
        return new Connection() {
            public FeatureCollection executeQuery(String query, Collection exceptions, TaskMonitor monitor) {
                try {
					return reader.read(getReaderDriverProperties());
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                    //<<TODO>> Modify Readers and Writers to store exceptions and continue
                    //processing. [Jon Aquino]
                }
            }

            public void executeUpdate(String update, FeatureCollection featureCollection, TaskMonitor monitor)
                throws Exception {
                writer.write(featureCollection, getWriterDriverProperties());
            }

            public void close() {}

            public FeatureCollection executeQuery(String query, TaskMonitor monitor) throws Exception {
                ArrayList exceptions = new ArrayList();
                FeatureCollection featureCollection = executeQuery(query, exceptions, monitor);
                if (!exceptions.isEmpty()) {
                    throw (Exception) exceptions.iterator().next();
                }
                return featureCollection;
            }
        };
    }

    protected DriverProperties getReaderDriverProperties() {
        return getDriverProperties();
    }

    protected DriverProperties getWriterDriverProperties() {
        return getDriverProperties();
    }

    private DriverProperties getDriverProperties() {
        DriverProperties properties = new DriverProperties();
        properties.putAll(getProperties());
        return properties;
    }

}
