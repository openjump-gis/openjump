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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.locationtech.jts.util.Assert;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.Timer;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.WorkbenchProperties;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorToolPluginWrapper;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * Loads plug-ins (or more precisely, Extensions), and any JAR files that they
 * depend on, from the plug-in directory.
 */
public class PlugInManager {
    private static final String NOT_INITIALIZED = "com.vividsolutions.jump.workbench.plugin.PlugInManager.could-not-be-initialized";
    private static final String LOADING = "com.vividsolutions.jump.workbench.plugin.PlugInManager.loading";
    private static final String LOADING_ERROR = "com.vividsolutions.jump.workbench.plugin.PlugInManager.throwable-encountered-loading";

    private TaskMonitor monitor;
    private WorkbenchContext context;
    private ArrayList<Configuration> configurations = new ArrayList<Configuration>();

    private List<File> extensionDirs = new ArrayList<File>();
    // switch to parse only jar/zips in extension dir root for extensions
    // enabled by default, if disabled also classes/zip recursively are parsed
    private boolean limitExtensionLookup = true;
    private PlugInClassLoader classLoader;

    /**
     * @param plugInDirectory
     *            null to leave unspecified
     */
    public PlugInManager(WorkbenchContext context, TaskMonitor monitor) throws Exception {
        this.monitor = monitor;

//          class ExtendedURLClassLoader extends URLClassLoader{
//
//            public ExtendedURLClassLoader(URL[] urls) {
//              super(urls);
//            }
//
//            /**
//             * not really necessary now, but we keep it for reference for a future
//             * classloader per extension for allowing extensions to use differently
//             * versioned dependency jars in separate subfolders under
//             * lib/ext/<extension_subfolder>/
//             */
//            @Override
//            public Class loadClass(String name) throws ClassNotFoundException {
//              Class c = findLoadedClass(name);
//              if (c == null) {
//                try {
//                // disabled but not removed: here is the place to enforce plugin
//                // cl for specific classes/paths
//                // // these have to be handled like external packages
//                  if (name.matches("(?i).*WFS.*"))
//                    System.out.println(name);
//                  if (!name.startsWith("de.latlon.deejump.wfs")
//                      && !name.startsWith("org.deegree.")){
////                  if (!name.matches(".*WFSExtension"))
//                    c = getParent().loadClass(name);
//                  }
//                } catch (ClassNotFoundException e) {
//                }
//                if (c == null)
//                  c = findClass(name);
//              }
//              return c;
//            }
//            
//            /**
//             * allow adding urls, any time
//             * @param urls
//             */
//            public void addUrls( URL[] urls ){
//              for (URL url : urls) {
//                addURL(url);
//              }
//            }
//          };
//          
//          ExtendedURLClassLoader mycl = new ExtendedURLClassLoader(new URL[]{});
          
//          System.out.println("A:"+ClassLoader.getSystemClassLoader().getClass().getClassLoader());
//          System.out.println("B:"+PlugInClassLoader.class.getClassLoader());;

        try {
          classLoader = (PlugInClassLoader) ClassLoader.getSystemClassLoader();
        } catch (ClassCastException e) {
          Exception je = new JUMPException(
              "Wrong classloader. Make sure to run JRE with property -Djava.system.class.loader=com.vividsolutions.jump.workbench.plugin.PlugInClassLoader set!",e);
          throw je;
        }

        this.context = context;

        // fetch default-plugins early, to enqueue them at the beginning
        configurations.addAll(findConfigurations(context.getWorkbench()
            .getProperties().getConfigurationClassNames()));
    }

    public void setLimitExtensionLookup(boolean limitExtensionLookup) {
      this.limitExtensionLookup = limitExtensionLookup;
    }

    /**
     * add an extensions folder
     * - adds folder root to classpath
     * - adds all contained jars to classpath
     * - looks for extensions in jars only in folder root (to speedup start)
     * - looks for extensions in any jar file and as class files
     *   recursively if limit-ext-lookup is 'false'
     * 
     * @param dir
     */
    public void addExtensionsFolder(File dir) {
      // already added? we don't want duplicates
      if (extensionDirs.contains(dir)) {
        Logger.error("extensions-dir '"+dir+"' was already added before!");
        return;
      }

      if (!dir.exists()) {
        Logger.error("given extensions-dir '"+dir+"' does not exist!");
        return;
      }

      // add contained jars/zips _and_ base folder to classpath
      ArrayList<File> files = new ArrayList();
      files.add( dir );
      files.addAll( findFilesRecursively( dir, jarOrFolderFilter, true) );
      classLoader.addUrls(toURLs(files));
      // reload translation resource bundles after changing  classpath
      I18N.reset();

      // memorize to internal list
      extensionDirs.add(dir);

      configurations.addAll(findConfigurations(dir));
    }

