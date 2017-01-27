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

import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * TODO Purpose of
 * <p>
 * </p>
 * 
 * @author
 * @since 1.0
 */
public abstract class OptionsPanelV2 extends JPanel implements OptionsPanel {

    /** long serialVersionUID field */
    private static final long serialVersionUID = 1L;

    /**
     * Get the name for this options panel. It will be visible in its tab.
     * 
     * @return
     */
    public abstract String getName();

    /**
     * Get the icon associated to this options panel. It will be visible in its tab
     * <p>
     * Return null if there is no icon associated to it
     * </p>
     * 
     * @return
     */
    public abstract Icon getIcon();

    /**
     * Notifies this panel that the OptionsDialog has been (re-)opened
     */
    public abstract void init();

    /**
     * @return an error message if a field is not valid; otherwise, null
     */
    public abstract String validateInput();

    /**
     * Notifies this panel that it should commit its entries to the system
     */
    public abstract void okPressed();
}