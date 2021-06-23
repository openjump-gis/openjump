/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This class implements extensions to JUMP and is
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
/***********************************************
 * created on 		19.10.2007
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 
 * 
 ***********************************************/
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.openjump.core.ui.plot.Plot2DPanelOJ;
import org.openjump.sextante.gui.additionalResults.AdditionalResults;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

public class CreateScatterPlotPlugIn extends AbstractPlugIn implements
        ThreadedPlugIn {

    private String sScatterPlot = "Scatter-Plot";

    private String CLAYER = "select layer";
    private String ATTRIBUTEA = "Select-attribute-for-east-axis";
    private String ATTRIBUTEB = "Select-attribute-for-north-axis";
    private Layer selLayer = null;
    private FeatureCollection fc = null;
    private String selAttributeA = null;
    private String selAttributeB = null;
    private String sName = "Create Scatter Plot";
    private String sWrongDataType = "Wrong datatype of chosen attribute";

    /**
     * this method is called on the startup by JUMP/OpenJUMP. We set here the
     * menu entry for calling the function.
     */
    @Override
    public void initialize(PlugInContext context) throws Exception {

        ATTRIBUTEA = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.statistics.CreateScatterPlotPlugIn.Select-attribute-for-east-axis");
        ATTRIBUTEB = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.statistics.CreateScatterPlotPlugIn.Select-attribute-for-north-axis");
        CLAYER = GenericNames.SELECT_LAYER;
        sScatterPlot = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.statistics.CreateScatterPlotPlugIn.Scatter-Plot");
        sName = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.statistics.CreateScatterPlotPlugIn");
        sWrongDataType = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.statistics.CreateBarPlotPlugIn.Wrong-datatype-of-chosen-attribute");

        final FeatureInstaller featureInstaller = new FeatureInstaller(
                context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(this, new String[] {
                MenuNames.TOOLS, MenuNames.STATISTICS, MenuNames.PLOT }, sName
                + "...", false, // checkbox
                null, // icon
                createEnableCheck(context.getWorkbenchContext()));
    }

    /**
     * This method is used to define when the menu entry is activated or
     * disabled. In this example we allow the menu entry to be usable only if
     * one layer exists.
     */
    public static MultiEnableCheck createEnableCheck(
            WorkbenchContext workbenchContext) {
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(checkFactory
                        .createWindowWithAssociatedTaskFrameMustBeActiveCheck());
    }

    /**
     * this function is called by JUMP/OpenJUMP if one clicks on the menu entry.
     * It is called before the "run" method and useful to do all the GUI
     * /user-input things In this example we call two additional methods
     * {@link #setDialogValues(MultiInputDialog, PlugInContext)} and
     * {@link #getDialogValues(MultiInputDialog)} to obtain the Layer and the
     * buffer radius by the user.
     */
    @Override
    public boolean execute(PlugInContext context) throws Exception {

        reportNothingToUndoYet(context);

        final MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), sName, true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);

        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
        createPlot(context, selLayer);

    }

    private void setDialogValues(final MultiInputDialog dialog,
            PlugInContext context) {

        dialog.addLayerComboBox(CLAYER, context.getCandidateLayer(0),
                context.getLayerManager());

        final List<String> attributes = AttributeTypeFilter.NUMERIC_FILTER
                .filter(context.getCandidateLayer(0));
        final String attA = attributes.size() > 0 ? attributes.get(0) : null;
        final String attB = attributes.size() > 0 ? attributes.get(0) : null;
        final JComboBox<String> jcb_attributeA = dialog.addComboBox(ATTRIBUTEA,
                attA, attributes, ATTRIBUTEA);
        if (attributes.size() == 0) {
            jcb_attributeA.setEnabled(false);
        }
        final JComboBox<String> jcb_attributeB = dialog.addComboBox(ATTRIBUTEB,
                attB, attributes, ATTRIBUTEB);
        if (attributes.size() == 0) {
            jcb_attributeB.setEnabled(false);
        }

        dialog.getComboBox(CLAYER).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
                        .filter(dialog.getLayer(CLAYER));
                if (list.size() == 0) {
                    jcb_attributeA.setModel(new DefaultComboBoxModel<>(
                            new String[0]));
                    jcb_attributeA.setEnabled(false);
                }
                jcb_attributeA.setModel(new DefaultComboBoxModel<>(list
                        .toArray(new String[0])));
            }
        });

        dialog.getComboBox(CLAYER).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<String> list = AttributeTypeFilter.NUMERIC_FILTER
                        .filter(dialog.getLayer(CLAYER));
                if (list.size() == 0) {
                    jcb_attributeB.setModel(new DefaultComboBoxModel<>(
                            new String[0]));
                    jcb_attributeB.setEnabled(false);
                }
                jcb_attributeB.setModel(new DefaultComboBoxModel<>(list
                        .toArray(new String[0])));
            }
        });

    }

    private void getDialogValues(MultiInputDialog dialog) {
        selLayer = dialog.getLayer(CLAYER);
        fc = selLayer.getFeatureCollectionWrapper();
        selAttributeA = dialog.getText(ATTRIBUTEA);
        selAttributeB = dialog.getText(ATTRIBUTEB);
    }

    private boolean createPlot(final PlugInContext context, Layer selLayer)
            throws Exception {

        final FeatureSchema fs = fc.getFeatureSchema();
        final AttributeType typeA = fs.getAttributeType(selAttributeA);
        final AttributeType typeB = fs.getAttributeType(selAttributeB);
        if ((typeA != AttributeType.DOUBLE && typeA != AttributeType.INTEGER && typeA != AttributeType.LONG)
                || (typeB != AttributeType.DOUBLE
                        && typeB != AttributeType.INTEGER && typeB != AttributeType.LONG)) {
            context.getWorkbenchFrame().warnUser(sWrongDataType);
            return false;
        }

        final double[][] data = new double[2][fc.size()];
        final int[] fID = new int[fc.size()];
        int i = 0;
        for (final Feature f : fc.getFeatures()) {
            fID[i] = f.getID();
            final Object valA = f.getAttribute(selAttributeA);
            final Object valB = f.getAttribute(selAttributeB);
            if (valA instanceof Number) { // triplecheck as typeA is numeric
                data[0][i] = ((Number) valA).doubleValue();
            }
            if (valB instanceof Number) { // triplecheck as typeB is numeric
                data[1][i] = ((Number) valB).doubleValue();
            }
            i++;
        }

        final Plot2DPanelOJ plot = new Plot2DPanelOJ();
        plot.addScatterPlotOJ(sScatterPlot, data, fID, context, selLayer);
        plot.plotToolBar.setVisible(true);
        plot.setAxisLabel(0, selAttributeA);
        plot.setAxisLabel(1, selAttributeB);

        AdditionalResults.addAdditionalResultAndShow(sScatterPlot, plot);
        // FrameView fv = new FrameView(plot);
        // -- replace the upper line by:
        /*
         * JInternalFrame frame = new JInternalFrame(sScatterPlot);
         * frame.setLayout(new BorderLayout()); frame.add(plot,
         * BorderLayout.CENTER); frame.setClosable(true);
         * frame.setResizable(true); frame.setMaximizable(true);
         * frame.setSize(450, 450); frame.setVisible(true);
         * 
         * context.getWorkbenchFrame().addInternalFrame(frame);
         */
        return true;
    }

}
