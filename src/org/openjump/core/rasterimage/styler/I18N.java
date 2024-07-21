package org.openjump.core.rasterimage.styler;

import java.io.File;

/**
 * light weight wrapper to easily integrate raster styler translation
 */
public class I18N {
  private static com.vividsolutions.jump.I18N I18N = null;

  private static void initialize() {
    if (I18N == null)
      I18N = com.vividsolutions.jump.I18N.getInstance(new File("language/rasterimage/styler"));
  }

  public static String get(final String label, final Object... objects) {
    // initialize lazily, only if needed
    initialize();
    return I18N.get(label, objects);
  }

  public static String get(final String label) {
    return get(label, null);
  }
}
