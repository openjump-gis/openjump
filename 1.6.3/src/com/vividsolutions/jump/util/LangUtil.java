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
package com.vividsolutions.jump.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.util.Assert;

/**
 * Utilities to support the Java language.
 */
public class LangUtil {
    private static Map primitiveToWrapperMap = new HashMap() {

        {
            put(byte.class, Byte.class);
            put(char.class, Character.class);
            put(short.class, Short.class);
            put(int.class, Integer.class);
            put(long.class, Long.class);
            put(float.class, Float.class);
            put(double.class, Double.class);
            put(boolean.class, Boolean.class);
        }
    };

    public static String emptyStringIfNull(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Useful because an expression used to generate o need only be
     * evaluated once.
     */
    public static Object ifNull(Object o, Object alternative) {
        return (o == null) ? alternative : o;
    }

    public static Object ifNotNull(Object o, Object alternative) {
        return (o != null) ? alternative : o;
    }

    public static Class toPrimitiveWrapperClass(Class primitiveClass) {
        return (Class) primitiveToWrapperMap.get(primitiveClass);
    }

    public static boolean isPrimitive(Class c) {
        return primitiveToWrapperMap.containsKey(c);
    }

    public static boolean bothNullOrEqual(Object a, Object b) {
        return (a == null && b == null) || (a != null && b != null && a.equals(b));
    }

    public static Object newInstance(Class c) {
        try {
            return c.newInstance();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
            return null;
        }
    }

    public static Collection classesAndInterfaces(Class c) {
        ArrayList classesAndInterfaces = new ArrayList();
        classesAndInterfaces.add(c);
        superclasses(c, classesAndInterfaces);
        for (Iterator i = new ArrayList(classesAndInterfaces).iterator(); i.hasNext(); ) {
            Class x = (Class) i.next();
            classesAndInterfaces.addAll(Arrays.asList(x.getInterfaces()));
        }
        return classesAndInterfaces;
    }

    private static void superclasses(Class c, Collection results) {
        if (c.getSuperclass() == null) {
            return;
        }
        results.add(c.getSuperclass());
        superclasses(c.getSuperclass(), results);
    }

}
