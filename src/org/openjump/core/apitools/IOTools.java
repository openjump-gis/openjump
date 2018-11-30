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

import static com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn.get;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.geotools.dbffile.DbfFieldDef;
import org.geotools.dbffile.DbfFile;
import org.geotools.dbffile.DbfFileWriter;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;
import org.openjump.core.ui.plugin.style.ImportSLDPlugIn;
import org.openjump.core.ui.util.ScreenScale;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.java2xml.Java2XML;
import com.vividsolutions.jump.util.java2xml.XML2Java;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

import de.latlon.deejump.plugin.style.LayerStyle2SLDPlugIn;

public class IOTools {

    static LayerManager manager = JUMPWorkbench.getInstance().getFrame()
            .getContext().getLayerManager();

    private static String getExtension(String filename) {
        final int len = filename.length();
        return filename.substring(len - 3, len);
    }

    public static FeatureCollection load(String filename) throws Exception {
        final String extension = getExtension(filename);
        if (extension.equalsIgnoreCase("SHP")) {
            return loadShapefile(filename);
        }
        if (extension.equalsIgnoreCase("JML")) {
            return loadJMLFile(filename);
        }
        if (extension.equalsIgnoreCase("WKT")) {
            return loadWKT(filename);
        }
        throw new Exception("Unknown file type: " + extension);
    }

    public static FeatureCollection load(String filename, String zipFileName)
            throws Exception {
        final String extension = getExtension(filename);
        if (extension.equalsIgnoreCase("SHP")) {
            return loadShapefile(filename, zipFileName);
        }
        throw new Exception("Unknown file type: " + extension);
    }

