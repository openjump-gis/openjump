/**
 * Copyright 2011 Edgar Soldin
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.soldin.jumpcore;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

//@SuppressWarnings("unchecked")
/**
 * pretty much an url classloader that wraps a parent (by default) and let's you
 * add urls in front (prepend) or at the end (append via finalCl) during runtime
 */
public class FlexibleClassLoader extends URLClassLoader {
  private String id = null;

  // list of classloaders to ask if we fail
  // ATTENTION: cls.loadclass results in a private classspace
  private Vector<URLClassLoader> cls = new Vector<URLClassLoader>();
  // list of regexp's that will loadClass will always delegate to parent
  // classloader
  private Vector<String> blacklist = new Vector<String>();
  // final Classloader, store URLs that are appended
  // prepended URLs are handled by the parent URLClassLoader
  private FlexibleClassLoader finalCl = new FlexibleClassLoader();

  public FlexibleClassLoader() {
    super(new URL[] {}, null);
    this.id = getCaller();
  }

  public FlexibleClassLoader(URL[] urls) {
    super(urls, null);
    this.id = getCaller();
  }

  public FlexibleClassLoader(ClassLoader parent, boolean set_parent) {
    super(new URL[] {}, set_parent ? parent : null);
    this.id = getCaller();
    if (!set_parent)
      this.addCL(parent);
  }

  public FlexibleClassLoader(URL[] urls, ClassLoader parent, boolean set_parent) {
    super(urls, set_parent ? parent : null);
    this.id = getCaller();
    if (!set_parent)
      this.addCL(parent);
  }

