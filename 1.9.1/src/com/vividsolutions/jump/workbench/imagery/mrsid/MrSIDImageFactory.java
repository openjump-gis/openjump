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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;

import org.openjump.core.CheckOS;
import org.openjump.util.UriUtil;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPException;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;

public class MrSIDImageFactory implements ReferencedImageFactory {

  public static final String MRSIDDECODE = "mrsidgeodecode";
  public static final String MRSIDINFO = "mrsidgeoinfo";
  private static HashMap<String, String> binariesAvailable = new HashMap();

  final static String sNotInstalled = I18N
      .get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.not-installed");
  final static String sErrorSeeOutputWindow = I18N
      .get("org.openjump.core.ui.plugin.layer.AddSIDLayerPlugIn.Error-See-Output-Window");

  public String getTypeName() {
    return "MrSID";
  }

  public ReferencedImage createImage(String location) throws Exception {
    URI uri = new URI(location);
    if (CompressedFile.isArchive(uri) || CompressedFile.isCompressed(uri))
      throw new JUMPException("Compressed files not supported for this format.");
    
    String filepath = new File( UriUtil.getFilePath(uri) ).getAbsolutePath();
    return new MrSIDReferencedImage(SIDInfo.readInfo(filepath), filepath);
  }

  public String getDescription() {
    return getTypeName();
  }

  public String[] getExtensions() {
    return new String[] { "sid" };
  }

  public boolean isEditableImage(String location) {
    return false;
  }

  public boolean isAvailable(WorkbenchContext context) {

    String msg = which(MRSIDDECODE);
    if (msg != "") {
      context
          .getWorkbench()
          .getFrame()
          .log(MRSIDDECODE + " " + sNotInstalled + " - " + msg, this.getClass());
      return false;
    }

    msg = which(MRSIDINFO);
    if (msg != "") {
      context.getWorkbench().getFrame()
          .log(MRSIDINFO + " " + sNotInstalled + " - " + msg, this.getClass());
      return false;
    }

    JUMPWorkbench.getInstance().getFrame()
        .log("found Mrsid binaries in path", this.getClass());
    return true;
  }

  // return empty string on success or error string
  private String which(String filename) {
    // return cached result
    if (binariesAvailable.containsKey(filename))
      return binariesAvailable.get(filename);

    String[] runStr;
    if (CheckOS.isLinux()) {
      runStr = new String[] { "which", filename };
    } else if (CheckOS.isWindows()) {
      runStr = new String[] {
          "cmd",
          "/C",
          "@for %i in (" + filename
              + ".exe) do @if NOT \"%~$PATH:i\"==\"\" echo %~$PATH:i" };
    } else {
      return "os not supported.";
    }

    Process p = null;
    BufferedReader in = null;
    try {
      p = Runtime.getRuntime().exec(runStr);
      p.waitFor();
      in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      if ((line = in.readLine()) != null && !line.trim().isEmpty()) {
        // cache result
        binariesAvailable.put(filename, "");
        return "";
      }
//      System.out.println(line);
      binariesAvailable.put(filename, filename + " not in path");
      return filename + " not in path";
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    } finally {
      // cleanup
      try {
        if (in != null)
          in.close();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (p != null)
          p.destroy();
      }
    }

  }

}