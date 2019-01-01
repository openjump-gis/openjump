/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2007, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Información Geográfica, S.L.
 * Avnda. República Argentina, 28
 * Edificio Domocenter Planta 2ª Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * España / Spain
 *
 * Teléfono / Phone Number
 * +34 954 788876
 * 
 * Correo electrónico / Email
 * info@saig.es
 *
 */
package org.saig.jump.widgets.config;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import org.saig.jump.lang.I18N;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.OptionsPanelV2;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * Panel de configuracion de los tooltips de las capas </p>
 * 
 * @author Eduardo Montero Ruiz
 * @since 1.0.0
 * 
 *        [2015_03_28] Giuseppe Aruta - ported to OpenJUMP
 */
public class ConfigTooltipPanel extends OptionsPanelV2 {

    private Blackboard blackboard;
    private JPanel tooltipPanel;
    private JCheckBox tooltipCheck;

    /** Nombre asociado al panel */
    public final static String NAME = "Configure Tooltip";
    //I18N.getString("org.saig.jump.widgets.config.ConfigTooltipPanel.layer-info");

    /** Icono asociado al panel */
    public final static Icon ICON = null;
    //IconLoader.icon("note.png");
    /** Opciones de tooltip */
    public static final String LAYER_TOOLTIPS_ON = ConfigTooltipPanel.class
            .getName() + " - LAYER_TOOLTIPS";

    /**
     * Constructor del panel
     * 
     * @param blackboard
     */
    public ConfigTooltipPanel(Blackboard blackboard) {
        super();
        this.blackboard = blackboard;
        this.setLayout(new GridBagLayout());

        // Anyadimos los paneles
        FormUtils.addRowInGBL(this, 1, 0, getTooltipPanel());
        FormUtils.addFiller(this, 2, 0);
    }

    /**
     * 
     * @return
     */
    private JPanel getTooltipPanel() {
        if (tooltipPanel == null) {
            tooltipPanel = new JPanel(new GridBagLayout());
            tooltipPanel.setBorder(BorderFactory.createTitledBorder(I18N
                    .getString("Configure layer tree tooltip")));
            tooltipCheck = new JCheckBox("Enable JUMP basic tooltips");
            FormUtils.addRowInGBL(tooltipPanel, 0, 0, tooltipCheck);
        }
        return tooltipPanel;
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        boolean layerTooltipsOn = PersistentBlackboardPlugIn.get(blackboard)
                .get(LAYER_TOOLTIPS_ON, false);
        if (layerTooltipsOn) {
            tooltipCheck.setSelected(true);
        }
    }

    @Override
    public void okPressed() {
        PersistentBlackboardPlugIn.get(blackboard).put(LAYER_TOOLTIPS_ON,
                tooltipCheck.isSelected());
    }

    @Override
    public String validateInput() {
        return null;
    }

}
