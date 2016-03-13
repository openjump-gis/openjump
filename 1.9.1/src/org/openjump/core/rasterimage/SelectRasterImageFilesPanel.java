/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.rasterimage;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;
import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectRasterImageFilesPanel extends JFCWithEnterAction implements WizardPanel {

  public static final String KEY = SelectRasterImageFilesPanel.class.getName();
  
  public static final String FILE_CHOOSER_DIRECTORY_KEY = KEY + " - FILE CHOOSER DIRECTORY";

  public static final String TITLE = I18N.get("org.openjump.core.rasterimage.SelectRasterImageFilesPanel.Select-Raster-Image");

  public static final String INSTRUCTIONS = I18N.get("org.openjump.core.ui.plugin.file.open.SelectFileOptionsPanel.instructions");

  public static final String ALL_FILES = I18N.get("org.openjump.core.ui.plugin.file.open.SelectFilesPanel.all-files");

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private Blackboard blackboard;
  private ActionListener dialogActionListener;

  public SelectRasterImageFilesPanel(final WorkbenchContext context) {
    setDialogType(JFileChooser.OPEN_DIALOG);
    
    if (PersistentBlackboardPlugIn.get(context).get(FILE_CHOOSER_DIRECTORY_KEY) != null) {
      setCurrentDirectory(new File((String)PersistentBlackboardPlugIn
                          .get(context)
                          .get(FILE_CHOOSER_DIRECTORY_KEY)));
    }
    
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    setMultiSelectionEnabled(true);
    GUIUtil.removeChoosableFileFilters(this);
    /*
    FileFilter GEOTIFF_FILE_FILTER = GUIUtil.createFileFilter("GeoTIFF", new String[]{ "tif", "tiff" });
    FileFilter GIF_FILE_FILTER = GUIUtil.createFileFilter("GIF", new String[]{ "gif"});
    FileFilter JPG_FILE_FILTER = GUIUtil.createFileFilter("JPEG", new String[]{ "jpg"});
    FileFilter PNG_FILE_FILTER = GUIUtil.createFileFilter("PNG", new String[]{ "png"});
    addChoosableFileFilter(GEOTIFF_FILE_FILTER);
    addChoosableFileFilter(GIF_FILE_FILTER);
    addChoosableFileFilter(JPG_FILE_FILTER);
    addChoosableFileFilter(PNG_FILE_FILTER);
    */
    FileFilter JAI_IMAGE_FILE_FILTER = GUIUtil.createFileFilter(I18N.get("org.openjump.core.rasterimage.SelectRasterImageFilesPanel.supported-raster-image-formats"), 
    		new String[]{ "tif", "tiff", "gif", "jpg", "png", "flt", "asc", "txt" });
    addChoosableFileFilter(JAI_IMAGE_FILE_FILTER);
    addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
    
    setFileFilter(JAI_IMAGE_FILE_FILTER);

    setControlButtonsAreShown(false);

    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        PersistentBlackboardPlugIn.get(context)
            .put(FILE_CHOOSER_DIRECTORY_KEY, getCurrentDirectory().toString());
        fireInputChanged();
      }
    });
  }

  public void setDialog(WizardDialog dialog) {
    removeActionListener(dialogActionListener);
    dialogActionListener = new InvokeMethodActionListener(dialog, "next");
    addActionListener(dialogActionListener);
    
  }
  public void enteredFromLeft(final Map dataMap) {
    rescanCurrentDirectory();
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return getClass().getName();
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getNextID() {
    return null;
  }

  public String getTitle() {
    return TITLE;
  }

  public boolean isInputValid() {
    return getSelectedFile() != null;
  }

  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

  private void fireInputChanged() {
    for (InputChangedListener listener : listeners) {
      listener.inputChanged();
    }
  }
}
