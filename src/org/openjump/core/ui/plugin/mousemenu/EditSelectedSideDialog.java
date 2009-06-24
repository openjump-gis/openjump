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
 
package org.openjump.core.ui.plugin.mousemenu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.NoninvertibleTransformException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openjump.core.geomutils.Arc;
import org.openjump.core.geomutils.GeoUtils;
import org.openjump.core.geomutils.MathVector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableEditReceiver;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;
import com.vividsolutions.jump.workbench.ui.SelectionManager;
import com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle;

//adapted from MultiInputDialog
public class EditSelectedSideDialog extends JDialog
{
	final static String sIsAnInvalidDouble=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.is-an-invalid-double");
	final static String sLength=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.length");
	final static String sAngle=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.angle");
	final static String sInteriorAngle=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.interior-angle");
	final static String sSide=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Side");
	final static String sChangeDirection=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Change-Direction");
	final static String sZoomToSide=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Zoom-To-Side");
	final static String sMakeSideOne=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Make-Side-One");
	final static String sFront=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Front");
	final static String sSelectedSide=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Selected-Side");
	final static String sEditSelectedSide=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Edit-Selected-Side");
	final static String sNA=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.NA");
	final static String sLengthMustBeGreaterThanZero=I18N.get("org.openjump.core.ui.plugin.mousemenu.EditSelectedSideDialog.Length-must-be-greater-than-zero");
	
    private final static int SIDEBAR_WIDTH = 150;
    OKCancelPanel okCancelPanel = new OKCancelPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JPanel outerMainPanel = new JPanel();
    
    static final int LENGTH = 0;
    static final int ANGLE = 1;
    static final int INTERIOR_ANGLE = 2;
    
    private BorderLayout borderLayout2 = new BorderLayout();
    private JPanel imagePanel = new JPanel();
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private JLabel imageLabel = new JLabel();
    private JPanel mainPanel = new JPanel();
    private GridBagLayout mainPanelGridBagLayout = new GridBagLayout();
    private JPanel innerMainPanel = new JPanel();
    private JPanel innerMainPanel2 = new JPanel();
    private GridBagLayout gridBagLayout5 = new GridBagLayout();
    private GridBagLayout gridBagLayout7 = new GridBagLayout();
    private GridBagLayout gridBagLayout6 = new GridBagLayout();
    private JTextArea descriptionTextArea = new JTextArea();
    private JPanel strutPanel = new JPanel();
    private JPanel currentMainPanel = innerMainPanel;
    private JPanel verticalSeparatorPanel = new JPanel();
    
    //edit dialog variables
    private JPanel mainEditPanel = new JPanel();
    private Border titleBorder;
    private JPanel inputPanel = new JPanel();
    private JPanel inputSubPanel = new JPanel();
    private GridBagLayout inputPanelGridBagLayout = new GridBagLayout();
    private GridBagLayout inputSubPanelGridBagLayout = new GridBagLayout();
    private TitledBorder inputPanelTitle = new TitledBorder(titleBorder, "");;
    private JCheckBox readonlyCheckBox = new JCheckBox();
    //    private JTextField sideTextField = new JTextField(3);
    private JTextField lengthTextField = new JTextField(10);
    private JTextField angleTextField = new JTextField(6);
    private JTextField interiorAngleTextField = new JTextField(6);
    private JLabel sideLabel = new JLabel();
    private JLabel lengthLabel = new JLabel();
    private JLabel angleLabel = new JLabel();
    private JLabel interiorAngleLabel = new JLabel();
    
    private JPanel buttonPanel = new JPanel();
    private JPanel buttonSubPanel = new JPanel();
    private GridBagLayout buttonPanelGridBagLayout = new GridBagLayout();
    private GridBagLayout buttonSubPanelGridBagLayout = new GridBagLayout();
    private TitledBorder buttonPanelTitle = new TitledBorder(titleBorder, "");;
    private JButton changeDirectionButton = new JButton();
    private JButton makeSideOneButton = new JButton();
    private JCheckBox zoomToSideCheckBox = new JCheckBox();
//    private JButton zoomToSideButton = new JButton();
//    private JButton zoomToShapeButton = new JButton();
    private JButton selectedButton;
    
