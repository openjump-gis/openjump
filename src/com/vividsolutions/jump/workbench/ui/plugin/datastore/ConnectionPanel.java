package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;


/**
 *  Base class for panels with a Connection combobox.
 */
public class ConnectionPanel extends JPanel {
    protected final static ImageIcon ADD_DB_ICON = IconLoader.icon( "famfam/add.png" );

    protected final static int MAIN_COLUMN_WIDTH = 550;

    protected final static Insets INSETS = new Insets( 2, 2, 2, 2 );

    private WorkbenchContext context;

    private JComboBox connectionComboBox = null;

    private int nextRow = 0;

    private JButton chooseConnectionButton = null;


    public ConnectionPanel( WorkbenchContext context ) {
        this.context = context;
        initialize();
        populateConnectionComboBox();
    }

    public ConnectionDescriptor getConnectionDescriptor() {
        return ( ConnectionDescriptor ) connectionComboBox.getSelectedItem();
    }

    public WorkbenchContext getContext() {
        return context;
    }


    public void populateConnectionComboBox() {
        ConnectionDescriptor selectedConnectionDescriptor = getConnectionDescriptor();
        connectionComboBox.setModel( new DefaultComboBoxModel(
            sortByString( connectionDescriptors().toArray() ) ) );
        // Note that the selectedConnectionDescriptor may no longer exist,
        // in which case #setSelectedItem will have no effect.
        // [Jon Aquino 2005-03-10]
        connectionComboBox.setSelectedItem( selectedConnectionDescriptor );
    }

    public String validateInput() {
        if ( getConnectionDescriptor() == null ) {
            return I18N.get("com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel.Required-field-missing-Connection");
        }
        return null;
    }

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    
    protected JComboBox getConnectionComboBox() {
        if ( connectionComboBox == null ) {
            connectionComboBox = new JComboBox();
            connectionComboBox.setPreferredSize( new Dimension( MAIN_COLUMN_WIDTH,
                ( int ) connectionComboBox.getPreferredSize().getHeight() ) );
            connectionComboBox.setRenderer(new ListCellRenderer() {
              @Override
              public Component getListCellRendererComponent(
                  JList list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
                if (!(value instanceof ConnectionDescriptor))
                  return renderer;
                ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) value;
                try {
                  Icon icon = ConnectionManagerPanel.getConnectionIcon(connectionDescriptor,
                      connectionManager().getConnection(
                          connectionDescriptor).isClosed());
                  renderer.setIcon(icon);
                } catch (DataStoreException e) {
                  // we ignore those for eye candy
                }
                
                return renderer;
              }
              
            });
        }
        return connectionComboBox;
    }
    
    public void addActionListener(ActionListener l) {
        connectionComboBox.addActionListener(l);
    }
    
    public void removeActionListener(ActionListener l) {
        connectionComboBox.removeActionListener(l);
    }


    protected void addRow( String caption,
                           Component a,
                           Component b,
                           boolean aStretchesVertically ) {
        //caption = caption == null ? "" : I18N.get(this.getClass().getName()+"."+caption);
        caption = caption == null ? "" : I18N.get("com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel."+caption);
        add( new JLabel( caption ),
             new GridBagConstraints( 0,
               nextRow,
               1,
               1,
               0,
               0,
               GridBagConstraints.NORTHWEST,
               GridBagConstraints.NONE,
               INSETS,
               0,
               0 )
            );

        if ( aStretchesVertically ) {
            add( a,
                 new GridBagConstraints( 1,
                 nextRow,
                 1,
                 1,
                 1,
                 1,
                 GridBagConstraints.NORTHWEST,
                 GridBagConstraints.BOTH,
                 INSETS,
                 0,
                 0 )
            );
        } else {
            add( a,
                 new GridBagConstraints( 1,
                 nextRow,
                 1,
                 1,
                 1,
                 0,
                 GridBagConstraints.NORTHWEST,
                 GridBagConstraints.HORIZONTAL,
                 INSETS,
                 0,
                 0 )
            );
        }

        if ( b != null ) {
            add( b, new GridBagConstraints( 2, nextRow, 1, 1, 0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                INSETS, 0, 0 ) );
        }

        nextRow++;
    }


    protected Collection connectionDescriptors() {
        return connectionManager().getConnectionDescriptors();
    }

    protected ConnectionManager connectionManager() {
        return ConnectionManager.instance( context );
    }

    protected Object[] sortByString( Object[] objects ) {
        Arrays.sort( objects,
            new Comparator() {
                public int compare( Object o1, Object o2 ) {
                    return o1.toString().compareTo( o2.toString() );
                }
            } );
        return objects;
    }

    private JButton getChooseConnectionButton() {
        if ( chooseConnectionButton == null ) {
            chooseConnectionButton = new JButton();
            chooseConnectionButton.setIcon( ADD_DB_ICON );
            chooseConnectionButton.setToolTipText( I18N.get("com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel.Connection-Manager"));
            chooseConnectionButton.setMargin( new Insets( 0, 0, 0, 0 ) );
            chooseConnectionButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        chooseConnection();
                    }
                } );
        }
        return chooseConnectionButton;
    }

    private void initialize() {
        setLayout( new GridBagLayout() );
        addRow( "Connection", getConnectionComboBox(), getChooseConnectionButton(), false );
    }

    private void chooseConnection() {
        ConnectionManagerPanel panel = new ConnectionManagerPanel(
            ConnectionManager.instance( getContext() ),
            getContext().getRegistry(), getContext().getErrorHandler(), context );
        OKCancelDialog dialog = new OKCancelDialog( ( Dialog ) SwingUtilities.windowForComponent( ConnectionPanel.this ), 
        	I18N.get("com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel.Connection-Manager"),
            true, panel,
            new OKCancelDialog.Validator() {
                public String validateInput( Component component ) {
                    return null;
                }
            } );
        dialog.setVisible( true );
        // Even if OK was not pressed, refresh the combobox.
        // [Jon Aquino 2005-03-16]
        populateConnectionComboBox();
        if ( !dialog.wasOKPressed() ) {
            return;
        }
        if ( panel.getSelectedConnectionDescriptors().isEmpty() ) {
            return;
        }
        getConnectionComboBox().setSelectedItem(
            panel.getSelectedConnectionDescriptors().iterator().next() );
    }
}
