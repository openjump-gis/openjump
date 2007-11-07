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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoaderExtensionFilter;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectFilesPanel extends JFileChooser implements WizardPanel {

  public static final String KEY = SelectFilesPanel.class.getName();

  public static final String TITLE = I18N.get(KEY);

  public static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  public static final String ALL_FILES = I18N.get(KEY + ".all-files");

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private Blackboard blackboard;

  private OpenFileWizardState state;

  private WorkbenchContext workbenchContext;

  private boolean initialized = false;

  private WizardDialog dialog;

  public SelectFilesPanel(final WorkbenchContext workbenchContext) {
    this.workbenchContext = workbenchContext;
  }

  public OpenFileWizardState getState() {
    return state;
  }

  public void setState(OpenFileWizardState state) {
    this.state = state;
    for (FileFilter filter : getChoosableFileFilters()) {
      removeChoosableFileFilter(filter);
    }
  }

  public WizardDialog getDialog() {
    return dialog;
  }

  public void setDialog(WizardDialog dialog) {
    this.dialog = dialog;
  }

  private void initialize() {
    initialized = true;
    blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
    Registry registry = workbenchContext.getRegistry();

    String savedDirectoryName = (String)blackboard.get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY);
    if (savedDirectoryName != null) {
      setCurrentDirectory(new File(savedDirectoryName));
    }

    setAcceptAllFileFilterUsed(false);
    setMultiSelectionEnabled(true);
    List loaders = registry.getEntries(FileLayerLoader.KEY);
    Set<String> allExtensions = new TreeSet<String>();
    allExtensions.add("zip");
    allExtensions.add("gz");
    Map<String, FileFilter> filters = new TreeMap<String, FileFilter>();
    for (Object loader : loaders) {
      final FileLayerLoader fileLayerLoader = (FileLayerLoader)loader;
      FileFilter filter = new FileLayerLoaderExtensionFilter(fileLayerLoader);
      allExtensions.addAll(fileLayerLoader.getFileExtensions());
      filters.put(filter.getDescription(), filter);
    }

    FileFilter allFilter = new FileNameExtensionFilter(ALL_FILES,
      allExtensions.toArray(new String[0]));
    addChoosableFileFilter(allFilter);
    for (FileFilter filter : filters.values()) {
      addChoosableFileFilter(filter);

    }
    setFileFilter(allFilter);

    setControlButtonsAreShown(false);

    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        FileLayerLoader fileLayerLoader = null;
        File[] files = getSelectedFiles();
        FileFilter selectedFileFilter = getFileFilter();
        if (selectedFileFilter instanceof FileLayerLoaderExtensionFilter) {
          FileLayerLoaderExtensionFilter filter = (FileLayerLoaderExtensionFilter)selectedFileFilter;
          fileLayerLoader = filter.getFileLoader();
        }
        state.setupFileLoaders(files, fileLayerLoader);
        fireInputChanged();
      }
    });

    addActionListener(new InvokeMethodActionListener(dialog, "next"));
  }

  public void enteredFromLeft(final Map dataMap) {
    initialize();
    rescanCurrentDirectory();
    state.setCurrentPanel(KEY);
  }

  public void exitingToRight() throws Exception {
    blackboard.put(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY,
      getCurrentDirectory().getAbsolutePath());
  }

  public String getID() {
    return getClass().getName();
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getNextID() {
    return state.getNextPanel(KEY);
  }

  public String getTitle() {
    return TITLE;
  }

  public boolean isInputValid() {
    return state.hasSelectedFiles();
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
