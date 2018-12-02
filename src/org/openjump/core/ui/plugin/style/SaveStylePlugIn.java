/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package org.openjump.core.ui.plugin.style;

import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openjump.core.apitools.IOTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.SaveFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * plugin to save a layer style as file.
 * Currently supported JUMP XML format [file_name.style.xml] and (partially) 
 * Spatial layer descriptor [file_name.sld]
 * @author Giuseppe Aruta
 * @date 2018_21_2
 *
 */
public class SaveStylePlugIn extends ThreadedBasePlugIn {

    public final static ImageIcon ICON = IconLoader.icon("style_out.png");
    private final String name = I18N
            .get("org.openjump.core.ui.plugin.style.StylePlugIns.export-style");

    public ImageIcon getIcon() {

        return ICON;
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);

    }

    @Override
    public String getName() {
        return name;
    }

    File file;
    Layer layer;
    private final FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "JUMP layer symbology", "style.xml");
    private final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
            "Spatial layer descriptor", "sld");
    private final JFCWithEnterAction fc = new GUIUtil.FileChooserWithOverwritePrompting();
    private static final String FILE_CHOOSER_DIRECTORY_KEY = SaveFileDataSourceQueryChooser.class
            .getName() + " - FILE CHOOSER DIRECTORY";

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);

        layer = context.getSelectedLayer(0);

        fc.setCurrentDirectory(

        new File((String) PersistentBlackboardPlugIn.get(
                context.getWorkbenchContext()).get(FILE_CHOOSER_DIRECTORY_KEY)));

        fc.setSelectedFile(new File(fc.getCurrentDirectory(), layer.getName()
                .replaceAll("[/:\\\\><\\|]", "_")));

        fc.setDialogTitle(name);
        fc.setDialogType(JFileChooser.SAVE_DIALOG);

        fc.setFileFilter(filter2);
        fc.setFileFilter(filter);
        fc.addChoosableFileFilter(filter);

        return true;
    }

    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        final EnableCheckFactory ecf = new EnableCheckFactory(workbenchContext);
        final MultiEnableCheck mec = new MultiEnableCheck().add(
                ecf.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(ecf.createExactlyNLayerablesMustBeSelectedCheck(1,
                        Layer.class));
        return mec;

    }

    private void monitor(TaskMonitor monitor, File file) {
        monitor.allowCancellationRequests();
        monitor.report(I18N.get("ui.plugin.SaveDatasetAsPlugIn.saving") + ": "
                + file.getAbsolutePath());
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {

        if (JFileChooser.APPROVE_OPTION != fc.showSaveDialog(context
                .getWorkbenchFrame())) {
            return;
        }
        String filePath = "";
        if (fc.getFileFilter().equals(filter)) {
            file = fc.getSelectedFile();
            file = FileUtil.addExtensionIfNone(file, "style.xml");
            monitor(monitor, file);
            IOTools.saveSimbology_Jump(file, layer);
            filePath = file.getAbsolutePath();

        } else if (fc.getFileFilter().equals(filter2)) {
            file = fc.getSelectedFile();
            file = FileUtil.addExtensionIfNone(file, "sld");
            monitor(monitor, file);
            IOTools.saveSimbology_SLD2(file, layer);
            filePath = file.getAbsolutePath();
        }

        JOptionPane
                .showMessageDialog(
                        JUMPWorkbench.getInstance().getFrame(),
                        I18N.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved")
                                + ": " + filePath, getName(),
                        JOptionPane.PLAIN_MESSAGE);

        return;

    }

}