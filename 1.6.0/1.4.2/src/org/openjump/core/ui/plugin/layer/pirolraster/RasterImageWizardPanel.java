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
* @author jan.ruzicka@vsb.cz
*/
public class RasterImageWizardPanel extends JPanel implements WizardPanel, ActionListener {

    private static final long serialVersionUID = -6644440388147608621L;   
    
    public static final String MINX_KEY = I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.minx"); //$NON-NLS-1$
    public static final String MAXX_KEY = I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxx"); //$NON-NLS-1$
    public static final String MINY_KEY = I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.miny"); //$NON-NLS-1$
    public static final String MAXY_KEY = I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxy"); //$NON-NLS-1$

    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private Map dataMap;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private JPanel fillerPanel = new JPanel();
    private JLabel minxLabel = new JLabel();
    private JTextField minxTextField = new JTextField();
    private JLabel maxxLabel = new JLabel();
    private JTextField maxxTextField = new JTextField();
    private JLabel minyLabel = new JLabel();
    private JTextField minyTextField = new JTextField();
    private JLabel maxyLabel = new JLabel();
    private JTextField maxyTextField = new JTextField();

    public RasterImageWizardPanel() {
        try {
            jbInit();
            minxTextField.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.west-coordinate")); //$NON-NLS-1$
            maxxTextField.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.east-coordinate")); //$NON-NLS-1$
            minyTextField.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.south-coordinate")); //$NON-NLS-1$
            maxyTextField.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.north-coordinate")); //$NON-NLS-1$
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

        minxLabel.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.minx")); //$NON-NLS-1$
	    maxxLabel.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxx")); //$NON-NLS-1$
	    minyLabel.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.miny")); //$NON-NLS-1$
	    maxyLabel.setText(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.maxy")); //$NON-NLS-1$

        this.setLayout(gridBagLayout1);
        minxTextField.setPreferredSize(new Dimension(270, 21));
        minxTextField.setCaretPosition(minxTextField.getText().length());
 	    maxxTextField.setPreferredSize(new Dimension(270, 21));
        maxxTextField.setCaretPosition(maxxTextField.getText().length());
	    minyTextField.setPreferredSize(new Dimension(270, 21));
        minyTextField.setCaretPosition(minyTextField.getText().length());
	    maxyTextField.setPreferredSize(new Dimension(270, 21));
        maxyTextField.setCaretPosition(maxyTextField.getText().length());

        this.add(minxLabel,
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(minxTextField,
            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxxLabel,
            new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxxTextField,
            new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(minyLabel,
            new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(minyTextField,
            new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxyLabel,
            new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(maxyTextField,
            new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    public String getInstructions() {
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.please-enter-the-image-path"); //$NON-NLS-1$
    }

    /**
    * Works after Finish button. Sets the values from text boxes to the dataMap object.
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
        return I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.RasterImageWizardPanel.worldfile-dialog"); //$NON-NLS-1$
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
    public void actionPerformed(ActionEvent e) {}	

}