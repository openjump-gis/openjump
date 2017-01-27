/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.io.file;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.task.TaskMonitor;

/**
 * <p>
 * The FileLayerLoader defines the interface for plug-ins that can load files
 * into the current Task.
 * </p>
 * <p>
 * A file loader has a {@link #getDescription()} used in the GUI and a list of
 * {@link #getFileExtensions()} that it can be used to load.
 * </p>
 * <p>
 * The {@link #getOptionMetadata()} can be used to define a list of
 * {@link Option}s that a user can/must provide when loading the file. These
 * will be used by the GUI to create fields for entry of these options.
 * </p>
 * 
 * @author Paul Austin
 */
public interface FileLayerLoader {
  /** The key in the registry where loaders are registered. */
  String KEY = FileLayerLoader.class.getName();

  /**
   * Get the list of file extensions supported by the plug-in.
   * 
   * @return The list of file extensions.
   */
  Collection<String> getFileExtensions();

  /**
   * Get the descriptive name of the file format (e.g. ESRI Shapefile).
   * 
   * @return The file format name.
   */
  String getDescription();

  /**
   * Open the file specified by the URI with the map of option values.
   * 
   * @param monitor The TaskMonitor.
   * @param uri The URI to the file to load.
   * @param options The map of options.
   * @return True if the file could be loaded false otherwise.
   */
  boolean open(TaskMonitor monitor, URI uri, Map<String, Object> options);

  /**
   * Get the list of Options supported by the plug-in.
   * 
   * @return The list of Options.
   */
  List<Option> getOptionMetadata();
}
