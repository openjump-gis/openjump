package org.openjump.core.ui.plugin.layer;

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.Iterator;

import javax.swing.Icon;

import com.vividsolutions.jump.io.datasource.Connection;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionManagerToolboxPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.ConnectionPanel;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.apitools.objecttyperoles.FeatureCollectionRole;
import org.openjump.core.apitools.objecttyperoles.RoleOutline;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.TiffTags;

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
import com.vividsolutions.jump.util.FileUtil;
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
 * PlugIn to export the envelope of any type of layerable (Layer.class,
 * ReferencedImageStyle.class, WMSLayer.class, WFSLayer.class,
 * RasterImageLayer.class) as geometry. Layer name, layer datasource (if any)
 * and CRS (only for WMS and WFS) are saved as layer attributes
 * 
 * Giuseppe Aruta 2015_01_19 If multiple layerables are selected, It will be
 * exported one geometry including all of them
 * 
 * Giuseppe Aruta 2017_11_12 Correct bug. Added output of srid for Layer and
 * RasterImageLayer
 */

public class ExportLayerableEnvelopeAsGeometryPlugIn extends AbstractPlugIn {

    private final static String SOURCE_PATH = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Source-Path");
    private final static String NOTSAVED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not-Saved");
    private final static String MULTIPLESOURCE = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-Sources");
    private final static String SRID = "SRID";
    private final static String minX = "minX";
    private final static String maxX = "maxX";
    private final static String minY = "minY";
    private final static String maxY = "maxY";

    private final static String LAYER = I18N.get("ui.GenericNames.LAYER");

    public Icon getIcon() {
        return IconLoader.icon("envelope.png");
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);

