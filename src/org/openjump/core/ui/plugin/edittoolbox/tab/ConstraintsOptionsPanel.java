/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2004 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.edittoolbox.tab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.OptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class ConstraintsOptionsPanel extends JPanel implements OptionsPanel {
    private Border titleBorder;
    WorkbenchContext workbenchContext;
    final static String length = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Length");
    final static String incAngle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Incremental-Angle");
    final static String incAngleShiftCtrl = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Incremental-Angle-Shift-to-activate-Ctrl-to-close");
    final static String angle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Angle");
    final static String angleShift = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Angle-Shift-to-activate");
    final static String constrainAngleByStepsOf = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-angle-by-steps-of");
    final static String degree = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.degree");
    final static String negativeNumbersNotAllowedForLength=I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.negative-numbers-not-allowed-for-length");
    final static String isToSmall = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.is-too-small-must-be-3-or-greater");
    final static String invalidNumbers = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.invalid-numbers");
    final static String constrainLengthToNearest = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-length-to-nearest");
    final static String modelUnits = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.model-units");
    final static String ConstrainAngleTo45DegreeIncrements = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-angle-to-45-degree-increments");
    final static String byDividing360DegreesInto = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.by-dividing-360-degrees-into");
    final static String parts = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.parts");
    final static String constrainToRelativeAngle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-to-relative-angle");
    final static String constrainToAbsoluteAngle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-to-absolute-angle");
    final static String degrees = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.degrees");
    final static String constrainToAngle = I18N.get("org.openjump.core.ui.plugin.edittoolbox.tab.ConstraintsOptionsPanel.Constrain-to-angle");
    
    
    //main constraints panel
    private JPanel constraintsPanel = new JPanel();
    private BorderLayout constraintsBorderLayout = new BorderLayout();
    private GridBagLayout constraintsGridBagLayout = new GridBagLayout();
    
    //length constraint
    private JPanel lengthPanel = new JPanel();
    private JPanel lengthSubPanel = new JPanel();
    private GridBagLayout lengthPanelGridBagLayout = new GridBagLayout();
    private GridBagLayout lengthSubPanelGridBagLayout = new GridBagLayout();
    private TitledBorder lengthPanelTitle = new TitledBorder(titleBorder, length);
    private JCheckBox lengthCheckBox = new JCheckBox();
    private JTextField lengthTextField = new JTextField(4);
    private JLabel lengthUnitsLabel = new JLabel();
    
    //incremental angle constraint
    private JPanel incrementalAnglePanel = new JPanel();
    private JPanel incrementalAngleSubPanel = new JPanel();
    private GridBagLayout incrementalAnglePanelGridBagLayout = new GridBagLayout();
    private GridBagLayout incrementalAngleSubPanelGridBagLayout = new GridBagLayout();
    private TitledBorder IncrementalAnglePanelTitleShort = new TitledBorder(titleBorder, incAngle);
    private TitledBorder IncrementalAnglePanelTitleLong = new TitledBorder(titleBorder, incAngleShiftCtrl);
    private JCheckBox constrainIncrementalAngleCheckBox = new JCheckBox();
    private JTextField numPartsTextField = new JTextField(4);
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private GridBagLayout gridBagLayout4 = new GridBagLayout();
    private JPanel numPartsPanel = new JPanel();
    private JLabel numPartsPreLabel = new JLabel();
    private JLabel numPartsPostLabel = new JLabel();
    
    //angle constraint
    private JPanel anglePanel = new JPanel();
    private TitledBorder anglePanelTitleShort = new TitledBorder(titleBorder, angle);
    private TitledBorder anglePanelTitleLong = new TitledBorder(titleBorder, angleShift);
    private GridBagLayout AnglePanelGridBagLayout = new GridBagLayout();
    private GridBagLayout angleDataPanelGridBagLayout = new GridBagLayout();
    private JPanel angleDataPanel = new JPanel();
    private JTextField angleConstraintTextField = new JTextField(4);
    private JCheckBox constrainAngleCheckBox = new JCheckBox();
    private JRadioButton relativeAngleRadioButton = new JRadioButton();
    private JRadioButton absoluteAngleRadioButton = new JRadioButton();
    private JLabel angleUnitsLabel = new JLabel();
    private ButtonGroup angleButtonGroup = new ButtonGroup();
    private ButtonGroup buttonGroup = new ButtonGroup();
    private JLabel absAngleImageLabel = new JLabel();
    private JLabel relAngleImageLabel = new JLabel();
  
    public ConstraintsOptionsPanel(WorkbenchContext workbenchContext) {
        this.workbenchContext = workbenchContext;

        try {
            this.jbInit();
        } catch (Exception e) {
            Assert.shouldNeverReachHere(e.toString());
        }

        lengthCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                updateEnabled();
            }
        });
        
        constrainIncrementalAngleCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                updateEnabled();
            }
        });
        
        constrainAngleCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                updateEnabled();
            }
        });
        
        //numPartsTextField.addFocusListener(new myFocusListener()
        //{
        //    public void focusLost(FocusEvent e)
        //    {
        //        double newAngle = 360.0 / Double.parseDouble(numPartsTextField.getText());
        //        //constrainIncrementalAngleCheckBox.setText("Constrain angle to " + newAngle + " degree increments");
        //        //[sstein: 16.10.2005]
        //        constrainIncrementalAngleCheckBox.setText(constrainAngleByStepsOf + " " + newAngle + " " + degree);
        //    }
        //});
    }

    private void updateEnabled() {
        lengthTextField.setEnabled(lengthCheckBox.isSelected());
        numPartsTextField.setEnabled(constrainIncrementalAngleCheckBox.isSelected());
        angleConstraintTextField.setEnabled(constrainAngleCheckBox.isSelected());
        relativeAngleRadioButton.setEnabled(constrainAngleCheckBox.isSelected());
        absoluteAngleRadioButton.setEnabled(constrainAngleCheckBox.isSelected());
        
        if (constrainIncrementalAngleCheckBox.isSelected())
            incrementalAnglePanel.setBorder(IncrementalAnglePanelTitleLong);
        else
            incrementalAnglePanel.setBorder(IncrementalAnglePanelTitleShort);
        
        if (constrainAngleCheckBox.isSelected())
            anglePanel.setBorder(anglePanelTitleLong);
        else
            anglePanel.setBorder(anglePanelTitleShort);

    }

    public String validateInput()
    {
        String errorMessage1 = "\"" + lengthTextField.getText() +
        "\"" + negativeNumbersNotAllowedForLength;
        String errorMessage2 = "\"" + numPartsTextField.getText() +
        "\"" + isToSmall;
        String errorMessage3 = invalidNumbers;
        
        try
        {
            if (Double.parseDouble(lengthTextField.getText()) < 0)
            {
                return errorMessage1;
            }
            if (Integer.parseInt(numPartsTextField.getText()) < 3)
            {
                return errorMessage2;
            }
        } catch (NumberFormatException e)
        {
            return errorMessage3;
        }
        return null;
    }

    public void okPressed() 
    {
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.CONSTRAIN_LENGTH_ENABLED_KEY, lengthCheckBox.isSelected());
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.LENGTH_CONSTRAINT_KEY, Double.parseDouble(lengthTextField.getText()));
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.CONSTRAIN_INCREMENTAL_ANGLE_ENABLED_KEY, constrainIncrementalAngleCheckBox.isSelected());
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.INCREMENTAL_ANGLE_SIZE_KEY, Integer.parseInt(numPartsTextField.getText()));
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.CONSTRAIN_ANGLE_ENABLED_KEY, constrainAngleCheckBox.isSelected());
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.ANGLE_SIZE_KEY, Double.parseDouble(angleConstraintTextField.getText()));
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.RELATIVE_ANGLE_KEY, relativeAngleRadioButton.isSelected());
        PersistentBlackboardPlugIn.get(workbenchContext).put(ConstraintManager.ABSOLUTE_ANGLE_KEY, absoluteAngleRadioButton.isSelected());
    }

    public void init() {
        lengthCheckBox.setSelected(PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.CONSTRAIN_LENGTH_ENABLED_KEY, false));
        lengthTextField.setText("" + PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.LENGTH_CONSTRAINT_KEY, 0.5d));
        constrainIncrementalAngleCheckBox.setSelected(PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.CONSTRAIN_INCREMENTAL_ANGLE_ENABLED_KEY, false));
        numPartsTextField.setText("" + PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.INCREMENTAL_ANGLE_SIZE_KEY, 8));
        constrainAngleCheckBox.setSelected(PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.CONSTRAIN_ANGLE_ENABLED_KEY, false));
        angleConstraintTextField.setText("" + PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.ANGLE_SIZE_KEY, 30d));
        relativeAngleRadioButton.setSelected(PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.RELATIVE_ANGLE_KEY, true));
        absoluteAngleRadioButton.setSelected(PersistentBlackboardPlugIn.get(workbenchContext).get(ConstraintManager.ABSOLUTE_ANGLE_KEY, false));
        double newAngle = 360.0 / Double.parseDouble(numPartsTextField.getText());
        //constrainIncrementalAngleCheckBox.setText("Constrain angle to " + newAngle + " degree increments"); [sstein]
        constrainIncrementalAngleCheckBox.setText(constrainAngleByStepsOf + " " + newAngle + " " + degree);
                
        updateEnabled();
    }

    private void jbInit() throws Exception
    {
        titleBorder = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));

        //**********************
        //main constraints panel
        //**********************
        
        this.setLayout(constraintsBorderLayout);
        this.add(constraintsPanel, BorderLayout.CENTER);
        constraintsPanel.setLayout(constraintsGridBagLayout);
        
        
