/**
 *
 * Copyright 2011 Edgar Soldin
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.soldin.jumpcore.geomconv;

import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

//import de.soldin.jumpcore.ExtClassLoader;

/**
 * Installs the {@link de.soldin.jumpcore.geomconv.GCPlugin}
 * TODO Is this class useless now that GeometryConverted has been included into
 * the CORE ?
 * 
 * @see com.vividsolutions.jump.workbench.plugin.Extension
 */
public class GCExtension extends Extension {
  public static final String NAME = "Geometry Converter (de.soldin.jumpcore.geomconv)";
  public static final String VERSION = JUMPVersion.CURRENT_VERSION;

  public void configure(PlugInContext context) throws Exception {
    // TODO: move installation here from GCPlugin.initialize()
  }

  public String getVersion() {
    return VERSION;
  }

  public String getName() {
    return NAME;
  }

}
