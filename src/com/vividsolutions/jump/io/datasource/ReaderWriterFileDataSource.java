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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.io.JUMPWriter;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorSupport;
import com.vividsolutions.jump.task.TaskMonitorUtil;

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

      @Override
      public FeatureCollection executeQuery(String query,
          Collection<Throwable> exceptions, TaskMonitor monitor) {
        try {
          // make readers task monitor aware
          DriverProperties dp = getReaderDriverProperties();
          if (reader instanceof TaskMonitorSupport) {
            ((TaskMonitorSupport) reader).setTaskMonitor(monitor);
            TaskMonitorUtil
                .setTitle(
                    monitor,
                    I18N.getMessage(
                        "com.vividsolutions.jump.io.datasource.ReaderWriterFileDataSource.open-{0}",
                        createDescriptiveName(new URI(dp
                            .getProperty("Uri")))));
          }
          FeatureCollection fc = reader.read(dp);
          exceptions.addAll(reader.getExceptions());
          return fc;
        } catch (Exception e) {
          exceptions.add(e);
          return null;
          // <<TODO>> Modify Readers and Writers to store exceptions and
          // continue processing. [Jon Aquino]
        }
      }

      @Override
      public void executeUpdate(String update,
          FeatureCollection featureCollection, TaskMonitor monitor)
          throws Exception {
        writer.write(featureCollection, getWriterDriverProperties());
      }

      @Override
      public void close() {
      }

      @Override
      public FeatureCollection executeQuery(String query, TaskMonitor monitor)
          throws Exception {
        ArrayList<Throwable> exceptions = new ArrayList<>();
        FeatureCollection featureCollection = executeQuery(query, exceptions,
            monitor);
        if (!exceptions.isEmpty()) {
          throw (Exception) exceptions.iterator().next();
        }
        return featureCollection;
      }

    };
  }

  /**
   * return 'file.ext (archive.ext)' for archive members and 'file.ext' for all others
   */
  private static String createDescriptiveName(URI uri) {
    if (CompressedFile.isArchive(uri))
      return UriUtil.getZipEntryName(uri) + " (" + UriUtil.getZipFileName(uri)
          + ")";

    return UriUtil.getFileName(uri);
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

  @Override
  public boolean isReadable() {
    return reader instanceof JUMPReader;
  }

  @Override
  public boolean isWritable() {
    return writer instanceof JUMPWriter;
  }

}
