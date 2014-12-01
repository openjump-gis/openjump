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
package com.vividsolutions.jump.workbench.ui;

import java.awt.*;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.ui.cursortool.CursorTool;
import com.vividsolutions.jump.workbench.ui.cursortool.DummyTool;
import com.vividsolutions.jump.workbench.ui.cursortool.LeftClickFilter;
import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;
import com.vividsolutions.jump.workbench.ui.renderer.java2D.Java2DConverter;
import com.vividsolutions.jump.workbench.ui.renderer.style.PinEqualCoordinatesStyle;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;
import com.vividsolutions.jump.workbench.ui.cursortool.QuasimodeTool;

//<<TODO:FIX>> One user (GK) gets an infinite repaint loop (the map moves around
//chaotically) when the LayerViewPanel is put side by side with the LayerTreePanel
//in a GridBagLayout. Something to do with determining the size, I think --
//the problem doesn't occur when the size is well defined (as when the two
//panels are in a GridLayout or SplitPane). [Jon Aquino]

/**
 * Be sure to call #dispose() when the LayerViewPanel is no longer needed.
 */
public class LayerViewPanel extends JPanel
		implements
			LayerListener,
			LayerManagerProxy,
			SelectionManagerProxy {
	private static JPopupMenu popupMenu = new TrackedPopupMenu();
	private ToolTipWriter toolTipWriter = new ToolTipWriter(this);
	BorderLayout borderLayout1 = new BorderLayout();
	private LayerManager layerManager;
	private CursorTool currentCursorTool = new DummyTool();
	private Viewport viewport = new Viewport(this);
	private boolean viewportInitialized = false;
	private java.awt.Point lastClickedPoint;
	private ArrayList listeners = new ArrayList();
	private LayerViewPanelContext context;
	private RenderingManager renderingManager = new RenderingManager(this);
	private FenceLayerFinder fenceLayerFinder;
	private SelectionManager selectionManager;
	private Blackboard blackboard = new Blackboard();
	private boolean deferLayerEvents = false;
	
	class MouseWheelZoomListener implements MouseWheelListener {
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (currentCursorTool instanceof QuasimodeTool) {
				Object tool = ((QuasimodeTool) currentCursorTool).getDelegate();
				if (tool instanceof ZoomTool)  {
					((ZoomTool) tool).mouseWheelMoved(e);
				} else if  (tool instanceof LeftClickFilter) {
					CursorTool wrappee = ((LeftClickFilter) tool).getWrappee();
					if (wrappee instanceof PanTool)
						((PanTool) wrappee).mouseWheelMoved(e);					
				}
			}
		}
	}
	
	public LayerViewPanel(LayerManager layerManager,
			LayerViewPanelContext context) {
		//Errors occur if the LayerViewPanel is sized to 0. [Jon Aquino]
		setMinimumSize(new Dimension(100, 100));

		//Set toolTipText to null to disable, "" to use default (i.e. show all
		// attributes),
		//or a custom template. [Jon Aquino]
		setToolTipText("");
		GUIUtil.fixClicks(this);

		try {
			this.context = context;
			this.layerManager = layerManager;
			selectionManager = new SelectionManager(this, this);
			fenceLayerFinder = new FenceLayerFinder(this);

			//Immediately register with the LayerManager because
			// #getLayerManager will
			//be called right away (when #setBackground is called in #jbInit)
			// [Jon Aquino]
			layerManager.addLayerListener(this);

			try {
				jbInit();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					//Re-activate WorkbenchFrame. Otherwise, user may try
					// entering
					//a quasi-mode by pressing a modifier key -- nothing will
					// happen because the
					//WorkbenchFrame does not have focus. [Jon Aquino]
					//JavaDoc for #toFront says some platforms will not
					// activate the window.
					//So use #requestFocus instead. [Jon Aquino 12/9/2003]
					WorkbenchFrame workbenchFrame = (WorkbenchFrame) SwingUtilities
							.getAncestorOfClass(WorkbenchFrame.class,
									LayerViewPanel.this);
					if (workbenchFrame != null && !workbenchFrame.isActive()) {
						workbenchFrame.requestFocus();
					}
				}
			});

			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) {
					mouseLocationChanged(e);
				}

				public void mouseMoved(MouseEvent e) {
					mouseLocationChanged(e);
				}

				private void mouseLocationChanged(MouseEvent e) {
					try {
						Point2D p = getViewport().toModelPoint(e.getPoint());
						fireCursorPositionChanged(format(p.getX()), format(p
								.getY()));
					} catch (Throwable t) {
						LayerViewPanel.this.context.handleThrowable(t);
					}
				}
			});
			
			addMouseWheelListener(new MouseWheelZoomListener() );

		} catch (Throwable t) {
			context.handleThrowable(t);
		}
	}

	public ToolTipWriter getToolTipWriter() {
		return toolTipWriter;
	}

	//In Java 1.3, if you try and do a #mouseClicked or a #mouseDragged on an
	//inactive internal frame, it won't work. [Jon Aquino]
	//In Java 1.4, the #mouseDragged will work, but not the #mouseClicked.
	//See the Sun Java Bug Database, ID 4398733. The evaluation for Bug ID
	// 4256525
	//states that the fix is scheduled for the Java release codenamed Tiger.
	//[Jon Aquino]
	public String getToolTipText(MouseEvent event) {
		return toolTipWriter.write(getToolTipText(), event.getPoint());
	}

	public static List components(Geometry g) {
		if (!(g instanceof GeometryCollection)) {
			return Arrays.asList(new Object[]{g});
		}

		GeometryCollection c = (GeometryCollection) g;
		ArrayList components = new ArrayList();

		for (int i = 0; i < c.getNumGeometries(); i++) {
			components.addAll(components(c.getGeometryN(i)));
		}

		return components;
	}

	/**
	 * Workaround for the fact that GeometryCollection#intersects is not
	 * currently implemented.
	 */
	public static boolean intersects(Geometry a, Geometry b) {
		GeometryFactory factory = new GeometryFactory(a.getPrecisionModel(), a
				.getSRID());
		List aComponents = components(a);
		List bComponents = components(b);

		for (Iterator i = aComponents.iterator(); i.hasNext();) {
			Geometry aComponent = (Geometry) i.next();
			Assert.isTrue(!(aComponent instanceof GeometryCollection));

			//Collapse to point as workaround for JTS defect: #contains doesn't
			// work for
			//polygons and zero-length vectors. [Jon Aquino]
			aComponent = collapseToPointIfPossible(aComponent, factory);

			for (Iterator j = bComponents.iterator(); j.hasNext();) {
				Geometry bComponent = (Geometry) j.next();
				Assert.isTrue(!(bComponent instanceof GeometryCollection));
				bComponent = collapseToPointIfPossible(bComponent, factory);

				if (aComponent.intersects(bComponent)) {
					return true;
				}
			}
		}

		return false;
	}

	private static Geometry collapseToPointIfPossible(Geometry g,
			GeometryFactory factory) {
		if (!g.isEmpty() && PinEqualCoordinatesStyle.coordinatesEqual(g)) {
			g = factory.createPoint(g.getCoordinate());
		}

		return g;
	}

	/**
	 * The Fence layer will be excluded.
	 */
	public Map visibleLayerToFeaturesInFenceMap() {
		Map visibleLayerToFeaturesInFenceMap = visibleLayerToFeaturesInFenceMap(getFence());
		visibleLayerToFeaturesInFenceMap.remove(new FenceLayerFinder(this)
				.getLayer());

		return visibleLayerToFeaturesInFenceMap;
	}

	/**
	 * The Fence layer will be included.
	 */
	public Map visibleLayerToFeaturesInFenceMap(Geometry fence) {
		Map map = new HashMap();

		for (Iterator i = getLayerManager().iterator(); i.hasNext();) {
			Layer layer = (Layer) i.next();

			if (!layer.isVisible()) {
				continue;
			}

			HashSet features = new HashSet();

			for (Iterator j = layer.getFeatureCollectionWrapper().query(
					fence.getEnvelopeInternal()).iterator(); j.hasNext();) {
				Feature candidate = (Feature) j.next();

				if (intersects(candidate.getGeometry(), fence)) {
					features.add(candidate);
				}
			}

			if (!features.isEmpty()) {
				map.put(layer, features);
			}
		}

		return map;
	}

	public static JPopupMenu popupMenu() {
		return popupMenu;
	}

	public void setCurrentCursorTool(CursorTool currentCursorTool) {
		this.currentCursorTool.deactivate();
		removeMouseListener(this.currentCursorTool);
		removeMouseMotionListener(this.currentCursorTool);
		this.currentCursorTool = currentCursorTool;
		currentCursorTool.activate(this);
		setCursor(currentCursorTool.getCursor());
		addMouseListener(currentCursorTool);
		addMouseMotionListener(currentCursorTool);
	}

	/**
	 * When a layer is added, if this flag is false, the viewport will be zoomed
	 * to the extent of the layer.
	 */
	public void setViewportInitialized(boolean viewportInitialized) {
		this.viewportInitialized = viewportInitialized;
	}

	public CursorTool getCurrentCursorTool() {
		return currentCursorTool;
	}

	/**
	 * Note: the popup menu is shown only if the user right-clicks the panel.
	 * Thus, popup-menu event handlers don't need to check whether the return
	 * value is null.
	 */
	public java.awt.Point getLastClickedPoint() {
		return lastClickedPoint;
	}

	public Viewport getViewport() {
		return viewport;
	}

	public Java2DConverter getJava2DConverter() {
		return viewport.getJava2DConverter();
	}

	/**
	 * @return the fence in model-coordinates, or null if there is no fence
	 */
	public Geometry getFence() {
		return fenceLayerFinder.getFence();
	}

	public LayerManager getLayerManager() {
		return layerManager;
	}

	public void featuresChanged(FeatureEvent e) {
	}

	public void categoryChanged(CategoryEvent e) {
	}

	public void layerChanged(LayerEvent e) {
		try {
			if (e.getType() == LayerEventType.METADATA_CHANGED) {
				return;
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						//Invoke later because other layers may be created in a
						// few
						//moments. [Jon Aquino]
						initializeViewportIfNecessary();
					} catch (Throwable t) {
						context.handleThrowable(t);
					}
				}
			});

			if (! deferLayerEvents)
			{
				if ((e.getType() == LayerEventType.ADDED)
						|| (e.getType() == LayerEventType.REMOVED)
						|| (e.getType() == LayerEventType.APPEARANCE_CHANGED)) {
					renderingManager.render(e.getLayerable());
				} else if (e.getType() == LayerEventType.VISIBILITY_CHANGED) {
					renderingManager.render(e.getLayerable(), false);
				} else {
					Assert.shouldNeverReachHere();
				}
			}
		} catch (Throwable t) {
			context.handleThrowable(t);
		}
	}

	/**
	 * Returns an image with the dimensions of this panel. Note that the image
	 * has an alpha component, and thus is not suitable for creating JPEGs --
	 * they will look pinkish.
	 */
	public Image createBlankPanelImage() {
		//The pixels will be transparent because we're creating a BufferedImage
		//from scratch instead of calling #createImage. [Jon Aquino]
		return new BufferedImage(getWidth(), getHeight(),
				BufferedImage.TYPE_INT_ARGB);
	}

	public void repaint() {
		if (renderingManager == null) {
			//It's null during initialization [Jon Aquino]
			superRepaint();

			return;
		}

		renderingManager.renderAll();
	}

	public void superRepaint() {
		super.repaint();
	}

	public void paintComponent(Graphics g) {
		try {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
			erase((Graphics2D) g);
			renderingManager.copyTo((Graphics2D) g);

			//g may not be the same as the result of #getGraphics; it may be an
			//off-screen buffer. [Jon Aquino]
			firePainted(g);
		} catch (Throwable t) {
			context.handleThrowable(t);
		}
	}

	public void erase(Graphics2D g) {
		fill(g, getBackground());
	}

	public void fill(Graphics2D g, Color color) {
		g.setColor(color);

		Rectangle2D.Double r = new Rectangle2D.Double(0, 0, getWidth(),
				getHeight());
		g.fill(r);
	}

	void jbInit() throws Exception {
		this.setBackground(Color.white);
		this.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				this_mouseReleased(e);
			}
		});
		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				this_componentResized(e);
			}
		});
		this.setLayout(borderLayout1);
	}

	void this_componentResized(ComponentEvent e) {
		try {
			viewport.update();
		} catch (Throwable t) {
			context.handleThrowable(t);
		}
	}

	public LayerViewPanelContext getContext() {
		return context;
	}

	void this_mouseReleased(MouseEvent e) {
		lastClickedPoint = e.getPoint();

		if (currentCursorTool.isRightMouseButtonUsed()) {
			return;
		}

		if (SwingUtilities.isRightMouseButton(e)) {
			//Custom workbenches might not add any items to the LayerViewPanel
			// popup menu.
			//[Jon Aquino]
			if (popupMenu.getSubElements().length == 0) {
				return;
			}

			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * When the first layer is added, zoom to its extent.
	 */
	private void initializeViewportIfNecessary()
			throws NoninvertibleTransformException {
		//Check envelope of *visible* layers because #zoomToFullExtent
		//now considers only visible layers [Jon Aquino 2004-06-18]
		if (!viewportInitialized && (layerManager.size() > 0)
				&& (layerManager.getEnvelopeOfAllLayers(true).getWidth() > 0)) {
			setViewportInitialized(true);
			viewport.zoomToFullExtent();
			//Return here because #zoomToFullExtent will eventually cause a
			// call to #paintComponent [Jon Aquino]
			return;
		}
	}

	public void addListener(LayerViewPanelListener listener) {
		listeners.add(listener);
	}

	public void removeListener(LayerViewPanelListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return d rounded off to the distance represented by one pixel
	 */
	public String format(double d) {
		double pixelWidthInModelUnits = viewport
				.getEnvelopeInModelCoordinates().getWidth()
				/ getWidth();

		return format(d, pixelWidthInModelUnits);
	}

	protected String format(double d, double pixelWidthInModelUnits) {
		int precisionInDecimalPlaces = (int) Math.max(0, //because
																							   // if
																							   // pixelWidthInModelUnits
																							   // > 1,
																							   // the
																							   // negative
																							   // log
																							   // will
																							   // be
																							   // negative
				Math.round( //not floor, which brings 0.999 down to
									   // 0
						(-Math.log(pixelWidthInModelUnits)) / Math.log(10)));
		precisionInDecimalPlaces++;

		//An extra decimal place, for good measure [Jon Aquino]
		String formatString = "#.";

		for (int i = 0; i < precisionInDecimalPlaces; i++) {
			formatString += "#";
		}

		return new DecimalFormat(formatString).format(d);
	}

	private void firePainted(Graphics graphics) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			LayerViewPanelListener l = (LayerViewPanelListener) i.next();
			l.painted(graphics);
		}
	}

	public void fireSelectionChanged() {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			LayerViewPanelListener l = (LayerViewPanelListener) i.next();
			l.selectionChanged();
		}
	}

	private void fireCursorPositionChanged(String x, String y) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			LayerViewPanelListener l = (LayerViewPanelListener) i.next();
			l.cursorPositionChanged(x, y);
		}
	}

	public RenderingManager getRenderingManager() {
		return renderingManager;
	}

	//Not sure where this method should reside. [Jon Aquino]
	public Collection featuresWithVertex(Point2D viewPoint,
			double viewTolerance, Collection features)
			throws NoninvertibleTransformException {
		Point2D modelPoint = viewport.toModelPoint(viewPoint);
		double modelTolerance = viewTolerance / viewport.getScale();
		Envelope searchEnvelope = new Envelope(modelPoint.getX()
				- modelTolerance, modelPoint.getX() + modelTolerance,
				modelPoint.getY() - modelTolerance, modelPoint.getY()
						+ modelTolerance);
		Collection featuresWithVertex = new ArrayList();

		for (Iterator j = features.iterator(); j.hasNext();) {
			Feature feature = (Feature) j.next();

			if (geometryHasVertex(feature.getGeometry(), searchEnvelope)) {
				featuresWithVertex.add(feature);
			}
		}

		return featuresWithVertex;
	}

	private boolean geometryHasVertex(Geometry geometry, Envelope searchEnvelope) {
		Coordinate[] coordinates = geometry.getCoordinates();

		for (int i = 0; i < coordinates.length; i++) {
			if (searchEnvelope.contains(coordinates[i])) {
				return true;
			}
		}

		return false;
	}

	public void dispose() {
		renderingManager.dispose();
		selectionManager.dispose();
		layerManager.removeLayerListener(this);
	}

	/**
	 * @param millisecondDelay
	 *                     the GUI will be unresponsive for this length of time, so keep
	 *                     it short!
	 */
	public void flash(final Shape shape, Color color, Stroke stroke,
			final int millisecondDelay) {
		final Graphics2D graphics = (Graphics2D) getGraphics();
		graphics.setColor(color);
		graphics.setXORMode(Color.white);
		graphics.setStroke(stroke);

		try {
			GUIUtil.invokeOnEventThread(new Runnable() {
				public void run() {
					try {
						graphics.draw(shape);

						//Use sleep rather than Timer (which could allow a
						// third party to paint
						//the panel between my XOR draws, messing up the XOR).
						// Hopefully the user
						//won't Alt-Tab away and back! [Jon Aquino]
						Thread.sleep(millisecondDelay);
						graphics.draw(shape);
					} catch (Throwable t) {
						getContext().handleThrowable(t);
					}
				}
			});
		} catch (Throwable t) {
			getContext().handleThrowable(t);
		}
	}

	public SelectionManager getSelectionManager() {
		return selectionManager;
	}

	public Blackboard getBlackboard() {
		return blackboard;
	}

	public void flash(final GeometryCollection geometryCollection)
			throws NoninvertibleTransformException {
		flash(getViewport().getJava2DConverter().toShape(geometryCollection),
				Color.red, new BasicStroke(5, BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND), 100);
	}

	public void setDeferLayerEvents(boolean defer)
	{
		deferLayerEvents = defer;
	}
}