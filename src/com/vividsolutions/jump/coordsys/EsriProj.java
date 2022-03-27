package com.vividsolutions.jump.coordsys;

import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.*;
import java.util.*;

/**
 * This class is used to find the precise Esri prj string for a given srid.
 *
 * Prj strings are taken from https://github.com/Esri/projection-engine-db-doc,
 * licensed under the Apache License, Version 2.0 (the "License").
 *
 * This class uses the two following files from the github repository :
 * <ul>
 *   <li>/csv/pe_list_projcs.csv</li>
 *   <li>/csv/pe_list_geogcs.csv</li>
 * </ul>
 * The two files have been concatenated in a single file and zipped in a single file
 * containing a single entry named pe_list_projcs_geogcs.csv.
 *
 * The file is not decompressed during installation process. Searching a prj from an id
 * fast enough (< 1s including the decompression step). Once a prj has been searched, it
 * is cached in a map and the second search takes no time.
 *
 * There is room for improvement : the Esri file contains also bounding boxes for each
 * projection and the wkt2 version of its description. A reverse search function (from
 * string to id) could be implemented).
 */
public class EsriProj {

  // A cache to remember already used projection string
  public static Map<Integer,String> PROJMAP = new HashMap<>();
  // A cache to remember already used projection ids
  public static Map<String,Integer> CODEMAP = new HashMap<>();

  private static final File projfile = JUMPWorkbench.getInstance().getPlugInManager()
      .findFileOrFolderInExtensionDirs("coord_ref_sys/pe_list_projcs_geogcs.zip");
  private static final String entryName = "pe_list_projcs_geogcs.csv";


  public static void main(String[] args) throws IOException, ArchiveException {
    System.out.println(findProj(2154));
    System.out.println(findProj(2154));
  }


  private static String[] tokenize(String line) {
    List<String> tokens = new ArrayList<>(15);
    boolean quoted = false;
    StringBuilder sb = new StringBuilder();
    char[] array = line.toCharArray();
    for (int i = 0 ; i < array.length ; i++) {
      char c = array[i];
      if (c == ',' && !quoted) {
        tokens.add(sb.toString());
        sb.setLength(0);
      } else if (c == ' ' && !quoted) {
        // pass
      } else if (c == '"' && array[i+1] != '"') {
        quoted = !quoted;
      } else {
        sb.append(c);
        if (c == '"' && array[i+1] != '"') i++;
      }
    }
    tokens.add(sb.toString());
    return tokens.toArray(new String[0]);
  }

  public static String findProj(final int id) throws IOException, ArchiveException {
    //long t0 = System.currentTimeMillis();
    String proj = PROJMAP.get(id);
    if (proj == null) proj = PROJMAP.get(id);
    if (proj == null) {
      //InputStream fis = new FileInputStream(projfile);
      //InputStream is = CompressedFile.getUncompressedStream(fis, entryName);
      InputStream is = CompressedFile.openFile(projfile.getPath(), entryName);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      Optional<String[]> tokens = br.lines()
          .map(EsriProj::tokenize)
          .filter(it -> it[0].equals("" + id) || it[1].equals("" + id))
          .findFirst();
      if (tokens.isPresent()) {
        String[] r = tokens.get();
        PROJMAP.put(id, r[4]);
        proj = r[4];
      }
    }
    //System.out.println("found in " + (System.currentTimeMillis()-t0) + " ms");
    if (proj != null)
      Logger.info("Found Esri prj file for srid " + id);
    else
      Logger.warn("No Esri prj file found for srid " + id);
    return proj;
  }

}
