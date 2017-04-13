package org.openjump.core.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openjump.core.apitools.IOTools;
import org.openjump.core.geomutils.GeoUtils;
import org.openjump.core.rasterimage.RasterImageIOUtils;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.WorldFileHandler;
import org.openjump.core.rasterimage.styler.SLDHandler;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
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
     * RasterImageLayer.class
     * 
     * @return true if selected sextante raster (RasterImageLayer.class) is
     *         Temporary layer (layer stored into TEMP folder)
     */
    public static boolean isTemporary(RasterImageLayer layer) {
        if (layer.getImageFileName().contains(
                System.getProperty("java.io.tmpdir"))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * RasterImageLayer.class
     * 
     * @return true if selected sextante raster (RasterImageLayer.class) has
     *         been modified
     */
    public static boolean isModified(RasterImageLayer layer) {
        if (layer.isRasterDataChanged()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * RasterImageLayer.class
     * 
     * @return true if selected sextante raster layer is monoband layer
     */
    public static boolean isMonoband(RasterImageLayer layer) {
        if (layer.getNumBands() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * RasterImageLayer.class
     *
     * @return the File path of a sextante raster (RasterImageLayer.class) eg.
     *         C/File/imagename.tif
     */
    public static String getFilePath(RasterImageLayer layer) {
        String fileName = null;
        if (!layer.getImageFileName().contains(
                System.getProperty("java.io.tmpdir"))) {
            fileName = layer.getImageFileName();
        } else {
            fileName = NODATASOURCELAYER;
        }
        return fileName;
    }

    /*
     * Methods for Layer.class
     */

    /**
     * Layer.class
     * 
     * @return true if the layer (Layer.class) is a temporary layer Both layers
     *         in memory and layes stored into TEMP folder are considered as
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
     * @return true if the layer (Layer.class) has been modified
     */
    public static boolean isModified(Layer layer) {
        if (layer.isFeatureCollectionModified()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
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
     * @return true if the layer (Layer.class) belongs form an image file (eg.
     *         JPG, TIF, ECW)
     */
    public static boolean isImage(Layer layer) {
        if (layer.getStyle(ReferencedImageStyle.class) != null) {

            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true if the Layer (Layer.class) is a collection of Image layers
     *         (eg. JPG, TIF, ECW)
     */
    public static boolean isMultipleImages(Layer layer) {
        if (layer.getStyle(ReferencedImageStyle.class) != null
                && layer.getFeatureCollectionWrapper().getFeatures().size() > 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true if the layer (Layer.class) is a datastore layer (eg. Oracle,
     *         SpatiaLite, MySQL)
     */
    public static boolean isDataStore(Layer layer) {
        if (layer.getDataSourceQuery().getDataSource() instanceof DataStoreQueryDataSource) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true if the layer is a system Layer currently Fence and Measure
     *         Layers
     */
    public static boolean isSystem(Layer layer) {
        if (layer.equals(SystemLayerFinder.class)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true Check if the layer is a cad Layer following DXF PlugIn
     *         schema it defines Cad layer with the presence of COLOR and TEXT
     *         attributes
     */
    public static boolean isCad(Layer layer) {
        if (layer.getFeatureCollectionWrapper().getFeatureSchema()
                .hasAttribute("COLOR")
                && layer.getFeatureCollectionWrapper().getFeatureSchema()
                        .hasAttribute("TEXT")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true if the layer is empty
     */

    public static boolean isEmpty(Layer layer) {
        FeatureCollectionWrapper fcw = layer.getFeatureCollectionWrapper();
        if (fcw.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Layer.class
     * 
     * @return true if the selected layer has multiple geometry types ex. points
     *         with linestrings, polygons with points, etc It exclude collection
     *         of geometries and multigeometries ex. points with multipoints,
     *         linestrings with linearing, linestrings with multilinestrings,
     *         etc
     */

    public static boolean isMixedGeometryType(Layer layer) {
        FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        @SuppressWarnings("unchecked")
        List<Feature> featureList = featureCollection.getFeatures();
        BitSet layerBit = new BitSet();
        BitSet currFeatureBit = new BitSet();
        if (featureList.size() > 0) {
            Geometry firstGeo = ((Feature) featureList.iterator().next())
                    .getGeometry();
            layerBit = GeoUtils.setBit(layerBit, firstGeo); // this is the layer
                                                            // type
        }
        for (Iterator<Feature> i = featureList.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            Geometry geo = feature.getGeometry();
            currFeatureBit = GeoUtils.setBit(currFeatureBit, geo);
        }
        if ((layerBit.get(GeoUtils.pointBit) && currFeatureBit
                .get(GeoUtils.lineBit))
                || (layerBit.get(GeoUtils.polyBit) && currFeatureBit
                        .get(GeoUtils.lineBit))
                || (layerBit.get(GeoUtils.pointBit) && currFeatureBit
                        .get(GeoUtils.polyBit))) {
            return true;
        } else {
            return false;
        }
    }

    public static String getGeometryType(Layer layer) {
        String geoClass = "";
        FeatureCollectionWrapper fcw = layer.getFeatureCollectionWrapper();
        int numFeatures = fcw.size();
        Geometry geo = null;
        boolean multipleGeoTypes = false;
        for (@SuppressWarnings("unchecked")
        Iterator<Feature> i = fcw.getFeatures().iterator(); i.hasNext();) {
            geo = ((Feature) i.next()).getGeometry();
            if (geo != null) {
                if (geoClass.equals(""))
                    geoClass = geo.getClass().getName();
                else if (!geo.getClass().getName().equals(geoClass))
                    multipleGeoTypes = true;
            }
        }
        if (geoClass.equals(""))
            geoClass = NULL_GEOMETRIES;

        if (numFeatures == 0)
            geoClass = NO_FEATURES;
        else {
            if (multipleGeoTypes) {
                geoClass = MULTIPLE_GEOMETRY_TYPES;
            } else {
                int dotPos = geoClass.lastIndexOf(".");
                if (dotPos > 0)
                    geoClass = geoClass.substring(dotPos + 1);
            }
        }

        return geoClass;
    }

    /**
     * @return the File path of a Layer.class eg. C/File/vectorname.shp
     */
    public static String getFilePath(Layer layer) {
        DataSourceQuery dsq = layer.getDataSourceQuery();
        String fileName = null;
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
     * @return the source class of a Layer.class eg. C/File/vectorname.shp
     */

    public static String getLayerSourceClass(Layer layer) {

        String sourceClass = "";
        DataSourceQuery dsq = layer.getDataSourceQuery();
        if (dsq != null) {
            String dsqSourceClass = dsq.getDataSource().getClass().getName();
            if (sourceClass.equals(""))
                sourceClass = dsqSourceClass;
            int dotPos = sourceClass.lastIndexOf(".");
            if (dotPos > 0)
                sourceClass = sourceClass.substring(dotPos + 1);
            dotPos = sourceClass.lastIndexOf("$");
            if (dotPos > 0)
                sourceClass = sourceClass.substring(dotPos + 1);
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
     * @return the type of the file as string
     */
    public static String getFileType(File file) {
        TypeFile extension = TypeFile.valueOf(FileUtil.getExtension(file));
        if (!extension.equals(TypeFile.values()))
            filetype = FileUtil.getExtension(file).toUpperCase();
        else {
            switch (extension) {
            case ASC: {
                filetype = "ASC - ESRI ASCII grid";
                break;
            }
            case CSV: {
                filetype = "CSV - Comma-separated values";
                break;
            }
            case DXF: {
                filetype = "Autocad DXF - Drawing Exchange Format";
                break;
            }
            case FLT: {
                filetype = "FLT - ESRI Binary grid";
                break;
            }
            case TIF: {
                filetype = "GEOTIF/TIFF -  Tagged Image File Format";
                break;
            }
            case TIFF: {
                filetype = "GEOTIF/TIFF -  Tagged Image File Format";
                break;
            }
            case JPG: {
                filetype = "JPEG/JPG - Joint Photographic Experts Group";
                break;
            }
            case JPEG: {
                filetype = "JPEG/JPG - Joint Photographic Experts Group";
                break;
            }
            case PNG: {
                filetype = "PNG - Portable Network Graphics";
                break;
            }
            case GIF: {
                filetype = "GIF - Graphics Interchange Format";
                break;
            }
            case GRD: {
                filetype = "GRD - Surfer ASCII Grid";
                break;
            }
            case JP2: {
                filetype = "JPEG 2000 - Joint Photographic Experts Group";
                break;
            }
            case BMP: {
                filetype = "BMP - Windows Bitmap";
                break;
            }
            case ECW: {
                filetype = "ECW - Enhanced Compression Wavelet";
                break;
            }
            case MrSID: {
                filetype = "MrSID - Multiresolution seamless image database";
                break;
            }
            case TXT: {
                filetype = "TXT - Text file";
                break;
            }
            case SHP: {
                filetype = "SHP - Esri Shapefile";
                break;
            }
            case JML: {
                filetype = "JML - OpenJUMP JML format";
                break;
            }
            case GML: {
                filetype = "GML - Geography Markup Language";
                break;
            }
            case KML: {
                filetype = "KML - Keyhole Markup Language";
                break;
            }
            case OSM: {
                filetype = "OSM - OpenStreetMap XML";
                break;
            }
            }
        }
        return filetype;
    }

    /*
     * Get the extension of the file
     */
    public static String getExtension(File file) {
        String ext = null;
        String s = file.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toUpperCase();
        }
        return ext;
    }

    /**
     *
     * @param layer
     * @return a description of the vector file ex. "SHP - ESRI Shapefile" if
     *         the file extension is not into the enum list it returns the
     *         extension (eg. "DWG")
     */

    public static String getVectorImageFileDescription(Layer layer) {
        String name = null;
        DataSourceQuery dsq = layer.getDataSourceQuery();
        Object fnameObj = dsq.getDataSource().getProperties().get("File");
        String sourcePath = fnameObj.toString();
        File file = new File(sourcePath);
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
        String name = null;
        File file = new File(layer.getImageFileName());
        name = getFileType(file);
        return name;
    }

    /**
     * Export vector layer as SHP or JML depending to the geometries
     * 
     * @param context
     * @param layer
     * @param path
     */
    // TODO: to check if vector layer is multigeometry and to divide it into
    // primitives collections (Point, Linestring and Polygon)
    public static void ExportVector(PlugInContext context, Layer layer,
            String path) {
        FeatureCollection features = layer.getFeatureCollectionWrapper();
        String vector_name = context.getLayerManager().uniqueLayerName(
                FileUtil.getFileNameFromLayerName(layer.getName()));
        // remove extension if any (ex. for layer image.png, will remove png
        int dotPos = vector_name.indexOf(".");
        if (dotPos > 0)
            vector_name = vector_name.substring(0, dotPos);
        File vfileName;
        if (isMixedGeometryType(layer)) {
            vfileName = FileUtil.addExtensionIfNone(new File(vector_name),
                    "jml");
            String vpath = new File(path, vfileName.getName())
                    .getAbsolutePath();
            try {
                IOTools.saveJMLFile(features, vpath);
            } catch (Exception te) {
            }
        } else {
            vfileName = FileUtil.addExtensionIfNone(new File(vector_name),
                    "shp");
            String vpath = new File(path, vfileName.getName())
                    .getAbsolutePath();
            try {
                IOTools.saveShapefile(features, vpath);
            } catch (Exception te) {
            }
        }
    }

    /**
     * Export Sextante Raster layerable to TIF
     * 
     * @param context
     * @param layer
     * @param path
     */

    public static void ExportSextanteRaster(PlugInContext context,
            RasterImageLayer layer, String path) {
        Envelope envelope = layer.getWholeImageEnvelope();
        String outTIF_name = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".tif");
        File outTIF_File = new File(path.concat(File.separator).concat(
                outTIF_name));
        try {
            RasterImageIOUtils.saveTIF(outTIF_File, layer, envelope);
        } catch (Exception te) {
        }

    }

    /**
     * Export Image: simple method to save an image file, loaded to a folde by
     * Layer.class, with corresponding worldfile, if exists
     * 
     * @param context
     * @param layer
     * @param path
     * @throws IOException
     */

    public static void ExportImage(PlugInContext context, Layer layer,
            String path) throws IOException {
        String filepath = filepath(layer);
        File inputFile = new File(filepath);
        String outImage_name = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".tif");
        File outFile = new File(path.concat(File.separator).concat(
                outImage_name));
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(inputFile).getChannel();
            outputChannel = new FileOutputStream(outFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
        WorldFileHandler worldFileHandler = new WorldFileHandler(filepath,
                false);
        if (worldFileHandler.isWorldFileExistentForImage() != null) {
            copyWorldFile(layer, filepath);

        }

    }

    /**
     * Export Vector layer styles as SLD This part of the code derives from
     * de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn.class and it has been
     * modified to automatically export SLD files for vectors
     * 
     * @param context
     * @param layer
     * @param path
     * @throws IOException
     */

    public static void ExportVectorStyleToSLD(PlugInContext context,
            Layer layer, String path) throws Exception {
        double internalScale = 1d / context.getLayerViewPanel().getViewport()
                .getScale();
        double realScale = ScreenScale.getHorizontalMapScale(context
                .getLayerViewPanel().getViewport());
        double scaleFactor = internalScale / realScale;
        String outSLD = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".sld");

        File sld_outFile = new File(path.concat(File.separator).concat(outSLD));
        File inputXML = File.createTempFile("temptask", ".xml");
        inputXML.deleteOnExit();
        String name = layer.getName();
        // TODO don't assume has 1 item!!!
        // Should create this condition in EnableCheckFactory
        if (layer.getFeatureCollectionWrapper().getFeatures().size() == 0) {
            throw new Exception(
                    I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Selected-layer-is-empty"));
        }
        BasicFeature bf = (BasicFeature) layer.getFeatureCollectionWrapper()
                .getFeatures().get(0);
        Geometry geo = bf.getGeometry();
        String geoType = geo.getGeometryType();
        Java2XML java2Xml = new Java2XML();
        java2Xml.write(layer, "layer", inputXML);
        FileInputStream input = new FileInputStream(inputXML);
        // FileWriter fw = new FileWriter( outputXML );
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
                sld_outFile), "UTF-8");
        HashMap<String, String> map = new HashMap<String, String>(9);
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
        d = d != null ? d : new Double(0);
        map.put("minScale",
                ""
                        + LayerStyle2SLDPlugIn.toRealWorldScale(scaleFactor,
                                d.doubleValue()));
        // using Double.MAX_VALUE is creating a large number - too many 0's
        // make it simple and hardcde a large number
        final double largeNumber = 99999999999d;
        d = layer.getMaxScale();
        d = d != null ? d : new Double(largeNumber);
        map.put("maxScale",
                ""
                        + LayerStyle2SLDPlugIn.toRealWorldScale(scaleFactor,
                                d.doubleValue()));
        fw.write(LayerStyle2SLDPlugIn.transformContext(input, map));
        fw.close();
    }

    /**
     * Export RasterImage layer styles as SLD
     * 
     * @param context
     * @param rLayer
     * @param path
     */
    public static void ExportRasterStyleToSLD(PlugInContext context,
            RasterImageLayer rLayer, String path) {
        String outSLD = context.getLayerManager().uniqueLayerName(
                rLayer.getName() + ".sld");
        File sld_outFile = new File(path.concat(File.separator).concat(outSLD));
        try {
            SLDHandler.write(rLayer.getSymbology(), null, sld_outFile);
        } catch (Exception te) {
        }

    }

    /**
     * Export .prj projection auxiliary file for vector/image layer
     * 
     * @param context
     * @param layer
     * @param proj
     *            OGC wkt projection code
     * @param path
     *            to export
     * @throws IOException
     */
    public static void ExportVectorProjection(PlugInContext context,
            Layer layer, String proj, String path) throws IOException {
        String outPRJ = "";
        File outFile = null;
        outPRJ = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".prj");
        outFile = new File(path.concat(File.separator).concat(outPRJ));
        FileUtils.writeStringToFile(outFile, proj);
    }

    /**
     * Export .prj projection auxiliary file for sextante raster image
     * 
     * @param context
     * @param layer
     * @param proj
     *            OGC wkt projection code
     * @param path
     *            to export
     * @throws IOException
     */
    public static void ExportRasterProjection(PlugInContext context,
            RasterImageLayer layer, String proj, String path)
            throws IOException {
        String outPRJ = "";
        File outFile = null;
        outPRJ = context.getLayerManager().uniqueLayerName(
                layer.getName() + ".prj");
        outFile = new File(path.concat(File.separator).concat(outPRJ));
        FileUtils.writeStringToFile(outFile, proj);
    }

    /**
     * Simple method to copy a file (as worldfile)
     */
    public static void copyWorldFile(Layer layer, String path)
            throws IOException {
        // String path=
        // jTextField_Output.getText().concat(File.separator).concat("raster");
        String wfilepath = worldFilepath(layer);
        File inputFile = new File(wfilepath);
        File outFile = new File(path.concat(File.separator).concat(
                inputFile.getName()));
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(inputFile).getChannel();
            outputChannel = new FileOutputStream(outFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    /**
     * Return the path of the file associated to a ReferencedImage layer /* EX.
     * c:\folder\image.tif
     * 
     * @param layer
     * @return file path
     */
    public static String filepath(Layer layer) {
        String sourcePathImage = null;
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String filePath1 = null;
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            sourcePathImage = (String) feature
                    .getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            File f = new File(sourcePathImage);
            String filePath = f.getAbsolutePath();
            filePath1 = filePath.replace("%20", " ");
        }
        return filePath1;
    }

    /**
     * Return path/name of a worldfile associated to a ReferencedImage layer /*
     * Ex. if c:\folder\image.tif exists, it returns c:\folder\image.tfw
     * 
     * @param layer
     * @return
     */
    public static String worldFilepath(Layer layer) {
        String sourcePathImage = null;
        FeatureCollection featureCollection = layer
                .getFeatureCollectionWrapper();
        String worldPath = null;
        for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            sourcePathImage = (String) feature
                    .getString(ImageryLayerDataset.ATTR_URI);
            sourcePathImage = sourcePathImage.substring(5);
            File f = new File(sourcePathImage);
            String filePath = f.getAbsolutePath();
            String filePath1 = filePath.replace("%20", " ");
            String worldFileName = filePath1.substring(0,
                    filePath1.lastIndexOf("."));
            String imageExtension = filePath1.substring(
                    filePath1.lastIndexOf(".") + 1).toLowerCase();
            worldPath = worldFileName + "." + imageExtension.substring(0, 1)
                    + imageExtension.substring(imageExtension.length() - 1)
                    + "w";
        }
        return worldPath;
    }

}
