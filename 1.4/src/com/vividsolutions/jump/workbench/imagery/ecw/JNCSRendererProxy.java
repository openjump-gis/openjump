package com.vividsolutions.jump.workbench.imagery.ecw;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2009 Intevation GmbH
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
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.awt.Point;

import java.awt.geom.Point2D;

/**
 * The purpose of this proxy class is to defer linking to
 * the proprietary ECW ER Mapper library till runtime. So it's
 * not necessary to ship the library and/or its Java wrapper
 * in binary distributions of OpenJUMP.
 *
 * @author Sascha L. Teichmann (sascha.teichmann@intevation.de)
 */

public class JNCSRendererProxy
{
    public static final String RENDERER_CLASS =
        System.getProperty("ecw.java.wrapper", "com.ermapper.ecw.JNCSRenderer");

    protected Object renderer;

    public JNCSRendererProxy(String location, boolean flag) throws Exception
    {
        Class clazz = Class.forName(RENDERER_CLASS);
        Constructor constructor = clazz.getConstructor(
            new Class [] { String.class, boolean.class });

        try {
            renderer = constructor.newInstance(
                new Object [] { location, flag ? Boolean.TRUE : Boolean.FALSE });
        }
        catch (InvocationTargetException ite) {
            throwAsException(ite);
        }
    }

    public static final void throwAsException(InvocationTargetException ite) 
    throws Exception 
    {
        Throwable t = ite.getTargetException();
        throw t instanceof Exception
            ? (Exception)t
            : (t != null ? new Exception(t.getMessage()) : ite);
    }

    public static final int getInt(Object object, String fieldname) {
        try {
            Field field = object.getClass().getField(fieldname);
            return field.getInt(object);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int getInt(String fieldname) {
        return getInt(renderer, fieldname);
    }

    public static final double getDouble(Object object, String fieldname) {
        try {
            Field field = object.getClass().getField(fieldname);
            return field.getDouble(object);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public double getDouble(String fieldname) {
        return getDouble(renderer, fieldname);
    }

    public double getOriginX() {
        return getDouble("originX");
    }

    public double getOriginY() {
        return getDouble("originY");
    }

    public int getWidth() {
        return getInt("width");
    }

    public int getHeight() {
        return getInt("height");
    }

    public double getCellIncrementX() {
        return getDouble("cellIncrementX");
    }

    public double getCellIncrementY() {
        return getDouble("cellIncrementY");
    }

    public int getNumBands() {
        return getInt("numBands");
    }

    public Point convertWorldToDataset(double x, double y) 
    throws Exception
    {
        try {
            Method method = renderer.getClass()
                .getMethod("convertWorldToDataset", 
                    new Class [] { double.class, double.class });

            Object datasetPoint = // JNCSDatasetPoint
                method.invoke(
                    renderer,
                    new Object [] { Double.valueOf(x), Double.valueOf(y) });

            return new Point(
                getInt(datasetPoint, "x"), getInt(datasetPoint, "y"));
        }
        catch (InvocationTargetException ite) {
            throwAsException(ite);
        }

        throw new RuntimeException("Never reached!");
    }

    public Point2D.Double convertDatasetToWorld(int x, int y) 
    throws Exception
    {
        try {
            Method method = renderer.getClass()
                .getMethod("convertDatasetToWorld", 
                    new Class [] { int.class, int.class });

            Object worldPoint = // JNCSWorldPoint
                method.invoke(
                    renderer,
                    new Object [] { Integer.valueOf(x), Integer.valueOf(y) });

            return new Point2D.Double(
                getDouble(worldPoint, "x"), 
                getDouble(worldPoint, "y"));
        }
        catch (InvocationTargetException ite) {
            throwAsException(ite);
        }

        throw new RuntimeException("Never reached!");
    }

    public void setView(
        int numBands,    int [] bands,
        int firstColumn, int firstLine,
        int lastColumn,  int lastLine,
        int width,       int height
    ) 
    throws Exception
    {
        try {
            Method method = renderer.getClass()
                .getMethod("setView", 
                    new Class [] { 
                        int.class,    // numBands
                        int [].class, // bands
                        int.class,    // firstColumn
                        int.class,    // firstLine
                        int.class,    // lastColumn
                        int.class,    // lastLine
                        int.class,    // width
                        int.class }); // height

                method.invoke(
                    renderer,
                    new Object [] { 
                        Integer.valueOf(numBands), 
                        bands,
                        Integer.valueOf(firstColumn),
                        Integer.valueOf(firstLine),
                        Integer.valueOf(lastColumn),
                        Integer.valueOf(lastLine),
                        Integer.valueOf(width),
                        Integer.valueOf(height)});
        }
        catch (InvocationTargetException ite) {
            throwAsException(ite);
        }
    }

    public void readLineRGBA(int [] rgba) 
    throws Exception
    {
        try {
            Method method = renderer.getClass()
                .getMethod("readLineRGBA", 
                    new Class [] { int [].class });

            method.invoke(renderer, new Object [] { rgba });
        }
        catch (InvocationTargetException ite) {
            throwAsException(ite);
        }
    }

    public void close(boolean flag) {
        try {
            Method method = renderer.getClass()
                .getMethod("close", 
                    new Class [] { boolean.class });

            method.invoke(
                renderer,
                new Object [] { flag ? Boolean.TRUE : Boolean.FALSE });
        }
        catch (Exception e) {
            /* ignore me! */
        }
    }
}
// end of file
