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


public class FileLayerLoaderExtensionFilter extends FileNameExtensionFilter {

  private FileLayerLoader fileLayerLoader;

  public FileLayerLoaderExtensionFilter(FileLayerLoader fileLayerLoader) {
    super(getDescription(fileLayerLoader), getExtensionArray(fileLayerLoader));
    this.fileLayerLoader = fileLayerLoader;
  }

  public FileLayerLoader getFileLoader() {
    return fileLayerLoader;
  }

/*
  private static String createDescription(final String description,
    Collection fileExtensions) {
    StringBuffer fullDescription = new StringBuffer(description);
    fullDescription.append(" (");
    for (Iterator extensions = fileExtensions.iterator(); extensions.hasNext();) {
      String extension = (String)extensions.next();
      fullDescription.append("*.");
      fullDescription.append(extension);
      if (extensions.hasNext()) {
        fullDescription.append(",");
      }
    }
    fullDescription.append(")");
    return fullDescription.toString();
  }
*/

  private static String getDescription(FileLayerLoader fileLayerLoader) {
    return fileLayerLoader.getDescription();
  }

  private static String[] getExtensionArray(FileLayerLoader fileLayerLoader) {
    return fileLayerLoader.getFileExtensions().toArray(new String[0]);
  }

}
