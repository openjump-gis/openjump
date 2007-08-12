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

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelListener;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.snap.SnapManager;
import com.vividsolutions.jump.workbench.ui.snap.SnapPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToFeaturesPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToGridPolicy;
import com.vividsolutions.jump.workbench.ui.snap.SnapToVerticesPolicy;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * A tool that draws an XOR visual indicator. Subclasses need not keep track of
 * the XOR state of the indicator -- that logic is all handled by this class.
 * Even if the LayerViewPanel is repainted while the XOR indicator is on-screen.
 */
public abstract class AbstractCursorTool implements CursorTool {
	private static Logger LOG = Logger.getLogger(AbstractCursorTool.class);
	
	private boolean snappingConfigured = false;

	private boolean configuringSnapping = false;

	private boolean controlPressed;

	private boolean shiftPressed;

	private Color color = Color.red;

	private boolean filling = false;

	private Shape lastShapeDrawn;

	private LayerViewPanelListener layerViewPanelListener = new LayerViewPanelListener() {

		public void cursorPositionChanged(String x, String y) {
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

	private LayerViewPanel panel;

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
		configuringSnapping = true;
	}

	protected boolean wasShiftPressed() {
		return shiftPressed;
	}

	protected boolean wasControlPressed() {
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

	public void activate(LayerViewPanel layerViewPanel) {
		if (workbenchFrame(layerViewPanel) != null) {
			workbenchFrame(layerViewPanel).log(
					I18N.get("ui.cursortool.AbstractCursorTool.activating")+" " + getName());
		}

		if (this.panel != null) {
			this.panel.removeListener(layerViewPanelListener);
		}

		this.panel = layerViewPanel;
		this.panel.addListener(layerViewPanelListener);

		if (configuringSnapping && !snappingConfigured) {
            //Must wait until now because #getWorkbench needs the panel to be set. [Jon Aquino]
            //getSnapManager().addPolicies(
            //createStandardSnappingPolicies(getWorkbench().getBlackboard()));
            
            //fix bug 1713295 - change blackboard to PersistentBlackboard.
			//Snap options have been broken since PersistentBlackboard has
			//replaced blackboard in InstallGridPlugIn [Michael Michaud 2007-05-12]
            getSnapManager().addPolicies(createStandardSnappingPolicies(
                PersistentBlackboardPlugIn.get(getWorkbench().getContext())));
			snappingConfigured = true;
		}
	}

	public static WorkbenchFrame workbenchFrame(LayerViewPanel layerViewPanel) {
		Window window = SwingUtilities.windowForComponent(layerViewPanel);

		//Will not be a WorkbenchFrame in apps that don't use the workbench
		//e.g. LayerViewPanelDemoFrame. [Jon Aquino]
		return (window instanceof WorkbenchFrame)
				? (WorkbenchFrame) window
				: null;
	}

	public static List createStandardSnappingPolicies(Blackboard blackboard) {
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
		cancelGesture();
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
		controlPressed = e.isControlDown();
		shiftPressed = e.isShiftDown();
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

	protected LayerViewPanel getPanel() {
		return panel;
	}

	/**
	 * @return null if nothing should be drawn
	 */
	protected abstract Shape getShape() throws Exception;

	protected void cleanup(Graphics2D graphics) {
		graphics.setPaintMode();
		graphics.setColor(originalColor);
		graphics.setStroke(originalStroke);
	}

	protected void clearShape() {
		clearShape(getGraphics2D());
	}

	private Graphics2D getGraphics2D() {
		Graphics2D g = (Graphics2D) panel.getGraphics();

		if (g != null) {
			//Not sure why g is null sometimes [Jon Aquino]
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		}

		return g;
	}

	public void cancelGesture() {
		clearShape();
	}

	protected void drawShapeXOR(Graphics2D g) throws Exception {
		Shape newShape = getShape();
		drawShapeXOR(newShape, g);
		lastShapeDrawn = newShape;
	}

	protected void drawShapeXOR(Shape shape, Graphics2D graphics) {
		setup(graphics);

		try {
			//Pan tool returns a null shape. [Jon Aquino]
			if (shape != null) {
				//Can't both draw and fill, because we're using XOR. [Jon
				// Aquino]
				if (filling) {
					graphics.fill(shape);
				} else {
					graphics.draw(shape);
				}
			}
		} finally {
			cleanup(graphics);
		}
	}

	protected void redrawShape() throws Exception {
		redrawShape(getGraphics2D());
	}

	protected Coordinate snap(Point2D viewPoint)
			throws NoninvertibleTransformException {
		return snap(getPanel().getViewport().toModelCoordinate(viewPoint));
	}

	protected Coordinate snap(Coordinate modelCoordinate) {
		return snapManager.snap(getPanel(), modelCoordinate);
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

	private void redrawShape(Graphics2D graphics) throws Exception {
		clearShape(graphics);
		drawShapeXOR(graphics);

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

	public static JUMPWorkbench workbench(LayerViewPanel panel) {
		return ((WorkbenchFrame) SwingUtilities.getAncestorOfClass(
				WorkbenchFrame.class, panel)).getContext().getWorkbench();
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
		return getName();
	}

	public String getName() {
		return name(this);
	}

	public static String name(CursorTool tool) {
		try {
	        return I18N.get(tool.getClass().getName());
	    } catch(java.util.MissingResourceException e){
	    	// No I18N for the PlugIn so log it, but don't stop
	    	LOG.error(e.getMessage()+" "+tool.getClass().getName());
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
}