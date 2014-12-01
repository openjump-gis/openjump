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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Benjamin Gudehus
 */
public final class PerformanceUtils {
    
    //-----------------------------------------------------------------------------------
    // CONSTRUCTORS.
    //-----------------------------------------------------------------------------------
    
    private PerformanceUtils() {
        throw new UnsupportedOperationException();
    }
    
    //-----------------------------------------------------------------------------------
    // STATIC METHODS.
    //-----------------------------------------------------------------------------------

    public static long startTime() {
        return System.nanoTime();
    }

    public static long stopDuration(long startTime) {
        return System.nanoTime() - startTime;
    }
    
    public static void printDuration(String message, long startTime) {
        String duration = formatDuration(stopDuration(startTime));
        System.out.println(message + ": " + duration + " sec");
    }
    
    //-----------------------------------------------------------------------------------
    // PRIVATE STATIC METHODS.
    //-----------------------------------------------------------------------------------
    
    private static String formatDuration(long nanoTime) {
        double seconds = nanoTime / 1.0e9;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat formatter = new DecimalFormat("0.000", symbols);
        return formatter.format(seconds);
    }
    
}
