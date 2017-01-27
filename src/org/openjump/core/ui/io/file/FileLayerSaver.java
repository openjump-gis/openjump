package org.openjump.core.ui.io.file;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.task.TaskMonitor;

public interface FileLayerSaver {
  /** The key in the registry where loaders are registered. */
  String KEY = FileLayerSaver.class.getName();

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
   * Write the file specified by the URI with the map of option values.
   * 
   * @param monitor The TaskMonitor.
   * @param uri The URI to the file to load.
   * @param options The map of options.
   * @return True if the file could be loaded false otherwise.
 * @throws Exception 
   */
  boolean write(TaskMonitor monitor, URI uri, Map<String, Object> options) throws Exception;

  /**
   * Get the list of Options supported by the plug-in.
   * 
   * @return The list of Options.
   */
  List<Option> getOptionMetadata();
}
