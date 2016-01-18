
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


public class OKCancelPanel extends ButtonPanel {

    public OKCancelPanel() {
        super(new String[] {I18N.get("ui.OKCancelPanel.ok"), I18N.get("ui.OKCancelPanel.cancel")});
    }
    
    public OKCancelPanel(String[] buttonNames) {
        super(buttonNames);
    }

    public boolean wasOKPressed() {
        return getSelectedButton() == getButton(I18N.get("ui.OKCancelPanel.ok"));
    }

    public void setOKPressed(boolean okPressed) {
      if (okPressed)
        setSelectedButton(getButton(I18N.get("ui.OKCancelPanel.ok")));
      else
        setSelectedButton(null);
    }

    public void setOKEnabled(boolean okEnabled) {
        getButton(I18N.get("ui.OKCancelPanel.ok")).setEnabled(okEnabled);
    }

    public void setCancelVisible(boolean cancelVisible) {
        if (cancelVisible && !innerButtonPanel.isAncestorOf(getButton( I18N.get("ui.OKCancelPanel.cancel")))) {
            innerButtonPanel.add(getButton( I18N.get("ui.OKCancelPanel.cancel")), null);
        }

        if (!cancelVisible && innerButtonPanel.isAncestorOf(getButton( I18N.get("ui.OKCancelPanel.cancel")))) {
            innerButtonPanel.remove(getButton( I18N.get("ui.OKCancelPanel.cancel")));
        }
    }
}
