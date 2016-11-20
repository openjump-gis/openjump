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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.openjump.test.ReflectionUtils.privateField;

import java.io.File;
import java.util.HashMap;

import javax.swing.JInternalFrame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * @author Benjamin Gudehus
 */
public class TestToolsTest {
    
    //-----------------------------------------------------------------------------------
    // FIELDS.
    //-----------------------------------------------------------------------------------
    
    public static JUMPWorkbench workbench;
    public static WorkbenchContext workbenchContext;
    public static WorkbenchFrame workbenchFrame;
    
    //-----------------------------------------------------------------------------------
    // SETUP AND CLEANUP.
    //-----------------------------------------------------------------------------------
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        workbench = TestTools.buildWorkbench(new String[] {"-i18n", "en"});
        workbenchContext = workbench.getContext();
        workbenchFrame = workbench.getFrame();
        // TODO: Wait until frame is visible.
        // TODO: Refactor PlugIns so that a visible frame is not needed.
        workbenchFrame.setVisible(true);
    }
    
    @Before
    public void before() {
        //workbenchFrame.addTaskFrame();
    }
    
    @After
    public void after() {
        workbench.getBlackboard().getProperties().remove("parameter1");
        workbench.getBlackboard().getProperties().remove("parameter2");
        for (JInternalFrame frame : workbenchFrame.getInternalFrames()) {
            workbenchFrame.removeInternalFrame(frame);
        }
    }
    
    @AfterClass
    public static void afterClass() {
        workbenchFrame.setVisible(false);
        workbenchFrame.dispose();
    }
    
    //-----------------------------------------------------------------------------------
    // TEST CASES.
    //-----------------------------------------------------------------------------------
    
    @Ignore("currently broken")
    @Test
    public void testBuildWorkbench() {
        // expect: "Workbench contains WorkbenchFrame and WorkbenchContext"
        assertNotNull(workbench.getFrame());
        assertNotNull(workbench.getContext());
    }
    
    @Ignore("currently broken")
    @Test
    public void testOpenFile() {
        // when: "a shapefile is opened"
        TestTools.openFile(new File("src/fixtures/dissolve.jml"), workbench.getContext());
        
        // then: "layer manager contains one layer"
        LayerManager layerManager = workbench.getContext().getLayerManager();
        assertEquals(1, layerManager.getLayers().size());
        assertNotNull(layerManager.getLayer("dissolve"));
    }
    
    @Ignore("currently broken")
    @Test
    public void testOpenFileAgain() {
        // when: "a shapefile is opened again"
        TestTools.openFile(new File("src/fixtures/dissolve.jml"), workbench.getContext());
        
        // then: "layer manager contains one layer"
        LayerManager layerManager = workbench.getContext().getLayerManager();
        assertEquals(1, layerManager.getLayers().size());
        assertNotNull(layerManager.getLayer("dissolve"));
    }
   
    @Ignore("currently broken")
    @Test
    public void testConfigurePlugInWithFields() throws Exception {
        // given: "an example plugin with fields"
        PlugIn plugin = new ExamplePlugInWithFields();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter1", "foo");
        
        // when: "configure plugin with fields"
        TestTools.configurePlugIn(plugin, parameters);
        
        // then: "contains the field"
        assertEquals("foo", privateField(plugin, "parameter1"));
    }
    
    @Ignore("currently broken")
    @Test
    public void testConfigurePlugInWithDialog() throws Exception {
        // given: "an example plugin with dialog"
        PlugIn plugin = new ExamplePlugInWithDialog();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter1", "foo");
        
        // when: "configure plugin with dialog"
        TestTools.configurePlugIn(plugin, parameters, false);
        
        // then: "contains the dialog with parameters"
        MultiInputDialog dialog = (MultiInputDialog) privateField(plugin, "dialog");
        assertNotNull(dialog);
        assertEquals("foo", dialog.getText("parameter1"));
    }
    
    @Ignore("currently broken")
    @Test(expected=NoSuchFieldException.class)
    public void testConfigurePlugInWithoutFields() throws Exception {
        // given: "an example plugin without dialog"
        PlugIn plugin = new ExampleAbstractPlugIn();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter1", "foo");
        
        // when: "configure plugin without dialog"
        TestTools.configurePlugIn(plugin, parameters);
        
        // then: "complain gracefully that no field for parameters exists"
    }
    
    @Ignore("currently broken")
    @Test(expected=NoSuchFieldException.class)
    public void testConfigurePlugInWithoutDialog() throws Exception {
        // given: "an example plugin without dialog"
        PlugIn plugin = new ExampleAbstractPlugIn();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter1", "foo");
        
        // when: "configure plugin without dialog"
        TestTools.configurePlugIn(plugin, parameters, false);
        
        // then: "complain gracefully that no field for parameters exists"
    }
    
    @Ignore("currently broken")
    @Test
    public void testExecutePluginWithFields() throws Exception {
        // given: "a threaded plugin with parameters"
        PlugIn plugin = new ExamplePlugInWithFields();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter1", "foo");
        parameters.put("parameter2", 42);
        TestTools.configurePlugIn(plugin, parameters);
        
        // when: "the plugin is executed"
        TestTools.executePlugIn(plugin, workbench.getContext());
        
        // then: "a property was added to the blackboard"
        Blackboard blackboard = workbench.getContext().getBlackboard();
        assertEquals("foo", blackboard.get("parameter1"));
        assertEquals(42, blackboard.get("parameter2"));
    }
    
    @Ignore("currently broken")
    @Test
    public void testExecutePluginWithDialog() throws Exception {
        // given: "a threaded plugin with parameters"
        PlugIn plugin = new ExamplePlugInWithDialog();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("PARAMETER_1", "foo");
        parameters.put("PARAMETER_2", 42);
        TestTools.configurePlugIn(plugin, parameters, true);
        
        // when: "the plugin is executed"
        TestTools.executePlugIn(plugin, workbench.getContext());
        
        // then: "a property was added to the blackboard"
        Blackboard blackboard = workbench.getContext().getBlackboard();
        assertEquals("foo", blackboard.get("parameter1"));
        assertEquals(42, blackboard.get("parameter2"));
    }
    
    @Ignore("currently broken")
    @Test(expected=IllegalArgumentException.class)
    public void testExecutePluginWithoutThreadedPlugIn() throws Exception {
        // given: "an non-threaded plugin"
        PlugIn plugin = new ExampleAbstractPlugIn();
        
        // when: "the plugin is executed"
        TestTools.executePlugIn(plugin, workbench.getContext());
        
        // then: "an exception was thrown"
    }
    
    //-----------------------------------------------------------------------------------
    // TEST FIXTURES.
    //-----------------------------------------------------------------------------------
    
    /**
     * Fixture that outlines a plugin which accepts parameters in instance fields.
     * Both an user dialog and a map can be used to provide execution parameters.
     * 
     * <p><b>Configuration:</b> {@code #execute(PlugInContext)} shows a {@link
     * MultiInputDialog}, in which the user can set field values. When the dialog is 
     * closed these values are assigned as execution parameters to the instance fields 
     * {@code #parameter1} and {@code #parameter2}. The dialog is used only within
     * this method.
     * 
     * <p><b>Programmatic configuration:</b> Calling {@code #execute(PlugInContext)} 
     * can be omitted by providing the execution parameters directly to the instance 
     * fields using {@link TestTools#configurePlugIn(PlugIn, java.util.Map)}.
     * 
     * <p><b>Execution:</b> All operations take place in {@code #run(TaskMonitor, 
     * PlugInContext)} which uses the execution parameters. To execute operations
     * without the need of an user dialog use {@link TestTools#executePlugIn(PlugIn, 
     * WorkbenchContext)} after the parameters were configured.
     * 
     * @see TestTools#configurePlugIn(PlugIn, java.util.Map)
     * @see TestTools#executePlugIn(PlugIn, WorkbenchContext)
     */
    public static class ExamplePlugInWithFields extends AbstractPlugIn 
            implements ThreadedPlugIn {
        private MultiInputDialog dialog;
        private String parameter1 = "";
        private int parameter2 = 0;

        /** Configures plugin. */
        public boolean execute(PlugInContext context) throws Exception {
            dialog = new MultiInputDialog();
            dialog.addTextField("parameter1", "", 10, null, "");
            dialog.addIntegerField("parameter2", 0, 10, "");
            dialog.setVisible(true);
            parameter1 = dialog.getText("parameter1");
            parameter2 = dialog.getInteger("parameter2");
            return dialog.wasOKPressed();
        }
        
        /** Executes operations. */
        public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
            Blackboard blackboard = context.getWorkbenchContext().getBlackboard();
            blackboard.put("parameter1", parameter1);
            blackboard.put("parameter2", parameter2);
        }
    }
    
    /**
     * Fixture that outlines a plugin which accepts parameters in an user dialog.
     * Both an user dialog and a map can be used to provide execution parameters.
     * 
     * <p><b>Configuration:</b> {@code #execute(PlugInContext)} shows a {@link
     * MultiInputDialog}, in which the user can set field values. The dialog uses static 
     * fields with strings from {@link I18N} to name its fields. When the dialog is 
     * closed it contains the execution parameters to provide them for operations.
     * 
     * <p><b>Programmatic configuration:</b> Calling {@code #execute(PlugInContext)} 
     * can be omitted by providing the execution parameters directly as a new dialog
     * instance using {@link TestTools#configurePlugIn(PlugIn, java.util.Map, boolean)}.
     * The keys in the map are the names from the static {@code I18N} fields.
     * 
     * <p><b>Execution:</b> All operations take place in {@code #run(TaskMonitor, 
     * PlugInContext)} which uses the execution parameters provided by the dialog.
     * That means that this method has a dependency to the dialog. To execute operations
     * without showing the user dialog use {@link TestTools#executePlugIn(PlugIn, 
     * WorkbenchContext)} after the parameters in the dialog were configured.
     * 
     * <p>The only methods allowed to be called on the dialog within the {@code #run}
     * method are {@code getText()}, {@code getDouble()}, {@code getInteger()}, 
     * {@code getLayer()} and {@code getBoolean()} in order to use the operations 
     * without showing the user dialog.
     * 
     * @see TestTools#configurePlugIn(PlugIn, java.util.Map, boolean)
     * @see TestTools#executePlugIn(PlugIn, WorkbenchContext)
     */
    public static class ExamplePlugInWithDialog extends AbstractPlugIn 
            implements ThreadedPlugIn {
        private MultiInputDialog dialog;
        private final static String PARAMETER_1 = 
                I18N.get("ExamplePlugInWithDialog.param1");
        private final static String PARAMETER_2 = 
                I18N.get("ExamplePlugInWithDialog.param2");
        
        /** Configures plugin. */
        public boolean execute(PlugInContext context) throws Exception {
            dialog = new MultiInputDialog();
            dialog.addTextField(PARAMETER_1, "", 10, null, "");
            dialog.addIntegerField(PARAMETER_2, 0, 10, "");
            dialog.setVisible(true);
            return dialog.wasOKPressed();
        }
        
        /** Executes operations. */
        public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
            Blackboard blackboard = context.getWorkbenchContext().getBlackboard();
            blackboard.put("parameter1", dialog.getText(PARAMETER_1));
            blackboard.put("parameter2", dialog.getInteger(PARAMETER_2));
        }
    }
    
    /**
     * Fixture that outlines a simple plugin which does not use an user dialog.
     * 
     * <p><b>Configuration:</b> This fixture neither uses a dialog nor execution
     * parameters. However it may be possible to provide parameters using field 
     * setters or a constructor to use them in {@code #execute(PlugInContext)}.
     * 
     * <p><b>Execution:</b> All operations take place directly in {@code 
     * #execute(PlugInContext)}.
     */
    public static class ExampleAbstractPlugIn extends AbstractPlugIn {
        /** Executes operations. */
        public boolean execute(PlugInContext context) throws Exception {
            Blackboard blackboard = context.getWorkbenchContext().getBlackboard();
            blackboard.put("parameter1", "");
            blackboard.put("parameter2", 0);
            return true;
        }
    }
    
}
