package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;

import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.ui.RecordPanel;
import com.vividsolutions.jump.workbench.ui.RecordPanelModel;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;

/**
 * The panel used to choose a connection and input a SQL query.
 */
public class RunDatastoreQueryPanel extends ConnectionPanel
                                   implements ActionListener, RecordPanelModel {

    private JTextField layerNameTextField;
    //private JTextField maxFeaturesTextField;
    private Hashtable queryMap = new Hashtable();
    private ArrayList currentConnectionQueries = new ArrayList();
    private int currentIndex = -1;
    private RecordPanel recordPanel = new RecordPanel(this);
    private LayerManager layerManager;


    public RunDatastoreQueryPanel(WorkbenchContext context) {
        super(context);
        layerManager = context.getLayerManager();
        initialize();
    }

    public int getRecordCount() {
        int num = 0;
        if (currentConnectionQueries != null) {
            num = currentConnectionQueries.size();
        }
        return num;
    }

    public void setCurrentIndex(int index) {
        currentIndex = index;
        String query = null;
        if (index > -1) {
            query = (String)currentConnectionQueries.get(index);
        }
        getQueryTextArea().setText(query);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    private void initialize() {
        JButton jbView = new JButton(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.View"));
        jbView.setToolTipText(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.View-Help"));
        final LayerManager layerM = layerManager;
        jbView.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                int currentSRID = layerM.getBlackboard().get("SRID", 0);
                queryTextArea.replaceSelection("${view:" + currentSRID + "}");
                queryTextArea.requestFocusInWindow();
            }
        });
        JButton jbFence = new JButton(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Fence"));
        jbFence.setToolTipText(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Fence-Help"));
        jbFence.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int currentSRID = layerM.getBlackboard().get("SRID", 0);
                queryTextArea.replaceSelection("${fence:" + currentSRID + "}");
                queryTextArea.requestFocusInWindow();
            }
        });
        JButton jbSelection = new JButton(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Selection"));
        jbSelection.setToolTipText(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Selection-Help"));
        jbSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int currentSRID = layerM.getBlackboard().get("SRID", 0);
                queryTextArea.replaceSelection("${selection:" + currentSRID + "}");
                queryTextArea.requestFocusInWindow();
            }
        });
        JPanel jpButtons = new JPanel(new java.awt.GridLayout(3,1));
        jpButtons.add(jbView);
        jpButtons.add(jbFence);
        jpButtons.add(jbSelection);
        addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Layer-Name"), getLayerNameTextField(), null, false);
        //addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Max-Features"), getMaxFeaturesTextField(), null, false);
        addRow(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Query"), new JScrollPane(getQueryTextArea()) {
            {setPreferredSize(new Dimension(MAIN_COLUMN_WIDTH, 100));}
        }, jpButtons, true);

        // We are not using addRow because we want the widgets centered over the
        // OK/Cancel buttons.
        add( recordPanel,
                 new GridBagConstraints( 0, //x
                 3, // y
                 3, // width
                 1, // height
                 1,
                 0,
                 GridBagConstraints.NORTHWEST,
                 GridBagConstraints.HORIZONTAL,
                 INSETS,
                 0,
                 0 )
            );

        getConnectionComboBox().addActionListener(this);
    }
    
    private JTextField getLayerNameTextField() {
        if (layerNameTextField == null) {
            layerNameTextField = new JTextField(
                layerManager.uniqueLayerName(I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.New-Query-Layer"))
            );
        }
        return layerNameTextField;
    }

    //private JTextField getMaxFeaturesTextField() {
    //    if (maxFeaturesTextField == null) {
    //        maxFeaturesTextField = new ValidatingTextField("", 10,
    //                new ValidatingTextField.BoundedIntValidator(1,
    //                        Integer.MAX_VALUE));
    //    }
    //    return maxFeaturesTextField;
    //}

    private JTextArea getQueryTextArea() {
        if (queryTextArea == null) {
            queryTextArea = new JTextArea();
        }
        return queryTextArea;
    }

    private JTextArea queryTextArea;

    public String validateInput() {
        String errMsg = super.validateInput();
        if (errMsg == null) {
            if (getQuery().length() == 0) {
                errMsg = I18N.get("jump.workbench.ui.plugin.datastore.RunDatastoreQueryPanel.Required-field-missing-Query");
            }
        }
        return errMsg;
    }
    
    public String getLayerName() {
        return layerManager.uniqueLayerName(layerNameTextField.getText().trim());
    }
    
    public void setLayerName(String layerName) {
        layerNameTextField.setText(layerName);
    }

    public String getQuery() {
        return queryTextArea.getText().trim();
    }
    
    public void setQuery(String query) {
        getQueryTextArea().setText(query);
    }

    public void saveQuery() {
        String query = getQuery();
        // maybe we should check for duplicates
        currentConnectionQueries.add(query);
        currentIndex = currentConnectionQueries.size()-1;
    }

    public void actionPerformed( ActionEvent actionEvent ) {
        ConnectionDescriptor cd = getConnectionDescriptor();
        if ( cd != null ) {
            if ( queryMap.containsKey(cd) ) {
                ArrayList prevQueries = (ArrayList) queryMap.get(cd);
                currentConnectionQueries = prevQueries;
            } else {
                currentConnectionQueries = new ArrayList();
                queryMap.put( cd, currentConnectionQueries );
            }
            setCurrentIndex( currentConnectionQueries.size()-1 );
            recordPanel.updateAppearance();
        }
    }


    /**
     * @return null if the user has left the Max Features text field blank.
     */
    //public Integer getMaxFeatures() {
    //    return maxFeaturesTextField.getText().trim().length() > 0 ? new Integer(
    //            maxFeaturesTextField.getText().trim()) : null;
    //}

    protected Collection connectionDescriptors() {
        return CollectionUtil.select(super.connectionDescriptors(),
            new Block() {
                public Object yield(Object connectionDescriptor) {
                    try {
                        return Boolean.valueOf(connectionManager()
                            .getDriver(
                                ((ConnectionDescriptor) connectionDescriptor)
                                .getDataStoreDriverClassName())
                            .isAdHocQuerySupported());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
    }
}
