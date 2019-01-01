
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

import com.vividsolutions.jump.I18N;


public class OKCancelApplyPanel extends OKCancelPanel {

    public OKCancelApplyPanel() {
        super(new String[] {I18N.get("ui.OKCancelPanel.ok"), 
                            I18N.get("ui.OKCancelPanel.cancel"),
                            I18N.get("ui.OKCancelApplyPanel.apply")});
        setApplyVisible(false);
    }

    public boolean wasApplyPressed() {
        return getSelectedButton() == getButton(I18N.get("ui.OKCancelApplyPanel.apply"));
    }
    
    public void setApplyPressed(boolean applyPressed) {
      if (applyPressed)
        setSelectedButton(getButton(I18N.get("ui.OKCancelApplyPanel.apply")));
      else
        setSelectedButton(null);
    }
    
    public void setApplyEnabled(boolean applyEnabled) {
        getButton(I18N.get("ui.OKCancelApplyPanel.apply")).setEnabled(applyEnabled);
    }

    public void setApplyVisible(boolean applyVisible) {
        if (applyVisible && !innerButtonPanel.isAncestorOf(getButton( I18N.get("ui.OKCancelApplyPanel.apply")))) {
            innerButtonPanel.add(getButton( I18N.get("ui.OKCancelApplyPanel.apply")), null);
        }
    
        if (!applyVisible && innerButtonPanel.isAncestorOf(getButton( I18N.get("ui.OKCancelApplyPanel.apply")))) {
            innerButtonPanel.remove(getButton( I18N.get("ui.OKCancelApplyPanel.apply")));
        }
    }
}
