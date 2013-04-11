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

import javax.swing.JFileChooser;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

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
        final String FILE_CHOOSER_PANEL_KEY = SaveFileDataSourceQueryChooser.class.getName() +
            " - SAVE FILE CHOOSER PANEL";

        //SaveFileDataSourceQueryChoosers share the same JFileChooser so that the user's
        //work is not lost when he switches data-source types. The JFileChooser options
        //are set once because setting them freezes the GUI for a few seconds. [Jon Aquino]
        if (blackboard().get(FILE_CHOOSER_PANEL_KEY) == null) {
            final JFileChooser fileChooser = GUIUtil.createJFileChooserWithOverwritePrompting();
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
        }

        return (FileChooserPanel) blackboard().get(FILE_CHOOSER_PANEL_KEY);
    }

    private Blackboard blackboard() {
        return context.getBlackboard();
    }

    public Collection getDataSourceQueries() {
        //User has pressed OK, so persist the directory. [Jon Aquino]
        PersistentBlackboardPlugIn.get(context).put(FILE_CHOOSER_DIRECTORY_KEY,
            getFileChooserPanel().getChooser().getCurrentDirectory().toString());

        return super.getDataSourceQueries();
    }
}
