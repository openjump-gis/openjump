package com.vividsolutions.jump.workbench.datasource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JFileChooser;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

/**
 * 
 * @deprecated in favor of com.vividsolutions.jump.workbench.datasource.SaveDatasetAsPlugIn
 *
 */
@Deprecated
public class SaveDatasetAsFilePlugIn extends AbstractSaveDatasetAsPlugIn {
    protected void setSelectedFormat(String format) {
        loadSaveDatasetFileMixin.setSelectedFormat(format);
    }
    protected String getSelectedFormat() {
        return loadSaveDatasetFileMixin.getSelectedFormat();
    }
    protected Collection showDialog(WorkbenchContext context) {
        JFileChooser fileChooser = GUIUtil
                .createJFileChooserWithOverwritePrompting();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        return loadSaveDatasetFileMixin.showDialog(fileChooser,
                LoadFileDataSourceQueryChooser.class, context);
    }
    private LoadSaveDatasetFileMixin loadSaveDatasetFileMixin = new LoadSaveDatasetFileMixin() {
        protected String getName() {
            return SaveDatasetAsFilePlugIn.this.getName();
        }
        protected String getLastDirectoryKey() {
            return SaveDatasetAsFilePlugIn.this.getLastDirectoryKey();
        }
        public boolean isAddingExtensionIfRequested() {
            return true;
        }
        public File initiallySelectedFile(File currentDirectory) {
            // Call #getCanonicalFile to validate that the layer name is a valid filename. [Jon Aquino 2005-07-28[]
            try {
                return new File(currentDirectory, getContext()
                        .createPlugInContext().getSelectedLayer(0).getName()).getCanonicalFile();
            } catch (IOException e) {
                return null;
            }
        }
    };
    public String getName() {
        return I18N
                .get("datasource.SaveDatasetAsFilePlugIn.save-dataset-as-file");
    }
}
