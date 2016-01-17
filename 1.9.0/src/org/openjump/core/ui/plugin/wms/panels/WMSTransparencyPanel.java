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
package org.openjump.core.ui.plugin.wms.panels;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;

public class WMSTransparencyPanel extends JPanel {
    /**
     * July 3 2015 - [Giuseppe Aruta] Panel that allows to set Transparency for
     * Panel that allows to set Transparency for WMS layers
     */
    private static final long serialVersionUID = 1L;

    private Border border1;
    private JLabel transparencyLabel = new JLabel();

    public String getTitle() {
        return I18N.get("ui.plugin.wms.EditWMSQueryPanel.transparency");
    }

    private WMSLayer layer;
    private LayerViewPanel panel;

    public WMSTransparencyPanel(WMSLayer layer, LayerViewPanel panel) {
        initialize();
        this.layer = layer;
        int alpha = layer.getAlpha();
        this.panel = panel;
        panel.setName(getTitle());

        // int alpha = layer.getAlpha();

        setAlpha(alpha);
    }

    public int getAlpha() {

        return 255 - transparencySlider.getValue() * 255 / 100;

    }

    private void setAlpha(int alpha) {

        transparencySlider.setValue(255 - alpha * 255 / 100);

    }

    protected JSlider transparencySlider = new JSlider();
    protected Dictionary sliderLabelDictionary = new Hashtable();

    private void initialize() {
        border1 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        this.setLayout(new GridBagLayout());

        this.setBorder(border1);
        this.setToolTipText("");

        transparencyLabel.setText(I18N
                .get("ui.plugin.wms.EditWMSQueryPanel.transparency"));

        Insets defaultInsets = new Insets(3, 3, 3, 3);

        this.add(new JLabel(
                " "     + I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageLayerControllPanel.set-overall-transparency")));

        for (int i = 0; i <= 100; i += 25) {
            this.sliderLabelDictionary.put(new Integer(i), new JLabel(i + "%"));
        }
        this.transparencySlider.setLabelTable(this.sliderLabelDictionary);
        this.transparencySlider.setPaintLabels(true);

        this.transparencySlider.setMaximum(100);
        this.transparencySlider.setMinimum(0);
        this.transparencySlider.setMajorTickSpacing(10);
        this.transparencySlider.setMinorTickSpacing(5);
        this.transparencySlider.setPaintTicks(true);

        this.transparencySlider.setMinimumSize(new Dimension(150, 20));
        this.transparencySlider.setValue((int) (getAlpha()));
        this.setLayout(new GridLayout(2, 1));
        this.add(this.transparencySlider);
        // transparencyPanel.setLabelTable(this.sliderLabelDictionary);

    }

    public void updateStyles() {
        layer.setAlpha(getAlpha());
        layer.fireAppearanceChanged();
    }
}
