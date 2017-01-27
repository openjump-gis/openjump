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

import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

//import de.soldin.jumpcore.ExtClassLoader;


/**
 * Installs the {@link de.soldin.jumpcore.geomconv.GCPlugin}
 *
 * @see com.vividsolutions.jump.workbench.plugin.Extension
 */
public class GCExtension
	extends Extension
	{
	public static final String NAME = "Geometry Converter (de.soldin.jumpcore.geomconv)";
	public static final String VERSION = "0.3core";
	//private static ExtClassLoader ecl;
	
	public void configure(PlugInContext context) throws Exception {
		//ExtClassLoader ecl = getClassLoader();
		//Class clazz = ecl.loadClass("de.soldin.jumpcore.GCPlugin");
		//PlugIn plugin = (PlugIn) clazz.newInstance();
		GCPlugin plugin = new GCPlugin();
		plugin.initialize(context);
	}

	public String getVersion(){ return VERSION; }
	
	public String getName(){ return NAME; }
	
	/*public static ExtClassLoader getClassLoader() throws Exception{
		if (ecl instanceof ExtClassLoader)
			return ecl;
		
		Class clazz = GCExtension.class;
		ecl = new ExtClassLoader( clazz.getClassLoader(), false );
		// keep interfaces in parent loader
		ecl.blacklist("^(?i:de.soldin.jumpcore.IExtExtension)$");
		
		String base = ExtClassLoader.getBase( clazz );
		// add extension.jar
		ecl.add( base );
		//System.out.println(clazz.getName()+" base is: "+base);
		// add <extension>/ folder
		String libFolder = ExtClassLoader.getLibFolder( clazz, "geomconv" );
		ecl.add( libFolder );
		System.out.println(clazz.getName()+" libs are in: "+libFolder);
		// add <extension>/*.jar
		ecl.addAllFiles( libFolder, "jar", true );
		
		return ecl;
	}*/
}
