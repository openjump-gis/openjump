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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;

import javax.swing.JInternalFrame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjump.test.PerformanceUtils;
import org.openjump.test.TestTools;

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.PlugIn;

/**
 * @author Benjamin Gudehus
 * @author Michaël Michaud
 */
public class DeleteDuplicateGeometriesPlugInTest {
    
    //-----------------------------------------------------------------------------------
    // FIELDS.
    //-----------------------------------------------------------------------------------
    
    public static JUMPWorkbench workbench;
    
    public PlugIn plugin;
    
    //-----------------------------------------------------------------------------------
    // FIXTURE METHODS.
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
        plugin = new DeleteDuplicateGeometriesPlugIn();
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
    // FEATURE METHODS.
    //-----------------------------------------------------------------------------------
    
    @Test
    public void remove_duplicate_geometries() throws Exception {
        // given: "a loaded shapefile fixture"
        File fixture = new File("src/fixtures/delete-duplicate-geometries.jml");
        TestTools.openFile(fixture, workbench.getContext());
        LayerManager layerManager = workbench.getContext().getLayerManager();

        // and: "a configured plugin"
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("confSourceLayer", layerManager.getLayer("delete-duplicate-geometries"));
        params.put("confDeleteOnlySameAttributes", false);
        TestTools.configurePlugIn(plugin, params);
        
        // when: "processed a dataset with 41 features, including 17 duplicates"
        long startTime = PerformanceUtils.startTime();
        TestTools.executePlugIn(plugin, workbench.getContext());
        PerformanceUtils.printDuration("duplicates", startTime);
        
        // then: "results with 24 features"
        assertEquals(24, layerManager.getLayer("delete-duplicate-geometries-cleaned")
        		                     .getFeatureCollectionWrapper()
        		                     .size());
        //TestTools.installPlugIn(plugin, workbench.getContext());
        //Thread.sleep(Integer.MAX_VALUE);
    }
    
    @Test
    public void remove_duplicate_geometries_with_same_attributes() throws Exception {
        // given: "a loaded shapefile fixture"
        File fixture = new File("src/fixtures/delete-duplicate-geometries.jml");
        TestTools.openFile(fixture, workbench.getContext());
        LayerManager layerManager = workbench.getContext().getLayerManager();
        
        // and: "a configured plugin"
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("confSourceLayer", layerManager.getLayer("delete-duplicate-geometries"));
        params.put("confDeleteOnlySameAttributes", true);
        TestTools.configurePlugIn(plugin, params);
        
        // when: "processed a dataset with 41 features, including 8 strict duplicates"
        long startTime = PerformanceUtils.startTime();
        TestTools.executePlugIn(plugin, workbench.getContext());
        PerformanceUtils.printDuration("strict duplicates", startTime);
        
        // then: "results with 33 features"
        assertEquals(33, layerManager.getLayer("delete-duplicate-geometries-cleaned")
                .getFeatureCollectionWrapper()
                .size());
        //Thread.sleep(Integer.MAX_VALUE);
    }
    
    @Test
    public void configuration_parameters() throws Exception {
        // expect: "sourceLayer can be set to a layer"
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("confSourceLayer", new Layer());
        TestTools.configurePlugIn(plugin, params);
        
        // and: "deleteOnlySameAttributes can be set to boolean"
        params.put("confDeleteOnlySameAttributes", false);
        TestTools.configurePlugIn(plugin, params);
    }
    
    @Test
    public void result_layer_name_and_category() throws Exception {
        // given: "a loaded shapefile fixture"
        File fixture = new File("src/fixtures/delete-duplicate-geometries.jml");
        TestTools.openFile(fixture, workbench.getContext());
        LayerManager layerManager = workbench.getContext().getLayerManager();
        
        // and: "a configured plugin"
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("confSourceLayer", layerManager.getLayer("delete-duplicate-geometries"));
        params.put("confDeleteOnlySameAttributes", false);
        TestTools.configurePlugIn(plugin, params);
        
        // when: "the dataset is processed"
        TestTools.executePlugIn(plugin, workbench.getContext());
        
        // then: "the result layer has a correct name"
        Layer resultLayer = layerManager.getLayer("delete-duplicate-geometries-cleaned");
        assertNotNull(resultLayer);
        
        // and: "the result layer is in the result category"
        Category category = layerManager.getCategory(resultLayer);
        assertEquals(StandardCategoryNames.RESULT, category.getName());
    }
    
    
}
