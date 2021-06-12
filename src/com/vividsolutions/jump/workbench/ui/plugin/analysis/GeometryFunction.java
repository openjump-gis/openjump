
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

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.linemerge.LineSequencer;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.locationtech.jts.simplify.VWSimplifier;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.algorithm.Densifier;
import com.vividsolutions.jump.geom.AbstractGeometryProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A function object for {@link Geometry} functions (which return a Geometry).
 * Provides metadata about the function.
 *
 * @author Martin Davis
 * @version 1.0
 */
public abstract class GeometryFunction
{
  // [sstein, 16.07.2006] due to language setting problems loaded in corresponding class
  /*
  private static final String METHOD_INTERSECTION = I18N.get("ui.plugin.analysis.GeometryFunction.intersection");
  private static final String METHOD_UNION = I18N.get("ui.plugin.analysis.GeometryFunction.union");
  private static final String METHOD_DIFFERENCE_AB = I18N.get("ui.plugin.analysis.GeometryFunction.difference-a-b");
  private static final String METHOD_DIFFERENCE_BA = I18N.get("ui.plugin.analysis.GeometryFunction.difference-b-a");
  private static final String METHOD_SYMDIFF = I18N.get("ui.plugin.analysis.GeometryFunction.symetric-difference");
  private static final String METHOD_CENTROID_A = I18N.get("ui.plugin.analysis.GeometryFunction.centroid-of-a");
  static final String METHOD_BUFFER = I18N.get("ui.plugin.analysis.GeometryFunction.buffer");
  
  private static final String sFunction = I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.function");
  */  
  private static GeometryFunction[] methods = {
    new IntersectionFunction(),
    new UnionFunction(),
    new DifferenceABFunction(),
    new DifferenceBAFunction(),
    new SymDifferenceFunction(),
    new CentroidFunction(),
    new InteriorPointFunction(),
    new BufferFunction(),
    new SimplifyFunction(),
    new SimplifyTopologyFunction(),
    new SimplifyVWFunction(),
    new RemoveSmallSegmentsFunction(),
    new DensifyFunction(),
    new ConvexHullFunction(),
    new BoundaryFunction(),
    new EnvelopeFunction(),
    new WrapIntoMultiFunction(),
    new UnwrapSingleFunction(),
    new LineMergeFunction(),
    new LineSequenceFunction(),
    new PolygonizeFunction(),
    new ReverseLinestringFunction(),
    new MinimumBoundingCircleFunction(),
    new MinimumDiameterFunction(),
    new MinimumBoundingRectangleFunction(),
    new RemoveHolesFunction(),
    new RemoveSmallHolesFunction(),
    new SnapToSelfFunction()
  };

  public static List<String> getNames()
  {
    List<String> methodNames = new ArrayList<>();
    for (GeometryFunction method : methods) {
      methodNames.add(method.name);
    }
    return methodNames;
  }

  public static List<String> getNames(Collection<GeometryFunction> functions)
  {
    List<String> names = new ArrayList<>();
    for (GeometryFunction fun : functions) {
    	names.add(fun.name);
    }
    return names;
  }

  public static GeometryFunction getFunction(String name)
  {
    for (GeometryFunction method : methods) {
      if (method.name.equals(name))
        return method;
    }
    return null;
  }

  public static GeometryFunction getFunction(Collection<GeometryFunction> functions, String name)
  {
	  for (GeometryFunction func : functions) {
		  if (func.name.equals(name))
			  return func;
	  }
	  return null;
  }

  public static GeometryFunction[] getFunctions()
  {
	  return methods;
  }

  private String name;
  private int nArguments;
  private int nParams;
  private boolean isAggregate = false;   // not yet used
  private String description;

  public String getName() { return name; }
  public int getGeometryArgumentCount() { return nArguments; }
  public int getParameterCount() { return nParams; }

  public GeometryFunction(String name, int nArgs, int nParams)
  {
    this(name, nArgs, nParams, name + " " + I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.function"));
  }

  public GeometryFunction(String name, int nArgs, int nParams,
                          String description)
  {
    this.name = name;
    this.nArguments = nArgs;
    this.nParams = nParams;
    this.description = description;
  }

  public String getDescription() { return description; }

  /**
   * Exectute the function on the geometry(s) in the geom array.
   * The function can expect that the correct number of geometry arguments
   * is present in the array.
   * Integer parameters must be passed as doubles.
   * If no result can be computed for some reason, null should be returned
   * to indicate this to the caller.
   * Exceptions may be thrown and must be handled by the caller.
   *
   * @param geom the geometry arguments
   * @param param any non-geometric arguments.
   * @return the geometry result, or null if no result could be computed.
   */
  public abstract Geometry execute(Geometry[] geom, double[] param);

  public String toString() { return name; }

  //====================================================

  private static class IntersectionFunction extends GeometryFunction {
    public IntersectionFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.intersection"), 2, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].intersection(geom[1]);
    }
  }

