package org.openjump.core.ui.plugin.file;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class FindFile {
    
    private static final String KEY = FindFile.class.getName();

    private static final String CHOOSE_LOCATION = I18N.getInstance().get(KEY
        + ".choose-current-location-of");

    private HashMap<Path, Path> prefixCache = new HashMap<Path, Path>();

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
      return getFileName( filenamepath, null);
    }

    public String getFileName(String filenamepath, File startPath) throws Exception {
      // consult prefix cache, the folder structure may just have been moved
      Path oldFilePath = Paths.get(filenamepath);
      for (Map.Entry<Path, Path> entry : prefixCache.entrySet()) {
        if (oldFilePath.startsWith(entry.getKey())) {
          Path newFilePath = entry.getValue().resolve( entry.getKey().relativize(oldFilePath) );
          File newFile = newFilePath.toFile();
          if (newFile.exists())
            return newFile.getPath();
        }
      }

      // at this point we didn't find a match, ask user to point out file
      fileChooser.setDialogTitle(CHOOSE_LOCATION + " " + filenamepath);
      // preselect only the filename, so user knows what we are looking for
      fileChooser.setSelectedFile(new File(new File(filenamepath).getName()));
      if (startPath != null)
        fileChooser.setCurrentDirectory(startPath);

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

        // find where they differ, compare backwards from last folder to first
        Path oldPath = Paths.get(oldParent);
        int oldPathCount = oldPath.getNameCount();
        Path newPath = Paths.get(newParent);
        int newPathCount = newPath.getNameCount();
        int max = (oldPathCount<newPathCount)? oldPathCount : newPathCount;
        Path oldPathFragment, newPathFragment = null;
        for (int i = 1; i <= max; ++i) {
          Path oldPart = oldPath.getName(oldPathCount - i);
          Path newPart = newPath.getName(newPathCount - i);
          if (!oldPart.equals(newPart)) {
            // cache the whole prefixes minus the identical folders
            oldPathFragment = oldPath.getRoot().resolve(oldPath.subpath(0, oldPathCount - i + 1));
            newPathFragment = newPath.getRoot().resolve(newPath.subpath(0, newPathCount - i + 1));
            prefixCache.put(oldPathFragment, newPathFragment);
            break;
          }
        }

        return fileChooser.getSelectedFile().getPath();
      }
      return ""; // user canceled find file
    }
  }