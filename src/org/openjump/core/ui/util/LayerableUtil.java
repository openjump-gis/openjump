package org.openjump.core.ui.util;

import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.Logger;
import org.apache.commons.io.FileUtils;
import org.openjump.core.apitools.IOTools;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;
import org.openjump.core.rasterimage.styler.SLDHandler;
import org.openjump.sigle.utilities.geom.FeatureCollectionUtil;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.SystemLayerFinder;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreQueryDataSource;

import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;

public abstract class LayerableUtil {
    private final static String NO_FEATURES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.No-Features"); // no
                                                                                         // features
                                                                                         // were
                                                                                         // found
    private final static String NULL_GEOMETRIES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Null-Geometries");
    private final static String MULTIPLE_GEOMETRY_TYPES = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Multiple-geometry-types"); // mixed
    private final static String NODATASOURCELAYER = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.nodatasourcelayer.message");

    /*
     * methods for RasterImageLayer.class
     */

    /**
     * Boolean to verify if a &lt;RasterImageLayer&gt; rLayer1 is spatially equivalent
     * to another &lt;RasterImageLayer&gt; rLayer2
     * @param rLayer1 first layer
     * @param rLayer2 second layer
     * @return true if they are equivalent. False if they are not equivalent
     * @throws IOException if an IOException occurs during layer reading
     */
    public static boolean isSpatiallyEqualTo(RasterImageLayer rLayer1,
            RasterImageLayer rLayer2) throws IOException {

        boolean isEqual = true;
        final Raster raster1 = rLayer1.getRasterData(null);
        final Raster raster2 = rLayer2.getRasterData(null);
        if (raster1.getHeight() != raster2.getHeight()) {
            isEqual = false;
        }
        if (raster1.getWidth() != raster2.getWidth()) {
            isEqual = false;
        }
        if (rLayer1.getMetadata().getActualLowerLeftCoord() != rLayer2
                .getMetadata().getActualLowerLeftCoord()) {
            isEqual = false;

        }
        if (rLayer1.getMetadata().getActualCellSize() != rLayer2.getMetadata()
                .getActualCellSize()) {
            isEqual = false;
        }
        if (rLayer1.getNoDataValue() != rLayer2.getNoDataValue()) {
            isEqual = false;
        }

        return isEqual;

    }

    /**
     * RasterImageLayer.class
     *
     * @param layer the layer to test
     * @return true if selected sextante raster (RasterImageLayer.class) is
     *         Temporary layer (layer stored into TEMP folder)
     */
    public static boolean isTemporary(RasterImageLayer layer) {
        return layer.getImageFileName().contains(
                System.getProperty("java.io.tmpdir"));
    }

    /**
     * RasterImageLayer.class
     *
     * @param layer the layer to test
     * @return true if selected sextante raster (RasterImageLayer.class) has
     *         been modified
     */
    public static boolean isModified(RasterImageLayer layer) {
        return layer.isRasterDataChanged();
    }

    /**
     * RasterImageLayer.class
     *
     * @param layer the RasterImageLayer to test
     * @return true if selected sextante raster layer is monoband layer
     */
    public static boolean isMonoband(RasterImageLayer layer) {
        return layer.getNumBands() == 1;
    }

