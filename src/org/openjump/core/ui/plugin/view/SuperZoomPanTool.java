package org.openjump.core.ui.plugin.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.cursortool.DragTool;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * This tool have the following functions:
 * - zoom in/out with left/right mouse click
 * - pan with mouse drag
 * - zoom in/out with mousewheel and then left click for zoom or right click for cancel
 * In wheelMode you can see the new area after zooming. Also known as "Area of interest".
 * 
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class SuperZoomPanTool extends DragTool implements MouseWheelListener {

	public static final double WHEEL_ZOOM_FACTOR = 1.15;
	public static final double ZOOM_IN_FACTOR = 2;
	
	public static Cursor CURSOR_ZOOM = createCursor(IconLoader.icon("MagnifyCursor.gif").getImage());
	public static Cursor CURSOR_PAN = createCursor(IconLoader.icon("Hand.gif").getImage());
	public static Cursor CURSOR_WHEEL = createCursor(IconLoader.icon("MagnifyAreaCursor.gif").getImage());
    
    /**
     * The visual indicator (area of interest) is painted as a Shape.
     */
    public static final int INDICATOR_MODE_SHAPE = 1;
    /**
     * The visual indicator (area of interest) is painted as an Image.
     */
    public static final int INDICATOR_MODE_IMAGE = 2;
	
	private boolean dragging = false;
	private Image origImage;
	private Image auxImage = null;
	private Point mousePosition = null;
	private boolean wheelMode = false;
	private int mouseWheelCount;
	private double scale = 1d;
	private boolean mouseWheelListenerAdded = false;
	private boolean isAnimatingZoom = false;
	private Timer zoomPanClickTimer = null;
    private int indicatorMode = INDICATOR_MODE_IMAGE;
    private Point imagePosition = null;

	public SuperZoomPanTool() {
	}

	@Override
	public void activate(LayerViewPanel layerViewPanel) {
		super.activate(layerViewPanel);
		// during instanciation we do not have the LayerViewPanel, so we must add here the MouseWheelListener, but only once!
		if (!mouseWheelListenerAdded) {
			getWorkbench().getContext().getLayerViewPanel().addMouseWheelListener(this);
			mouseWheelListenerAdded = true;
		}
	}

	@Override
	public void deactivate() {
		super.deactivate();
		// remove the MouseWheelListener, because other tools do not need this MouseWheelListener ;-)
		if (mouseWheelListenerAdded) {
			LayerViewPanel layerViewPanel = getWorkbench().getContext().getLayerViewPanel();
			if (layerViewPanel != null) {
				layerViewPanel.removeMouseWheelListener(this);
				mouseWheelListenerAdded = false;
				// if the tool was deactivated, it's better to reset all parameters
				wheelMode = false;
				getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_ZOOM);
				scale = 1;
				mouseWheelCount = 0;
			}
		}
	}
		
	public void mouseWheelMoved(MouseWheelEvent e) {
		getWorkbench().getFrame().setStatusMessage(I18N.get("org.openjump.core.ui.plugin.view.SuperZoomPanTool.wheelmode-message"));
		int nclicks = e.getWheelRotation();  //negative is up/away
		mouseWheelCount = mouseWheelCount + nclicks;
		if (mouseWheelCount == 0) {
			scale = 1d;
		} else if (mouseWheelCount < 0) {
			scale = Math.abs(mouseWheelCount) * WHEEL_ZOOM_FACTOR;
		} else {
			scale = 1 / (mouseWheelCount * WHEEL_ZOOM_FACTOR);
		}
		wheelMode = true;
		// change the Cursor to reflect the new mode
		getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_WHEEL);
		getWorkbench().getFrame().setTimeMessage("1:" + (int) Math.floor(ScreenScale.getHorizontalMapScale(panel.getViewport()) / scale));
		try {
            redrawIndicator();
		} catch (Exception ex) {
			getPanel().getContext().handleThrowable(ex);
		}
	}

	@Override
	protected Shape getShape() throws Exception {
		// we need a Shape which represents the area of interest
		Dimension onScreenRectangleDimension = null;
		setColor(Color.magenta); // TODO und fill transparent;
		setStroke(new BasicStroke(1)); // TODO
		onScreenRectangleDimension = getPanel().getSize();
		onScreenRectangleDimension.setSize(onScreenRectangleDimension.getWidth() * 1 / scale, onScreenRectangleDimension.getHeight() * 1 / scale);
		if (wheelMode) {
			return new Rectangle((int) (mousePosition.getX() - onScreenRectangleDimension.getWidth() / 2), (int) (mousePosition.getY() - onScreenRectangleDimension.getHeight() / 2),
					(int) onScreenRectangleDimension.getWidth(), (int) onScreenRectangleDimension.getHeight());
		} else {
			return null;
		}
	}

    @Override
    protected Image getImage() {
        if (wheelMode) {
            // size of the area for text info such as scale etc.
            int textAreaWidth = 200;
            int textAreaHeight = 64;

            // offset of the textArea from the mouse position aka. the middle point of the zoomArea
            int textAreaOffsetX = 10;
            int textAreaOffsetY = 10;

            Dimension onScreenRectangleDimension = getPanel().getSize();
            onScreenRectangleDimension.setSize(onScreenRectangleDimension.getWidth() * 1 / scale, onScreenRectangleDimension.getHeight() * 1 / scale);
            // size of the new zoom level rectangle
            int zoomAreaWidth = (int) onScreenRectangleDimension.getWidth();
            int zoomAreaHeight = (int) onScreenRectangleDimension.getHeight();

            /* Compute the image size. This depends on the zoomArea size.
             * If the textArea plus textAreaOffset fits into the zoomArea,
             * the image size is eqal the zoomArea size. But if not, the
             * image size have to grow up.
             */
            int biWidth = zoomAreaWidth / 2 + textAreaOffsetX + textAreaWidth <= zoomAreaWidth ? zoomAreaWidth : zoomAreaWidth / 2 + textAreaOffsetX + textAreaWidth;
            int biHeight = zoomAreaHeight / 2 + textAreaOffsetY + textAreaHeight <= zoomAreaHeight ? zoomAreaHeight : zoomAreaHeight / 2 + textAreaOffsetY + textAreaHeight;

            // create the image
            BufferedImage image = new BufferedImage(biWidth, biHeight, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D imageG2d = image.createGraphics();
            // fill the whole zoomArea
            imageG2d.setColor(Color.yellow);
            imageG2d.fillRect(0, 0, zoomAreaWidth, zoomAreaHeight);
            // draw the enclosing zoom rectangle
            imageG2d.setColor(Color.red);
            imageG2d.drawRect(0, 0, zoomAreaWidth - 1, zoomAreaHeight - 1);
            imageG2d.drawRect(1, 1, zoomAreaWidth - 3, zoomAreaHeight - 3);

            // draw the textArea stuff
            // fill the whole textArea
            imageG2d.setColor(new Color(180,200,0));
            imageG2d.fillRect(zoomAreaWidth / 2 + textAreaOffsetX, zoomAreaHeight / 2 + textAreaOffsetY, textAreaWidth, textAreaHeight);
            // draw a border around the textArea
            imageG2d.setColor(Color.black);
            imageG2d.drawRect(zoomAreaWidth / 2 + textAreaOffsetX, zoomAreaHeight / 2 + textAreaOffsetY, textAreaWidth - 1, textAreaHeight -1);
            // draw the map scale
            imageG2d.setColor(Color.black);
            imageG2d.drawString(I18N.get("org.openjump.core.ui.plugin.view.ZoomToScalePlugIn.scale") + " 1:" + (int) Math.floor(ScreenScale.getHorizontalMapScale(panel.getViewport()) / scale), zoomAreaWidth / 2 + textAreaOffsetX + 5, zoomAreaHeight / 2 + textAreaOffsetY + 14);
            // draw the tool hint with a simple linebreaker, because drawString do not make this
            int hintX = zoomAreaWidth / 2 + textAreaOffsetX + 5;
            int hintOffsetX = 0;
            int hintY = zoomAreaHeight / 2 + textAreaOffsetY + 32;
            FontMetrics fm = imageG2d.getFontMetrics();
            String hintString = I18N.get("org.openjump.core.ui.plugin.view.SuperZoomPanTool.wheelmode-message");
            for (String word : hintString.split(" ")) {
                word += " ";
                if (hintX + hintOffsetX + fm.stringWidth(word)> hintX + textAreaWidth -2) {
                    // \n
                    hintY += 14;
                    // \r
                    hintOffsetX = 0;
                }
                imageG2d.drawString(word, hintX + hintOffsetX, hintY);
                hintOffsetX += fm.stringWidth(word);
            }

            // free resources
            imageG2d.dispose();

            // imagePosition is the center of the zoomArea rectangle
            imagePosition = new Point((int) mousePosition.getX() - zoomAreaWidth / 2, (int) mousePosition.getY() - zoomAreaHeight / 2);

            return image;
        } else {
            imagePosition = null;
            return null;
        }

    }

    @Override
    protected Point getImagePosition() {
        return imagePosition;
    }
    
    
    
    @Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		try {
			mousePosition = e.getPoint();
            redrawIndicator();
        } catch (Exception ex) {
			getPanel().getContext().handleThrowable(ex);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		if (wheelMode) {
			// left button performs the zoom
			if (SwingUtilities.isLeftMouseButton(e)) {
				try {
					getPanel().getViewport().zoomToViewPoint(new Point(e.getX(), e.getY()), scale);
				} catch (NoninvertibleTransformException ex) {
					getPanel().getContext().handleThrowable(ex);
				}
			}
			// for the right button we do not need some different code,
			// because it's cancel and the following redraw clears the Shape.
			// Clear the Shape is common for left and right click.
			wheelMode = false;
			scale = 1;
			mouseWheelCount = 0;
			try {
				// the following redrawShape() clears the Shape from screen,
				// because wheelMode is false.
                redrawIndicator();
			} catch (Exception ex) {
				getPanel().getContext().handleThrowable(ex);
			}

			getWorkbench().getFrame().setStatusMessage("");
			getWorkbench().getFrame().setTimeMessage("");
		} else {
			double zoomFactor = SwingUtilities.isRightMouseButton(e) ? (1 / ZOOM_IN_FACTOR) : ZOOM_IN_FACTOR;
            try {
				zoomAt(e.getPoint(), zoomFactor);
			} catch (Throwable t) {
				getPanel().getContext().handleThrowable(t);
			}
		}
		if (zoomPanClickTimer != null) {
			zoomPanClickTimer.stop();
			zoomPanClickTimer = null;
		}
		getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_ZOOM);

	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if (wheelMode) {
			getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
		} else {
			if (zoomPanClickTimer == null) {
				zoomPanClickTimer = new Timer(900, new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
					}
				});
				zoomPanClickTimer.setRepeats(false);
				zoomPanClickTimer.start();
			}
		}
	}
	
	@Override
	public boolean isRightMouseButtonUsed() {
		return true;
	}

	@Override
	public Cursor getCursor() {
		if (wheelMode) {
			return CURSOR_WHEEL;
		} else {
			return CURSOR_ZOOM;
		}
	}

	public Icon getIcon() {
		return IconLoader.icon("BigHandZoom.gif");
	}

	@Override
	public String getName() {
		return I18N.get("org.openjump.core.ui.plugin.view.SuperZoomPanTool.zoom-pan");
	}
	
	
	
	@Override
	public void mouseDragged(MouseEvent e) {
		try {
			if (zoomPanClickTimer != null) {
				zoomPanClickTimer.stop();
				zoomPanClickTimer = null;
			}
			getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_PAN);
			if (!dragging) {
				dragging = true;
				getPanel().getRenderingManager().setPaintingEnabled(false);
				cacheImage();
			}

			drawImage(e.getPoint());
			super.mouseDragged(e);
		} catch (Throwable t) {
			getPanel().getContext().handleThrowable(t);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!dragging) {
			return;
		}

		getPanel().getRenderingManager().setPaintingEnabled(true);
		dragging = false;
		super.mouseReleased(e);
		if (wheelMode) {
			getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_WHEEL);
			try {
				mousePosition = e.getPoint();
                redrawIndicator();
			} catch (Exception ex) {
				getPanel().getContext().handleThrowable(ex);
			}
		} else {
			if (zoomPanClickTimer != null) {
				zoomPanClickTimer.stop();
				zoomPanClickTimer = null;
			}
			getWorkbench().getContext().getLayerViewPanel().setCursor(CURSOR_ZOOM);
		}
	}

	@Override
	protected Shape getShape(Point2D source, Point2D destination) {
		return null;
	}

	protected void gestureFinished() throws NoninvertibleTransformException {
		reportNothingToUndoYet();

		double xDisplacement = getModelDestination().x - getModelSource().x;
		double yDisplacement = getModelDestination().y - getModelSource().y;
		Envelope oldEnvelope = getPanel().getViewport().getEnvelopeInModelCoordinates();
		getPanel().getViewport().zoom(new Envelope(oldEnvelope.getMinX()
				- xDisplacement, oldEnvelope.getMaxX() - xDisplacement,
				oldEnvelope.getMinY() - yDisplacement,
				oldEnvelope.getMaxY() - yDisplacement));
	}
	
	/**
	 * Creates a new Image if currImage doesn't exist
	 * or is the wrong size for the panel.
	 * @param currImage an image buffer
	 * @return a new image, or the existing one if it's compatible
	 */
	public Image createImageIfNeeded(Image currImage)
	{
		if (currImage == null
				|| currImage.getHeight(null) != getPanel().getHeight()
				|| currImage.getWidth(null) != getPanel().getWidth()) {
			Graphics2D g = (Graphics2D) getPanel().getGraphics();
			Image img = g.getDeviceConfiguration().createCompatibleImage(
					getPanel().getWidth(), getPanel().getHeight(), Transparency.OPAQUE);
			return img;

		}
		return currImage;
	}

	public void cacheImage() {
		origImage = createImageIfNeeded(origImage);
		getPanel().paint(origImage.getGraphics());		    
	}

	private void drawImage(Point p) throws NoninvertibleTransformException {
		double dx = p.getX() - getViewSource().getX();
		double dy = p.getY() - getViewSource().getY();

		auxImage = createImageIfNeeded(auxImage);
		auxImage.getGraphics().setColor(Color.WHITE);
		auxImage.getGraphics().fillRect(0, 0, auxImage.getWidth(getPanel()), auxImage.getHeight(getPanel()));
		auxImage.getGraphics().drawImage(origImage, (int) dx, (int) dy, getPanel());
		getPanel().getGraphics().drawImage(auxImage, 0, 0, getPanel());
	}

	public boolean setAnimatingZoom(boolean animating) {
    	boolean previousValue = isAnimatingZoom;
    	isAnimatingZoom = animating;
    	return previousValue;
    }
    
    public boolean getAnimatingZoom(){
    	return isAnimatingZoom;
    }


	private void zoomAt(Point2D p, double zoomFactor)
        throws NoninvertibleTransformException {                                
        //getPanel().getViewport().zoomToViewPoint(p, zoomFactor);
    	zoomAt(p,zoomFactor,getAnimatingZoom());
    }

	protected void zoomAt(Point2D p, double zoomFactor, boolean animatingZoom)
    throws NoninvertibleTransformException { //zoom while keeping cursor over same model point                         
		Viewport vp = getPanel().getViewport();
		Point2D zoomPoint = vp.toModelPoint(p);
		Envelope modelEnvelope = vp.getEnvelopeInModelCoordinates();           
		Coordinate centre = modelEnvelope.centre();
		double width = modelEnvelope.getWidth();
		double height = modelEnvelope.getHeight();
		double dx = (zoomPoint.getX() - centre.x) / zoomFactor;
		double dy = (zoomPoint.getY() - centre.y) / zoomFactor;
		Envelope zoomModelEnvelope = new Envelope(  
				zoomPoint.getX() - (0.5 * (width / zoomFactor)) - dx, 
				zoomPoint.getX() + (0.5 * (width / zoomFactor)) - dx,
				zoomPoint.getY() - (0.5 * (height / zoomFactor)) - dy,
				zoomPoint.getY() + (0.5 * (height / zoomFactor)) - dy);
    	vp.zoom(zoomModelEnvelope);   		
    }

    /**
     * Redraws the visual indicator. This can be a Shape or an Image. It depends
     * of the indicatorMode value.
     */
    public void redrawIndicator() {
        try {
            switch (getIndicatorMode()) {
                case INDICATOR_MODE_SHAPE:
                    redrawShape();
                    break;
                case INDICATOR_MODE_IMAGE:
                    redrawImage();
                    break;
                default:
                    Logger.warn("Unknown indicatorMode " + getIndicatorMode() + "!");
            }
        } catch (Exception e) {
            Logger.error("Unable to redraw the visual indicator!", e);
        }
    }

    /**
     * @return the indicatorMode
     */
    public int getIndicatorMode() {
        return indicatorMode;
    }

    /**
     * Sets the indicatorMode. Valid values are {@link #INDICATOR_MODE_SHAPE},
     * {@link #INDICATOR_MODE_IMAGE}.
     * @param indicatorMode the indicatorMode to set
     */
    public void setIndicatorMode(int indicatorMode) {
        if (indicatorMode == INDICATOR_MODE_SHAPE || indicatorMode == INDICATOR_MODE_IMAGE) {
            this.indicatorMode = indicatorMode;
        }
    }
}
