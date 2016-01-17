//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/tags/2.2-rc5/src/org/deegree/model/spatialschema/GMLGeometryAdapter.java $
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: klaus.greve@uni-bonn.de


 ---------------------------------------------------------------------------*/
package org.deegree.model.spatialschema;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.deegree.framework.log.ILogger;
import org.deegree.framework.util.StringTools;
import org.deegree.framework.xml.ElementList;
import org.deegree.framework.xml.NamespaceContext;
import org.deegree.framework.xml.XMLFragment;
import org.deegree.framework.xml.XMLParsingException;
import org.deegree.framework.xml.XMLTools;
import org.deegree.model.crs.CRSFactory;
import org.deegree.model.crs.CoordinateSystem;
import org.deegree.model.crs.UnknownCRSException;
import org.deegree.ogcbase.CommonNamespaces;
import org.deegree.ogcbase.InvalidGMLException;
import org.saig.jump.lang.I18N;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.vividsolutions.jump.workbench.Logger;

/**
 * Adapter class for converting GML geometries to deegree geometries and vice versa. Some logical
 * problems results from the fact that an envelope isn't a geometry according to ISO 19107 (where
 * the deegree geometry model is based on) but according to GML2/3 specification it is.<br>
 * So if the wrap(..) method is called with an envelope a <tt>Surface</tt> will be returned
 * representing the envelops shape. To export an <tt>Envelope</tt> to a GML box/envelope two
 * specialized export methods are available.<BR>
 * The export method(s) doesn't return a DOM element as one may expect but a <tt>StringBuffer</tt>.
 * This is done because the transformation from deegree geometries to GML mainly is required when a
 * GML representation of a geometry shall be serialized to a file or to a network connection. For
 * both cases the string representation is required and it is simply faster to create the string
 * directly instead of first creating a DOM tree that after this must be serialized to a string.<BR>
 * In future version geometries will be serialized to a stream.
 * 
 * @author <a href="mailto:poth@lat-lon.de">Andreas Poth</a>
 * @author last edited by: $Author: rbezema $
 * @version $Revision: 11344 $, $Date: 2008-04-22 13:38:48 +0200 (Di, 22 Apr 2008) $
 */
public class GMLGeometryAdapter {

    protected static final NamespaceContext nsContext = CommonNamespaces.getNamespaceContext();

    protected static Map<String, CoordinateSystem> crsMap = new HashMap<String, CoordinateSystem>();

    protected static final String COORD = CommonNamespaces.GML_PREFIX + ":coord";

    protected static final String COORDINATES = CommonNamespaces.GML_PREFIX + ":coordinates";

    protected static final String POS = CommonNamespaces.GML_PREFIX + ":pos";

    protected static final String POSLIST = CommonNamespaces.GML_PREFIX + ":posList";

    /** [SBCALVO - 23/02/2010] - Constants for loading different EPSG CRS representations */
    protected static String EPSG_SINGLE = "EPSG:";
    protected static String EPSG_DOUBLE = "EPSG::";
    protected static String X_OGC = "urn:x-ogc:def:";
    protected static String OGC = "urn:ogc:def:";

    public static String GML_DEFAULT_FORMAT_TYPE = "";
    public static String GML2_FORMAT_TYPE = "GML2";

