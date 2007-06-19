package com.vividsolutions.jump.workbench.imagery.ecw;

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
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;

/**
 */
public class ECWImageFactory implements ReferencedImageFactory {

    public static final String TYPE_NAME = "ECW";

    public ECWImageFactory() {
    }

    public String getTypeName() {
        return TYPE_NAME;
    }

    public ReferencedImage createImage(String location) throws JUMPException {
        return new ECWImage(location);
    }

    public String getDescription() {
        return "Enhanced Compressed Wavelet";
    }

    public String[] getExtensions() {
        return new String[] { "ecw" };
    }

    public boolean isEditableImage(String location) {
        return false;
    }

	public boolean isAvailable() {
		Class c = null;
		try{
			c = this.getClass().getClassLoader().loadClass("com.ermapper.ecw.JNCSRenderer");
		}catch(ClassNotFoundException e){
			// eat it
			return false;
		}
		
		return c != null;
	}

}