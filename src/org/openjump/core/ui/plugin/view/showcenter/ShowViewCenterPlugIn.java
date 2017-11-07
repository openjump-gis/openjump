/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */
/*****************************************************
 * created:  original version by Vivid Solution
 * last modified:  03.06.2005
 * 
 * - initializes renderplugin
 * - plugin calculates the actual scale and draws the text
 *   (and a white rectangle around) in the map window
 *   all things are done in ShowScaleRenderer		
 *
 * @author sstein 
 * TODO how to put a mark on the menue item if tool is activated?
 *****************************************************/

package org.openjump.core.ui.plugin.view.showcenter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import org.saig.core.gui.swing.sldeditor.util.FormUtils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

/**
 * - initializes renderplugin - plugin calculates the actual scale and draws the
 * text (and a white rectangle around) in the map window all things are done in
 * ShowScaleRenderer
 * 
 * @author sstein
 */
public class ShowViewCenterPlugIn extends AbstractPlugIn {

    public static String NAME = "Show a crosshair in the center of the view";

    public static String DIMENSION = I18N.get("ui.FontChooser.size");

    public static String COLOR = I18N.get("ui.ColorChooserPanel.choose-color");

    Integer windth = 30;
    ImageIcon radio_icon_3 = GUIUtil.toSmallIcon(new ImageIcon(getClass()
            .getResource("icon3.gif")), windth);
    ImageIcon radio_icon_2 = GUIUtil.toSmallIcon(new ImageIcon(getClass()
            .getResource("icon2.gif")), windth);
    ImageIcon radio_icon = GUIUtil.toSmallIcon(new ImageIcon(getClass()
            .getResource("icon.gif")), windth);

    public static JRadioButtonMenuItem radio_button = new JRadioButtonMenuItem();
    public static JRadioButtonMenuItem radio_button_2 = new JRadioButtonMenuItem();
    public static JRadioButtonMenuItem radio_button_3 = new JRadioButtonMenuItem();
    public static JSpinner dimensionSpinner = new JSpinner();
    public static SpinnerModel dimensionModel = new SpinnerNumberModel(40, // initial
            // value
            10, // min
            200, // max
            5);

    static Color[] colors = { Color.black, Color.red, Color.orange,
            Color.yellow, Color.green, Color.cyan, Color.cyan.darker(),
            Color.blue, Color.magenta, Color.pink, Color.darkGray, Color.gray,
            Color.lightGray, Color.white };
    public static JComboBox<Color> colorBox = new JComboBox<Color>(colors);

    private MultiInputDialog dialog;

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        reportNothingToUndoYet(context);
        final LayerViewPanel pan = context.getLayerViewPanel();
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), getName(),
                true);
        dialog.setResizable(false);
        ButtonGroup buttonGroup = new ButtonGroup();
        radio_button = new JRadioButtonMenuItem("", radio_icon, true);
        buttonGroup.add(radio_button);
        radio_button_2 = new JRadioButtonMenuItem("", radio_icon_2);
        buttonGroup.add(radio_button_2);
        radio_button_3 = new JRadioButtonMenuItem("", radio_icon_3);
        buttonGroup.add(radio_button_3);

        dimensionSpinner = new JSpinner(dimensionModel);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(radio_button, BorderLayout.WEST);
        buttonPanel.add(radio_button_2, BorderLayout.CENTER);
        buttonPanel.add(radio_button_3, BorderLayout.EAST);
        JPanel pani = new JPanel(new GridBagLayout());
        dialog.addRow(buttonPanel);
        FormUtils.addRowInGBL(pani, 2, 0, DIMENSION, dimensionSpinner);

        colorBox.setMaximumRowCount(9);
        // colorBox.setPreferredSize(new Dimension(50, 20));
        colorBox.setRenderer(new ColorComboRenderer());

        // FormUtils.addRowInGBL(pani, 3, 0, COLOR, colorBox);
        dialog.addRow(pani);

        dialog.pack();
        if (ShowViewCenterRenderer.isEnabled(pan)) {

        } else {

            GUIUtil.centreOnWindow(this.dialog);
            this.dialog.setVisible(true);

            if (!this.dialog.wasOKPressed()) {
                return false;
            }
            dialog.setApplyVisible(true);

        }
        ShowViewCenterRenderer.setEnabled(
                !ShowViewCenterRenderer.isEnabled(pan), pan);
        pan.getRenderingManager().render(ShowViewCenterRenderer.CONTENT_ID);

        return true;
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
        ShowViewCenterInstallRenderer installRenderer = new ShowViewCenterInstallRenderer();
        installRenderer.initialize(context);
        featureInstaller.addMainMenuPlugin(this, new String[] { MenuNames.VIEW,
                MenuNames.MAP_DECORATIONS }, getName(), true, null,
                createEnableCheck(context.getWorkbenchContext()));

    }

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        MultiEnableCheck multiEnableCheck = new MultiEnableCheck();

        multiEnableCheck.add(
                checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck())
                .add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        ((JCheckBoxMenuItem) component)
                                .setSelected(ShowViewCenterRenderer
                                        .isEnabled(workbenchContext
                                                .getLayerViewPanel()));
                        return null;
                    }
                });
        return multiEnableCheck;
    }

    @Override
    public String getName() {
        return NAME;
    }

}

/*
 * Adapted from
 * http://www.java2s.com/Code/Java/Swing-JFC/Colorcomboboxrenderer.htm
 */

class ColorComboRenderer extends JPanel implements ListCellRenderer<Object> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected Color m_c = Color.black;

    public ColorComboRenderer() {
        super();
        setBorder(new CompoundBorder(
                new MatteBorder(2, 10, 2, 10, Color.white), new LineBorder(
                        Color.black)));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Component getListCellRendererComponent(JList list, Object obj,
            int row, boolean sel, boolean hasFocus) {
        if (obj instanceof Color)
            m_c = (Color) obj;
        return this;
    }

    @Override
    public void paint(Graphics g) {
        setBackground(m_c);
        super.paint(g);
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] a) {
        JComboBox<Color> cbColor = new JComboBox<Color>();
        int[] values = new int[] { 0, 128, 192, 255 };
        for (int r = 0; r < values.length; r++)
            for (int g = 0; g < values.length; g++)
                for (int b = 0; b < values.length; b++) {
                    Color c = new Color(values[r], values[g], values[b]);
                    cbColor.addItem(c);
                }
        cbColor.setRenderer(new ColorComboRenderer());

        JFrame f = new JFrame();
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.getContentPane().add(cbColor);
        f.pack();
        f.setSize(new Dimension(50, 20));
        f.show();

    }
}