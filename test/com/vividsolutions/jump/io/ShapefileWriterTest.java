package com.vividsolutions.jump.io;

import com.vividsolutions.jump.coordsys.EsriProj;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;


public class ShapefileWriterTest {


  @Test
  public void testFindProjInPe_list_projcs_geogcs() throws Exception {
    EsriProj.setProjFile(new File("lib/ext/coord_ref_sys/pe_list_projcs_geogcs.zip"),
        "pe_list_projcs_geogcs.csv");
    String proj = new ShapefileWriter().getPrjString("3147");
    //    String proj = EsriProj.findProj(3147);
    System.out.println(proj);
    Assert.assertNotNull(proj);
  }

  @Test
  public void testFindProjInPe_list_projcs_geogc2() throws Exception {
    EsriProj.setProjFile(new File("lib/ext/coord_ref_sys/pe_list_projcs_geogcs.zip"),
        "pe_list_projcs_geogcs.csv");
    String proj = new ShapefileWriter().getPrjString("2046");
    //    EsriProj.setProjFile(new File("lib/ext/coord_ref_sys/pe_list_projcs_geogcs.zip"),
    //        "pe_list_projcs_geogcs.csv");
    //    String proj = EsriProj.findProj(2046);
    System.out.println(proj);
    // 2046 is not in pe_list_projcs_geogcs.csv but can be found from alternative file
    // srid2prj.txt by EsriProj
    Assert.assertNotNull(proj);
  }
}
