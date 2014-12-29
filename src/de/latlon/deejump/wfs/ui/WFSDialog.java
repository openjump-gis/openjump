/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.ui;

import static de.latlon.deejump.wfs.Version.VERSION;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JScrollPane;

import com.vividsolutions.jump.workbench.WorkbenchContext;

import de.latlon.deejump.wfs.plugin.*;

/**
 * This dialog presents a graphical user interface to OGC Filter operations. It encapsulates two panels, one for
 * attribute-based feature search and the other for geometry-based search. Both search methods can be combined. The
 * dialog generates a GetFeature request as an XML string. This can be used to query a WFS.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei </a>
 * 
 */
public class WFSDialog extends JDialog {

    private static final long serialVersionUID = 5540535312268661105L;

    /**
     * 
     */
    public static final String WFS_URL_LIST = "WFS_URL_LIST";

    /**
     * Whether the dialog has enough info to produce a search or it makes sense to carry on. For example, when the user
     * closed (cancelled) the dialog.
     */
    boolean canSearch = false;

    private WFSPanel wfsPanel;

    private WorkbenchContext context;

    /**
     * Creates a dialog from an owner, with a title and a WFS server address.
     * 
     * @param context
     * 
     * @param owner
     *            the parent window
     * @param title
     *            the name to appear on the window bar
     * @param urls
     *            the address of the server. This is something like http://my.domain.com/deegreewfs/wfs
     * @throws java.awt.HeadlessException
     */
    public WFSDialog( WorkbenchContext context, Frame owner, String title ) {

        super( owner, title, true );
        this.context = context;
        setTitle( "WFSPlugin v. " + VERSION );

        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent we ) {
                canSearch = false;
                dispose();
            }
        } );

        initGUI();

    }

    /** Initialize main GUI and its children */
    private void initGUI( ) {

        getContentPane().setLayout( new BorderLayout() );

        this.wfsPanel = new WFSPanel( context );

        getContentPane().add(wfsPanel, BorderLayout.CENTER);

        WFSPanelButtons buttons = new WFSPanelButtons( this, this.wfsPanel );
        this.wfsPanel.controlButtons = buttons;
        buttons.okButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setVisible( false );
                setCanSearch( true );
            }
        } );
        buttons.okButton.setEnabled( false );

        buttons.cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setVisible( false );
                setCanSearch( false );
            }
        } );
        getContentPane().add( buttons, BorderLayout.SOUTH );

        setResizable( true );
        pack();
    }

    /**
     * Whether it makes sense to ask for a GetFeature request. This is generally true, but clicking on the Cancel or
     * clisong the dialog will return <code>false</code>, meaning that the user changed his mind and no requst should
     * be sent.
     * 
     * @return a boolean value hinting whether to carry on or not
     */
    public boolean canSearch() {
        return this.canSearch;
    }

    /**
     * @param canSearch
     */
    public void setCanSearch( boolean canSearch ) {
        this.canSearch = canSearch;
    }

    /**
     * @return the internal panel
     */
    public WFSPanel getWFSPanel() {
        return this.wfsPanel;
    }

}