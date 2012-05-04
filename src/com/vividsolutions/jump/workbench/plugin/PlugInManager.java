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
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;

/**
 * Loads plug-ins (or more precisely, Extensions), and any JAR files that they
 * depend on, from the plug-in directory.
 */
public class PlugInManager {
    private static Logger LOG = Logger.getLogger(PlugInManager.class);
    private static final String NOT_INITIALIZED = 
      I18N.get("com.vividsolutions.jump.workbench.plugin.PlugInManager.could-not-be-initialized");
	  private static final String LOADING = 
	    I18N.get("com.vividsolutions.jump.workbench.plugin.PlugInManager.loading");
    private static final String LOADING_ERROR = 
      I18N.get("com.vividsolutions.jump.workbench.plugin.PlugInManager.throwable-encountered-loading");
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
        // add plugin folder and recursively all jar/zip files in it to classpath
        if ( plugInDirectory instanceof File ) {
          ArrayList<File> files = new ArrayList();
          files.add( plugInDirectory );
          files.addAll( findFilesRecursively(plugInDirectory,true) );
          classLoader = new URLClassLoader(toURLs(files));
        } else {
          classLoader = getClass().getClassLoader();
        }

//        classLoader = plugInDirectory != null ? new URLClassLoader(
//            toURLs(findFilesRecursively(plugInDirectory,true))) : getClass().getClassLoader();
        
