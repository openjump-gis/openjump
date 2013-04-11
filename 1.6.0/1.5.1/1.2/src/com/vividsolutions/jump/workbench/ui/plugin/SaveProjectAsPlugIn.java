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
package com.vividsolutions.jump.workbench.ui.plugin;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
public class SaveProjectAsPlugIn extends AbstractSaveProjectPlugIn {
    public static final FileFilter JUMP_PROJECT_FILE_FILTER = GUIUtil
            .createFileFilter(I18N.get("ui.plugin.SaveProjectAsPlugIn.jump-project-files"), new String[]{"jmp", "jcs"});
    private JFileChooser fileChooser;
    public void initialize(PlugInContext context) throws Exception {
        //Don't initialize fileChooser at field declaration; otherwise get
        // intermittent
        //exceptions:
        //java.lang.NullPointerException
        //        at javax.swing.ImageIcon.<init>(ImageIcon.java:161)
        //        at javax.swing.ImageIcon.<init>(ImageIcon.java:147)
        //        at
        // com.sun.java.swing.plaf.windows.WindowsFileChooserUI$ShortCutPanel.<init>(WindowsFileChooserUI.java:603)
        //        at
        // com.sun.java.swing.plaf.windows.WindowsFileChooserUI.installComponents(WindowsFileChooserUI.java:361)
        //        at
        // javax.swing.plaf.basic.BasicFileChooserUI.installUI(BasicFileChooserUI.java:130)
        //        at
        // com.sun.java.swing.plaf.windows.WindowsFileChooserUI.installUI(WindowsFileChooserUI.java:176)
        //        at javax.swing.JComponent.setUI(JComponent.java:449)
        //        at javax.swing.JFileChooser.updateUI(JFileChooser.java:1701)
        //        at javax.swing.JFileChooser.setup(JFileChooser.java:345)
        //        at javax.swing.JFileChooser.<init>(JFileChooser.java:320)
        //[Jon Aquino 2004-01-12]
        fileChooser = GUIUtil.createJFileChooserWithOverwritePrompting("jmp");
        fileChooser.setDialogTitle(I18N.get("ui.plugin.SaveProjectAsPlugIn.save-project"));
        GUIUtil.removeChoosableFileFilters(fileChooser);
        fileChooser.addChoosableFileFilter(JUMP_PROJECT_FILE_FILTER);
        fileChooser.addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
        fileChooser.setFileFilter(JUMP_PROJECT_FILE_FILTER);
    }
    public String getName() {
        return I18N.get("ui.plugin.SaveProjectAsPlugIn.save-project-as");
    }
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        if (context.getTask().getProjectFile() != null) {
            fileChooser.setSelectedFile(context.getTask().getProjectFile());
        }
        if (JFileChooser.APPROVE_OPTION != fileChooser.showSaveDialog(context
                .getWorkbenchFrame())) {
            return false;
        }
        File file = fileChooser.getSelectedFile();
        file = FileUtil.addExtensionIfNone(file, "jmp");
        save(context.getTask(), file, context.getWorkbenchFrame());
        return true;
    }
}