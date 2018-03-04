/*
 * Project Name: OpenJUMP 
 * Original Organization Name: The JUMP Pilot Project
 * Original Programmer Name: Martin Davis
 * Current Maintainer Name: The JUMP Pilot Project
 * Current Maintainer Contact Information
 *    E-Mail Address: sunburned.surveyor@gmail.com
 * Copyright Holder: Martin Davis
 * Date Last Modified: Dec 12, 2007
 * IDE Name: Eclipse
 * IDE Version: Europa
 * Type: Java Class
 */

package org.openjump.core.apitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.geotools.dbffile.DbfFieldDef;
import org.geotools.dbffile.DbfFile;
import org.geotools.dbffile.DbfFileWriter;
import org.openjump.core.ui.util.ScreenScale;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.FMEGMLReader;
import com.vividsolutions.jump.io.JMLReader;
import com.vividsolutions.jump.io.JMLWriter;
import com.vividsolutions.jump.io.ShapefileReader;
import com.vividsolutions.jump.io.ShapefileWriter;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;

import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;

public class IOTools {

    static LayerManager manager = JUMPWorkbench.getInstance().getFrame()
            .getContext().getLayerManager();

    private static String getExtension(String filename) {
        int len = filename.length();
        return filename.substring(len - 3, len);
    }

    public static FeatureCollection load(String filename) throws Exception {
        String extension = getExtension(filename);
        if (extension.equalsIgnoreCase("SHP"))
            return loadShapefile(filename);
        if (extension.equalsIgnoreCase("JML"))
            return loadJMLFile(filename);
        if (extension.equalsIgnoreCase("WKT"))
            return loadWKT(filename);
        throw new Exception("Unknown file type: " + extension);
    }

    public static FeatureCollection load(String filename, String zipFileName)
            throws Exception {
        String extension = getExtension(filename);
        if (extension.equalsIgnoreCase("SHP"))
            return loadShapefile(filename, zipFileName);
        throw new Exception("Unknown file type: " + extension);
    }

