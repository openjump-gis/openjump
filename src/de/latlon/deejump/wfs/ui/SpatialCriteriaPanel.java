/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.ui;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.deegree.datatypes.QualifiedName;
import org.deegree.model.crs.CRSFactory;
import org.deegree.model.crs.UnknownCRSException;
import org.deegree.model.filterencoding.OperationDefines;
import org.deegree.model.filterencoding.PropertyName;
import org.deegree.model.filterencoding.SpatialOperation;
import org.deegree.model.spatialschema.Geometry;
import org.deegree.model.spatialschema.GeometryImpl;

import de.latlon.deejump.wfs.i18n.I18N;

/**
 * This panel provides a user interface to spatial filter operations.<p/> Original design: Poth
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * 
 */
class SpatialCriteriaPanel extends JPanel {

    private static final long serialVersionUID = 7173251547630376008L;

    /** Operation names as defined by the OGC */
    public static final String[] OPERATION_NAMES = new String[] { "Intersects", "Within", "DWithin", "Contains",
                                                                 "Beyond", "Touches", "Crosses", "Overlaps", "Equals",
                                                                 "Disjoint", };

    /** A Distance input filed for the DWithin operation */
    private DistanceInputField dWithinDistanceField = new DistanceInputField();

    /** A Distance input filed for the Beyond operation */
    private DistanceInputField beyondDistanceField = new DistanceInputField();

    /** The currently selected operation */
    String selectedOperation = "Intersects";

    /** The parent dialog. Keep this reference to make matters simple */
    private WFSPanel wfsPanel;

    private JComboBox geomPropsCombo;

    private JComboBox srsCombo;

    private AbstractButton[] opButtons;

    /**
     * Create a SpatialResearchPanel.
     * 
     * @param panel
     */
    public SpatialCriteriaPanel( WFSPanel panel ) {
        super();
        this.wfsPanel = panel;
        initGUI();
    }

    /** Initialize the GUI */
    private void initGUI() {

        // setLayout( null );
        LayoutManager lm = new BoxLayout( this, BoxLayout.Y_AXIS );
        setLayout( lm );

        add( createGeomPropCombo() );
        add( createSRSCombo() );

        add( createOperationButtons() );
    }

    private JComponent createGeomPropCombo() {

        String[] gg = new String[0];

        geomPropsCombo = new JComboBox( gg );

        JPanel p = new JPanel();
        p.add( new JLabel( I18N.get( "SpatialResearchPanel.geometryName" ) ) );
        p.add( geomPropsCombo );
        add( p );
        return p;
    }

    private JComponent createSRSCombo() {

        srsCombo = new ExtensibleComboBox( new String[0] );

        JPanel p = new JPanel();
        p.add( new JLabel( I18N.get( "SpatialResearchPanel.srs" ) ) );
        p.add( srsCombo );
        add( p );
        return p;
    }

    /**
     * @param crs
     */
    public void setCrs( String[] crs ) {
        this.srsCombo.setModel( new DefaultComboBoxModel( crs ) );
        srsCombo.setEnabled( true );
    }