    private PlugInContext context;
    private Layer selectedSideLayer;
    private Layer editLayer;
    private Layer activeLayer;
    private Collection selectedFeatures;
    private Feature selectedFeature;
    private Geometry selectedGeo;
    public Geometry ghostGeo;
    private Coordinate[] ghostCoords;
    private Arc angleArc;
    private Feature arrowFeature;
    private int direction;
    private JSpinner sideSpinner;
    private DecimalFormat df2 = new DecimalFormat("##0.0#");
    private DecimalFormat df3 = new DecimalFormat("###,###,##0.0##");
    private int currSide;
    private boolean isClockwise;
    private boolean isLineString;
    private boolean hasPendingEdits; //if true then user has done some editing and has not applied them

    /**
     * @param context the context on which to make this dialog modal and centred
     */
    public EditSelectedSideDialog(PlugInContext context, String title, boolean modal)
    {
        super(context.getWorkbenchFrame(), title, modal);
        this.context = context;
        try
        {
            verticalSeparatorPanel.setBackground(Color.black);
            okCancelPanel.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    okCancelPanel_actionPerformed(e);
                }
            });
            this.addComponentListener(new java.awt.event.ComponentAdapter()
            {
                public void componentShown(ComponentEvent e)
                {
                    this_componentShown(e);
                }
            });
            this.setResizable(true);
            this.getContentPane().setLayout(borderLayout2);
            this.getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
            this.getContentPane().add(outerMainPanel, BorderLayout.CENTER);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        inputPanel.setBorder(inputPanelTitle);
        inputPanel.setLayout(inputPanelGridBagLayout);
        inputSubPanel.setLayout(inputSubPanelGridBagLayout);
        
        sideLabel.setText(sSide + ": ");
        SpinnerModel sideSpinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
        sideSpinner = new JSpinner(sideSpinnerModel);
        sideSpinner.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JSpinner spinner = (JSpinner)e.getSource();
                currSide = ((Integer)spinner.getValue()).intValue();
                
                if (currSide == 0)
                {
                    currSide = ghostCoords.length - 1;
                    spinner.setValue(new Integer(ghostCoords.length - 1));
                }
                
                if (currSide == ghostCoords.length)
                {
                    currSide = 1;
                    spinner.setValue(new Integer(1));
                }
                
                changeSide();
                if (zoomToSideCheckBox.isSelected()) zoomToSide();
            }
        });
        
        
        lengthLabel.setText(sLength + ": ");
        lengthTextField.setText("999999.000");
        lengthTextField.setHorizontalAlignment(SwingConstants.LEADING);
        
        angleLabel.setText(sAngle + ": ");
        angleTextField.setText("000.00");
        angleTextField.setHorizontalAlignment(SwingConstants.LEADING);
        
        interiorAngleLabel.setText(sInteriorAngle + ": ");
        interiorAngleTextField.setText("000.00");
        interiorAngleTextField.setHorizontalAlignment(SwingConstants.LEADING);
               
        lengthTextField.addFocusListener(new FocusListener()
        {   //need these to handle tab key; will also handle click in/out
            public void focusGained(FocusEvent e)
            {
            }
            
            public void focusLost(FocusEvent e)
            {
                updateGeo(LENGTH);
            }
        });
        
        angleTextField.addFocusListener(new FocusListener()
        {   //need these to handle tab key; will also handle click in/out
            public void focusGained(FocusEvent e)
            {
            }
            
            public void focusLost(FocusEvent e)
            {
                updateGeo(ANGLE);
            }
        });
        
        interiorAngleTextField.addFocusListener(new FocusListener()
        {   //need these to handle tab key; will also handle click in/out
            public void focusGained(FocusEvent e)
            {
            }
            
            public void focusLost(FocusEvent e)
            {
                updateGeo(INTERIOR_ANGLE);
            }
        });
        
        inputPanel.add(inputSubPanel,
        new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
        
        inputSubPanel.add(sideLabel,
        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        inputSubPanel.add(sideSpinner,
        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 5), 0, 0));
        
        inputSubPanel.add(lengthLabel,
        new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        inputSubPanel.add(lengthTextField,
        new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 5), 0, 0));
        
        inputSubPanel.add(angleLabel,
        new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        inputSubPanel.add(angleTextField,
        new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 5), 0, 0));
        
        inputSubPanel.add(interiorAngleLabel,
        new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        inputSubPanel.add(interiorAngleTextField,
        new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 5), 0, 0));
        
        buttonPanel.setBorder(buttonPanelTitle);
        buttonPanel.setLayout(buttonPanelGridBagLayout);
        buttonSubPanel.setLayout(buttonSubPanelGridBagLayout);
        
        changeDirectionButton.setText(sChangeDirection);
        makeSideOneButton.setText(sMakeSideOne + " (" + sFront + ")");
        zoomToSideCheckBox.setText(sZoomToSide);
