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
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
import javax.swing.JTextField;
import javax.swing.border.Border;

import de.latlon.deejump.wfs.i18n.I18N;

/**
 * <code>WFSOptionsPanel2</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1516 $, $Date: 2008-07-17 11:32:16 +0200 (Do, 17 Jul 2008) $
 */
public class WFSOptionsPanel2 extends JPanel {

    private static final long serialVersionUID = -5976611628887849767L;

    protected WFSOptions options;

    private JFormattedTextField maxFeaturesField;

    private JComponent protocolPanel;

    private JComboBox outputFormatChooser;

    /**
     * @param options
     */
    public WFSOptionsPanel2( WFSOptions options ) {
        if ( options == null ) {
            throw new IllegalArgumentException( "WFSOptions cannot be null." );
        }
        this.options = options;
        initGUI();
    }

    private void initGUI() {
        LayoutManager layoutManager = new BoxLayout( this, BoxLayout.Y_AXIS );
        setLayout( layoutManager );

        maxFeaturesField = new JFormattedTextField();
        maxFeaturesField.setColumns( 10 );
        maxFeaturesField.setHorizontalAlignment(JTextField.RIGHT);
        maxFeaturesField.addPropertyChangeListener( "value", new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                options.setMaxFeatures( ( (Integer) evt.getNewValue() ).intValue() );
            }
        } );
        JPanel dummy = new JPanel();
        dummy.add( new JLabel( I18N.get( "WFSOptions.maxNumberOfFeatures" ) ) );
        dummy.add( maxFeaturesField );

        add( dummy );
        add( createProtocolPanel() );
        add( Box.createVerticalStrut( 5 ) );
        add( createOutputFormatChooser() );

        refreshGUI();

    }

    private void refreshGUI() {

        maxFeaturesField.setValue( new Integer( options.getMaxFeatures() ) );

        protocolPanel.removeAll();
        createProtocolButtons();

        outputFormatChooser.setModel( new DefaultComboBoxModel( options.getOutputFormats() ) );
        outputFormatChooser.setSelectedItem( options.getSelectedOutputFormat() );

    }

    private void createProtocolButtons() {

        ButtonGroup bg = new ButtonGroup();
        final String[] protocs = options.getProtocols();
        final String selProtoc = options.getSelectedProtocol();

        for ( int i = 0; i < protocs.length; i++ ) {

            JRadioButton rb = new JRadioButton( protocs[i] );

            // disabling protocol buttons! this feature isn't implemented
            rb.setEnabled( false );

            if ( selProtoc.equals( protocs[i] ) ) {
                rb.setSelected( true );
            }
            final int ix = i;
            rb.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    options.setSelectedProtocol( protocs[ix] );
                }
            } );
            bg.add( rb );
            protocolPanel.add( rb );
        }
    }

    private Component createProtocolPanel() {
        protocolPanel = new JPanel();
        protocolPanel.setBorder( BorderFactory.createTitledBorder( I18N.get( "WFSOptions.protocols" ) ) );
        protocolPanel.setLayout( new BoxLayout( protocolPanel, BoxLayout.Y_AXIS ) );
        protocolPanel.setMaximumSize( new Dimension( 150, 100 ) );
        protocolPanel.setAlignmentX( 1f );
        return protocolPanel;
    }

    private JComponent createOutputFormatChooser() {

        outputFormatChooser = new JComboBox();
        Border b1 = BorderFactory.createTitledBorder( I18N.get( "WFSOptions.outputFormats" ) );
        Border b2 = BorderFactory.createEmptyBorder( 2, 4, 4, 4 );

        outputFormatChooser.setBorder( BorderFactory.createCompoundBorder( b1, b2 ) );
        outputFormatChooser.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                JComboBox jcb = (JComboBox) e.getSource();
                options.setSelectedOutputFormat( (String) jcb.getSelectedItem() );
            }
        } );

        return outputFormatChooser;
    }

}
