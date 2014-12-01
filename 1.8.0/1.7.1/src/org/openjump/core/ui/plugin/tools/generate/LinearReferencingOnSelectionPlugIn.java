package org.openjump.core.ui.plugin.tools.generate;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import org.openjump.core.ui.images.IconLoader;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * Create point along a selected linear geometry from a distance
 * and an offset values.
 * One can also repeat points at regular interval.
 */
public class LinearReferencingOnSelectionPlugIn extends AbstractLinearReferencingPlugIn {

    private static final String KEY = LinearReferencingOnSelectionPlugIn.class.getName();

    public LinearReferencingOnSelectionPlugIn() {
        super(I18N.get(KEY), IconLoader.icon("linearref_selection.png"));
    }

    private String categoryName = StandardCategoryNames.RESULT;

    public void setCategoryName(String value) {
        categoryName = value;
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE, MenuNames.TOOLS_LINEARREFERENCING},
                getName()+"...", false, getIcon(),
                createEnableCheck(context.getWorkbenchContext())
        );
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(checkFactory.createAtLeastNFeaturesMustBeSelectedCheck(1));
    }

    public boolean execute(PlugInContext context) throws Exception {

        super.execute(context);

        MultiTabInputDialog dialog = new MultiTabInputDialog(
                context.getWorkbenchFrame(), getName(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        return true;

    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception{
        monitor.allowCancellationRequests();
        FeatureSchema featureSchema = new FeatureSchema();
        featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        featureSchema.addAttribute("LAYER", AttributeType.STRING);
        featureSchema.addAttribute("PATH", AttributeType.STRING);
        featureSchema.addAttribute("NUM", AttributeType.INTEGER);
        featureSchema.addAttribute("DISTANCE", AttributeType.DOUBLE);
        featureSchema.addAttribute("OFFSET", AttributeType.DOUBLE);
        FeatureCollection resultFC = new FeatureDataset(featureSchema);

        Collection<Layer> layers = context.getLayerViewPanel().getSelectionManager().getLayersWithSelectedItems();
        for (Layer layer : layers) {
            Collection<Feature> features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems(layer);
            for (Feature feature : features) {
                if (feature.getGeometry().getDimension() == 1) {
                    setPointsAlong(resultFC, layer.getName(), Integer.toString(feature.getID()), feature.getGeometry());
                }
                else if (feature.getGeometry().getDimension() == 2) {
                    setPointsAlong(resultFC, layer.getName(), Integer.toString(feature.getID()), feature.getGeometry().getBoundary());
                }
                // do nothing if geometry is a point
            }
        }
        context.addLayer(categoryName, "Linear-Referencing", resultFC);
    }

    private void setDialogValues(final MultiTabInputDialog dialog, PlugInContext context) {
        dialog.setSideBarDescription(DESCRIPTION);
        dialog.addSubTitle(DISTANCE_UNIT);
        dialog.addRadioButton(MAP_UNIT, "UNIT", map_unit, MAP_UNIT_TOOLTIP);
        dialog.addRadioButton(LINESTRING_FRACTION, "UNIT", linestring_fraction, LINESTRING_FRACTION_TOOLTIP);
        dialog.addSubTitle(DISTANCE_AND_OFFSET);
        dialog.addDoubleField(DISTANCE, distance, 6, DISTANCE_TOOLTIP);
        dialog.addDoubleField(OFFSET, offset, 6, OFFSET_TOOLTIP);
        dialog.addSeparator();
        final JCheckBox repeatCheckBox = dialog.addCheckBox(REPEAT, repeat, null);
        final JTextField repeatDistanceTextField = dialog.addDoubleField(REPEAT_DISTANCE, repeat_distance, 6, null);
        final JCheckBox addEndPointCheckBox = dialog.addCheckBox(ADD_END_POINT, add_end_point, null);
        dialog.addSeparator();
        repeatCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repeatDistanceTextField.setEnabled(repeatCheckBox.isSelected());
                addEndPointCheckBox.setEnabled(repeatCheckBox.isSelected());
            }
        });
    }

    private void getDialogValues(MultiInputDialog dialog) {
        map_unit = dialog.getBoolean(MAP_UNIT);
        linestring_fraction = dialog.getBoolean(LINESTRING_FRACTION);
        distance = dialog.getDouble(DISTANCE);
        offset = dialog.getDouble(OFFSET);
        repeat = dialog.getBoolean(REPEAT);
        repeat_distance = dialog.getDouble(REPEAT_DISTANCE);
        if (repeat_distance == 0) repeat = false;
        add_end_point = dialog.getBoolean(ADD_END_POINT);
    }

}