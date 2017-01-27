package com.vividsolutions.jump.workbench.imagery.graphic;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class WorldFile {

	public static final WorldFile DEFAULT = new WorldFile();
	
	private String filename;
	private float xSize = 1;
	private float ySize = -1;
	private float rowRotation = 0;
	private float colRotation = 0;
	
	private double xUpperLeft = 0.5;
	private double yUpperLeft = -0.5;

	private WorldFile(){
	}
	
	/**
	 * bpw: bip, bmp
	 * gfw: gif
	 * tfw: tif
	 * jgw: jpg
	 * 
	 * wld: universal
	 * 
	 * @param file
	 * @return a WorldFile
	 */
	public static WorldFile read(File file) throws IOException{

        FileReader fin = new FileReader(file);
        BufferedReader in = new BufferedReader(fin);
        String lineIn = in.readLine();
        int line = 0;
        
        WorldFile wf = new WorldFile();
        wf.filename = file.getPath();
        
        while((in.ready() || lineIn!=null) && line < 6){
        	if(lineIn!=null && !"".equals(lineIn)){
	        	switch(line){
	        	case 0:
	        		wf.xSize = Float.valueOf(lineIn.trim()).floatValue();
	        		break;
	        	case 1:
	        		wf.rowRotation = Float.valueOf(lineIn.trim()).floatValue();
	        		break;
	        	case 2:
	        		wf.colRotation = Float.valueOf(lineIn.trim()).floatValue();
	        		break;
	        	case 3:
	        		wf.ySize = Float.valueOf(lineIn.trim()).floatValue();
	        		break;
	        	case 4:
	        		wf.xUpperLeft = Double.valueOf(lineIn.trim()).doubleValue();
	        		break;
	        	case 5:
	        		wf.yUpperLeft = Double.valueOf(lineIn.trim()).doubleValue();
	        		break;
	        	}
        	}
        	line++;
        	lineIn = null;
        	if(in.ready())
        		lineIn = in.readLine();
        }
        
        return wf;
	}

	public float getColRotation() {
		return colRotation;
	}

	public String getFilename() {
		return filename;
	}

	public float getRowRotation() {
		return rowRotation;
	}

	public float getXSize() {
		return xSize;
	}

	public double getXUpperLeft() {
		return xUpperLeft;
	}

	public float getYSize() {
		return ySize;
	}

	public double getYUpperLeft() {
		return yUpperLeft;
	}
}
