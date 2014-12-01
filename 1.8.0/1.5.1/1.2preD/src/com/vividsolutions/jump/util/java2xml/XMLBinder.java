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
package com.vividsolutions.jump.util.java2xml;

import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.StringUtil;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;

import org.jdom.input.SAXBuilder;

import java.awt.Color;
import java.awt.Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


//I wrote Java2XML and XML2Java because I couldn't get Betwixt to do Collections.
//Java2XML and XML2Java are very easy to setup, are easier to comprehend, and
//have better error reporting. [Jon Aquino]
public class XMLBinder {
    private HashMap classToCustomConverterMap = new HashMap();

    public XMLBinder() {
        classToCustomConverterMap.put(Class.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    try {
                        return Class.forName(value);
                    } catch (ClassNotFoundException e) {
                        Assert.shouldNeverReachHere();

                        return null;
                    }
                }

                public String toXML(Object object) {
                    return ((Class) object).getName();
                }
            });
        classToCustomConverterMap.put(Color.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    List parameters = StringUtil.fromCommaDelimitedString(value);

                    return new Color(Integer.parseInt(
                            (String) parameters.get(0)),
                        Integer.parseInt((String) parameters.get(1)),
                        Integer.parseInt((String) parameters.get(2)),
                        Integer.parseInt((String) parameters.get(3)));
                }

                public String toXML(Object object) {
                    Color color = (Color) object;
                    ArrayList parameters = new ArrayList();
                    parameters.add(new Integer(color.getRed()));
                    parameters.add(new Integer(color.getGreen()));
                    parameters.add(new Integer(color.getBlue()));
                    parameters.add(new Integer(color.getAlpha()));

                    return StringUtil.toCommaDelimitedString(parameters);
                }
            });
        classToCustomConverterMap.put(Font.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    List parameters = StringUtil.fromCommaDelimitedString(value);

                    return new Font((String) parameters.get(0),
                        Integer.parseInt((String) parameters.get(1)),
                        Integer.parseInt((String) parameters.get(2)));
                }

                public String toXML(Object object) {
                    Font font = (Font) object;
                    ArrayList parameters = new ArrayList();
                    parameters.add(font.getName());
                    parameters.add(new Integer(font.getStyle()));
                    parameters.add(new Integer(font.getSize()));

                    return StringUtil.toCommaDelimitedString(parameters);
                }
            });
        classToCustomConverterMap.put(double.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Double(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(Double.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Double(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(int.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Integer(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(Integer.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Integer(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
            //not fixed in original jump
        classToCustomConverterMap.put(Long.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return new Long(value);
                    }

                    public String toXML(Object object) {
                        return object.toString();
                    }
                });        
        classToCustomConverterMap.put(String.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return value;
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(boolean.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Boolean(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(Boolean.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    return new Boolean(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(File.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return new File(value);
                    }

                    public String toXML(Object object) {
                        return object.toString();
                    }
                });
    }

    private String specFilename(Class c) {
        return StringUtil.classNameWithoutPackageQualifiers(c.getName()) +
        ".java2xml";
    }

    protected List specElements(Class c)
        throws XMLBinderException, JDOMException, IOException {
        InputStream stream = specResourceStream(c);

        if (stream == null) {
            throw new XMLBinderException("Could not find java2xml file for " +
                c.getName() + " or its interfaces or superclasses");
        }

        try {
            Element root = new SAXBuilder().build(stream).getRootElement();

            if (!root.getAttributes().isEmpty()) {
                throw new XMLBinderException("Root element of " +
                    specFilename(c) + " should not have attributes");
            }

            if (!root.getName().equals("root")) {
                throw new XMLBinderException("Root element of " +
                    specFilename(c) + " should be named 'root'");
            }

            return root.getChildren();
        } finally {
            stream.close();
        }
    }

    private InputStream specResourceStream(Class c) {
        for (Iterator i = LangUtil.classesAndInterfaces(c).iterator();
                i.hasNext();) {
            Class type = (Class) i.next();
            Assert.isTrue(type.isAssignableFrom(c));

            InputStream stream = type.getResourceAsStream(specFilename(type));

            if (stream != null) {
                return stream;
            }
        }

        return null;
    }

    public void addCustomConverter(Class c, CustomConverter converter) {
        classToCustomConverterMap.put(c, converter);
    }

    /**
     * @param c for error messages
     */
    protected void visit(List specElements, SpecVisitor visitor, Class c)
        throws Exception {
        for (Iterator i = specElements.iterator(); i.hasNext();) {
            Element specElement = (Element) i.next();
            Attribute xmlName = specElement.getAttribute("xml-name");

            if (xmlName == null) {
                throw new XMLBinderException(StringUtil.classNameWithoutPackageQualifiers(
                        c.getName()) + ": Expected 'xml-name' attribute in <" +
                    specElement.getName() + "> but found none");
            }

            Attribute javaName = specElement.getAttribute("java-name");

            //javaName is null if tag does nothing other than add a level to the
            //hierarchy [Jon Aquino]
            if (specElement.getName().equals("element")) {
                visitor.tagSpecFound(xmlName.getValue(),
                    (javaName != null) ? javaName.getValue() : null,
                    specElement.getChildren());
            }

            if (specElement.getName().equals("attribute")) {
                visitor.attributeSpecFound(xmlName.getValue(),
                    javaName.getValue());
            }
        }
    }

    public Object toJava(String text, Class c) {
        return (!text.equals("null"))
        ? ((CustomConverter) classToCustomConverterMap.get(customConvertableClass(
                c))).toJava(text) : null;
    }

    protected boolean specifyingTypeExplicitly(Class c)
        throws XMLBinderException {
        //The int and double classes are abstract. Filter them out. [Jon Aquino]
        if (hasCustomConverter(c)) {
            return false;
        }

        //In the handling of Maps, c may be the Object class. [Jon Aquino]
        return (c == Object.class) || Modifier.isAbstract(c.getModifiers()) ||
        c.isInterface();
    }

    protected Class fieldClass(Method setter) {
        Assert.isTrue(setter.getParameterTypes().length == 1);

        return setter.getParameterTypes()[0];
    }

    public Method setter(Class c, String field) throws XMLBinderException {
        Method[] methods = c.getMethods();

        //Exact match first [Jon Aquino]
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().toUpperCase().equals("SET" +
                        field.toUpperCase()) &&
                    !methods[i].getName().toUpperCase().equals("ADD" +
                        field.toUpperCase())) {
                continue;
            }

            if (methods[i].getParameterTypes().length != 1) {
                continue;
            }

            return methods[i];
        }

        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().toUpperCase().startsWith("SET" +
                        field.toUpperCase()) &&
                    !methods[i].getName().toUpperCase().startsWith("ADD" +
                        field.toUpperCase())) {
                continue;
            }

            if (methods[i].getParameterTypes().length != 1) {
                continue;
            }

            return methods[i];
        }

        throw new XMLBinderException("Could not find setter named like '" +
            field + "' in class " + c);
    }

    protected String toXML(Object object) {
        return ((CustomConverter) classToCustomConverterMap.get(customConvertableClass(
                object.getClass()))).toXML(object);
    }

    protected boolean hasCustomConverter(Class fieldClass) {
        return customConvertableClass(fieldClass) != null;
    }

    /**
     * @return null if c doesn't have a custom converter
     */
    private Class customConvertableClass(Class c) {
        //Use #isAssignableFrom rather than #contains because some classes
        //may be interfaces. [Jon Aquino]
        for (Iterator i = classToCustomConverterMap.keySet().iterator();
                i.hasNext();) {
            Class customConvertableClass = (Class) i.next();

            if (customConvertableClass.isAssignableFrom(c)) {
                return customConvertableClass;
            }
        }

        return null;
    }

    protected interface SpecVisitor {
        public void tagSpecFound(String xmlName, String javaName,
            List specChildElements) throws Exception;

        public void attributeSpecFound(String xmlName, String javaName)
            throws Exception;
    }

    /**
     * Sometimes you need to use a CustomConverter rather than a .java2xml
     * file i.e. when the class is from a third party (e.g. a Swing class) and you
     * can't add a .java2xml file to the jar.
     */
    public interface CustomConverter {
        public Object toJava(String value);

        public String toXML(Object object);
    }

    public static class XMLBinderException extends Exception {
        public XMLBinderException(String message) {
            super(message);
        }
    }

}
