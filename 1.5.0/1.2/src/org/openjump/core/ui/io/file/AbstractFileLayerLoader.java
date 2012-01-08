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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A base implementation of {@link FileLayerLoader}.
 * 
 * @author Paul Austin
 */
public abstract class AbstractFileLayerLoader implements FileLayerLoader {
  /** The descriptive name of the file format (e.g. ESRI Shapefile). */
  private String description;

  /** The list of file extensions supported by the plug-in. */
  private List<String> extensions = new ArrayList<String>();

  /** The list of Options supported by the plug-in. */
  private List<Option> optionMetadata = new ArrayList<Option>();

  /**
   * Construct a new AbstractFileLayerLoader with a description and list of
   * extensions.
   * 
   * @param description The format description.
   * @param extensions The supported file extension.
   */
  public AbstractFileLayerLoader(final String description,
    final List<String> extensions) {
    this.description = description;
    for (String extension : extensions) {
      this.extensions.add(extension.toLowerCase());
    }
  }

  /**
   * Get the descriptive name of the file format (e.g. ESRI Shapefile).
   * 
   * @return The file format name.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the list of file extensions supported by the plug-in.
   * 
   * @return The list of file extensions.
   */
  public Collection<String> getFileExtensions() {
    return extensions;
  }

  /**
   * Add an option.
   * 
   * @param name The name of the option.
   * @param type The type of the option.
   * @param required True if a value for the option is required.
   * @see Option
   */
  public void addOption(String name, String type, boolean required) {
    optionMetadata.add(new Option(name, type, required));
  }

  /**
   * Get the list of Options supported by the plug-in.
   * 
   * @return The list of Options.
   */
  public List<Option> getOptionMetadata() {
    return optionMetadata;
  }

  /**
   * Return a string representation of the loader.
   * 
   * @return The string.
   */
  public String toString() {
    return description;
  }

}
