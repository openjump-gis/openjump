package com.vividsolutions.jump.workbench.imagery.mrsid;

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

import com.vividsolutions.jump.JUMPException;

public class SIDInfo {
  private String fileName;
  private int pixelWidth;
  private int pixelHeight;
  private double xres;
  private double xrot;
  private double yrot;
  private double yres;
  private double upper_left_x; // realworld coords
  private double upper_left_y; // realworld coords

  private int numLevels;
  private String colorSpace;

  SIDInfo(String fileName, int pixelWidth, int pixelHeight, double xres,
      double xrot, double yrot, double yres, double ulx, double uly,
      int numLevels, String colorSpace) {
    this.fileName = fileName;
    this.pixelWidth = pixelWidth;
    this.pixelHeight = pixelHeight;
    this.xres = xres;
    this.xrot = xrot;
    this.yrot = yrot;
    this.yres = yres;
    this.upper_left_x = ulx;
    this.upper_left_y = uly;
    this.numLevels = numLevels;
    this.colorSpace = colorSpace;
  }

  String getFileName() {
    return fileName;
  }

  int getPixelWidth() {
    return pixelWidth;
  }

  int getPixelHeight() {
    return pixelHeight;
  }

  double getXRes() {
    return xres;
  }

  double getXRot() {
    return xrot;
  }

  double getYRot() {
    return yrot;
  }

  double getYRes() {
    return yres;
  }

  double getUpperLeftX() {
    return upper_left_x;
  }

  double getUpperLeftY() {
    return upper_left_y;
  }

  int getNumLevels() {
    return numLevels;
  }

  String getColorSpace() {
    return colorSpace;
  }

  static SIDInfo readInfo(String sidFilename) throws JUMPException {
    int sidPixelWidth = 0;
    int sidPixelHeight = 0;
    double sid_xres = 1;
    double sid_xrot = 0;
    double sid_yrot = 0;
    double sid_yres = 1;
    double sid_ulx = 0; // realworld coords
    double sid_uly = 0; // realworld coords
    int maxLevel = 0;

    int numInfoItems = 0;
    String colorSpace = null;

    try {
      File file = File.createTempFile("mrsidinfo-", ".txt");
      String[] runStr = { MrSIDImageFactory.MRSIDINFO, sidFilename,
          // "-sid",
          "-quiet", "-log", file.getCanonicalPath() };

      Process p = Runtime.getRuntime().exec(runStr);
      p.waitFor();
      p.destroy();

      // if (!(file.exists() && file.isFile() && file.canRead()))
      // return null; //this could happen if mrsidinfo.exe couldn't produce a
      // file

      // read the info
      FileReader fin = new FileReader(file);
      BufferedReader in = new BufferedReader(fin);
      String lineIn = in.readLine();

      while (in.ready()) {
        String value = "";

        if (lineIn.indexOf("width:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          sidPixelWidth = Integer.parseInt(value.trim());
          numInfoItems++;
        }

        if (lineIn.indexOf("height:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          sidPixelHeight = Integer.parseInt(value.trim());
          numInfoItems++;
        }

        if (lineIn.indexOf("number of levels:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          maxLevel = Integer.parseInt(value.trim());
          numInfoItems++;
        }

        if (lineIn.indexOf("X UL:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          // mrsidgeoinfo uses the locale for number formatting
          // hence on a german os it produces 1234,567 values
          sid_ulx = Double.parseDouble(value.trim().replace(',', '.'));
          numInfoItems++;
        }

        if (lineIn.indexOf("Y UL:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          sid_uly = Double.parseDouble(value.trim().replace(',', '.'));
          numInfoItems++;
        }

        if (lineIn.indexOf("X res:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          sid_xres = Double.parseDouble(value.trim().replace(',', '.'));
          numInfoItems++;
        }

        if (lineIn.indexOf("Y res:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          sid_yres = Double.parseDouble(value.trim().replace(',', '.'));
          numInfoItems++;
        }

        if (lineIn.indexOf("color space:") != -1) {
          value = lineIn.substring(lineIn.indexOf(":") + 1);
          colorSpace = value.trim();
          numInfoItems++;
        }

        lineIn = in.readLine();
      }

      in.close();
      fin.close();
      file.delete();

      if (numInfoItems == 8)
        return new SIDInfo(sidFilename, sidPixelWidth, sidPixelHeight,
            sid_xres, sid_xrot, sid_yrot, sid_yres, sid_ulx, sid_uly, maxLevel,
            colorSpace);
      else
        return null;

    } catch (Throwable t) {
      throw new JUMPException(t);
    }
  }
}
