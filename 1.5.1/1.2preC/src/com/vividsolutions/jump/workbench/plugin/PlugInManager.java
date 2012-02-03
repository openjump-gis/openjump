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
package com.vividsolutions.jump.workbench.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;

/**
 * Loads plug-ins (or more precisely, Extensions), and any JAR files that they
 * depend on, from the plug-in directory.
 */
public class PlugInManager {
	private static Logger LOG = Logger.getLogger(PlugInManager.class);
    private TaskMonitor monitor;
    private WorkbenchContext context;
    private Collection configurations = new ArrayList();

    private File plugInDirectory;

    /**
     * @param plugInDirectory
     *            null to leave unspecified
     */
    public PlugInManager(WorkbenchContext context, File plugInDirectory,
            TaskMonitor monitor) throws Exception {
        this.monitor = monitor;
        Assert.isTrue((plugInDirectory == null)
                || plugInDirectory.isDirectory());
        classLoader = plugInDirectory != null ? new URLClassLoader(
                toURLs((File[]) findFilesRecursively(plugInDirectory).toArray(
                        new File[] {}))) : getClass().getClassLoader();
        this.context = context;
        //Find the configurations right away so they get reported to the splash
        //screen ASAP. [Jon Aquino]
        configurations
                .addAll(plugInDirectory != null ? findConfigurations(plugInDirectory)
                        : new ArrayList());
        configurations.addAll(findConfigurations(context.getWorkbench()
                .getProperties().getConfigurationClasses()));
        this.plugInDirectory = plugInDirectory;
    }

    public void load() throws Exception {
        loadPlugInClasses(context.getWorkbench().getProperties()
                .getPlugInClasses(getClassLoader()));
        loadConfigurations();
    }

    private void loadConfigurations() throws Exception {
        for (Iterator i = configurations.iterator(); i.hasNext();) {
            Configuration configuration = (Configuration) i.next();
            configuration.configure(new PlugInContext(context, null, null,
                    null, null));
        }
    }

    public static String name(Configuration configuration) {
        if (configuration instanceof Extension) {
            return ((Extension) configuration).getName();
        }
        return StringUtil.toFriendlyName(configuration.getClass().getName(),
                "Configuration")
                + " (" + configuration.getClass().getPackage().getName() + ")";
    }

    public static String version(Configuration configuration) {
        if (configuration instanceof Extension) {
            return ((Extension) configuration).getVersion();
        }
        return "";
    }

    private Collection findConfigurations(List classes) throws Exception {
        ArrayList configurations = new ArrayList();
        for (Iterator i = classes.iterator(); i.hasNext();) {
            Class c = (Class) i.next();
            if (!Configuration.class.isAssignableFrom(c)) {
                continue;
            }
            LOG.debug("Loading " + c.getName());
            System.out.println("Loading " + c.getName());
            Configuration configuration = (Configuration) c.newInstance();
            configurations.add(configuration);
            monitor.report("Loading " + name(configuration) + " "
                    + version(configuration));
        }
        return configurations;
    }

    private void loadPlugInClasses(List plugInClasses) throws Exception {
        for (Iterator i = plugInClasses.iterator(); i.hasNext();) {
            Class plugInClass = (Class) i.next();
            PlugIn plugIn = (PlugIn) plugInClass.newInstance();
            plugIn.initialize(new PlugInContext(context, null, null, null,
                            null));
        }
    }

    private ClassLoader classLoader;

    private Collection findFilesRecursively(File directory) {
        Assert.isTrue(directory.isDirectory());
        Collection files = new ArrayList();
        for (Iterator i = Arrays.asList(directory.listFiles()).iterator(); i
                .hasNext();) {
            File file = (File) i.next();
            if (file.isDirectory()) {
                files.addAll(findFilesRecursively(file));
            }
            if (!file.isFile()) {
                continue;
            }
            files.add(file);
        }
        return files;
    }

    private Collection findConfigurations(File plugInDirectory)
            throws Exception {
        ArrayList configurations = new ArrayList();
        for (Iterator i = findFilesRecursively(plugInDirectory).iterator(); i
                .hasNext();) {
            File file = (File) i.next();
            try {
                configurations.addAll(findConfigurations(classes(new ZipFile(
                        file), classLoader)));
            } catch (ZipException e) {
                //Might not be a zipfile. Eat it. [Jon Aquino]
            }
        }
        return configurations;
    }

    private URL[] toURLs(File[] files) {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                urls[i] = new URL("jar:file:" + files[i].getPath() + "!/");
            } catch (MalformedURLException e) {
                Assert.shouldNeverReachHere(e.toString());
            }
        }
        return urls;
    }

    private List classes(ZipFile zipFile, ClassLoader classLoader) {
        ArrayList classes = new ArrayList();
        for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            //Filter by filename; otherwise we'll be loading all the classes,
            // which takes
            //significantly longer [Jon Aquino]
            if (!(entry.getName().endsWith("Extension.class") || entry
                    .getName().endsWith("Configuration.class"))) {
                //Include "Configuration" for backwards compatibility. [Jon
                // Aquino]
                continue;
            }
            Class c = toClass(entry, classLoader);
            if (c != null) {
                classes.add(c);
            }
        }
        return classes;
    }

    private Class toClass(ZipEntry entry, ClassLoader classLoader) {
        if (entry.isDirectory()) {
            return null;
        }
        if (!entry.getName().endsWith(".class")) {
            return null;
        }
        if (entry.getName().indexOf("$") != -1) {
            //I assume it's not necessary to load inner classes explicitly.
            // [Jon Aquino]
            return null;
        }
        String className = entry.getName();
        className = className.substring(0, className.length()
                - ".class".length());
        className = StringUtil.replaceAll(className, "/", ".");
        Class candidate;
        try {
            candidate = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Assert.shouldNeverReachHere("Class not found: " + className
                    + ". Refine class name algorithm.");
            return null;
        } catch (Throwable t) {
            LOG.error("Throwable encountered loading " + className
                    + ":");
            //e.g. java.lang.VerifyError: class
            // org.apache.xml.serialize.XML11Serializer
            //overrides final method [Jon Aquino]
            t.printStackTrace(System.out);
            return null;
        }
        return candidate;
    }

    public Collection getConfigurations() {
        return Collections.unmodifiableCollection(configurations);
    }

    /**
     * To access extension classes, use this ClassLoader rather than the default
     * ClassLoader. Extension classes will not be present in the latter.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    /**
     * @return possibly null
     */
    public File getPlugInDirectory() {
        return plugInDirectory;
    }
}