//        zoomToSideButton.setText("Zoom To Side");
//        zoomToShapeButton.setText("Zoom To Shape");
        
        changeDirectionButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectedButton = changeDirectionButton;
                changeDirection();
            }
        });
        
        makeSideOneButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectedButton = makeSideOneButton;
                makeSideOne();
            }
        });
        
        zoomToSideCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (zoomToSideCheckBox.isSelected()) zoomToSide();
            }
        });
        
//        zoomToSideButton.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                selectedButton = zoomToSideButton;
//                zoomToSide();
//            }
//        });
//        
//        zoomToShapeButton.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                selectedButton = zoomToSideButton;
//                zoomToShape();
//            }
//        });
        
        buttonPanel.add(buttonSubPanel,
        new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
        
        buttonSubPanel.add(changeDirectionButton,
        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        buttonSubPanel.add(makeSideOneButton,
        new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
 
        buttonSubPanel.add(zoomToSideCheckBox,
        new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
//        buttonSubPanel.add(zoomToSideButton,
//        new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
//        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//        new Insets(0, 0, 0, 0), 0, 0));
//        
//        buttonSubPanel.add(zoomToShapeButton,
//        new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
//        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//        new Insets(0, 0, 0, 0), 0, 0));
        
        mainEditPanel.add(inputPanel,
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        mainEditPanel.add(buttonPanel,
        new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
        
        this.getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
        this.getContentPane().add(mainEditPanel, BorderLayout.CENTER);
        
        init();
    }
    
    public EditSelectedSideDialog()
    {
        this(null, "", false);
    }
    
    private String removeGroupingSeparators(String numberString) {
    	char comma = df3.getDecimalFormatSymbols().getGroupingSeparator();
    	return numberString.replaceAll(""+comma, "");
    }

    public void init()//(PlugInContext context)//, Layer selectedSideLayer, Layer editLayer, Layer activeLayer, Collection selectedFeatures)//, ArrayList transactions, EditTransaction transaction)
    {
        SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
        Collection selectedLayers = selectionManager.getLayersWithSelectedItems();
        activeLayer = (Layer)selectedLayers.iterator().next();
        selectedFeatures = selectionManager.getFeaturesWithSelectedItems();
        selectedFeature = (Feature) selectedFeatures.iterator().next();
        Collection selectedItems = selectionManager.getSelectedItems(activeLayer);
        selectedGeo = (Geometry) selectedItems.iterator().next();
        
        //set up the selected side layer
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        FeatureDataset featureDataset = new FeatureDataset(featureSchema);
        
        Collection selectedCategories = context.getLayerNamePanel()
        .getSelectedCategories();
        selectedSideLayer = context.addLayer(selectedCategories.isEmpty()
        ? StandardCategoryNames.WORKING
        : selectedCategories.iterator().next().toString(), sSelectedSide,
        featureDataset);
        
        selectedSideLayer.setFeatureCollectionModified(false).setEditable(true);
        selectedSideLayer.addStyle(new ArrowLineStringEndpointStyle.SolidEnd());
        selectedSideLayer.getBasicStyle().setLineColor(Color.red);
        
        selectedSideLayer.getLayerManager().addLayerListener(new LayerListener()
        {
            public void featuresChanged(FeatureEvent e){}
            public void categoryChanged(CategoryEvent e){}
            public void layerChanged(LayerEvent e){
                if (e.getType() == LayerEventType.METADATA_CHANGED) 
                    selectedSideLayer.setEditable(false);}
        });        
        
        //set up the edit layer
        FeatureSchema featureSchema2 = new FeatureSchema();
        featureSchema2.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        FeatureDataset featureDataset2 = new FeatureDataset(featureSchema2);
        
        editLayer = context.addLayer(selectedCategories.isEmpty()
        ? StandardCategoryNames.WORKING
        //: selectedCategories.iterator().next().toString(), "Edit", //[sstein 19.10.2005] hope that works 
        : selectedCategories.iterator().next().toString(), MenuNames.EDIT,
        featureDataset2);
        editLayer.getBasicStyle().setLineColor(Color.red);
        
        editLayer.getLayerManager().addLayerListener(new LayerListener()
        {
            public void featuresChanged(FeatureEvent e){}
            public void categoryChanged(CategoryEvent e){}
            public void layerChanged(LayerEvent e){
                if (e.getType() == LayerEventType.METADATA_CHANGED) 
                    editLayer.setEditable(false);}
        });
        
//        selectionManager.clear();
        final ArrayList transactions = new ArrayList();
        
        EditTransaction transaction = new EditTransaction(selectedFeatures, getName(), activeLayer, false, false, context.getWorkbenchContext().getLayerViewPanel());
        selectedGeo = selectedFeature.getGeometry();
        direction = 0;
        isClockwise = new GeoUtils().clockwise(selectedGeo);
        hasPendingEdits = false;
        Coordinate[] geoCoords = selectedGeo.getCoordinates();
        ghostCoords = new Coordinate[geoCoords.length];

        for (int i = 0; i < ghostCoords.length; i++)
        {
            ghostCoords[i] = (Coordinate) geoCoords[i].clone();
        }
        
        Coordinate clickPoint = new Coordinate(0,0,0);
        
        try
        {
            clickPoint = context.getLayerViewPanel().getViewport().toModelCoordinate(context.getLayerViewPanel().getLastClickedPoint());
        }
        catch (NoninvertibleTransformException e)
        {   
        }
        
        double distToSide = new GeoUtils().getDistance(clickPoint, (Coordinate) ghostCoords[0], (Coordinate) ghostCoords[1]);
        currSide = 1;
        
        for (int i = 1; i < ghostCoords.length - 1; i++)
        {
            double currDistToSide = new GeoUtils().getDistance(clickPoint, (Coordinate) ghostCoords[i], (Coordinate) ghostCoords[i+1]); 
            if (currDistToSide < distToSide)
            {
                distToSide = currDistToSide;
                currSide = i + 1;
            }
        }
        
        sideSpinner.setModel(new SpinnerNumberModel(currSide, 0, ghostCoords.length, 1));
        
        //set up ghost shape
        FeatureSchema fs2 = editLayer.getFeatureCollectionWrapper().getFeatureSchema();
        Feature feature2 = new BasicFeature(fs2);
        
        if ((selectedGeo instanceof Polygon) || (selectedGeo instanceof LinearRing))
        {
            isLineString = false;
            ghostGeo = new GeometryFactory().createLinearRing(ghostCoords);
        }
        else
        {
            isLineString = true;
            ghostGeo = new GeometryFactory().createLineString(ghostCoords);
            makeSideOneButton.setEnabled(false);
        }
        
        ghostCoords = ghostGeo.getCoordinates();
        feature2.setGeometry(ghostGeo);
        editLayer.getFeatureCollectionWrapper().add(feature2);
        
        //set up the edit arrow
        FeatureSchema fs = selectedSideLayer.getFeatureCollectionWrapper().getFeatureSchema();
        arrowFeature = new BasicFeature(fs);
        arrowFeature.setGeometry(getArcArrow());
        selectedSideLayer.getFeatureCollectionWrapper().add(arrowFeature);
        changeSide();
    }
    
    public void setVisible(boolean visible)
    {
        //Workaround for Java bug  4446522 " JTextArea.getPreferredSize()
        //incorrect when line wrapping is used": call #pack twice [Jon Aquino]
        pack();
        pack();
        GUIUtil.centreOnWindow(EditSelectedSideDialog.this);
        super.setVisible(visible);
    }
    
    protected Coordinate[] toArray(List coordinates)
    {
        return (Coordinate[]) coordinates.toArray(new Coordinate[]
        {  });
    }
    
    public boolean wasOKPressed()
    {
        return okCancelPanel.wasOKPressed();
    }
    //Experience suggests that one should avoid using weights when using the
    //GridBagLayout. I find that nonzero weights can cause layout bugs that are
    //hard to track down. [Jon Aquino]
//    void jbInit() throws Exception
//    {
//        verticalSeparatorPanel.setBackground(Color.black);
//        
//        okCancelPanel.addActionListener(new java.awt.event.ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                okCancelPanel_actionPerformed(e);
//            }
//        });
//        
//        this.addComponentListener(new java.awt.event.ComponentAdapter()
//        {
//            public void componentShown(ComponentEvent e)
//            {
//                this_componentShown(e);
//            }
//        });
//        this.setResizable(false);
//        this.getContentPane().setLayout(borderLayout2);
//        this.getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
//        this.getContentPane().add(outerMainPanel, BorderLayout.CENTER);
//    }
    
    void okCancelPanel_actionPerformed(ActionEvent e)
    {
        if (okCancelPanel.wasOKPressed())
        {
            if (hasPendingEdits)
            {
//                SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
//                Collection selectedLayers = selectionManager.getLayersWithSelectedItems();
//                Layer activeLayer = (Layer)selectedLayers.iterator().next();
//                Collection selectedFeatures = selectionManager.getFeaturesWithSelectedItems();
//                Feature selectedFeature = (Feature) selectedFeatures.iterator().next();
                
                UndoableEditReceiver undoableEditReceiver =
                context.getWorkbenchContext().getLayerManager() != null
                ? context.getWorkbenchContext()
                .getLayerManager()
                .getUndoableEditReceiver()
                : null;
                
                if (undoableEditReceiver != null)
                {
                    undoableEditReceiver.startReceiving();
                }
                
                if (selectedGeo instanceof Polygon)
                    ghostGeo =  new GeometryFactory().createPolygon((LinearRing)ghostGeo, null);
                
                EditTransaction transaction = new EditTransaction(selectedFeatures, sEditSelectedSide,
                activeLayer, false, false,
                context.getWorkbenchContext().getLayerViewPanel());
                transaction.setGeometry(selectedFeature, ghostGeo);
                transaction.commit();
                
                if (undoableEditReceiver != null)
                {
                    undoableEditReceiver.stopReceiving();
                }
            }
        }
        setVisible(false);
        selectedSideLayer.getLayerManager().remove(selectedSideLayer);
        editLayer.getLayerManager().remove(editLayer);
        return;
    }

    void this_componentShown(ComponentEvent e)
    {
        okCancelPanel.setOKPressed(false);
    }
    
    private void reportValidationError(String errorMessage)
    {
        JOptionPane.showMessageDialog(
        this,
        errorMessage,
        "JUMP",
        JOptionPane.ERROR_MESSAGE);
    }
    
    void changeDirection()
    {
        direction = 1 - direction;
        changeSide();
    };
    
    void makeSideOne()
    {
        if (currSide != 1)
        {
            Coordinate[] tempCoords = new Coordinate[ghostCoords.length];
            
            for (int i = 0; i < ghostCoords.length; i++)
            {
                tempCoords[i] = (Coordinate) ghostCoords[i].clone();
            }
            
            int index = 0;
            
            for (int i = currSide-1; i < ghostCoords.length; i++)
            {
                ghostCoords[index] = tempCoords[i];
                index++;
            }
            
            for (int i = 1; i < currSide; i++)
            {
                ghostCoords[index] = tempCoords[i];
                index++;
            }
            
            ghostGeo = new GeometryFactory().createLinearRing(ghostCoords);
            sideSpinner.setModel(new SpinnerNumberModel(1, 0, ghostCoords.length, 1));
            hasPendingEdits = true;
            currSide = 1;
        }
        return;
  };

    void zoomToSide()
    {
        Envelope sideEnv = getArcArrow().getEnvelopeInternal();
        double width = sideEnv.getWidth() / 10.0;
        double height = sideEnv.getHeight() / 10.0;
        double extraOnRight = sideEnv.getMaxX() + width;
        double extraOnLeft = sideEnv.getMinX() - width;
        double extraOnTop = sideEnv.getMaxY() + height;
        double extraOnBot = sideEnv.getMinY() - height;
        sideEnv.expandToInclude(extraOnRight, extraOnTop);
        sideEnv.expandToInclude(extraOnLeft, extraOnBot);
        
        try
        {
            context.getWorkbenchContext().getLayerViewPanel().getViewport().zoom(sideEnv);
        }
        catch (NoninvertibleTransformException e)
        {
        }
        
        changeSide();
        return;
    };
    
    void zoomToShape()
    {
        
        Envelope shapeEnv = ghostGeo.getEnvelopeInternal();
        shapeEnv.expandToInclude(selectedGeo.getEnvelopeInternal());
        double width = shapeEnv.getWidth() / 10.0;
        double height = shapeEnv.getHeight() / 10.0;
        double extraOnRight = shapeEnv.getMaxX() + width;
        double extraOnLeft = shapeEnv.getMinX() - width;
        double extraOnTop = shapeEnv.getMaxY() + height;
        double extraOnBot = shapeEnv.getMinY() - height;
        shapeEnv.expandToInclude(extraOnRight, extraOnTop);
        shapeEnv.expandToInclude(extraOnLeft, extraOnBot);
        
        try
        {
            context.getWorkbenchContext().getLayerViewPanel().getViewport().zoom(shapeEnv);
        }
        catch (NoninvertibleTransformException e)
        {
        }
        
        changeSide();
        return;
    };

    private void changeSide()
    {
        Coordinate startPt = ghostCoords[getStartIndex()];
        Coordinate endPt = ghostCoords[getEndIndex()];
        Coordinate prevPt = ghostCoords[getPrevIndex()]; //point prior to startPt
        lengthTextField.setText(df3.format(startPt.distance(endPt)));
        angleTextField.setText(df2.format(new GeoUtils().getBearing180(startPt, endPt)));
        
        if (prevPt.equals2D(startPt))
        {
            interiorAngleTextField.setEnabled(false);
            interiorAngleTextField.setText(sNA);
        }
        else
        {
            interiorAngleTextField.setEnabled(true);
            interiorAngleTextField.setText(df2.format(getInteriorAngle(prevPt, startPt, endPt)));
        }
        
        selectedSideLayer.getFeatureCollectionWrapper().remove(arrowFeature);
        FeatureSchema fs = selectedSideLayer.getFeatureCollectionWrapper().getFeatureSchema();
        arrowFeature = new BasicFeature(fs);
        arrowFeature.setGeometry(getArcArrow());
        selectedSideLayer.getFeatureCollectionWrapper().add(arrowFeature);
        selectedSideLayer.fireAppearanceChanged();
    }
    
    private LineString getArcArrow()
    {
        Coordinate startPt = ghostCoords[getStartIndex()];
        Coordinate endPt = ghostCoords[getEndIndex()];
        Coordinate prevPt = ghostCoords[getPrevIndex()]; //point prior to startPt
        CoordinateList arcCoords = new CoordinateList();
        
        if (prevPt.equals2D(startPt))
        {
            arcCoords.add(startPt);
        }
        else
        {
            double scale = context.getWorkbenchContext().getLayerViewPanel().getViewport().getScale();
            double arcAngle = getInteriorAngle(prevPt, startPt, endPt);
            Coordinate startArcPt = new GeoUtils().along(15/scale, startPt, prevPt);;
            boolean cw = isClockwise;
            if (direction == 1) cw = !cw;
            if (cw) arcAngle = -arcAngle;
            angleArc = new Arc(startPt, startArcPt, arcAngle);
            arcCoords = angleArc.getCoordinates();
        }
        
        arcCoords.add(endPt);
        return new GeometryFactory().createLineString(arcCoords.toCoordinateArray());
    }
    
    private double getInteriorAngle(Coordinate prevPt, Coordinate startPt, Coordinate endPt)
    {
        if (prevPt.equals2D(startPt))
        {
            return 0.0;
        }
        else
        {
            MathVector v1 = (new MathVector(prevPt)).vectorBetween(new MathVector(startPt));
            MathVector v2 = (new MathVector(startPt)).vectorBetween(new MathVector(endPt));
            boolean toRight = new GeoUtils().pointToRight(endPt, prevPt, startPt);
            boolean cw = isClockwise;
            if (direction == 1) cw = !cw;
            double angle = v1.angleDeg(v2);
            
            if ((cw && toRight) || (!cw && !toRight))
                angle = 180 - angle;
            else
                angle = 180 + angle;
            
            return angle;
        }
    }
    
    private void updateGeo(int from)
    {
        switch (from)
        {
            case LENGTH:
            {
                try
                {
                    double length = Double.parseDouble(removeGroupingSeparators(lengthTextField.getText().trim()));
                    if (length <= 0)
                    {
                        reportValidationError(sLengthMustBeGreaterThanZero);
                    }
                    else
                    {
                        Coordinate startPt = ghostCoords[getStartIndex()];
                        Coordinate endPt = ghostCoords[getEndIndex()];
                        Coordinate newPt = new GeoUtils().along(length, startPt, endPt);
                        replaceVertex(getEndIndex(), newPt);
                    }
                }
                catch (NumberFormatException e)
                {
                    reportValidationError(
                    "\""
                    + lengthTextField.getText().trim()
                    + "\" " + sIsAnInvalidDouble + " ("
                    + sLength
                    + ")");
                }
                break;
            }
            case ANGLE:
            {
                try
                {
                    double angle = Double.parseDouble(angleTextField.getText().trim());
                    Coordinate startPt = ghostCoords[getStartIndex()];
                    Coordinate endPt = ghostCoords[getEndIndex()];
                    double length = startPt.distance(endPt);
                    Coordinate newPt = (Coordinate) startPt.clone();
                    newPt.x += length;
                    newPt = new GeoUtils().rotPt(newPt, startPt, -angle);
                    replaceVertex(getEndIndex(), newPt);
                }
                catch (NumberFormatException e)
                {
                    reportValidationError(
                    "\""
                    + angleTextField.getText().trim()
                    + "\" " + sIsAnInvalidDouble + " ("
                    + sAngle
                    + ")");
                }
                break;
            }
            case INTERIOR_ANGLE:
            {
                try
                {
                    Coordinate prevPt = ghostCoords[getPrevIndex()];
                    Coordinate startPt = ghostCoords[getStartIndex()];
                    Coordinate endPt = ghostCoords[getEndIndex()];
                    double angle = Double.parseDouble(interiorAngleTextField.getText().trim());
                    double length = startPt.distance(endPt);
                    boolean cw = isClockwise;
                    if (direction == 1) cw = !cw;
                    if (cw) angle = -angle;
                    Coordinate newPt = new GeoUtils().rotPt(prevPt, startPt, angle);
                    newPt = new GeoUtils().along(length, startPt, newPt);
                    replaceVertex(getEndIndex(), newPt);
                }
                catch (NumberFormatException e)
                {
                    reportValidationError(
                    "\""
                    + interiorAngleTextField.getText().trim()
                    + "\" " + sIsAnInvalidDouble + " ("
                    + sInteriorAngle
                    + ")");
                }
                break;
            }
        }
        changeSide();
        editLayer.fireAppearanceChanged();
    }
    
    private void replaceVertex(int index, Coordinate coord)
    {
        ghostCoords[index].x = coord.x;
        ghostCoords[index].y = coord.y;
        
        if (!isLineString)
        {
            if (index == 0)
            {
                ghostCoords[ghostCoords.length - 1].x = coord.x;
                ghostCoords[ghostCoords.length - 1].y = coord.y;
            }
            
            if (index == ghostCoords.length - 1)
            {
                ghostCoords[0].x = coord.x;
                ghostCoords[0].y = coord.y;
            }
        }
        ghostGeo.geometryChanged();
        hasPendingEdits = true;
    }
    
    private int getStartIndex()
    {
        if (direction == 0) 
            return currSide - 1;
        else 
            return currSide;
    }
    
    private int getEndIndex()
    {
        if (direction == 0) 
            return currSide;
        else 
            return currSide - 1;
    }
    
    private int getPrevIndex()
    {
        int prevIndex = currSide - 2;
        int nextIndex = currSide + 1;
        
        if (isLineString)
        {
            if (prevIndex < 0) prevIndex = 0;
            if (nextIndex >= ghostCoords.length) nextIndex = ghostCoords.length - 1;
        }
        else
        {
            if (prevIndex < 0) prevIndex = ghostCoords.length - 2;
            if (nextIndex >= ghostCoords.length) nextIndex = 1;
            
        }
        
        if (direction == 1) prevIndex = nextIndex;
        return prevIndex;
    }
    
    private int getNextIndex()
    {
        int prevIndex = currSide - 2;
        int nextIndex = currSide + 1;
        
        if (isLineString)
        {
            if (prevIndex < 0) prevIndex = 0;
            if (nextIndex >= ghostCoords.length) nextIndex = ghostCoords.length - 1;
        }
        else
        {
            if (prevIndex < 0) prevIndex = ghostCoords.length - 2;
            if (nextIndex >= ghostCoords.length) nextIndex = 1;
            
        }
        
        if (direction == 1) nextIndex = prevIndex;
        return nextIndex;
    }
    
    private boolean goodDouble(String fieldName)
    {
        try
        {
            Double.parseDouble(lengthTextField.getText().trim());
            return true;
        }
        catch (NumberFormatException e)
        {
            reportValidationError(
            "\""
            + lengthTextField.getText().trim()
            + "\" " + sIsAnInvalidDouble + " ("
            + fieldName
            + ")");
            return false;
        }
    }
}