  public void prepend(String path) {
    try {
      super.addURL(new File(path).toURI().toURL());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public void prependURL(URL url) {
    super.addURL(url);
  }

  public void prependURLs(URL[] urls) {
    for (int i = 0; i < urls.length; i++) {
      super.addURL(urls[i]);
    }
  }

  public boolean prependAllFiles(String path) {
    return addAllFiles(path, null, false, true);
  }

  public boolean prependAllFilesRecursive(String path) {
    return addAllFiles(path, null, true, true);
  }

  public boolean prependAllFilesRecursive(String path, String suffix) {
    return addAllFiles(path, suffix, true, true);
  }

  public void append(String path) {
    try {
      finalCl.addURL(new File(path).toURI().toURL());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public void appendURL(URL url) {
    finalCl.addURL(url);
  }

  public void appendURLs(URL[] urls) {
    for (int i = 0; i < urls.length; i++) {
      finalCl.addURL(urls[i]);
    }
  }

  public boolean appendAllFiles(String path) {
    return addAllFiles(path, null, false, false);
  }

  public boolean appendAllFilesRecursive(String path) {
    return addAllFiles(path, null, true, false);
  }

  public boolean appendAllFilesRecursive(String path, String suffix) {
    return addAllFiles(path, suffix, true, false);
  }

  public boolean addAllFiles(String path, String suffix, boolean recursive, boolean prepend) {
    // initialize null suffix to empty string
    suffix = suffix instanceof String ? suffix : "";
    File file = new File(path);
    File[] files = file.listFiles();
    // replace failed listing with file itself
    if (!(files instanceof File[]))
      files = new File[] { file };
    // iterate through entries
    for (int i = 0; files != null && i < files.length; i++) {
      file = files[i];
      if (recursive && file.isDirectory()) {
        this.addAllFiles(file.getAbsolutePath(), suffix, recursive, prepend);
      } else if (file.getName().endsWith(suffix)) {
        if (prepend)
          this.prepend(file.getAbsolutePath());
        else
          this.append(file.getAbsolutePath());
        // System.out.println(this.getClass().getName()+" added lib: "+
        // file.getAbsolutePath());
      }
    }
    return true;
  }

  public boolean addCL(ClassLoader cl) {
    if (isURLCL(cl) && !this.cls.contains(cl)) {
      this.cls.add(0, (URLClassLoader) cl);
      return true;
    }

    return false;
  }

  public boolean remCL(ClassLoader cl) {
    return this.cls.remove(cl);
  }

  public boolean isBlacklisted(String name) {
    Iterator it = blacklist.iterator();
    while (it.hasNext()) {
      String entry = (String) it.next();
      if (name.matches(entry))
        return true;
    }
    return false;
  }

  public boolean blacklist(String regexp) {
    Iterator it = blacklist.iterator();
    while (it.hasNext()) {
      String entry = (String) it.next();
      if (entry.equals(regexp))
        return false;
    }
    this.blacklist.add(0, regexp);
    return true;
  }

  public boolean whitelist(String regexp) {
    boolean res = false;
    Iterator it = blacklist.iterator();
    while (it.hasNext()) {
      String entry = (String) it.next();
      if (entry.equals(regexp)) {
        res = blacklist.remove(entry);
      }
    }
    return res;
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    System.out.println(this.getClass().getName() + " loadClass(" + name + ")");

    if (!isBlacklisted(name)) {
      // search me
      try {
        return super.loadClass(name, false);
      } catch (ClassNotFoundException e) {
        // search next
      }
    }
    System.out.println(this.getClass().getName() + " loadClass parent(" + name
        + ")");

    // search parents
    Iterator it = cls.iterator();
    while (it.hasNext()) {
      URLClassLoader cl = (URLClassLoader) it.next();
      try {
        return cl.loadClass(name);
      } catch (ClassNotFoundException e) {
        // try next
      }
    }

    // search finalCL
    try {
      return finalCl.loadClass(name, false);
    } catch (ClassNotFoundException e) {
      // search next
    }

    // System.out.println(this.getClass().getName()+" loadClass("+name+")->failed");
    throw new ClassNotFoundException(name);
  }

  public URL findResource(final String name) {
    URL url = null;

    // search me
    if (!(url instanceof URL))
      url = super.findResource(name);

    // search parents
    Iterator it = cls.iterator();
    while (it.hasNext() && !(url instanceof URL)) {
      URLClassLoader cl = (URLClassLoader) it.next();
      url = cl.findResource(name);
    }

    // search finalCl
    if (!(url instanceof URL))
      url = finalCl.findResource(name);

    // System.out.println(this.getClass().getName()+" found("+name+"): "+url);
    return url;
  }

  public Enumeration<URL> findResources(final String name) throws IOException {

    Vector<Enumeration> enums = new Vector<Enumeration>();
    // add mine
    enums.add(super.findResources(name));
    // add parents
    Iterator it = cls.iterator();
    while (it.hasNext()) {
      URLClassLoader cl = (URLClassLoader) it.next();
      enums.add(cl.findResources(name));
    }
    // add finalCl
    enums.add(finalCl.findResources(name));
    // finalize an iterator for use in Enum
    final Vector enums_final = new Vector(enums);

    return new Enumeration<URL>() {

      public URL nextElement() {
        URL url = null;
        Enumeration<URL> urls;
        Iterator it = enums_final.iterator();
        while (it.hasNext() && url == null) {
          urls = (Enumeration<URL>) it.next();
          while (urls.hasMoreElements() && url == null) {
            url = (URL) urls.nextElement();
          }
        }
        return url;
      }

      public boolean hasMoreElements() {
        Iterator it = enums_final.iterator();
        boolean more = false;
        Enumeration<URL> urls;
        while (it.hasNext() && !more) {
          urls = (Enumeration<URL>) it.next();
          more = urls.hasMoreElements();
        }
        return more;
      }
    };
  }

  public URL[] getURLs() {
    Vector urls = new Vector(Arrays.asList(super.getURLs()));

    // add parents
    Iterator it = cls.iterator();
    URL[] parents;
    while (it.hasNext()) {
      URLClassLoader cl = (URLClassLoader) it.next();
      if (cl == this)
        break;
      parents = cl.getURLs();
      urls.addAll(Arrays.asList(parents));
    }
    
    // add finalCl
    urls.addAll(Arrays.asList(finalCl.getURLs()));

    return (URL[]) urls.toArray(new URL[urls.size()]);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(super.toString() + " --> " + id + "\n");
    for (URL url : getURLs()) {
      buf.append(url + "\n");
    }
    return buf.toString();
  }

  public static boolean isURLCL(ClassLoader cl) {
    return (cl instanceof URLClassLoader);
  }

  public String getCaller() {
    return Thread.currentThread().getStackTrace()[3].toString();
  }

  static public String getBase() {
    return getBase(null);
  }

  static public String getBase(Class clazz) {

    URL whereami = clazz instanceof Class ? clazz.getProtectionDomain()
        .getCodeSource().getLocation() : FlexibleClassLoader.class
        .getProtectionDomain().getCodeSource().getLocation();
    // System.out.println( getStaticName( clazz ) + " is in: "+whereami );

    if (whereami == null) {
      return "";
    }

    // postprocessing
    String path = whereami.toString();
    path = path.endsWith("!/") ? path.subSequence(0, path.length() - 2)
        .toString() : path;
    path = path.startsWith("jar:") ? path.subSequence(4, path.length())
        .toString() : path;
    path = path.startsWith("file:") ? path.subSequence(5, path.length())
        .toString() : path;

    // as extension is always in jar, simply get the parent
    // should result in "JUMPHOME/ext/"
    File basefile = new File(path);
    String baseFolder = basefile.getAbsolutePath();

    return baseFolder;
  }

  static public String getBaseFolder() {
    return new File(getBase()).getParent();
  }

}