//        constraintsPanel.add(constraintsSubPanel,
//                             new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
//                             GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
//                             new Insets(0, 0, 0, 0), 0, 0));
       
        //****************
        //length constraint
        //*****************
        lengthPanel.setBorder(lengthPanelTitle);
        lengthPanel.setLayout(lengthPanelGridBagLayout);
        lengthSubPanel.setLayout(lengthSubPanelGridBagLayout);
        lengthTextField.setText("0.5");
        lengthTextField.setHorizontalAlignment(SwingConstants.TRAILING);
        lengthCheckBox.setToolTipText("");
        lengthCheckBox.setText(constrainLengthToNearest + " ");
        lengthUnitsLabel.setText(modelUnits);
        
        constraintsPanel.add(lengthPanel,
                             new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                             GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                             new Insets(10, 10, 10, 10), 0, 0));
        
        lengthPanel.add(lengthSubPanel,
                        new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
                        new Insets(0, 0, 0, 0), 0, 0));
        
        lengthSubPanel.add(lengthCheckBox,   
                        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
                        new Insets(0, 0, 0, 0), 0, 0));
        
        lengthSubPanel.add(lengthTextField,
                        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
                        new Insets(0, 0, 0, 5), 0, 0));
        
        lengthSubPanel.add(lengthUnitsLabel, 
                        new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
                        new Insets(0, 0, 0, 0), 0, 0));
        
        //****************************
        //incremental angle constraint
        //****************************
        
        incrementalAnglePanel.setLayout(incrementalAnglePanelGridBagLayout);
        incrementalAngleSubPanel.setLayout(incrementalAngleSubPanelGridBagLayout);
        numPartsTextField.setText("8");
        constrainIncrementalAngleCheckBox.setText(ConstrainAngleTo45DegreeIncrements);
        
        constraintsPanel.add(     incrementalAnglePanel,
                                  new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                                  GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                  new Insets(10, 10, 10, 10), 0, 1));
        
        incrementalAnglePanel.add(incrementalAngleSubPanel,
                                  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                                  GridBagConstraints.WEST, GridBagConstraints.NONE, 
                                  new Insets(0, 0, 0, 0), 0, 0));
        
        incrementalAngleSubPanel.add(constrainIncrementalAngleCheckBox,
                                  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                  GridBagConstraints.WEST, GridBagConstraints.NONE, 
                                  new Insets(0, 0, 0, 0), 0, 0));
        
        incrementalAngleSubPanel.add(numPartsPanel,   
                                  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
                                  GridBagConstraints.WEST, GridBagConstraints.NONE, 
                                  new Insets(0, 0, 0, 0), 0, 0));
        
        numPartsTextField.setHorizontalAlignment(SwingConstants.TRAILING);
        numPartsTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {updateAngle();}
            public void insertUpdate(DocumentEvent e) {updateAngle();}
            public void removeUpdate(DocumentEvent e) {updateAngle();}
            private void updateAngle() {
                try {
                    double newAngle = Math.rint(3600.0 / Double.parseDouble(numPartsTextField.getText()))/10.0;
                    constrainIncrementalAngleCheckBox.setText(constrainAngleByStepsOf + " " + newAngle + " " + degree);
                } catch(java.lang.NumberFormatException e) {}
            }
        });
        numPartsPanel.setLayout(gridBagLayout4);
        numPartsPreLabel.setText(byDividing360DegreesInto);
        numPartsPostLabel.setText(parts);
        
        numPartsPanel.add(numPartsPreLabel,      
                          new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 
                          GridBagConstraints.WEST, GridBagConstraints.NONE, 
                          new Insets(0, 0, 0, 5), 0, 0));
        
        numPartsPanel.add(numPartsTextField, 
                          new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
                          GridBagConstraints.CENTER, GridBagConstraints.NONE, 
                          new Insets(0, 0, 0, 5), 0, 0));
        
        numPartsPanel.add(numPartsPostLabel, 
                          new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
                          GridBagConstraints.CENTER, GridBagConstraints.NONE, 
                          new Insets(0, 0, 0, 0), 0, 0));
        
        //****************
        //angle constraint
        //****************
        
        anglePanel.setBorder(anglePanelTitleShort);
        anglePanel.setLayout(AnglePanelGridBagLayout);
        angleDataPanel.setLayout(angleDataPanelGridBagLayout);
        
        constraintsPanel.add(anglePanel,
                             new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                             GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                             new Insets(10, 10, 10, 10), 0, 0));
        
        anglePanel.add(    angleDataPanel,
                           new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, 
                           GridBagConstraints.WEST, GridBagConstraints.NONE, 
                           new Insets(0, 0, 0, 0), 0, 0));
        
        angleDataPanel.add(constrainAngleCheckBox, 
                           new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
                           GridBagConstraints.WEST, GridBagConstraints.NONE, 
                           new Insets(0, 0, 0, 0), 0, 0));
        
        angleDataPanel.add(angleConstraintTextField,
                           new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
                           GridBagConstraints.CENTER, GridBagConstraints.NONE, 
                           new Insets(0, 0, 0, 5), 0, 0));
        
        angleDataPanel.add(relativeAngleRadioButton,
                           new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                           GridBagConstraints.WEST, GridBagConstraints.NONE,
                           new Insets(0, 0, 0, 0), 0, 0));
        
        angleDataPanel.add(relAngleImageLabel,
                           new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                           GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                           new Insets(0, 0, 0, 0), 0, 0));
        relAngleImageLabel.setIcon((new ImageIcon(getClass().getResource("relAngle.png"))));

        angleDataPanel.add(absoluteAngleRadioButton,
                           new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                           GridBagConstraints.WEST, GridBagConstraints.NONE,
                           new Insets(0, 0, 0, 0), 0, 0));
        
        angleDataPanel.add(absAngleImageLabel,
                           new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                           GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                           new Insets(0, 0, 0, 0), 0, 0));
        absAngleImageLabel.setIcon((new ImageIcon(getClass().getResource("absAngle.png"))));

        angleDataPanel.add(angleUnitsLabel, 
                           new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
                           GridBagConstraints.CENTER, GridBagConstraints.NONE, 
                           new Insets(0, 0, 0, 0), 0, 0));
        
        relativeAngleRadioButton.setText(constrainToRelativeAngle);
        absoluteAngleRadioButton.setText(constrainToAbsoluteAngle);
        angleUnitsLabel.setText(degrees);
        angleButtonGroup.add(relativeAngleRadioButton);
        angleButtonGroup.add(absoluteAngleRadioButton);
        angleConstraintTextField.setText("30");
        angleConstraintTextField.setHorizontalAlignment(SwingConstants.TRAILING);
        constrainAngleCheckBox.setText(constrainToAngle);        
   }

    public class myFocusListener extends FocusAdapter
    {
        public void focusGained(FocusEvent e)
        {
        }
        
        public void focusLost(FocusEvent e)
        {
        }
    }
}
