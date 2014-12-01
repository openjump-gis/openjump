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
package com.vividsolutions.jump;

/**
 * JUMP version information.
 * <p>
 * Versions consist of a 3-part version number: <code>major.minor.patch</code>
 * An optional release status string may be present in the string version of
 * the version.
 *
 * @version 1.5.0
 */
public class JUMPVersion {

  /**
   * The current version number of the JTS API.
   */
  public static final JUMPVersion CURRENT_VERSION = new JUMPVersion();

  /**
   * The major version number.
   */
  //public static final int MAJOR = 1;

  /**
   * The minor version number.
   */
  //public static final int MINOR = 5;

  /**
   * The patch version number.
   */
  //public static final int PATCH = 0;

  /**
   * An optional string providing further release info (such as "alpha 1" or
   * svn version);
   */
  //private static final String releaseInfo = "alpha";

  /**
   * Prints the current JTS version to stdout.
   *
   * @param args the command-line arguments (none are required).
   */
  /*public static void main(String[] args) {
    System.out.println(CURRENT_VERSION);
  }

  private JUMPVersion() {}
*/
  /**
   * Gets the major number of the release version.
   *
   * @return the major number of the release version.
   */
  //public int getMajor() { return MAJOR; }

  /**
   * Gets the minor number of the release version.
   *
   * @return the minor number of the release version.
   */
  //public int getMinor() { return MINOR; }

  /**
   * Gets the patch number of the release version.
   *
   * @return the patch number of the release version.
   */
  //public int getPatch() { return PATCH; }

  /**
   * Gets the full version number, suitable for display.
   *
   * @return the full version number, suitable for display.
   */
  public String toString() {
    String ver = I18N.get("JUMPWorkbench.version.number");
    String releaseInfo = I18N.get("JUMPWorkbench.version.release");
    if (releaseInfo != null && releaseInfo.length() > 0)
      return ver + " " + releaseInfo;
    return ver;
  }

}