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

import java.io.File;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import com.vividsolutions.jump.workbench.datasource.FileDataSourceQueryChooser.FileChooserPanel;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Prompts the user to pick a dataset to save.
 * @see DataSourceQueryChooserDialog
 */
public class SaveDatasetAsPlugIn extends AbstractSaveDatasetAsPlugIn {
    protected Collection showDialog(WorkbenchContext context) {
        GUIUtil.centreOnWindow(getDialog());
        
        // initialize the FileChooser with the layer name [mmichaud 2007-08-25]
        FileChooserPanel fcp = (FileChooserPanel)context.getBlackboard().get(SaveFileDataSourceQueryChooser.FILE_CHOOSER_PANEL_KEY);
        if (fcp != null) {
            JFileChooser jfc = fcp.getChooser();
            jfc.setSelectedFile(new File(jfc.getCurrentDirectory(),
                context.getLayerNamePanel().getSelectedLayers()[0].getName().replaceAll("[/:\\\\><\\|]","_")));
        }

        getDialog().setVisible(true);
        return getDialog().wasOKPressed() ? getDialog().getCurrentChooser().getDataSourceQueries() : null;        
    }
    protected void setSelectedFormat(String format) {
        getDialog().setSelectedFormat(format);
    }
    protected String getSelectedFormat() {
        return getDialog().getSelectedFormat();
    }
    private DataSourceQueryChooserDialog getDialog() {
        String KEY = getClass().getName() + " - DIALOG";
        if (null == getContext().getWorkbench().getBlackboard().get(KEY)) {
            getContext().getWorkbench().getBlackboard().put(
                    KEY,
                    new DataSourceQueryChooserDialog(
                            DataSourceQueryChooserManager
                                    .get(
                                            getContext().getWorkbench()
                                                    .getBlackboard())
                                    .getSaveDataSourceQueryChoosers(),
                            getContext().getWorkbench().getFrame(), getName(),
                            true));
        }
        DataSourceQueryChooserDialog dialog = (DataSourceQueryChooserDialog) getContext().getWorkbench().getBlackboard().get(KEY);
        dialog.setDialogTask(DataSourceQueryChooserDialog.SAVEDIALOG);
        return dialog;
    }
    
    public static final ImageIcon ICON = IconLoader.icon("disk.png");
}
