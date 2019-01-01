package org.openjump.core.ui.style.decoration;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LineStringSegmentStyle;

/**
 * @author Paul Austin
 */
public abstract class SegmentDownhillArrowStyle extends LineStringSegmentStyle {

  private final static double SMALL_ANGLE = 10;

  private final static double MEDIUM_ANGLE = 30;

  private final static double MEDIUM_LENGTH = 10;

  private final static double LARGE_LENGTH = 15;

  private boolean filled;

  private double finAngle;

  protected double finLength;

  public SegmentDownhillArrowStyle(String name, String iconFile,
    double finAngle, double finLength, boolean filled) {
    super(I18N.get(SegmentDownhillArrowStyle.class.getName()
      + "." + name), IconLoader.icon(iconFile));
    this.finAngle = finAngle;
    this.finLength = finLength;
    this.filled = filled;
  }

  /**
   * @param tail the tail of the whole arrow; just used to determine angle
   * @param finLength required distance from the tip to each fin's tip
   */
  private GeneralPath arrowhead(Point2D shaftTip, Point2D shaftTail,
    double finLength, double finAngle) {
    GeneralPath arrowhead = new GeneralPath();
    Point2D finTip1 = fin(shaftTip, shaftTail, finLength, finAngle);
    Point2D finTip2 = fin(shaftTip, shaftTail, finLength, -finAngle);
    arrowhead.moveTo((float)finTip1.getX(), (float)finTip1.getY());
    arrowhead.lineTo((float)shaftTip.getX(), (float)shaftTip.getY());
    arrowhead.lineTo((float)finTip2.getX(), (float)finTip2.getY());

    return arrowhead;
  }

  private Point2D fin(Point2D shaftTip, Point2D shaftTail, double length,
    double angle) {
    double shaftLength = shaftTip.distance(shaftTail);
    Point2D finTail = shaftTip;
    Point2D finTip = GUIUtil.add(GUIUtil.multiply(GUIUtil.subtract(shaftTail,
      shaftTip), length / shaftLength), finTail);
    AffineTransform affineTransform = new AffineTransform();
    affineTransform.rotate((angle * Math.PI) / 180, finTail.getX(),
      finTail.getY());

    return affineTransform.transform(finTip, null);
  }

  protected void paint(Coordinate terminal, Coordinate next, Viewport viewport,
    Graphics2D graphics) throws Exception {
    Point2D startPoint = viewport.toViewPoint(new Point2D.Double(terminal.x,
      terminal.y));
    Point2D endPoint = viewport.toViewPoint(new Point2D.Double(next.x, next.y));
    if (terminal.z != next.z && !Double.isNaN(terminal.z)
      && !Double.isNaN(next.z)) {
      if (terminal.z < next.z) {
        paint(startPoint, endPoint, viewport, graphics);
      } else {
        paint(endPoint, startPoint, viewport, graphics);
      }
    }
  }

  protected void paint(Point2D terminal, Point2D next, Viewport viewport,
    Graphics2D graphics) throws NoninvertibleTransformException {
    if (terminal.equals(next)) {
      return;
    }

    graphics.setColor(lineColorWithAlpha);
    graphics.setStroke(stroke);
    Point2D middle = new Point();

    middle.setLocation((terminal.getX() + next.getX()) / 2.0,
      (terminal.getY() + next.getY()) / 2.0);
    GeneralPath arrowhead = arrowhead(middle, next, finLength, finAngle);

    if (filled) {
      arrowhead.closePath();
      graphics.fill(arrowhead);
    }
    graphics.draw(arrowhead);
  }

  public static class Open extends SegmentDownhillArrowStyle {
    public Open() {
      super("Open", "ArrowMidOpen.gif", MEDIUM_ANGLE, MEDIUM_LENGTH, false);
    }
  }

  public static class Solid extends SegmentDownhillArrowStyle {
    public Solid() {
      super("Solid", "ArrowMidSolid.gif", MEDIUM_ANGLE, MEDIUM_LENGTH, true);
    }
  }

  public static class NarrowSolidMiddle extends SegmentDownhillArrowStyle {
    public NarrowSolidMiddle() {
      super("SolidNarrow", "ArrowMidSolidNarrow.gif", SMALL_ANGLE, LARGE_LENGTH,
        true);
    }
  }
}