    public static FeatureCollection loadJMLFile(String filename)
            throws Exception {
        final JMLReader rdr = new JMLReader();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadShapefile(String filename)
            throws Exception {
        final ShapefileReader rdr = new ShapefileReader();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadShapefile(String filename,
            String zipFileName) throws Exception {
        final ShapefileReader rdr = new ShapefileReader();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        if (zipFileName != null) {
            dp.set(DataSource.COMPRESSED_KEY, zipFileName);
        }
        return rdr.read(dp);
    }

    public static FeatureCollection loadFMEGML(String filename)
            throws Exception {
        final FMEGMLReader rdr = new FMEGMLReader();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        return rdr.read(dp);
    }

    public static FeatureCollection loadWKT(String filename) throws Exception {
        final WKTReader rdr = new WKTReader();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        final FeatureCollection fc = rdr.read(dp);
        return fc;
    }

    public static void save(FeatureCollection fc, String filename)
            throws Exception {
        final String extension = getExtension(filename);
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
        final ShapefileWriter writer = new ShapefileWriter();
        final DriverProperties dp = new DriverProperties();
        dp.set(DataSource.FILE_KEY, filename);
        writer.write(fc, dp);
    }

    public static void saveJMLFile(FeatureCollection fc, String filename)
            throws Exception {
        final JMLWriter writer = new JMLWriter();
        final DriverProperties dp = new DriverProperties();
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

    /**
     * Export jtable to csv file using commas as separators
     * 
     * @param table
     * @param filename
     * @throws Exception
     */
    public static void saveCSV(JTable table, String filename) throws Exception {

        saveCSV(table, filename, ",");
        /*
         * try { final File file = new File(filename); final BufferedWriter bw =
         * new BufferedWriter( new OutputStreamWriter(new FileOutputStream(
         * file.getAbsoluteFile()), "UTF-8"));
         * 
         * for (int j = 0; j < table.getColumnCount(); j++) {
         * bw.write(table.getModel().getColumnName(j) + "\t"); } bw.newLine(); ;
         * for (int i = 0; i < table.getRowCount(); i++) { for (int j = 0; j <
         * table.getColumnCount(); j++) {
         * bw.write(table.getModel().getValueAt(i, j) + "\t"); } bw.newLine(); }
         * bw.close(); } catch (final Exception e) {
         * 
         * // }
         */
    }

    /**
     * Export jtable to csv file using a cell separator
     * 
     * @param table
     *            JTable
     * @param filename
     *            output file name: C:\folder\file.csv
     * @param cellseparator
     *            cell break to separate values: "," ";" tab etc
     * @throws Exception
     */
    public static void saveCSV(JTable table, String filename,
            String cellseparator) throws Exception {

        try {
            final File file = new File(filename);
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(
                            file.getAbsoluteFile()), "UTF-8"));

            for (int j = 0; j < table.getColumnCount(); j++) {
                String columnName = table.getModel().getColumnName(j);
                final int number = StringUtils.countMatches(columnName, "\"");
                if (columnName.contains(cellseparator) & number == 2) {
                    columnName = "\"" + columnName + "\"";
                }
                bw.write(columnName + cellseparator);
            }
            bw.newLine();
            ;
            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < table.getColumnCount(); j++) {
                    String value = table.getModel().getValueAt(i, j).toString();
                    final int number = StringUtils.countMatches(value, "\"");
                    if (value.contains(cellseparator) & number == 2) {
                        value = "\"" + value + "\"";
                    }
                    bw.write(value + cellseparator);
                }
                bw.newLine();
            }
            bw.close();
        } catch (final Exception e) {

            //
        }
    }

    public static void saveShapefile(Layer layer, String filename)
            throws Exception {
        final DataSourceQuery dsq = layer.getDataSourceQuery();
        final DriverProperties dp = new DriverProperties();
        final Object charsetName = dsq.getDataSource().getProperties()
                .get("charset");
        if (charsetName != null) {
            dp.set("charset", charsetName.toString());
        }
        (new ShapefileWriter()).write(layer.getFeatureCollectionWrapper(), dp);
        dp.set("File", filename);
        (new ShapefileWriter()).write(layer.getFeatureCollectionWrapper(), dp);
    }

    public static void saveJMLFile(Layer layer, String filename)
            throws Exception {
        final DataSourceQuery dsq = layer.getDataSourceQuery();
        final DriverProperties dp = new DriverProperties();
        final Object charsetName = dsq.getDataSource().getProperties()
                .get("charset");
        if (charsetName != null) {
            dp.set("charset", charsetName.toString());
        }
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
        final double internalScale = 1d / JUMPWorkbench.getInstance()
                .getFrame().getContext().getLayerViewPanel().getViewport()
                .getScale();
        final double realScale = ScreenScale
                .getHorizontalMapScale(JUMPWorkbench.getInstance().getFrame()
                        .getContext().getLayerViewPanel().getViewport());
        final double scaleFactor = internalScale / realScale;
        final String outSLD = manager.uniqueLayerName(layer.getName() + ".sld");

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
                new FileOutputStream(sld_outFile), Charset.defaultCharset());
        // "UTF-8");
        final HashMap<String, String> map = new HashMap<String, String>(9);
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
        final List featList = fc.getFeatures();
        for (final Iterator i = featList.iterator(); i.hasNext();) {
            final Feature f = (Feature) i.next();
            System.out.println(f.getGeometry());
        }
    }

    /**
     * Method to save Openjump symbology of a layer  to  a XML file
     * 
     * 
     * @param style
     *            file to save (ex. file.style.xml)
     * @param layer
     *            source layer
     * @throws Exception
     */

    public static void saveSimbology_Jump(File file, Layer layer)
            throws Exception {
        final StringWriter stringWriter = new StringWriter();
        try {

            new Java2XML().write(layer, "layer", stringWriter);
        } finally {
            stringWriter.flush();
        }

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
        final InputSource is = new InputSource(new StringReader(
                stringWriter.toString()));
        final Document document = documentBuilder.parse(is);
        // Remove all other elements than <Styles> from XML
        removeElements(document);

        document.normalize();
        //Write the style.xml file
        final DOMSource source = new DOMSource(document);
        final FileWriter writer = new FileWriter(new File(
                file.getAbsolutePath()));
        final StreamResult result = new StreamResult(writer);
        final TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);

        //define the folder for possible pictures used as vertex symbols
        final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");
        final String folder = ext.matcher(file.getAbsolutePath())
                .replaceAll("");

        //Create a list of pictures
        final List<String> listPictures = getListOfElements(document);

        if (!listPictures.isEmpty()) {
            new File(folder).mkdir();
            //copy pictures into the folder
            for (final String str : listPictures) {
                final File inputFile = new File(
                        convertPathToSystemIndependentPath(str));
                copyFileToFolder(inputFile, new File(folder).getAbsolutePath());
            }

            //save a list of original paths of the pictures as txt file
            try {
                final OutputStreamWriter wrt = new OutputStreamWriter(
                        new FileOutputStream(new File(folder).getAbsolutePath()
                                .concat(File.separator).concat("picture.txt")),
                        StandardCharsets.UTF_8);
                String listString = "";
                for (final String s : listPictures) {
                    listString += s + System.getProperty("line.separator");
                }
                wrt.write(listString);
                wrt.close();

            } catch (final IOException i) {
            }
        }
    }

    /**
     * Simple method to copy a file to a folder 
     * @param inputFile
     * @param outputDir
     * @throws IOException
     */
    public static void copyFileToFolder(File inputFile, String outputDir)
            throws IOException {
        final File directoryFile = new File(outputDir);
        if (directoryFile.canRead()) {
            final File outFile = new File(outputDir.concat(File.separator)
                    .concat(inputFile.getName()));
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                inputChannel = new FileInputStream(inputFile).getChannel();
                outputChannel = new FileOutputStream(outFile).getChannel();
                outputChannel
                        .transferFrom(inputChannel, 0, inputChannel.size());
            } finally {
                inputChannel.close();
                outputChannel.close();
            }
        }
    }

    /**
     * Converts a system-dependent path into an independent one
     * 
     * @param oldPath
     * @return newPath
     */
    public static String convertPathToSystemIndependentPath(String oldPath) {
        final String path = Paths.get(oldPath).toUri().getPath();
        return path;

    }

    /**
     * filter all elements to remove all tags except symbology ones
     * used by Save To JUMP Symbology method
    */
    private static void removeElements(Node parent) {
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals("description")
                        || child.getNodeName().equals("data-source-query")
                        || child.getNodeName().equals(
                                "feature-schema-operation")) {

                    parent.removeChild(child);
                    while (child.hasChildNodes()) {
                        child.removeChild(child.getFirstChild());
                    }

                } else {
                    removeElements(child);
                }
                final Element eElement = (Element) child;
                if (eElement.getAttribute("class").contains(
                        "org.openjump.core.ccordsys.srid.SRIDStyle")) {

                    parent.removeChild(child);
                    while (child.hasChildNodes()) {
                        child.removeChild(child.getFirstChild());
                    }
                }
            }
        }

    }

    /**
     * get a list of urls of pictures used as vertex symbols
     * search into <style> and <vertex-style> elements
     * used by Save To JUMP Symbology method
     * @param parent
     * @return List
     */
    private static List<String> getListOfElements(Node parent) {
        final NodeList children = ((Document) parent).getDocumentElement()
                .getElementsByTagName("style");
        final LinkedHashSet<String> uniqueStrings = new LinkedHashSet<String>();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = eElement.getAttribute("imageURL");
                    uniqueStrings.add(url);
                }
            }
        }
        final NodeList children1 = ((Document) parent).getDocumentElement()
                .getElementsByTagName("vertex-style");
        for (int i = 0; i < children1.getLength(); i++) {
            final Node child = children1.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = eElement.getAttribute("imageURL");
                    uniqueStrings.add(url);
                }
            }
        }
        final List<String> asList = new ArrayList<String>(uniqueStrings);
        return asList;
    }

    /**
     * check if in the XML file there are nodes with attributes as "imageURL"
     * used by Load JUMP Symbology method
     * @param file
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException
     */

    private static boolean imageURLExist(File file)
            throws ParserConfigurationException, SAXException, IOException,
            TransformerException {

        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(file);
        final LinkedHashSet<String> uniqueStrings = new LinkedHashSet<String>();
        final NodeList children = doc.getDocumentElement()
                .getElementsByTagName("style");
        final NodeList children1 = doc.getDocumentElement()
                .getElementsByTagName("vertex-style");
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = eElement.getAttribute("imageURL");

                    uniqueStrings.add(url);

                }
            }
        }

        for (int i = 0; i < children1.getLength(); i++) {
            final Node child = children1.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = eElement.getAttribute("imageURL");

                    uniqueStrings.add(url);

                }
            }
        }
        if (uniqueStrings.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**

     * This method will recompile the XML file according to location (folder) of the image symbology files
     * used by Load JUMP Symbology method     
     * @param xml.
     *            XML file
     * @param directory. 
     *                  Folder where the images are stored
     * @return File
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException
     */
    public static File recompileXMLFile(File file, String directory)
            throws ParserConfigurationException, SAXException, IOException,
            TransformerException {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(file);
        final NodeList children = doc.getDocumentElement()
                .getElementsByTagName("style");
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = convertPathToSystemIndependentPath(eElement
                            .getAttribute("imageURL"));
                    final String name = new File(url).getName();
                    final String newNamePath = directory.concat(File.separator)
                            .concat(name);
                    eElement.setAttribute("imageURL", newNamePath);
                }
            }
        }
        final NodeList children1 = doc.getDocumentElement()
                .getElementsByTagName("vertex-style");
        for (int i = 0; i < children1.getLength(); i++) {
            final Node child = children1.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element eElement = (Element) child;
                if (!eElement.getAttribute("imageURL").isEmpty()) {
                    final String url = convertPathToSystemIndependentPath(eElement
                            .getAttribute("imageURL"));
                    final String name = new File(url).getName();
                    final String newNamePath = directory.concat(File.separator)
                            .concat(name);
                    eElement.setAttribute("imageURL", newNamePath);
                }
            }
        }
        final Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();
        final DOMSource input = new DOMSource(doc);
        final File outFile = new File(file.getAbsolutePath());
        final StreamResult output = new StreamResult(outFile);
        transformer.transform(input, output);
        return outFile;
    }

    /**
         * Method to load an OpenJUMP symbology file (xml jump) into a layer
         * 
         * @param style
         *            file to load (ex. file.style.xml)
         * @param layer
         *            target layer
         * @throws Exception
         */
    public static void loadSimbology_Jump(File file, Layer layer)
            throws Exception {
        final WorkbenchFrame workbenchFrame = JUMPWorkbench.getInstance()
                .getFrame();
        File recompiled = null;
        //Check if the xml file contains urls for vertex bitmaps.
        //if not, there is no need to recompile the file
        if (!imageURLExist(file)) {
            recompiled = file;
        } else {
            final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");
            final String folder = ext.matcher(file.getAbsolutePath())
                    .replaceAll("");
            final File fold = new File(folder);
            //If the folder, with the same name of the xml file, containing vertex bitmaps, exist
            //It is automatically loaded to recompile XML file
            if (fold.exists() && fold.isDirectory()) {
                recompiled = recompileXMLFile(file, fold.getAbsolutePath());
            } else {
                // If the folder is not automatically found
                // a filechooser is open to select that dolder
                final JFCWithEnterAction chooser = new JFCWithEnterAction();
                chooser.setCurrentDirectory(new java.io.File("."));
                chooser.setDialogTitle("Select folder where vertex images are located");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(JUMPWorkbench.getInstance()
                        .getFrame()) == JFileChooser.APPROVE_OPTION) {
                    recompiled = recompileXMLFile(file, chooser
                            .getSelectedFile().getAbsolutePath());
                } else {
                    //Last choice, load any possible symbology from XML file and
                    //warning to user that some symbols could be not loaded
                    recompiled = file;

                    workbenchFrame.getContext().getLayerViewPanel()
                            .getContext()
                            .warnUser("Some styles could not be loaded");
                }
            }
        }
        final FileReader reader = new FileReader(recompiled);

        try {
            final Layer sourceLayer = (Layer) new XML2Java(workbenchFrame
                    .getContext().getWorkbench().getPlugInManager()
                    .getClassLoader()).read(reader, Layer.class);
            final Collection<Style> styleColection = sourceLayer.getStyles();
            final ArrayList<Style> names = new ArrayList<Style>();
            for (final Style style2 : styleColection) {
                names.add(style2);
                if (style2 instanceof SRIDStyle) {
                    names.remove(style2);
                }
            }
            try {
                layer.setStyles(names);
            } catch (final Exception e) {
                Logger.error(e);
                final String errorMessage = "Error on loading symbols. Try to do it manually"; //$NON-NLS-1$ 
                JOptionPane.showMessageDialog(
                        workbenchFrame.getActiveInternalFrame(), errorMessage,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Method to load a style file ( SLD - Spatial layer descriptor) into a
     * layer
     * 
     * @param style
     *            file to load (ex. file.sld)
     * @param layer
     *            target layer
     * @throws Exception
     */

    public static void loadSimbology_SLD(File file, PlugInContext context)
            throws SAXException, IOException, ParserConfigurationException {
        final Blackboard bb = get(JUMPWorkbench.getInstance().getFrame()
                .getContext());
        bb.put("ImportSLDPlugin.filename", file.getAbsoluteFile().toString());
        final DocumentBuilderFactory dbf = newInstance();
        dbf.setNamespaceAware(true);

        final Document doc = dbf.newDocumentBuilder().parse(file);
        try {
            ImportSLDPlugIn.importSLD(doc, context);
        } catch (final Exception e) {
            Logger.error(e);
        }

    }

    /**
     * Method to save the style of a layer as SLD (Spatial layer
     * descriptor) file
     * 
     * @param style
     *            file to save (ex. file.style.sld)
     * @param layer
     *            source layer
     * @throws Exception
     */
    public static void saveSimbology_SLD(File file, Layer layer)
            throws Exception {

        final double internalScale = 1d / JUMPWorkbench.getInstance()
                .getFrame().getContext().getLayerViewPanel().getViewport()
                .getScale();
        final double realScale = ScreenScale
                .getHorizontalMapScale(JUMPWorkbench.getInstance().getFrame()
                        .getContext().getLayerViewPanel().getViewport());
        final double scaleFactor = internalScale / realScale;

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
                new FileOutputStream(file), Charset.defaultCharset());
        // "UTF-8");
        final HashMap<String, String> map = new HashMap<String, String>(9);
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
        // ATTENTION : note that min and max are swapped in JUMP!!!
        // It will swap later, in transformContext
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

    public static void saveSimbology_SLD2(File file, Layer layer)
            throws Exception {
        final String name = layer.getName();
        final BasicFeature bf = (BasicFeature) layer
                .getFeatureCollectionWrapper().getFeatures().get(0);
        final Geometry geo = bf.getGeometry();
        final String geoType = geo.getGeometryType();
        final MultiInputDialog dialog = new MultiInputDialog(
                JUMPWorkbench.getInstance().getFrame(),
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.SLD-Parameters"),
                true);
        String geomProperty = "GEOM";

        final FeatureSchema schema = layer.getFeatureCollectionWrapper()
                .getFeatureSchema();
        for (int i = 0; i < schema.getAttributeCount(); ++i) {
            if (schema.getAttributeType(i) == AttributeType.GEOMETRY) {
                geomProperty = schema.getAttributeName(i);
            }
        }
        dialog.addSeparator();

        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"),
                geomProperty,
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Input-the-name-of-the-geometry-property"));

        dialog.addSeparator();

        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"),
                name,
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"));
        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"),
                name,
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"));
        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"),
                name,
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"));
        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"),
                name,
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"));
        dialog.addTextField(
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"),
                "http://www.deegree.org/app",
                25,
                null,
                I18N.get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"));
        GUIUtil.centreOnWindow(dialog);

        dialog.setVisible(true);

        if (!dialog.wasOKPressed()) {
            return;
        }

        // why the field name given when constructing the dialog are used both
        // for the label and
        // for the key is beyond me
        final String wmsLayerName = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.WMS-Layer-name"));
        final String styleName = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-name"));
        final String styleTitle = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Style-title"));
        final String featureTypeStyle = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Feature-Type-Style"));
        geomProperty = dialog
                .getText(I18N
                        .get("deejump.pluging.style.LayerStyle2SLDPlugIn.geomProperty"));
        final String namespace = dialog.getText(I18N
                .get("deejump.pluging.style.LayerStyle2SLDPlugIn.Namespace"));

        final double internalScale = 1d / JUMPWorkbench.getInstance()
                .getFrame().getContext().getLayerViewPanel().getViewport()
                .getScale();
        final double realScale = ScreenScale
                .getHorizontalMapScale(JUMPWorkbench.getInstance().getFrame()
                        .getContext().getLayerViewPanel().getViewport());
        final double scaleFactor = internalScale / realScale;

        final File inputXML = File.createTempFile("temptask", ".xml");
        inputXML.deleteOnExit();

        // TODO don't assume has 1 item!!!
        // Should create this condition in EnableCheckFactory
        if (layer.getFeatureCollectionWrapper().getFeatures().size() == 0) {
            throw new Exception(
                    I18N.get("org.openjump.core.ui.plugin.tools.statistics.StatisticOverViewPlugIn.Selected-layer-is-empty"));
        }

        final Java2XML java2Xml = new Java2XML();
        java2Xml.write(layer, "layer", inputXML);
        final FileInputStream input = new FileInputStream(inputXML);
        // FileWriter fw = new FileWriter( outputXML );
        final OutputStreamWriter fw = new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8");// Charset.defaultCharset());
        // "UTF-8");
        final HashMap<String, String> map = new HashMap<String, String>(9);
        map.put("wmsLayerName", wmsLayerName);
        map.put("featureTypeStyle", featureTypeStyle);
        map.put("styleName", styleName);
        map.put("styleTitle", styleTitle);
        map.put("geoType", geoType);
        map.put("geomProperty", geomProperty);
        map.put("Namespace", namespace);
        // map.put("NamespacePrefix", prefix + ":");
        // map.put("NamespacePrefixWithoutColon", prefix);
        // ATTENTION : note that min and max are swapped in JUMP!!!
        // It will swap later, in transformContext
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

}
