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

package com.vividsolutions.jump.workbench.ui.plugin.skin;

import java.io.File;
import java.io.FileFilter;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.Configuration;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * 
 * Installs custom 'look and feel' for UI via 'Skins'.
 * 
 */

public class InstallSkinsPlugIn extends AbstractPlugIn {
  private static Logger LOG = Logger.getLogger(InstallSkinsPlugIn.class);
  private static String SKINS = I18N
      .get("ui.plugin.skin.InstallSkinsPlugIn.skins");
  private static String DEFAULT = I18N
      .get("ui.plugin.skin.InstallSkinsPlugIn.default");

  public static LookAndFeelProxy createProxy(final String name,
      final String lookAndFeelClassName) {
    return new LookAndFeelProxy() {
      public LookAndFeel getLookAndFeel() {
        try {
          return (LookAndFeel) Class.forName(lookAndFeelClassName)
              .newInstance();
        } catch (InstantiationException e) {
          Assert.shouldNeverReachHere(e.toString());
        } catch (IllegalAccessException e) {
          Assert.shouldNeverReachHere(e.toString());
        } catch (ClassNotFoundException e) {
          Assert.shouldNeverReachHere(e.toString());
        }

        return null;
      }

      public String toString() {
        return name;
      }
    };
  }

  public void initialize(PlugInContext context) throws Exception {
    SkinOptionsPanel skinpan = new SkinOptionsPanel(context);
    
    String saved_skin = (String) PersistentBlackboardPlugIn.get(
        context.getWorkbenchContext()).get(SkinOptionsPanel.CURRENT_SKIN_KEY);
    
    //System.out.println(saved_skin);
    try {
      URLClassLoader ucl = (URLClassLoader) this.getClass().getClassLoader();
      // import lib/skins
      
      // check for availability
      Class c = this.getClass().getClassLoader().loadClass(saved_skin);
      // apply
      skinpan.updateAll((LookAndFeel)c.newInstance());
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    OptionsDialog.instance(context.getWorkbenchContext().getWorkbench())
    .addTab(SKINS, skinpan);
  }

  public static void addSkins(File skinsDirectory) throws Exception {
    ArrayList skins = new ArrayList();

    for (Iterator i = findFilesRecursively(skinsDirectory, true).iterator(); i
        .hasNext();) {
      File file = (File) i.next();
      try {
        ZipFile zipFile = new ZipFile(file);
        // add to classloader
        // TODO: ucl.
        for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
          ZipEntry entry = (ZipEntry) e.nextElement();
          //System.out.println(entry.getName());
          if ( entry.isDirectory() || !entry.getName().matches(".*\\.(?i:class)$") )
            continue;
          String className = entry.getName().substring(0,
              entry.getName().length() - ".class".length());
          className = StringUtil.replaceAll(className, "/", ".");
          try {
            Class c = InstallSkinsPlugIn.class.getClassLoader().loadClass(className);
            if (c != null) {
            // try to instantiate
            LookAndFeel skin = (LookAndFeel) c.newInstance();
            // add if successful
            UIManager.installLookAndFeel(skin.getName(),skin.getClass().getName());
            }
          } catch (Throwable t) { /*class loading issue or no lnf*/ }
        }
      } catch (ZipException e) {
        // Might not be a zipfile
      }
    }
  }

  static FileFilter jarfilter = new FileFilter() {
    public boolean accept(File f) {
      return f.getName().matches(".*\\.(?i:jar|zip)$") || f.isDirectory();
    }
  };

  private static Collection<File> findFilesRecursively(File directory,
      boolean recursive) {
    Collection files = new ArrayList();
    // add only jars/zips, recursively if requested
    for (Iterator i = Arrays.asList(directory.listFiles(jarfilter)).iterator(); i
        .hasNext();) {
      File file = (File) i.next();
      //System.out.println(file);
      if (file.isDirectory() && recursive) {
        files.addAll(findFilesRecursively(file, recursive));
      }
      if (!file.isFile()) {
        continue;
      }

      files.add(file);
    }
    return files;
  }

}
