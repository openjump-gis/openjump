package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.MeasureLayerFinder;
import com.vividsolutions.jump.workbench.ui.cursortool.CoordinateListMetrics;
import com.vividsolutions.jump.workbench.ui.cursortool.PolygonTool;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.Icon;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import org.openide.awt.DropDownButtonFactory;

/**
 * A tool for measurment of distance or area.
 *
 *
 * @author Matthias Scholz  <ms@jammerhund.de>
 * @version 0.1
 */
/*
 * TODO:
 *			- i18n
 *			- JavaDoc
 *			- Icons and Cursors
 *			- OptionDialog for Styling
 */
public class AdvancedMeasureTool extends PolygonTool implements ActionListener {

	/**
	 * Measure mode distance.
	 */
	public static final int MEASURE_MODE_DISTANCE = 1;
	/**
	 * Measure mode area.
	 */
	public static final int MEASURE_MODE_AREA = 2;
	// the JPopupMenu of the DropDownToggleButton
	JPopupMenu popupMenu;
	// the two menuitems
	JMenuItem distanceMenuItem;
	JMenuItem areaMenuItem;
	// in which mode we are?
	private int measureMode = MEASURE_MODE_DISTANCE;
	// the JToggleButton in the WorkbenchToolBar
	private JToggleButton toolbarButton = null;
	WorkbenchContext context;
	Point mousePosition = null;
	Shape lastShape = null;

	public AdvancedMeasureTool(WorkbenchContext context) {
		this.context = context;
		allowSnapping();
		setMetricsDisplay(new CoordinateListMetrics());
		setCloseRing(false); // distance mode
		popupMenu = new JPopupMenu();
		distanceMenuItem = new JMenuItem("Distance measuring", IconLoader.icon("Ruler.gif")); // TODO: i18n
		distanceMenuItem.addActionListener(this);
		popupMenu.add(distanceMenuItem);
		areaMenuItem = new JMenuItem("Area measuring", IconLoader.icon("Ruler_area.gif")); // TODO: i18n
		areaMenuItem.addActionListener(this);
		popupMenu.add(areaMenuItem);

		// the Button for the ToolBar
		toolbarButton = DropDownButtonFactory.createDropDownToggleButton(getIcon(), getPopupMenu());
	}

	/**
	 * Returns the Icon depending on the measureMode.
	 *
	 * @return
	 */
	public Icon getIcon() {
		String iconName = "Ruler.gif";
		switch (measureMode) {
			case MEASURE_MODE_DISTANCE:
				iconName = "Ruler.gif";
				break;
			case MEASURE_MODE_AREA:
				iconName = "Ruler_area.gif";
				break;
		}
		return IconLoader.icon(iconName);
	}

	/**
	 * Returns the Cursor depending on the measureMode.
	 *
	 * @return the Cursor
	 */
	@Override
	public Cursor getCursor() {
		String cursorName = "RulerCursor.gif";
		switch (measureMode) {
			case MEASURE_MODE_DISTANCE:
				cursorName = "RulerCursor.gif";
				break;
			case MEASURE_MODE_AREA:
				cursorName = "RulerCursor_area.gif";
				break;
		}
		return createCursor(IconLoader.icon(cursorName).getImage());
	}

	@Override
	public void mouseLocationChanged(MouseEvent e) {
		mousePosition = e.getPoint();

		try {
			if (isShapeOnScreen()) {
				ArrayList currentCoordinates = new ArrayList(getCoordinates());
				currentCoordinates.add(getPanel().getViewport().toModelCoordinate(e.getPoint()));
			}
			super.mouseLocationChanged(e);
		} catch (Throwable t) {
			getPanel().getContext().handleThrowable(t);
		}
	}

	/**
	 * With the first click we delete an old measurment from the screen.
	 *
	 * @param e
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		MeasureLayerFinder measureLayerFinder = new MeasureLayerFinder(getPanel());
		if (measureLayerFinder.getMeasureLayer() != null) { // no Layer, no old measurment
			if (measureLayerFinder.getMeasureLayer().getFeatureCollectionWrapper().getFeatures().size() > 0) { //only if we have an old measurment (Features on the Layer)
				// clear only if this is the first click
				if (getCoordinates().size() == 1) {
					measureLayerFinder.getMeasureLayer().getFeatureCollectionWrapper().clear();
				}
			}
		}
	}



	private boolean doubleClicked() {
        return getCoordinates().size() == 1;
    }


	protected void gestureFinished() throws NoninvertibleTransformException {
		reportNothingToUndoYet();

		getMetrics().displayMetrics(getCoordinates(), getPanel());

		Geometry measureGeometry = null;

        if (doubleClicked()) {
            measureGeometry = null;
        } else {
			if (measureMode == MEASURE_MODE_DISTANCE) {
				List coordinates = getCoordinates();
				measureGeometry = new GeometryFactory().createLineString(toArray(coordinates));
			} else if (measureMode == MEASURE_MODE_AREA) {
				if (!checkPolygon()) {
					return;
				}

				//Don't want viewport to change at this stage. [Jon Aquino]
				getPanel().setViewportInitialized(true);

				measureGeometry = getPolygon();
			}
        }

        MeasureLayerFinder measureLayerFinder = new MeasureLayerFinder(getPanel());
        measureLayerFinder.setMeasure(measureGeometry);

        if (!measureLayerFinder.getLayer().isVisible()) {
            measureLayerFinder.getLayer().setVisible(true);
        }
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	/**
	 * @return the toolbarButton
	 */
	public JToggleButton getToolbarButton() {
		return toolbarButton;
	}

	/**
	 * ActionListener for the JMenuItems.
	 * We must set the Icon, the TooltipText, Cursor and the CloseRing Mode.
	 * Second the tool will be activated through a JMenuItem ActionEvent.
	 * So you do not need a second click ;-)
	 *
	 * @param e
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == distanceMenuItem) { // Distance
			measureMode = MEASURE_MODE_DISTANCE;
			if (toolbarButton != null) {
				toolbarButton.setIcon(IconLoader.icon("Ruler.gif"));
				toolbarButton.setToolTipText("Distance measuring"); // TODO: i18n
				// activate this tool
				toolbarButton.setSelected(true);
				context.getLayerViewPanel().setCurrentCursorTool(this);
			}
			setCloseRing(false);
		} else if (e.getSource() == areaMenuItem) { // Area
			measureMode = MEASURE_MODE_AREA;
			if (toolbarButton != null) {
				toolbarButton.setIcon(IconLoader.icon("Ruler_area.gif"));
				toolbarButton.setToolTipText("Area measuring"); // TODO: i18n
				// activate this tool
				toolbarButton.setSelected(true);
				context.getLayerViewPanel().setCurrentCursorTool(this);
			}
			setCloseRing(true);
		}
		context.getLayerViewPanel().setCursor(getCursor());
	}
}
