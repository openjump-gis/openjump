/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for 
 * visualizing and manipulating spatial features with geometry and attributes.
 * Copyright (C) 2012  The JUMP/OpenJUMP contributors
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation, either version 2 of the License, or (at your option) 
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for 
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openjump.test;

import static org.openjump.test.ReflectionUtils.privateField;
import static org.openjump.test.ReflectionUtils.privateStaticField;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import org.openjump.OpenJumpConfiguration;
import org.openjump.core.ui.plugin.file.OpenFilePlugIn;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.commandline.CommandLine;
import com.vividsolutions.jump.workbench.JUMPConfiguration;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Setup;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.SplashPanel;
import com.vividsolutions.jump.workbench.ui.SplashWindow;

/**
 * @author Benjamin Gudehus
 */
public final class TestTools {
    
    //-----------------------------------------------------------------------------------
    // CONSTRUCTORS.
    //-----------------------------------------------------------------------------------
    
    private TestTools() {
        throw new UnsupportedOperationException();
    }
    
    //-----------------------------------------------------------------------------------
    // STATIC METHODS.
    //-----------------------------------------------------------------------------------
    
    /**
     * Builds a new Workbench with WorkbenchFrame and WorkbenchContext.
     * 
     * @see JUMPWorkbench#main
     * @return JUMPWorkbench
     */
    public static JUMPWorkbench buildWorkbench(String[] args) throws Exception {
        // Configure a SplashPanel.
        // TODO: (DONE) Do not show the splash window on startup.
        String title = "OpenJUMP";
        SplashPanel splashPanel = new SplashPanel(JUMPWorkbench.splashImage(), title);
        SplashWindow splashWindow = new SplashWindow(splashPanel);
        //splashWindow.setVisible(true);
        
        // Create a new Workbench with WorkbenchFrame and WorkbenchContext.
        TaskMonitor monitor = new DummyTaskMonitor();
        Setup setup = new JUMPConfiguration();
        privateStaticField(JUMPWorkbench.class, "commandLine", new CommandLine());
        //JUMPWorkbench.main(args, title, setup, splashPanel, monitor);
        JUMPWorkbench workbench = new JUMPWorkbench(title, args, splashWindow, monitor);
        
        // Setup Workbench.
        setup.setup(workbench.getContext());
        OpenJumpConfiguration.postExtensionInitialization(workbench.getContext());
        return workbench;
    }
    
    /**
     * Opens a geometric fixture in the task panel.
     * 
     * @param file File
     * @param context Context.
     */
    public static void openFile(File file, WorkbenchContext context) {
        OpenFilePlugIn filePlugin = new OpenFilePlugIn(context, file);
        filePlugin.actionPerformed(new ActionEvent(filePlugin, 0, ""));
    }
    
    public static void installPlugIn(PlugIn plugin, WorkbenchContext context) 
            throws Exception {
        PlugInContext plugInContext = context.createPlugInContext();
        plugin.initialize(plugInContext);
    }
    
    /**
     * Configures execution parameters for {@link PlugIn} using its instance fields.
     * 
     * @param plugin Plugin.
     * @param parameters Execution parameters.
     */
    public static void configurePlugIn(PlugIn plugin, Map<String, Object> parameters)
            throws Exception {
        for (String key : parameters.keySet()) {
            privateField(plugin, key, parameters.get(key));
        }
    }

    /**
     * Configures execution parameters for {@link PlugIn} using a new user dialog.
     * 
     * @param plugin Plugin.
     * @param parameters Execution parameters.
     * @param retrieveFieldNamesFromPlugIn Retrieve {@link I18N} string names.
     */
    public static void configurePlugIn(PlugIn plugin, Map<String, Object> parameters, 
            boolean retrieveFieldNamesFromPlugIn) throws Exception {
        DialogParameters dialogParameters = new DialogParameters();
        for (String key : parameters.keySet()) {
            Object fieldValue = parameters.get(key);
            String fieldName = key;
            if (retrieveFieldNamesFromPlugIn) {
                fieldName = (String) privateStaticField(plugin.getClass(), fieldName);
            }
            dialogParameters.putField(fieldName, fieldValue);
        }
        // TODO: Throw specific exception if plugin has no field "dialog".
        privateField(plugin, "dialog", dialogParameters);
    }
    
    /**
     * Executes operations of the {@link Plugin}.
     * 
     * @param plugin Plugin.
     * @param context Context.
     * @see com.vividsolutions.jump.workbench.plugin.AbstractPlugIn#toActionListener
     */
    public static void executePlugIn(PlugIn plugin, WorkbenchContext context) 
            throws Exception {
        TaskMonitor taskMonitor = new DummyTaskMonitor();
        PlugInContext plugInContext = context.createPlugInContext();
        // TODO: Start UndoableEditReceiver (see AbstractPlugIn.toActionListener).
        //AbstractPlugIn.toActionListener(plugin, context, taskMonitorManager);
        if (plugin instanceof ThreadedPlugIn) {
            // TODO: Wait until plugin has finished.
            ((ThreadedPlugIn) plugin).run(taskMonitor, plugInContext);
        }
        else {
            String message = "Please use PlugIn.execute(context) directly.";
            throw new IllegalArgumentException(message);
        }
    }
    
    //-----------------------------------------------------------------------------------
    // MAIN METHOD.
    //-----------------------------------------------------------------------------------
    
    public static void main(String[] args) throws Exception {
        final JUMPWorkbench workbench = TestTools.buildWorkbench(args);
        workbench.getFrame().addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent event) {
                TestTools.openFile(new File("share/dissolve.shp"), 
                        workbench.getContext());
            }
        });
        workbench.getFrame().setVisible(true);
    }    
    
}
