/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

import de.latlon.deejump.wfs.plugin.UpdateWFSLayerPlugIn;
import de.latlon.deejump.wfs.plugin.WFSPlugIn;

/**
 * Installs WFS and WFS Update plugin.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
public class WFSExtension extends Extension {
  private static String msg = "Disabled because deegree2-core is missing. Try OJ PLUS!";
  private static boolean disabled = false;

  public String getName() {
    return "WFS(-T) 1.0/1.1 Extension (Lat/Lon)";
  }

  /**
   * the default version is 'svn revision (build date)'
   */
  public String getVersion() {
    return "1.2 rev." + I18N.get("JUMPWorkbench.version.revision") + "("
        + I18N.get("JUMPWorkbench.version.buildDate") + ")";
  }

  public String getMessage() {
    return disabled ? msg : "";
  }

  public void configure(PlugInContext context) throws Exception {
    // we use our classloader as we should have been instantiated by the plugin classloader
    ClassLoader pluginLoader = getClass().getClassLoader();
    // only install WFS in PLUS, where the lib/wfs.plus/*.jar are packaged in
    try {
      Class.forName("org.deegree.ogcwebservices.wfs.WFService", false, pluginLoader);
      
      new WFSPlugIn().initialize(context);
      new UpdateWFSLayerPlugIn().initialize(context);
    } catch (ClassNotFoundException e) {
      disabled = true;
      context.getWorkbenchFrame().log(msg);
      return;
    }
  }

}
