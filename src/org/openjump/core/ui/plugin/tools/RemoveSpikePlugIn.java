package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.RingVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.locationtech.jts.geom.Polygon;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by UMichael on 10/03/2016.
 */
public class RemoveSpikePlugIn extends AbstractThreadedUiPlugIn {

    public static String SOURCE_LAYER            = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.source-layer");
    public static String DESCRIPTION             = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.description");
    public static String RESULT_LAYER_SUFFIX     = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.result-layer-suffix");
    public static String DIST_TOLERANCE          = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.dist-tolerance");
    public static String DIST_TOLERANCE_TOOLTIP  = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.dist-tolerance-tooltip");
    public static String ANGLE_TOLERANCE         = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.angle-tolerance");
    public static String ANGLE_TOLERANCE_TOOLTIP = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.angle-tolerance-tooltip");
    public static String SPIKES_LOCALIZATION     = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.spikes-localisation");
    public static String ATTRIBUTE_TRANSFER      = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.attribute-transfer");
    public static String NONE                    = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.none");
    public static String ALL                     = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.all");
    public static String LOCATION_TYPE           = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.spike-location_type");
    public static String AS_LINESTRING           = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.as-linestring");
    public static String ON_SPIKE_TIP            = I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn.spike-tip");

    private Layer layerA;
    private double distTolerance = 1.0;
    private double angleTolerance = 5.0;
    //private boolean preventInvalid = true;

    private String attributeName = NONE;
    private boolean asLineString = false;
    private boolean onSpikeTip = true;

    public RemoveSpikePlugIn() {
    }

    public String getName() {
        return I18N.getInstance().get("org.openjump.core.ui.plugin.tools.RemoveSpikePlugIn");
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        super.initialize(context);
        FeatureInstaller featureInstaller = context.getFeatureInstaller();
        featureInstaller.addMainMenuPlugin(
                this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_QA},
                getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck().add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
        initDialog(dialog, context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);
        return true;
    }

    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
        dialog.setSideBarDescription(DESCRIPTION);
        Layer candidateA = layerA == null ? context.getCandidateLayer(0) : layerA;
        final JComboBox<Layer> layerComboBoxA    =
                dialog.addLayerComboBox(SOURCE_LAYER, candidateA, context.getLayerManager());
        dialog.addDoubleField(DIST_TOLERANCE, distTolerance, 8, DIST_TOLERANCE_TOOLTIP);
        dialog.addDoubleField(ANGLE_TOLERANCE, angleTolerance, 8, ANGLE_TOLERANCE_TOOLTIP);
        List<String> attributes = Arrays.asList(getAttributes(candidateA));
        final JComboBox<String> attributeTransferJCB = dialog
            .addComboBox(ATTRIBUTE_TRANSFER, NONE, attributes, ATTRIBUTE_TRANSFER);
        dialog.addSubTitle(LOCATION_TYPE);
        dialog.addRadioButton(AS_LINESTRING, LOCATION_TYPE, asLineString, AS_LINESTRING);
        dialog.addRadioButton(ON_SPIKE_TIP, LOCATION_TYPE, onSpikeTip, ON_SPIKE_TIP);
        GUIUtil.centreOnWindow(dialog);

