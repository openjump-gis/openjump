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
package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelListener;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.cursortool.editing.EditingPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.snap.SnapManager;
import com.vividsolutions.jump.workbench.ui.snap.SnapPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToFeaturesPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToGridPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;

/**
 * A tool that draws an XOR visual indicator. Subclasses need not keep track of
 * the XOR state of the indicator -- that logic is all handled by this class.
 * Even if the LayerViewPanel is repainted while the XOR indicator is on-screen.
 * You can use a java.awt.Shape for a simple visual indicator such as a Rectangle
 * or Ellipse for example. For more complex indicators you can use a
 * java.awt.Image. Here you can build complex geometric stuff with
 * filling, transparency and so on. Real photos are possible too.
 * For the use of an image as a visual indicator you have to override the
 * {@link #getImage()}, {@link #getImagePosition()} methods for generating the
 * image to be draw. Instead to call {@link #redrawShape()}, you have to use the
 * {@link #redrawImage()} method.
 */
public abstract class AbstractCursorTool implements CursorTool {

	
	private boolean snappingInitialized = false;

	private boolean snappingAllowed = false;
	private boolean controlPressed = false;
	private boolean shiftPressed = false;

	private Color color = Color.red;

	private boolean filling = false;

	private Shape lastShapeDrawn;
    
    private Image lastImageDrawn = null;
    
    private Point lastMousePosition = null; 
	
	// special check for linux because of the painting bug in the JVM
	protected boolean isLinuxOS = System.getProperty("os.name").toLowerCase().startsWith("linux");

	private LayerViewPanelListener layerViewPanelListener = new LayerViewPanelListener() {

		public void cursorPositionChanged(String x, String y) {
			// show scale view when cursos moves on view      //
			// [Giuseppe Aruta 2012-feb-18] //
			// [Michaël Michaud 2013-03-13] move to workbenchFrame.changeZoom() 
			// getWorkbench().getFrame().setScaleText("1:" + (int) Math.floor(ScreenScale.getHorizontalMapScale(panel.getViewport())));
		}

		public void selectionChanged() {
		}

		public void fenceChanged() {
		}

		public void painted(Graphics graphics) {
			try {
				//If panel is repainted, force a redraw of the shape. Examples
				// of when the
				//panel is repainted: (1) the user Alt-Tabs away from the app
				//(2) the user fires an APPEARANCE_CHANGED LayerEvent. [Jon
				// Aquino]
				if (shapeOnScreen) {
					setShapeOnScreen(false);
					redrawShape((Graphics2D) graphics);
				}
			} catch (Throwable t) {
				panel.getContext().handleThrowable(t);
			}
		}
	};

	private Color originalColor;

	private Stroke originalStroke;

	protected LayerViewPanel panel = null;

	private boolean shapeOnScreen = false;

	private SnapManager snapManager = new SnapManager();

	private Stroke stroke = new BasicStroke(1);

	private ArrayList listeners = new ArrayList();

	private Cursor cursor;

	public AbstractCursorTool() {
	}

	/**
	 * Makes this CursorTool obey the snapping settings in the Options dialog.
	 */
  public void allowSnapping() {
    snappingAllowed = true;
  }

  public void prohibitSnapping() {
    snappingAllowed = false;
  }

  public boolean supportsSnapping() {
    return snappingAllowed;
  }

  protected void setShiftPressed(boolean onoff){
    shiftPressed = onoff;
  }
  
  protected boolean wasShiftPressed() {
    // System.out.println("act shift pressed");
    return shiftPressed;
  }

  protected void setControlPressed(boolean onoff) {
    // System.out.println("set ctrl "+onoff+" -> "+this);
    controlPressed = onoff;
  }

  protected boolean wasControlPressed() {
    // System.out.println("get ctrl "+controlPressed+" -> "+this);
    return controlPressed;
  }

