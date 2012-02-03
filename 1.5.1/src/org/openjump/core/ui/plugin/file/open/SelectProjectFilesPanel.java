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
package org.openjump.core.ui.plugin.file.open;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;

import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.SaveProjectAsPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectProjectFilesPanel extends JFileChooser implements WizardPanel {

  public static final String KEY = SelectProjectFilesPanel.class.getName();

  public static final String TITLE = I18N.get("ui.plugin.OpenProjectPlugIn.open-project");

  public static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  public static final String ALL_FILES = I18N.get(KEY + ".all-files");

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private Blackboard blackboard;
  private ActionListener dialogActionListener;

  public SelectProjectFilesPanel(final WorkbenchContext context) {
    setDialogType(JFileChooser.OPEN_DIALOG);
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    setMultiSelectionEnabled(true);
    GUIUtil.removeChoosableFileFilters(this);
    addChoosableFileFilter(SaveProjectAsPlugIn.JUMP_PROJECT_FILE_FILTER);
    addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
    setFileFilter(SaveProjectAsPlugIn.JUMP_PROJECT_FILE_FILTER);
    // [mmichaud 2011-11-08] start with last used directory 
    Blackboard blackboard = PersistentBlackboardPlugIn.get(context);
    String dir = (String)blackboard.get(OpenProjectWizard.FILE_CHOOSER_DIRECTORY_KEY);
    if (dir != null) setCurrentDirectory(new File(dir));

    setControlButtonsAreShown(false);

    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
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
