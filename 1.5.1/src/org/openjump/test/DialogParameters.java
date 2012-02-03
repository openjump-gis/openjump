/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for 
 * visualizing and manipulating spatial features with geometry and attributes.
 * Copyright (C) 2012  The JUMP/OpenJUMP contributors
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation, either version 2 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openjump.test;

import java.util.HashMap;

import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * @author Benjamin Gudehus
 */
@SuppressWarnings("serial")
public class DialogParameters extends MultiInputDialog {
    
    //-----------------------------------------------------------------------------------
    // FIELDS.
    //-----------------------------------------------------------------------------------
    
    private HashMap<String, Object> fields = new HashMap<String, Object>();

    //-----------------------------------------------------------------------------------
    // METHODS.
    //-----------------------------------------------------------------------------------
    
    public void putField(String fieldName, Object value) {
        fields.put(fieldName, value);
    }
    
    @Override
    public String getText(String fieldName) {
        return (String) fields.get(fieldName);
    }

    @Override
    public boolean getBoolean(String fieldName) {
        return (Boolean) fields.get(fieldName);
    }
    
    @Override
    public double getDouble(String fieldName) {
        return (Double) fields.get(fieldName);
    }
    
    @Override
    public int getInteger(String fieldName) {
        return (Integer) fields.get(fieldName);
    }
    
    @Override
    public Layer getLayer(String fieldName) {
        return (Layer) fields.get(fieldName);
    }
    
    @Override
    public void setVisible(boolean visible) {
        throw new UnsupportedOperationException("Shouldn't be called in execute().");
    }

}