    /** Creates a panel containing the radio buttons representaing the spatial operations */
    private JComponent createOperationButtons() {
        JPanel b = new JPanel();
        LayoutManager lm = new BoxLayout( this, BoxLayout.Y_AXIS );
        setLayout( lm );

        JPanel opsPanel = new JPanel();
        JPanel opsFieldPanel = new JPanel();
        opsPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 0 ) );
        opsFieldPanel.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 10 ) );

        LayoutManager lm2 = new GridLayout( OPERATION_NAMES.length, 1 );
        opsPanel.setLayout( lm2 );
        opsFieldPanel.setLayout( lm2 );

        ActionListener bal = new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                JRadioButton rb = (JRadioButton) e.getSource();
                selectedOperation = rb.getActionCommand();
            }
        };

        ButtonGroup bg = new ButtonGroup();

        opButtons = new JRadioButton[OPERATION_NAMES.length];
        String simpleName = getClass().getSimpleName();
        for ( int i = 0; i < OPERATION_NAMES.length; i++ ) {
            String txt = simpleName + "." + OPERATION_NAMES[i];
            String buttonTxt = I18N.get( txt );
            opButtons[i] = new JRadioButton( buttonTxt );
            opButtons[i].setBorder(null);
            txt += ".descrip";
            buttonTxt = I18N.get( txt );
            opButtons[i].setToolTipText( buttonTxt );
            opButtons[i].setActionCommand( OPERATION_NAMES[i] );
            opButtons[i].addActionListener( bal );
//            opButtons[i].setBounds( PropertyCriteriaPanel.LEFT_MARGIN + 10, ( i * 25 ) + 25, 270, 10 );
            opButtons[i].setAlignmentX( Component.LEFT_ALIGNMENT );

            bg.add( opButtons[i] );

            if ( "DWithin".equals( OPERATION_NAMES[i] ) ) {
                opsFieldPanel.add( dWithinDistanceField );
            } else if ( "Beyond".equals( OPERATION_NAMES[i] ) ) {
//                beyondDistanceField.setEnabled( false );
                opsFieldPanel.add( beyondDistanceField );
            } else {
                opsFieldPanel.add( Box.createHorizontalStrut( 1 ) );
            }

            opsPanel.add( opButtons[i] );

        }

        opButtons[0].doClick();
        JPanel combiPanel = new JPanel();
        combiPanel.setBorder( BorderFactory.createTitledBorder( I18N.get( "SpatialResearchPanel.spatialOperation" ) ) );
        combiPanel.setLayout( new GridLayout( 1, 2 ) );
        combiPanel.add( opsPanel );
        combiPanel.add( opsFieldPanel );

        b.add( combiPanel );

        return b;
    }

    /**
     * Returns the XML fragment conating the spatial operation
     * 
     * @return the XML description of the spatial operation
     */
    public StringBuffer getXmlElement() {

        int opType = OperationDefines.getIdByName( selectedOperation );

        double dist = 0;
        if ( "DWithin".equals( selectedOperation ) ) {
            dist = dWithinDistanceField.getDistance();
        } else if ( "Beyond".equals( selectedOperation ) ) {
            dist = beyondDistanceField.getDistance();
        }
        StringBuffer sb = new StringBuffer();
        Geometry geometry = wfsPanel.getSelectedGeometry();
        if ( geometry == null ) {
            return sb;
        }
        try {
            ( (GeometryImpl) geometry ).setCoordinateSystem( CRSFactory.create( (String) this.srsCombo.getSelectedItem() ) );
        } catch ( UnknownCRSException e ) {
            e.printStackTrace();
        }

        QualifiedName qn = this.wfsPanel.getFeatureType();
        QualifiedName geoQn = (QualifiedName) geomPropsCombo.getSelectedItem();

        geoQn = new QualifiedName( qn.getPrefix(), geoQn.getLocalName(), qn.getNamespace() );
        SpatialOperation spatialOp = new SpatialOperation( opType, new PropertyName( geoQn ), geometry, dist );

        sb = spatialOp.toXML();

        return sb;
    }

    /**
     * A conveniece class containing a text field for value input and a label. This class is used
     * for the operations DWithin and Beyond.
     */
    class DistanceInputField extends JPanel {

        private static final long serialVersionUID = 6119874124692251085L;

        JFormattedTextField distanceField;

        double distance = 0d;

        DistanceInputField() {
            super();
            distanceField = new JFormattedTextField( new Float( 0.0 ) );
            distanceField.addPropertyChangeListener( "value", new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    distance = ( (Number) distanceField.getValue() ).doubleValue();
                }
            } );

            distanceField.setColumns( 5 );
            add( distanceField );
            add( new JLabel( "m" ) );

        }

        /**
         * @return the distance
         */
        public double getDistance() {
            return distance;
        }

        @Override
        public void setEnabled( boolean enabled ) {
            distanceField.setEnabled( enabled );
        }
    }

    /**
     * @param geometryProperties
     */
    public void resetGeoCombo( QualifiedName[] geometryProperties ) {
        this.geomPropsCombo.removeAllItems();
        if ( geometryProperties != null ) {
            for ( int i = 0; i < geometryProperties.length; i++ ) {
                this.geomPropsCombo.addItem( geometryProperties[i] );
            }
        }
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        if ( !enabled ) {
            resetGeoCombo( new QualifiedName[0] );
            setCrs( new String[0] );
        }
        this.srsCombo.setEnabled( enabled );
        this.geomPropsCombo.setEnabled( enabled );
        for ( AbstractButton b : opButtons ) {
            b.setEnabled( enabled );
        }
    }

}
