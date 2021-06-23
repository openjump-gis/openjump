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
 * An optional release status string may be present in the string version of the
 * version.
 */
public class JUMPVersion {

  /**
   * The current version string of the OJ.
   */
  public static final String CURRENT_VERSION = getVersionNumber() + " "
      + getRelease() + " rev." + getRevision();

  /**
   * Gets the full version number, suitable for display.
   * 
   * @return the full version number, suitable for display.
   */
  public String toString() {
    return CURRENT_VERSION;
  }

  public static String getVersionNumber() {
    return I18N.getInstance().get("JUMPWorkbench.version.number");
  }

  public static String getRelease() {
    return I18N.getInstance().get("JUMPWorkbench.version.release");
  }

  public static String getRevision() {
    return I18N.getInstance().get("JUMPWorkbench.version.revision");
  }
}