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
package com.vividsolutions.jump.workbench.datasource;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;


/**
 * UI for picking a file-based dataset to save. Does not automatically append an
 * extension if user does not specify one because, unlike Windows, on
 * Unix it is common for files not to have extensions.
 */
public class SaveFileDataSourceQueryChooser extends FileDataSourceQueryChooser {
    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class.getName() +
        " - FILE CHOOSER DIRECTORY";
    public static final String FILE_CHOOSER_PANEL_KEY = SaveFileDataSourceQueryChooser.class.getName() +
            " - SAVE FILE CHOOSER PANEL";

    private static final Pattern fileNameRegex = Pattern.compile(
        // Start of the path
        "^" +
        // Protocole (ex. file:) or machine name (\\machine) or parent directory (..) or current directory (.)
        "([a-zA-Z]:|\\\\\\\\[^/\\\\\\?%\\*:\\|\"<>]+|\\.\\.|\\.)?" +
        // directory names (/directory or \directory) name does not include /\?%*:|"<>
        "([/\\\\]([^/\\\\\\?%\\*:\\|\"<>]+|\\.\\.|\\.))*" +
        // file name (does not include /\?%*:|"<>)
        "([^/\\\\\\?%\\*:\\|\"<>\\.]+)" +
        // file name extension
        "(\\.[^/\\\\\\?%\\*:\\|\"<>\\.]+)?" +
        "$");

    private WorkbenchContext context;

    /**
     * @param extensions e.g. txt
     */
    public SaveFileDataSourceQueryChooser(Class dataSourceClass,
        String description, String[] extensions, WorkbenchContext context) {
        super(dataSourceClass, description, extensions);
        this.context = context;
    }

    protected FileChooserPanel getFileChooserPanel() {
        //Moved this local String to a static public String to be able to access to it
        //from SaveDatasetAsPlugIn [mmichaud 2007-08-25]
        //final String FILE_CHOOSER_PANEL_KEY = SaveFileDataSourceQueryChooser.class.getName() +
        //    " - SAVE FILE CHOOSER PANEL";

        //SaveFileDataSourceQueryChoosers share the same JFileChooser so that the user's
        //work is not lost when he switches data-source types. The JFileChooser options
        //are set once because setting them freezes the GUI for a few seconds. [Jon Aquino]
        if (blackboard().get(FILE_CHOOSER_PANEL_KEY) == null) {
            // the overwrite check is implemented below in isInputValid()
            // because we want it to be checked when OK is pressed also
            final JFileChooser fileChooser = new JFCWithEnterAction();
            // enforce the type to have mac java show the file name input field
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setControlButtonsAreShown(false);
            blackboard().put(FILE_CHOOSER_PANEL_KEY,
                new FileChooserPanel(fileChooser, blackboard()));

            if (PersistentBlackboardPlugIn.get(context).get(FILE_CHOOSER_DIRECTORY_KEY) != null) {
                fileChooser.setCurrentDirectory(new File(
                        (String) PersistentBlackboardPlugIn.get(context).get(FILE_CHOOSER_DIRECTORY_KEY)));
            }
            fileChooser.addAncestorListener(new AncestorListener() {
                public void ancestorAdded(AncestorEvent event) {
                    if (event.getAncestor() instanceof DataSourceQueryChooserDialog) {
                        fileChooser.rescanCurrentDirectory();
                    }
                }
                public void ancestorMoved(AncestorEvent event) { }
                public void ancestorRemoved(AncestorEvent event) { }
            });
            // user doubleclicked/pressed enter in jfc should confirm dialog
            fileChooser.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Component c = fileChooser.getParent();
                while ( c != null ){
                  if ( c instanceof DataSourceQueryChooserDialog ){
                    ((DataSourceQueryChooserDialog)c).setOKPressed();
                    break;
                  }
                  c = c.getParent();
                }
              }
            });
        }

        return (FileChooserPanel) blackboard().get(FILE_CHOOSER_PANEL_KEY);
    }

    private Blackboard blackboard() {
        return context.getBlackboard();
    }

    public Collection<DataSourceQuery> getDataSourceQueries() {
        //User has pressed OK, so persist the directory. [Jon Aquino]
        PersistentBlackboardPlugIn.get(context).put(FILE_CHOOSER_DIRECTORY_KEY,
            getFileChooserPanel().getChooser().getCurrentDirectory().toString());

        return super.getDataSourceQueries();
    }

  /**
   * selected files get the default (first) file extension appended if they do
   * not carry a valid extension already, as save file chooser is supposed to
   */
    public File[] getSelectedFiles() {
      // this is a one file chooser, so we ask jfc to give us the one selected
      File[] files = new File[]{getFileChooserPanel().getChooser().getSelectedFile()};
      
      String extension = getExtensions().length > 0 ? getExtensions()[0] : null;
      // no extensions? nothing to do here
      if (extension == null || extension.isEmpty())
        return files;

      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        // only treat files w/ missing extension here
        if (!file.isDirectory() && !hasValidExtension(file)) {
          files[i] = new File(file.getPath() + "." + extension);
        }
      }
      return files;
    }

    private boolean hasValidExtension( File file ){
      String[] validExtensions = getExtensions();
      String fileName = file.getName();
      for (String validExt : validExtensions) {
        if (fileName.toLowerCase().endsWith("."+validExt.toLowerCase())) {
          return true;
        }
      }
      return false;
    }

    public boolean isInputValid() {
      // this must be run FIRST!!!
      // it is IMPORTANT! as we are not using the fc's buttons it re-runs approve
      // selection on the filechooser to make sure that the value from the
      // filename textfield is properly filled into selected files for us to fetch
      if (!super.isInputValid())
        return false;

      File file = getSelectedFiles().length > 0 ? getSelectedFiles()[0] : null;

      // no file selected?
      if (file == null)
        return false;

      if (!fileNameRegex.matcher(file.getPath()).matches()) {
        context
            .getWorkbench()
            .getFrame()
            .warnUser(
                I18N.get("com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser.Invalid-file-name"));
        return false;
      }

      if (file.exists()){
        boolean overwrite = GUIUtil.showConfirmOverwriteDialog(null, file);
        if (!overwrite)
          return false;
      }

      return super.isInputValid();
    }
}