    public static FeatureCollection loadJMLFile(String filename)
            throws Exception {
        JMLReader rdr = new JMLReader();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadShapefile(String filename)
            throws Exception {
        ShapefileReader rdr = new ShapefileReader();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadShapefile(String filename,
            String zipFileName) throws Exception {
        ShapefileReader rdr = new ShapefileReader();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        if (zipFileName != null)
            dp.set(DataSource.COMPRESSED_KEY, zipFileName);
        return rdr.read(dp);
    }

    public static FeatureCollection loadFMEGML(String filename)
            throws Exception {
        FMEGMLReader rdr = new FMEGMLReader();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadWKT(String filename) throws Exception {
        WKTReader rdr = new WKTReader();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        FeatureCollection fc = rdr.read(dp);
        return fc;
    }

    public static void save(FeatureCollection fc, String filename)
            throws Exception {
        String extension = getExtension(filename);
        if (extension.equalsIgnoreCase("SHP")) {
            saveShapefile(fc, filename);
            return;
        } else if (extension.equalsIgnoreCase("JML")) {
            saveJMLFile(fc, filename);
            return;
        }
        throw new Exception("Unknown file type: " + extension);
    }

    public static void saveShapefile(FeatureCollection fc, String filename)
            throws Exception {
        ShapefileWriter writer = new ShapefileWriter();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        writer.write(fc, dp);
    }

    public static void saveJMLFile(FeatureCollection fc, String filename)
            throws Exception {
        JMLWriter writer = new JMLWriter();
        DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        writer.write(fc, dp);
    }

    public static void saveDbfFile(FeatureCollection fc, String filename)

    throws Exception {
        final FeatureSchema fs = fc.getFeatureSchema();
        final DbfFieldDef[] fields = new DbfFieldDef[fs.getAttributeCount()];

        // dbf column type and size
        int f = 0;

        for (int t = 0; t < fs.getAttributeCount(); t++) {
            final AttributeType columnType = fs.getAttributeType(t);
            final String columnName = fs.getAttributeName(t);

            if (columnType == AttributeType.INTEGER) {
                fields[f] = new DbfFieldDef(columnName, 'N', 16, 0);
                f++;
            } else if (columnType == AttributeType.DOUBLE) {
                fields[f] = new DbfFieldDef(columnName, 'N', 33, 16);
                f++;
            } else if (columnType == AttributeType.STRING) {
                final int maxlength = findMaxStringLength(fc, t);
                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
                f++;
            } else if (columnType == AttributeType.DATE) {
                fields[f] = new DbfFieldDef(columnName, 'D', 8, 0);
                f++;
            } else if (columnType == AttributeType.BOOLEAN) {
                fields[f] = new DbfFieldDef(columnName, 'L', 1, 0);
                f++;
            } else if (columnType == AttributeType.NUMERIC) {
                final int maxlength = findMaxStringLength(fc, t);
                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
                f++;
            } else if (columnType == AttributeType.TIMESTAMP) {
                fields[f] = new DbfFieldDef(columnName, '@', 8, 0);
                f++;
            } else if (columnType == AttributeType.LONG) {
                final int maxlength = findMaxStringLength(fc, t);
                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
                f++;
            } else if (columnType == AttributeType.FLOAT) {
                fields[f] = new DbfFieldDef(columnName, 'I', 4, 0);
                f++;
            } else {
                final int maxlength = findMaxStringLength(fc, t);
                fields[f] = new DbfFieldDef(columnName, 'C', maxlength, 0);
                f++;
            }
        }
        // write header
        final DbfFileWriter dbf = new DbfFileWriter(filename);
        dbf.writeHeader(fields, fc.size());

        // write rows
        final int num = fc.size();

        final List<Feature> features = fc.getFeatures();

        for (int t = 0; t < num; t++) {
            final Feature feature = features.get(t);
            final Vector<Object> DBFrow = new Vector<Object>();
            // make data for each column in this feature (row)
            for (int u = 0; u < fs.getAttributeCount(); u++) {
                final AttributeType columnType = fs.getAttributeType(u);

                if (columnType == AttributeType.INTEGER) {
                    final Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(new Integer(0));
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.DOUBLE) {
                    final Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(new Double(0.0));
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.FLOAT) {
                    final Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(new Float(0.0));
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.BOOLEAN) {
                    final Object a = feature.getAttribute(u);
                    if (a == null) {
                        DBFrow.add(new Boolean(false));
                    } else {
                        DBFrow.add(a);
                    }
                } else if (columnType == AttributeType.TIMESTAMP) {
                    final Object a = feature.getAttribute(u);
                    if (a == null) {
                        DBFrow.add(new String(""));
                    } else {
                        DBFrow.add(a);
                    }
                }

                else if (columnType == AttributeType.DATE) {
                    final Object a = feature.getAttribute(u);
                    if (a == null) {
                        DBFrow.add("");
                    } else {
                        DBFrow.add(DbfFile.DATE_PARSER.format((Date) a));
                    }
                } else if (columnType == AttributeType.STRING
                        || columnType == AttributeType.NUMERIC
                        || columnType == AttributeType.LONG) {
                    final Object a = feature.getAttribute(u);

                    if (a == null) {
                        DBFrow.add(new String(""));
                    } else {
                        // MD 16 jan 03 - added some defensive programming
                        if (a instanceof String) {
                            DBFrow.add(a);
                        } else {
                            DBFrow.add(a.toString());
                        }
                    }
                }
            }

            dbf.writeRecord(DBFrow);
        }

        dbf.close();

    }

    public static void saveShapefile(Layer layer, String filename)
            throws Exception {
        DataSourceQuery dsq = layer.getDataSourceQuery();
        DriverProperties dp = new DriverProperties();
        Object charsetName = dsq.getDataSource().getProperties().get("charset");
        if (charsetName != null)
            dp.set("charset", charsetName.toString());
        (new ShapefileWriter()).write(layer.getFeatureCollectionWrapper(), dp);
        dp.set("File", filename);
        (new ShapefileWriter()).write(layer.getFeatureCollectionWrapper(), dp);
    }

    public static void saveJMLFile(Layer layer, String filename)
            throws Exception {
        DataSourceQuery dsq = layer.getDataSourceQuery();
        DriverProperties dp = new DriverProperties();
        Object charsetName = dsq.getDataSource().getProperties().get("charset");
        if (charsetName != null)
            dp.set("charset", charsetName.toString());
        (new JMLWriter()).write(layer.getFeatureCollectionWrapper(), dp);
        dp.set("File", filename);
        (new JMLWriter()).write(layer.getFeatureCollectionWrapper(), dp);
    }

    /**
     * look at all the data in the column of the featurecollection, and find the
     * largest string!
     * 
     * @param fc
     *            features to look at
     * @param attributeNumber
     *            which of the column to test.
     */
    static int findMaxStringLength(final FeatureCollection fc,
            final int attributeNumber) {
        int l;
        int maxlen = 0;
        Feature f;

        for (final Iterator i = fc.iterator(); i.hasNext();) {
            f = (Feature) i.next();
            l = f.getString(attributeNumber).length();

            if (l > maxlen) {
                maxlen = l;
            }
        }

        return maxlen;
    }

    /*
     * This method derives from
     * de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn.class and it has been
     * modified to automatically export SLD files for vectors
     */

    public static void saveStyleToFile(Layer layer, String path)
            throws Exception {
        double internalScale = 1d / JUMPWorkbench.getInstance().getFrame()
                .getContext().getLayerViewPanel().getViewport().getScale();
        double realScale = ScreenScale.getHorizontalMapScale(JUMPWorkbench
                .getInstance().getFrame().getContext().getLayerViewPanel()
                .getViewport());
        double scaleFactor = internalScale / realScale;
        String outSLD = manager.uniqueLayerName(layer.getName() + ".sld");

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
                sld_outFile), Charset.defaultCharset());
        // "UTF-8");
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
        // make it simple and hardcode a large number
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

    public static void print(FeatureCollection fc) {
        List featList = fc.getFeatures();
        for (Iterator i = featList.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            System.out.println(f.getGeometry());
        }
    }

}