        return new MultiEnableCheck().add(
                checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck())
                .add(checkFactory.createAtLeastNLayerablesMustBeSelectedCheck(
                        1, Layerable.class));
    }


    public ExportLayerableEnvelopeAsGeometryPlugIn() { }

    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getName() {
        return I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.ExportEnvelopeAsGeometryPlugIn.Export-Envelope-As-Geometry");
    }

    String ENVELOPE = I18N.get("ui.plugin.LayerStatisticsPlugIn.envelope")
            + "_";

    /**
     * @inheritDoc
     */
    @Override
    public boolean execute(PlugInContext context) throws Exception {
        Layerable layer = LayerTools.getSelectedLayerable(context,
                Layerable.class);
        final WorkbenchContext wbcontext = context.getWorkbenchContext();
        Envelope envelope = new Envelope();

        for (Object layerable : wbcontext.getLayerableNamePanel().selectedNodes(Layerable.class)) {
            Layerable slayer = (Layerable)layerable;
            if (slayer instanceof WMSLayer) {
                envelope.expandToInclude(((WMSLayer) slayer).getEnvelope());
            } else if (slayer instanceof WFSLayer) {
                envelope.expandToInclude(((WFSLayer) slayer)
                        .getFeatureCollectionWrapper().getEnvelope());

            } else if (slayer instanceof Layer) {
                if (((Layer) slayer).getFeatureCollectionWrapper().isEmpty()) {
                    context.getWorkbenchFrame()
                            .warnUser(
                                    I18N.get("org.openjump.sigle.plugin.ReplaceValuePlugIn.Layer-has-no-feature"));
                    return false;
                } else {
                    envelope.expandToInclude(((Layer) slayer)
                            .getFeatureCollectionWrapper().getEnvelope());
                }

            } else if (slayer instanceof RasterImageLayer) {
                envelope.expandToInclude(((RasterImageLayer) slayer)
                        .getWholeImageEnvelope());
            }

        }

        Geometry geom;
        GeometryFactory gf = new GeometryFactory();
        Envelope envCopy = new Envelope(envelope);
        if (envCopy.isNull()) {
            geom = gf.createPolygon(gf.createLinearRing(new Coordinate[0]));
        } else {
            if (envCopy.getArea() == 0) {
                envCopy.expandBy(1E-6);

            }
            geom = gf.toGeometry(envCopy);
        }

        FeatureSchema schema = getFeatureSchema();
        FeatureCollection newFeaturecollection = new FeatureDataset(schema);
        BasicFeature feature = new BasicFeature(schema);

        feature.setGeometry(geom);

        int size = context.getSelectedLayerables().size(); // Get number
        String name;
        String path = NOTSAVED;
        String srid = null;
        if (size == 1 && layer != null) {
            name = layer.getName();
            if (layer instanceof WMSLayer) {
                path = ((WMSLayer)layer).getServerURL();
                srid = ((WMSLayer) layer).getSRS();
            } else if (layer instanceof WFSLayer) {
                path = ((WFSLayer)layer).getServerURL();
                srid = ((WFSLayer)layer).getCrs();
            } else if (layer instanceof RasterImageLayer) {
                path = ((RasterImageLayer)layer).getImageFileName();
                srid = getInfoProjection((RasterImageLayer)layer).getCode();
            } else if (layer instanceof Layer
                    && ((Layer) layer).getStyle(ReferencedImageStyle.class) == null) {
                DataSourceQuery dsq = ((Layer)layer).getDataSourceQuery();
                if (dsq != null && dsq.getDataSource() != null) {
                    Object fnameObj = dsq.getDataSource().getProperties().get("File");
                    if (fnameObj == null) {
                        fnameObj = dsq
                                .getDataSource()
                                .getProperties()
                                .get(DataStoreDataSource.CONNECTION_DESCRIPTOR_KEY);
                        path = ((ConnectionDescriptor)fnameObj).getParametersString();
                    } else {
                        path = fnameObj.toString().replace("%20", " ");
                    }
                }
                srid = getInfoProjection((Layer)layer).getCode();
            } else if (layer instanceof Layer
                    && ((Layer) layer).getStyle(ReferencedImageStyle.class) != null) {

                FeatureCollection featureCollection = ((Layer) layer)
                        .getFeatureCollectionWrapper();
                for (Iterator i = featureCollection.iterator(); i.hasNext();) {
                    Feature feat = (Feature) i.next();
                    path = feat.getString("IMG_URI");
                    if (path != null) {
                        path = path.substring(5)
                                .replace("%20", " ");
                    }

                }
                srid = getInfoProjection((Layer)layer).getCode();
            }
        } else {
            name = MULTIPLESOURCE;
            path = MULTIPLESOURCE;
        }
        feature.setAttribute(LAYER, name);
        feature.setAttribute(SOURCE_PATH, path);
        feature.setAttribute(SRID, srid);

        feature.setAttribute(minX, envelope.isNull() ? null : roundOff(envelope.getMinX()));
        feature.setAttribute(maxX, envelope.isNull() ? null : roundOff(envelope.getMaxX()));
        feature.setAttribute(minY, envelope.isNull() ? null : roundOff(envelope.getMinY()));
        feature.setAttribute(maxY, envelope.isNull() ? null : roundOff(envelope.getMaxY()));
        newFeaturecollection.add(feature);

        addLayer(ENVELOPE + "_" + name, newFeaturecollection, context,
                new RoleOutline(), Color.yellow);

        return false;
    }

    public static Layer addLayer(String title,
            FeatureCollection featCollection, PlugInContext context,
            FeatureCollectionRole role, Color color) {
        return LayerTools.addStandardResultLayer(title, featCollection, color,
                context, role);
    }

    private FeatureSchema getFeatureSchema() {
        FeatureSchema defaultSchema = new FeatureSchema();
        defaultSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        defaultSchema.addAttribute(LAYER, AttributeType.STRING);
        defaultSchema.addAttribute(SOURCE_PATH, AttributeType.STRING);
        defaultSchema.addAttribute(SRID, AttributeType.STRING);
        defaultSchema.addAttribute(minX, AttributeType.DOUBLE);
        defaultSchema.addAttribute(maxX, AttributeType.DOUBLE);
        defaultSchema.addAttribute(minY, AttributeType.DOUBLE);
        defaultSchema.addAttribute(maxY, AttributeType.DOUBLE);
        return defaultSchema;
    }

    // Use BigDecimal to round off decimals
    public static double roundOff(double number) {
        BigDecimal bigDecimal = new BigDecimal(number);
        BigDecimal roundedWithScale = bigDecimal.setScale(6,
                BigDecimal.ROUND_HALF_EVEN);
        double rounded = roundedWithScale.doubleValue();
        return rounded;
    }

    private SRSInfo getInfoProjection(Layer layer) throws Exception {
        SRSInfo srsInfo;
        try {
            srsInfo = ProjUtils.getSRSInfoFromLayerStyleOrSource(layer);
        } catch (Exception e) {
            srsInfo = ProjUtils.getSRSInfoFromLayerSource(layer);
        }
        if (srsInfo == null) srsInfo = new SRSInfo();
        return srsInfo;
    }

    private SRSInfo getInfoProjection(RasterImageLayer layer) throws Exception {
        String fileSourcePath = layer.getImageFileName();
        String extension = FileUtil.getExtension(fileSourcePath).toLowerCase();
        SRSInfo srsInfo;
        if (extension.equals("tif") || extension.equals("tiff")) {
            TiffTags.TiffMetadata metadata = TiffTags.readMetadata(new File(
                    fileSourcePath));
            if (metadata.isGeoTiff()) {

                srsInfo = metadata.getSRSInfo();

            } else {
                srsInfo = ProjUtils.getSRSInfoFromAuxiliaryFile(fileSourcePath);

            }
        } else {
            srsInfo = ProjUtils.getSRSInfoFromAuxiliaryFile(fileSourcePath);

        }
        if (srsInfo == null) srsInfo = new SRSInfo();
        return srsInfo;
    }

}
