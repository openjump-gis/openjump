package com.vividsolutions.jump.io;

import com.vividsolutions.jts.geom.*;

import com.vividsolutions.jts.util.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Outputs the textual representation of a {@link Geometry}.
 * <p>
 * The <code>WKTWriter</code> outputs coordinates rounded to the precision
 * model. No more than the maximum number of necessary decimal places will be
 * output.
 * <p>
 * The Well-known Text format is defined in the <A
 * HREF="http://www.opengis.org/techno/specs.htm">OpenGIS Simple Features
 * Specification for SQL </A>.
 * <p>
 * A non-standard "LINEARRING" tag is used for LinearRings. The WKT spec does
 * not define a special tag for LinearRings. The standard tag to use is
 * "LINESTRING".
 * 
 * @version 1.4
 */
// Writes z-coordinates if they are not NaN. Will be moved into JTS in
// the future. [Jon Aquino 2004-10-25]
public class FUTURE_JTS_WKTWriter {

	private static int INDENT = 2;

	/**
	 * Creates the <code>DecimalFormat</code> used to write
	 * <code>double</code> s with a sufficient number of decimal places.
	 * 
	 * @param precisionModel
	 *            the <code>PrecisionModel</code> used to determine the number
	 *            of decimal places to write.
	 * @return a <code>DecimalFormat</code> that write <code>double</code> s
	 *         without scientific notation.
	 */
	private static DecimalFormat createFormatter(PrecisionModel precisionModel) {
		// the default number of decimal places is 16, which is sufficient
		// to accomodate the maximum precision of a double.
		int decimalPlaces = precisionModel.getMaximumSignificantDigits();
		// specify decimal separator explicitly to avoid problems in other
		// locales
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		return new DecimalFormat("#" + (decimalPlaces > 0 ? "." : "")
				+ stringOfChar('#', decimalPlaces), symbols);
	}

