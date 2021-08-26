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

import java.lang.reflect.Field;

/**
 * @author Benjamin Gudehus
 */
public final class ReflectionUtils {
    
    //-----------------------------------------------------------------------------------
    // CONSTRUCTORS.
    //-----------------------------------------------------------------------------------
    
    private ReflectionUtils() {
        throw new UnsupportedOperationException();
    }
    
    //-----------------------------------------------------------------------------------
    // STATIC METHODS.
    //-----------------------------------------------------------------------------------

    public static Object privateField(Object obj, String name) 
            throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }
    
    public static void privateField(Object obj, String name, Object value) 
            throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(obj, value);
    }
    
    public static Object privateStaticField(Class<?> cls, String name) 
            throws Exception {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(cls);
    }
    
    public static void privateStaticField(Class<?> cls, String name, Object value) 
            throws Exception {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        field.set(cls, value);
    }
    
}
