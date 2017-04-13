package com.vividsolutions.jump.workbench.ui.snap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.cursortool.MultiClickTool;

import java.util.ArrayList;

/**
 * A SnapPolicy to snap new vertices onto vetices being edited.
 */
public class SnapToLineStringBeingEditedPolicy implements SnapPolicy {

    private GeometryFactory factory = new GeometryFactory();
    //On-screen features are cached. The cache is built lazily. [Jon Aquino]

    private MultiClickTool cursor;

    private Blackboard blackboard;

    public static final String ENABLED_KEY = SnapToLineStringBeingEditedPolicy.class.getName() + " - ENABLED";

    public SnapToLineStringBeingEditedPolicy(Blackboard blackboard, MultiClickTool cursor) {
        this.blackboard = blackboard;
        this.cursor = cursor;
    }

    public Coordinate snap(LayerViewPanel panel, Coordinate originalPoint) {
        if (!blackboard.get(ENABLED_KEY, false)) {
            return null;
        }
        Geometry bufferedCursorLocation;
        bufferedCursorLocation =
                factory.createPoint(originalPoint).buffer(SnapManager.getToleranceInPixels(blackboard) / panel.getViewport().getScale());

        ArrayList vertices = new ArrayList();
        for (Object c : cursor.getCoordinates()) {
            if (bufferedCursorLocation.intersects(bufferedCursorLocation.getFactory().createPoint((Coordinate)c))) {
                vertices.add(c);
            }
        }
        if (vertices.isEmpty()) {
            return null;
        }
        return CoordUtil.closest(vertices, originalPoint);
    }

}
