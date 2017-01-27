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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.StringUtil;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import org.jdom2.input.SAXBuilder;

import java.awt.Color;
import java.awt.Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


//I wrote Java2XML and XML2Java because I couldn't get Betwixt to do Collections.
//Java2XML and XML2Java are very easy to setup, are easier to comprehend, and
//have better error reporting. [Jon Aquino]
public class XMLBinder {

    private static final WKTReader WKT_READER = new com.vividsolutions.jts.io.WKTReader();
    private static final WKTWriter WKT_WRITER = new com.vividsolutions.jts.io.WKTWriter();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private HashMap<Class,CustomConverter> classToCustomConverterMap = new HashMap<>();

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
                    List<String> parameters = StringUtil.fromCommaDelimitedString(value);

                    return new Color(
                        Integer.parseInt(parameters.get(0)),
                        Integer.parseInt(parameters.get(1)),
                        Integer.parseInt(parameters.get(2)),
                        Integer.parseInt(parameters.get(3)));
                }

                public String toXML(Object object) {
                    Color color = (Color) object;
                    List<Object> parameters = new ArrayList<>();
                    parameters.add(color.getRed());
                    parameters.add(color.getGreen());
                    parameters.add(color.getBlue());
                    parameters.add(color.getAlpha());

                    return StringUtil.toCommaDelimitedString(parameters);
                }
            });
        classToCustomConverterMap.put(Font.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    List<String> parameters = StringUtil.fromCommaDelimitedString(value);

                    return new Font(parameters.get(0),
                        Integer.parseInt(parameters.get(1)),
                        Integer.parseInt(parameters.get(2)));
                }

                public String toXML(Object object) {
                    Font font = (Font) object;
                    List<Object> parameters = new ArrayList<>();
                    parameters.add(font.getName());
                    parameters.add(font.getStyle());
                    parameters.add(font.getSize());

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
                    return Boolean.parseBoolean(value);
                }

                public String toXML(Object object) {
                    return object.toString();
                }
            });
        classToCustomConverterMap.put(Date.class,
            new CustomConverter() {
                public Object toJava(String value) {
                    try {
                        return DATE_FORMAT.parse(value);
                    } catch(java.text.ParseException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                public String toXML(Object object) {
                    return DATE_FORMAT.format((Date)object);
                }
            });
        classToCustomConverterMap.put(Boolean.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return Boolean.parseBoolean(value);
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
        classToCustomConverterMap.put(Character.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return value.length()>0?value.charAt(0):'\u0000';
                    }

                    public String toXML(Object object) {
                        return object.toString();
                    }
                });
        classToCustomConverterMap.put(Charset.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return Charset.forName(value);
                    }

                    public String toXML(Object object) {
                        return ((Charset)object).name();
                    }
                });
        classToCustomConverterMap.put(Pattern.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return Pattern.compile(value);
                    }

                    public String toXML(Object object) {
                        return object.toString();
                    }
                });
        classToCustomConverterMap.put(AttributeType.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        return AttributeType.toAttributeType(value);
                    }

                    public String toXML(Object object) {
                        return object.toString();
                    }
                });
        classToCustomConverterMap.put(Geometry.class,
                new CustomConverter() {
                    public Object toJava(String value) {
                        try {
                            return WKT_READER.read(value);
                        }
                        catch(com.vividsolutions.jts.io.ParseException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    public String toXML(Object object) {
                        return WKT_WRITER.write((Geometry)object);
                    }
                });
    }

    private String specFilename(Class c) {
        return StringUtil.classNameWithoutPackageQualifiers(c.getName()) +
        ".java2xml";
    }

    protected List<Element> specElements(Class c)
                throws XMLBinderException, JDOMException, IOException {

        try (InputStream stream = specResourceStream(c)) {

            if (stream == null) {
                throw new XMLBinderException("Could not find java2xml file for " +
                        c.getName() + " or its interfaces or superclasses");
            }

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
        }
    }

    private InputStream specResourceStream(Class c) {
        for (Class<?> clazz : LangUtil.classesAndInterfaces(c)) {
            Assert.isTrue(clazz.isAssignableFrom(c));

            InputStream stream = clazz.getResourceAsStream(specFilename(clazz));

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
    protected void visit(List<Element> specElements, SpecVisitor visitor, Class c)
        throws Exception {
        for (Element specElement : specElements) {
            Attribute xmlName = specElement.getAttribute("xml-name");

            if (xmlName == null) {
                throw new XMLBinderException(StringUtil.classNameWithoutPackageQualifiers(
                        c.getName()) + ": Expected 'xml-name' attribute in <" +
                    specElement.getName() + "> but found none");
            }

            Attribute javaName = specElement.getAttribute("java-name");
            String attributeValue = javaName == null ? null : javaName.getValue();
            //javaName is null if tag does nothing other than add a level to the
            //hierarchy [Jon Aquino]
            if (specElement.getName().equals("element")) {
                visitor.tagSpecFound(xmlName.getValue(), attributeValue,
                    specElement.getChildren());
            }

            if (specElement.getName().equals("attribute")) {
                visitor.attributeSpecFound(xmlName.getValue(), attributeValue);
            }
        }
    }

    public Object toJava(String text, Class c) {
        return (!text.equals("null")) ?
                classToCustomConverterMap.get(customConvertableClass(c)).toJava(text) : null;
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
        for (Method method : methods) {
            if (!method.getName().toUpperCase().equals("SET" +
                        field.toUpperCase()) &&
                    !method.getName().toUpperCase().equals("ADD" +
                        field.toUpperCase())) {
                continue;
            }

            if (method.getParameterTypes().length != 1) {
                continue;
            }

            return method;
        }

        for (Method method : methods) {
            if (!method.getName().toUpperCase().startsWith("SET" +
                        field.toUpperCase()) &&
                    !method.getName().toUpperCase().startsWith("ADD" +
                        field.toUpperCase())) {
                continue;
            }

            if (method.getParameterTypes().length != 1) {
                continue;
            }

            return method;
        }

        throw new XMLBinderException("Could not find setter named like '" +
            field + "' in class " + c);
    }

    protected String toXML(Object object) {
        return classToCustomConverterMap.get(
                customConvertableClass(object.getClass())
        ).toXML(object);
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
        for (Class<?> customConvertableClass : classToCustomConverterMap.keySet()) {
            if (customConvertableClass.isAssignableFrom(c)) {
                return customConvertableClass;
            }
        }

        return null;
    }

    protected interface SpecVisitor {
        void tagSpecFound(String xmlName, String javaName,
            List<Element> specChildElements) throws Exception;

        void attributeSpecFound(String xmlName, String javaName)
            throws Exception;
    }

    /**
     * Sometimes you need to use a CustomConverter rather than a .java2xml
     * file i.e. when the class is from a third party (e.g. a Swing class) and you
     * can't add a .java2xml file to the jar.
     */
    public interface CustomConverter {
        Object toJava(String value);

        String toXML(Object object);
    }

    public static class XMLBinderException extends Exception {
        public XMLBinderException(String message) {
            super(message);
        }
    }

}
