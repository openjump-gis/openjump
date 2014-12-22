/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs;

import org.python.modules.thread;

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

    public void configure( PlugInContext context )
                            throws Exception {
      // only install WFS in PLUS, where the lib/wfs.plus/*.jar are packaged in
      try {
        Class.forName("org.deegree.ogcwebservices.wfs.WFService", false,
            this.getClass().getClassLoader());
      } catch (ClassNotFoundException e) {
        context.getWorkbenchFrame().log(
            "WFS extension disabled because deegree2-core is missing.");
        return;
      }

      new WFSPlugIn().initialize(context);
      new UpdateWFSLayerPlugIn().initialize(context);
    }

}
