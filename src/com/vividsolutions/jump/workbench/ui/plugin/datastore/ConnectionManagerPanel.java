package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

public class ConnectionManagerPanel extends JPanel {
  // Nicolas Ribot, 07 dec 2015: 
  // add icons according to database source
  private static final Icon CONNECTED_ICON = IconLoader.icon("connect.png");
  private static final Icon DISCONNECTED_ICON = IconLoader.icon("disconnect.png");
  private static final Icon PG_CONNECTED_ICON = IconLoader.icon("ok_pg.png");
  private static final Icon PG_DISCONNECTED_ICON = IconLoader.icon("ko_pg.png");
  private static final Icon H2_CONNECTED_ICON = IconLoader.icon("h2_icon.png");
  private static final Icon H2_DISCONNECTED_ICON = GUIUtil.toGrayScale((ImageIcon)H2_CONNECTED_ICON);
  private static final Icon ORA_CONNECTED_ICON = IconLoader.icon("ok_oracle.png");
  private static final Icon ORA_DISCONNECTED_ICON = IconLoader.icon("ko_oracle.png");
  private static final Icon MARIA_CONNECTED_ICON = IconLoader.icon("ok_mariadb.png");
  private static final Icon MARIA_DISCONNECTED_ICON = IconLoader.icon("ko_mariadb.png");
  private static final Icon SQLITE_CONNECTED_ICON = IconLoader.icon("ok_spatialite.png");
  private static final Icon SQLITE_DISCONNECTED_ICON = GUIUtil.toGrayScale((ImageIcon)SQLITE_CONNECTED_ICON);
  private static final Icon MYSQL_CONNECTED_ICON = IconLoader.icon("dolphin_icon.png");
  private static final Icon MYSQL_DISCONNECTED_ICON = GUIUtil.toGrayScale((ImageIcon)MYSQL_CONNECTED_ICON);
//  private final Icon SQLSERVER_CONNECTED_ICON = IconLoader.icon("ok_sqlserver.png");
//  private final Icon SQLSERVER_DISCONNECTED_ICON = IconLoader.icon("ok_sqlserver.png");
  
  private final Icon DBS_ICON = IconLoader.icon("famfam/database_edit.png");
  private final Icon NEW_DB_ICON = IconLoader.icon("famfam/database_add.png");
  private final Icon DELETE_DB_ICON = IconLoader.icon("famfam/database_delete.png");

    // Partially generated using Eclipse Visual Editor [Jon Aquino 2005-03-08]

    private JScrollPane scrollPane = null;
    private JList connectionJList = null;
    private JPanel buttonPanel = null;
    private JButton addButton = null;
    private JButton copyButton = null;
    private JButton deleteButton = null;
    private JButton connectButton = null;
    private JPanel fillerPanel = null;
    private JButton disconnectButton = null;
    private ConnectionManager connectionManager;
    private ErrorHandler errorHandler;
    private Registry registry;
    private WorkbenchContext context;