    /**
     * add all jar files in a folder recursively to classpath 
     * @param dir
     */
    public void addJarsFolder(File dir) {
      // add contained jars/zips and base folder to classloader
      ArrayList<File> files = new ArrayList();
      files.addAll( findFilesRecursively( dir, jarOrFolderFilter, true) );
      classLoader.addUrls(toURLs(files));
      // reload translation resource bundles after changing  classpath
      I18N.reset();
    }

    /**
     * search extension directories in the order they were added
     * for a given file or folder
     * return first found or null if none exists
     * 
     * @param fileOrFolder
     * @return file object or null
     */
    public File findFileOrFolderInExtensionDirs( String fileOrFolder ) {
      for (File extDir : extensionDirs) {
        File candidate = new File( extDir, fileOrFolder );
        if ( candidate.exists() )
          return candidate;
      }
      return null;
    }

    // pretty much the main method, finds and loads extensions
    public void load() throws Exception {
      if (Logger.isDebugEnabled())
        Logger.debug("Pluginclassloader contains the folowing urls ->\n" + Arrays.asList(classLoader.getURLs()).stream()
            .map(Object::toString).collect(java.util.stream.Collectors.joining("\n")).toString());

      // load plugins from workbench-properties
      loadPlugIns(context.getWorkbench().getProperties());

      if (Logger.isDebugEnabled())
        Logger.debug("The following extensions were found and are about to be loaded ->\n" + configurations.stream()
            .map(Object::toString).collect(java.util.stream.Collectors.joining("\n")).toString());

      long start = Timer.milliSecondsSince(0);
      loadConfigurations();
      Logger.info("Loading all OJ extensions took "
          + Timer.secondsSinceString(start) + "s");

      FeatureInstaller f = FeatureInstaller.getInstance(context);
      // enable autoseparating in installer for plugins possibly installed later on
      f.setSeparatingEnabled(true);
      // for performance reasons we separate menu entries once
      // after all plugins/extensions were installed
      f.updateSeparatorsInAllMenus();
    }

    private void loadConfigurations() {
      PlugInContext pc = context.createPlugInContext();
      for (Iterator i = configurations.iterator(); i.hasNext();) {
        Configuration configuration = (Configuration) i.next();
        long start = Timer.milliSecondsSince(0);
        try {
          monitor.report(I18N.getInstance().get(LOADING) + " " + name(configuration) + " "
              + version(configuration));
          // we used the plugin classloader to instantiate extensions already above
          configuration.configure(pc);
          //System.out.println(Arrays.toString(((URLClassLoader)classLoader).getURLs()));
          Logger.info("Loading Config " + name(configuration) + " "
              + version(configuration) + " took " + Timer.secondsSinceString(start)
              + "s");
        }
        catch (Throwable e) {
          context.getErrorHandler().handleThrowable(e);
          context.getWorkbench().getFrame()
              .log(configuration.getClass().getName() + " " + I18N.getInstance().get(NOT_INITIALIZED), this.getClass());
        }
      }
    }
    
