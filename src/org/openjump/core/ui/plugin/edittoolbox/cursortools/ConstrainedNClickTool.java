package org.openjump.core.ui.plugin.edittoolbox.cursortools;

/**
 * @author Giuseppe Aruta. Adapted from OpenJUMP ConstrainedClickTool class
 * @since OpenJUMP 1.10 
 */
import java.awt.event.MouseEvent;

import org.locationtech.jts.geom.Coordinate;

import com.vividsolutions.jump.workbench.WorkbenchContext;

public abstract class ConstrainedNClickTool extends ConstrainedMultiClickTool {
    protected int n;

    public ConstrainedNClickTool(WorkbenchContext context, int n) {
        super(context);
        this.n = n;
    }

    public int numClicks() {
        return this.n;
    }

    protected Coordinate getModelSource() {
        return (Coordinate) getCoordinates().get(0);
    }

    protected Coordinate getModelDestination() {
        return (Coordinate) getCoordinates().get(this.n - 1);
    }

    @Override
    protected boolean isFinishingRelease(MouseEvent e) {
        return ((e.getClickCount() == 1) && (shouldGestureFinish()))
                || (super.isFinishingRelease(e));
    }

    private boolean shouldGestureFinish() {
        return getCoordinates().size() == this.n;
    }
}
