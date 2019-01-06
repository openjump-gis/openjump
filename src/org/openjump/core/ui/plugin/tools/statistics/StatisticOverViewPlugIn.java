/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.StatisticIndices;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import de.fho.jump.pirol.utilities.attributes.AttributeInfo;

/**
 * PlugIn that gives a quick impression on the value ranges, means (modes) and
 * the deviation of the layer features' attribute values.
 *
 * @author Ole Rahn, sstein modified: [sstein] 16.Feb.2009 adaptation for
 *         OpenJUMP
 */
public class StatisticOverViewPlugIn extends AbstractPlugIn {

    private static final String sStatisticsOverview = I18N
            .get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Attribute-Statistics-Overview");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        final WorkbenchContext workbenchContext = context.getWorkbenchContext();
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] { MenuNames.TOOLS, MenuNames.STATISTICS },
                getName() + "...", false, null,
                createEnableCheck(workbenchContext));
    }

    @Override
    public String getName() {
        return sStatisticsOverview;
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return "statsOverview.png";
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean execute(PlugInContext context) throws Exception {

        final Layer layer = context.getSelectedLayer(0);
        final FeatureCollection featureColl = layer
                .getFeatureCollectionWrapper().getWrappee();
        final Feature[] features = FeatureCollectionTools
                .FeatureCollection2FeatureArray(featureColl);

        // mmichaud fix bug 3229892 (2011-04-05)
        if (features.length == 0) {
            context.getWorkbenchFrame()
                    .warnUser(
                            I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Selected-layer-is-empty"));
            return false;
        }
        // final StatisticOverViewTableModel tableModel = new StatisticOverViewTableModel(
        //       features);

        // -- [Giuseppe Aruta 2018-3-12] Statistic table is opened into
        // AdditionalResult frame
        // final JTable table = new JTable(tableModel);
        // table.setRowHeight(22);

        // final JScrollPane scrollPane = new JScrollPane(table);
        // table.setPreferredScrollableViewportSize(new Dimension(500, 300));

        // AdditionalResults.addAdditionalResultAndShow(getName(), scrollPane);

        // StatisticOverViewDialog dialog = new
        // StatisticOverViewDialog(context.getWorkbenchFrame(), this.getName(),
        // false, features);

        // dialog.setVisible(true);
       
        AdditionalResults.addAdditionalResultAndShow(getName(),
                pan(features, 3));
        return true;

    }

    private static JScrollPane pan(Feature[] features, int scale) {
        final DefaultTableModel dtm = new DefaultTableModel();
        final JTable jTable = new JTable();

        jTable.setGridColor(Color.WHITE);
        jTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                final Component c = super.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? Color.white : new Color(230,
                        230, 230));
                if (isSelected) {
                    c.setBackground(Color.black);
                }
                return c;
            };
        });

        jTable.setModel(dtm);
        jTable.setEnabled(true);

        final Feature feat = features[0];
        final FeatureSchema fs = feat.getSchema();

        final AttributeInfo[] attrInfos = AttributeInfo
                .schema2AttributeInfoArray(fs);

        // we don't use the Geometry
        final String[] attrToWorkWith = new String[attrInfos.length - 1];
        int saveAttrIndex = 0;

        for (int i = 0; i < attrInfos.length; i++) {
            if (!attrInfos[i].getAttributeType().equals(AttributeType.GEOMETRY)) {
                attrToWorkWith[saveAttrIndex] = attrInfos[i].getAttributeName();
                saveAttrIndex++;
            }
        }

        saveAttrIndex = 0;

        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.queries.SimpleQuery.attribute"));
        dtm.addColumn(I18N
                .get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Attribute-type"));

        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.minimum"));
        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewTableModel.mean-mode"));
        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.maximum"));
        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.standard-dev"));
        dtm.addColumn(I18N
                .get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.sum"));

        for (final AttributeInfo attrInfo : attrInfos) {
            if (attrInfo.getAttributeType().equals(AttributeType.GEOMETRY)) {
                continue;
            }
            if (FeatureCollectionTools.isAttributeTypeNumeric(attrInfo
                    .getAttributeType())) {
                // numeric

                final StatisticIndices stat = FeatureCollectionTools
                        .getStatistics(features, fs,
                                attrInfo.getAttributeName());

                dtm.addRow(new Object[] {
                        attrInfo.getAttributeName(),
                        attrInfo.getAttributeType(),
                        new BigDecimal(stat.getMin()).setScale(scale,
                                RoundingMode.CEILING),
                        new BigDecimal(stat.getMean()).setScale(scale,
                                RoundingMode.CEILING),
                        new BigDecimal(stat.getMax()).setScale(scale,
                                RoundingMode.CEILING),
                        new BigDecimal(stat.getStdDev()).setScale(scale,
                                RoundingMode.CEILING),
                        new BigDecimal(stat.getSum()).setScale(scale,
                                RoundingMode.CEILING) });

            } else {
                final Object[] meansModes = FeatureCollectionTools
                        .getMeanOrModeForAttributes(features, attrToWorkWith);

                dtm.addRow(new Object[] { attrInfo.getAttributeName(),
                        attrInfo.getAttributeType(), null,
                        meansModes[saveAttrIndex], null, null, null });

            }
            saveAttrIndex++;
        }

        final JScrollPane pane = new JScrollPane(jTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return pane;

    }

    public MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayersMustBeSelectedCheck(1));
    }
}
