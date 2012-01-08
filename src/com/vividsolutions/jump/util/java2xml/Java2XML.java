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
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
public class Java2XML extends XMLBinder {
	private static Logger LOG = Logger.getLogger(Java2XML.class);
    public Java2XML() {
    }
    public String write(Object object, String rootTagName) throws Exception {
        StringWriter writer = new StringWriter();
        try {
            write(object, rootTagName, writer);
            return writer.toString();
        } finally {
            writer.close();
        }
    }
    public void write(Object object, String rootTagName, File file)
            throws Exception {
        FileWriter fileWriter = new FileWriter(file, false);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            try {
                new Java2XML().write(object, rootTagName, bufferedWriter);
                bufferedWriter.flush();
                fileWriter.flush();
            } finally {
                bufferedWriter.close();
            }
        } finally {
            fileWriter.close();
        }
    }
    public void write(Object object, String rootTagName, Writer writer)
            throws Exception {
        Document document = new Document(new Element(rootTagName));
        write(object, document.getRootElement(),
                specElements(object.getClass()));
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setNewlines(true);
        xmlOutputter.setIndent(true);
        xmlOutputter.output(document, writer);
    }
    private void write(final Object object, final Element tag, List specElements)
            throws Exception {
        try {
            visit(specElements, new SpecVisitor() {
                public void tagSpecFound(String xmlName, String javaName,
                        List specChildElements) throws Exception {
                    Collection childTags = new ArrayList();
                    if (javaName != null) {
                        childTags.addAll(writeChildTags(tag, xmlName, getter(
                                object.getClass(), javaName).invoke(object,
                                new Object[]{}),
                                specifyingTypeExplicitly(fieldClass(setter(
                                        object.getClass(), javaName)))));
                    } else {
                        Element childTag = new Element(xmlName);
                        tag.addContent(childTag);
                        childTags.add(childTag);
                    }
                    // The parent may specify additional tags for itself in the
                    // children. [Jon Aquino]
                    for (Iterator i = childTags.iterator(); i.hasNext();) {
                        Element childTag = (Element) i.next();
                        write(object, childTag, specChildElements);
                    }
                }
                public void attributeSpecFound(String xmlName, String javaName)
                        throws Exception {
                    writeAttribute(tag, xmlName, getter(object.getClass(),
                            javaName).invoke(object, new Object[]{}));
                }
            }, object.getClass());
        } catch (Exception e) {
        	LOG.error("Java2XML: Exception writing "
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
            for (Iterator i = ((Map) value).keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Element mappingTag = new Element("mapping");
                childTag.addContent(mappingTag);
                writeChildTag(mappingTag, "key", key, true);
                writeChildTag(mappingTag, "value", ((Map) value).get(key), true);
            }
        } else if (value instanceof Collection) {
            for (Iterator i = ((Collection) value).iterator(); i.hasNext();) {
                Object item = i.next();
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
    private Collection writeChildTags(Element tag, String name, Object value,
            boolean specifyingType) throws Exception {
        ArrayList childTags = new ArrayList();
        if (value instanceof Collection) {
            // Might or might not need to specify type, depending on how
            // concrete the setter's parameter is. [Jon Aquino]
            for (Iterator i = ((Collection) value).iterator(); i.hasNext();) {
                Object item = i.next();
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
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().toUpperCase().equals(
                    "GET" + field.toUpperCase())
                    && !methods[i].getName().toUpperCase().equals(
                            "IS" + field.toUpperCase())) {
                continue;
            }
            if (methods[i].getParameterTypes().length != 0) {
                continue;
            }
            return methods[i];
        }
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().toUpperCase().startsWith(
                    "GET" + field.toUpperCase())
                    && !methods[i].getName().toUpperCase().startsWith(
                            "IS" + field.toUpperCase())) {
                continue;
            }
            if (methods[i].getParameterTypes().length != 0) {
                continue;
            }
            return methods[i];
        }
        throw new XMLBinderException("Could not find getter named like '"
                + field + "' " + fieldClass);
    }
}