  private static class UnionFunction extends GeometryFunction {
    public UnionFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.union"), 2, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].union(geom[1]);
    }
  }

  private static class DifferenceABFunction extends GeometryFunction {
    public DifferenceABFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.difference-a-b"), 2, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].difference(geom[1]);
    }
  }

  private static class DifferenceBAFunction extends GeometryFunction {
    public DifferenceBAFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.difference-b-a"), 2, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[1].difference(geom[0]);
    }
  }

  private static class SymDifferenceFunction extends GeometryFunction {
    public SymDifferenceFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.symetric-difference"), 2, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].symDifference(geom[1]);
    }
  }

  private static class CentroidFunction extends GeometryFunction {
    public CentroidFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.centroid-of-a"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].getCentroid();
    }
  }

  private static class InteriorPointFunction extends GeometryFunction {
    public InteriorPointFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.interior-point"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].getInteriorPoint();
    }
  }

  private static class BufferFunction extends GeometryFunction {
    public BufferFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.buffer"), 1, 1);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].buffer(param[0]);
    }
  }

  private static class SimplifyFunction extends GeometryFunction {
    public SimplifyFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Simplify-(D-P)"), 1, 1, 
      		I18N.get("ui.plugin.analysis.GeometryFunction.Simplifies-a-geometry-using-the-Douglas-Peucker-algorithm"));
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return DouglasPeuckerSimplifier.simplify(geom[0], param[0]);
    }
  }

  private static class SimplifyTopologyFunction extends GeometryFunction {
    public SimplifyTopologyFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Simplify-(preserve-topology)"), 1, 1);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return TopologyPreservingSimplifier.simplify(geom[0], param[0]);
    }
  }

  private static class SimplifyVWFunction extends GeometryFunction {
    public SimplifyVWFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Simplify-(Visvalingam-Whyatt)"), 1, 1,
              I18N.get("ui.plugin.analysis.GeometryFunction.Simplifies-a-geometry-using-the-Visvalingam-Whyatt-algorithm"));
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return VWSimplifier.simplify(geom[0], param[0]);
    }
  }

  private static class ConvexHullFunction extends GeometryFunction {
    public ConvexHullFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Convex-Hull"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      ConvexHull hull = new ConvexHull(geom[0]);
      return hull.getConvexHull();
    }
  }

  private static class BoundaryFunction extends GeometryFunction {
    public BoundaryFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Boundary"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].getBoundary();
    }
  }

  private static class EnvelopeFunction extends GeometryFunction {
    public EnvelopeFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Envelope"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return geom[0].getEnvelope();
    }
  }

  // added on 2016-11-05 by mmichaud
  private static class WrapIntoMultiFunction extends GeometryFunction {
    public WrapIntoMultiFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.WrapIntoMulti"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      if (geom[0] instanceof GeometryCollection) return geom[0];
      else if (geom[0].getDimension() == 0) {
        return geom[0].getFactory().createMultiPoint(new Point[]{(Point)geom[0]});
      }
      else if (geom[0].getDimension() == 1) {
        return geom[0].getFactory().createMultiLineString(new LineString[]{(LineString)geom[0]});
      }
      else if (geom[0].getDimension() == 2) {
        return geom[0].getFactory().createMultiPolygon(new Polygon[]{(Polygon)geom[0]});
      }
      return geom[0];
    }
  }

  // added on 2016-11-05 by mmichaud
  private static class UnwrapSingleFunction extends GeometryFunction {
    public UnwrapSingleFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.UnwrapSingle"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      if (geom[0] instanceof GeometryCollection && geom[0].getNumGeometries() == 1) {
        return geom[0].getGeometryN(0);
      } else return geom[0];
    }
  }

  private static class LineMergeFunction extends GeometryFunction {
    public LineMergeFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Line-Merge"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      LineMerger merger = new LineMerger();
      merger.add(geom[0]);
      Collection lines = merger.getMergedLineStrings();
      return geom[0].getFactory().buildGeometry(lines);
    }
  }

  private static class LineSequenceFunction extends GeometryFunction {
    public LineSequenceFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Line-Sequence"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      LineSequencer sequencer = new LineSequencer();
      sequencer.add(geom[0]);
      return sequencer.getSequencedLineStrings();
    }
  }

  private static class PolygonizeFunction extends GeometryFunction {
    public PolygonizeFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Polygonize"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      Polygonizer polygonizer = new Polygonizer();
      polygonizer.add(geom[0]);
      Collection polyColl = polygonizer.getPolygons();
      return geom[0].getFactory().buildGeometry(polyColl);
    }
  }
  
  // added 3. March 2007 by finstef
  // fixed 26 July 2011 by mmichaud
  private static class ReverseLinestringFunction extends GeometryFunction {
    public ReverseLinestringFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Reverse-Line-Direction"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      Geometry clone = (Geometry)geom[0].clone();
      return clone.reverse();
    }
  }

  // added on 2012-04-13 by mmichaud
  private static class DensifyFunction extends GeometryFunction {
    public DensifyFunction() {
		  super(I18N.get("ui.plugin.analysis.GeometryFunction.Densify"), 1, 1);
	  }

    public Geometry execute(Geometry[] geom, double[] param)
	  {
	      return Densifier.densify(geom[0], param[0]);
	  }
  }

  // added on 2013-06-17 by mmichaud
  private static class MinimumBoundingCircleFunction extends GeometryFunction {
    public MinimumBoundingCircleFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Minimum-Bounding-Circle"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return new MinimumBoundingCircle(geom[0]).getCircle();
    }
  }

  // added on 2013-06-17 by mmichaud
  private static class MinimumDiameterFunction extends GeometryFunction {
    public MinimumDiameterFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Minimum-Diameter"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
        {
            return new MinimumDiameter(geom[0]).getDiameter();
        }
  }

  // added on 2013-06-17 by mmichaud
  private static class MinimumBoundingRectangleFunction extends GeometryFunction {
    public MinimumBoundingRectangleFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Minimum-Bounding-Rectangle"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      return new MinimumDiameter(geom[0]).getMinimumRectangle();
    }
  }

  // added on 2016-11-11 by mmichaud
  private static class RemoveHolesFunction extends GeometryFunction {
    public RemoveHolesFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Remove-Holes"), 1, 0);
    }

    public Geometry execute(Geometry[] geom, double[] param)
    {
      AbstractGeometryProcessor removeHoleProcessor = new AbstractGeometryProcessor() {
        public void process(Polygon polygon, List<Geometry> list) {
          list.add(polygon.getFactory().createPolygon((LinearRing)polygon.getExteriorRing()));
        }
      };
      return removeHoleProcessor.process(geom[0]);
    }
  }

  // added on 2016-11-11 by mmichaud
  private static class RemoveSmallHolesFunction extends GeometryFunction {
    public RemoveSmallHolesFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Remove-Small-Holes"), 1, 1);
    }

    public Geometry execute(Geometry[] geom, final double[] param)
    {
      AbstractGeometryProcessor removeHoleProcessor = new AbstractGeometryProcessor() {
        public void process(Polygon polygon, List<Geometry> list) {
          List<LinearRing> holes = new ArrayList<>();
          for (int i = 0 ; i < polygon.getNumInteriorRing() ; i++) {
            LinearRing ring = (LinearRing)polygon.getInteriorRingN(i);
            if (ring.getFactory().createPolygon(ring).getArea() >= param[0]) holes.add(ring);
          }
          list.add(polygon.getFactory().createPolygon(
                  (LinearRing)polygon.getExteriorRing(),
                  holes.toArray(new LinearRing[0])));
        }
      };
      return removeHoleProcessor.process(geom[0]);
    }
  }

  // added on 2016-11-11 by mmichaud
  private static class RemoveSmallSegmentsFunction extends GeometryFunction {
    public RemoveSmallSegmentsFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Remove-Small-Segments"), 1, 1);
    }

    public Geometry execute(Geometry[] geom, final double[] param)
    {
      RemoveSmallSegments removeSmallSegments = new RemoveSmallSegments(param[0]);
      return removeSmallSegments.process(geom[0]);
    }
  }

  // added on 2021-05-30 by mmichaud
  private static class SnapToSelfFunction extends GeometryFunction {
    public SnapToSelfFunction() {
      super(I18N.get("ui.plugin.analysis.GeometryFunction.Snap-To-Self"), 1, 1);
    }

    public Geometry execute(Geometry[] geom, final double[] param)
    {
      return GeometrySnapper.snapToSelf(geom[0], param[0], true);
    }
  }
  
}
