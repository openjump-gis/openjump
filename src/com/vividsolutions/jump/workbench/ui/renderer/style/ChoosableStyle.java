package com.vividsolutions.jump.workbench.ui.renderer.style;

import javax.swing.Icon;

public interface ChoosableStyle extends Style {

    /**
     * For display.
     * @return the name of this Style
     */
    String getName();

    /**
     * For display. 20 x 20 pixels.
     * @return an Icon representing this Style
     */
    Icon getIcon();

}