    /**
     * RasterImageLayer.class
     *
     * @param layer the layer to query
     * @return the File path of a sextante raster (RasterImageLayer.class) eg.
     *         C/File/imagename.tif
     */
    public static String getFilePath(RasterImageLayer layer) {
        String fileName;
        if (!layer.getImageFileName().contains(
                System.getProperty("java.io.tmpdir"))) {
            fileName = layer.getImageFileName();
        } else {
            fileName = NODATASOURCELAYER;
        }
        return fileName;
    }


    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the layer (Layer.class) is a temporary layer Both layers
     *         in memory and layers stored into TEMP folder are considered as
     *         "Temporary layers"
     */
    public static boolean isTemporary(Layer layer) {
        if (!layer.hasReadableDataSource()
                || layer.getName().contains(
                        System.getProperty("java.io.tmpdir"))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param layer the layer to test
     * @return true if the layer (Layer.class) has been modified
     */
    public static boolean isModified(Layer layer) {
        return layer.isFeatureCollectionModified();
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the layer (Layer,class) is a vector layer Eg. layer with
     *         datastore belonging to Shapefile, JML or GML file). This method
     *         excludes Datastores and Image file loaded by Layer,class
     */
    public boolean isVector(Layer layer) {
        if (!(layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource)
                && ((layer.getStyle(ReferencedImageStyle.class) == null))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the layer (Layer.class) belongs form an image file (eg.
     *         JPG, TIF, ECW)
     */
    public static boolean isImage(Layer layer) {
        return layer.getStyle(ReferencedImageStyle.class) != null;
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the Layer (Layer.class) is a collection of Image layers
     *         (eg. JPG, TIF, ECW)
     */
    public static boolean isMultipleImages(Layer layer) {
        return layer.getStyle(ReferencedImageStyle.class) != null
                && layer.getFeatureCollectionWrapper().getFeatures().size() > 1;
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the layer (Layer.class) is a datastore layer (eg. Oracle,
     *         SpatiaLite, MySQL)
     */
    public static boolean isDataStore(Layer layer) {
        return layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource;
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true Check if the layer is a cad Layer following DXF PlugIn
     *         schema it defines Cad layer with the presence of COLOR and TEXT
     *         attributes
     */
    public static boolean isCad(Layer layer) {
        FeatureSchema schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        return schema.hasAttribute("COLOR")
                && schema.hasAttribute("TEXT");
    }

    /**
     * Layer.class
     *
     * @param layer the layer to test
     * @return true if the layer is empty
     */
    public static boolean isEmpty(Layer layer) {
        return layer.getFeatureCollectionWrapper().isEmpty();
    }

    /**
     * check if selected layer has only polygons and/or multipolygons
     * 
     * @param layer the layer to test
     * @return boolean - true if the vector layer has only polygon/multipolygon
     *         geometries (Shapefile model)
     */
    public static boolean isPolygonalLayer(Layer layer) {
        final FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 2;
    }

    /**
     * check if selected featureCollection has only polygons and/or
     * multipolygons
     * 
     * @param featureCollection the feature collection to test
     * @return boolean - true if the vector layer has only polygon/multipolygon
     *         geometries (Shapefile model)
     */
    public static boolean isPolygonalLayer(FeatureCollection featureCollection) {
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 2;
    }

    /**
     * Check if selected layer has only points and/or multipoints
     * 
     * @param layer the layer to test
     * @return boolean - true if the vector layer has only point/multipoint
     *         geometries (Shapefile model)
     */
    public static boolean isPointLayer(Layer layer) {
        final FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 0;
    }

    /**
     * Check if selected featureCollection has only points and/or multipoints
     * 
     * @param featureCollection the feature collection to test
     * @return boolean - true if the vector layer has only point/multipoint
     *         geometries (Shapefile model)
     */
    public static boolean isPointLayer(FeatureCollection featureCollection) {
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 0;
    }

    /**
     * Check if selected layer has only linestring and/or multilinestring and/or
     * multilinearings
     * 
     * @param layer the layer to test
     * @return boolean - true if the vector layer has only
     *         LineString/MultiLineString geometries (Shapefile model)
     */
    public static boolean isLinealLayer(Layer layer) {
        final FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 1;

    }

    /**
     * Check if selected FeatureCollection has only linestring and/or
     * multilinestring and/or multilinearings (Shapefile model)
     * 
     * @param featureCollection the feature collection to test
     * @return boolean - true if the vector layer has only
     *         LineString/MultiLineString geometries (Shapefile model)
     */
    public static boolean isLinealLayer(FeatureCollection featureCollection) {
        return FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == 1;
    }

    /**
     * Check if selected layer has geoemtries of different types
     *
     * @param layer the layer to test
     * @return true if the selected layer has features of different geometries
     *         types (Shapefile model) (Shapefile model)
     */

    public static boolean isMixedGeometryType(Layer layer) {
        final FeatureCollection fc = layer.getFeatureCollectionWrapper();
        return isMixedGeometryType(fc);
    }

    /**
     * Check if selected FeatureCollection has geoemtries of different types
     *
     * @param featureCollection the feature collection to test
     * @return true if the selected layer has features of different geometries
     *         types (Shapefile model)
     */

    public static boolean isMixedGeometryType(
            FeatureCollection featureCollection) {

        if ((FeatureCollectionUtil
                .getFeatureCollectionDimension(featureCollection) == -1)
                && featureCollection.size() > 0) {

            return true;
        } else {
            return false;
        }
    }

    public static String getGeometryType(Layer layer) {
        String geoClass = "";
        final FeatureCollectionWrapper fcw = layer
                .getFeatureCollectionWrapper();
        final int numFeatures = fcw.size();
        Geometry geo;
        boolean multipleGeoTypes = false;
        for (final Feature feature : fcw.getFeatures()) {
            geo = feature.getGeometry();
            if (geo != null) {
                if (geoClass.equals("")) {
                    geoClass = geo.getClass().getName();
                } else if (!geo.getClass().getName().equals(geoClass)) {
                    multipleGeoTypes = true;
                }
            }
        }
        if (geoClass.equals("")) {
            geoClass = NULL_GEOMETRIES;
        }

        if (numFeatures == 0) {
            geoClass = NO_FEATURES;
        } else {
            if (multipleGeoTypes) {
                geoClass = MULTIPLE_GEOMETRY_TYPES;
            } else {
                final int dotPos = geoClass.lastIndexOf(".");
                if (dotPos > 0) {
                    geoClass = geoClass.substring(dotPos + 1);
                }
            }
        }

        return geoClass;
    }

    /**
     *
     * @param layer the layer from which to get the file path
     * @return the File path of a Layer.class eg. C/File/vectorname.shp
     */
    public static String getFilePath(Layer layer) {
        final DataSourceQuery dsq = layer.getDataSourceQuery();
        String fileName = null;
        // TODO Should it be && instead of || ?
        if (dsq != null
                || !layer.getName().contains(
                        System.getProperty("java.io.tmpdir"))) {
            Object fnameObj = dsq.getDataSource().getProperties().get("File");
            if (fnameObj == null) {
                fnameObj = dsq.getDataSource().getProperties()
                        .get(DataStoreDataSource.CONNECTION_DESCRIPTOR_KEY);
            }
            if (fnameObj != null) {
                fileName = fnameObj.toString();
            }
        } else {
            fileName = NODATASOURCELAYER;
        }

        return fileName;
    }

    /**
     * @param layer the Layer from which to get the DataSource class
     * @return the source class of a Layer.class eg. C/File/vectorname.shp
     */

    public static String getLayerSourceClass(Layer layer) {

        String sourceClass = "";
        final DataSourceQuery dsq = layer.getDataSourceQuery();
        if (dsq != null) {
            final String dsqSourceClass = dsq.getDataSource().getClass()
                    .getName();
            if (sourceClass.equals("")) {
                sourceClass = dsqSourceClass;
            }
            int dotPos = sourceClass.lastIndexOf(".");
            if (dotPos > 0) {
                sourceClass = sourceClass.substring(dotPos + 1);
            }
            dotPos = sourceClass.lastIndexOf("$");
            if (dotPos > 0) {
                sourceClass = sourceClass.substring(dotPos + 1);
            }
        } else {
            sourceClass = "In memory";
        }
        return sourceClass;
    }

    public enum TypeFile {
        ASC, CSV, DXF, FLT, TIF, TIFF, JPG, JPEG, PNG, GIF, GRD, JP2, BMP, ECW, MrSID, TXT, SHP, JML, GML, KML, OSM
    }

    public static String filetype;

    /**
     * input file
     *
     * @param file the file whose type we want to know
     * @return the type of the file as string
     */
    public static String getFileType(File file) {
      String extUC = FileUtil.getExtension(file).toUpperCase();
      switch (extUC) {
        case "ASC": {
          return "ASC - ESRI ASCII grid";
        }
        case "CSV": {
          return "CSV - Comma-separated values";
        }
        case "DXF": {
          return "Autocad DXF - Drawing Exchange Format";
        }
        case "FLT": {
          return "FLT - ESRI Binary grid";
        }
        case "TIF":
        case "TIFF": {
          return "GEOTIF/TIFF -  Tagged Image File Format";
        }
        case "JPG":
        case "JPEG": {
          return "JPEG/JPG - Joint Photographic Experts Group";
        }
        case "PNG": {
          return "PNG - Portable Network Graphics";
        }
        case "GIF": {
          return "GIF - Graphics Interchange Format";
        }
        case "GRD": {
          return "GRD - Surfer ASCII Grid";
        }
        case "JP2": {
          return "JPEG 2000 - Joint Photographic Experts Group";
        }
        case "BMP": {
          return "BMP - Windows Bitmap";
        }
        case "ECW": {
          return "ECW - Enhanced Compression Wavelet";
        }
        case "MrSID": {
          return "MrSID - Multiresolution seamless image database";
        }
        case "TXT": {
          return "TXT - Text file";
        }
        case "SHP": {
          return "SHP - Esri Shapefile";
        }
        case "JML": {
          return "JML - OpenJUMP JML format";
        }
        case "GML": {
          return "GML - Geography Markup Language";
        }
        case "KML": {
          return "KML - Keyhole Markup Language";
        }
        case "OSM": {
          return "OSM - OpenStreetMap XML";
        }
      }
      return extUC;
    }

    ///**
    // * Get the extension of the file
    // * @deprecated already in FileUtil
    // */
    //public static String getExtension(File file) {
    //    String ext = null;
    //    final String s = file.getName();
    //    final int i = s.lastIndexOf('.');
    //    if (i > 0 && i < s.length() - 1) {
    //        ext = s.substring(i + 1).toUpperCase();
    //    }
    //    return ext;
    //}

    /**
     *
     * @param layer the Layer whose description we want to know
     * @return a description of the vector file ex. "SHP - ESRI Shapefile" if
     *         the file extension is not into the enum list it returns the
     *         extension (eg. "DWG")
     */

    public static String getVectorImageFileDescription(Layer layer) {
        String name = null;
        final DataSourceQuery dsq = layer.getDataSourceQuery();
        final Object fnameObj = dsq.getDataSource().getProperties().get("File");
        final String sourcePath = fnameObj.toString();
        final File file = new File(sourcePath);
        name = getFileType(file);
        return name;

    }

    /**
     * @param layer
     *            the Raster image layer
     * @return a description of raster image layer ex.
     *         "GEOTIF/TIFF - Tagged Image File Format" if the file extension is
     *         not into the enum list it returns only the extension (eg. "MAP")
     */
    public static String getRasterFileDescription(RasterImageLayer layer) {
        String name;
        final File file = new File(layer.getImageFileName());
        name = getFileType(file);
        return name;
    }

    /**
     * Export vector layer as SHP or JML depending on the geometries
     * 
     * @param context the plugin context
     * @param layer a layer
     * @param path the path to export to
     */
    // TODO: to check if vector layer is multigeometry and to divide it into
    // primitives collections (Point, Linestring and Polygon)
    public static void ExportVector(PlugInContext context, Layer layer,
            String path) {
        final FeatureCollection features = layer.getFeatureCollectionWrapper();
        String vector_name = context.getLayerManager().uniqueLayerName(
                FileUtil.getFileNameFromLayerName(layer.getName()));
        // remove extension if any (ex. for layer image.png, will remove png
        final int dotPos = vector_name.indexOf(".");
        if (dotPos > 0) {
            vector_name = vector_name.substring(0, dotPos);
        }
        File vfileName;
        if (isMixedGeometryType(layer)) {
            vfileName = FileUtil.addExtensionIfNone(new File(vector_name),
                    "jml");
            final String vpath = new File(path, vfileName.getName())
                    .getAbsolutePath();
            try {
                IOTools.saveJMLFile(features, vpath);
            } catch (final Exception te) {
                Logger.warn("Error while saving layer " + layer.getName() +
                        " to " + path + " as JML", te);
            }
        } else {
            vfileName = FileUtil.addExtensionIfNone(new File(vector_name),
                    "shp");
            final String vpath = new File(path, vfileName.getName())
                    .getAbsolutePath();
            try {
                IOTools.saveShapefile(features, vpath);
            } catch (final Exception te) {
                Logger.warn("Error while saving layer " + layer.getName() +
                        " to " + path + " as Shapefile", te);
            }
        }
    }

    /**
     * Export Sextante Raster layerable to TIF
     * 
     * @param context plugin context
     * @param layer the RasterImageLayer to export
     * @param path the path to export to
     */

    public static void ExportSextanteRaster(PlugInContext context,
            RasterImageLayer layer, String path) {
        final Envelope envelope = layer.getWholeImageEnvelope();
        final String outTIF_name = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".tif");
        final File outTIF_File = new File(path.concat(File.separator).concat(
                outTIF_name));
        try {
            RasterImageIOUtils.saveTIF(outTIF_File, layer, envelope);
        } catch (final Exception te) {
            Logger.warn("Error while saving RasterImageLayer '" +
                    layer.getName() + "' to " + outTIF_name, te);
        }

    }

    /**
     * Export Image: simple method to save an image file, loaded to a folde by
     * Layer.class, with corresponding worldfile, if exists
     * 
     * @param context the plugin context
     * @param layer the image layer to export
     * @param path the path to export the image to
     * @throws IOException if an IOException occurs during export
     */

    public static void ExportImage(PlugInContext context, Layer layer,
            String path) throws IOException {
        final String filepath = filepath(layer);
        final File inputFile = new File(filepath);
        final String outImage_name = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".tif");
        final File outFile = new File(path.concat(File.separator).concat(
                outImage_name));
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(inputFile).getChannel();
            outputChannel = new FileOutputStream(outFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null)
                inputChannel.close();
            if (outputChannel != null)
                outputChannel.close();
        }
        final WorldFileHandler worldFileHandler = new WorldFileHandler(
                filepath, false);
        if (worldFileHandler.isWorldFileExistentForImage() != null) {
            copyWorldFile(layer, filepath);

        }

    }

    /**
     * Export Vector layer styles as SLD This part of the code derives from
     * de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn.class and it has been
     * modified to automatically export SLD files for vectors
     * 
     * @param context the plugin context
     * @param layer the layer whose style to export
     * @param path the path to export to
     * @throws IOException if an IOException occurs during SLD writing
     */
    public static void ExportVectorStyleToSLD(PlugInContext context,
            Layer layer, String path) throws Exception {
        final double internalScale = 1d / context.getLayerViewPanel()
                .getViewport().getScale();
        final double realScale = ScreenScale.getHorizontalMapScale(context
                .getLayerViewPanel().getViewport());
        final double scaleFactor = internalScale / realScale;
        final String outSLD = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".sld");

        final File sld_outFile = new File(path.concat(File.separator).concat(
                outSLD));
        final File inputXML = File.createTempFile("temptask", ".xml");
        inputXML.deleteOnExit();
        final String name = layer.getName();
        // TODO don't assume has 1 item!!!
        // Should create this condition in EnableCheckFactory
        if (layer.getFeatureCollectionWrapper().getFeatures().size() == 0) {
            throw new Exception(
                    I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Selected-layer-is-empty"));
        }
        final BasicFeature bf = (BasicFeature) layer
                .getFeatureCollectionWrapper().getFeatures().get(0);
        final Geometry geo = bf.getGeometry();
        final String geoType = geo.getGeometryType();
        final Java2XML java2Xml = new Java2XML();
        java2Xml.write(layer, "layer", inputXML);
        final FileInputStream input = new FileInputStream(inputXML);
        // FileWriter fw = new FileWriter( outputXML );
        final OutputStreamWriter fw = new OutputStreamWriter(
                new FileOutputStream(sld_outFile), StandardCharsets.UTF_8);
        final HashMap<String, String> map = new HashMap<>(9);
        map.put("wmsLayerName", name);
        map.put("featureTypeStyle", name);
        map.put("styleName", name);
        map.put("styleTitle", name);
        map.put("geoType", geoType);
        map.put("geomProperty", I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"));
        map.put("Namespace", "http://www.deegree.org/app");
        // map.put("NamespacePrefix", prefix + ":");
        // map.put("NamespacePrefixWithoutColon", prefix);
        // ATENTION : note that min and max are swapped in JUMP!!!
        // will swap later, in transformContext
        Double d = layer.getMinScale();
        d = d != null ? d : 0.0;
        map.put("minScale",
                "" + LayerStyle2SLDPlugIn.toRealWorldScale(scaleFactor, d));
        // using Double.MAX_VALUE is creating a large number - too many 0's
        // make it simple and hardcde a large number
        final double largeNumber = 99999999999d;
        d = layer.getMaxScale();
        d = d != null ? d : new Double(largeNumber);
        map.put("maxScale",
                "" + LayerStyle2SLDPlugIn.toRealWorldScale(scaleFactor, d));
        fw.write(LayerStyle2SLDPlugIn.transformContext(input, map));
        fw.close();
    }

    /**
     * Export RasterImage layer styles as SLD
     * 
     * @param context the plugin context
     * @param rLayer the RasterImageLayer to export
     * @param path the path to export to
     */
    public static void ExportRasterStyleToSLD(PlugInContext context,
            RasterImageLayer rLayer, String path) {
        final String outSLD = context.getLayerManager().uniqueLayerName(
                rLayer.getName() + ".sld");
        final File sld_outFile = new File(path.concat(File.separator).concat(
                outSLD));
        try {
            SLDHandler.write(rLayer.getSymbology(), null, sld_outFile);
        } catch (final Exception te) {
            Logger.warn("Error while saving layer " + rLayer.getName() +
                    " to " + path + " as SLD", te);
        }

    }

    /**
     * Export .prj projection auxiliary file for vector/image layer
     * 
     * @param context the plugin context
     * @param layer the layer to export
     * @param proj OGC wkt projection code
     * @param path the path to export to
     * @throws IOException if an IOException occurs during export
     */
    public static void ExportVectorProjection(PlugInContext context,
            Layer layer, String proj, String path) throws IOException {
        String outPRJ = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".prj");
        File outFile = new File(path.concat(File.separator).concat(outPRJ));
        FileUtils.writeStringToFile(outFile, proj, charSet);
    }

    /**
     * Export .prj projection auxiliary file for sextante raster image
     * 
     * @param context the plugin context
     * @param layer the rasterImageLayer
     * @param proj OGC wkt projection code
     * @param path the path to export to
     * @throws IOException if an IOException occurs during export
     */
    public static void ExportRasterProjection(PlugInContext context,
            RasterImageLayer layer, String proj, String path)
            throws IOException {
        String outPRJ = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".prj");
        File outFile = new File(path.concat(File.separator).concat(outPRJ));
        FileUtils.writeStringToFile(outFile, proj, charSet);
    }

    final static String charSet = "UTF-8";

    /**
     * Simple method to copy a file (as worldfile)
     * @param layer the image layer whose worldfile we want to copy
     * @param path the path to copy to
     * @throws IOException if an IOException occurs during world file writing
     */
    public static void copyWorldFile(Layer layer, String path)
            throws IOException {
        // String path=
        // jTextField_Output.getText().concat(File.separator).concat("raster");
        final String wfilepath = worldFilepath(layer);
        final File inputFile = new File(wfilepath);
        final File outFile = new File(path.concat(File.separator).concat(
                inputFile.getName()));
        try (FileChannel inputChannel = new FileInputStream(inputFile).getChannel();
             FileChannel outputChannel = new FileOutputStream(outFile).getChannel()) {
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        }
    }

    /**
     * Return the path of the file associated to a ReferencedImage layer /* EX.
     * c:\folder\image.tif
     * 
     * @param layer the Layer whose file path we want to know
     * @return file path of the Layer
     */
    public static String filepath(Layer layer) {
        String sourcePathImage;
        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String filePath1 = null;
        for (final Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            final Feature feature = (Feature) i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            final File f = new File(sourcePathImage);
            final String filePath = f.getAbsolutePath();
            filePath1 = filePath.replace("%20", " ");
        }
        return filePath1;
    }

    /**
     * Return path/name of a worldfile associated to a ReferencedImage layer /*
     * Ex. if c:\folder\image.tif exists, it returns c:\folder\image.tfw
     * 
     * @param layer the layer whose worldfile path we want to know
     * @return the path of the worldfile associated to the layer
     */
    public static String worldFilepath(Layer layer) {
        String sourcePathImage;
        final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String worldPath = null;
        for (final Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            final Feature feature = (Feature) i.next();
            sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            final File f = new File(sourcePathImage);
            final String filePath = f.getAbsolutePath();
            final String filePath1 = filePath.replace("%20", " ");
            final String worldFileName = filePath1.substring(0,
                    filePath1.lastIndexOf("."));
            final String imageExtension = filePath1.substring(
                    filePath1.lastIndexOf(".") + 1).toLowerCase();
            worldPath = worldFileName + "." + imageExtension.charAt(0)
                    + imageExtension.substring(imageExtension.length() - 1)
                    + "w";
        }
        return worldPath;
    }

    /**
     * Returns the area of feature collection (only Polygons or MultiPolygons)
     * in the selected Vector Layer.class.
     * This method exclude area of the layer which are not covered by feature
     * @param layer a Layer
     * @return area as a double
     */
    public static double getValidArea(Layer layer) {
      double area=0;
      final FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
       for (final Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
             final Feature feature = (Feature) i.next();
             area+= feature.getGeometry().getArea();
             }
    return area;
    }
    
    /**
     * Returns the area of selected RasterImageLayer.class.
     * This method excludes cells with no data value
     * @param layer RasterImageLayer
     * @return area as a double
     */
    public static double getValidArea(RasterImageLayer layer) throws IOException {
      Raster ras=layer.getRasterData(null);
      double noData =layer.getNoDataValue();
      double cellSize=layer.getMetadata().getOriginalCellSize();
      int counter = 0;
      int nx = ras.getWidth();
        int ny = ras.getHeight();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                double value = ras.getSampleDouble(x, y, 0);
                if (value != noData)
                    counter++;
            }
        }
    return cellSize*cellSize*counter;
    }

    public static void main(String[] args) {
      System.out.println(getFileType(new File("Test.tif")));
      System.out.println(getFileType(new File("Test.unknown")));
    }
}