        I18N.setClassLoader(classLoader);
        this.context = context;
        this.plugInDirectory = plugInDirectory;
    }

    // pretty much the main method, finds and loads extensions from 
    //  plugin dir 
    // and 
    //  plugins/extensions defined in workbench properties (developers use this)
    public void load() throws Exception {
        // load plugins from workbench-properties
        loadPlugInClasses(context.getWorkbench().getProperties()
                .getPlugInClasses(getClassLoader()));

        long start;
        //Find the configurations right away so they get reported to the splash
        //screen ASAP. [Jon Aquino]
        if (plugInDirectory != null) {
          start = secondsSince(0);
          configurations.addAll(findConfigurations(plugInDirectory));
          System.out.println("Finding all OJ extensions took "
              + secondsSince(start) + "s");
        }
        
        monitor.report("add standard extensions");
        configurations.addAll(findConfigurations(context.getWorkbench()
                .getProperties().getConfigurationClasses()));
        
        start = secondsSince(0);
        loadConfigurations();
        System.out.println("Loading all OJ extensions took " + secondsSince(start) + "s");
    }

    private void loadConfigurations() throws Exception {
      for (Iterator i = configurations.iterator(); i.hasNext();) {
        Configuration configuration = (Configuration) i.next();
        monitor.report(LOADING + " " + name(configuration) + " "
            + version(configuration));
        long start = secondsSince(0);
        configuration
            .configure(new PlugInContext(context, null, null, null, null));
        System.out.println("Loading " + name(configuration) + " "
            + version(configuration) + " took " + secondsSince(start) + "s");
      }
    }

    // a helper method to measure time frames in seconds 
    public static long secondsSince( long i ){
      return Math.abs(System.currentTimeMillis()/1000) - i;
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
            /*if ( c.newInstance() instanceof Configuration ) { //(!Configuration.class.isAssignableFrom(c)) {
                continue;
            }*/
            //LOG.debug(FOUND + " " + c.getName());
            //System.out.println(FOUND + " " + c.getName());
            //monitor.report(FOUND + " " + c.getName());
            try {
              Configuration configuration = (Configuration) c.newInstance();
              configurations.add(configuration);              
            }
            // well, no extension then ;)
            catch (Exception e) {}
            //monitor.report(LOADING + " " + name(configuration) + " "
            //        + version(configuration));
        }
        return configurations;
    }

    private void loadPlugInClasses(List plugInClasses) throws Exception {
        for (Iterator i = plugInClasses.iterator(); i.hasNext();) {
            Class plugInClass = null;
            try {
                plugInClass = (Class) i.next();
                PlugIn plugIn = (PlugIn) plugInClass.newInstance();
                plugIn.initialize(new PlugInContext(context, null, null, null, null));
            } catch (NoClassDefFoundError e) {
                LOG.warn(plugInClass + " " + NOT_INITIALIZED);
                LOG.info(e);
                System.out.println(plugInClass + " " + NOT_INITIALIZED);
            }
        }
    }

    private ClassLoader classLoader;

    private Collection findFiles(File directory) {
      return findFilesRecursively( directory, false);
    }

    FileFilter jarfilter = new FileFilter(){
      public boolean accept(File f) {
        return f.getName().matches(".*\\.(?i:jar|zip)$") || f.isDirectory();
      }
    };

    private Collection<File> findFilesRecursively(File directory, boolean recursive) {
        Collection files = new ArrayList();
        // add only jars/zips, recursively if requested
        for (Iterator i = Arrays.asList(directory.listFiles(jarfilter)).iterator(); i
                .hasNext();) {
            File file = (File) i.next();
            if (file.isDirectory() && recursive) {
                files.addAll(findFilesRecursively(file,recursive));
            }
            if (!file.isFile()) {
                continue;
            }

            //System.out.println(file.getPath()+"->"+file.isFile());
            files.add(file);
        }
        return files;
    }

    private Collection findConfigurations(File plugInDirectory) throws Exception {
      ArrayList configurations = new ArrayList();
      long start;
      for (Iterator i = findFiles(plugInDirectory).iterator(); i.hasNext();) {
        start = secondsSince(0);
        File file = (File) i.next();
//        if (!file.getPath().toLowerCase().endsWith(".zip")
//            && !file.getPath().toLowerCase().endsWith(".jar"))
//          continue;
        String msg = I18N.getMessage(
            "com.vividsolutions.jump.workbench.plugin.PlugInManager.scan",
            new String[] { file.getName() });
        monitor.report(msg);
        try {
          configurations.addAll(findConfigurations(classes(new ZipFile(file),
              classLoader)));
        } catch (ZipException e) {
          // Might not be a zipfile. Eat it. [Jon Aquino]
        }
        System.out.println("Scanning " + file + " took " + secondsSince(start)
            + "s");
      }
  
      return configurations;
    }

    private URL[] toURLs(Collection<File> files) {
      URL[] urls = new URL[files.size()];
      int i = 0;
      for (File file : files) {
        try {
          // TODO: if "jar:file:" not explicitely defined VertexSymbols Extensions die with
          //       "java.lang.SecurityException: sealing violation: can't seal package <>: already loaded"
          // [ede 05.2012]
          urls[i++] = file.isFile() ? new URL("jar:file:" + file.getPath() + "!/") : file.toURI().toURL();
        } catch (MalformedURLException e) {
          Assert.shouldNeverReachHere(e.toString());
        }
      }
      return urls;
    }

//    private URL[] toURLs(File[] files) {
//      URL[] urls = new URL[files.length];
//      for (int i = 0; i < files.length; i++) {
//          try {
//              urls[i] = new URL("jar:file:" + files[i].getPath() + "!/");
//          } catch (MalformedURLException e) {
//              Assert.shouldNeverReachHere(e.toString());
//          }
//      }
//      return urls;
//  }
    
    private List classes(ZipFile zipFile, ClassLoader classLoader) {

        ArrayList classes = new ArrayList();
        for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            // Filter by filename; otherwise we'll be loading all the classes,
            // which takes significantly longer [Jon Aquino]
            // no $ ensures that inner classes are ignored as well [ede]
            // Include "Configuration" for backwards compatibility. [Jon Aquino]
            if (!(entry.getName().matches("[^$]+(Extension|Configuration)\\.class"))
                  || entry.isDirectory()) {
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
//        if (entry.isDirectory()) {
//            return null;
//        }
//        if (!entry.getName().endsWith(".class")) {
//            return null;
//        }
//        if (entry.getName().indexOf("$") != -1) {
//            //I assume it's not necessary to load inner classes explicitly.
//            // [Jon Aquino]
//            return null;
//        }
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
            LOG.error(LOADING_ERROR + " " + className + ":");
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