    private void loadPlugIns(WorkbenchProperties props) {
      PlugInContext pc = context.createPlugInContext();
      // List<String> classNames = props.getPlugInClassNames();
      Map<String, Map<String, String>> pluginSettings = props
          .getSettings(new String[]{WorkbenchProperties.KEY_PLUGIN});

      for (String className : pluginSettings.keySet()) {
//        System.out.println(i++ + "/"+ className);
        String initSetting = pluginSettings.get(className).get(
            WorkbenchProperties.ATTR_INITIALIZE);
        if (initSetting instanceof String
            && initSetting.equals(WorkbenchProperties.ATTR_VALUE_FALSE))
          continue;

        monitor.report(I18N.getInstance().get(LOADING) + " " + className);

        Class plugInClass = null;
        try {
          long start = Timer.milliSecondsSince(0);

          // make sure we use the plugin classloader for plugins
          plugInClass = classLoader.loadClass(className);
          if (plugInClass == null)
            throw new JUMPException("class '"+className+"' is not available in the class path!");

          Object o = plugInClass.newInstance();
          PlugIn plugIn;
          if (o instanceof CursorTool) {
            plugIn = new CursorToolPluginWrapper((CursorTool) o);
          } else {
            plugIn = (PlugIn) o;
          }

          plugIn.initialize(pc);

          // get plugin's menu settings
          Map<String, Map> menuSettings = props.getSettings(new String[] {
              WorkbenchProperties.KEY_PLUGIN, className,
              WorkbenchProperties.KEY_MENUS});

          // interpret menu settings
          for (Map.Entry<String, Map> entry : menuSettings.entrySet()) {

            String menuKey = entry.getKey();
            if (pc.getFeatureInstaller().fetchMenuForKey(menuKey)==null){
              if (menuKey != "order_id")
                Logger.error("'"+menuKey+"' is an invalid menu handle.");

              continue;
            }

            // install me to menu?
            String installSetting = props.getSetting(new String[] {
                WorkbenchProperties.KEY_PLUGIN, className,
                WorkbenchProperties.KEY_MENUS,
                menuKey,
                WorkbenchProperties.ATTR_INSTALL});
//            String orderSetting = props.getSetting(new String[] {
//                WorkbenchProperties.KEY_PLUGIN, className,
//                WorkbenchProperties.KEY_MENUS,
//                menuKey,
//                WorkbenchProperties.ATTR_ORDERID});
            // log (order) info
//            context
//                .getWorkbench()
//                .getFrame()
//                .log(
//                    "install " + className + " to " + menuKey + " = "
//                        + installSetting + " with orderid = " + orderSetting);
            // install, or not
            if (installSetting.equals(WorkbenchProperties.ATTR_VALUE_TRUE))
              pc.getFeatureInstaller().addMenuPlugin(menuKey, plugIn);
          }

          // register shortcuts of plugins
          pc.getFeatureInstaller().registerShortcuts(plugIn);

          context
              .getWorkbench()
              .getFrame()
              .log(
                  "Loading Plugin " + className + " took " + Timer.secondsSinceString(start)
                      + "s " );
          
        } catch (Throwable e) {
          context.getErrorHandler().handleThrowable(e);
          context.getWorkbench().getFrame()
              .log(className + " " + I18N.getInstance().get(NOT_INITIALIZED), this.getClass());
        }
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

    public static String message(Configuration configuration) {
      if (configuration instanceof Extension) {
          return ((Extension) configuration).getMessage();
      }
      return "";
  }

    /**
     * filter all Configurations from a list of class names
     * @param classNames name of Configuration classes
     * @return a collection of Configurations
     */
    private Collection<Configuration> findConfigurations(List<String> classNames) {
      List<Configuration> configurations = new ArrayList<>();
      for (Iterator i = classNames.iterator(); i.hasNext();) {
        String name = (String) i.next();
        
        try {
          // find class using the plugin classloader
          Class clazz = Class.forName(name, false, classLoader);
          if ( Configuration.class.isAssignableFrom(clazz) ) {
            Configuration configuration = (Configuration) clazz.newInstance();
            configurations.add(configuration);
          }
        } 
        // make sure ClassVersionErrors or such do not break OJ startup
        catch (Throwable t) {
          context.getErrorHandler().handleThrowable(t);
          continue;
        }
      }
      return configurations;
    }

    static Pattern jarFilePattern = Pattern.compile(".*\\.(?i:jar|zip)$");

    static FileFilter jarOrFolderFilter = new FileFilter(){
      public boolean accept(File f) {
        return jarFilePattern.matcher(f.getName()).matches() || f.isDirectory();
      }
    };

    static FileFilter extensionClassOrFolderFilter = new FileFilter(){
      public boolean accept(File f) {
        return isExtensionClassByName(f.getName()) || f.isDirectory();
      }
    };

    // no $ ensures that inner classes are ignored as well [ede]
    // Include "Configuration" for backwards compatibility. [Jon Aquino]
    static Pattern extensionClassPattern = Pattern.compile("[^$]+(Extension|Configuration)\\.class");

    private static boolean isExtensionClassByName( String name ) {
      return extensionClassPattern.matcher(name).matches();
    }

    private Collection<File> findFilesRecursively(File directory, FileFilter filter, boolean recursive) {
        Collection<File> filesFiltered = new ArrayList();
        // add only jars/zips/classes if filter is set (may be null), recursively if requested
        File[] files = directory.listFiles(filter);
        if (files == null) files = new File[0];
        for (File file : files) {
            if (file.isDirectory() && recursive) {
                filesFiltered.addAll(findFilesRecursively(file, filter, recursive));
            }
            else if (file.isFile())
              filesFiltered.add(file);
        }
        return filesFiltered;
    }

    private Collection<Configuration> findConfigurations(File plugInDirectory) {
      ArrayList<Configuration> configurations = new ArrayList<Configuration>();
      long start = Timer.milliSecondsSince(0);
      String folder = plugInDirectory.getPath();
      String msg = I18N.getInstance().get("com.vividsolutions.jump.workbench.plugin.PlugInManager.scan", folder);
      monitor.report(msg);

      // find class files that are extensions if '-limit-ext-lookup false'
      for (Iterator<File> i = findFilesRecursively(plugInDirectory, extensionClassOrFolderFilter, true)
          .iterator(); !limitExtensionLookup && i.hasNext();) {
        File file = (File) i.next();

        String relativePath = plugInDirectory.toURI().relativize(file.toURI()).getPath();
        Configuration c = toConfiguration(relativePath);
        if (c != null)
          configurations.add(c);
      }

      // we're looking into jar files in ext-folder root only by default (to speedup OJ
      // start), may be disabled for testing via cmd line param '-limit-ext-lookup false'
      for (Iterator i = findFilesRecursively(plugInDirectory, jarOrFolderFilter, !limitExtensionLookup).iterator(); i
          .hasNext();) {
        File file = (File) i.next();

        ZipFile zipFile = null;
        try {
          zipFile = new ZipFile(file);
          for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            // Filter by filename; otherwise we'll be loading all the classes,
            // which takes significantly longer [Jon Aquino]
            // no $ ensures that inner classes are ignored as well [ede]
            // Include "Configuration" for backwards compatibility. [Jon Aquino]
            if (!isExtensionClassByName(entry.getName()) || entry.isDirectory())
              continue;

            Configuration c = toConfiguration(entry.getName());
            if (c != null)
              configurations.add(c);
          }

        } catch (IOException e) {
          Logger.error(e);
        } finally {
          FileUtil.close(zipFile);
        }
      }

      Logger.info("Scanning " + folder + " took " + Timer.secondsSinceString(start) + "s. Found "
          + configurations.size() + " configurations.");

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

    private Configuration toConfiguration( String fileName ) 
    {
        String className = toClassName(fileName);
        Class<?> candidate;
        try {
          candidate = Class.forName(className, false, classLoader);
          // make sure we got us a real configuration here
          if ( Configuration.class.isAssignableFrom(candidate) ) {
            return (Configuration) candidate.getDeclaredConstructor().newInstance();
          }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("This should never happen. Check classpath!", e);
        } catch (Throwable t) {
            throw new RuntimeException(I18N.getInstance().get(LOADING_ERROR) + " " + className, t);
            //e.g. java.lang.VerifyError: class
            // org.apache.xml.serialize.XML11Serializer
            //overrides final method [Jon Aquino]
        }

        return null;
    }

    private String toClassName(String className) 
    {
        className = className.substring(0, className.length()
                - ".class".length());
        className = StringUtil.replaceAll(className, "/", ".");

        return className;
    }

    public Collection<Configuration> getConfigurations() {
        return Collections.unmodifiableCollection(configurations);
    }

    /**
     * To access extension classes, use this ClassLoader rather than the default
     * ClassLoader. Extension classes will not be present in the latter.
     * 
     * @deprecated use {@link #getPlugInClassLoader()} instead
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * To access extension classes, use this ClassLoader rather than the default
     * ClassLoader. Extension classes will not be present in the latter.
     */
    public PlugInClassLoader getPlugInClassLoader() {
        return classLoader;
    }

    /**
     * fetch a list of folders holding extension jars that were added during start 
     */
    public List<File> getExtensionDirs(){
      return Collections.unmodifiableList(extensionDirs);
    }

    /**
     * get extension folder, cloned to prevent modification
     * @deprecated use {@link #getExtensionDirs()}
     * @return possibly null
     */
    @Deprecated
    public File getPlugInDirectory() {
      return extensionDirs.isEmpty() ? null : new File(extensionDirs.get(0).toURI());
    }
}