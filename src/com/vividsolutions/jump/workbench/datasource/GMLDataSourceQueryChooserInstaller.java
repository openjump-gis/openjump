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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.FileNamePanel;

import java.awt.Component;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Adds to the JUMP Workbench the UIs for opening and saving GML files.
 * Called by InstallStandardDataSourceQueryChoosersPlugIn.
 * @see InstallStandardDataSourceQueryChoosersPlugIn
 */
public class GMLDataSourceQueryChooserInstaller {
    private static final String GML_DESCRIPTION = "GML 2.0";

    public void addSaveGMLFileDataSourceQueryChooser(
        final PlugInContext context) {
        DataSourceQueryChooserManager.get(context.getWorkbenchContext()
                                                 .getBlackboard())
                                     .addSaveDataSourceQueryChooser(new SaveFileDataSourceQueryChooser(
                StandardReaderWriterFileDataSource.GML.class, GML_DESCRIPTION,
                InstallStandardDataSourceQueryChoosersPlugIn.extensions(
                    StandardReaderWriterFileDataSource.GML.class),
                context.getWorkbenchContext()) {
                public boolean isInputValid() {
                    return isValid(getTemplateFileNamePanel()) &&
                    super.isInputValid();
                }

                protected Map<String,Object> toProperties(File file) {
                    HashMap<String,Object> properties = new HashMap<>(super.toProperties(file));
                    properties.put(StandardReaderWriterFileDataSource.OUTPUT_TEMPLATE_FILE_KEY,
                        getTemplateFileNamePanel().getSelectedFile().getPath());

                    return properties;
                }

                private FileNamePanel templateFileNamePanel;
                private FileNamePanel getTemplateFileNamePanel() {
                    //Lazily initialize to prevent NullPointerExceptions in WindowsFileChooserUI
                    //[Jon Aquino 2004-01-19]
                    if (templateFileNamePanel == null) {
                        templateFileNamePanel = createTemplateFileNamePanel("Output Template: ",
                                getFileChooserPanel().getChooser(),
                                context.getErrorHandler());
                    }
                    return templateFileNamePanel;
                }

                protected Component getSouthComponent1() {
                    return getTemplateFileNamePanel();
                }
            });
    }

    private boolean isValid(FileNamePanel templateFileNamePanel) {
        if (!templateFileNamePanel.isInputValid()) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(
                    templateFileNamePanel),
                    I18N.get("datasource.GMLDataSourceQueryChooserInstaller.template-file")+" " + templateFileNamePanel.getValidationError(),
                    I18N.get("datasource.GMLDataSourceQueryChooserInstaller.error"), JOptionPane.ERROR_MESSAGE);

            return false;
        }

        return true;
    }

    public void addLoadGMLFileDataSourceQueryChooser(
        final PlugInContext context) {
        DataSourceQueryChooserManager.get(context.getWorkbenchContext()
                                                 .getBlackboard())
                                     .addLoadDataSourceQueryChooser(new LoadFileDataSourceQueryChooser(
                StandardReaderWriterFileDataSource.GML.class, GML_DESCRIPTION,
                InstallStandardDataSourceQueryChoosersPlugIn.extensions(
                    StandardReaderWriterFileDataSource.GML.class),
                context.getWorkbenchContext()) {
                protected void addFileFilters(JFileChooser chooser) {
                    super.addFileFilters(chooser);
                    InstallStandardDataSourceQueryChoosersPlugIn.addCompressedFileFilter(GML_DESCRIPTION,
                        chooser);
                }

                public boolean isInputValid() {
                    return isValid(getTemplateFileNamePanel()) &&
                    super.isInputValid();
                }

                protected Map<String,Object> toProperties(File file) {
                    HashMap<String,Object> properties = new HashMap<>(super.toProperties(file));
                    properties.put(StandardReaderWriterFileDataSource.INPUT_TEMPLATE_FILE_KEY,
                        getTemplateFileNamePanel().getSelectedFile().getPath());

                    return properties;
                }

                private FileNamePanel templateFileNamePanel;

                private FileNamePanel getTemplateFileNamePanel() {
                    //Lazily initialize to facilitate CoordinateSystemSupport flag [Jon Aquino]
                    if (templateFileNamePanel == null) {
                        templateFileNamePanel = createTemplateFileNamePanel("Input Template: ",
                                getFileChooserPanel().getChooser(),
                                context.getErrorHandler());
                    }

                    return templateFileNamePanel;
                }

                protected Component getSouthComponent1() {
                    return getTemplateFileNamePanel();
                }
            });
    }

    private FileNamePanel createTemplateFileNamePanel(String description,
        final JFileChooser fileChooser, ErrorHandler errorHandler) {
        return new TemplateFileNamePanel(I18N.get("datasource.GMLDataSourceQueryChooserInstaller.input-template")+" ", errorHandler) {

                {
                    setFileMustExist(true);
                }

                protected File getInitialFile() {
                    File initialFile = super.getInitialFile();

                    if (!initialFile.exists() &&
                            ((initialFile.getParent() == null) ||
                            !initialFile.getParentFile().exists())) {
                        return fileChooser.getCurrentDirectory();
                    }

                    return initialFile;
                }
            };
    }

    private class TemplateFileNamePanel extends FileNamePanel {
        public TemplateFileNamePanel(String description,
            ErrorHandler errorHandler) {
            super(errorHandler);

            //"" gives upper description zero height. [Jon Aquino]
            setUpperDescription("");
            setLeftDescription(description);
        }
    }
}
