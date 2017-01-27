package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.MeasureLayerFinder;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
import de.latlon.deejump.plugin.style.VertexStylesFactory;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;

/**
 * A special Style for usage in the AdvancedMeasureTool.
 * This Style paints the area and distance in the center of the Geometry's
 * Envelope. At every vertex the actual distance will be painted.
 * 
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class MeasurementStyle implements Style {

	private Layer layer;
	private boolean enabled = false;
	private String areaAttribute = MeasureLayerFinder.FEATURE_ATTRIBUTE_AREA;
	private String lengthAttribute = MeasureLayerFinder.FEATURE_ATTRIBUTE_LENGTH;
	// summary variables
	private boolean paintSummaryLength = AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_PAINT_LENGTH;
	private boolean paintSummaryArea = AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_PAINT_AREA;
	private Font summaryFont = AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_FONT;
	private Color summaryColor = AdvancedMeasureOptionsPanel.DEFAULT_SUMMARY_COLOR;
	// vertex variables
	private boolean vertexPaintDistance = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT_DISTANCE;
	private boolean vertexPaintDistanceRelative = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT_DISTANCE_RELATIVE;
	private Font vertexFont = new Font("Dialog", Font.PLAIN, 12);
	private Color vertexFontColor = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FONT_COLOR;
	private boolean vertexPaint = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_PAINT;
	private Color vertexFirstColor = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_COLOR;
	private String vertexFirstForm = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_FORM;
	private int vertexFirstSize = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FIRST_SIZE;
	private Color vertexFollowingColor = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_COLOR;
	private String vertexFollowingForm = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_FORM;
	private int vertexFollowingSize = AdvancedMeasureOptionsPanel.DEFAULT_VERTEX_FOLLOWING_SIZE;
	private VertexStyle vertexStyleFirst;
	private VertexStyle vertexStyleFollowing;

	public MeasurementStyle() {
		// create the both VertexStyle's
		vertexStyleFirst = VertexStylesFactory.createVertexStyle(vertexFirstForm);
		vertexStyleFirst.setFillColor(vertexFirstColor);
		vertexStyleFirst.setAlpha(255);
		vertexStyleFirst.setSize(vertexFirstSize);
		vertexStyleFollowing = VertexStylesFactory.createVertexStyle(vertexFollowingForm);
		vertexStyleFollowing.setFillColor(vertexFollowingColor);
		vertexStyleFollowing.setAlpha(255);
		vertexStyleFollowing.setSize(vertexFollowingSize);
	}

	public void paint(Feature f, Graphics2D g, Viewport viewport) throws Exception {
		Point2D centerPoint;
		TextLayout layout;

		// get area and length values
		Double area = (Double) f.getAttribute(areaAttribute);
		Double length = (Double) f.getAttribute(lengthAttribute);

		// formatting area and length values
		DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
		String formatPattern = "#,##0.00";
		// adaptive format pattern, thanks to MichaÃ«l Michaud for his idea!
		if (length >= 10) {
			formatPattern = "#,##0.00";
		} else if (length >= 1) {
			formatPattern = "#,##0.000";
		} else if (length >= 0.1) {
			formatPattern = "#,##0.0000";
		} else if (length >= 0.01) {
			formatPattern ="#,##0.00000";
		} else formatPattern = "#,##0.000000";
		decimalFormat.applyPattern(formatPattern);
		
		// length per vertex and vertex himself
		Coordinate[] coordinates = f.getGeometry().getCoordinates();
		double actualLength = 0;
		// in area mode we do not paint the last vertex, because we paint only the first one (which is at the same coordinate)
		int numberOfCoordinates = coordinates.length;
		for (int i = 0; i < numberOfCoordinates; i++) {
			centerPoint = viewport.toViewPoint(coordinates[i]);
			if (i > 0) {
				// paint the vertex only if vertexPaint is true AND ( in area mode it's not the last vertex OR in distance mode )
				if (vertexPaint && (area > 0 && i < numberOfCoordinates - 1 || area == 0)) {
					vertexStyleFollowing.paint(g, centerPoint);
				}
                g.setColor(vertexFontColor);
                double partLength = coordinates[i].distance(coordinates[i-1]);
				if (vertexPaintDistance) {
                    actualLength += partLength; 
        			layout = new TextLayout(decimalFormat.format(actualLength) + "m", vertexFont, g.getFontRenderContext());
                    layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() - layout.getAscent());
                }
                if (vertexPaintDistanceRelative) {
                    boolean rotateText = false;
                    double angle;
                    // compute the rotation of this part
                    LineSegment lineSegment = new LineSegment(coordinates[i], coordinates[i -1]);
                    /*
                     * The angle() method returns a counterclockwise angle in radians from -PI to PI (-180° to 180°).
                     * Graphics2D.rotate use clockwise angle from 0 to 2PI.
                     * So we must invert (* -1) and shift the range ( + Math.PI) to get the same.
                     */
                    angle = lineSegment.angle() * -1 + Math.PI;
                    // take care, that the text is not upside down
                    // from PI/2 (90°) to PI*1.5 (270°) we rotate for PI (180°)
                    if (angle > Math.PI / 2 && angle < Math.PI * 1.5) {
                        angle += Math.PI;
                        rotateText = true; // indicator for decision of outside printing, see below
                    }
                    // for relative distance the optimal text position is between the two points
                    centerPoint = viewport.toViewPoint(LineSegment.midPoint(coordinates[i], coordinates[i -1]));
                    // save transformation
                    AffineTransform transform = g.getTransform();
                    g.rotate(angle, centerPoint.getX(), centerPoint.getY());
                    layout = new TextLayout(decimalFormat.format(partLength) + "m", vertexFont, g.getFontRenderContext());
                    // next line would paint the text on the line
                    // layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() + layout.getAscent() / 2);
                    // but for a better view we paint above the line
                    // The text should allways outside of the geometry. This is looks like better for areas!
                    // We distinguish this by the rotation decision for the upside down problem above.
                    if (rotateText) {
                        layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() + layout.getAscent());
                    } else {
                        layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() - 3);
                    }
                    // after rotate and draw, restore transformation
                    g.setTransform(transform);
                }
			} else {
				if (vertexPaint) {
					vertexStyleFirst.paint(g, centerPoint);
				}
                // in relative distance mode we do not have a distance to the previous, because its the first point
				if (vertexPaintDistance) {
				layout = new TextLayout(decimalFormat.format(actualLength) + "m", vertexFont, g.getFontRenderContext());
					g.setColor(vertexFontColor);
					layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() + layout.getAscent() + 5);
				}
			}
		}

		// paint summary or not ;-)
		// paint the area (if area measurement) and length
		g.setColor(summaryColor);
		centerPoint = viewport.toViewPoint(f.getGeometry().getEnvelope().getCentroid().getCoordinate());
		double x = centerPoint.getX();
		double y = centerPoint.getY();
		if (paintSummaryLength) {
			layout = new TextLayout(I18N.get("org.openjump.core.ui.plugin.tools.MeasurementStyle.distance") + " " + decimalFormat.format(length) + "m", summaryFont, g.getFontRenderContext());
			x -= layout.getAdvance() / 2;
			layout.draw(g, (float) x, (float) y);
			y += layout.getAscent();
		}
		if (area > 0 && paintSummaryArea) {
			layout = new TextLayout(I18N.get("org.openjump.core.ui.plugin.tools.MeasurementStyle.area") + " " + decimalFormat.format(area) + "m\u00B2", summaryFont, g.getFontRenderContext());
			if (!paintSummaryLength) x -= layout.getAdvance() / 2;
			layout.draw(g, (float) x, (float) y);
		}
	}

	public void initialize(Layer layer) {
		this.layer = layer;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			Assert.shouldNeverReachHere();
			return null;
		}
	}

	/**
	 * @return the areaAttribute
	 */
	public String getAreaAttribute() {
		return areaAttribute;
	}

	/**
	 * @param areaAttribute the areaAttribute to set
	 */
	public void setAreaAttribute(String areaAttribute) {
		this.areaAttribute = areaAttribute;
	}

	/**
	 * @return the lengthAttribute
	 */
	public String getLengthAttribute() {
		return lengthAttribute;
	}

	/**
	 * @param lengthAttribute the lengthAttribute to set
	 */
	public void setLengthAttribute(String lengthAttribute) {
		this.lengthAttribute = lengthAttribute;
	}

	/**
	 * @return the font
	 */
	public Font getSummaryFont() {
		return summaryFont;
	}

	/**
	 * @param font the font to set
	 */
	public void setSummaryFont(Font font) {
		this.summaryFont = font;
	}

	/**
	 * @return the summaryColor
	 */
	public Color getSummaryColor() {
		return summaryColor;
	}

	/**
	 * @param summaryColor the summaryColor to set
	 */
	public void setSummaryColor(Color summaryColor) {
		this.summaryColor = summaryColor;
	}

	/**
	 * @return the paintSummary
	 */
	public boolean isPaintSummaryLength() {
		return paintSummaryLength;
	}

	/**
	 * @param paintSummary the paintSummary to set
	 */
	public void setPaintSummaryLength(boolean paintSummary) {
		this.paintSummaryLength = paintSummary;
	}

	/**
	 * @return the paintSummaryArea
	 */
	public boolean isPaintSummaryArea() {
		return paintSummaryArea;
	}

	/**
	 * @param paintSummaryArea the paintSummaryArea to set
	 */
	public void setPaintSummaryArea(boolean paintSummaryArea) {
		this.paintSummaryArea = paintSummaryArea;
	}

	/**
	 * @return the vertexFont
	 */
	public Font getVertexFont() {
		return vertexFont;
	}

	/**
	 * @param vertexFont the vertexFont to set
	 */
	public void setVertexFont(Font vertexFont) {
		this.vertexFont = vertexFont;
	}

	/**
	 * @return the vertexFontColor
	 */
	public Color getVertexFontColor() {
		return vertexFontColor;
	}

	/**
	 * @param vertexFontColor the vertexFontColor to set
	 */
	public void setVertexFontColor(Color vertexFontColor) {
		this.vertexFontColor = vertexFontColor;
	}

	/**
	 * @return the vertexPaintDistance
	 */
	public boolean isVertexPaintDistance() {
		return vertexPaintDistance;
	}

	/**
	 * @param vertexPaintDistance the vertexPaintDistance to set
	 */
	public void setVertexPaintDistance(boolean vertexPaintDistance) {
		this.vertexPaintDistance = vertexPaintDistance;
	}

	/**
	 * @return the vertexPaint
	 */
	public boolean isVertexPaint() {
		return vertexPaint;
	}

	/**
	 * @param vertexPaint the vertexPaint to set
	 */
	public void setVertexPaint(boolean vertexPaint) {
		this.vertexPaint = vertexPaint;
	}

	/**
	 * @return the vertexFirstColor
	 */
	public Color getVertexFirstColor() {
		return vertexFirstColor;
	}

	/**
	 * @param vertexFirstColor the vertexFirstColor to set
	 */
	public void setVertexFirstColor(Color vertexFirstColor) {
		this.vertexFirstColor = vertexFirstColor;
		vertexStyleFirst.setFillColor(vertexFirstColor);
	}

	/**
	 * @return the vertexFirstForm
	 */
	public String getVertexFirstForm() {
		return vertexFirstForm;
	}

	/**
	 * @param vertexFirstForm the vertexFirstForm to set
	 */
	public void setVertexFirstForm(String vertexFirstForm) {
		this.vertexFirstForm = vertexFirstForm;
		vertexStyleFirst = VertexStylesFactory.createVertexStyle(vertexFirstForm);
		vertexStyleFirst.setFillColor(vertexFirstColor);
		vertexStyleFirst.setAlpha(255);
		vertexStyleFirst.setSize(vertexFirstSize);
	}

	/**
	 * @return the vertexFirstSize
	 */
	public int getVertexFirstSize() {
		return vertexFirstSize;
	}

	/**
	 * @param vertexFirstSize the vertexFirstSize to set
	 */
	public void setVertexFirstSize(int vertexFirstSize) {
		this.vertexFirstSize = vertexFirstSize;
		vertexStyleFirst.setSize(vertexFirstSize);
	}

	/**
	 * @return the vertexFollowingColor
	 */
	public Color getVertexFollowingColor() {
		return vertexFollowingColor;
	}

	/**
	 * @param vertexFollowingColor the vertexFollowingColor to set
	 */
	public void setVertexFollowingColor(Color vertexFollowingColor) {
		this.vertexFollowingColor = vertexFollowingColor;
		vertexStyleFollowing.setFillColor(vertexFollowingColor);
	}

	/**
	 * @return the vertexFollowingForm
	 */
	public String getVertexFollowingForm() {
		return vertexFollowingForm;
	}

	/**
	 * @param vertexFollowingForm the vertexFollowingForm to set
	 */
	public void setVertexFollowingForm(String vertexFollowingForm) {
		this.vertexFollowingForm = vertexFollowingForm;
		vertexStyleFollowing = VertexStylesFactory.createVertexStyle(vertexFollowingForm);
		vertexStyleFollowing.setFillColor(vertexFollowingColor);
		vertexStyleFollowing.setAlpha(255);
		vertexStyleFollowing.setSize(vertexFollowingSize);
	}

	/**
	 * @return the vertexFollowingSize
	 */
	public int getVertexFollowingSize() {
		return vertexFollowingSize;
	}

	/**
	 * @param vertexFollowingSize the vertexFollowingSize to set
	 */
	public void setVertexFollowingSize(int vertexFollowingSize) {
		this.vertexFollowingSize = vertexFollowingSize;
		vertexStyleFollowing.setSize(vertexFollowingSize);
	}

    /**
     * @return the vertexPaintDistanceRelative
     */
    public boolean isVertexPaintDistanceRelative() {
        return vertexPaintDistanceRelative;
    }

    /**
     * @param vertexPaintDistanceRelative the vertexPaintDistanceRelative to set
     */
    public void setVertexPaintDistanceRelative(boolean vertexPaintDistanceRelative) {
        this.vertexPaintDistanceRelative = vertexPaintDistanceRelative;
    }


}
