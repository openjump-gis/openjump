/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ghassan Hamammi (hamammi@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.ui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;

/**
 * @author hamammi
 * 
 * TODO To change the template for this generated type comment go to Window - Preferences - Java -
 * Code Style - Code Templates
 */
public class ExtensibleComboBox extends JComboBox {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param objects
     */
    public ExtensibleComboBox( Object[] objects ) {
        super( objects );
        createListener();
        setEditable( true );
    }

    /**
     * 
     */
    public void createListener() {
        addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent e ) {
                if ( e.getStateChange() == ItemEvent.SELECTED ) {
                    JComboBox box = (JComboBox) e.getSource();
                    String newServer = (String) e.getItem();
                    int size = box.getModel().getSize();
                    List<Object> candidateGeoProps = new ArrayList<Object>( size );
                    for ( int i = 0; i < size; i++ ) {
                        candidateGeoProps.add( box.getModel().getElementAt( i ) );
                    }
                    if ( newServer != null && !candidateGeoProps.contains( newServer ) ) {
                        box.addItem( newServer );
                    }
                }
            }
        } );
    }

}