	/**
	 * Returns a <code>String</code> of repeated characters.
	 * 
	 * @param ch
	 *            the character to repeat
	 * @param count
	 *            the number of times to repeat the character
	 * @return a <code>String</code> of characters
	 */
	public static String stringOfChar(char ch, int count) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < count; i++) {
			buf.append(ch);
		}
		return buf.toString();
	}

	private DecimalFormat formatter;

	private boolean isFormatted = false;

	private int level = 0;

	public FUTURE_JTS_WKTWriter() {
	}

	/**
	 * Converts a <code>Geometry</code> to its Well-known Text representation.
	 * 
	 * @param geometry
	 *            a <code>Geometry</code> to process
	 * @return a <Geometry Tagged Text> string (see the OpenGIS Simple Features
	 *         Specification)
	 */
	public String write(Geometry geometry) {
		Writer sw = new StringWriter();
		try {
			writeFormatted(geometry, false, sw);
		} catch (IOException ex) {
			Assert.shouldNeverReachHere();
		}
		return sw.toString();
	}

	/**
	 * Converts a <code>Geometry</code> to its Well-known Text representation.
	 * 
	 * @param geometry
	 *            a <code>Geometry</code> to process
	 * @return a <Geometry Tagged Text> string (see the OpenGIS Simple Features
	 *         Specification)
	 */
	public void write(Geometry geometry, Writer writer) throws IOException {
		writeFormatted(geometry, false, writer);
	}

	/**
	 * Same as <code>write</code>, but with newlines and spaces to make the
	 * well-known text more readable.
	 * 
	 * @param geometry
	 *            a <code>Geometry</code> to process
	 * @return a <Geometry Tagged Text> string (see the OpenGIS Simple Features
	 *         Specification), with newlines and spaces
	 */
	public String writeFormatted(Geometry geometry) {
		Writer sw = new StringWriter();
		try {
			writeFormatted(geometry, true, sw);
		} catch (IOException ex) {
			Assert.shouldNeverReachHere();
		}
		return sw.toString();
	}

	/**
	 * Same as <code>write</code>, but with newlines and spaces to make the
	 * well-known text more readable.
	 * 
	 * @param geometry
	 *            a <code>Geometry</code> to process
	 * @return a <Geometry Tagged Text> string (see the OpenGIS Simple Features
	 *         Specification), with newlines and spaces
	 */
	public void writeFormatted(Geometry geometry, Writer writer)
			throws IOException {
		writeFormatted(geometry, true, writer);
	}

	/**
	 * Converts a <code>Geometry</code> to its Well-known Text representation.
	 * 
	 * @param geometry
	 *            a <code>Geometry</code> to process
	 * @return a <Geometry Tagged Text> string (see the OpenGIS Simple Features
	 *         Specification)
	 */
	private void writeFormatted(Geometry geometry, boolean isFormatted,
			Writer writer) throws IOException {
		this.isFormatted = isFormatted;
		formatter = createFormatter(geometry.getPrecisionModel());
		appendGeometryTaggedText(geometry, 0, writer);
	}

	/**
	 * Converts a <code>Geometry</code> to &lt;Geometry Tagged Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param geometry
	 *            the <code>Geometry</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendGeometryTaggedText(Geometry geometry, int level,
			Writer writer) throws IOException {
		indent(level, writer);

		if (geometry instanceof Point) {
			Point point = (Point) geometry;
			appendPointTaggedText(point.getCoordinate(), level, writer, point
					.getPrecisionModel());
		} else if (geometry instanceof LinearRing) {
			appendLinearRingTaggedText((LinearRing) geometry, level, writer);
		} else if (geometry instanceof LineString) {
			appendLineStringTaggedText((LineString) geometry, level, writer);
		} else if (geometry instanceof Polygon) {
			appendPolygonTaggedText((Polygon) geometry, level, writer);
		} else if (geometry instanceof MultiPoint) {
			appendMultiPointTaggedText((MultiPoint) geometry, level, writer);
		} else if (geometry instanceof MultiLineString) {
			appendMultiLineStringTaggedText((MultiLineString) geometry, level,
					writer);
		} else if (geometry instanceof MultiPolygon) {
			appendMultiPolygonTaggedText((MultiPolygon) geometry, level, writer);
		} else if (geometry instanceof GeometryCollection) {
			appendGeometryCollectionTaggedText((GeometryCollection) geometry,
					level, writer);
		} else {
			Assert.shouldNeverReachHere("Unsupported Geometry implementation:"
					+ geometry.getClass());
		}
	}

	/**
	 * Converts a <code>Coordinate</code> to &lt;Point Tagged Text&gt; format,
	 * then appends it to the writer.
	 * 
	 * @param coordinate
	 *            the <code>Coordinate</code> to process
	 * @param writer
	 *            the output writer to append to
	 * @param precisionModel
	 *            the <code>PrecisionModel</code> to use to convert from a
	 *            precise coordinate to an external coordinate
	 */
	private void appendPointTaggedText(Coordinate coordinate, int level,
			Writer writer, PrecisionModel precisionModel) throws IOException {
		writer.write("POINT ");
		appendPointText(coordinate, level, writer, precisionModel);
	}

	/**
	 * Converts a <code>LineString</code> to &lt;LineString Tagged Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param lineString
	 *            the <code>LineString</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendLineStringTaggedText(LineString lineString, int level,
			Writer writer) throws IOException {
		writer.write("LINESTRING ");
		appendLineStringText(lineString, level, false, writer);
	}

	/**
	 * Converts a <code>LinearRing</code> to &lt;LinearRing Tagged Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param linearRing
	 *            the <code>LinearRing</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendLinearRingTaggedText(LinearRing linearRing, int level,
			Writer writer) throws IOException {
		writer.write("LINEARRING ");
		appendLineStringText(linearRing, level, false, writer);
	}

	/**
	 * Converts a <code>Polygon</code> to &lt;Polygon Tagged Text&gt; format,
	 * then appends it to the writer.
	 * 
	 * @param polygon
	 *            the <code>Polygon</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendPolygonTaggedText(Polygon polygon, int level,
			Writer writer) throws IOException {
		writer.write("POLYGON ");
		appendPolygonText(polygon, level, false, writer);
	}

	/**
	 * Converts a <code>MultiPoint</code> to &lt;MultiPoint Tagged Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param multipoint
	 *            the <code>MultiPoint</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiPointTaggedText(MultiPoint multipoint, int level,
			Writer writer) throws IOException {
		writer.write("MULTIPOINT ");
		appendMultiPointText(multipoint, level, writer);
	}

	/**
	 * Converts a <code>MultiLineString</code> to &lt;MultiLineString Tagged
	 * Text&gt; format, then appends it to the writer.
	 * 
	 * @param multiLineString
	 *            the <code>MultiLineString</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiLineStringTaggedText(
			MultiLineString multiLineString, int level, Writer writer)
			throws IOException {
		writer.write("MULTILINESTRING ");
		appendMultiLineStringText(multiLineString, level, false, writer);
	}

	/**
	 * Converts a <code>MultiPolygon</code> to &lt;MultiPolygon Tagged
	 * Text&gt; format, then appends it to the writer.
	 * 
	 * @param multiPolygon
	 *            the <code>MultiPolygon</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiPolygonTaggedText(MultiPolygon multiPolygon,
			int level, Writer writer) throws IOException {
		writer.write("MULTIPOLYGON ");
		appendMultiPolygonText(multiPolygon, level, writer);
	}

	/**
	 * Converts a <code>GeometryCollection</code> to &lt;GeometryCollection
	 * Tagged Text&gt; format, then appends it to the writer.
	 * 
	 * @param geometryCollection
	 *            the <code>GeometryCollection</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendGeometryCollectionTaggedText(
			GeometryCollection geometryCollection, int level, Writer writer)
			throws IOException {
		writer.write("GEOMETRYCOLLECTION ");
		appendGeometryCollectionText(geometryCollection, level, writer);
	}

	/**
	 * Converts a <code>Coordinate</code> to &lt;Point Text&gt; format, then
	 * appends it to the writer.
	 * 
	 * @param coordinate
	 *            the <code>Coordinate</code> to process
	 * @param writer
	 *            the output writer to append to
	 * @param precisionModel
	 *            the <code>PrecisionModel</code> to use to convert from a
	 *            precise coordinate to an external coordinate
	 */
	private void appendPointText(Coordinate coordinate, int level,
			Writer writer, PrecisionModel precisionModel) throws IOException {
		if (coordinate == null) {
			writer.write("EMPTY");
		} else {
			writer.write("(");
			appendCoordinate(coordinate, writer, precisionModel);
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>Coordinate</code> to &lt;Point&gt; format, then
	 * appends it to the writer.
	 * 
	 * @param coordinate
	 *            the <code>Coordinate</code> to process
	 * @param writer
	 *            the output writer to append to
	 * @param precisionModel
	 *            the <code>PrecisionModel</code> to use to convert from a
	 *            precise coordinate to an external coordinate
	 */
	private void appendCoordinate(Coordinate coordinate, Writer writer,
			PrecisionModel precisionModel) throws IOException {
		writer.write(writeNumber(coordinate.x)
				+ " "
				+ writeNumber(coordinate.y)
				+ (Double.isNaN(coordinate.z) ? "" : " "
						+ writeNumber(coordinate.z)));
	}

	/**
	 * Converts a <code>double</code> to a <code>String</code>, not in
	 * scientific notation.
	 * 
	 * @param d
	 *            the <code>double</code> to convert
	 * @return the <code>double</code> as a <code>String</code>, not in
	 *         scientific notation
	 */
	private String writeNumber(double d) {
		return formatter.format(d);
	}

	/**
	 * Converts a <code>LineString</code> to &lt;LineString Text&gt; format,
	 * then appends it to the writer.
	 * 
	 * @param lineString
	 *            the <code>LineString</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendLineStringText(LineString lineString, int level,
			boolean doIndent, Writer writer) throws IOException {
		if (lineString.isEmpty()) {
			writer.write("EMPTY");
		} else {
			if (doIndent)
				indent(level, writer);
			writer.write("(");
			for (int i = 0; i < lineString.getNumPoints(); i++) {
				if (i > 0) {
					writer.write(", ");
					if (i % 10 == 0)
						indent(level + 2, writer);
				}
				appendCoordinate(lineString.getCoordinateN(i), writer,
						lineString.getPrecisionModel());
			}
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>Polygon</code> to &lt;Polygon Text&gt; format, then
	 * appends it to the writer.
	 * 
	 * @param polygon
	 *            the <code>Polygon</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendPolygonText(Polygon polygon, int level,
			boolean indentFirst, Writer writer) throws IOException {
		if (polygon.isEmpty()) {
			writer.write("EMPTY");
		} else {
			if (indentFirst)
				indent(level, writer);
			writer.write("(");
			appendLineStringText(polygon.getExteriorRing(), level, false,
					writer);
			for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
				writer.write(", ");
				appendLineStringText(polygon.getInteriorRingN(i), level + 1,
						true, writer);
			}
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>MultiPoint</code> to &lt;MultiPoint Text&gt; format,
	 * then appends it to the writer.
	 * 
	 * @param multiPoint
	 *            the <code>MultiPoint</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiPointText(MultiPoint multiPoint, int level,
			Writer writer) throws IOException {
		if (multiPoint.isEmpty()) {
			writer.write("EMPTY");
		} else {
			writer.write("(");
			for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
				if (i > 0) {
					writer.write(", ");
				}
				appendCoordinate(((Point) multiPoint.getGeometryN(i))
						.getCoordinate(), writer, multiPoint
						.getPrecisionModel());
			}
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>MultiLineString</code> to &lt;MultiLineString Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param multiLineString
	 *            the <code>MultiLineString</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiLineStringText(MultiLineString multiLineString,
			int level, boolean indentFirst, Writer writer) throws IOException {
		if (multiLineString.isEmpty()) {
			writer.write("EMPTY");
		} else {
			int level2 = level;
			boolean doIndent = indentFirst;
			writer.write("(");
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				if (i > 0) {
					writer.write(", ");
					level2 = level + 1;
					doIndent = true;
				}
				appendLineStringText((LineString) multiLineString
						.getGeometryN(i), level2, doIndent, writer);
			}
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>MultiPolygon</code> to &lt;MultiPolygon Text&gt;
	 * format, then appends it to the writer.
	 * 
	 * @param multiPolygon
	 *            the <code>MultiPolygon</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendMultiPolygonText(MultiPolygon multiPolygon, int level,
			Writer writer) throws IOException {
		if (multiPolygon.isEmpty()) {
			writer.write("EMPTY");
		} else {
			int level2 = level;
			boolean doIndent = false;
			writer.write("(");
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				if (i > 0) {
					writer.write(", ");
					level2 = level + 1;
					doIndent = true;
				}
				appendPolygonText((Polygon) multiPolygon.getGeometryN(i),
						level2, doIndent, writer);
			}
			writer.write(")");
		}
	}

	/**
	 * Converts a <code>GeometryCollection</code> to
	 * &lt;GeometryCollectionText&gt; format, then appends it to the writer.
	 * 
	 * @param geometryCollection
	 *            the <code>GeometryCollection</code> to process
	 * @param writer
	 *            the output writer to append to
	 */
	private void appendGeometryCollectionText(
			GeometryCollection geometryCollection, int level, Writer writer)
			throws IOException {
		if (geometryCollection.isEmpty()) {
			writer.write("EMPTY");
		} else {
			int level2 = level;
			writer.write("(");
			for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
				if (i > 0) {
					writer.write(", ");
					level2 = level + 1;
				}
				appendGeometryTaggedText(geometryCollection.getGeometryN(i),
						level2, writer);
			}
			writer.write(")");
		}
	}

	private void indent(int level, Writer writer) throws IOException {
		if (!isFormatted || level <= 0)
			return;
		writer.write("\n");
		writer.write(stringOfChar(' ', INDENT * level));
	}

}

