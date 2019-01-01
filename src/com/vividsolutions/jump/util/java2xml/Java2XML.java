/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.util.java2xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.vividsolutions.jump.workbench.Logger;

public class Java2XML extends XMLBinder {

    public Java2XML() {
    }

    public String write(Object object, String rootTagName) throws Exception {
        try (StringWriter writer = new StringWriter()) {
            write(object, rootTagName, writer);
            return writer.toString();
        }
    }

    public void write(Object object, String rootTagName, File file)
            throws Exception {
        try (FileWriter fileWriter = new FileWriter(file, false);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            new Java2XML().write(object, rootTagName, bufferedWriter);
            bufferedWriter.flush();
            fileWriter.flush();
        }
    }

    public void write(Object object, String rootTagName, Writer writer)
            throws Exception {
        Document document = new Document(new Element(rootTagName));
        write(object, document.getRootElement(), specElements(object.getClass()));
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
        xmlOutputter.output(document, writer);
    }

    private void write(final Object object, final Element tag, List<Element> specElements)
            throws Exception {
        try {
            visit(specElements, new SpecVisitor() {
                public void tagSpecFound(String xmlName, String javaName,
                        List<Element> specChildElements) throws Exception {
                    Collection<Element> childTags = new ArrayList<>();
                    if (javaName != null) {
                        childTags.addAll(writeChildTags(tag, xmlName, getter(
                                object.getClass(), javaName).invoke(object),
                                specifyingTypeExplicitly(fieldClass(setter(
                                        object.getClass(), javaName)))));
                    } else {
                        Element childTag = new Element(xmlName);
                        tag.addContent(childTag);
                        childTags.add(childTag);
                    }
                    // The parent may specify additional tags for itself in the
                    // children. [Jon Aquino]
                    for (Element childTag : childTags) {
                        write(object, childTag, specChildElements);
                    }
                }
                public void attributeSpecFound(String xmlName, String javaName)
                        throws Exception {
                    writeAttribute(tag, xmlName, getter(object.getClass(),
                            javaName).invoke(object));
                }
            }, object.getClass());
        } catch (Exception e) {
        	Logger.error("Java2XML: Exception writing "
                    + object.getClass());
            throw e;
        }
    }
    private void writeAttribute(Element tag, String name, Object value)
            throws XMLBinderException {
        if (value == null) {
            throw new XMLBinderException("Cannot store null value as "
                    + "attribute. Store as element instead. (" + name + ").");
        }
        tag.setAttribute(new Attribute(name, toXML(value)));
    }
    private Element writeChildTag(Element tag, String name, Object value,
            boolean specifyingType) throws Exception {
        Element childTag = new Element(name);
        if ((value != null) && specifyingType) {
            childTag.setAttribute(new Attribute("class", value.getClass()
                    .getName()));
        }
        if (value == null) {
            childTag.setAttribute(new Attribute("null", "true"));
        } else if (hasCustomConverter(value.getClass())) {
            childTag.setText(toXML(value));
        } else if (value instanceof Map) {
            for (Object key : ((Map)value).keySet()) {
                Element mappingTag = new Element("mapping");
                childTag.addContent(mappingTag);
                writeChildTag(mappingTag, "key", key, true);
                writeChildTag(mappingTag, "value", ((Map) value).get(key), true);
            }
        } else if (value instanceof Collection) {
            for (Object item : (Collection)value) {
                writeChildTag(childTag, "item", item, true);
            }
        } else if (value instanceof QName) {
            childTag.addContent(value.toString());
        } else {
            write(value, childTag, specElements(value.getClass()));
        }
        tag.addContent(childTag);
        return childTag;
    }

    private Collection<Element> writeChildTags(Element tag, String name, Object value,
            boolean specifyingType) throws Exception {

        ArrayList<Element> childTags = new ArrayList<>();
        if (value instanceof Collection) {
            // Might or might not need to specify type, depending on how
            // concrete the setter's parameter is. [Jon Aquino]
            for (Object item : (Collection)value) {
                childTags.add(writeChildTag(tag, name, item, specifyingType));
            }
        } else {
            childTags.add(writeChildTag(tag, name, value, specifyingType));
        }
        return childTags;
    }

    private Method getter(Class fieldClass, String field)
            throws XMLBinderException {
        Method[] methods = fieldClass.getMethods();
        // Exact match first [Jon Aquino]
        for (Method method : methods) {
            if (!method.getName().toUpperCase().equals(
                    "GET" + field.toUpperCase())
                    && !method.getName().toUpperCase().equals(
                            "IS" + field.toUpperCase())) {
                continue;
            }
            if (method.getParameterTypes().length != 0) {
                continue;
            }
            return method;
        }
        for (Method method : methods) {
            if (!method.getName().toUpperCase().startsWith(
                    "GET" + field.toUpperCase())
                    && !method.getName().toUpperCase().startsWith(
                            "IS" + field.toUpperCase())) {
                continue;
            }
            if (method.getParameterTypes().length != 0) {
                continue;
            }
            return method;
        }
        throw new XMLBinderException("Could not find getter named like '"
                + field + "' " + fieldClass);
    }
}