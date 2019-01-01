/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Code is based on code from com.vividsolutions.jump.workbench.ui.plugin.wms.URLWizardPanel.java
 *
 * $Id: ImageWizardPanel.java,v 0.1 20041110 
 *
 * Copyright (C) 2004 Jan Ruzicka jan.ruzicka@vsb.cz
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
 */

/**
 *
 * <p>Title: ImageWizardPanel</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author Jan Ruzicka jan.ruzicka@vsb.cz
 * @version 0.1
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */

package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

/**
 * Panel for setting properties for viewed image.
 * 
 * @author jan.ruzicka@vsb.cz
 */
public class RasterImageWizardPanel extends JPanel implements WizardPanel,
        ActionListener {

    private static final long serialVersionUID = -6644440388147608621L;
    public static final String WRITE_WORLDFILE = I18N
            .get("ui.plugin.SaveImageAsPlugIn.write-world-file");
    public static final String MINX_KEY = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.minx");
    public static final String MAXX_KEY = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxx");
    public static final String MINY_KEY = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.miny");
    public static final String MAXY_KEY = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxy");
    public static final String TITLE = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.worldfile-dialog");
    public static final String INSTRUCTION = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.please-enter-the-image-path");
    public static final String WORLD = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.optimizedworldfile");
    public static final String TOOLTIP = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.warp-tooltip");;
    public static final String WARP = I18N
            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.warp");

    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private Map dataMap;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private JPanel fillerPanel = new JPanel();
    private JLabel minxLabel = new JLabel();
    public static JTextField minxTextField = new JTextField();
    private JLabel maxxLabel = new JLabel();
    public static JTextField maxxTextField = new JTextField();
    private JLabel minyLabel = new JLabel();
    public static JTextField minyTextField = new JTextField();
    private JLabel maxyLabel = new JLabel();
    public static JTextField maxyTextField = new JTextField();
    public static JCheckBox warpCheckBox = new JCheckBox();

    public RasterImageWizardPanel() {
        try {
            jbInit();
            minxTextField
                    .setText(I18N
                            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.west-coordinate"));
            maxxTextField
                    .setText(I18N
                            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.east-coordinate"));
            minyTextField
                    .setText(I18N
                            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.south-coordinate"));
            maxyTextField
                    .setText(I18N
                            .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.north-coordinate"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    public void remove(InputChangedListener listener) {
        inputChangedFirer.remove(listener);
    }

    /**
     * Creates UI on JPanel
     */
    void jbInit() throws Exception {

        minxLabel
                .setText(I18N
                        .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.minx"));
        maxxLabel
                .setText(I18N
                        .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxx"));
        minyLabel
                .setText(I18N
                        .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.miny"));
        maxyLabel
                .setText(I18N
                        .get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxy"));

        this.setLayout(gridBagLayout1);
        minxTextField.setPreferredSize(new Dimension(270, 21));
        minxTextField.setCaretPosition(minxTextField.getText().length());
        maxxTextField.setPreferredSize(new Dimension(270, 21));
        maxxTextField.setCaretPosition(maxxTextField.getText().length());
        minyTextField.setPreferredSize(new Dimension(270, 21));
        minyTextField.setCaretPosition(minyTextField.getText().length());
        maxyTextField.setPreferredSize(new Dimension(270, 21));
        maxyTextField.setCaretPosition(maxyTextField.getText().length());
        this.setPreferredSize(new Dimension(450, 100));

        this.add(minxLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 4), 0, 0));
        this.add(minxTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxxLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 4), 0, 0));
        this.add(maxxTextField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(minyLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 4), 0, 0));
        this.add(minyTextField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxyLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
                        0, 0, 0, 4), 0, 0));
        this.add(maxyTextField, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        warpCheckBox.setText(WARP);
        warpCheckBox.setToolTipText(TOOLTIP);
        // textPane.setText(WORLD);
        warpCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnabled();

            }
        });

        this.add(warpCheckBox, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));

        this.add(fillerPanel, new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                        0, 0, 0, 0), 0, 0));
    }

    private void updateEnabled() {

        minxLabel.setEnabled(warpCheckBox.isSelected() == false);
        minxTextField.setEnabled(warpCheckBox.isSelected() == false);
        maxxLabel.setEnabled(warpCheckBox.isSelected() == false);
        maxxTextField.setEnabled(warpCheckBox.isSelected() == false);
        minyLabel.setEnabled(warpCheckBox.isSelected() == false);
        minyTextField.setEnabled(warpCheckBox.isSelected() == false);
        maxyLabel.setEnabled(warpCheckBox.isSelected() == false);
        maxyTextField.setEnabled(warpCheckBox.isSelected() == false);
    }

    public String getInstructions() {
        return INSTRUCTION + "\n" + WORLD;
    }

    /**
     * Works after Finish button. Sets the values from text boxes to the dataMap
     * object.
     */
    public void exitingToRight() throws IOException, WorkbenchException {
        dataMap.put(MINX_KEY, minxTextField.getText());
        dataMap.put(MAXX_KEY, maxxTextField.getText());
        dataMap.put(MINY_KEY, minyTextField.getText());
        dataMap.put(MAXY_KEY, maxyTextField.getText());
    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;
    }

    public String getTitle() {
        return TITLE;
    }

    public String getID() {
        return getClass().getName();
    }

    public boolean isInputValid() {
        return true;
    }

    public String getNextID() {
        return null;
    }

    /**
     * Works after Browse button. Opens OpenFile Dialog.
     */
    public void actionPerformed(ActionEvent e) {
    }

}