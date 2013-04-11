
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

package com.vividsolutions.jump.workbench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.vividsolutions.jump.workbench.ui.ErrorHandler;

public class WorkbenchPropertiesFile implements WorkbenchProperties {
    private ErrorHandler errorHandler;

    private Element root;

    public WorkbenchPropertiesFile(File file, ErrorHandler errorHandler) throws JDOMException, IOException {
        //alainvm [mav92@tiscali.fr] reports that he needs IOException in the throws
        //clause. I think he may be using a different version of JDOM.
        //[Jon Aquino 1/12/2004]
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        root = document.getRootElement();
        this.errorHandler = errorHandler;        
    }

    public List getPlugInClasses() {
        return getPlugInClasses(null); //null invokes default ClassLoader
    }

    public List getPlugInClasses(ClassLoader classLoader) {
        ArrayList plugInClasses = new ArrayList();

        for (Iterator i = root.getChildren("plug-in").iterator(); i.hasNext();) {
            Element plugInElement = (Element) i.next();
            try {
                plugInClasses.add(Class.forName(plugInElement.getTextTrim(),false,classLoader));
            } catch (ClassNotFoundException e) {
                errorHandler.handleThrowable(e);
            }
        }

        return plugInClasses;
    }

    public List getInputDriverClasses() throws ClassNotFoundException {
        ArrayList inputDriverClasses = new ArrayList();

        for (Iterator i = root.getChildren("input-driver").iterator(); i.hasNext();) {
            Element inputDriverElement = (Element) i.next();
            inputDriverClasses.add(Class.forName(inputDriverElement.getTextTrim()));
        }

        return inputDriverClasses;
    }

    public List getOutputDriverClasses() throws ClassNotFoundException {
        ArrayList outputDriverClasses = new ArrayList();

        for (Iterator i = root.getChildren("output-driver").iterator(); i.hasNext();) {
            Element outputDriverElement = (Element) i.next();
            outputDriverClasses.add(Class.forName(outputDriverElement.getTextTrim()));
        }

        return outputDriverClasses;
    }
    
    public List getConfigurationClasses() throws ClassNotFoundException {
        ArrayList getConfigurationClasses = new ArrayList();

        for (Iterator i = root.getChildren("extension").iterator(); i.hasNext();) {
            Element configurationElement = (Element) i.next();
            getConfigurationClasses.add(Class.forName(configurationElement.getTextTrim()));
        }

        return getConfigurationClasses;
    }    
}
