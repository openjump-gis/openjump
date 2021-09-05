package com.vividsolutions.jump.geom;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryFixer;

public class JtsMakeValidOp extends MakeValidOp {

  public Geometry makeValid(Geometry geometry) {
    GeometryFixer fixer = new GeometryFixer(geometry);
    fixer.setKeepCollapsed(!this.getPreserveGeomDim());
    return fixer.getResult();
  }

}
