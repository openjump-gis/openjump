package com.vividsolutions.jump.workbench.ui.renderer.style;

import javax.swing.Icon;

public interface ChoosableStyle extends Style {

    /**
     * For display.
     */
    public String getName();

    /**
     * For display. 20 x 20 pixels. 
     */
    public Icon getIcon();

}
