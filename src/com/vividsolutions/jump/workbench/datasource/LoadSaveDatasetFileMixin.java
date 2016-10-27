package com.vividsolutions.jump.workbench.datasource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public abstract class LoadSaveDatasetFileMixin {

    protected void setSelectedFormat(String selectedFormat) {
        this.selectedFormat = selectedFormat;
    }
    private String selectedFormat = "";
    protected String getSelectedFormat() {
        return selectedFormat;
    }

    protected Collection showDialog(JFileChooser fileChooser,
            Class fileDataSourceQueryChooserClass, WorkbenchContext context) {
        try {
            return showDialogProper(fileChooser,
                    fileDataSourceQueryChooserClass, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    protected abstract String getName();
    protected abstract String getLastDirectoryKey();
    private Collection showDialogProper(final JFileChooser fileChooser,
            Class fileDataSourceQueryChooserClass, WorkbenchContext context)
            throws IOException {
        fileChooser.setDialogTitle(getName());
        fileChooser.setCurrentDirectory(new File(
                (String) PersistentBlackboardPlugIn.get(context).get(
                        getLastDirectoryKey(),
                        fileChooser.getCurrentDirectory().getCanonicalPath())));
        fileChooser.setSelectedFile(initiallySelectedFile(fileChooser.getCurrentDirectory()));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        GUIUtil.removeChoosableFileFilters(fileChooser);
        final Map fileFilterToFileDataSourceQueryChooserMap = new HashMap();
        for (Iterator i = CollectionUtil.concatenate(
                DataSourceQueryChooserManager.get(
                        context.getWorkbench().getBlackboard())
                        .getLoadDataSourceQueryChoosers(),
                DataSourceQueryChooserManager.get(
                        context.getWorkbench().getBlackboard())
                        .getSaveDataSourceQueryChoosers()).iterator(); i
                .hasNext();) {
            DataSourceQueryChooser dataSourceQueryChooser = (DataSourceQueryChooser) i
                    .next();
            if (!fileDataSourceQueryChooserClass
                    .isInstance(dataSourceQueryChooser)) {
                continue;
            }
            FileDataSourceQueryChooser fileDataSourceQueryChooser = (FileDataSourceQueryChooser) dataSourceQueryChooser;
            fileChooser.addChoosableFileFilter(fileDataSourceQueryChooser
                    .getFileFilter());
            fileFilterToFileDataSourceQueryChooserMap.put(
                    fileDataSourceQueryChooser.getFileFilter(),
                    fileDataSourceQueryChooser);
        }
        for (Iterator i = fileFilterToFileDataSourceQueryChooserMap.keySet()
                .iterator(); i.hasNext();) {
            FileFilter fileFilter = (FileFilter) i.next();
            if (fileFilter.getDescription().equals(selectedFormat)) {
                fileChooser.setFileFilter(fileFilter);
            }
        }
        if (JFileChooser.APPROVE_OPTION != fileChooser.showDialog(context
                .getWorkbench().getFrame(), null)) {
            return null;
        }
        PersistentBlackboardPlugIn.get(context).put(getLastDirectoryKey(),
                fileChooser.getCurrentDirectory().getCanonicalPath());
        selectedFormat = fileChooser.getFileFilter().getDescription();
        return CollectionUtil.collect(fileChooser.isMultiSelectionEnabled()
                ? Arrays.asList(fileChooser.getSelectedFiles())
                : Collections.singletonList(fileChooser.getSelectedFile()),
                new Block() {
                    public Object yield(Object file) {
                        FileDataSourceQueryChooser fileDataSourceQueryChooser = ((FileDataSourceQueryChooser) fileFilterToFileDataSourceQueryChooserMap
                                .get(fileChooser.getFileFilter()));
                        return fileDataSourceQueryChooser
                                .toDataSourceQuery(addExtensionIfRequested(
                                        (File) file, fileDataSourceQueryChooser
                                                .getExtensions()[0]));
                    }
                });
    }

    public abstract File initiallySelectedFile(File currentDirectory);

    private File addExtensionIfRequested(File file, String extension) {
        return isAddingExtensionIfRequested() ? FileUtil.addExtensionIfNone(file,
                extension) : file;
    }
    public abstract boolean isAddingExtensionIfRequested();
}
