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

package de.fho.jump.pirol.utilities.i18n;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;

import de.fho.jump.pirol.utilities.debugOutput.DebugUserIds;
import de.fho.jump.pirol.utilities.debugOutput.PersonalLogger;

/**
 
 * Class taken from the RasterImage-i18N PlugIn of Jan Ruzicka (jan.ruzicka@vsb.cz) and modified for PIROL.
 
 */

public final class I18NPlug {
	
	protected static PersonalLogger logger = new de.fho.jump.pirol.utilities.debugOutput.PersonalLogger(
			DebugUserIds.ALL);
	
	public static boolean jumpi18n = true;
	
	private static Hashtable plugInResourceBundle = new Hashtable();
	
	/** 
	 
	 * Set plugin I18N resource file
	 
	 * Tries to use locale set in command line (if set)
	 
	 * @param pluginName (path + name)
	 
	 * @param bundle reference of the bundle file
	 
	 */
	
	public static void setPlugInRessource(String pluginName, String bundle) {
		
		try {
			
			String local = JUMPWorkbench.I18N_SETLOCALE;
			
		} catch (java.lang.NoSuchFieldError s) {
			
			jumpi18n = false;
			
		}
		
		if (jumpi18n == true) {
			
			if (JUMPWorkbench.I18N_SETLOCALE == "") {
				
				// No locale has been specified at startup: choose default locale
				
				I18N.plugInsResourceBundle.put(pluginName, ResourceBundle
						.getBundle(bundle));
				
				//logger.printDebug(I18N.plugInsResourceBundle.get(pluginName)+" "+bundle);
				
			}
			
			else {
				
				String lang = JUMPWorkbench.I18N_SETLOCALE.split("_")[0];
				
				try {
					
					String country = JUMPWorkbench.I18N_SETLOCALE.split("_")[1];
					
					Locale locale = new Locale(lang, country);
					
					I18N.plugInsResourceBundle.put(pluginName, ResourceBundle
							.getBundle(bundle, locale));
					
					//logger.printDebug(I18N.plugInsResourceBundle.get(pluginName)+" "+bundle+" "+locale);
					
				} catch (java.lang.ArrayIndexOutOfBoundsException e) {
					
					Locale locale = new Locale(lang);
					
					I18N.plugInsResourceBundle.put(pluginName, ResourceBundle
							.getBundle(bundle, locale));
					
					//LOG.debug(I18N.plugInsResourceBundle.get(pluginName)+" "+bundle+" "+locale);
					
				}
				
			}
			
		}
		
		else {
			
			// in this case we use the default .properties file (en)
			
			I18NPlug.plugInResourceBundle.put(pluginName, ResourceBundle
					.getBundle(bundle));
			
			//logger.printDebug(I18NPlug.plugInResourceBundle.get(pluginName)+" cz.vsb.gisak.jump.rasterimage");					
			
		}
		
	}
	
	/**
	 
	 * Process text with the locale 'pluginName_<locale>.properties' file
	 
	 * 
	 
	 * @param pluginName (path + name)
	 
	 * @param label
	 
	 * @return i18n label
	 
	 */
	
	public static String get(String pluginName, String label)
	
	{
		
		if (jumpi18n == true) {
			
			/*
			 
			 logger.printDebug(I18N.plugInsResourceBundle.get(pluginName)+" "+label
			 
			 + ((ResourceBundle)I18N.plugInsResourceBundle
			 
			 .get(pluginName))
			 
			 .getString(label));
			 
			 */
			
			return ((ResourceBundle) I18N.plugInsResourceBundle
					
					.get(pluginName))
					
					.getString(label);
			
		}
		
		return ((ResourceBundle) I18NPlug.plugInResourceBundle
				
				.get(pluginName))
				
				.getString(label);
		
	}
	
	/**
	 
	 * Process text with the locale 'pluginName_<locale>.properties' file
	 
	 * 
	 
	 * @param pluginName (path + name)
	 
	 * @param label with argument insertion : {0} 
	 
	 * @param objects
	 
	 * @return i18n label
	 
	 */
	
	public static String getMessage(String pluginName, String label,
			Object[] objects) {
		
		if (jumpi18n == true) {
			
			MessageFormat mf = new MessageFormat(
					((ResourceBundle) I18N.plugInsResourceBundle
							
							.get(pluginName))
							
							.getString(label));
			
			return mf.format(objects);
			
		}
		
		MessageFormat mf = new MessageFormat(
				((ResourceBundle) I18NPlug.plugInResourceBundle
						
						.get(pluginName))
						
						.getString(label));
		
		return mf.format(objects);
		
	}
	
}
