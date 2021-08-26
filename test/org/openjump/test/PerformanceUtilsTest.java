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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Benjamin Gudehus
 */
public class PerformanceUtilsTest {

    //-----------------------------------------------------------------------------------
    // TEST CASES.
    //-----------------------------------------------------------------------------------
    
    @Test
    public void start_time() {
        // expect: "generates time stamp"
        long startTime = PerformanceUtils.startTime();
        assertTrue(startTime <= System.nanoTime());
    }
    
    @Test
    public void stop_duration() throws InterruptedException {
        // expect: "stops duration"
        long startTime = PerformanceUtils.startTime();
        Thread.sleep(150);
        long duration = PerformanceUtils.stopDuration(startTime);
        assertEquals(150, duration / 1.0e6, 50.0);
    }
    
}
