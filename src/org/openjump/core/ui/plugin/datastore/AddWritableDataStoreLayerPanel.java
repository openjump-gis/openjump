package org.openjump.core.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.GeometryColumn;
import com.vividsolutions.jump.datastore.PrimaryKeyColumn;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.PasswordPrompter;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorManager;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Panel to define a connection to a read/write datastore.
 */
public class AddWritableDataStoreLayerPanel extends ConnectionPanel {

    private static final String KEY = AddWritableDataStoreLayerPanel.class.getName();

    private Map<ConnectionDescriptor,String[]> connectionDescriptorToDatasetNamesMap
            = new HashMap<>();

    private JComboBox<Object> datasetComboBox = null;
    private JComboBox<GeometryColumn> geometryAttributeComboBox = null;
    private JComboBox<PrimaryKeyColumn> identifierAttributeComboBox = null;
    private JTextField maxFeaturesTextField = null;
    private JTextArea whereTextArea = null;
    //private JCheckBox cachingCheckBox = null;
    private JCheckBox limitedToViewCheckBox = null;
    private JCheckBox manageConflictsCheckBox = null;

    public AddWritableDataStoreLayerPanel(WorkbenchContext context) {
        super( context );
        initialize();
        getConnectionComboBox().addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                getDatasetComboBox().setSelectedItem( null );
            }
        });
    }

    private static <T> T runInKillableThread( final String description,
                                              WorkbenchContext context,
                                              final Block<T> block ) {

        final List<T> result = new ArrayList<>();
        // ThreadedBasePlugIn displays a dialog that the user can
        // use to kill the thread by pressing the close button
        // [Jon Aquino 2005-03-14]
        AbstractPlugIn.toActionListener(
                new ThreadedBasePlugIn() {
                    public String getName() {
                        return description;
                    }

                    public boolean execute(PlugInContext context) throws Exception {
                        return true;
                    }

                    public void run(TaskMonitor monitor, PlugInContext context)
                            throws Exception {
                        monitor.report(description);
                        result.add(block.yield());
                    }
                }, context, new TaskMonitorManager()).actionPerformed( null );
        return result.size()>0?result.get(0):null;
    }

    public String getDatasetName() {
        return datasetComboBox.getSelectedItem() != null ?
                ((String)datasetComboBox.getSelectedItem()).trim() : null;
    }

    public GeometryColumn getGeometryColumn() {
        return geometryAttributeComboBox.getSelectedItem() != null ?
                ((GeometryColumn)geometryAttributeComboBox.getSelectedItem()) : null;
    }

    public PrimaryKeyColumn getIdentifierColumn() throws Exception {
        Object selectedItem = identifierAttributeComboBox.getSelectedItem();
        if (selectedItem != null) {
            if (selectedItem instanceof PrimaryKeyColumn) {
                return (PrimaryKeyColumn) identifierAttributeComboBox.getSelectedItem();
            } else {
                throw new SQLException(I18N.get(KEY + ".invalid-primary-key"));
            }
        } else {
            return null;
        }
    }

    public String getGeometryAttributeName() {
        return geometryAttributeComboBox.getSelectedItem() != null ?
                getGeometryColumn().getName().trim() : null;
    }

    public String getIdentifierAttributeName() throws Exception {
        return identifierAttributeComboBox.getSelectedItem() != null ?
                getIdentifierColumn().getName().trim() : null;
    }

    /**
     * @return Integer.MAX_VALUE if the user has left the Max Features text field blank.
     */
    public Integer getMaxFeatures() {
        if (maxFeaturesTextField.getText() == null) return Integer.MAX_VALUE;
        if (maxFeaturesTextField.getText().trim().length() == 0) return Integer.MAX_VALUE;
        if (maxFeaturesTextField.getText().trim().equals("-")) return Integer.MAX_VALUE;
        return new Integer(maxFeaturesTextField.getText().trim());
    }

    public String getWhereClause() {
        return getWhereClauseProper().toLowerCase().startsWith("where") ?
                getWhereClauseProper().substring("where".length()).trim() : getWhereClauseProper();
    }

    public String getWhereClauseProper() {
        return whereTextArea.getText().trim();
    }

    //public boolean isCaching() {
    //    return getCachingCheckBox().isSelected();
    //}

    //public void setCaching( boolean caching ) {
    //    getCachingCheckBox().setSelected( caching );
    //}

    public boolean isLimitedToView() {
        return getLimitedToViewCheckBox().isSelected();
    }

    void setLimitedToView( boolean limitedToView ) {
        getLimitedToViewCheckBox().setSelected( limitedToView );
    }

    public boolean isManageConfictsActive() {
        return getManageConflictsCheckBox().isSelected();
    }

    void setManageConfictsActive( boolean manageConflicts ) {
        getManageConflictsCheckBox().setSelected( manageConflicts );
    }

    public String validateInput() {
        if ( super.validateInput() != null ) {
            return super.validateInput();
        }
        if (((String) LangUtil.ifNull(getDatasetName(), "")).length() == 0) {
            return I18N.get(KEY + ".missing-dataset-name");
        }
        if (((String)LangUtil.ifNull(getGeometryAttributeName(), "")).length() == 0) {
            return I18N.get(KEY + ".missing-geometry-column-name");
        }
        try {
            if (((String) LangUtil.ifNull(getIdentifierAttributeName(), "")).length() == 0) {
                return I18N.get(KEY + ".missing-pk-name");
            }
        } catch(Exception e) {
            return I18N.get(KEY + ".invalid-primary-key");
        }
        return null;
    }

    private JTextArea getWhereTextArea() {
        if ( whereTextArea == null ) {
            whereTextArea = new JTextArea();
        }
        return whereTextArea;
    }

    private JComboBox getDatasetComboBox() {
        if ( datasetComboBox == null ) {
            datasetComboBox = new JComboBox<>();
            datasetComboBox.setPreferredSize( new Dimension( MAIN_COLUMN_WIDTH,
                    ( int ) datasetComboBox.getPreferredSize().getHeight() ) );
            datasetComboBox.setEditable( true );
            datasetComboBox.addActionListener(
                    new ActionListener() {
                        public void actionPerformed( ActionEvent e ) {
                            populateGeometryAttributeComboBox();
                            if ( geometryAttributeComboBox.getItemCount() > 0 ) {
                                geometryAttributeComboBox.setSelectedIndex( 0 );
                            }
                            populateIdentifierAttributeComboBox();
                            if ( identifierAttributeComboBox.getItemCount() > 0 ) {
                                identifierAttributeComboBox.setSelectedIndex( 0 );
                            }
                        }
                    } );
            // Populate the dataset combobox only if the user pushes the
            // drop-down button, as it requires a time-consuming query.
            // The user can also simply type in the dataset name. If they
            // inadvertently press the drop-down button, they can press
            // the X button on the progress dialog to kill the thread.
            // [Jon Aquino 2005-03-14]
            addSafePopupListener( datasetComboBox,
                    new Block() {
                        public Object yield() throws Exception {
                            populateDatasetComboBox();
                            return null;
                        }
                    } );
        }
        return datasetComboBox;
    }

    private JComboBox getGeometryAttributeComboBox() {
        if ( geometryAttributeComboBox == null ) {
            geometryAttributeComboBox = new JComboBox<>();
            geometryAttributeComboBox.setPreferredSize( new Dimension(
                    MAIN_COLUMN_WIDTH, ( int ) geometryAttributeComboBox.getPreferredSize().getHeight() ) );
            geometryAttributeComboBox.setEditable( true );
            //addSafePopupListener( geometryAttributeComboBox,
            //    new Block() {
            //        public Object yield() throws Exception {
            //            populateGeometryAttributeComboBox();
            //            return null;
            //        }
            //    } );
        }
        return geometryAttributeComboBox;
    }

    private JComboBox getIdentifierAttributeComboBox() {
        if ( identifierAttributeComboBox == null ) {
            identifierAttributeComboBox = new JComboBox<>();
            identifierAttributeComboBox.setPreferredSize( new Dimension(
                    MAIN_COLUMN_WIDTH, ( int ) identifierAttributeComboBox.getPreferredSize().getHeight() ) );
            identifierAttributeComboBox.setEditable( true );
            //addSafePopupListener( identifierAttributeComboBox,
            //    new Block() {
            //        public Object yield() throws Exception {
            //            populateIdentifierAttributeComboBox();
            //            return null;
            //        }
            //    } );
        }
        return identifierAttributeComboBox;
    }

    private JTextField getMaxFeaturesTextField() {
        if (maxFeaturesTextField == null) {
            maxFeaturesTextField = new ValidatingTextField("", 10,
                    new ValidatingTextField.BoundedIntValidator(1, Integer.MAX_VALUE));
        }
        return maxFeaturesTextField;
    }

    //private JTextField getMaxFeaturesTextField() {
    //    if (maxFeaturesTextField == null) {
    //        maxFeaturesTextField = new ValidatingTextField("", 10,
    //                new ValidatingTextField.BoundedIntValidator(1, Integer.MAX_VALUE));
    //    }
    //    return maxFeaturesTextField;
    //}

    //private JCheckBox getCachingCheckBox() {
    //    if ( cachingCheckBox == null ) {
    //        cachingCheckBox = new JCheckBox();
    //        cachingCheckBox.setText( I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Cache-features"));
    //        String s1= I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Prevents-unnecessary-queries-to-the-datastore");
    //        String s2= I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.The-recommended-setting-is-to-leave-this-checked");
    //        cachingCheckBox.setToolTipText( "<html>"+ s1 + "<br>"+s2+"</html>" );
    //        cachingCheckBox.setSelected( true );
    //    }
    //    return cachingCheckBox;
    //}

    private JCheckBox getLimitedToViewCheckBox() {
        if (limitedToViewCheckBox == null) {
            limitedToViewCheckBox = new JCheckBox();
            limitedToViewCheckBox.setText( I18N.get(KEY + ".limit-to-view"));
            String s1= I18N.get(KEY + ".limited-updates-to-the-viewport");
            limitedToViewCheckBox.setToolTipText("<html>" + s1 + "</html>");
            limitedToViewCheckBox.setSelected(false);
        }
        return limitedToViewCheckBox;
    }

    private JCheckBox getManageConflictsCheckBox() {
        if (manageConflictsCheckBox == null) {
            manageConflictsCheckBox = new JCheckBox();
            manageConflictsCheckBox.setText( I18N.get(KEY + ".manage-conflicts"));
            String s1= I18N.get(KEY + ".manage-conflicts-tooltip");
            manageConflictsCheckBox.setToolTipText("<html>" + s1 + "</html>");
            manageConflictsCheckBox.setSelected(true);
        }
        return manageConflictsCheckBox;
    }

    private void populateGeometryAttributeComboBox() {
        if ( getConnectionDescriptor() == null ) {
            return;
        }
        if ( getDatasetName() == null ) {
            return;
        }
        if ( getDatasetName().length() == 0 ) {
            return;
        }
        try {
            GeometryColumn selectedGeometryColumn = getGeometryColumn();
            geometryAttributeComboBox.setModel( new DefaultComboBoxModel<>(
                    sortGeometryColumns( getGeometryAttributes( getDatasetName(),
                            getConnectionDescriptor() ) ) ) );
            geometryAttributeComboBox.setSelectedItem( selectedGeometryColumn );
        } catch ( Exception e ) {
            getContext().getErrorHandler().handleThrowable( e );
            geometryAttributeComboBox.setModel( new DefaultComboBoxModel<GeometryColumn>() );
        }
    }

    private void populateIdentifierAttributeComboBox() {
        if ( getConnectionDescriptor() == null ) {
            return;
        }
        if ( getDatasetName() == null ) {
            return;
        }
        if ( getDatasetName().length() == 0 ) {
            return;
        }
        try {
            PrimaryKeyColumn selectedIdentifierColumn = getIdentifierColumn();
            PrimaryKeyColumn[] pks = sortIdentifierColumns(getIdentifierAttributes(getDatasetName(),
                    getConnectionDescriptor()));
            identifierAttributeComboBox.setModel( new DefaultComboBoxModel<>(pks));
            if (pks.length > 0) {
                // preserve the last used pk if pk list has not changed
                identifierAttributeComboBox.setSelectedItem( selectedIdentifierColumn );
            }
        } catch ( Exception e ) {
            getContext().getErrorHandler().handleThrowable( e );
            identifierAttributeComboBox.setModel( new DefaultComboBoxModel<PrimaryKeyColumn>() );
        }
    }

    private GeometryColumn[] sortGeometryColumns(java.util.List<GeometryColumn> list) {
        Collections.sort(list, new Comparator<GeometryColumn>() {
            public int compare(GeometryColumn o1, GeometryColumn o2) {
                return o1.getName().compareTo(o2.getName());
            }
            public boolean equals(Object obj) {
                return this == obj;
            }
        });
        return list.toArray(new GeometryColumn[list.size()]);
    }

    private PrimaryKeyColumn[] sortIdentifierColumns(List<PrimaryKeyColumn> list) {
        Collections.sort(list, new Comparator<PrimaryKeyColumn>() {
            public int compare(PrimaryKeyColumn o1, PrimaryKeyColumn o2) {
                return o1.getName().compareTo(o2.getName());
            }
            public boolean equals(Object obj) {
                return this == obj;
            }
        });
        return list.toArray(new PrimaryKeyColumn[list.size()]);
    }

    private void populateDatasetComboBox() {
        if ( getConnectionDescriptor() == null ) {
            return;
        }
        try {
            String selectedDatasetName = getDatasetName();
            String[] datasetNames = datasetNames( getConnectionDescriptor() );
            // avoid a NPE, if there are no spatial enabled tables in this database
            if (datasetNames != null) {
                datasetComboBox.setModel( new DefaultComboBoxModel<>(
                        sortByString( datasetNames) ) );
                datasetComboBox.setSelectedItem( selectedDatasetName );
            }
        } catch ( Exception e ) {
            getContext().getErrorHandler().handleThrowable( e );
            datasetComboBox.setModel( new DefaultComboBoxModel<>() );
        }
    }

    private List<GeometryColumn> getGeometryAttributes( final String datasetName,
                                                                  final ConnectionDescriptor connectionDescriptor ) throws Exception {
        // Prompt for a password outside the ThreadedBasePlugIn thread,
        // which is not the GUI thread. [Jon Aquino 2005-03-16]
        new PasswordPrompter().getOpenConnection( connectionManager(),
                connectionDescriptor, this );
        // Retrieve the dataset names using a ThreadedBasePlugIn, so
        // that the user can kill the thread if desired
        // [Jon Aquino 2005-03-16]
        return runInKillableThread(
                I18N.get(KEY + ".retrieving-list-of-geometry-attributes"), getContext(),
                new Block<List<GeometryColumn>>() {
                    public List<GeometryColumn> yield() throws Exception {
                        try {
                            return new PasswordPrompter().getOpenConnection(
                                    connectionManager(), connectionDescriptor,
                                    AddWritableDataStoreLayerPanel.this ).getMetadata()
                                    .getGeometryAttributes( datasetName );
                        } catch ( Exception e ) {
                            // Can get here if dataset name is not found in the
                            // datastore [Jon Aquino 2005-03-16]
                            Logger.warn("Exception thrown while retrieving GeometryColumns from the database", e);
                            return new ArrayList<>();
                        }
                    }
                } );
    }

    private List<PrimaryKeyColumn> getIdentifierAttributes( final String datasetName,
                                                        final ConnectionDescriptor connectionDescriptor ) throws Exception {
        // Prompt for a password outside the ThreadedBasePlugIn thread,
        // which is not the GUI thread. [Jon Aquino 2005-03-16]
        new PasswordPrompter().getOpenConnection( connectionManager(),
                connectionDescriptor, this );
        // Retrieve the dataset names using a ThreadedBasePlugIn, so
        // that the user can kill the thread if desired
        // [Jon Aquino 2005-03-16]
        return runInKillableThread(
                I18N.get(KEY + ".retrieving-list-of-geometry-attributes"), getContext(),
                new Block<List<PrimaryKeyColumn>>() {
                    public List<PrimaryKeyColumn> yield() throws Exception {
                        try {
                            return new PasswordPrompter().getOpenConnection(
                                    connectionManager(), connectionDescriptor,
                                    AddWritableDataStoreLayerPanel.this ).getMetadata()
                                    .getPrimaryKeyColumns(datasetName);
                        } catch ( Exception e ) {
                            // Can get here if dataset name is not found in the
                            // datastore [Jon Aquino 2005-03-16]
                            Logger.warn("Exception thrown while retrieving PrimaryKeyColumn from the database", e);
                            return new ArrayList<>();
                        }
                    }
                } );
    }

    private String[] datasetNames(
            final ConnectionDescriptor connectionDescriptor ) throws Exception {
        // [mmichaud 2013-03-23] remove this test to be sure that the list of
        // tables is always uptodate (hopefully, the query to the metadata
        // table is always fast)
        //if ( !connectionDescriptorToDatasetNamesMap.containsKey( connectionDescriptor ) ) {
        // Prompt for a password outside the ThreadedBasePlugIn thread,
        // which is not the GUI thread. [Jon Aquino 2005-03-11]
        new PasswordPrompter().getOpenConnection( connectionManager(), connectionDescriptor, this );
        // Retrieve the dataset names using a ThreadedBasePlugIn, so
        // that the user can kill the thread if desired
        // [Jon Aquino 2005-03-11]
        String[] datasetNames = runInKillableThread(
                I18N.get(KEY + ".retrieving-list-of-tables"), getContext(),
                new Block<String[]>() {
                    public String[] yield() throws Exception {
                        return new PasswordPrompter().getOpenConnection(
                                connectionManager(), connectionDescriptor,
                                AddWritableDataStoreLayerPanel.this ).getMetadata()
                                .getDatasetNames();
                    }
                } );
        // Don't cache the dataset array if it is empty, as a problem
        // likely occurred. [Jon Aquino 2005-03-14]
        if ( datasetNames != null && datasetNames.length != 0 ) {
            connectionDescriptorToDatasetNamesMap.put( connectionDescriptor,
                    datasetNames );
        }

        return connectionDescriptorToDatasetNamesMap.get( connectionDescriptor );
    }

    private void initialize() {
        JScrollPane sp = new JScrollPane( getWhereTextArea() );
        sp.setPreferredSize( new Dimension( MAIN_COLUMN_WIDTH, 100 ) );

        addRow("Dataset", getDatasetComboBox(), null, false );
        addRow("Geometry", getGeometryAttributeComboBox(), null, false );
        addRow("Identifier", getIdentifierAttributeComboBox(), null, false );
        addRow("Max-Features", getMaxFeaturesTextField(), null, false);
        addRow("Where", sp, null, true );
        addRow(null, getLimitedToViewCheckBox(), null, false );
        addRow(null, getManageConflictsCheckBox(), null, true );
    }

    /**
     *  Workaround for undesirable Java 1.5 behaviour: after showing a dialog in
     *  the #popupMenuWillBecomeVisible event handler, the combobox popup would
     *  not hide.
     *
     * @param  comboBox  The feature to be added to the SafePopupListener
     *      attribute
     * @param  listener  The feature to be added to the SafePopupListener
     *      attribute
     */
    private void addSafePopupListener( final JComboBox comboBox,
                                       final Block listener ) {
        comboBox.addPopupMenuListener(
                new PopupMenuListener() {

                    private boolean ignoringPopupEvent = false;

                    public void popupMenuCanceled( PopupMenuEvent e ) { }

                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e ) { }

                    public void popupMenuWillBecomeVisible( PopupMenuEvent e ) {
                        if ( ignoringPopupEvent ) {
                            ignoringPopupEvent = false;
                            return;
                        }
                        SwingUtilities.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        comboBox.hidePopup();
                                        try {
                                            listener.yield();
                                        } catch ( Exception e ) {
                                            throw new RuntimeException( e );
                                        } finally {
                                            ignoringPopupEvent = true;
                                            comboBox.showPopup();
                                        }
                                    }
                                } );
                    }
                } );
    }

    public interface Block<T> {
        T yield() throws Exception;
    }
}
