package com.vividsolutions.jump.workbench.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PlugInClassLoader extends URLClassLoader {

  public PlugInClassLoader(ClassLoader parent) {
    super(new URL[0], parent);
    addUrls(((URLClassLoader) parent).getURLs());
  }

  public PlugInClassLoader(URL[] urls) {
    super(urls);
  }

  /**
   * not really necessary now, but we keep it for reference for a future
   * classloader per extension for allowing extensions to use differently
   * versioned dependency jars in separate subfolders under
   * lib/ext/<extension_subfolder>/
   */
  @Override
  public Class loadClass(String name) throws ClassNotFoundException {
    // if (name.matches("(?i).*PlugInClassLoader"))
    // System.out.println("foo");
    Class c = findLoadedClass(name);

    // skip the default classloader which we replace and
    // try it's parent to load java system jars and such
    if (c == null) {
      try {
        c = getParent().getParent().loadClass(name);
      } catch (ClassNotFoundException e) {
      }
    }

    // we prefer this class loader to the sun.misc.Launcher one to have all OJ
    // classes within one classloader, advantages are: 
    // - instanceof does not work over different classloaders
    // - we override some classes from extension jars (wfs, deegree), which is
    //   only possible if they are found before the ones in the jars
    // Note: 
    // exception is this class which is already instantiated with
    // sun.misc.Launcher so we keep it that way
    if (c == null
        && !name
            .equals("com.vividsolutions.jump.workbench.plugin.PlugInClassLoader")) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException e) {
      }
    }

    // this classloader is always loaded by the default cl, so find it there
    if (c == null
        && name
            .equals("com.vividsolutions.jump.workbench.plugin.PlugInClassLoader")) {
      try {
        c = getParent().loadClass(name);
      } catch (ClassNotFoundException e) {
      }
    }
    
    return c;
  }

  /**
   * allow adding urls, any time
   * 
   * @param urls
   */
  public void addUrls(URL[] urls) {
    for (URL url : urls) {
      addURL(url);
    }
  }
};