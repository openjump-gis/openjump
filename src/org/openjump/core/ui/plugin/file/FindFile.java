package org.openjump.core.ui.plugin.file;

import java.awt.Component;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class FindFile {
    
    private static final String KEY = FindFile.class.getName();

    private static final String CHOOSE_LOCATION = I18N.get(KEY
        + ".choose-current-location-of");
    
    private Vector prefixList = new Vector(5, 5);

    private JFileChooser fileChooser;

    private Component window;

    public FindFile(Component window) {
      this.window = window;
      fileChooser = new JFCWithEnterAction();
      fileChooser = GUIUtil.createJFileChooserWithExistenceChecking();
      fileChooser.setDialogTitle(CHOOSE_LOCATION);
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setMultiSelectionEnabled(false);
    }

    public FindFile(Component window, JFileChooser fileChooser) {
      this(window);
      this.fileChooser = fileChooser;
    }

    public String getFileName(String filenamepath) throws Exception {
      // strip off file name
      File oldFile = new File(filenamepath);
      String oldPath = oldFile.getPath();
      // see if something in the prefixList matches all or part of oldPath
      for (Iterator i = prefixList.iterator(); i.hasNext();) {
        PathPrefixes prefix = (PathPrefixes)i.next();
        if (oldPath.toLowerCase().indexOf(prefix.getOldPrefix().toLowerCase()) > -1) // found
        // match
        {
          // replace matching portion with new prefix
          String newFileNamePath = filenamepath.substring(prefix.getOldPrefix()
            .length());
          newFileNamePath = prefix.getNewPrefix() + newFileNamePath;
          File newFile = new File(newFileNamePath);
          if (newFile.exists())
            return newFileNamePath;
          // else continue to look through list
        }
      }

      // at this point didn't find a match
      // ask user to find file
      fileChooser.setDialogTitle(CHOOSE_LOCATION + filenamepath);
      GUIUtil.removeChoosableFileFilters(fileChooser);
      fileChooser.addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
      String ext = "";
      int k = filenamepath.lastIndexOf('.');

      if ((k > 0) && (k < (filenamepath.length() - 1))) {
        ext = filenamepath.substring(k + 1);
        FileFilter fileFilter = GUIUtil.createFileFilter(ext.toUpperCase()
          + " Files", new String[] {
          ext.toLowerCase()
        });
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
      }

      if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(window)) {
        String newParent = fileChooser.getSelectedFile().getParent()
          + File.separator;
        String oldParent = new File(filenamepath).getParent() + File.separator;

        // find where they differ
        int i = newParent.length();
        int j = oldParent.length();
        while (newParent.substring(i).equalsIgnoreCase(oldParent.substring(j))) {
          i--;
          j--;
        }
        while (newParent.charAt(i) != File.separatorChar) {
          i++;
        }
        while (oldParent.charAt(j) != File.separatorChar) {
          j++;
        }

        String newPrefix = newParent.substring(0, ++i);
        String oldPrefix = oldParent.substring(0, ++j);

        PathPrefixes pathPrefix = new PathPrefixes(oldPrefix, newPrefix);
        prefixList.add(pathPrefix);
        return fileChooser.getSelectedFile().getPath();
        
      }
      return ""; // user canceled find file
    }
  }