package com.vividsolutions.jump.workbench.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.Logger;

public class FeatureCollectionPanel extends JPanel {

    /**
     * [Giuseppe Aruta 2017-14-12] A panel to show a collection of features
     */
    private static final long serialVersionUID = 1L;
    private static FeatureCollection featureCollection;
    private static DefaultTableModel model = new DefaultTableModel();
    private static JScrollPane pane = new JScrollPane();
    private static JTable jTable = new JTable();
    private static JLabel jLabel = new JLabel();
    private final Color LIGHT_GRAY = new Color(230, 230, 230);

    @SuppressWarnings("static-access")
    public FeatureCollectionPanel(FeatureCollection featureCollection) {
        super();
        this.featureCollection = featureCollection;
        try {
            jbInit();
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void jbInit() throws Exception {
        setLayout(new BorderLayout());
        jLabel.setText(I18N.get("ui.AttributeTablePanel.featurecollection")
                + " - (" + featureCollection.size() + " "
                + I18N.get("ui.AttributeTablePanel.features") + ")");
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        setFeatureCollection(featureCollection);
        jTable = new JTable();
        jTable.setGridColor(Color.WHITE);
        jTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? Color.white : LIGHT_GRAY);
                if (isSelected) {
                    c.setBackground(Color.black);
                }
                return c;
            };
        });
        // tModel = setTableModelFromFeatureCollection();
        pane = new JScrollPane(jTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // if (tModel != null) {
        jTable.setModel(setTableModelFromFeatureCollection());
        // }
        jTable.setEnabled(true);
        add(jLabel, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);

    }

    public static FeatureCollection setFeatureCollection(
            FeatureCollection featurecollection) {
        return featureCollection;
    }

    public static FeatureCollection getFeatureCollection() {
        return featureCollection;
    }

    public static TableModel setTableModelFromFeatureCollection() {
        model = new DefaultTableModel();
        String[] fields;
        int iCount;
        iCount = featureCollection.getFeatures().size();
        FeatureSchema schema = featureCollection.getFeatureSchema();
        ArrayList<String> ar = new ArrayList<String>();
        String name;
        for (int j = 0; j < schema.getAttributeNames().size(); j++) {
            name = schema.getAttributeName(j).toString();
            ar.add(name);

        }
        fields = ar.toArray(new String[0]);
        final String[][] data = new String[iCount][fields.length];

        for (int i = 0; i < featureCollection.size(); i++) {
            Feature feat = featureCollection.getFeatures().get(i);
            for (int j = 0; j < schema.getAttributeCount(); j++) {
                if (feat.getSchema().getAttributeType(j) != AttributeType.GEOMETRY) {
                    data[i][j] = feat.getAttribute(j).toString();
                } else {

                    String geomType = feat.getGeometry().getClass().getName();
                    int dotPos = geomType.lastIndexOf(".");
                    if (dotPos > 0) {
                        geomType = geomType.substring(dotPos + 1);
                    }

                    data[i][j] = geomType.toUpperCase();
                }

            }

        }

        model.setDataVector(data, fields);
        return model;
    }

}
