/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deegree.datatypes.QualifiedName;
import org.deegree.model.feature.schema.FeatureType;
import org.deegree.model.feature.schema.GMLSchema;
import org.deegree.model.feature.schema.PropertyType;

import de.latlon.deejump.wfs.i18n.I18N;

/**
 * ...
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version 2.0, $Revision: 1516 $, $Date: 2008-07-17 11:32:16 +0200 (Do, 17 Jul 2008) $
 * 
 * @since 2.0
 */

public class PropertySelectionPanel extends JPanel {

    private static final long serialVersionUID = 2886180413810632383L;

    private WFSPanel parentDialog;

    protected JList propertiesList;

    protected JComboBox geoPropsCombo;

    /**
     * @param parentDialog
     */
    public PropertySelectionPanel( WFSPanel parentDialog ) {
        super(new GridBagLayout());
        this.parentDialog = parentDialog;
        initGUI();
    }

    private void initGUI() {

        JPanel p = new JPanel();
        p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
        p.setBorder( BorderFactory.createTitledBorder( I18N.get( "PropertySelectionPanel.downloadProps" ) ) );

        propertiesList = new JList();
        JScrollPane scrollPane = new JScrollPane( propertiesList );

        p.add( scrollPane );

        p.add(Box.createRigidArea(new Dimension(5,5)));
        
        geoPropsCombo = new JComboBox();

        geoPropsCombo.setBorder( BorderFactory.createTitledBorder( I18N.get( "SpatialResearchPanel.geometryName" ) ) );

        p.add( geoPropsCombo );

        add(p, new GridBagConstraints(0, 0, 1, 1, 0.8, 0.8,
            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
            new Insets(10,10,10,10), 0, 0));
    }

    /**
     * @param simpleProps
     * @param geoProps
     */
    public void setProperties( String[] simpleProps, QualifiedName[] geoProps ) {

        resetPropsList( simpleProps );
        resetGeoCombo( geoProps );
    }

    private void resetGeoCombo( QualifiedName[] geoProps ) {
        geoPropsCombo.removeAllItems();
        if ( geoProps != null ) {
            for ( int i = 0; i < geoProps.length; i++ ) {

                if ( i == 0 ) {
                    this.parentDialog.setGeoProperty( geoProps[i] );
                }
                geoPropsCombo.addItem( geoProps[i] );
            }
        }
    }

    private void resetPropsList( String[] props ) {
        propertiesList.removeAll();
        DefaultListModel listModel = new DefaultListModel();
        int[] selIndices = new int[props.length];
        for ( int i = 0; i < props.length; i++ ) {
            listModel.addElement( props[i] );
            selIndices[i] = i;
        }
        propertiesList.setModel( listModel );
        propertiesList.setSelectedIndices( selIndices );

    }

    /**
     * @return not sure, part of the internal 'XML' processing
     */
    public StringBuffer getXmlElement() {

        StringBuffer sb = new StringBuffer( 5000 );

        QualifiedName ftQualiName = parentDialog.getFeatureType();

        GMLSchema schema = parentDialog.getWfService().getSchemaForFeatureType( ftQualiName.getPrefixedName() );

        if ( schema == null ) {
            return sb;
        }
        FeatureType[] featTypes = schema.getFeatureTypes();

        if ( featTypes.length < 1 ) {
            throw new RuntimeException( "Schema doesn't define any FeatureType. Must have at least one." );
        }

        // put what's been chosen in a list
        Object[] objs = propertiesList.getSelectedValues();
        List<Object> chosenProps = Arrays.asList( objs );

        // and loop over the correct order, seing what's in the list
        PropertyType[] featProperties = featTypes[0].getProperties();
        for ( int i = 0; i < featProperties.length; i++ ) {
            if ( chosenProps.contains( featProperties[i].getName().getLocalName() ) ) {
                sb.append( "<wfs:PropertyName>" );
                if ( ftQualiName.getPrefix() != null && ftQualiName.getPrefix().length() > 0 ) {
                    sb.append( ftQualiName.getPrefix() ).append( ":" );
                }
                sb.append( featProperties[i].getName().getLocalName() ).append( "</wfs:PropertyName>" );
            }

            // geom prop
            QualifiedName qn = (QualifiedName) geoPropsCombo.getSelectedItem();
            if ( qn.equals( featProperties[i].getName() ) ) {
                sb.append( "<wfs:PropertyName>" );
                if ( ftQualiName.getPrefix() != null && ftQualiName.getPrefix().length() > 0 ) {
                    sb.append( ftQualiName.getPrefix() ).append( ":" );
                }
                sb.append( qn.getLocalName() ).append( "</wfs:PropertyName>" );
            }

        }

        return sb;
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        geoPropsCombo.setEnabled( enabled );
        propertiesList.setEnabled( enabled );

        if ( !enabled ) {
            propertiesList.removeAll();
            geoPropsCombo.setModel( new DefaultComboBoxModel( new String[0] ) );
        }

    }

}