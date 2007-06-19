package com.vividsolutions.jump.workbench.datasource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class LoadDatasetFromFilePlugIn extends AbstractLoadDatasetPlugIn {
    protected void setSelectedFormat(String format) {
        loadSaveDatasetFileMixin.setSelectedFormat(format);
    }
    protected String getSelectedFormat() {
        return loadSaveDatasetFileMixin.getSelectedFormat();
    }
    protected Collection showDialog(WorkbenchContext context) {
        final JFileChooser fileChooser = GUIUtil
                .createJFileChooserWithExistenceChecking();
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.rescanCurrentDirectory(); //[sstein: 29.10.2005 line added]
        return loadSaveDatasetFileMixin.showDialog(fileChooser,
                LoadFileDataSourceQueryChooser.class, context);
    }
    private LoadSaveDatasetFileMixin loadSaveDatasetFileMixin = new LoadSaveDatasetFileMixin() {
        protected String getName() {
            return LoadDatasetFromFilePlugIn.this.getName();
        }
        protected String getLastDirectoryKey() {
            return LoadDatasetFromFilePlugIn.this.getLastDirectoryKey();
        }
        public boolean isAddingExtensionIfRequested() { return false; }
        public File initiallySelectedFile(File currentDirectory) { return null; }
    };
    public String getName() {
        return I18N
                .get("datasource.LoadDatasetFromFilePlugIn.load-dataset-from-file");
    }
}