        layerComboBoxA.addItemListener(e ->
            attributeTransferJCB.setModel(new DefaultComboBoxModel<>(getAttributes(dialog.getLayer(SOURCE_LAYER))))
        );
    }

    private String[] getAttributes(Layer layer) {
        if (layer != null) {
            FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
            List<String> attributes = new ArrayList<>(schema.getAttributeNames());
            String geomName = schema.getAttributeName(schema.getGeometryIndex());
            attributes.remove(geomName);
            attributes.add(0, ALL);
            attributes.add(0, NONE);
            return attributes.toArray(new String[0]);
        } else return new String[] {NONE};
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layerA = dialog.getLayer(SOURCE_LAYER);
        distTolerance = dialog.getDouble(DIST_TOLERANCE);
        angleTolerance = dialog.getDouble(ANGLE_TOLERANCE);
        attributeName = dialog.getText(ATTRIBUTE_TRANSFER);
        asLineString = dialog.getBoolean(AS_LINESTRING);
        onSpikeTip = dialog.getBoolean(ON_SPIKE_TIP);
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();

        // Clone layerA
        FeatureSchema srcSchema = layerA.getFeatureCollectionWrapper().getFeatureSchema();
        FeatureCollection result1 = new FeatureDataset(srcSchema.clone());
        FeatureSchema spikeSchema = new FeatureSchema();
        if (attributeName.equals(ALL)) {
            spikeSchema = srcSchema.clone();
            spikeSchema.addAttribute("status", AttributeType.STRING);
        }
        else if (attributeName.equals(NONE)) {
            spikeSchema.addAttribute("geometry", AttributeType.GEOMETRY);
            spikeSchema.addAttribute("status", AttributeType.STRING);
        }
        else {
            spikeSchema.addAttribute("geometry", AttributeType.GEOMETRY);
            spikeSchema.addAttribute(attributeName,
                srcSchema.getAttributeType(srcSchema.getAttributeIndex(attributeName)));
            spikeSchema.addAttribute("status", AttributeType.STRING);
        }
        FeatureCollection result2 = new FeatureDataset(spikeSchema);
        for (Feature f : layerA.getFeatureCollectionWrapper().getFeatures()) {
            Feature feature = f.clone(true, true);
            List<Geometry> spikes = new ArrayList<>();
            Geometry newGeom = null;
            if (feature.getGeometry() instanceof GeometryCollection) {
                newGeom = removeSpike((GeometryCollection) feature.getGeometry(),
                        distTolerance, angleTolerance, spikes);
            } else if (feature.getGeometry() instanceof Polygon) {
                newGeom = removeSpike((Polygon) feature.getGeometry(),
                        distTolerance, angleTolerance, spikes);
            }
            boolean isValid = false;
            if (newGeom != null && newGeom.isValid()) {
                feature.setGeometry(newGeom);
                isValid = true;
            }
            if (spikes.size() > 0) {
                Feature spikesFeature = new BasicFeature(spikeSchema);
                for (int i = 0 ; i < spikeSchema.getAttributeCount() ; i++) {
                    if (spikeSchema.getAttributeType(i) == AttributeType.GEOMETRY) {
                        if (onSpikeTip) {
                            spikes = spikes.stream().map(Geometry::getInteriorPoint).collect(Collectors.toList());
                        }
                        spikesFeature.setGeometry(f.getGeometry().getFactory().buildGeometry(spikes));
                    }
                    else if (spikeSchema.getAttributeName(i).equals("status")) {
                        spikesFeature.setAttribute("status", isValid ? "Fixed" : "Not fixed");
                    }
                    else {
                        spikesFeature.setAttribute(i, f.getAttribute(spikeSchema.getAttributeName(i)));
                    }
                }
                result2.add(spikesFeature);
            }
            result1.add(feature);
        }
        workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT,
                    layerA.getName() + " - " + RESULT_LAYER_SUFFIX, result1);
        Layer locationLayer =
            workbenchContext.getLayerManager().addLayer(StandardCategoryNames.RESULT, SPIKES_LOCALIZATION, result2);
        if (locationLayer != null) {
            boolean firingEvents = locationLayer.getLayerManager().isFiringEvents();
            locationLayer.getLayerManager().setFiringEvents(false);
            try {
                if (asLineString) {
                    locationLayer.getBasicStyle().setLineWidth(3);
                    locationLayer.getBasicStyle().setLineColor(Color.RED);
                    locationLayer.getVertexStyle().setEnabled(false);
                } else if (onSpikeTip) {
                    locationLayer.getBasicStyle().setFillColor(Color.RED);
                    locationLayer.getBasicStyle().setLineColor(Color.RED);
                    locationLayer.getBasicStyle().setEnabled(false);
                    changeVertexToRing(locationLayer);
                    locationLayer.getVertexStyle().setEnabled(true);
                }
            } finally {
                locationLayer.getLayerManager().setFiringEvents(firingEvents);
            }
            locationLayer.fireAppearanceChanged();
        }
    }


    private Geometry removeSpike(GeometryCollection geomCollection,
                                 double distTolerance, double angleTolerance, List<Geometry> spikes) {
        List<Geometry> geometries = new ArrayList<>();
        for (int i = 0; i < geomCollection.getNumGeometries() ; i++) {
            Geometry g = geomCollection.getGeometryN(i);
            if (g instanceof GeometryCollection) {
                geometries.add(removeSpike((GeometryCollection) g, distTolerance, angleTolerance, spikes));
            } else if (g instanceof Polygon) {
                geometries.add(removeSpike((Polygon) g, distTolerance, angleTolerance, spikes));
            } else {
                geometries.add(g);
            }
        }
        return geomCollection.getFactory().buildGeometry(geometries);
    }


    private Polygon removeSpike(Polygon poly, double distTolerance, double angleTolerance, List<Geometry> spikes) {
        LinearRing shell = removeSpike(poly.getExteriorRing(), distTolerance, angleTolerance, spikes);
        LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
        for (int i = 0 ; i < poly.getNumInteriorRing() ; i++) {
            holes[i] = removeSpike(poly.getInteriorRingN(i), distTolerance, angleTolerance, spikes);
        }
        return poly.getFactory().createPolygon(shell, holes);
    }


    private LinearRing removeSpike(LinearRing ring, double distTolerance,
                                   double angleTolerance, List<Geometry> spikes) {
        CoordinateList cl = new CoordinateList(ring.getCoordinates(), false);
        CoordinateList newCl = new CoordinateList();
        int size = cl.size();
        // If the LinearRing has only four points, it cannot be reduced
        if (size < 5) return ring;
        boolean vertexRemoved = false;
        for (int i = 0, j = 1, k = 2 ; i < size; ) {
            Coordinate a = cl.get(i);
            Coordinate b = cl.get((j)%(size-1));
            Coordinate c = cl.get((k)%(size-1));
            PointPairDistance ppd = new PointPairDistance();
            DistanceToPoint.computeDistance(new LineSegment(a,b), c, ppd);
            double d1 = ppd.getDistance();
            DistanceToPoint.computeDistance(new LineSegment(b,c), a, ppd);
            double d2 = ppd.getDistance();
            if ((!a.equals(c) && d1 > distTolerance && d2 > distTolerance) ||
                    Angle.angleBetween(a,b,c)*180/Math.PI > angleTolerance) {
                newCl.add(cl.get((i+1)%(size-1)), false);
                i++; j++; k++;
            } else {
                spikes.add(ring.getFactory().createLineString(new Coordinate[]{a,b,c}));
                i = j; j++; k++;
                vertexRemoved = true;
            }
        }
        if (newCl.size() == size) return ring;
        else if (newCl.size() < 4) return ring;
        else {
            newCl.closeRing();
            LinearRing newLinearRing = ring.getFactory().createLinearRing(newCl.toCoordinateArray());
            if (vertexRemoved) newLinearRing = removeSpike(newLinearRing, distTolerance, angleTolerance, spikes);
            return newLinearRing;
        }
    }

    private void changeVertexToRing(Layer errorLayer) {
        errorLayer.removeStyle(errorLayer.getStyle(VertexStyle.class));
        RingVertexStyle rvStyle = new RingVertexStyle();
        rvStyle.setLineColor(Color.RED);
        rvStyle.setLineWidth(3);
        rvStyle.setSize(24);
        errorLayer.addStyle(rvStyle);
    }

}
