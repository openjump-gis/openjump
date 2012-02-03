package org.openjump.core.ui.plugin.file;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;

public class WorldFileWriter {


    public static void writeWorldFile(File imageFile, LayerViewPanel panel ) throws IOException {
    	
    	if ((panel == null)||  (imageFile == null))
    		throw new NullPointerException("WorldFileWriter");
 
        double xrot = 0d; // rotation about y-axis  - always 0
        double yrot = 0d; // rotation about x-axis  - always 0
 
        Viewport viewport = panel.getViewport();
        Envelope vpEnvelope = viewport.getEnvelopeInModelCoordinates();
        
        double view_res = 1 / viewport.getScale(); //panel resolution
        double xres = view_res;   // pixel size in the x-direction in map units/pixel
        double yres = -view_res;  // pixel size in the y-direction in map units, almost always negative
        double halfPixel = 0.5 * view_res;
        double ulx = vpEnvelope.getMinX() + halfPixel;  // x-coordinate of the center of the upper left pixel
        double uly = vpEnvelope.getMaxY() - halfPixel;  // y-coordinate of the center of the upper left pixe
               
    	PrintWriter outputStream = null;
          	
    	try {
    		String imagePath = imageFile.getCanonicalPath();
    		int dotPos = imagePath.lastIndexOf(".");
    		String worldExtention = (imagePath.substring(dotPos).equalsIgnoreCase(".jpg")) ? ".jgw" : ".pgw";
    		String worldPath = imagePath.substring(0, dotPos) + worldExtention;
    		
    		outputStream = new PrintWriter(new FileWriter(worldPath));
    		
    		outputStream.println(xres);
    		outputStream.println(xrot);
    		outputStream.println(yrot);
    		outputStream.println(yres);
    		outputStream.println(ulx);
    		outputStream.println(uly);
    	} finally {
    		if (outputStream != null) {
    			outputStream.close();
    		}
    	}
    }

}