	/**
	 * The cursor will look best if the image is a 32 x 32 transparent GIF.
	 */
	public static Cursor createCursor(Image image) {
		//<<TODO>> Compute image center rather than hardcoding 16, 16. [Jon
		// Aquino]
		return createCursor(image, new Point(16, 16));
	}

	public static Cursor createCursor(Image image, Point hotSpot) {
		return GUIUtil.createCursor(image, hotSpot);
	}

	public Cursor getCursor() {
		if (cursor == null) {
			cursor = getIcon() instanceof ImageIcon
					? GUIUtil.createCursorFromIcon(((ImageIcon) getIcon())
							.getImage())
					: Cursor.getDefaultCursor();
		}
		return cursor;
	}

	/**
	 * Used by OrCompositeTool to determine whether a CursorTool is busy
	 * interacting with the user.
	 */
	public boolean isGestureInProgress() {
		//For most CursorTools, the presence of the shape on the screen
		// indicates
		//that the user is making a gesture. An exception, however, is
		//SnapIndicatorTool -- it provides its own implementation of this
		// method.
		//[Jon Aquino]
		return isShapeOnScreen();
	}

	public boolean isRightMouseButtonUsed() {
		return false;
	}

	/**
	 * Important for XOR drawing. Even if #getShape returns null, this method
	 * will return true between calls of #redrawShape and #clearShape.
	 */
	public boolean isShapeOnScreen() {
		return shapeOnScreen;
	}

  public void activate(LayerViewPanel new_panel) {
    if (workbenchFrame(new_panel) != null) {
      workbenchFrame(new_panel).log(
          I18N.get("ui.cursortool.AbstractCursorTool.activating") + " "
              + getName());
    }

    LayerViewPanel old_panel = getPanel();
    // cancel ongoing possibly gestures if we switch LayerViews (switch Tasks)
    if ((old_panel != null) && !(old_panel.equals(new_panel))) {
      cancelGesture();
    }

    this.panel = new_panel;
    this.panel.addListener(layerViewPanelListener);

    if (snappingAllowed && !snappingInitialized) {
      getSnapManager().addPolicies(
          createStandardSnappingPolicies(PersistentBlackboardPlugIn
              .get(getWorkbench().getContext())));
      snappingInitialized = true;
    }
    
    // following added to handle KEY shortcuts e.g. SPACEBAR snap switching
    WorkbenchFrame frame = this.panel.getWorkBenchFrame();
    frame.addEasyKeyListener(keyListener);
  }

	public static WorkbenchFrame workbenchFrame(LayerViewPanel layerViewPanel) {
		Window window = SwingUtilities.windowForComponent(layerViewPanel);

		//Will not be a WorkbenchFrame in apps that don't use the workbench
		//e.g. LayerViewPanelDemoFrame. [Jon Aquino]
		return (window instanceof WorkbenchFrame)
				? (WorkbenchFrame) window
				: null;
	}

	protected List createStandardSnappingPolicies(Blackboard blackboard) {
		return Arrays.asList(new SnapPolicy[]{
				new SnapToVerticesPolicy(blackboard),
				new SnapToFeaturesPolicy(blackboard),
				new SnapToGridPolicy(blackboard)});
	}

	protected boolean isRollingBackInvalidEdits() {
		return getWorkbench().getBlackboard().get(
				EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false);
	}

  public void deactivate() {
    // gestures are cancelled explicitly only when layerview changed
    //cancelGesture();

    // following added to handle SPACEBAR snap switching
    getWorkbenchFrame().removeEasyKeyListener(keyListener);
  }

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void setColor(Color color) {
		this.color = color;
	}

	protected void setFilling(boolean filling) {
		this.filling = filling;
	}

	/**
	 * @deprecated Use #setStroke instead.
	 */
	protected void setStrokeWidth(int strokeWidth) {
		setStroke(new BasicStroke(strokeWidth));
	}

	protected void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	protected void setup(Graphics2D graphics) {
		originalColor = graphics.getColor();
		originalStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setXORMode(Color.white);
		graphics.setStroke(stroke);
	}

