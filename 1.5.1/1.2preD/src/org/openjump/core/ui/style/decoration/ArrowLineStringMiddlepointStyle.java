/*
 * Created on 23 juin 2005
 *
 * Olivier BEDEL 
 * Bassin Versant du Jaudy-Guindy-Bizien, 
 * Laboratoire RESO UMR ESO 6590 CNRS, Université de Rennes 2
 * 
 */
package org.openjump.core.ui.style.decoration;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.renderer.style.LineStringEndpointStyle;

/**
 * @author Olivier
 *
 */
public class ArrowLineStringMiddlepointStyle extends LineStringEndpointStyle {
    
    private final static double SMALL_ANGLE = 10;
    private final static double MEDIUM_ANGLE = 30;

    private final static double MEDIUM_LENGTH = 10;
    private final static double LARGE_LENGTH = 15;
    private boolean filled;
    private double finAngle;
    protected double finLength;
    
    public ArrowLineStringMiddlepointStyle(String name, boolean start, String iconFile,
            double finAngle, double finLength, boolean filled) {
        	super(name, IconLoader.icon(iconFile), start);
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
        arrowhead.moveTo((float) finTip1.getX(), (float) finTip1.getY());
        arrowhead.lineTo((float) shaftTip.getX(), (float) shaftTip.getY());
        arrowhead.lineTo((float) finTip2.getX(), (float) finTip2.getY());

        return arrowhead;
    }
    
    private Point2D fin(Point2D shaftTip, Point2D shaftTail, double length,
            double angle) {
            double shaftLength = shaftTip.distance(shaftTail);
            Point2D finTail = shaftTip;
            Point2D finTip = GUIUtil.add(GUIUtil.multiply(GUIUtil.subtract(
                            shaftTail, shaftTip), length / shaftLength), finTail);
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate((angle * Math.PI) / 180, finTail.getX(),
                finTail.getY());

            return affineTransform.transform(finTip, null);
        }
    
    protected void paint(Point2D terminal, Point2D next, Viewport viewport,
            Graphics2D graphics) throws NoninvertibleTransformException {
            if (terminal.equals(next)) {
                return;
            }

            graphics.setColor(lineColorWithAlpha);
            graphics.setStroke(stroke);
            Point2D middle = new Point();
            
            middle.setLocation((terminal.getX()+next.getX()) /2.0, (terminal.getY()+next.getY()) /2.0);
            GeneralPath arrowhead = arrowhead(middle, next, finLength, finAngle);

            if (filled) {
                arrowhead.closePath();
                graphics.fill(arrowhead);
            }

            //#fill isn't affected by line width, but #draw is. Therefore, draw even
            //if we've already filled. [Jon Aquino]
            graphics.draw(arrowhead);
        }
    
    public static class NarrowSolidMiddle extends ArrowLineStringMiddlepointStyle {
        public NarrowSolidMiddle() {
            super(I18N.get("ui.renderer.style.ArrowLineStringMiddlepointStyle.Middle-Arrow-Solid-Narrow"), false, "ArrowEndSolidNarrow.gif", //$NON-NLS-1$ //$NON-NLS-2$
                SMALL_ANGLE, LARGE_LENGTH, true);
        }
    }
}
