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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vividsolutions.jump.workbench.model.Layer;

/**
 * @author Benjamin Gudehus
 */
public class DialogParametersTest {

    // TODO: Test that methods gracefully complain about invalid calls.
    // TODO: Test nonexistent hash keys.
    
    @Test
    public void testGetText() {
        // expect: "text value"
        DialogParameters parameters = new DialogParameters();
        parameters.putField("text", "value");
        assertEquals("value", parameters.getText("text"));
    }
    
    @Test
    public void testGetBoolean() {
        // expect: "boolean value"
        DialogParameters parameters = new DialogParameters();
        parameters.putField("boolean", false);
        assertEquals(false, parameters.getBoolean("boolean"));
    }
    
    @Test
    public void testGetDouble() {
        // expect: "double value"
        DialogParameters parameters = new DialogParameters();
        parameters.putField("double", 3.14);
        assertEquals(3.14, parameters.getDouble("double"), 0.01);
    }
    
    @Test
    public void testGetInteger() {
        // expect: "integer value"
        DialogParameters parameters = new DialogParameters();
        parameters.putField("integer", 42);
        assertEquals(42, parameters.getInteger("integer"));
    }
    
    @Test
    public void testGetLayer() {
        // expect: "layer object"
        DialogParameters parameters = new DialogParameters();
        Layer layer = new Layer();
        parameters.putField("layer", layer);
        assertEquals(layer, parameters.getLayer("layer"));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSetVisible() {
        // expect: set visible throws an exception.
        DialogParameters parameters = new DialogParameters();
        parameters.setVisible(true);
    }
    
}
