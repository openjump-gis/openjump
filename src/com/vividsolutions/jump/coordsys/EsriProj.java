package com.vividsolutions.jump.coordsys;

import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.Logger;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class EsriProj {

  // A cache to remember already used projection string
  public static Map<Integer,String> PROJMAP = new HashMap<>();
  // A cache to remember already used projection ids
  public static Map<String,Integer> CODEMAP = new HashMap<>();

  static File resource = null;
  static {
    try {
      File wb = new File(EsriProj.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      resource = new File(wb.getParent(), "resources/coord_ref_sys/pe_list_projcs_geogcs.zip");
    } catch(URISyntaxException e) {
      Logger.warn(e);
    }
  }
  private static String entryName = "pe_list_projcs_geogcs.csv";


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
      InputStream fis = new FileInputStream(resource);
      InputStream is = CompressedFile.getUncompressedStream(fis, entryName);
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