	/**
	 * @return null if nothing should be drawn
	 */
	protected abstract Shape getShape() throws Exception;
    
    /**
     * This method have to return an Image as a visual indicator.
     * Usually this method should to be abstract. But if we do this here, many
     * other derived classes have to implement this method. Thereby we would
     * break the compatibility for many other classes in OJ and 3rd party
     * plugins.
     * 
     * @return a Image or null if nothing should be drawn.
     */
    protected Image getImage() {
        return null;
    }
    
    /**
     * This method have to return the position for the Image. The position is
     * the top left corner of the image for the Graphics2D.drawImage() method.
     * This method is called after the {@link #getImage()} method.
     * For the abstract problematic please see {@link #getImage()}.
     * 
     * @return the position for the Image
     */
    protected Point getImagePosition() {
        return null;
    }

	protected void cleanup(Graphics2D graphics) {
		graphics.setPaintMode();
		graphics.setColor(originalColor);
		graphics.setStroke(originalStroke);
	}

  protected void clearShape() {
    Graphics2D g;
    if (panel != null && (g=getGraphics2D())!=null)
      clearShape(g);
  }

  /**
   * Clears an previously painted image from screen.
   */
  protected void clearImage() {
    Graphics2D g;
    if (panel != null && (g=getGraphics2D())!=null)
      clearImage(g);
  }

	private Graphics2D getGraphics2D() {
		Graphics2D g = (Graphics2D) panel.getGraphics();

		if (g != null) {
			//Not sure why g is null sometimes [Jon Aquino]
			
			// Workaround for the Linux X11 rendering bug with buggy screenrefresh
			// on all other platforms there are no problems, so we don't use
			// antialaising for drawing the shape under Linux [Matthias Scholz 19. Jan 2012]
			if (!isLinuxOS) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			}
		}

