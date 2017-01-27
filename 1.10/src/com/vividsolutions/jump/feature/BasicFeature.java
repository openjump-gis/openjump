
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

package com.vividsolutions.jump.feature;

import java.io.Serializable;


/**
 *  Default implementation of the Feature interface.
 */
public class BasicFeature extends AbstractBasicFeature implements Serializable {

    private static final long serialVersionUID = -7891137208054228529L;
    
    private Object[] attributes;
    private short modCount = 0;
    private boolean modified = false;

    /**
     * Constructs a BasicFeature with the given FeatureSchema specifying the
     * attribute names and types.
     */
    public BasicFeature(FeatureSchema featureSchema) {
        super(featureSchema);
        attributes = new Object[featureSchema.getAttributeCount()];        
    }


    /**
     * A low-level accessor that is not normally used. It is called by ViewSchemaPlugIn.
     */
    public void setAttributes(Object[] attributes) {
    	Object[] attributesOld = this.attributes;
    	this.attributes = attributes;
    	if (attributes != null)
    		if (attributesOld.length != attributes.length) 
    			modified = true;
    		else {
    			for (int i=0; i<attributes.length; i++) {
    				if ( attributesOld[i] != null && attributesOld[i] != attributes[i]) {
    					modified = true;
    				}
    			}
    		}
    }

    /**
     * Sets the specified attribute.
     *
     *@param  attributeIndex  the array index at which to put the new attribute
     *@param  newAttribute    the new attribute
     */
    public void setAttribute(int attributeIndex, Object newAttribute) {
    	modCount++;
    	if (attributes[attributeIndex] != null || modCount > attributes.length) {
    		modified = true;
    	}	        	                                  
        attributes[attributeIndex] = newAttribute;
    }

    /**
     * Returns the specified attribute.
     *
     *@param  i the index of the attribute to get
     *@return the attribute
     */
    public Object getAttribute(int i) {
        // [mmichaud 2012-10-13] handle dynamic attributes
        if (getSchema().isOperation(i)) {
            try {
                return getSchema().getOperation(i).invoke(this);
            } catch(Exception e) {
                return new Error(e); // error is not catched
            }
        }
        else return attributes[i];
        //We used to eat ArrayOutOfBoundsExceptions here. I've removed this behaviour
        //because ArrayOutOfBoundsExceptions are bugs and should be exposed. [Jon Aquino]
    }

    /**
     * A low-level accessor that is not normally used. It is called by ViewSchemaPlugIn.
     */
    public Object[] getAttributes() {
        return attributes;
    }

    /**
     * @return true if any attribute of this Feature (including Geometry) has been set more
     * than once.
     */
    public boolean isModified() { 
    	return modified;
    }
    
    /**
     * @param modified - allows the modified flag to be set or reset
     */
    public void setModified(boolean modified) {
    	this.modified = modified;
    	modCount = 0;
    }
 }
