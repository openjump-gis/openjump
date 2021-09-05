package com.vividsolutions.jump.geom;

import org.locationtech.jts.geom.Geometry;

public abstract class MakeValidOp {
  // If preserveGeomDim is true, the geometry dimension returned by MakeValidOp
  // must be the same as the inputGeometryType (degenerate components of lower
  // dimension are removed).
  // If preserveGeomDim is false MakeValidOp will preserve as much coordinates
  // as possible and may return a geometry of lower dimension or a
  // GeometryCollection if input geometry or geometry components have not the
  // required number of points.
  private boolean preserveGeomDim = true;

  // If preserveDuplicateCoord is true, MakeValidOp will preserve duplicate
  // coordinates as much as possible. Generally, duplicate coordinates can be
  // preserved for linear geometries but not for areal geometries (overlay
  // operations used to repair polygons remove duplicate points).
  // If preserveDuplicateCoord is false, all duplicated coordinates are removed.
  private boolean preserveDuplicateCoord = false;

  public MakeValidOp setPreserveGeomDim(boolean preserveGeomDim) {
    this.preserveGeomDim = preserveGeomDim;
    return this;
  }

  public boolean getPreserveGeomDim() {
    return preserveGeomDim;
  }

  public MakeValidOp setPreserveDuplicateCoord(boolean preserveDuplicateCoord) {
    this.preserveDuplicateCoord = preserveDuplicateCoord;
    return this;
  }

  public boolean getPreserveDuplicateCoord() {
    return preserveDuplicateCoord;
  }

  abstract public Geometry makeValid(Geometry geometry);

}
