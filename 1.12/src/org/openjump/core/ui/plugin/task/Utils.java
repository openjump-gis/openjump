package org.openjump.core.ui.plugin.task;

import java.awt.Component;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.openjump.core.ccordsys.utils.ProjUtils;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.TiffTags;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;

import de.latlon.deejump.wfs.jump.WFSLayer;

public class Utils {

    /**
     * Get a list of all layerables names
     * 
     * @param context
     * @return
     */
    public static List<String> getNamesOfLayerableList(PlugInContext context) {
        ArrayList<String> ListImageLayerNames = new ArrayList<String>();
        Collection<Layerable> layerables = context.getTask().getLayerManager()
                .getLayerables(Layerable.class);
        for (Iterator<Layerable> i = layerables.iterator(); i.hasNext();) {
            Layerable currentLayer = i.next();
            ListImageLayerNames.add(currentLayer.getName());
        }
        return ListImageLayerNames;
    }

    /**
     * Get layerable type (Shapefile, RasterImageLayer, WMSLayer, etc)
     * 
     * @param layer
     * @return
     */
    public static String getLayerableType(Layerable layer) {
        String sclass = "";
        sclass = layer.getClass().getSimpleName();
        /*
         * if (sclass.equals("Layer")) { Layer lay = (Layer) layer;
         * DataSourceQuery dsq = lay.getDataSourceQuery(); String dsqSourceClass
         * = dsq.getDataSource().getClass().getName(); String sourceClass =
         * dsqSourceClass; if (sourceClass.equals("")) sourceClass =
         * TaskPropertiesPlugIn.NOT_SAVED; int dotPos =
         * sourceClass.lastIndexOf("."); if (dotPos > 0) { sourceClass =
         * sourceClass.substring(dotPos + 1); } dotPos =
         * sourceClass.lastIndexOf("$"); if (dotPos > 0) { sourceClass =
         * sourceClass.substring(dotPos + 1); } sclass = sourceClass;
         * 
         * } else { sclass = layer.getClass().getSimpleName(); }
         */

        return sclass;
    }

    /**
     * Get Layerable Spatial reference system
     * 
     * @param layer
     * @return
     */
    public static String getLayerableSRS(Layerable layer) {
        String name = "";
        String sclass = layer.getClass().getSimpleName();
        if (sclass.equals("WMSLayer")) {
            WMSLayer wlyr = (WMSLayer) layer;
            name = StringUtils.remove(wlyr.getSRS(), "EPSG:");
        } else if (sclass.equals("WFSLayer")) {
            WFSLayer wlyr = (WFSLayer) layer;
            name = wlyr.getCrs();
        } else if (sclass.equals("RasterImageLayer")) {
            RasterImageLayer wlyr = (RasterImageLayer) layer;
            try {
                name = rasterProjection(wlyr);
            } catch (Exception e) {
                name = "0";
                e.printStackTrace();
            }
        } else {
            Layer wlyr = (Layer) layer;
            SRSInfo srsInfo;
            try {
                srsInfo = ProjUtils.getSRSInfoFromLayerStyleOrSource(wlyr);
                name = srsInfo.getCode();
            } catch (Exception e) {
                name = "0";
                e.printStackTrace();
            }

        }
        return name;
    }

    /*
     * Get Projection of selected raster. First it checks if selected raster is
     * a GeoTIF and scan tiff tags for projection. If selected file is not a
     * GeoTIF, it checks if <Filename>.AUX.XML exists and scans inside it. As
     * last choice it scans into <filename>.PRJ file
     */
    public static String rasterProjection(RasterImageLayer layer)
            throws Exception {
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
        String proj_coordinate = srsInfo.getCode();
        return proj_coordinate;
    }

