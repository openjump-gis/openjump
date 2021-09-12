/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import java.util.Collection;

import com.vividsolutions.jump.feature.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;

import javax.swing.*;


public class UnionPlugIn extends AbstractPlugIn implements ThreadedPlugIn {

    private final String LAYER = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.layer");
    private final String SELECTED_ONLY = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.selected-features-only");
    private final String LEGACY_ALGO = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.legacy-algo");
    private final String LEGACY_ALGO_TT = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.legacy-algo-tt");
    private final String OVERLAY_NG_ALGO = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.overlay-ng-algo");
    private final String OVERLAY_NG_ALGO_TT = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.overlay-ng-algo-tt");
    private final String FLOATING_PRECISION_MODEL = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.floating-precision-model");
    private final String FLOATING_PRECISION_MODEL_TT = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.floating-precision-model-tt");
    private final String FIXED_PRECISION_MODEL = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.fixed-precision-model");
    private final String FIXED_PRECISION_MODEL_TT = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.fixed-precision-model-tt");
    private final String PRECISION = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.fixed-precision-model");
    private final String PRECISION_TT = I18N.JUMP.get("ui.plugin.analysis.UnionPlugIn.fixed-precision-model-tt");

    private boolean useSelected = false;
    private boolean overlayNgAlgo = true;
    private boolean legacyAlgo = false;
    private boolean floatingPrecision = true;
    private boolean fixedPrecision = false;
    private double precision = 0.001;
    private MultiInputDialog dialog;

    
    public UnionPlugIn() {
    }
    
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
        featureInstaller.addMainMenuPlugin(this,
            new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
            this.getName() + "...", false, null,
            createEnableCheck(context.getWorkbenchContext()));  
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                        .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }
    
    public boolean execute(PlugInContext context) throws Exception {
    	  //[sstein, 16.07.2006] put here again to load correct language
        //[mmichaud 2007-05-20] move to UnionPlugIn constructor to load the string only once
        //LAYER = I18N.getInstance().get("ui.plugin.analysis.UnionPlugIn.layer");
        //Unlike ValidatePlugIn, here we always call #initDialog because we want
        //to update the layer comboboxes. [Jon Aquino]
        int n = context.getLayerViewPanel().getSelectionManager()
            .getFeaturesWithSelectedItems().size();
        useSelected = (n > 0);
        initDialog(context);
        dialog.setVisible(true);

        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        return true;
    }

    private void initDialog(PlugInContext context) {
        dialog = new MultiInputDialog(context.getWorkbenchFrame(), I18N.getInstance().get("ui.plugin.analysis.UnionPlugIn.union"), true);

        //dialog.setSideBarImage(IconLoader.icon("Overlay.gif"));
        if (useSelected) {
            dialog.setSideBarDescription(
                I18N.getInstance().get("ui.plugin.analysis.UnionPlugIn.creates-a-new-layer-containing-the-union-of-selected-features-in-the-input-layer"));
        }
        else {
            dialog.setSideBarDescription(
                I18N.getInstance().get("ui.plugin.analysis.UnionPlugIn.creates-a-new-layer-containing-the-union-of-all-the-features-in-the-input-layer"));
        }
        if (useSelected) {
            dialog.addLabel(SELECTED_ONLY);
        }
        else {
            dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
        }
        JRadioButton overlayNgAlgoRB = dialog
            .addRadioButton(OVERLAY_NG_ALGO,"ALGO_TYPE",overlayNgAlgo,OVERLAY_NG_ALGO_TT);
        JRadioButton legacyAlgoRB = dialog
            .addRadioButton(LEGACY_ALGO,"ALGO_TYPE",legacyAlgo,LEGACY_ALGO_TT);
        JRadioButton floatingPrecisionRB = dialog
            .addRadioButton(FLOATING_PRECISION_MODEL,"MODEL",floatingPrecision, FLOATING_PRECISION_MODEL_TT);
        JRadioButton fixedPrecisionRB = dialog
            .addRadioButton(FIXED_PRECISION_MODEL,"MODEL",fixedPrecision,FIXED_PRECISION_MODEL_TT);
        JTextField precisionModelTF = dialog
            .addDoubleField(PRECISION,precision, 12, PRECISION_TT);
        legacyAlgoRB.addActionListener(e -> {
            floatingPrecisionRB.setEnabled(!legacyAlgoRB.isSelected());
            fixedPrecisionRB.setEnabled(!legacyAlgoRB.isSelected());
            precisionModelTF.setEnabled(!legacyAlgoRB.isSelected() && fixedPrecisionRB.isSelected());
        });
        overlayNgAlgoRB.addActionListener(e -> {
            floatingPrecisionRB.setEnabled(overlayNgAlgoRB.isSelected());
            fixedPrecisionRB.setEnabled(overlayNgAlgoRB.isSelected());
            precisionModelTF.setEnabled(overlayNgAlgoRB.isSelected() && fixedPrecisionRB.isSelected());
        });
        floatingPrecisionRB.addActionListener(e ->
            precisionModelTF.setEnabled(!floatingPrecisionRB.isSelected()));
        fixedPrecisionRB.addActionListener(e ->
            precisionModelTF.setEnabled(fixedPrecisionRB.isSelected()));

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        legacyAlgo = dialog.getBoolean(LEGACY_ALGO);
        overlayNgAlgo = dialog.getBoolean(OVERLAY_NG_ALGO);
        floatingPrecision = dialog.getBoolean(FLOATING_PRECISION_MODEL);
        fixedPrecision = dialog.getBoolean(FIXED_PRECISION_MODEL);
        precision = dialog.getDouble(PRECISION);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        FeatureCollection a;
        Collection<Feature> inputC;
        if (useSelected) {
            inputC = context.getLayerViewPanel()
                            .getSelectionManager()
                            .getFeaturesWithSelectedItems();
            FeatureSchema featureSchema = new FeatureSchema();
            featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
            a = new FeatureDataset(inputC, featureSchema);
        }
        else {
            a = dialog.getLayer(LAYER).getFeatureCollectionWrapper();
        }
        
        Collection<Geometry> geoms = FeatureUtil.toGeometries(a.getFeatures());
        Geometry g;
        if (legacyAlgo) {
            g = UnaryUnionOp.union(geoms);
        } else {
            if (floatingPrecision) {
                g = OverlayNGRobust.union(geoms);
            } else {
                g = UnaryUnionNG.union(geoms, new PrecisionModel(precision));
            }
        }
        geoms.clear();
        geoms.add(g);
        FeatureCollection fc = FeatureDatasetFactory.createFromGeometry(geoms);
        
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        context.addLayer(StandardCategoryNames.RESULT, I18N.getInstance().get("ui.plugin.analysis.UnionPlugIn.union"), fc);
    }
    
}