		return g;
	}

	public void cancelGesture() {
		clearShape();
        clearImage();
	}

	protected void drawShapeXOR(Graphics2D g) throws Exception {
		Shape newShape = getShape();
		drawShapeXOR(newShape, g);
		lastShapeDrawn = newShape;
	}

    /**
     * Draw the image in XOR mode at the specified position on screen.
     * The position and the image is remembered for a later clear.
     * 
     * @param g
     * @throws Exception 
     */
    protected void drawImageXOR(Graphics2D g) throws Exception {
        Image newImage = getImage();
        Point newPosition = getImagePosition();
		drawImageXOR(newImage, newPosition, g);
        lastImageDrawn = newImage;
        lastMousePosition = newPosition;
	}

  protected void drawShapeXOR(Shape shape, Graphics2D graphics) {
    setup(graphics);

    try {
      // Pan tool returns a null shape. [Jon Aquino]
      if (shape != null) {
        // Can't both draw and fill, because we're using XOR. [Jon
        // Aquino]
        if (filling) {
          graphics.fill(shape);
        } else {
          graphics.draw(shape);
        }
          }
      }
    // easy workaround for 
    //  java.lang.InternalError: Unable to Stroke shape (attempt to 
    //  validate Pipe with invalid SurfaceData)
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7153339
    catch (java.lang.InternalError ie){
      ie.printStackTrace(System.err);
      Logger.error(ie.getLocalizedMessage() +": "+Arrays.toString(ie.getStackTrace()));
    }
    finally {
      cleanup(graphics);
    }
  }
  
  /**
   * Draw an image in XOR mode on screen.
   * 
   * @param image the image to be draw
   * @param position the position
   * @param graphics the Graphics2D
   */
  protected void drawImageXOR(Image image, Point position, Graphics2D graphics) {
    if (image != null && position != null) {
        setup(graphics);
        graphics.drawImage(image, (int) position.getX(), (int) position.getY(), null);
    }
  }

	protected void redrawShape() throws Exception {
		redrawShape(getGraphics2D());
	}

    /**
     * Redraws the image on screen.
     * 
     * @throws Exception 
     */
	protected void redrawImage() throws Exception {
		redrawImage(getGraphics2D());
	}

	protected Coordinate snap(Point2D viewPoint)
			throws NoninvertibleTransformException {
		return snap(getPanel().getViewport().toModelCoordinate(viewPoint));
	}

	protected Coordinate snap(Coordinate modelCoordinate) {
		return snappingAllowed ? snapManager.snap(getPanel(), modelCoordinate) : modelCoordinate;
	}

	private void setShapeOnScreen(boolean shapeOnScreen) {
		this.shapeOnScreen = shapeOnScreen;
	}

	private void clearShape(Graphics2D graphics) {
		if (!shapeOnScreen) {
			return;
		}

        drawShapeXOR(lastShapeDrawn, graphics);
		setShapeOnScreen(false);
	}

    /**
     * Clears an previously painted image from screen.
     * 
     * @param graphics 
     */
	private void clearImage(Graphics2D graphics) {
		if (!shapeOnScreen) {
			return;
		}

        drawImageXOR(lastImageDrawn, lastMousePosition, graphics);
        setShapeOnScreen(false);
	}

	private void redrawShape(Graphics2D graphics) throws Exception {
		clearShape(graphics);
		drawShapeXOR(graphics);

		//<<TODO:INVESTIGATE>> Race conditions on the shapeOnScreen field?
		//Might we need synchronization? [Jon Aquino]
		setShapeOnScreen(true);
	}

    /**
     * Redraws the image on screen. This means the clearing the old image and
     * draw the actual image.
     * 
     * @param graphics the Graphics2D
     * @throws Exception 
     */
    private void redrawImage(Graphics2D graphics) throws Exception {
		clearImage(graphics);
		drawImageXOR(graphics);

		//<<TODO:INVESTIGATE>> Race conditions on the shapeOnScreen field?
		//Might we need synchronization? [Jon Aquino]
		setShapeOnScreen(true);
	}

	/**
	 * @return null if the LayerViewPanel is not inside a TaskFrame
	 */
	protected TaskFrame getTaskFrame() {
		return (TaskFrame) SwingUtilities.getAncestorOfClass(TaskFrame.class,
				getPanel());
	}

	public JUMPWorkbench getWorkbench() {
		return workbench(getPanel());
	}

  public WorkbenchFrame getWorkbenchFrame() {
    return workbench(getPanel()).getFrame();
  }

	public static JUMPWorkbench workbench(LayerViewPanel panel) {
		return JUMPWorkbench.getInstance();
	}

	protected abstract void gestureFinished() throws Exception;

	protected void fireGestureFinished() throws Exception {
		getPanel().getContext().setStatusMessage("");

		if (getTaskFrame() != null) {
			// Log if a WorkbenchFrame is available. [Sheldon Young 2004-06-03]
			WorkbenchFrame workbenchFrame = (WorkbenchFrame) SwingUtilities
					.getAncestorOfClass(WorkbenchFrame.class, getTaskFrame());
			if (workbenchFrame != null) {
				workbenchFrame.log(I18N.get("ui.cursortool.AbstractCursorTool.gesture-finished")+": " + getName());
			}
		}

		getPanel().getLayerManager().getUndoableEditReceiver().startReceiving();

		try {
			gestureFinished();
		} finally {
			getPanel().getLayerManager().getUndoableEditReceiver()
					.stopReceiving();
		}

		for (Iterator i = listeners.iterator(); i.hasNext();) {
			Listener listener = (Listener) i.next();
			listener.gestureFinished();
		}
	}

	public void add(Listener listener) {
		listeners.add(listener);
	}

	/**
	 * Optional means of execution, with undoability.
	 */
	protected void execute(UndoableCommand command) {
		AbstractPlugIn.execute(command, getPanel());
	}

	/**
	 * Notifies the UndoManager that this PlugIn did not modify any model
	 * states, and therefore the undo history should remain unchanged. Call this
	 * method inside #execute(PlugInContext).
	 */
	protected void reportNothingToUndoYet() {
		getPanel().getLayerManager().getUndoableEditReceiver()
				.reportNothingToUndoYet();
	}

	public String toString() {
		return name(this)+"@"+hashCode();
	}

	public String getName() {
		return name(this);
	}

  public static final LayerViewPanel getPanel(CursorTool ct) {
    if (ct instanceof AbstractCursorTool)
      return ((AbstractCursorTool)ct).getPanel();
    return null;
  }
	
  public final LayerViewPanel getPanel() {
    return this.panel;
  }

  protected void setPanel(LayerViewPanel panel) {
    this.panel = panel;
  }

  public static String name(CursorTool tool) {
    try {
      String key = tool.getClass().getName();
      Class c;
      // use superclass name if tool was modified as anonymous inner class in any way
      while (key.contains("$") && (c = tool.getClass().getSuperclass())!=null) {
        key = c.getName();
       }
      return I18N.get(key);
    } catch (java.util.MissingResourceException e) {
      // No I18N for the PlugIn so log it, but don't stop
      Logger.error(e.getMessage() + " " + tool.getClass().getName());
      return StringUtil.toFriendlyName(tool.getClass().getName(),
          I18N.get("ui.cursortool.AbstractCursorTool.tool"));
    }
  }

	protected boolean check(EnableCheck check) {
		String warning = check.check(null);

		if (warning != null) {
			getPanel().getContext().warnUser(warning);

			return false;
		}

		return true;
	}

	public SnapManager getSnapManager() {
		return snapManager;
	}

	public Color getColor() {
		return color;
	}

	public static interface Listener {

		public void gestureFinished();
	}

  // snap on/off via key listener
  private KeyListener keyListener = new KeyListener() {
    boolean off = false;

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      if(!componentWithFocusIsHandledByCursorTools())
        return;
      
      //System.out.println(e);
      if (snappingInitialized && isSpace(e) && !off) {
        off = true;
        prohibitSnapping();
        // System.out.println("snap off");
        showMsg("com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool.snapping-off");
      }
      saveModifiers(e);
    }

    public void keyReleased(KeyEvent e) {
      if(!componentWithFocusIsHandledByCursorTools())
        return;
      
      //System.out.println(e);
      if (snappingInitialized && isSpace(e) && off) {
        off = false;
        allowSnapping();
        // System.out.println("snap on");
        showMsg("com.vividsolutions.jump.workbench.ui.cursortool.AbstractCursorTool.snapping-on");
      }
      saveModifiers(e);
    }

    private void saveModifiers(KeyEvent e){
      setShiftPressed( e.isShiftDown() );
      setControlPressed( e.isControlDown() );
//      System.out.println("act "+wasShiftPressed()+"/"+wasControlPressed());
    }

    private void showMsg(String msg) {
      getPanel().getWorkBenchFrame().setStatusMessage(I18N.get(msg),5000);
    }

    private boolean isSpace(KeyEvent e) {
      return (e.getKeyCode() == KeyEvent.VK_SPACE);
    }
  };

  /**
   * utility method to be used by cursor tools to determine if the
   * ui component with focus falls into it's purview 
   * 
   * @return boolean
   */
  public static boolean componentWithFocusIsHandledByCursorTools(){
    // only react if LayerView, one of it's subcomponents
    // or the EditToolBox has got the focus
    Component c = KeyboardFocusManager
            .getCurrentKeyboardFocusManager().getFocusOwner();
    // traverse through parents, see if we are in a valid one
    boolean valid = false;
    while (c != null) {
      if (c instanceof LayerViewPanel
          || (c instanceof ToolboxDialog && c.equals(EditingPlugIn
              .getInstance().getToolbox()))) {
        valid = true;
        break;
      }

      c = c.getParent();
    }

    return valid;
  }
}