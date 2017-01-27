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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.Logger;
public class XML2Java extends XMLBinder {
    private ArrayList listeners = new ArrayList();
    private ClassLoader classLoader = getClass().getClassLoader();

    public XML2Java() {
    }
    public XML2Java(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    public Object read(String xml, Class c) throws Exception {
        StringReader reader = new StringReader(xml);
        try {
            return read(reader, c);
        } finally {
            reader.close();
        }
    }
    public Object read(Reader reader, Class c) throws Exception {
        return read(new SAXBuilder().build(reader).getRootElement(), c);
    }
    public Object read(InputStream inputStream, Class c) throws Exception {
        return read(new SAXBuilder().build(inputStream).getRootElement(), c);
    }
    public Object read(File file, Class c) throws Exception {
        //FileReader fileReader = new FileReader(file);
        BufferedInputStream bis = null;
        try {
            //BufferedReader bufferedReader = new BufferedReader(fileReader);
            bis = new BufferedInputStream(new FileInputStream(file));
            try {
                return new XML2Java().read(bis, c);
            } finally {
                bis.close();
            }
        } finally {
            bis.close();
        }
    }
    private void read(final Element tag, final Object object, List specElements)
            throws Exception {
        Assert.isTrue(tag != null);
        visit(specElements, new SpecVisitor() {
            private void fillerTagSpecFound(String xmlName,
                    List specChildElements) throws Exception {
                if (tag.getChildren(xmlName).size() == 0) {
                	System.err.println("WARNING: Expected 1 <" + xmlName + "> tag but found None");
                	return;
                }
                if (tag.getChildren(xmlName).size() != 1) {
                    throw new XMLBinderException("Expected 1 <" + xmlName
                            + "> tag but found "
                            + tag.getChildren(xmlName).size());
                }
                read(tag.getChild(xmlName), object, specChildElements);
            }
            private void normalTagSpecFound(String xmlName, String javaName,
                    List specChildElements) throws Exception {
                try {
                    setValuesFromTags(object, setter(object.getClass(), javaName),
                        tag.getChildren(xmlName));
                    //The parent may specify additional tags for itself in the
                    // children. [Jon Aquino]
                    for (Iterator i = tag.getChildren(xmlName).iterator(); i.hasNext();) {
                        Element childTag = (Element) i.next();
                        read(childTag, object, specChildElements);
                    }

                } catch(ClassNotFoundException e) {
                    // Here, we don't throw e so that the parsing process can go on
                    //throw e;
                }
            }
            public void tagSpecFound(String xmlName, String javaName,
                    List specChildElements) throws Exception {
                if (javaName == null) {
                    fillerTagSpecFound(xmlName, specChildElements);
                } else {
                    normalTagSpecFound(xmlName, javaName, specChildElements);
                }
            }
            public void attributeSpecFound(String xmlName, String javaName)
                    throws Exception {
                if (tag.getAttribute(xmlName) == null) {
                    String msg = ("Expected '"
                            + xmlName
                            + "' attribute but found none. Tag = "
                            + tag.getName()
                            + "; Attributes = "
                            + StringUtil.toCommaDelimitedString(tag
                                    .getAttributes()));
                    // [sstein 5April2008] replaced XMLB exception by Log
                    // so when a problem with styling appears data are still loaded
                    if (tag.getName().equalsIgnoreCase("style")){
                    	Logger.warn(msg);
                    	System.out.println(msg);
                    	return; //return to avoid further messages
                    }
                    // [mmichaud 2011-07-04] Following attributes were introduced in 1.4.1 release.
                    // Skip them to keep compatibility with old project files
                    if (tag.getName().equalsIgnoreCase("layer") && xmlName != null &&
                        (xmlName.equalsIgnoreCase("editable")   || 
                         xmlName.equalsIgnoreCase("selectable") ||
                         xmlName.equalsIgnoreCase("read-only") ) ) {
                    	Logger.warn(msg);
                    	System.out.println(msg);
                    	return; //return to avoid further messages
                    }
                    else{
                    	throw new XMLBinderException(msg);
                    }
                }
                Method setter = setter(object.getClass(), javaName);
                setValue(object, setter, toJava(tag.getAttribute(xmlName)
                        .getValue(), setter.getParameterTypes()[0]));
            }
        }, object.getClass());
    }
    private Object read(Element tag, Class c) throws Exception {
        if (tag.getAttribute("null") != null
                && tag.getAttributeValue("null").equals("true")) {
            return null;
        }
        if (c == QName.class) {
          return QName.valueOf(tag.getTextTrim());
        }
        if (specifyingTypeExplicitly(c)) {
            if (tag.getAttribute("class") == null) {
                throw new XMLBinderException("Expected <" + tag.getName()
                        + "> to have 'class' attribute but found none");
            }
            try {
                return read(tag, Class.forName(tag.getAttributeValue("class"), true, classLoader));
            } catch (ClassNotFoundException e) {
                Logger.error("Could not find class for " + tag, e);
                System.out.println("Class not found for tag " + tag.getName() + ": " + tag.getAttribute("class").getValue());
                throw e;
            }
        }
        fireCreatingObject(c);
        if (hasCustomConverter(c)) {
            return toJava(tag.getTextTrim(), c);
        }
        Object object = c.newInstance();
        if (object instanceof Map) {
            for (Iterator i = tag.getChildren().iterator(); i.hasNext();) {
                Element mappingTag = (Element) i.next();
                if (!mappingTag.getName().equals("mapping")) {
                    throw new XMLBinderException("Expected <" + tag.getName()
                            + "> to have <mapping> tag but found none");
                }
                if (mappingTag.getChildren().size() != 2) {
                    throw new XMLBinderException("Expected <" + tag.getName()
                            + "> to have 2 tags under <mapping> but found "
                            + mappingTag.getChildren().size());
                }
                if (mappingTag.getChildren("key").size() != 1) {
                    throw new XMLBinderException(
                            "Expected <"
                                    + tag.getName()
                                    + "> to have 1 <key> tag under <mapping> but found "
                                    + mappingTag.getChildren("key").size());
                }
                if (mappingTag.getChildren("value").size() != 1) {
                    throw new XMLBinderException(
                            "Expected <"
                                    + tag.getName()
                                    + "> to have 1 <value> tag under <mapping> but found "
                                    + mappingTag.getChildren("key").size());
                }
                ((Map) object).put(read(mappingTag.getChild("key"),
                        Object.class), read(mappingTag.getChild("value"),
                        Object.class));
            }
        } else if (object instanceof Collection) {
            for (Iterator i = tag.getChildren().iterator(); i.hasNext();) {
                Element itemTag = (Element) i.next();
                if (!itemTag.getName().equals("item")) {
                    throw new XMLBinderException("Expected <" + tag.getName()
                            + "> to have <item> tag but found none");
                }
                ((Collection) object).add(read(itemTag, Object.class));
            }
        } else {
            read(tag, object, specElements(object.getClass()));
        }
        return object;
    }
    private void fireCreatingObject(Class c) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            Listener l = (Listener) i.next();
            l.creatingObject(c);
        }
    }
    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    private void setValuesFromTags(Object object, Method setter, Collection tags)
            throws Exception {
        for (Iterator i = tags.iterator(); i.hasNext();) {
            Element tag = (Element) i.next();
            setValueFromTag(object, setter, tag);
        }
    }
    private void setValueFromTag(Object object, Method setter, Element tag)
            throws Exception {
        setValue(object, setter, read(tag, fieldClass(setter)));
    }
    private void setValue(Object object, Method setter, Object value)
            throws IllegalAccessException, InvocationTargetException {
        //If you get an InvocationTargetException, check the bottom of the
        // stack
        //trace -- you should see the stack trace for the underlying exception.
        //[Jon Aquino]
        setter.invoke(object, new Object[]{value});
    }
    public static interface Listener {
        public void creatingObject(Class c);
    }
}