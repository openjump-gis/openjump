/*
 * Created on 09.01.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 10:01:50 +0000 (Fr, 06 Okt 2006) $
 *  $Id: ExportEnvelopeAsGeometryPlugIn.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.layer;

import java.awt.Color;
import java.util.Iterator;

import javax.swing.Icon;

import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.objecttyperoles.FeatureCollectionRole;
import org.openjump.core.apitools.objecttyperoles.RoleOutline;
import org.openjump.core.rasterimage.RasterImageLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;

import de.latlon.deejump.wfs.jump.WFSLayer;

/**
 * Giuseppe Aruta 2015_01_15
 * 
 * PlugIn to export the envelope of any type of layer (Layer.class,
 * ReferencedImageStyle.class, WMSLayer.class, WFSLayer.class,
 * RasterImageLayer.class) as geometry. Layer name, layer datasource (if any)
 * and CRS (only for WMS and WFS) are saved as layer attributes
 */

public class ExportLayerableEnvelopeAsGeometryPlugIn extends AbstractPlugIn {

    private final static String SOURCE_PATH = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
    private final static String NOTSAVED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved");

    private final static String LAYER = I18N.get("ui.GenericNames.LAYER");

    public Icon getIcon() {
        return IconLoader.icon("envelope.png");
    }

    /*
     * public void initialize(PlugInContext context) throws Exception {
     * 
     * context.getFeatureInstaller().addMainMenuPlugin( this, new String[] {
     * MenuNames.PLUGINS }, // new String[] {MenuNames.PLUGINS, //
     * I18NPlug.getI18N("RasterInfo_Extension")}, getName(), false,
     * IconLoader.icon("envelope.png"),
     * createEnableCheck(context.getWorkbenchContext()));
     * 
     * }
     */
    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createExactlyNLayerablesMustBeSelectedCheck(
                        1, Layerable.class));
    }

    protected static FeatureSchema defaultSchema = null;

    public ExportLayerableEnvelopeAsGeometryPlugIn() {
        // super(new PersonalLogger(DebugUserIds.OLE));

        if (ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema == null) {
            ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema = new FeatureSchema();

            ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema.addAttribute(
                    "GEOMETRY", AttributeType.GEOMETRY);
            ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema.addAttribute(
                    LAYER, AttributeType.STRING);
            ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema.addAttribute(
                    SOURCE_PATH, AttributeType.STRING);
            ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema.addAttribute(
                    "CRS", AttributeType.STRING);

        }
    }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ExportEnvelopeAsGeometryPlugIn.Export-Envelope-As-Geometry");
    }

    String ENVELOPE = I18N.get(" ui.plugin.LayerStatisticsPlugIn.envelope")
            + "_";

    /**
     * @inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        Layerable layer = (Layerable) LayerTools.getSelectedLayerable(context,
                Layerable.class);
        Envelope e = new Envelope();
        String name = layer.getName();
        String sourceClass = "";
        String sourcePath = NOTSAVED;

        if (layer instanceof WMSLayer) {
            e = ((WMSLayer) layer).getEnvelope();

        } else if (layer instanceof WFSLayer) {
            e = ((WFSLayer) layer).getFeatureCollectionWrapper().getEnvelope();

        } else if (layer instanceof Layer) {
            if (((Layer) layer).getFeatureCollectionWrapper().isEmpty()) {
                context.getWorkbenchFrame()
                        .warnUser(
                                I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Layer-has-no-feature"));
                return false;
            } else {
                e = ((Layer) layer).getFeatureCollectionWrapper().getEnvelope();
            }

        } else if (layer instanceof RasterImageLayer) {
            e = ((RasterImageLayer) layer).getWholeImageEnvelope();
        }
        Geometry geom = new GeometryFactory().createPolygon(
                new GeometryFactory().createLinearRing(new Coordinate[] {
                        new Coordinate(e.getMinX(), e.getMinY()),
                        new Coordinate(e.getMinX(), e.getMaxY()),
                        new Coordinate(e.getMaxX(), e.getMaxY()),
                        new Coordinate(e.getMaxX(), e.getMinY()),
                        new Coordinate(e.getMinX(), e.getMinY()) }), null);

        FeatureCollection newFeaturecollection = new FeatureDataset(
                (FeatureSchema) ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema
                        .clone());

        BasicFeature feature = new BasicFeature(
                (FeatureSchema) ExportLayerableEnvelopeAsGeometryPlugIn.defaultSchema
                        .clone());

        feature.setAttribute("GEOMETRY", geom);
        feature.setAttribute(LAYER, name);
        if (layer instanceof WMSLayer) {
            feature.setAttribute(SOURCE_PATH, ((WMSLayer) layer).getServerURL());
            feature.setAttribute("CRS", ((WMSLayer) layer).getSRS());
        } else if (layer instanceof WFSLayer) {
            feature.setAttribute(SOURCE_PATH, ((WFSLayer) layer).getServerURL());
            feature.setAttribute("CRS", ((WFSLayer) layer).getCrs());
        } else if (layer instanceof RasterImageLayer) {
            sourcePath = ((RasterImageLayer) layer).getImageFileName();
            feature.setAttribute(SOURCE_PATH, sourcePath);
            feature.setAttribute("CRS", null);

        } else if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) == null) {

            DataSourceQuery dsq = ((Layer) layer).getDataSourceQuery();
            if (dsq != null) {
                String dsqSourceClass = dsq.getDataSource().getClass()
                        .getName();
                if (sourceClass.equals(""))
                    sourceClass = dsqSourceClass;
                Object fnameObj = dsq.getDataSource().getProperties()
                        .get("File");
                if (fnameObj == null) {
                    fnameObj = dsq.getDataSource().getProperties()
                            .get(DataStoreDataSource.CONNECTION_DESCRIPTOR_KEY);
                }
                if (fnameObj != null) {
                    sourcePath = fnameObj.toString();
                }
            }

            feature.setAttribute(SOURCE_PATH, sourcePath);
            feature.setAttribute("CRS", null);

        } else if (layer instanceof Layer
                && ((Layer) layer).getStyle(ReferencedImageStyle.class) != null) {

            FeatureCollection featureCollection = ((Layer) layer)
                    .getFeatureCollectionWrapper();
            for (Iterator i = featureCollection.iterator(); i.hasNext();) {
                Feature feat = (Feature) i.next();
                sourcePath = (String) feat.getString("IMG_URI");
                sourcePath = sourcePath.substring(5);

            }

            feature.setAttribute(SOURCE_PATH, sourcePath);
            feature.setAttribute("CRS", null);
        }

        newFeaturecollection.add(feature);

        addLayer(
                I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ExportEnvelopeAsGeometryPlugIn.Geometry")
                        + "_" + name, newFeaturecollection, context,
                new RoleOutline(), Color.yellow);

        return false;
    }

    public static Layer addLayer(String title,
            FeatureCollection featCollection, PlugInContext context,
            FeatureCollectionRole role, Color color) {
        return LayerTools.addStandardResultLayer(title, featCollection, color,
                context, role);
    }

    WorkbenchContext workbenchContext;

}
