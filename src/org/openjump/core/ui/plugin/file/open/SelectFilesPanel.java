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


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.FileLayerLoaderExtensionFilter;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.swing.listener.InvokeMethodActionListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanelV2;

public class SelectFilesPanel extends JFCWithEnterAction implements WizardPanelV2 {

  /**
   * 
   */
  private static final long serialVersionUID = 1081945397331254012L;

  public static final String KEY = SelectFilesPanel.class.getName();

  public static final String TITLE = I18N.get(KEY);

  public static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  public static final String ALL_FILES = I18N.get(KEY + ".all-files");
  
  public static final String ALL_SUPPORTED_FILES = I18N.get(KEY + ".all-supported-files");
  
  public static final String ARCHIVED_FILES = I18N.get(KEY + ".archived-files");

  public static final String COMPRESSED_FILES = I18N.get(KEY + ".compressed-files");

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private Blackboard blackboard;

  private OpenFileWizardState state;

  private WorkbenchContext workbenchContext;

  private boolean initialized = false;

  private WizardDialog dialog;

  private Class loaderFilter;

  public SelectFilesPanel(final WorkbenchContext workbenchContext) {
    super();
    this.workbenchContext = workbenchContext;
  }

  public SelectFilesPanel(final WorkbenchContext workbenchContext, final Class loaderFilter) {
    this(workbenchContext);
    this.loaderFilter = loaderFilter;
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
    // make sure we only initialize once
    if (initialized)
      return;
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
    Map<String, FileFilter> filters = new TreeMap<String, FileFilter>();
    
    // archive support is hardcoded in OpenFileWizardState,CompressedFile and 
    String[] zipExtensions = CompressedFile.getArchiveExtensions();
    allExtensions.addAll( Arrays.asList(zipExtensions) );
    FileFilter zipFilter = new FileNameExtensionFilter(ARCHIVED_FILES, zipExtensions );
    filters.put(zipFilter.getDescription(), zipFilter);
    // separate single compressed files to signal that they are not archives
    String[] packedExtensions = CompressedFile.getFileExtensions();
    allExtensions.addAll( Arrays.asList(packedExtensions) );
    FileFilter packedFilter = new FileNameExtensionFilter(COMPRESSED_FILES, packedExtensions );
    filters.put(packedFilter.getDescription(), packedFilter);

    for (Object loader : loaders) {
      if (loaderFilter != null && !loaderFilter.isInstance(loader))
        continue;
      final FileLayerLoader fileLayerLoader = (FileLayerLoader)loader;
      FileFilter filter = new FileLayerLoaderExtensionFilter(fileLayerLoader);
      allExtensions.addAll(fileLayerLoader.getFileExtensions());
      filters.put(filter.getDescription(), filter);
    }

    // ATTENTION: ALL and ALL_SUPPORTED have leading spaces so they get sorted to the 
    //            beginning of the formats list regardless of translations first character ;) 
    FileFilter filterNone = new FileNameExtensionFilter(" "+ALL_FILES, new String[]{"*"} );
    filters.put(filterNone.getDescription(), filterNone);
    
    FileFilter allFilter = new FileNameExtensionFilter(" "+ALL_SUPPORTED_FILES,
      allExtensions.toArray(new String[0]));
    filters.put(allFilter.getDescription(), allFilter);
    
    // add all filters from above
    for (FileFilter filter : filters.values()) {
      addChoosableFileFilter(filter);
    }
    
    setFileFilter(allFilter);

    setControlButtonsAreShown(false);

    
    PropertyChangeListener changeListener = new PropertyChangeListener() {
      Object lastNew = null;
      
      // user selected something in the fc
      public void propertyChange(PropertyChangeEvent evt) {
        // for some reason JFC calls this once with the File and then
        // again with a File array for multiselection. NO NEED to update
        // it two times if only one file was selected though
        Object newNew = evt.getNewValue();
        if (newNew instanceof File[] && !(lastNew instanceof File[])){
          File[] newFiles = (File[])newNew;
          if (newFiles.length == 1){
            Object newFile = newFiles[0];
            if (newFile.equals(lastNew))
              return;
          }
        }
        lastNew=newNew;

        // only listen to fc changes while we're on _this_ panel
        // OSX fc weirdly fires events even when we're on the next panel
        if (state.getCurrentPanel() != KEY){
          return;
        }
          
        changeState();
        fireInputChanged();
      }
    };
    addPropertyChangeListener(changeListener);

    addActionListener(new InvokeMethodActionListener(dialog, "next"));
  }

  /**
   * updating state if file selection changed
   */
  private void changeState(){
    FileLayerLoader fileLayerLoader = null;
    File[] files = getSelectedFiles();
    FileFilter selectedFileFilter = getFileFilter();
    if (selectedFileFilter instanceof FileLayerLoaderExtensionFilter) {
      FileLayerLoaderExtensionFilter filter = (FileLayerLoaderExtensionFilter)selectedFileFilter;
      fileLayerLoader = filter.getFileLoader();
    }
    state.setupFileLoaders(files, fileLayerLoader);
  }
  
  public void enteredFromLeft(final Map dataMap) {
    initialize();
    rescanCurrentDirectory();
    state.setCurrentPanel(KEY);
    // this is needed so that on reopening, when JFC still holds the
    // old selection, the next button gets activated properly in case
    // the user really wants to reopen the same file
    // NOTE: the file is mentioned in the textfield, but no gui selection
    //       is visible, that is a known JFC bug as far as i could find out
    changeState();
    fireInputChanged();
  }
  
  public void enteredFromRight() throws Exception {
    // reset state's cur panel for the prop listener above to react properly
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