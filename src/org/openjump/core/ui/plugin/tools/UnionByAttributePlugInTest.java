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
package org.openjump.core.ui.plugin.tools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;

import javax.swing.JInternalFrame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openjump.test.TestTools;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.PlugIn;

/**
 * @author Benjamin Gudehus
 */
public class UnionByAttributePlugInTest {
    
    //-----------------------------------------------------------------------------------
    // FIELDS.
    //-----------------------------------------------------------------------------------
    
    public static JUMPWorkbench workbench;
    
    //-----------------------------------------------------------------------------------
    // SETUP AND CLEANUP.
    //-----------------------------------------------------------------------------------
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        workbench = TestTools.buildWorkbench(new String[] {"-i18n", "en"});
        // TODO: Wait until frame is visible.
        // TODO: Refactor PlugIns so that a visible frame isn't needed.
        workbench.getFrame().setVisible(true);
    }
    
    @Before
    public void before() {
        //workbench.getFrame().addTaskFrame();
    }
    
    @After
    public void after() {
        for (JInternalFrame frame : workbench.getFrame().getInternalFrames()) {
            workbench.getFrame().removeInternalFrame(frame);
        }
    }
    
    @AfterClass
    public static void afterClass() {
        workbench.getFrame().setVisible(false);
        workbench.getFrame().dispose();
    }
    
    //-----------------------------------------------------------------------------------
    // TEST CASES.
    //-----------------------------------------------------------------------------------

    @Ignore("currently broken")
    @Test
    public void testAddedResultLayer() throws Exception {
        // given: "a loaded shapefile fixture"
        File fixture = new File("src/fixtures/dissolve.jml");
        TestTools.openFile(fixture, workbench.getContext());
        
        // and: "plugin with dialog values"
        PlugIn plugin = new UnionByAttributePlugIn();
        LayerManager layerManager = workbench.getContext().getLayerManager();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("layer", layerManager.getLayer("dissolve"));
        parameters.put("use_attribute", true);
        parameters.put("attribute", "LABEL");
        parameters.put("ignore_empty", false);
        parameters.put("merge_linestrings", false);
        parameters.put("aggregate_unused_fields", false);
        TestTools.configurePlugIn(plugin, parameters);
        
        // when: "union by attribute is called"
        TestTools.executePlugIn(plugin, workbench.getContext());
        
        // then: "layer manager contains the source and result layer" 
        assertEquals(2, layerManager.getLayers().size());
        //Thread.sleep(Integer.MAX_VALUE);
    }
    
}
