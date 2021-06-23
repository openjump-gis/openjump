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

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class SaveProjectAsPlugIn extends AbstractSaveProjectPlugIn {
    
    public static final ImageIcon ICON = IconLoader.icon("disk_oj_dots.png");
    
    public static final String FILE_CHOOSER_DIRECTORY_KEY = 
        SaveProjectAsPlugIn.class.getName() + " - FILE CHOOSER DIRECTORY";
    
    public static final FileFilter JUMP_PROJECT_FILE_FILTER =
        GUIUtil.createFileFilter(I18N.getInstance().get("ui.plugin.SaveProjectAsPlugIn.jump-project-files"),
                                 new String[]{"jmp", "jcs"});
    
        
    private JFileChooser fileChooser;
    

    public SaveProjectAsPlugIn() {
      super();
      this.setShortcutKeys(KeyEvent.VK_S);
      this.setShortcutModifiers(KeyEvent.CTRL_MASK+KeyEvent.SHIFT_MASK);
    }

    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
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
        fileChooser.setDialogTitle(I18N.getInstance().get("ui.plugin.SaveProjectAsPlugIn.save-project"));
        GUIUtil.removeChoosableFileFilters(fileChooser);
        fileChooser.addChoosableFileFilter(JUMP_PROJECT_FILE_FILTER);
        fileChooser.addChoosableFileFilter(GUIUtil.ALL_FILES_FILTER);
        fileChooser.setFileFilter(JUMP_PROJECT_FILE_FILTER);
        Blackboard blackboard = PersistentBlackboardPlugIn.get(context.getWorkbenchContext());
        String dir = (String)blackboard.get(FILE_CHOOSER_DIRECTORY_KEY);
        if (dir != null && new File(dir).exists()) {
          try {
            fileChooser.setCurrentDirectory(new File(dir));
          } catch (IndexOutOfBoundsException e) {
            // eat it
          }
        }
    }
    
    public String getName() {
        return I18N.getInstance().get("ui.plugin.SaveProjectAsPlugIn.save-project-as");
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
        
        Collection<Layer> collection = ignoredLayers(context.getTask());
        if (collection.size() > 0) {
            // Starting with OpenJUMP 1.4.1beta (2011-04-20), the plugin uses
            // org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn
            // to give the user the possibility to save unsaved layers to HD
            // before saving the project
            new org.openjump.core.ui.plugin.file.SaveLayersWithoutDataSourcePlugIn()
            .execute(context, collection, FileUtil.removeExtensionIfAny(file));
        }
        
        file = FileUtil.addExtensionIfNone(file, "jmp");
        save(context.getTask(), file, context.getWorkbenchFrame());
        // Session-based persistence
        context.getWorkbenchContext()
               .getBlackboard()
               .put(FILE_CHOOSER_DIRECTORY_KEY, file.getAbsoluteFile().getParent());
        // File-based persistence
        PersistentBlackboardPlugIn.get(context.getWorkbenchContext())
                                  .put(FILE_CHOOSER_DIRECTORY_KEY, 
                                       file.getAbsoluteFile().getParent());
        return true;
    }
    
}