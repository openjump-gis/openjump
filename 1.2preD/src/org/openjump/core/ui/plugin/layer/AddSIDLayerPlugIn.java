
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */


package org.openjump.core.ui.plugin.layer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openjump.io.SIDLayer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.wms.MapLayer;

//modeled after the AddWMSQueryPlugIn
public class AddSIDLayerPlugIn extends AbstractPlugIn
{
	final static String sAddMrSIDLayer = I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.Add-MrSID-Layer");
	final static String sErrorSeeOutputWindow =I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.Error-See-Output-Window");
	final static String sNotInstalled=I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.not-installed");
	final static String sOpenMrSIDFile=I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.open-MrSID-file");
	final static String sFiles=I18N.get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.files");
	
    public static String WORKING_DIR;
    public static String ETC_PATH;
    public static String TMP_PATH;
    public static String MRSIDDECODE;
    public static String MRSIDINFO;

    public void initialize(PlugInContext context) throws Exception
    {
        context.getFeatureInstaller().addMainMenuItemWithJava14Fix(this,
        new String[] {MenuNames.LAYER}, sAddMrSIDLayer +"{pos:3}", false, null, this.createEnableCheck(context.getWorkbenchContext()));
        File empty = new File("");
        String sep = File.separator;
        WORKING_DIR = empty.getAbsoluteFile().getParent() + sep;
        ETC_PATH = WORKING_DIR + "etc" + sep;
        TMP_PATH = WORKING_DIR + "tmp" + sep;
        MRSIDDECODE = ETC_PATH + "mrsiddecode.exe";
        MRSIDINFO = ETC_PATH + "mrsidinfo.exe";
    }
    
    public boolean execute(final PlugInContext context) throws Exception
    {
        reportNothingToUndoYet(context);
        try
        {
            context.getWorkbenchFrame().getOutputFrame().createNewDocument();
            
            if (!new File(MRSIDDECODE).exists())
            {
                context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
                context.getWorkbenchFrame().getOutputFrame().addText(MRSIDDECODE + " " + sNotInstalled);
                return false;
            }
            
            if (!new File(MRSIDINFO).exists())
            {
                context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
                context.getWorkbenchFrame().getOutputFrame().addText(MRSIDINFO + " " + sNotInstalled);
                return false;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser = GUIUtil.createJFileChooserWithExistenceChecking();
            fileChooser.setDialogTitle(sOpenMrSIDFile);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(true);
            GUIUtil.removeChoosableFileFilters(fileChooser);
            FileFilter fileFilter = GUIUtil.createFileFilter("MrSID " + sFiles, new String[]{"sid"});
            fileChooser.addChoosableFileFilter(fileFilter);
            fileChooser.setFileFilter(fileFilter);
            
            if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(context.getWorkbenchFrame()))
            {
                List imageFilenames = new ArrayList();
                File[] files = fileChooser.getSelectedFiles();
                for(int i = 0; i < files.length; i++)
                {
                    String filename = files[i].getCanonicalPath();
                    imageFilenames.add(filename);
                }
                
                final SIDLayer layer = new SIDLayer(context, imageFilenames);
                execute(new UndoableCommand(getName())
                {
                    public void execute()
                    {
                        Collection selectedCategories = context.getLayerNamePanel().getSelectedCategories();
                        context.getLayerManager().addLayerable(selectedCategories.isEmpty()
                        ? StandardCategoryNames.WORKING
                        : selectedCategories.iterator().next().toString(), layer);
                    }
                    
                    public void unexecute()
                    {
                        context.getLayerManager().remove(layer);
                    }
                }, context);
                
                return true;
            }
            else
            {
                return false;
            }
        }
        
        catch (Exception e)
        {
            context.getWorkbenchFrame().warnUser(sErrorSeeOutputWindow);
            context.getWorkbenchFrame().getOutputFrame().addText("AddSIDLayerPlugIn Exception:" + e.toString());
            return false;
        }
    }
    
    public MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck());
    }    
}

