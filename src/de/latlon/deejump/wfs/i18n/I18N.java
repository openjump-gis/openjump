/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Serge N'Cho (ncho@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.i18n;

import java.io.File;

/**
 * Thin wrapper to use OJ's i18n code.
 */
public class I18N {
  private static final File path = new File("language/wfs/messages");

  /**
   * @param key
   * @param arguments
   * @return a translated string
   */
  public static String get(String key, Object... arguments) {
    return com.vividsolutions.jump.I18N.getMessage(path, key, arguments);
  }
}