    public static boolean isTable(Layer layer) {
        FeatureCollectionWrapper featureCollection = layer
                .getFeatureCollectionWrapper();
        @SuppressWarnings("rawtypes")
        List featureList = featureCollection.getFeatures();
        Geometry nextGeo = null;
        for (@SuppressWarnings("unchecked")
        Iterator<FeatureCollectionWrapper> i = featureList.iterator(); i
                .hasNext();) {
            Feature feature = (Feature) i.next();
            nextGeo = feature.getGeometry();
        }
        if (!featureCollection.isEmpty() && nextGeo.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check temporary Layer or RasterImageLayer
     * 
     * @param context
     * @return
     */
    public static boolean checkTemporaryLayerables(PlugInContext context) {
        List<String> temporalLayerables = new ArrayList<String>();
        Collection<RasterImageLayer> rlayers = context.getLayerManager()
                .getLayerables(RasterImageLayer.class);
        for (Iterator<RasterImageLayer> iterator = rlayers.iterator(); iterator
                .hasNext();) {
            RasterImageLayer currentLayer = iterator.next();

            if (currentLayer.getImageFileName().contains(
                    System.getProperty("java.io.tmpdir"))) {
                temporalLayerables.add(currentLayer.getName());
            }
        }
        Collection<Layer> layers = context.getLayerManager().getLayers();
        for (Iterator<Layer> iterator = layers.iterator(); iterator.hasNext();) {
            Layer currentLayer = iterator.next();
            if (!currentLayer.hasReadableDataSource()
                    || getLayerablePath(currentLayer).contains(
                            System.getProperty("java.io.tmpdir"))
                    || currentLayer.getDataSourceQuery() == null) {
                temporalLayerables.add(currentLayer.getName());
            }
        }
        if (!temporalLayerables.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Check modified layers
     * 
     * @param context
     * @return
     */
    public static boolean checkModifiedLayers(PlugInContext context) {
        List<String> modifiedLayers = new ArrayList<String>();

        Collection<Layer> layers = context.getLayerManager().getLayers();
        for (Iterator<Layer> iterator = layers.iterator(); iterator.hasNext();) {
            Layer currentLayer = iterator.next();

            if (currentLayer.hasReadableDataSource()
                    & !getLayerablePath(currentLayer).contains(
                            System.getProperty("java.io.tmpdir"))
                    & currentLayer.isFeatureCollectionModified()) {
                modifiedLayers.add(currentLayer.getName());
            }
        }
        if (!modifiedLayers.isEmpty()) {
            return true;
        }
        return false;
    }

    public static boolean checkTempAndModLayerables(PlugInContext context) {
        List<String> temporalLayerables = new ArrayList<String>();
        Collection<RasterImageLayer> rlayers = context.getLayerManager()
                .getLayerables(RasterImageLayer.class);
        for (Iterator<RasterImageLayer> iterator = rlayers.iterator(); iterator
                .hasNext();) {
            RasterImageLayer currentLayer = iterator.next();

            if (currentLayer.getImageFileName().contains(
                    System.getProperty("java.io.tmpdir"))) {
                temporalLayerables.add(currentLayer.getName());
            }
        }
        Collection<Layer> layers = context.getLayerManager().getLayers();
        for (Iterator<Layer> iterator = layers.iterator(); iterator.hasNext();) {
            Layer currentLayer = iterator.next();
            if (!currentLayer.hasReadableDataSource()
                    || getLayerablePath(currentLayer).contains(
                            System.getProperty("java.io.tmpdir"))
                    || currentLayer.getDataSourceQuery() == null) {
                temporalLayerables.add(currentLayer.getName());
            }
        }
        for (Iterator<Layer> i = context.getLayerManager()
                .getLayersWithModifiedFeatureCollections().iterator(); i
                .hasNext();) {
            Layer currentLayer = i.next();

            if (!checkTemporaryLayerables(context))
            // (!(currentLayer.getDataSourceQuery().getDataSource() instanceof
            // DataStoreQueryDataSource))
            // && (currentLayer.getStyle(ReferencedImageStyle.class) == null)
            // && (currentLayer.hasReadableDataSource())
            {
                temporalLayerables.add(currentLayer.getName());
            }
        }
        if (!temporalLayerables.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Return layerable name. (*) if vector is modified, (**) if raster or
     * vector are temporary
     * 
     * @param layer
     * @return
     */
    public static String getLayerableName(Layerable layer) {
        String name = "";
        String sclass = layer.getClass().getSimpleName();
        if (sclass.equals("RasterImageLayer")) {
            RasterImageLayer rlay = (RasterImageLayer) layer;
            if (rlay.getImageFileName().contains(
                    System.getProperty("java.io.tmpdir"))) {
                name = "(**) " + rlay.getName();
            } else {
                name = rlay.getName();
            }
        } else if (sclass.equals("WFSLayer")) {
            WFSLayer wlyr = (WFSLayer) layer;
            name = wlyr.getName();

        } else if (sclass.equals("WMSLayer")) {
            WMSLayer wlyr = (WMSLayer) layer;
            name = wlyr.getName();

        } else {

            Layer lay = (Layer) layer;
            if (!lay.hasReadableDataSource()
                    || getLayerablePath(lay).contains(
                            System.getProperty("java.io.tmpdir"))
                    || lay.getDataSourceQuery() == null) {
                name = "<font color=\"red\">(**) " + lay.getName();
            } else if (lay.hasReadableDataSource()
                    & lay.isFeatureCollectionModified()) {
                name = "<font color=\"red\">(*) " + lay.getName();
            } else if (lay.hasReadableDataSource()
                    & !lay.isFeatureCollectionModified()) {
                name = lay.getName();
            }
        }
        return name;
    }

    public static String getLayerablePath(Layerable layer) {
        String path = "";
        String sclass = layer.getClass().getSimpleName();
        if (sclass.equals("WFSLayer")) {
            try {
                WFSLayer lay = (WFSLayer) layer;
                path = lay.getServerURL();
            } catch (Exception ex) {
                path = "";
            }
        } else if (sclass.equals("WMSLayer")) {
            try {
                WMSLayer lay = (WMSLayer) layer;
                path = lay.getServerURL();
            } catch (Exception ex) {
                path = "";
            }
        } else if (sclass.equals("RasterImageLayer")) {
            try {
                RasterImageLayer lay = (RasterImageLayer) layer;
                path = lay.getFilePath();
            } catch (Exception ex) {
                path = "";
            }
        } else if (sclass.equals("ReferencedImagesLayer")) {
            try {
                Layer lay = (Layer) layer;
                FeatureCollection featureCollection = lay
                        .getFeatureCollectionWrapper();
                for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    String sourcePathImage = feature
                            .getString(ImageryLayerDataset.ATTR_URI);
                    sourcePathImage = sourcePathImage.substring(5);
                    File f = new File(sourcePathImage);
                    String filePath = f.getAbsolutePath();
                    String filePath1 = filePath.replace("%20", " ");
                    // Load all the names into name cell
                    path = filePath1;
                }
            } catch (Exception ex) {
                path = "";
            }
        } else {
            Layer lay = (Layer) layer;
            DataSourceQuery dsq = lay.getDataSourceQuery();
            if (dsq != null) {
                Object fnameObj = dsq.getDataSource().getProperties()
                        .get("File");
                if (fnameObj == null) {
                    fnameObj = dsq.getDataSource().getProperties()
                            .get("Connection Descriptor");
                }
                if (fnameObj != null) {
                    path = fnameObj.toString();
                }
            }
        }
        return path;
    }

    // The following method should be moved to org.openjump.core.ccordsys.utils
    // package
    public static Map<String, String> mapSRIDS() throws IOException {
        LinkedHashMap<String, String> localLinkedHashMap = new LinkedHashMap<String, String>();
        InputStream localInputStream = ProjUtils.class
                .getResourceAsStream("srid.txt");
        try {

            BufferedReader localBufferedReader = new BufferedReader(
                    new InputStreamReader(localInputStream));
            String str2 = null;
            String str1;
            while (null != (str1 = localBufferedReader.readLine())) {
                if (str1.startsWith("<")) {
                    int first = str1.indexOf("<");
                    int next = str1.indexOf(">", first);
                    String str3 = str1.substring(first + 1, next);
                    localLinkedHashMap.put(str3, str3);
                    if ((str2 != null) && (str2.length() > 0)) {
                        localLinkedHashMap.put(str2, str3);
                    }
                    str2 = null;
                } else {
                    str2 = null;
                }
            }
            return localLinkedHashMap;
        } finally {
            if (localInputStream != null) {
                try {
                    localInputStream.close();
                } catch (IOException localIOException2) {
                }
            }
        }
    }

    
    public static String[] mapSRIDasString() throws IOException {
      String[] arr = null;
      List<String> codes = new ArrayList<String>();
      InputStream localInputStream = ProjUtils.class
              .getResourceAsStream("srid.txt");
      try {

          BufferedReader localBufferedReader = new BufferedReader(
                  new InputStreamReader(localInputStream));

          String str1;
          while (null != (str1 = localBufferedReader.readLine())) {
              if (str1.startsWith("<")) {
                  String str3 = str1.substring(1, str1.indexOf(">"));
                  codes.add(str3);

              }
          }
          arr = codes.toArray(new String[codes.size()]);
          return arr;
      } finally {
          if (localInputStream != null) {
              try {
                  localInputStream.close();
              } catch (IOException localIOException2) {
              }
          }
      }
  }
    
    public static void removeButton(Container container) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof AbstractButton) {
                container.remove(component);
            }
        }
    }

    /**
     * Restore Task properties from file
     * 
     * @param task
     *            - current task
     * @param file
     *            - file task
     * @param workbenchFrame
     *            - current workbench
     * @throws Exception
     */

    public static void restorePropertiesFromFile(Task task, File file,
            PlugInContext context) throws Exception {
        FileReader reader = new FileReader(file);
        try {
            Task sourceTask = (Task) new XML2Java(context.getWorkbenchContext()
                    .getWorkbench().getPlugInManager().getClassLoader()).read(
                    reader, Task.class);
            task.setProperties(sourceTask.getProperties());

        } finally {
            reader.close();

        }
    }

    public static Map<QName, Object> getSavedProperties(PlugInContext context,
        File file){
    Task sourceTask = null;
    InputStream inputStream = null;
    try {
        inputStream = new FileInputStream(file);
    } catch (FileNotFoundException e1) {
       context.getWorkbenchFrame().warnUser(I18N
                .get("org.openjump.core.ui.plugin.layer.pirolraster.SaveRasterImageAsImagePlugIn.File-not-found"));
    }
    PlugInManager plugInManager = context.getWorkbenchContext()
            .getWorkbench().getPlugInManager();
    ClassLoader pluginClassLoader = plugInManager.getClassLoader();
    try {
        sourceTask = (Task) new XML2Java(pluginClassLoader).read(
                inputStream, Task.class);
        sourceTask.getProperties();
    } catch (Exception e) {
        context.getWorkbenchFrame().warnUser(
                "Missing class: " + e.getCause());
    }
    return sourceTask.getProperties();
  }
}
