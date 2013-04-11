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
import static org.openjump.test.ReflectionUtils.privateField;
import static org.openjump.test.ReflectionUtils.privateStaticField;

import org.junit.Test;

/**
 * @author Benjamin Gudehus
 */
public class ReflectionUtilsTest {

    //-----------------------------------------------------------------------------------
    // TEST CASES.
    //-----------------------------------------------------------------------------------
    
    @Test
    public void testGetPrivateField() throws Exception {
        // expect: "private field"
        PrivateClass object = new PrivateClass();
        assertEquals("foo", privateField(object, "privateField"));
    }
    
    @Test(expected=NoSuchFieldException.class)
    public void testGetPrivateFieldException() throws Exception {
        // expect: "private field"
        PrivateClass object = new PrivateClass();
        privateField(object, "invalid");
    }
    
    @Test
    public void testSetPrivateField() throws Exception {
        // expect: "private field"
        PrivateClass object = new PrivateClass();
        privateField(object, "privateField", "bar");
        assertEquals("bar", privateField(object, "privateField"));
    }
    
    @Test
    public void testGetPrivateStaticField() throws Exception {
        // expect: "private static field"
        Class<PrivateClass> cls = PrivateClass.class;
        assertEquals("foo", privateStaticField(cls, "privateStaticField"));
    }
    
    @Test(expected=NoSuchFieldException.class)
    public void testGetPrivateStaticFieldException() throws Exception {
        // expect: "private static field"
        Class<PrivateClass> cls = PrivateClass.class;
        privateStaticField(cls, "invalid");
    }
    
    @Test
    public void testSetPrivateStaticField() throws Exception {
        // expect: "private static field"
        Class<PrivateClass> cls = PrivateClass.class;
        privateStaticField(cls, "privateStaticField", "bar");
        assertEquals("bar", privateStaticField(cls, "privateStaticField"));
    }
    
    //-----------------------------------------------------------------------------------
    // TEST FIXTURES.
    //-----------------------------------------------------------------------------------
    
    public static class PrivateClass {
        @SuppressWarnings("unused")
        private String privateField = "foo";
        
        @SuppressWarnings("unused")
        private static String privateStaticField = "foo";
    }
    
}