    public ConnectionManagerPanel(ConnectionManager connectionManager,
            Registry registry, ErrorHandler errorHandler, WorkbenchContext context) {
        super();
        initialize();
        this.connectionManager = connectionManager;
        this.registry = registry;
        this.errorHandler = errorHandler;
        this.context = context;
        initializeConnectionJList();
        updateButtons();
        connectionJList.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        updateButtons();
                    }
                });
    }

    private void initializeConnectionJList() {
        connectionJList.setModel(createListModel());
    }

    private void updateButtons() {
      boolean hasDrivers = ! registry.getEntries(DataStoreDriver.REGISTRY_CLASSIFICATION).isEmpty();
      boolean hasSelected = ! getSelectedConnectionDescriptors().isEmpty();
      addButton.setEnabled(hasDrivers);
      copyButton.setEnabled(hasDrivers && hasSelected);
        deleteButton.setEnabled(!getSelectedConnectionDescriptors().isEmpty());
        connectButton.setEnabled(findSelectedConnection(new Block() {
            public Object yield(Object connection) {
                try {
                    return Boolean.valueOf(((DataStoreConnection) connection)
                            .isClosed());
                } catch (DataStoreException e) {
                    errorHandler.handleThrowable(e);
                    return Boolean.FALSE;
                }
            }
        }));
        disconnectButton.setEnabled(findSelectedConnection(new Block() {
            public Object yield(Object connection) {
                try {
                    return Boolean.valueOf(!((DataStoreConnection) connection)
                            .isClosed());
                } catch (DataStoreException e) {
                    errorHandler.handleThrowable(e);
                    return Boolean.FALSE;
                }
            }
        }));
    }

    private boolean findSelectedConnection(Block criterion) {
        for (Iterator i = getSelectedConnectionDescriptors().iterator(); i
                .hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            if (criterion.yield(connectionManager
                    .getConnection(connectionDescriptor)) == Boolean.TRUE) {
                return true;
            }
        }
        return false;
    }

    private ListModel createListModel() {
        DefaultListModel listModel = new DefaultListModel();
        for (Iterator i = sort(
                new ArrayList(connectionManager.getConnectionDescriptors()),
                new Comparator() {
                    public int compare(Object a, Object b) {
                        return ((ConnectionDescriptor) a).toString().compareTo(
                                ((ConnectionDescriptor) b).toString());
                    }
                }).iterator(); i.hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            listModel.addElement(connectionDescriptor);
        }
        return listModel;
    }

    private List sort(List collection, Comparator comparator) {
        Collections.sort(collection, comparator);
        return collection;
    }

    private void initialize() {
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        // Use #setPreferredSize rather than #setSize; otherwise if the
        // connection list is pre-populated with items, the preferred
        // size will be based on the preferred sizes of the items,
        // and will probably be too narrow. [Jon Aquino 2005-03-11]
        this.setPreferredSize(new Dimension(400, 300));
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 0;
        gridBagConstraints2.weightx = 1.0;
        gridBagConstraints2.weighty = 1.0;
        gridBagConstraints2.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints2.gridheight = 2;
        gridBagConstraints2.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints4.gridx = 1;
        gridBagConstraints4.gridy = 1;
        gridBagConstraints4.weighty = 1.0D;
        gridBagConstraints4.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints1.insets = new java.awt.Insets(5, 0, 0, 5);
        this.add(getScrollPane(), gridBagConstraints2);
        this.add(getButtonPanel(), gridBagConstraints1);
        this.add(getFillerPanel(), gridBagConstraints4);
    }

    private JScrollPane getScrollPane() {
        if (scrollPane == null) {
            scrollPane = new JScrollPane();
            scrollPane.setViewportView(getConnectionJList());
        }
        return scrollPane;
    }

    private JList getConnectionJList() {
        if (connectionJList == null) {
            connectionJList = new JList();
            connectionJList.setCellRenderer(new DefaultListCellRenderer() {

                public Component getListCellRendererComponent(JList list,
                        Object value, int index, boolean isSelected,
                        boolean cellHasFocus) {
                    ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) value;
                    super.getListCellRendererComponent(list,
                            connectionDescriptor, index, isSelected,
                            cellHasFocus);
                    try {
                        Icon icon = getConnectionIcon(connectionDescriptor,
                            connectionManager.getConnection(
                                connectionDescriptor).isClosed());
                        setIcon(icon);
//                        setIcon(connectionManager.getConnection(
//                                connectionDescriptor).isClosed() ? DISCONNECTED_ICON
//                                : CONNECTED_ICON);
                    } catch (DataStoreException e) {
                        errorHandler.handleThrowable(e);
                    }
                    return this;
                }
            });
        }
        return connectionJList;
    }

    private JPanel getButtonPanel() {
      if (buttonPanel == null) {
        GridLayout gridLayout3 = new GridLayout();
        gridLayout3.setRows(6);
        gridLayout3.setVgap(5);
        gridLayout3.setColumns(1);
        gridLayout3.setHgap(0);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(gridLayout3);
        buttonPanel.add(getAddButton(), null);
        buttonPanel.add(getCopyButton(), null);
        buttonPanel.add(getDeleteButton(), null);
        buttonPanel.add(new JLabel());
        buttonPanel.add(getConnectButton(), null);
        buttonPanel.add(getDisconnectButton(), null);
      }
      return buttonPanel;
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton();
            addButton.setIcon(NEW_DB_ICON);
            addButton.setText(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Add"));
            addButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ConnectionDescriptor connectionDescriptor = addConnection();
                    initializeConnectionJList();
                    if (connectionDescriptor != null) {
                        getConnectionJList().setSelectedValue(
                                connectionDescriptor, true);
                    }
                    updateButtons();
                }
            });
        }
        return addButton;
    }

    private JButton getCopyButton() {
        if (copyButton == null) {
            copyButton = new JButton();
            copyButton.setIcon(DBS_ICON);
            copyButton.setText(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Copy"));
            copyButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ConnectionDescriptor connectionDescriptor = copyConnection();
                    initializeConnectionJList();
                    if (connectionDescriptor != null) {
                        getConnectionJList().setSelectedValue(
                                connectionDescriptor, true);
                    }
                    updateButtons();
                }
            });
        }
        return copyButton;
    }

    private ConnectionDescriptor copyConnection() {
      return addOrCopyConnection(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Copy-Connection"), getSelectedConnection());
    }

    private ConnectionDescriptor addConnection() {
      // MD - this behavior no longer needed?
      if (registry.getEntries(DataStoreDriver.REGISTRY_CLASSIFICATION).isEmpty()) {
        JOptionPane
            .showMessageDialog(
                SwingUtilities.windowForComponent(this),
                I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.No-datastore-drivers-are-loaded"));
        return null;
      }
      return addOrCopyConnection(
          I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Add-Connection"),
          null);
    }

    private ConnectionDescriptor addOrCopyConnection(String title, ConnectionDescriptor connDesc) {
        Window window = SwingUtilities
                .windowForComponent(ConnectionManagerPanel.this);
        OKCancelDialog.Validator validator = new OKCancelDialog.Validator() {
            public String validateInput(Component component) {
                return ((ConnectionDescriptorPanel) component).validateInput();
            }
        };
        final ConnectionDescriptorPanel connectionDescriptorPanel = new ConnectionDescriptorPanel(registry, context);
        if (connDesc != null)
          connectionDescriptorPanel.setParameters(connDesc);
        final OKCancelDialog dialog = window instanceof Dialog ? new OKCancelDialog(
                (Dialog) window, title, true,
                connectionDescriptorPanel, validator)
                : new OKCancelDialog((Frame) window, title, true,
                        connectionDescriptorPanel, validator);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return null;
        }
        try {
            // Don't use PasswordPrompter dialog, as the user has already
            // entered a password in the ConnectionDescriptorPanel
            // [Jon Aquino 2005-03-15]
            connectionManager.getOpenConnection(connectionDescriptorPanel
                    .getConnectionDescriptor());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // At least make sure a closed connection exists, so
            // a connection appears in the list box. [Jon Aquino 2005-03-09]
            connectionManager.getConnection(connectionDescriptorPanel
                    .getConnectionDescriptor());
        }
        return connectionDescriptorPanel.getConnectionDescriptor();
    }

    private JButton getDeleteButton() {
        if (deleteButton == null) {
            deleteButton = new JButton();
            deleteButton.setIcon(DELETE_DB_ICON);
            deleteButton.setText(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Delete"));
            deleteButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        deleteSelectedConnections();
                    } catch (DataStoreException x) {
                        errorHandler.handleThrowable(x);
                    }
                    initializeConnectionJList();
                    updateButtons();
                }
            });
        }
        return deleteButton;
    }

    private JButton getConnectButton() {
        if (connectButton == null) {
            connectButton = new JButton();
            connectButton.setIcon(CONNECTED_ICON);
            connectButton.setText(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Connect"));
            connectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        openSelectedConnections();
                    } catch (Exception x) {
                        errorHandler.handleThrowable(x);
                    }
                    updateButtons();
                    repaintConnectionJList();
                }
            });
        }
        return connectButton;
    }

    private JPanel getFillerPanel() {
        if (fillerPanel == null) {
            fillerPanel = new JPanel();
            fillerPanel.setLayout(new GridBagLayout());
        }
        return fillerPanel;
    }

    private JButton getDisconnectButton() {
        if (disconnectButton == null) {
            disconnectButton = new JButton();
            disconnectButton.setIcon(DISCONNECTED_ICON);
            disconnectButton.setText(I18N.get("jump.workbench.ui.plugin.datastore.ConnectionManagerPanel.Disconnect"));
            disconnectButton
                    .addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            try {
                                closeSelectedConnections();
                            } catch (DataStoreException x) {
                                errorHandler.handleThrowable(x);
                            }
                            updateButtons();
                            repaintConnectionJList();
                        }
                    });
        }
        return disconnectButton;
    }

    private void repaintConnectionJList() {
        connectionJList.repaint();
    }

    /**
     * Gets the first selected connection, or null if none.
     *
     * @return the first selected connection
     * @return null if none selected
     */
    private ConnectionDescriptor getSelectedConnection()
    {
      for (Iterator i = getSelectedConnectionDescriptors().iterator(); i
              .hasNext();) {
          ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                  .next();
          return connectionDescriptor;
      }
      return null;
    }

    private void deleteSelectedConnections() throws DataStoreException {
        for (Iterator i = getSelectedConnectionDescriptors().iterator(); i
                .hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            connectionManager.deleteConnectionDescriptor(connectionDescriptor);
        }
    }

    private void openSelectedConnections() throws Exception {
        for (Iterator i = getSelectedConnectionDescriptors().iterator(); i
                .hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            if (connectionManager.getConnection(connectionDescriptor)
                    .isClosed()) {
                new PasswordPrompter().getOpenConnection(connectionManager,
                        connectionDescriptor, this);
            }
        }
    }

    private void closeSelectedConnections() throws DataStoreException {
        for (Iterator i = getSelectedConnectionDescriptors().iterator(); i
                .hasNext();) {
            ConnectionDescriptor connectionDescriptor = (ConnectionDescriptor) i
                    .next();
            if (!connectionManager.getConnection(connectionDescriptor)
                    .isClosed()) {
                connectionManager.getConnection(connectionDescriptor).close();
            }
        }
    }

    public Collection getSelectedConnectionDescriptors() {
        return Arrays.asList(connectionJList.getSelectedValues());
    }
    
    /**
     * Returns the icon corresponding to this @link DataStoreConnection and its state 
     * (closed or opened)
     * @param desc the connection to get icon from
     * @return the corresponding icon
     */
    public static Icon getConnectionIcon(ConnectionDescriptor desc,
        boolean isClosed) throws DataStoreException {
      String driverClassName = desc.getDataStoreDriverClassName();
      
      if (driverClassName
          .equals(com.vividsolutions.jump.datastore.postgis.PostgisDataStoreDriver.class
              .getName())) {
        return isClosed ? PG_DISCONNECTED_ICON : PG_CONNECTED_ICON;
      } else if (driverClassName
          .equals(com.vividsolutions.jump.datastore.oracle.OracleDataStoreDriver.class
              .getName())) {
        return isClosed ? ORA_DISCONNECTED_ICON : ORA_CONNECTED_ICON;
      } else if (driverClassName
          .equals(com.vividsolutions.jump.datastore.mariadb.MysqlDataStoreDriver.class
              .getName())) {
        return isClosed ? MYSQL_DISCONNECTED_ICON : MYSQL_CONNECTED_ICON;
      }
      // match MariadbDataStoreDriver & MysqlMariadbDataStoreDriver
      else if (driverClassName.matches("(?i).+\\.\\w*Mariadb\\w+Driver")) {
        return isClosed ? MARIA_DISCONNECTED_ICON : MARIA_CONNECTED_ICON;
      } else if (driverClassName
          .equals(com.vividsolutions.jump.datastore.spatialite.SpatialiteDataStoreDriver.class
              .getName())) {
        return isClosed ? SQLITE_DISCONNECTED_ICON : SQLITE_CONNECTED_ICON;
      } 
      // match both H2 DataStoreDrivers
      else if (driverClassName.matches("(?i).+\\.H2\\w+Driver")) {
        return isClosed ? H2_DISCONNECTED_ICON : H2_CONNECTED_ICON;
      }
      // Default
      return isClosed ? DISCONNECTED_ICON : CONNECTED_ICON;
    }

/*
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Connection Manager");
        frame.getContentPane().add(
                new ConnectionManagerPanel(ConnectionManager
                        .instance(new Blackboard()), new Registry()
                        .createEntry(DataStoreDriver.REGISTRY_CLASSIFICATION,
                                new OracleDataStoreDriver()),
                        new ErrorHandler() {
                            public void handleThrowable(Throwable t) {
                                System.out.println("Handling error: ");
                                t.printStackTrace(System.out);
                            }
                        },null));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.pack();
        GUIUtil.centreOnScreen(frame);
        frame.setVisible(true);
    }
*/

}