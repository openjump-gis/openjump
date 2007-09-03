/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2007 Integrated Systems Analysts, Inc.
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
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.file;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JMenu;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.OpenProjectPlugIn;

public class OpenRecentProjectPlugIn extends OpenProjectPlugIn {
    private int taskNum = -1;
    private static final String OPEN_RECENT_TASK = I18N.get("org.openjump.core.ui.plugin.file.OpenRecentProjectPlugIn.Open-Recent-Task");
    private static final String NO_TASKS_ON_RECENT_LIST = I18N.get("org.openjump.core.ui.plugin.file.OpenRecentProjectPlugIn.No-Tasks-on-Recent-List");
    private static final String FILE_MENU = MenuNames.FILE;
    private static final String[] MENU_FILE_RECENT = new String[] { FILE_MENU, OPEN_RECENT_TASK};
    private static final String FILE_NOT_FOUND = "File not found: ";
    private OpenRecentProjectPlugIn[] openRecentProjectPlugInArray = 
    		new OpenRecentProjectPlugIn[OpenProjectPlugIn.RECENT_MENU_LIST_MAX];
    private static ArrayList recentList = null;
    
    public OpenRecentProjectPlugIn() {
    	this.taskNum = -1;  //default constructor disables list
    }
    public OpenRecentProjectPlugIn(int taskNum) {
    	this.taskNum = taskNum;
    }

    public String getName() {
        return OPEN_RECENT_TASK;
    }

    public void initialize(PlugInContext context) throws Exception {
        recentList = new ArrayList( //make a new copy that never changes
        		OpenProjectPlugIn.getRecentList(context.getWorkbenchContext()));
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
       if (recentList.size() == 0){
           featureInstaller.addMainMenuItem(this, FILE_MENU,
        		   OPEN_RECENT_TASK + "{pos:5}", null, 
           		new EnableCheck() {
		                public String check(JComponent component) {
		                    return  NO_TASKS_ON_RECENT_LIST;
		                }
           		}
           );
        }
        else {
        	//submenus need a little help overriding the default location at the end of the menu
        	JMenu fileMenu = featureInstaller.menuBarMenu(FILE_MENU);
            final JMenu recentMenu =new JMenu(OPEN_RECENT_TASK);
            fileMenu.insert(recentMenu, 5);
            //once the submenu has been created, the featureInstaller code will do the rest
          	for (int i = 0; i < recentList.size(); i++){
        		openRecentProjectPlugInArray[i] = new OpenRecentProjectPlugIn(i);
        		String path = (String) recentList.get(i);
        		featureInstaller.addMainMenuItem(openRecentProjectPlugInArray[i], 
        				MENU_FILE_RECENT,
        				path, false, null, new MultiEnableCheck());
        	}
        }
     }

    public boolean execute(PlugInContext context) throws Exception {
//    	ArrayList recentList = getRecentList(context.getWorkbenchContext());
    	if (taskNum<0) return false;
    	if (recentList == null) return false;
    	String fileName = (String) recentList.get(taskNum);
    	if (fileName == null) return false;
    	File file = new File(fileName);
    	if (file.exists()) {
    		execute(context, file);
        	return true;
    	}
    	else {
     		context.getWorkbenchFrame().
     			warnUser(FILE_NOT_FOUND + file.getAbsolutePath());
     		recentList.remove(taskNum);
    	}
    	return false;
    }
    
}