    /**
     * Converts the given string representation of a GML geometry object to a corresponding
     * <code>Geometry</code>. Notice that GML Boxes will be converted to Surfaces because in ISO
     * 19107 Envelopes are no geometries.
     * 
     * @param gml
     * @param srsName default SRS for the geometry (may be overwritten in geometry elements)
     * @return corresponding geometry object
     * @throws GeometryException
     * @throws XMLParsingException
     */
    public static Geometry wrap( String gml, String srsName ) throws GeometryException,
            XMLParsingException {
        StringReader sr = new StringReader(gml);
        Document doc = null;
        try {
            doc = XMLTools.parse(sr);
        } catch (Exception e) {
            Logger.error(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.could-not-parse") + ": '" + gml + "' " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.as-gml-xml"), e);
            throw new XMLParsingException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.could-not-parse") + ": '" + gml + "' " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.as-gml-xml") + e.getMessage());
        }
        return wrap(doc.getDocumentElement(), srsName);
    }

    /**
     * Converts the given DOM representation of a GML geometry to a corresponding
     * <code>Geometry</code>. Notice that GML Boxes will be converted to Surfaces because in ISO
     * 19107 Envelopes are no geometries.
     * <p>
     * Currently, the following conversions are supported:
     * <ul>
     * <li>GML Point -> Point
     * <li>GML MultiPoint -> MultiPoint
     * <li>GML LineString -> Curve
     * <li>GML MultiLineString -> MultiCurve
     * <li>GML Polygon -> Surface
     * <li>GML MultiPolygon -> MultiSurface
     * <li>GML Box -> Surface
     * <li>GML Curve -> Curve
     * <li>GML Surface -> Surface
     * <li>GML MultiCurve -> MultiCurve
     * <li>GML MultiSurface -> MultiSurface
     * </ul>
     * <p>
     * 
     * @param element
     * @param srsName default SRS for the geometry
     * @return corresponding <code>Geometry</code> instance
     * @throws GeometryException if type unsupported or conversion failed
     */
    public static Geometry wrap( Element element, String srsName ) throws GeometryException {

        Geometry geometry = null;
        try {
            String name = element.getLocalName();
            if ((name.equals("Point")) || (name.equals("Center"))) {
                geometry = wrapPoint(element, srsName);
            } else if (name.equals("LineString")) {
                geometry = wrapLineString(element, srsName);
            } else if (name.equals("Polygon")) {
                geometry = wrapPolygon(element, srsName);
            } else if (name.equals("MultiPoint")) {
                geometry = wrapMultiPoint(element, srsName);
            } else if (name.equals("MultiLineString")) {
                geometry = wrapMultiLineString(element, srsName);
            } else if (name.equals("MultiPolygon")) {
                geometry = wrapMultiPolygon(element, srsName);
            } else if (name.equals("Box") || name.equals("Envelope")) {
                geometry = wrapBoxAsSurface(element, srsName);
            } else if (name.equals("Curve")) {
                geometry = wrapCurveAsCurve(element, srsName);
            } else if (name.equals("Surface")) {
                geometry = wrapSurfaceAsSurface(element, srsName);
            } else if (name.equals("MultiCurve")) {
                geometry = wrapMultiCurveAsMultiCurve(element, srsName);
            } else if (name.equals("MultiSurface")) {
                geometry = wrapMultiSurfaceAsMultiSurface(element, srsName);
            } else if (name.equals("CompositeSurface")) {
                geometry = wrapCompositeSurface(element, srsName);
            } else {
                new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.not-a-supported-geometry-type") + ": " + name);
            }
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            throw new GeometryException(StringTools.stackTraceToString(e));
        }
        return geometry;
    }

    /**
     * Returns an instance of {@link Envelope} created from the passed <code>gml:Box</code> or
     * <code>gml:Envelope</code> element.
     * 
     * @param element <code>gml:Box</code> or <code>gml:Envelope</code> element
     * @param srsName default SRS for the geometry
     * @return instance of <code>Envelope</code>
     * @throws XMLParsingException
     * @throws InvalidGMLException
     * @throws UnknownCRSException
     */
    public static Envelope wrapBox( Element element, String srsName ) throws XMLParsingException,
            InvalidGMLException, UnknownCRSException {
        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }
        Position[] bb = null;
        List<Node> nl = XMLTools.getNodes(element, COORD, nsContext);
        if (nl != null && nl.size() > 0) {
            bb = new Position[2];
            bb[0] = createPositionFromCoord((Element) nl.get(0));
            bb[1] = createPositionFromCoord((Element) nl.get(1));
        } else {
            nl = XMLTools.getNodes(element, COORDINATES, nsContext);
            if (nl != null && nl.size() > 0) {
                bb = createPositionFromCoordinates((Element) nl.get(0));
            } else {
                nl = XMLTools.getNodes(element, POS, nsContext);
                if (nl != null && nl.size() > 0) {
                    bb = new Position[2];
                    bb[0] = createPositionFromPos((Element) nl.get(0));
                    bb[1] = createPositionFromPos((Element) nl.get(1));
                } else {
                    Element lowerCorner =
                        (Element) XMLTools.getRequiredNode(element, "gml:lowerCorner", nsContext);
                    Element upperCorner =
                        (Element) XMLTools.getRequiredNode(element, "gml:upperCorner", nsContext);
                    bb = new Position[2];
                    bb[0] = createPositionFromCorner(lowerCorner);
                    bb[1] = createPositionFromCorner(upperCorner);
                }
            }
        }
        Envelope box = GeometryFactory.createEnvelope(bb[0], bb[1], crs);
        return box;
    }

    /**
     * Returns an instance of {@link Curve} created from the passed <code>gml:Curve</code> element.
     * 
     * @param element <code>gml:Curve</code> element
     * @param srsName default SRS for the geometry
     * @return corresponding Curve instance
     * @throws XMLParsingException
     * @throws GeometryException
     * @throws UnknownCRSException
     */
    protected static Curve wrapCurveAsCurve( Element element, String srsName )
            throws XMLParsingException, GeometryException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }
        String srsDimension = findSrsDimension(element);

        Element segment = (Element) XMLTools.getRequiredNode(element, "gml:segments", nsContext);
        CurveSegment[] segments = parseCurveSegments(crs, srsDimension, segment);

        return GeometryFactory.createCurve(segments, crs);
    }

    /**
     * parses CurveSegments
     * 
     * @param crs
     * @param srsDimension
     * @param segment
     * @return CurveSegments
     * @throws XMLParsingException
     * @throws GeometryException
     */
    private static CurveSegment[] parseCurveSegments( CoordinateSystem crs, String srsDimension,
            Element segment ) throws XMLParsingException, GeometryException {
        List<Node> list = XMLTools.getNodes(segment, "child::*", nsContext);

        CurveSegment[] segments = new CurveSegment[list.size()];
        for( int i = 0; i < list.size(); i++ ) {
            if (list.get(i).getLocalName().equals("LineStringSegment")) {
                segments[i] = parseLineStringSegment((Element) list.get(i), crs, srsDimension);
            } else if (list.get(i).getLocalName().equals("Arc")) {
                segments[i] = parseArc((Element) list.get(i), crs, srsDimension);
            } else {
                throw new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.not-supported-type-for-a-curvesegment") + ": " + list.get(i).getLocalName());
            }
        }
        return segments;
    }

    /**
     * parses an Arc
     * 
     * @param element
     * @param crs
     * @param srsDimension
     * @return
     * @throws GeometryException
     */
    private static CurveSegment parseArc( Element element, CoordinateSystem crs, String srsDimension )
            throws GeometryException {
        CurveSegment segment = null;
        try {
            Position[] pos = createPositions(element, null, srsDimension);
            segment = GeometryFactory.createCurveSegment(pos, crs);
        } catch (Exception e) {
            throw new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-creating-segments-for-the-element-arc") + ".");
        }
        return segment;
    }

    /**
     * parses a LineStringSegment (linear interpolated CurveSegment)
     * 
     * @param element
     * @param crs
     * @param srsDimension
     * @return
     * @throws GeometryException
     */
    private static CurveSegment parseLineStringSegment( Element element, CoordinateSystem crs,
            String srsDimension ) throws GeometryException {
        CurveSegment segment = null;
        try {
            Position[] pos = createPositions(element, null, srsDimension);
            segment = GeometryFactory.createCurveSegment(pos, crs);
        } catch (Exception e) {
            throw new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-creating-segments-for-the-element-linestringsegment") + ".");
        }
        return segment;

    }

    /**
     * Returns an instance of {@link MultiCurve} created from the passed <code>gml:MultiCurve</code>
     * element.
     * 
     * @param element <code>gml:MultiCurve</code> element
     * @param srsName default SRS for the geometry
     * @return <code>MultiCurve</code> instance
     * @throws XMLParsingException
     * @throws GeometryException
     * @throws UnknownCRSException
     * @throws InvalidGMLException
     */
    protected static MultiCurve wrapMultiCurveAsMultiCurve( Element element, String srsName )
            throws XMLParsingException, GeometryException, UnknownCRSException, InvalidGMLException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        MultiCurve multiCurve = null;
        try {
            // gml:curveMember
            List<Node> listCurveMember = XMLTools.getNodes(element, "gml:curveMember", nsContext);
            List<Curve> curveList = new ArrayList<Curve>();
            if (listCurveMember.size() > 0) {

                for( int i = 0; i < listCurveMember.size(); i++ ) {
                    Element curveMember = (Element) listCurveMember.get(i);
                    Element curve = (Element) XMLTools.getNode(curveMember, "gml:Curve", nsContext);
                    if (curve != null) {
                        curveList.add(wrapCurveAsCurve(curve, srsName));
                    } else {
                        curve =
                            (Element) XMLTools.getRequiredNode(curveMember,
                                "gml:LineString", nsContext);
                        curveList.add(wrapLineString(curve, srsName));
                    }
                }
            }
            Element curveMembers =
                (Element) XMLTools.getNode(element, "gml:curveMembers", nsContext);
            if (curveMembers != null) {
                // gml:curveMembers
                List<Node> listCurves = XMLTools.getNodes(curveMembers, "gml:Curve", nsContext);
                if (listCurves != null) {
                    for( int i = 0; i < listCurves.size(); i++ ) {
                        Element curve = (Element) listCurves.get(i);
                        curveList.add(wrapCurveAsCurve(curve, srsName));
                    }
                }
                listCurves = XMLTools.getNodes(curveMembers, "gml:LineString", nsContext);
                if (listCurves != null) {
                    for( int i = 0; i < listCurves.size(); i++ ) {
                        Element curve = (Element) listCurves.get(i);
                        curveList.add(wrapLineString(curve, srsName));
                    }
                }
            }
            Curve[] curves = new Curve[curveList.size()];
            multiCurve = GeometryFactory.createMultiCurve(curveList.toArray(curves), crs);
        } catch (XMLParsingException e) {
            Logger.error(e.getMessage(), e);
            throw new XMLParsingException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-parsing") + "<gml:curveMember> " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.elements") + ". " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.please-check-the-xml-document") + ".");
        } catch (GeometryException e) {
            Logger.error(e.getMessage(), e);
            throw new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-creating-a-curve-from-the-curve-element") + ". " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.please-check-the-gml-specifications-for-correct-element-declaration") + ".");
        }
        return multiCurve;
    }

    /**
     * Returns an instance of {@link Surface} created from the passed <code>gml:Surface</code>
     * element.
     * 
     * @param element
     * @param srsName default SRS for the geometry
     * @return Surface
     * @throws XMLParsingException
     * @throws GeometryException
     * @throws UnknownCRSException
     */
    protected static Surface wrapSurfaceAsSurface( Element element, String srsName )
            throws XMLParsingException, GeometryException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        Element patches = extractPatches(element);
        List<Element> polygonList =
            XMLTools.getRequiredElements(patches, "gml:Polygon | gml:PolygonPatch", nsContext);

        SurfacePatch[] surfacePatches = new SurfacePatch[polygonList.size()];

        for( int i = 0; i < polygonList.size(); i++ ) {
            Curve exteriorRing = null;
            Element polygon = polygonList.get(i);
            try {
                Element exterior =
                    (Element) XMLTools.getNode(polygon,
                        "gml:exterior | gml:outerBounderyIs", nsContext);
                if (exterior != null) {
                    exteriorRing = parseRing(crs, exterior);
                } else {
                    String msg = "Cannot interpret GML surface: surface has no exterior ring. ";
                    throw new XMLParsingException(msg);
                }

                List<Element> interiorList =
                    XMLTools.getElements(polygon, "gml:interior | gml:outerBounderyIs",
                        nsContext);
                Curve[] interiorRings = null;
                if (interiorList != null && interiorList.size() > 0) {

                    interiorRings = new Curve[interiorList.size()];

                    for( int j = 0; j < interiorRings.length; j++ ) {
                        Element interior = interiorList.get(j);
                        interiorRings[j] = parseRing(crs, interior);
                    }
                }
                surfacePatches[i] =
                    GeometryFactory.createSurfacePatch(exteriorRing, interiorRings, crs);
            } catch (InvalidGMLException e) {
                Logger.error(e.getMessage(), e);
                throw new XMLParsingException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-parsing-the-polygon-element") + " '" + polygon.getNodeName()
                    + "' " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.to-create-a-surface-geometry") + ".");
            }

        }
        Surface surface = null;
        try {
            surface = GeometryFactory.createSurface(surfacePatches, crs);
        } catch (GeometryException e) {
            throw new GeometryException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-creating-a-surface-from") + " '" + surfacePatches.length + "' " + "polygons" + ".");
        }
        return surface;
    }

    private static String findSrsName( Element element, String srsName ) throws XMLParsingException {
        Node elem = element;
        while( srsName == null && elem != null ) {
            srsName = XMLTools.getNodeAsString(elem, "@srsName", nsContext, srsName);
            elem = elem.getParentNode();
        }
        elem = element;
        if (srsName == null) {
            srsName = XMLTools.getNodeAsString(elem, "//@srsName", nsContext, srsName);
        }
        return srsName;
    }

    /**
     * parses a ring; a ring may is a gml:LinearRing or a gml:Ring
     * 
     * @param crs
     * @param parent parent of a gml:LinearRing or gml:Ring
     * @return curves of a ring
     * @throws XMLParsingException
     * @throws InvalidGMLException
     * @throws GeometryException
     */
    private static Curve parseRing( CoordinateSystem crs, Element parent )
            throws XMLParsingException, InvalidGMLException, GeometryException {
        String srsName = null;
        String srsDimension = findSrsDimension(parent);
        if (crs != null) {
            srsName = crs.getIdentifier();
        }
        List<CurveSegment> curveMembers = null;
        Element ring = (Element) XMLTools.getNode(parent, "gml:LinearRing", nsContext);
        if (ring != null) {
            Position[] exteriorRing = createPositions(ring, srsName, srsDimension);
            curveMembers = new ArrayList<CurveSegment>();
            curveMembers.add(GeometryFactory.createCurveSegment(exteriorRing, crs));
        } else {
            List<Node> members =
                XMLTools.getRequiredNodes(parent, "gml:Ring/gml:curveMember/child::*", nsContext);
            curveMembers = new ArrayList<CurveSegment>(members.size());
            for( Node node : members ) {
                Curve curve = (Curve) wrap((Element) node, srsName);
                CurveSegment[] tmp = curve.getCurveSegments();
                for( int i = 0; i < tmp.length; i++ ) {
                    curveMembers.add(tmp[i]);
                }
            }
        }
        CurveSegment[] cs = curveMembers.toArray(new CurveSegment[curveMembers.size()]);

        return GeometryFactory.createCurve(cs);
    }

    /**
     * Returns an instance of {@link MultiSurface} created from the passed
     * <code>gml:MultiSurface</code> element.
     * 
     * @param element <code>gml:MultiSurface</code> element
     * @param srsName default SRS for the geometry
     * @return MultiSurface
     * @throws XMLParsingException
     * @throws GeometryException
     * @throws InvalidGMLException
     * @throws UnknownCRSException
     */
    protected static MultiSurface wrapMultiSurfaceAsMultiSurface( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }
        MultiSurface multiSurface = null;
        try {
            List<Surface> surfaceList = new ArrayList<Surface>();
            // gml:surfaceMember
            List<Node> listSurfaceMember =
                XMLTools.getNodes(element, "gml:surfaceMember", nsContext);
            if (listSurfaceMember != null) {
                for( int i = 0; i < listSurfaceMember.size(); i++ ) {
                    Element surfaceMember = (Element) listSurfaceMember.get(i);
                    Element surface =
                        (Element) XMLTools.getNode(surfaceMember, "gml:Surface", nsContext);
                    if (surface != null) {
                        surfaceList.add(wrapSurfaceAsSurface(surface, srsName));
                    } else {
                        surface =
                            (Element) XMLTools.getRequiredNode(surfaceMember,
                                ".//gml:Polygon", nsContext);
                        surfaceList.add(wrapPolygon(surface, srsName));
                    }
                }
            }

            Element surfaceMembers =
                (Element) XMLTools.getNode(element, "gml:surfaceMembers", nsContext);
            if (surfaceMembers != null) {
                // gml:surfaceMembers

                List<Node> listSurfaces =
                    XMLTools.getNodes(surfaceMembers, "gml:Surface", nsContext);
                if (listSurfaces != null) {
                    for( int i = 0; i < listSurfaces.size(); i++ ) {
                        Element surface = (Element) listSurfaces.get(i);
                        surfaceList.add(wrapSurfaceAsSurface(surface, srsName));
                    }
                }

                listSurfaces = XMLTools.getNodes(surfaceMembers, ".//gml:Polygon", nsContext);
                if (listSurfaces != null) {
                    for( int i = 0; i < listSurfaces.size(); i++ ) {
                        Element surface = (Element) listSurfaces.get(i);
                        surfaceList.add(wrapPolygon(surface, srsName));
                    }
                }
            }
            Surface[] surfaces = new Surface[surfaceList.size()];
            surfaces = surfaceList.toArray(surfaces);
            multiSurface = GeometryFactory.createMultiSurface(surfaces, crs);
        } catch (XMLParsingException e) {
            Logger.error(e.getMessage(), e);
            String msg =
                "Error parsing <gml:surfaceMember> elements. Please check the xml document.";
            throw new XMLParsingException(msg);
        } catch (GeometryException e) {
            Logger.error(e.getMessage(), e);
            String msg =
                "Error creating a multi surface from the MultiSurface element. Please check the GML specifications for correct element declaration.";
            throw new GeometryException(msg);
        }
        return multiSurface;
    }

    /**
     * Returns a {@link Point} instance created from the passed <code>gml:Point</code> element.
     * 
     * @param element <code>gml:Point</code> element
     * @param srsName default SRS for the geometry
     * @return instance of Point
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static Point wrapPoint( Element element, String srsName ) throws XMLParsingException,
            InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        Position[] bb = null;
        List<Node> nl = XMLTools.getNodes(element, COORD, nsContext);
        if (nl != null && nl.size() > 0) {
            bb = new Position[1];
            bb[0] = createPositionFromCoord((Element) nl.get(0));
        } else {
            nl = XMLTools.getNodes(element, COORDINATES, nsContext);
            if (nl != null && nl.size() > 0) {
                bb = createPositionFromCoordinates((Element) nl.get(0));
            } else {
                nl = XMLTools.getNodes(element, POS, nsContext);
                bb = new Position[1];
                bb[0] = createPositionFromPos((Element) nl.get(0));
            }
        }
        Point point = GeometryFactory.createPoint(bb[0], crs);
        return point;
    }

    /**
     * Returns a {@link Curve} instance created from the passed <code>gml:LineString</code> element.
     * 
     * @param element <code>gml:LineString</code> element
     * @param srsName default SRS for the geometry
     * @return instance of Curve
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static Curve wrapLineString( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        String srsDimension = findSrsDimension(element);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }
        Position[] pos = createPositions(element, srsName, srsDimension);
        Curve curve = GeometryFactory.createCurve(pos, crs);
        return curve;
    }

    /**
     * Try to find the srsDimension value, if possible
     * 
     * @param element
     * @return
     */
    private static String findSrsDimension( Element element ) {
        return XMLTools.getAttrValue(element, null, "srsDimension", null);
    }

    /**
     * Returns a {@link Surface} instance created from the passed <code>gml:Polygon</code> element.
     * 
     * @param element <code>gml:Polygon</code> element
     * @param srsName default SRS for the geometry
     * @return instance of Surface
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static Surface wrapPolygon( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }
        String srsDimension = findSrsDimension(element);

        List<Node> nl =
            XMLTools.getNodes(element, CommonNamespaces.GML_PREFIX + ":outerBoundaryIs", nsContext);
        if (nl == null || nl.size() == 0) {
            nl =
                XMLTools.getRequiredNodes(element,
                    CommonNamespaces.GML_PREFIX + ":exterior", nsContext);
        }
        Element outs = (Element) nl.get(0);
        nl =
            XMLTools.getRequiredNodes(outs, CommonNamespaces.GML_PREFIX + ":LinearRing", nsContext);
        Element ring = (Element) nl.get(0);
        nl = XMLTools.getNodes(ring, COORDINATES, nsContext);
        Position[] outerRing = correctRing(createPositions(ring, srsName, srsDimension));

        Position[][] innerRings = null;
        List<Node> inns =
            XMLTools.getNodes(element, CommonNamespaces.GML_PREFIX + ":innerBoundaryIs", nsContext);
        if (inns == null || inns.size() == 0) {
            inns = XMLTools.getNodes(element, CommonNamespaces.GML_PREFIX + ":interior", nsContext);
        }
        if (inns != null && inns.size() > 0) {
            innerRings = new Position[inns.size()][];
            for( int i = 0; i < innerRings.length; i++ ) {

                nl =
                    XMLTools.getRequiredNodes(inns.get(i), CommonNamespaces.GML_PREFIX
                        + ":LinearRing",
                        nsContext);

                ring = (Element) nl.get(0);
                innerRings[i] = correctRing(createPositions(ring, srsName, srsDimension));
            }
        }

        SurfaceInterpolation si = new SurfaceInterpolationImpl();
        Surface surface = GeometryFactory.createSurface(outerRing, innerRings, si, crs);
        return surface;
    }

    /**
     * Returns a {@link MultiPoint} instance created from the passed <code>gml:MultiPoint</code>
     * element.
     * 
     * @param element <code>gml:MultiPoint</code> element
     * @param srsName default SRS for the geometry
     * @return instance of MultiPoint
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static MultiPoint wrapMultiPoint( Element element, String srsName )
            throws XMLParsingException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        List<Point> pointList = new ArrayList<Point>();
        List<Node> listPointMember = XMLTools.getNodes(element, "gml:pointMember", nsContext);
        if (listPointMember != null) {
            for( int i = 0; i < listPointMember.size(); i++ ) {
                Element pointMember = (Element) listPointMember.get(i);
                Element point = (Element) XMLTools.getNode(pointMember, "gml:Point", nsContext);
                pointList.add(wrapPoint(point, srsName));
            }
        }

        Element pointMembers = (Element) XMLTools.getNode(element, "gml:pointMembers", nsContext);
        if (pointMembers != null) {
            List<Node> pointElems = XMLTools.getNodes(pointMembers, "gml:Point", nsContext);
            for( int j = 0; j < pointElems.size(); j++ ) {
                pointList.add(wrapPoint((Element) pointElems.get(j), srsName));
            }
        }

        Point[] points = new Point[pointList.size()];
        return GeometryFactory.createMultiPoint(pointList.toArray(points), crs);

    }

    /**
     * Returns a {@link MultiCurve} instance created from the passed
     * <code>gml:MultiLineString</code> element.
     * 
     * @param element <code>gml:MultiLineString</code> element
     * @param srsName default SRS for the geometry
     * @return instance of MultiCurve
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static MultiCurve wrapMultiLineString( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        ElementList el =
            XMLTools.getChildElements("lineStringMember", CommonNamespaces.GMLNS, element);
        Curve[] curves = new Curve[el.getLength()];
        for( int i = 0; i < curves.length; i++ ) {
            curves[i] = wrapLineString(XMLTools.getFirstChildElement(el.item(i)), srsName);
        }
        MultiCurve mp = GeometryFactory.createMultiCurve(curves, crs);
        return mp;
    }

    /**
     * Returns a {@link MultiSurface} instance created from the passed <code>gml:MultiPolygon</code>
     * element.
     * 
     * @param element <code>gml:MultiPolygon</code> element
     * @param srsName default SRS for the geometry
     * @return instance of MultiCurve
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static MultiSurface wrapMultiPolygon( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {

        srsName = findSrsName(element, srsName);
        CoordinateSystem crs = null;
        if (srsName != null) {
            crs = getCRS(srsName);
        }

        ElementList el =
            XMLTools.getChildElements("polygonMember", CommonNamespaces.GMLNS, element);
        Surface[] surfaces = new Surface[el.getLength()];
        for( int i = 0; i < surfaces.length; i++ ) {
            surfaces[i] = wrapPolygon(XMLTools.getFirstChildElement(el.item(i)), srsName);
        }
        return GeometryFactory.createMultiSurface(surfaces, crs);
    }

    /**
     * Returns a <code>Surface</code> created from the given <code>gml:Box</code> element. This
     * method is useful because an Envelope that would normally be created from a Box isn't a
     * geometry in context of ISO 19107.
     * 
     * @param element <code>gml:Box</code> element
     * @return instance of <code>Surface</code>
     * @throws XMLParsingException
     * @throws UnknownCRSException
     */
    private static Surface wrapBoxAsSurface( Element element, String srsName )
            throws XMLParsingException, GeometryException, InvalidGMLException, UnknownCRSException {
        Envelope env = wrapBox(element, srsName);
        return GeometryFactory.createSurface(env, env.getCoordinateSystem());
    }

    /**
     * Returns an instance of {@link CompositeSurface} created from the passed
     * <code>gml:CompositeSurface</code> element. TODO
     * 
     * @param element
     * @param srsName default SRS for the geometry
     * @return CompositeSurface
     * @throws GeometryException
     */
    private static CompositeSurface wrapCompositeSurface( Element element, String srsName ) {
        throw new UnsupportedOperationException("#wrapCompositeSurface(Element) " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.is-not-implemented-as-yet") + ". " + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.work-in-progress") + ".");
    }

    /**
     * Extract the <gml:patches> node from a <gml:Surface> element.
     * 
     * @param surface
     * @return Element
     * @throws XMLParsingException
     */
    private static Element extractPatches( Element surface ) throws XMLParsingException {
        Element patches = null;
        try {
            patches = (Element) XMLTools.getRequiredNode(surface, "gml:patches", nsContext);
        } catch (XMLParsingException e) {
            throw new XMLParsingException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.error-retrieving-the-patches-element-from-the-surface-element") + ".");
        }
        return patches;
    }

    /**
     * returns an instance of CS_CoordinateSystem corrsponding to the passed crs name
     * 
     * @param name name of the crs
     * @return CS_CoordinateSystem
     * @throws UnknownCRSException
     */
    public static CoordinateSystem getCRS( String name ) throws UnknownCRSException {

        if ((name != null) && (name.length() > 2)) {
            if (name.startsWith("http://www.opengis.net/gml/srs/")) {
                // as declared in the GML 2.1.1 specification
                // http://www.opengis.net/gml/srs/epsg.xml#4326
                int p = name.lastIndexOf("/");

                if (p >= 0) {
                    name = name.substring(p, name.length());
                    p = name.indexOf(".");

                    String s1 = name.substring(1, p).toUpperCase();
                    p = name.indexOf("#");

                    String s2 = name.substring(p + 1, name.length());
                    name = s1 + ":" + s2;
                }
            }
        }

        CoordinateSystem crs = crsMap.get(name);
        if (crs == null) {
            // [SBCALVO - 23/02/2010] - Process the crs name in order to transform it to
            // EPSG:<epsg_code> String
            String epsgCode = transformCRSNameToEPSG(name);
            crs = CRSFactory.create(epsgCode);
            crsMap.put(name, crs);
        }
        return crs;
    }

    /**
     * Transforms the CRS name to a EPSG:<epsg_code> string
     * <p>
     * All known epsg representations, currently:
     * <ul>
     * <li>urn:x-ogc:def:${operationName}:EPSG::${epsgCode}</li>
     * <li>urn:x-ogc:def:${operationName}:EPSG:${epsgCode}</li>
     * <li>urn:ogc:def:${operationName}:EPSG::${epsgCode}</li>
     * <li>urn:ogc:def:${operationName}:EPSG:${epsgCode}</li>
     * <li>EPSG::${epsgCode}</li>
     * <li>EPSG:${epsgCode}</li>
     * <li>http://www.opengis.net/gml/srs/epsg.xml#<EPSG code></li>
     * <li>Any string containing EPSG:${epsgCode} or EPSG::${epsgCode}
     * </ul>
     * </p>
     * 
     * @param name
     * @author sbcalvo
     * @return
     */
    public static String transformCRSNameToEPSG( String name ) {
        String result = null;
        if (StringUtils.startsWithIgnoreCase(name, EPSG_SINGLE)) {
            // EPSG:${epsgCode}
            result = name;
        } else if (StringUtils.startsWithIgnoreCase(name, EPSG_DOUBLE)) {
            // EPSG::${epsgCode}
            result = EPSG_SINGLE + StringUtils.substringAfterLast(name, ":");
        } else if (StringUtils.startsWithIgnoreCase(name, X_OGC)) {
            // urn:x-ogc:def:${operationName}:EPSG:${epsgCode}
            // urn:x-ogc:def:${operationName}:EPSG:${epsgCode}
            result = EPSG_SINGLE + StringUtils.substringAfterLast(name, ":");
        } else if (StringUtils.startsWithIgnoreCase(name, OGC)) {
            // urn:ogc:def:${operationName}:EPSG::${epsgCode}
            // urn:ogc:def:${operationName}:EPSG:${epsgCode}
            result = EPSG_SINGLE + StringUtils.substringAfterLast(name, ":");
        } else if (StringUtils.containsIgnoreCase(name, "epsg.xml")) {
            // http://www.opengis.net/gml/srs/epsg.xml#<EPSG code>
            result = EPSG_SINGLE + StringUtils.substringAfterLast(name, "#");
        } else if (StringUtils.containsIgnoreCase(name, "urn:epsg")) {
            // urn:EPSG:geographicCRC:<epsg code>
            result = EPSG_SINGLE + StringUtils.substringAfterLast(name, ":");
        } else {
            result = name;
        }

        Logger.debug("Transformed from " + name + " to " + result);
        return result;
    }

    private static Position createPositionFromCorner( Element corner ) throws InvalidGMLException {

        String tmp = XMLTools.getAttrValue(corner, null, "dimension", null);
        int dim = 0;
        if (tmp != null) {
            dim = Integer.parseInt(tmp);
        }
        tmp = XMLTools.getStringValue(corner);
        double[] vals = StringTools.toArrayDouble(tmp, ", ");
        if (dim != 0) {
            if (vals.length != dim) {
                throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.dimension-must-be-equal-to-the-number-of-coordinate-values-defined-in-pos-element") + ".");
            }
        } else {
            dim = vals.length;
        }

        Position pos = null;
        if (dim == 3) {
            pos = GeometryFactory.createPosition(vals[0], vals[1], vals[2]);
        } else {
            pos = GeometryFactory.createPosition(vals[0], vals[1]);
        }

        return pos;

    }

    /**
     * returns an instance of Position created from the passed coord
     * 
     * @param element <coord>
     * @return instance of <tt>Position</tt>
     * @throws XMLParsingException
     */
    private static Position createPositionFromCoord( Element element ) throws XMLParsingException {

        Position pos = null;
        Element elem = XMLTools.getRequiredChildElement("X", CommonNamespaces.GMLNS, element);
        double x = Double.parseDouble(XMLTools.getStringValue(elem));
        elem = XMLTools.getRequiredChildElement("Y", CommonNamespaces.GMLNS, element);
        double y = Double.parseDouble(XMLTools.getStringValue(elem));
        elem = XMLTools.getChildElement("Z", CommonNamespaces.GMLNS, element);

        if (elem != null) {
            double z = Double.parseDouble(XMLTools.getStringValue(elem));
            pos = GeometryFactory.createPosition(new double[]{x, y, z});
        } else {
            pos = GeometryFactory.createPosition(new double[]{x, y});
        }

        return pos;
    }

    /**
     * returns an array of Positions created from the passed coordinates
     * 
     * @param element <coordinates>
     * @return instance of <tt>Position[]</tt>
     * @throws XMLParsingException
     */
    private static Position[] createPositionFromCoordinates( Element element ) {

        Position[] points = null;
        // fixing the failure coming from the usage of the xmltools.getAttrib method
        String ts = XMLTools.getAttrValue(element, null, "ts", " ");

        // not used because javas current decimal seperator will be used
        // String ds = XMLTools.getAttrValue( element, null, "decimal", "." );
        String cs = XMLTools.getAttrValue(element, null, "cs", ",");

        String value = XMLTools.getStringValue(element).trim();

        // first tokenizer, tokens the tuples
        StringTokenizer tuple = new StringTokenizer(value, ts);
        points = new Position[tuple.countTokens()];
        int i = 0;
        while( tuple.hasMoreTokens() ) {
            String s = tuple.nextToken();
            // second tokenizer, tokens the coordinates
            StringTokenizer coort = new StringTokenizer(s, cs);
            double[] p = new double[coort.countTokens()];

            for( int k = 0; k < p.length; k++ ) {
                s = coort.nextToken();
                p[k] = Double.parseDouble(s);
            }

            points[i++] = GeometryFactory.createPosition(p);
        }

        return points;
    }

    /**
     * creates a <tt>Point</tt> from the passed <pos> element containing a GML pos.
     * 
     * @param element
     * @return created <tt>Point</tt>
     * @throws XMLParsingException
     * @throws InvalidGMLException
     */
    private static Position createPositionFromPos( Element element ) throws InvalidGMLException {

        String tmp = XMLTools.getAttrValue(element, null, "dimension", null);
        int dim = 0;
        if (tmp != null) {
            dim = Integer.parseInt(tmp);
        }
        tmp = XMLTools.getStringValue(element);
        double[] vals = StringTools.toArrayDouble(tmp, "\t\n\r\f ,");
        if (vals != null) {
            if (dim != 0) {
                if (vals.length != dim) {
                    throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.dimension-must-be-equal-to-the-number-of-coordinate-values-defined-in-pos-element") + ".");
                }
            } else {
                dim = vals.length;
            }
        } else {
            throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.the-given-element")
                + "{"
                + element.getNamespaceURI()
                + "}"
                + element.getLocalName()
                + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.does-not-contain-any-coordinates")
                + ". "
                + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.this-may-not-be"));
        }

        Position pos = null;
        if (dim == 3) {
            pos = GeometryFactory.createPosition(vals[0], vals[1], vals[2]);
        } else {
            pos = GeometryFactory.createPosition(vals[0], vals[1]);
        }

        return pos;
    }

    /**
     * @param element
     * @param srsDimension
     * @return Position
     * @throws InvalidGMLException
     * @throws XMLParsingException
     */
    private static Position[] createPositionFromPosList( Element element, String srsName,
            String srsDimension ) throws InvalidGMLException, XMLParsingException {

        if (srsName == null) {
            srsName = findSrsName(element, srsName);
        }

        // If the parent doesn't have it, look for it here
        if (srsDimension == null) {
            srsDimension = findSrsDimension(element);
        }

        if (Logger.isDebugEnabled()) {
            XMLFragment doc = new XMLFragment(element);
            Logger.debug(doc.getAsPrettyString());
        }
        int dim = 0;
        if (srsDimension != null) {
            dim = Integer.parseInt(srsDimension);
        }
        if (dim == 0) {
            // TODO
            // determine dimension from CRS
            // default dimension set.
            dim = 2;

        }

        String axisLabels = XMLTools.getAttrValue(element, null, "gml:axisAbbrev", null);

        String uomLabels = XMLTools.getAttrValue(element, null, "uomLabels", null);

        if (srsName == null) {
            if (srsDimension != null) {
                throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.attribute-srsdimension-cannot-be-defined-unless-attribute-srsname-has-been-defined") + ".");
            }
            if (axisLabels != null) {
                throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.attribute-axislabels-cannot-be-defined-unless-attribute-srsname-has-been-defined") + ".");
            }

        }
        if (axisLabels == null) {
            if (uomLabels != null) {
                throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.attribute-uomlabels-cannot-be-defined-unless-attribute-axisLabels-has-been-defined") + ".");
            }
        }
        String tmp = XMLTools.getStringValue(element);
        double[] values = StringTools.toArrayDouble(tmp, "\t\n\r\f ,");
        int size = values.length / dim;
        Logger.debug(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.number-of-points") + " = "+ size);
        Logger.debug(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.size-of-the-original-array") + ": "+ values.length);
        Logger.debug(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.dimension") + ": "+ dim);

        if (values.length < 4) {
            throw new InvalidGMLException(I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.a-point-list-must-have-minimum-two-coordinate-tuples")
                + ". "
                + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.here-only")
                + " '"
                + size + "' "
                + I18N.getString("org.deegree.model.spatialschema.GMLGeometryAdapter.are-defined")
                + ".");
        }
        double positions[][] = new double[size][dim];
        int a = 0, b = 0;
        for( int i = 0; i < values.length; i++ ) {
            if (b == dim) {
                a++;
                b = 0;
            }
            positions[a][b] = values[i];
            b++;
        }

        Position[] position = new Position[positions.length];
        for( int i = 0; i < positions.length; i++ ) {
            double[] vals = positions[i];
            if (dim == 3) {
                position[i] = GeometryFactory.createPosition(vals[0], vals[1], vals[2]);
            } else {
                position[i] = GeometryFactory.createPosition(vals[0], vals[1]);
            }
        }

        return position;

    }

    /**
     * creates an array of <tt>Position</tt>s from the <coordinates> or <pos> Elements located as
     * children under the passed parent element.
     * <p>
     * example:<br>
     * 
     * <pre>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          &lt;gml:Box&gt;
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                &lt;gml:coordinates cs=&quot;,&quot; decimal=&quot;.&quot; ts=&quot; &quot;&gt;0,0 4000,4000&lt;/gml:coordinates&gt;
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          &lt;/gml:Box&gt;
     * </pre>
     * 
     * </p>
     * 
     * @param parent
     * @param srsName
     * @param srsDimension
     * @return
     * @throws XMLParsingException
     * @throws InvalidGMLException
     */
    private static Position[] createPositions( Element parent, String srsName, String srsDimension )
            throws XMLParsingException, InvalidGMLException {

        List<Node> nl = XMLTools.getNodes(parent, COORDINATES, nsContext);
        Position[] pos = null;
        if (nl != null && nl.size() > 0) {
            pos = createPositionFromCoordinates((Element) nl.get(0));
        } else {
            nl = XMLTools.getNodes(parent, POS, nsContext);
            if (nl != null && nl.size() > 0) {
                pos = new Position[nl.size()];
                for( int i = 0; i < pos.length; i++ ) {
                    pos[i] = createPositionFromPos((Element) nl.get(i));
                }
            } else {
                Element posList = (Element) XMLTools.getRequiredNode(parent, POSLIST, nsContext);
                if (posList != null) {
                    pos = createPositionFromPosList(posList, srsName, srsDimension);
                }
            }
        }
        return pos;
    }

    /**
     * Creates a GML representation from the passed <code>Geometry<code>
     * 
     * @param geometry
     * @param target
     * @throws GeometryException
     */
    public static PrintWriter export( Geometry geometry, OutputStream target, String formatType )
            throws GeometryException {

        PrintWriter printwriter = new PrintWriter(target);

        if (geometry instanceof SurfacePatch) {
            geometry = new SurfaceImpl((SurfacePatch) geometry);
        } else if (geometry instanceof LineString) {
            geometry = new CurveImpl((LineString) geometry);
        }
        // Create geometries from the wkb considering the geomerty typ
        if (geometry instanceof Point) {
            exportPoint((Point) geometry, printwriter);
        } else if (geometry instanceof Curve) {
            exportCurve((Curve) geometry, printwriter);
        } else if (geometry instanceof Surface) {
            exportSurface((Surface) geometry, printwriter, formatType);
        } else if (geometry instanceof MultiPoint) {
            exportMultiPoint((MultiPoint) geometry, printwriter);
        } else if (geometry instanceof MultiCurve) {
            exportMultiCurve((MultiCurve) geometry, printwriter);
        } else if (geometry instanceof MultiSurface) {
            exportMultiSurface((MultiSurface) geometry, printwriter, formatType);
        }
        printwriter.flush();
        return printwriter;
    }

    /**
     * Creates a GML representation from the passed <code>Geometry</code>.
     * 
     * @param geometry
     * @return a string buffer containing the XML
     * @throws GeometryException
     */
    public static StringBuffer export( Geometry geometry, String formatType )
            throws GeometryException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);

        export(geometry, bos, formatType);

        return new StringBuffer(new String(bos.toByteArray()));
    }
    
    /**
     * convenience method for above
     * @param geometry
     * @return
     * @throws GeometryException
     */
    public static StringBuffer export(Geometry geometry) throws GeometryException {
  
      return export(geometry, GML_DEFAULT_FORMAT_TYPE);
    }

    /**
     * creates a GML representation from the passed <tt>Envelope</tt>. This method is required
     * because in ISO 19107 Envelops are no geometries.
     * 
     * @param envelope
     * @return
     * @throws GeometryException
     */
    public static StringBuffer exportAsBox( Envelope envelope ) {

        StringBuffer sb = new StringBuffer("<gml:Box xmlns:gml=\"http://www.opengis.net/gml\">");
        sb.append("<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">");
        sb.append(envelope.getMin().getX()).append(',');
        sb.append(envelope.getMin().getY());
        int dim = envelope.getMax().getCoordinateDimension();
        if (dim == 3) {
            sb.append(',').append(envelope.getMin().getZ());
        }
        sb.append(' ').append(envelope.getMax().getX());
        sb.append(',').append(envelope.getMax().getY());
        if (dim == 3) {
            sb.append(',').append(envelope.getMax().getZ());
        }
        sb.append("</gml:coordinates></gml:Box>");

        return sb;
    }

    /**
     * creates a GML representation from the passed <tt>Envelope</tt>. This method is required
     * because in ISO 19107 Envelops are no geometries.
     * 
     * @param envelope
     * @param formatType
     * @return
     * @throws GeometryException
     */
    public static StringBuffer exportAsEnvelope( Envelope envelope, String formatType ) {

        String crs = null;
        if (envelope.getCoordinateSystem() != null) {
            crs = envelope.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }

        String srs = null;
        if (crs != null) {
            srs = "<gml:Envelope srsName=\"" + crs + "\"";
        } else {
            srs = "<gml:Envelope";
        }

        StringBuffer sb = new StringBuffer(srs);

        int dim = envelope.getMax().getCoordinateDimension();

        if (formatType.contains(GML2_FORMAT_TYPE)) {
            sb.append("xmlns:gml='http://www.opengis.net/gml'>");
            sb.append("<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">");
            sb.append(envelope.getMin().getX()).append(',');
            sb.append(envelope.getMin().getY());

            if (dim == 3) {
                sb.append(',').append(envelope.getMin().getZ());
            }
            sb.append(' ').append(envelope.getMax().getX());
            sb.append(',').append(envelope.getMax().getY());
            if (dim == 3) {
                sb.append(',').append(envelope.getMax().getZ());
            }
            sb.append("</gml:coordinates>");

        } else {
            sb.append(">");
            sb.append("<gml:lowerCorner>");
            sb.append(envelope.getMin().getX()).append(' ');
            sb.append(envelope.getMin().getY());
            if (dim == 3) {
                sb.append(' ').append(envelope.getMin().getZ());
            }
            sb.append("</gml:lowerCorner>");
            sb.append("<gml:upperCorner>");
            sb.append(envelope.getMax().getX());
            sb.append(' ').append(envelope.getMax().getY());
            if (dim == 3) {
                sb.append(' ').append(envelope.getMax().getZ());
            }
            sb.append("</gml:upperCorner>");
        }
        sb.append("</gml:Envelope>");

        return sb;
    }
    
    /**
     * convenience method for the above
     * 
     * @param envelope
     * @return
     */
    public static StringBuffer exportAsEnvelope(Envelope envelope) {
      
      return exportAsEnvelope(envelope, GML_DEFAULT_FORMAT_TYPE);
    }

    /**
     * creates a GML expression of a point geometry
     * 
     * @param point point geometry
     * @return
     */
    protected static void exportPoint( Point point, PrintWriter pw ) {

        String crs = null;
        int dim = point.getCoordinateDimension();
        if (point.getCoordinateSystem() != null) {
            crs = point.getCoordinateSystem().getIdentifier().replace(' ', ':');
            dim = point.getCoordinateSystem().getDimension();
        }
        String srs = null;
        if (crs != null) {
            srs = "<gml:Point srsName=\"" + crs + "\">";
        } else {
            srs = "<gml:Point>";
        }
        pw.println(srs);

        if (dim != 0) {
            String dimension = "<gml:pos srsDimension=\"" + dim + "\">";
            pw.print(dimension);
        } else {
            pw.print("<gml:pos>");
        }

        String coordinates = point.getX() + " " + point.getY();
        if (dim == 3) {
            coordinates = coordinates + " " + point.getZ();
        }
        pw.print(coordinates);
        pw.println("</gml:pos>");
        pw.print("</gml:Point>");

    }

    /**
     * creates a GML expression of a curve geometry
     * 
     * @param o curve geometry
     * @return
     * @throws GeometryException
     */
    protected static void exportCurve( Curve o, PrintWriter pw ) throws GeometryException {

        String crs = null;
        if (o.getCoordinateSystem() != null) {
            crs = o.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }
        String srs = null;
        if (crs != null) {
            srs = "<gml:Curve srsName=\"" + crs + "\">";
        } else {
            srs = "<gml:Curve>";
        }
        pw.println(srs);
        pw.println("<gml:segments>");

        int curveSegments = o.getNumberOfCurveSegments();
        for( int i = 0; i < curveSegments; i++ ) {
            pw.print("<gml:LineStringSegment>");
            CurveSegment segment = o.getCurveSegmentAt(i);
            Position[] p = segment.getAsLineString().getPositions();
            pw.print("<gml:posList>");
            for( int j = 0; j < (p.length - 1); j++ ) {
                pw.print(p[j].getX() + " " + p[j].getY());
                if (o.getCoordinateDimension() == 3) {
                    pw.print(' ');
                    pw.print(p[j].getZ());
                    pw.print(' ');
                } else {
                    pw.print(' ');
                }
            }
            pw.print(p[p.length - 1].getX() + " " + p[p.length - 1].getY());
            if (o.getCoordinateDimension() == 3) {
                pw.print(" " + p[p.length - 1].getZ());
            }
            pw.println("</gml:posList>");
            pw.println("</gml:LineStringSegment>");
        }
        pw.println("</gml:segments>");
        pw.print("</gml:Curve>");

    }

    /**
     * @param sur
     * @throws RemoteException
     * @throws GeometryException
     */
    protected static void exportSurface( Surface surface, PrintWriter pw, String formatType )
            throws GeometryException {

        String crs = null;
        if (surface.getCoordinateSystem() != null) {
            crs = surface.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }

        if (formatType.contains(GML2_FORMAT_TYPE)) {
            pw.println("<gml:Polygon>");
            int patches = surface.getNumberOfSurfacePatches();
            for( int i = 0; i < patches; i++ ) {
                SurfacePatch patch = surface.getSurfacePatchAt(i);
                if (i == 0) {
                    printExteriorRing(surface, pw, patch, formatType);
                } else {
                    printInteriorRing(surface, pw, patch, formatType);
                }
            }
            pw.println("</gml:Polygon>");
        } else {

            String srs = null;
            if (crs != null) {
                srs = "<gml:Surface srsName='" + crs + "'>";
            } else {
                srs = "<gml:Surface>";
            }

            pw.println(srs);
            int patches = surface.getNumberOfSurfacePatches();
            pw.println("<gml:patches>");
            for( int i = 0; i < patches; i++ ) {
                pw.println("<gml:PolygonPatch>");

                SurfacePatch patch = surface.getSurfacePatchAt(i);
                printExteriorRing(surface, pw, patch, formatType);
                printInteriorRing(surface, pw, patch, formatType);
                pw.println("</gml:PolygonPatch>");

            }
            pw.println("</gml:patches>");
            pw.print("</gml:Surface>");
        }

    }

    /**
     * @param surface
     * @param pw
     * @param patch
     */
    protected static void printInteriorRing( Surface surface, PrintWriter pw, SurfacePatch patch,
            String formatType ) {
        // interior rings
        Position[][] ip = patch.getInteriorRings();
        if (ip != null) {
            for( int j = 0; j < ip.length; j++ ) {
                if (formatType.contains(GML2_FORMAT_TYPE)) {
                    pw.println("<gml:innerBoundaryIs>");
                } else {
                    pw.println("<gml:interior>");
                }
                pw.println("<gml:LinearRing>");
                if (surface.getCoordinateSystem() != null) {
                    printPositions(pw, ip[j], surface.getCoordinateDimension(), formatType);
                } else {
                    printPositions(pw, ip[j], 0, formatType);
                }
                pw.println("</gml:LinearRing>");
                if (formatType.contains(GML2_FORMAT_TYPE)) {
                    pw.println("</gml:innerBoundaryIs>");
                } else {
                    pw.println("</gml:interior>");
                }
            }
        }
    }

    /**
     * @param surface
     * @param pw
     * @param patch
     */
    protected static void printExteriorRing( Surface surface, PrintWriter pw, SurfacePatch patch,
            String formatType ) {
        // exterior ring
        if (formatType.contains(GML2_FORMAT_TYPE)) {
            pw.print("<gml:outerBoundaryIs>");    
        } else {
            pw.print("<gml:exterior>");
        }

        pw.print("<gml:LinearRing>");
        if (surface.getCoordinateSystem() != null) {
            printPositions(pw,
                patch.getExteriorRing(),
                surface.getCoordinateDimension(),
                formatType);
        } else {
            printPositions(pw, patch.getExteriorRing(), 0, formatType);
        }
        pw.print("</gml:LinearRing>");
        if (formatType.contains(GML2_FORMAT_TYPE)) {
            pw.print("</gml:outerBoundaryIs>");    
        } else {
            pw.print("</gml:exterior>");
        }
    }

    /**
     * TODO using this method for exporting Surfaces will change to output, so it must be tested
     * carefully
     * 
     * @param pw
     * @param ring
     * @param coordinateDimension
     */
    protected static void printRing( PrintWriter pw, Ring ring, int coordinateDimension,
            String formatType ) {
        pw.print("<gml:Ring><gml:curveMember><gml:Curve><gml:segments>");
        CurveSegment[] cs = ring.getCurveSegments();
        for( int i = 0; i < cs.length; i++ ) {
            printCurveSegment(pw, cs[i], coordinateDimension, formatType);
        }
        pw.print("</gml:segments></gml:Curve></gml:curveMember></gml:Ring>");

    }

    /**
     * @param pw
     * @param segment
     * @param coordinateDimension
     */
    private static void printCurveSegment( PrintWriter pw, CurveSegment segment,
            int coordinateDimension, String formatType ) {
        pw.print("<gml:LineStringSegment>");
        printPositions(pw, segment.getPositions(), coordinateDimension, formatType);
        pw.print("</gml:LineStringSegment>");
    }

    /**
     * @param pw
     * @param p
     * @param coordinateDimension
     */
    private static void printPositions( PrintWriter pw, Position[] p, int coordinateDimension,
            String formatType ) {
        String startTag = formatType.equals(GML2_FORMAT_TYPE) ? "<gml:coordinates" : "<gml:posList";
        String endTag =
            formatType.equals(GML2_FORMAT_TYPE) ? "</gml:coordinates>" : "</gml:posList>";
        StringBuilder posList = new StringBuilder(startTag);
        String coordSeparator = formatType.equals(GML2_FORMAT_TYPE) ? "," : " ";

        if (formatType.equals(GML2_FORMAT_TYPE)) {
            posList.append(" decimal=\".\" cs=\",\" ts=\" \"");

        } else {
            if (coordinateDimension > 0) {
                posList.append(" srsDimension='").append(coordinateDimension).append("'");
            }
            posList.append(" count='").append(p.length).append("'");
        }

        posList.append(">");
        pw.print(posList);

        for( int j = 0; j < (p.length - 1); j++ ) {
            pw.print(p[j].getX() + coordSeparator + p[j].getY());
            if (coordinateDimension == 3) {
                pw.print(coordSeparator + p[j].getZ() + " ");
            } else {
                pw.print(' ');
            }
        }
        pw.print(p[p.length - 1].getX() + coordSeparator + p[p.length - 1].getY());
        if (coordinateDimension == 3) {
            pw.print(coordSeparator + p[p.length - 1].getZ());
        }
        pw.print(endTag);
    }

    /**
     * @param mp
     * @return
     * @throws RemoteException
     */
    protected static void exportMultiPoint( MultiPoint mp, PrintWriter pw ) {

        String crs = null;
        if (mp.getCoordinateSystem() != null) {
            crs = mp.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }
        String srs = null;
        if (crs != null) {
            srs = "<gml:MultiPoint srsName=\"" + crs + "\">";
        } else {
            srs = "<gml:MultiPoint>";
        }
        pw.println(srs);
        pw.println("<gml:pointMembers>");
        for( int i = 0; i < mp.getSize(); i++ ) {

            pw.println("<gml:Point>");
            pw.print("<gml:pos>");
            pw.print(mp.getPointAt(i).getX() + " " + mp.getPointAt(i).getY());
            if (mp.getPointAt(i).getCoordinateDimension() == 3) {
                pw.print(" " + mp.getPointAt(i).getZ());
            }
            pw.println("</gml:pos>");
            pw.println("</gml:Point>");
        }
        pw.println("</gml:pointMembers>");
        pw.print("</gml:MultiPoint>");

    }

    /**
     * @param multiCurve
     * @return
     * @throws RemoteException
     * @throws GeometryException
     */
    protected static void exportMultiCurve( MultiCurve multiCurve, PrintWriter pw )
            throws GeometryException {

        String crs = null;
        if (multiCurve.getCoordinateSystem() != null) {
            crs = multiCurve.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }
        String srs = null;
        if (crs != null) {
            srs = "<gml:MultiCurve srsName=\"" + crs + "\">";
        } else {
            srs = "<gml:MultiCurve>";
        }
        pw.println(srs);

        Curve[] curves = multiCurve.getAllCurves();
        pw.println("<gml:curveMembers>");
        for( int i = 0; i < curves.length; i++ ) {
            Curve curve = curves[i];
            pw.println("<gml:Curve>");
            pw.println("<gml:segments>");
            pw.println("<gml:LineStringSegment>");
            int numberCurveSegments = curve.getNumberOfCurveSegments();
            for( int j = 0; j < numberCurveSegments; j++ ) {
                CurveSegment curveSegment = curve.getCurveSegmentAt(j);
                Position[] p = curveSegment.getAsLineString().getPositions();
                pw.print("<gml:posList>");
                for( int k = 0; k < (p.length - 1); k++ ) {
                    pw.print(p[k].getX() + " " + p[k].getY());
                    if (curve.getCoordinateDimension() == 3) {
                        pw.print(" " + p[k].getZ() + " ");
                    } else {
                        pw.print(" ");
                    }
                }
                pw.print(p[p.length - 1].getX() + " " + p[p.length - 1].getY());
                if (curve.getCoordinateDimension() == 3) {
                    pw.print(" " + p[p.length - 1].getZ());
                }
                pw.println("</gml:posList>");
            }
            pw.println("</gml:LineStringSegment>");
            pw.println("</gml:segments>");
            pw.println("</gml:Curve>");
        }
        pw.println("</gml:curveMembers>");
        pw.print("</gml:MultiCurve>");

    }

    /**
     * @param multiSurface
     * @return
     * @throws RemoteException
     * @throws GeometryException
     */
    protected static void exportMultiSurface( MultiSurface multiSurface, PrintWriter pw,
            String formatType ) throws GeometryException {

        String crs = null;
        if (multiSurface.getCoordinateSystem() != null) {
            crs = multiSurface.getCoordinateSystem().getIdentifier().replace(' ', ':');
        }
        String srs = null;
        if (crs != null) {
            srs = "<gml:MultiSurface srsName=\"" + crs + "\">";
        } else {
            srs = "<gml:MultiSurface>";
        }
        pw.println(srs);

        Surface[] surfaces = multiSurface.getAllSurfaces();

        pw.println("<gml:surfaceMembers>");
        for( int i = 0; i < surfaces.length; i++ ) {
            Surface surface = surfaces[i];
            exportSurface(surface, pw, formatType);
        }
        pw.println("</gml:surfaceMembers>");
        // substitution as requested in issue
        // http://wald.intevation.org/tracker/index.php?func=detail&aid=477&group_id=27&atid=212
        // can be removed if it was inserted correctly
        // pw.println( "<gml:surfaceMembers>" );
        // for ( int i = 0; i < surfaces.length; i++ ) {
        // Surface surface = surfaces[i];
        // pw.println( "<gml:Surface>" );
        // pw.println( "<gml:patches>" );
        // pw.println( "<gml:Polygon>" );
        // int numberSurfaces = surface.getNumberOfSurfacePatches();
        // for ( int j = 0; j < numberSurfaces; j++ ) {
        // SurfacePatch surfacePatch = surface.getSurfacePatchAt( j );
        // printExteriorRing( surface, pw, surfacePatch );
        // printInteriorRing( surface, pw, surfacePatch );
        // }
        // pw.println( "</gml:Polygon>" );
        // pw.println( "</gml:patches>" );
        // pw.println( "</gml:Surface>" );
        // }
        // pw.println( "</gml:surfaceMembers>" );
        pw.print("</gml:MultiSurface>");

    }

    /**
     * Converts the string representation of a GML geometry object to a corresponding
     * <code>Geometry</code>. Notice that GML Boxes will be converted to Surfaces because in ISO
     * 19107 Envelopes are no geometries.
     * 
     * @param gml
     * @return corresponding geometry object
     * @throws GeometryException
     * @throws XMLParsingException
     * @deprecated this method cannot provide default SRS information, please use
     *             {@link #wrap(String,String)} instead
     */
    @Deprecated
    public static Geometry wrap( String gml ) throws GeometryException, XMLParsingException {
        return wrap(gml, null);
    }

    /**
     * Converts a GML geometry object to a corresponding <tt>Geometry</tt>. Notice that GML Boxes
     * will be converted to Surfaces because in ISO 19107 Envelops are no geometries.
     * <p>
     * Currently, the following conversions are supported:
     * <ul>
     * <li>GML Point -> Point
     * <li>GML MultiPoint -> MultiPoint
     * <li>GML LineString -> Curve
     * <li>GML MultiLineString -> MultiCurve
     * <li>GML Polygon -> Surface
     * <li>GML MultiPolygon -> MultiSurface
     * <li>GML Box -> Surface
     * <li>GML Curve -> Curve
     * <li>GML Surface -> Surface
     * <li>GML MultiCurve -> MultiCurve
     * <li>GML MultiSurface -> MultiSurface
     * </ul>
     * <p>
     * 
     * @param gml
     * @return the corresponding <tt>Geometry</tt>
     * @throws GeometryException if type unsupported or conversion failed
     * @deprecated this method cannot provide default SRS information, please use
     *             {@link #wrap(Element,String)} instead
     */
    @Deprecated
    public static Geometry wrap( Element gml ) throws GeometryException {
        return wrap(gml, null);
    }

    /**
     * returns a Envelope created from Box element
     * 
     * @param element <boundedBy>
     * @return instance of <tt>Envelope</tt>
     * @throws XMLParsingException
     * @throws InvalidGMLException
     * @throws UnknownCRSException
     * @deprecated this method cannot provide default SRS information, please use
     *             {@link #wrapBox(Element,String)} instead
     */
    @Deprecated
    public static Envelope wrapBox( Element element ) throws XMLParsingException,
            InvalidGMLException, UnknownCRSException {
        return wrapBox(element, null);
    }

    /**
     * Corrects the rings if its necessary
     * 
     * @param createPositions
     * @return
     */
    private static Position[] correctRing( Position[] ringPositions ) {

        // [SBCALVO - 21/08/2008] - Correct the ring if necessary
        if (ringPositions != null && ringPositions.length > 2
            && !ringPositions[0].equals(ringPositions[ringPositions.length - 1])) {
            ringPositions =
                Arrays.copyOf(ringPositions, ringPositions.length + 1, Position[].class);
            ringPositions[ringPositions.length - 1] = ringPositions[0];
        } else if (ringPositions != null && ringPositions.length == 2) {
            // Fill until reach 4 positions
            ringPositions =
                Arrays.copyOf(ringPositions, ringPositions.length + 2, Position[].class);
            ringPositions[2] = ringPositions[0];
            ringPositions[3] = ringPositions[0];
        } else if (ringPositions != null && ringPositions.length == 1) {
            // Fill until reach 4 positions
            ringPositions =
                Arrays.copyOf(ringPositions, ringPositions.length + 3, Position[].class);
            ringPositions[1] = ringPositions[0];
            ringPositions[2] = ringPositions[0];
            ringPositions[3] = ringPositions[0];
        } else if (ringPositions != null && ringPositions.length == 0) {
            ringPositions = new Position[4];
            ringPositions[0] = new PositionImpl(0.0, 0.0);
            ringPositions[1] = new PositionImpl(0.0, 0.0);
            ringPositions[2] = new PositionImpl(0.0, 0.0);
            ringPositions[3] = new PositionImpl(0.0, 0.0);
        }
        return ringPositions;
    }
}
