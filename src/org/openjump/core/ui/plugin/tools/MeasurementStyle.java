package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.MeasureLayerFinder;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
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
	private Font font1 = new Font("Dialog", Font.PLAIN, 24);
	private Font font2 = new Font("Dialog", Font.PLAIN, 12);

	public void paint(Feature f, Graphics2D g, Viewport viewport) throws Exception {
		Point2D centerPoint;
		TextLayout layout;
		// get and format area and length values
		Double area = (Double) f.getAttribute(areaAttribute);
		Double length = (Double) f.getAttribute(lengthAttribute);
		DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
		decimalFormat.applyPattern("#,##0.000");
		// paint the area (if area measurement) and length
		g.setColor(Color.black);
		centerPoint = viewport.toViewPoint(f.getGeometry().getEnvelope().getCentroid().getCoordinate());
		double x = centerPoint.getX();
		double y = centerPoint.getY();
		layout = new TextLayout("Distance: " + decimalFormat.format(length) + "m", font1, g.getFontRenderContext());
		x -= layout.getAdvance() / 2;
		layout.draw(g, (float) x, (float) y);
		y += layout.getAscent();
		if (area > 0) {
			layout = new TextLayout("Area: " + decimalFormat.format(area) + "m²", font1, g.getFontRenderContext());
			layout.draw(g, (float) x, (float) y);
		} // TODO: i18n

		// length per vertex and vertex himself
		int vertexSize = 6;
		Coordinate[] coordinates = f.getGeometry().getCoordinates();
		double actualLength = 0;
		for (int i = 0; i < coordinates.length; i++) {
			if (i > 0) {
				actualLength += coordinates[i].distance(coordinates[i-1]);
				layout = new TextLayout(decimalFormat.format(actualLength) + "m", font2, g.getFontRenderContext());
				centerPoint = viewport.toViewPoint(coordinates[i]);
				g.setColor(Color.black);
				layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() - layout.getAscent());
				g.setColor(Color.red);
				g.fillRect((int) (centerPoint.getX() - vertexSize / 2), (int) (centerPoint.getY() - vertexSize / 2), vertexSize, vertexSize);
			} else {
				layout = new TextLayout(decimalFormat.format(actualLength) + "m", font2, g.getFontRenderContext());
				centerPoint = viewport.toViewPoint(coordinates[i]);
				g.setColor(Color.black);
				layout.draw(g, (float) centerPoint.getX() - layout.getAdvance() / 2, (float) centerPoint.getY() + layout.getAscent() + 5);
				g.setColor(Color.magenta);
				g.fillRect((int) (centerPoint.getX() - vertexSize / 2), (int) (centerPoint.getY() - vertexSize / 2), vertexSize, vertexSize);
			}
		}
		// if we are in area measurement mode, then paint the first vertex again, because it was overpainted by the last vertex
		if (coordinates.length > 0 && area > 0) {
			centerPoint = viewport.toViewPoint(coordinates[0]);
			g.setColor(Color.magenta);
			g.fillRect((int) (centerPoint.getX() - vertexSize / 2), (int) (centerPoint.getY() - vertexSize / 2), vertexSize, vertexSize);
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
	public Font getFont() {
		return font1;
	}

	/**
	 * @param font the font to set
	 */
	public void setFont(Font font) {
		this.font1 = font;
	}


}
