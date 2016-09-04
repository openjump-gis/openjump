package org.openjump.core.ccordsys.utils;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.io.FilenameUtils;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.rasterimage.GeoTiffConstants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Giuseppe Aruta [23_3_2016] This class is used to recognize file
 * projection. There are different methods A) a method to decode projection
 * information from GeoTIFF metadata. B) a method to decode projection info
 * from auxiliary files (.proj and .aux.xml).
 * http://landsathandbook.gsfc.nasa.gov/pdfs/geotiff_spec.pdf
 * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/GeoTiff.html
 * http://www.remotesensing.org/geotiff/spec/geotiff6.html
 *
 * the following datasets have been used to test this library: QGIS
 * (http://qgis.org/downloads/data/) OSGEO data samples (
 * http://download.osgeo.org/geotiff/samples/)
 */
public class ProjUtils2 {

    private static final String PROJECTION_UNSPECIFIED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.unknown_projection");
    private static final String USER_DEFINED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.User_defined");
    private static final String NOT_RECOGNIZED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not_recognized");


    /**
     * Test method - to get GeoTiff envelope in case of no srid def.
     *
     * @param fileSourcePath
     * @return envelope
     */
    public static Envelope GeoTiffEnvelope(String fileSourcePath)
            throws Exception {
        BufferedImage image = ImageIO.read(new File(fileSourcePath));
        Point imageDimensions = new Point(image.getWidth(), image.getHeight());
        Envelope env = null;
        Coordinate tiePoint = null, pixelOffset = null, pixelScale = null;
        FileSeekableStream fileSeekableStream = new FileSeekableStream(
                fileSourcePath);
        TIFFDirectory tiffDirectory = new TIFFDirectory(fileSeekableStream, 0);

        TIFFField[] availTags = tiffDirectory.getFields();

        for (TIFFField availTag : availTags) {
            if (availTag.getTag() == GeoTiffConstants.ModelTiepointTag) {
                double[] doubles = availTag.getAsDoubles();
                if (doubles.length != 6) {
                    throw new Exception(
                            "unsupported value for ModelTiepointTag ("
                                    + GeoTiffConstants.ModelTiepointTag + ")");
                }
                pixelOffset = new Coordinate(doubles[0], doubles[1]);
                tiePoint = new Coordinate(doubles[3], doubles[4]);
            } else if (availTag.getTag() == GeoTiffConstants.ModelPixelScaleTag) {
                // Karteneinheiten pro pixel x bzw. y
                double[] doubles = availTag.getAsDoubles();
                pixelScale = new Coordinate(doubles[0], doubles[1]);
            } else {}
        }

        fileSeekableStream.close();

        if (tiePoint != null && pixelScale != null) {
            Coordinate upperLeft;
            Coordinate lowerRight;

            if (pixelOffset == null) {
                upperLeft = tiePoint;
            } else {
                upperLeft = new Coordinate(tiePoint.x
                        - (pixelOffset.x * pixelScale.x), tiePoint.y
                        - (pixelOffset.y * pixelScale.y));
            }

            lowerRight = new Coordinate(upperLeft.x
                    + (imageDimensions.x * pixelScale.x), upperLeft.y
                    - (imageDimensions.y * pixelScale.y));

            env = new Envelope(upperLeft, lowerRight);
        }
        return env;
    }

    /**
     * Test method to read the entire GeoKeyDirectoryTag, only used to study
     * GeoKeyDirectoryTag structure
     *
     * @param fileSourcePath
     * @return GeoKeyDirectoryTag
     * @throws IOException
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-access")
    public static String readGeoTiffGeoKeyDirectoryTag(String fileSourcePath)
            throws IOException, URISyntaxException {
        String prjname = "";
        File tiffFile = new File(fileSourcePath);
        try {
            TiffImageParser parser = new TiffImageParser();
            TiffImageMetadata metadata = (TiffImageMetadata) parser
                    .getMetadata(tiffFile);
            if (metadata != null) {
                java.util.List<TiffField> tiffFields = metadata.getAllFields();
                GeoTiffConstants constants = new GeoTiffConstants();
                for (TiffField tiffField : tiffFields) {
                    if (tiffField.getTag() == constants.GeoKeyDirectoryTag) {
                        prjname = tiffField.getValueDescription();
                    }
                }
            } else {
                prjname = "GeoKeyDirectoryTagis empty";
            }
        } catch (Exception ex) {
            prjname = PROJECTION_UNSPECIFIED;
        }
        return prjname;
    }

    /**
     * - Read SRS from GeoTIFF tag - This method gets projection srid code and
     * projection info from a geotiff file. It first scans GeoKeyDirectoryTag to
     * get either geographic/geocentric (2048 - GeographicTypeGeoKey), projected
     * (3072 - ProjectedCSTypeGeoKey) or vertical (4096 - VerticalCSTypeGeoKey)
     * info. If no key ID is identified, it scans for GeoAsciiParamsTag
     * projection definition. Last choice, it search for an auxiliary file
     *
     * @param fileSourcePath
     *            . eg. "c\documents\folder\image.tif"
     * @return the projection srid as a string. eg "32632"
     * @throws IOException
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-access")
    public static String readSRSFromGeoTiffFile(String fileSourcePath)
            throws IOException, URISyntaxException {
        String geoDirTag = "";
        String geoDirTag2 = "";
        String prjname = "";
        File tiffFile = new File(fileSourcePath);
        try {
            TiffImageParser parser = new TiffImageParser();
            TiffImageMetadata metadata = (TiffImageMetadata) parser
                    .getMetadata(tiffFile);
            if (metadata != null) {
                java.util.List<TiffField> tiffFields = metadata.getAllFields();
                GeoTiffConstants constants = new GeoTiffConstants();
                for (TiffField tiffField : tiffFields) {
                    if (tiffField.getTag() == constants.GeoKeyDirectoryTag) {
                        geoDirTag = tiffField.getValueDescription();
                    }
                    if (tiffField.getTag() == constants.GeoAsciiParamsTag) {
                        geoDirTag2 = tiffField.getStringValue().replaceAll(
                                "[\\t\\n\\r\\_\\|]", " ");
                    }
                    if (tiffField.getTag() == constants.ModelTiepointTag) {
                    }
                }
                prjname = getPrjNameFromGeoDirTags(fileSourcePath, geoDirTag, geoDirTag2);
            }
        } catch (Exception ex) {
            prjname = PROJECTION_UNSPECIFIED;
        }
        return prjname;
    }

    private static String getPrjNameFromGeoDirTags(String fileSourcePath, String geoDirTag, String geoDirTag2)
            throws IOException, URISyntaxException {
        if (geoDirTag.contains("3072"))
            return getPrjNameFromGeoDirTags("3072", geoDirTag, geoDirTag2, "32767");
        else if (geoDirTag.contains("4096"))
            return getPrjNameFromGeoDirTags("4096", geoDirTag, geoDirTag2, "32767");
        else if (geoDirTag.contains("2048"))
            return getPrjNameFromGeoDirTags("2048", geoDirTag, geoDirTag2, "32767");
        else if (geoDirTag2.isEmpty())
            return readSRSFromAuxiliaryFile(fileSourcePath);
        else
            return "SRID: " + USER_DEFINED + " - " + geoDirTag2;
    }

    private static String getPrjNameFromGeoDirTags(String key, String geoDirTag, String geoDirTag2, String exclude)
            throws IOException, URISyntaxException {
        int start = geoDirTag.indexOf(key);
        String ID = geoDirTag.substring(start);
        String[] parts = ID.split(",");
        if (parts.length > 3 && !parts[3].contains(exclude)) {
            return getSRSFromWkt(parts[3].replaceAll(" ", ""));
        } else {
            return "SRID: " + USER_DEFINED + " - " + geoDirTag2;
        }
    }

    /**
     * - Read SRS from auxiliary file - Method to get a SRS (SRID code + SRID
     * definition) scanning the aux projection file (AUX.XML or PRJ file) for a
     * search string (SRID code or SRID definition). It scans into the registry
     * file (srid.txt) to find a correspondence between the search string and
     * lines of the srid.txt. If the source string corresponds as substring to a
     * line, it returns the complete line as string. For instance, search
     * strings like "NAD83 UTM zone 10N" or "26910" both return
     * "SRID:26910 - NAD83 UTM zone 10N".
     *
     * @param fileSourcePath auxiliary file path
     * @return SRID and Projection definition
     * @throws URISyntaxException
     * @throws IOException
     */

    public static String readSRSFromAuxiliaryFile(String fileSourcePath)
            throws URISyntaxException, IOException {

        // --- it reads an auxiliary file and decode a possible proj
        // --- definition to a simple string. Ex. "WGS 84 UTM Zone 32"
        int pos = fileSourcePath.lastIndexOf('.');
        // .shp, .dxf, .asc, .flt files
        String projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
        // image files
        String projectSourceRFilePrj = fileSourcePath + ".prj";
        String projectSourceRFileAux = fileSourcePath + ".aux.xml";

        String type = FilenameUtils.getExtension(fileSourcePath).toUpperCase();

        String textProj = "";
        if (type.matches("SHP|DXF|ASC|FLT|ADF|GRD|BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                Scanner scanner = new Scanner(new File(projectSourceFilePrj));
                textProj = scanner.nextLine();
                scanner.close();
            }
        } else if (new File(projectSourceRFileAux).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFileAux));
            textProj = scanner.useDelimiter("\\A").next();
            if (!textProj.contains("<WKT>") && !textProj.contains("<SRS>") &&
                    new File(projectSourceRFilePrj).exists()) {
                Scanner scanner2 = new Scanner(new File(projectSourceRFilePrj));
                textProj = scanner2.nextLine();
                scanner2.close();
            }
        } else if (new File(projectSourceRFilePrj).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFilePrj));
            textProj = scanner.nextLine();
            scanner.close();
        }

        String prjname = decodeProjDescription(textProj);

        // --- it extracts from proj register file all the info related
        // --- to the previous string (SRSDef). Ex.
        // --- "EPSG:32632 - WGS 84 UTM zone 32"
        if (!prjname.isEmpty()) {
            String[] srsAndUnit = SridLookupTable.getSrsAndUnitFromName(prjname);
            if (srsAndUnit == null) return PROJECTION_UNSPECIFIED;
            else return getPrefixedCode(srsAndUnit[0]) + " - " + srsAndUnit[1];
        } else {
            return PROJECTION_UNSPECIFIED;
        }
    }


    private static String getPrefixedCode(String code) {
        // 1) WKID <32768 or >5999999
        // will result in an AUTHORITY name of "EPSG".
        // 2) A WKID in range between 33000 and 199999
        // will result in an AUTHORITY name of "ESRI".
        // (http://help.arcgis.com/en/arcgisserver/10.0/apis/soap/whnjs.htm#SOAP_Geometry_FindSRByWKID.htm)

        String Registry = "SRID"; // Used if code is negative or non-numeric
        if (code.matches("\\d+")) {
            int srid = Integer.parseInt(code);
            if (srid < 32768 || srid > 5999999) {
                Registry = "EPSG";
            } else if (srid > 32999 && srid < 200000) {
                Registry = "ESRI";
            }
        }
        return Registry + ":" + code;
    }

    /**
     * Method to show an OGC WKT string in a more readable style
     *
     * @param WKT OGC WKT from auxiliary proj file
     * @return Readable string
     */
    public static String readableFormatWKTCode(String WKT) {
        String HROGC = "";
        // String add_spaces = String.format("%" + count_add++ + "s", "");
        HROGC = WKT.replace(",GEOGCS", ",<br>" + "GEOCS")
                .replace(",DATUM", ",<br>" + "DATUM")
                .replace(",SPHEROID", ",<br>" + "SPEROID")
                .replace("],", "],<br>");
        return HROGC;

    }

    /**
     * Decode a OGC string to get a unique SRS string definition. This method is
     * able to understand some WKT common aliases, like OGC WKT and ESRI WKTCode.
     * For instance: "WGS 84 / UTM zone 32", "WGS 1984 UTM zone 32" and
     * "WGS_84_UTM_Zone_32" are converted to the same string
     * "WGS 84 UTM zone 32"
     *
     * @param textProj
     *            <String> - OGC/ESRI/other WKT code
     * @return <String> - SRS definition
     */
    private static String decodeProjDescription(String textProj) {
        String prjname = "";
        try {
            // Workaround if aux.xml has been download from web.
            // convert HTML quotes [&quot;] to ["]
            textProj = textProj.replaceAll("&quot;", "\"");
            int start = textProj.indexOf("[\"");
            int end = textProj.indexOf("\",", start);
            prjname = textProj.substring(start + 2, end);
            // The following set of replacements allows to "harmonize" OGC, ESRI
            // and
            // few other WKT projection definitions
            prjname = prjname.replaceAll("_", " ").replace(" / ", " ")
                    .replaceAll("\\bft US\\b", "(ftUS)")
                    .replaceAll("\\bftUS\\b", "(ftUS)")
                    .replaceAll("\\bft\\b", "(ft)").replaceAll("feet", "ft")
                    .replaceAll("WGS 1984", "WGS 84")
                    .replaceAll("NAD 1983 UTM", "NAD83 UTM")
                    .replaceAll("HARN", "(HARN)")
                    .replaceAll("\\bCSRS98\\b", "(CSRS98)")
                    .replaceAll("CSRS", "(CSRS)")
                    .replaceAll("\\bNSRS2007\\b", "(NSRS2007)")
                    .replaceAll("\\bNAD27_76\\b", "NAD27(76)")
                    .replaceAll("\\bCGQ77\\b", " (CGQ77)")
                    .replaceAll("\\bED77\\b", "(ED77)")
                    .replaceAll("\\b1942 83\\b", "1942(83)")
                    .replaceAll("\\b1942 58\\b", "1942(58)")
                    .replaceAll("\\bSegara Jakarta\\b", "Segara (Jakarta)")
                    .replaceAll("\\bRome\\b", "(Rome)")
                    .replaceAll("\\bParis\\b", "(Paris)")
                    .replaceAll("\\bFerro\\b", "(Ferro)");

        } catch (Exception ex) {
            // If there is other info than a WKT definition in the aux file
            prjname = NOT_RECOGNIZED;
        }
        return prjname;
    }

    /**
     * returns OGC WKT string located between projection tags (<WKT> or <SRS>)
     * in a projection auxiliary file (AUX.XML)
     *
     * @param textProj
     *            string
     * @return OGC WKT string
     */
    private static String getWktProjDefinition(String textProj) {
        String prjname = "";
        try {
            if (textProj.contains("<WKT>")) {
                int start = textProj.indexOf("<WKT>");
                int end = textProj.indexOf("</WKT>", start);
                prjname = textProj.substring(start, end);
            } else if (textProj.contains("<SRS>")) {
                int start = textProj.indexOf("<SRS>");
                int end = textProj.indexOf("</SRS>", start);
                prjname = textProj.substring(start, end);
            } else
                prjname = textProj;
        } catch (Exception ex) {
            prjname = textProj;
        }
        return prjname;
    }

    /**
     * It returns the path name of the auxiliary file (AUX.XML or PRJ file)
     * where a projection code is located
     *
     * @param fileSourcePath auxiliary file path
     * @return path name of projection auxiliary file
     * @throws IOException
     */
    public static String getAuxiliaryProjFilePath(String fileSourcePath)
            throws IOException {

        String filename = "";
        Scanner scanner;
        int pos = fileSourcePath.lastIndexOf('.');
        String projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
        String projectSourceRFileAux = fileSourcePath + ".aux.xml";
        String projectSourceRFilePrj = fileSourcePath + ".prj";
        String type = FilenameUtils.getExtension(fileSourcePath).toUpperCase();

        if (type.matches("SHP|DXF|ASC|FLT|ADF|GRD|BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                filename = projectSourceFilePrj;
            }
        } else if (new File(projectSourceRFileAux).exists()) {
            scanner = new Scanner(new File(projectSourceRFileAux));
            String textProj = scanner.useDelimiter("\\A").next();
            scanner.close();
            if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
                filename = projectSourceRFileAux;
            } else if (new File(projectSourceRFilePrj).exists()) {
                filename = projectSourceRFilePrj;
            }
        } else if (new File(projectSourceRFilePrj).exists()) {
            filename = projectSourceRFilePrj;
        }
        return filename;
    }

    /**
     * Method to get a SRS (SRID code + SRID definition) using a search string.
     * It scans into the srid list (srid.txt) to find a correspondence between
     * the search string and lines of the srid.txt. If the source string
     * corresponds as substring to a line, it returns the complete line as
     * string.The code is biunivocal: it can use as searchQuerry either SRID
     * code ("26910" or Project definition ("NAD83 UTM zone 10N"). For instance,
     * search queries like "NAD83 UTM zone 10N" or "26910" both return
     * "SRID:26910 - NAD83 UTM zone 10N".
     *
     * @param searchQuery search quaery
     * @return SRID and Projection definition
     * @throws URISyntaxException
     * @throws IOException
     */

    public static String getSRSFromWkt(String searchQuery)
            throws URISyntaxException, IOException {

        String[] srsAndUnit = SridLookupTable.getSrsAndUnitFromCodeOrName(searchQuery);
        if (srsAndUnit == null) return PROJECTION_UNSPECIFIED;
        else return getPrefixedCode(srsAndUnit[0]) + " - " + srsAndUnit[1];
    }

    /**
     * Check if selected file is a GeoTIFF. This java code comes from Deegree
     * project org.deegree.tools.raster.MergeRaster
     * (https://github.com/camptocamp
     * /secureOWS/blob/master/owsproxyserver/src/org
     * /deegree/tools/raster/MergeRaster.java)
     *
     * @param fileSourcePath
     * @return true
     * @throws IOException
     */

    public static boolean isGeoTIFF(String fileSourcePath) throws IOException {
        FileSeekableStream fileSeekableStream = new FileSeekableStream(
                fileSourcePath);
        TIFFDirectory tifDir = new TIFFDirectory(fileSeekableStream, 0);
        // definition of a geotiff
        if (tifDir.getField(GeoTiffConstants.ModelPixelScaleTag) == null
                && tifDir.getField(GeoTiffConstants.ModelTransformationTag) == null
                && tifDir.getField(GeoTiffConstants.ModelTiepointTag) == null
                && tifDir.getField(GeoTiffConstants.GeoKeyDirectoryTag) == null
                && tifDir.getField(GeoTiffConstants.GeoDoubleParamsTag) == null
                && tifDir.getField(GeoTiffConstants.GeoAsciiParamsTag) == null) {
            return false;
        } else {
            // is a geotiff and possibly might need to be treated as raw data
            TIFFField bitsPerSample = tifDir.getField(258);
            if (bitsPerSample != null) {
                int samples = bitsPerSample.getAsInt(0);
                if (samples == 16)
                    new Integer(16);
            }
            // check the EPSG number
            TIFFField ff = tifDir.getField(GeoTiffConstants.GeoKeyDirectoryTag);
            if (ff == null) {
                return false;
            }
            char[] ch = ff.getAsChars();
            // resulting HashMap, containing the key and the array of values
            HashMap<Integer, int[]> geoKeyDirectoryTag = new HashMap<Integer, int[]>(
                    ff.getCount() / 4);
            // array of values. size is 4-1.
            int keydirversion, keyrevision, minorrevision, numberofkeys = -99;
            for (int i = 0; i < ch.length; i = i + 4) {
                int[] keys = new int[3];
                keydirversion = ch[i];
                keyrevision = ch[i + 1];
                minorrevision = ch[i + 2];
                numberofkeys = ch[i + 3];
                keys[0] = keyrevision;
                keys[1] = minorrevision;
                keys[2] = numberofkeys;
                geoKeyDirectoryTag.put(new Integer(keydirversion), keys);
            }
            int[] content = new int[3];
            if (geoKeyDirectoryTag.containsKey(new Integer(
                    GeoTiffConstants.ModelTiepointTag))) {
                content = (int[]) geoKeyDirectoryTag.get(new Integer(
                        GeoTiffConstants.ModelTiepointTag));
                // TIFFTagLocation
                if (content[0] == 0) {
                    // return Value_Offset key = content[2];
                } else {
                    // TODO other TIFFTagLocation that GeoKeyDirectoryTag
                }
            } else {
                Logger.warn("Can't check EPSG codes, make sure it is ok!");
            }
            return true;
        }
    }

    /**
     * - Read SRID from GeoTIFF tag - This method gets projection srid code from
     * a geotiff file. It first scans GeoKeyDirectoryTag to get either
     * geographic/geocentric (2048 - GeographicTypeGeoKey), projected (3072 -
     * ProjectedCSTypeGeoKey) or vertical (4096 - VerticalCSTypeGeoKey) info. If
     * no key ID is identified, it scans for GeoAsciiParamsTag projection
     * definition. Last choice, it search for an auxiliary file
     *
     * @param fileSourcePath
     *            . eg. "c\documents\folder\image.tif"
     * @return <String> projection srid code as string. eg "32632"
     * @throws IOException
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-access")
    public static String readSRIDFromGeoTiffFile(String fileSourcePath)
            throws IOException, URISyntaxException {
        String GeoDirTag = "";
        String GeoDirTag2 = "";
        String prjname = "";
        File tiffFile = new File(fileSourcePath);
        try {
            TiffImageParser parser = new TiffImageParser();
            TiffImageMetadata metadata = (TiffImageMetadata) parser
                    .getMetadata(tiffFile);
            if (metadata != null) {
                java.util.List<TiffField> tiffFields = metadata.getAllFields();
                GeoTiffConstants constants = new GeoTiffConstants();

                String ID = "";
                int start;
                for (TiffField tiffField : tiffFields) {
                    if (tiffField.getTag() == constants.GeoKeyDirectoryTag) {
                        GeoDirTag = tiffField.getValueDescription();
                    }
                    if (tiffField.getTag() == constants.GeoAsciiParamsTag) {
                        GeoDirTag2 = tiffField.getStringValue().replaceAll(
                                "[\\t\\n\\r\\_\\|]", " ");
                    }
                    if (tiffField.getTag() == constants.ModelTiepointTag) {
                    }
                }
                if (GeoDirTag.contains("3072")) {
                    start = GeoDirTag.indexOf("3072");
                    ID = GeoDirTag.substring(start);
                    String[] parts = ID.split(",");
                    String part1 = parts[3];
                    if (!part1.contains("32767")) {
                        prjname = part1.replaceAll(" ", "");
                    } else {
                        prjname = "0";
                    }
                } else if (!GeoDirTag.contains("3072")
                        & GeoDirTag.contains("4096")) {
                    start = GeoDirTag.indexOf("4096");
                    ID = GeoDirTag.substring(start);
                    String[] parts = ID.split(",");
                    String part1 = parts[3];
                    if (!part1.contains("32767")) {
                        prjname = part1.replaceAll(" ", "");
                    } else {
                        prjname = "0";
                    }
                } else if (!GeoDirTag.contains("3072")
                        & !GeoDirTag.contains("4096")
                        & GeoDirTag.contains("2048")) {
                    start = GeoDirTag.indexOf("2048");
                    ID = GeoDirTag.substring(start);
                    String[] parts = ID.split(",");
                    String part1 = parts[3];
                    if (!part1.contains("32767")) {
                        prjname = part1.replaceAll(" ", "");
                    } else {
                        prjname = "0";
                    }

                } else if (!GeoDirTag.contains("4096")
                        & !GeoDirTag.contains("3072")
                        & !GeoDirTag.contains("2048")) {
                    if (!GeoDirTag2.isEmpty())
                        // It gets "0" which is a non defined projection
                        prjname = "0";
                    else
                        prjname = readSRIDFromAuxiliaryFile(fileSourcePath);

                }
            }

        } catch (Exception ex) {
            prjname = PROJECTION_UNSPECIFIED;
        }
        return prjname;
    }

    /**
     * - Read SRID from auxiliary file - Method to get SRID code from auxiliary
     * projection file (AUX.XML or PRJ file). It scans into the registry file
     * (srid.txt) to find a correspondence between the search string (auxiliary
     * layer) and lines of the srid.txt. If the source string corresponds as
     * substring to a line, it returns its SRID. For instance, search strings
     * like "NAD83 UTM zone 10N" returns "26910".
     *
     * @param fileSourcePath auxiliary file path
     * @return SRID as String
     * @throws URISyntaxException
     * @throws IOException
     */

    public static String readSRIDFromAuxiliaryFile(String fileSourcePath)
            throws URISyntaxException, IOException {

        // --- it reads an auxiliary file and decode a possible proj
        // --- definition to a simple string. Ex. "WGS 84 UTM Zone 32"
        int pos = fileSourcePath.lastIndexOf('.');
        // .shp, .dxf, .asc, .flt files
        String projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
        // image files
        String projectSourceRFilePrj = fileSourcePath + ".prj";
        String projectSourceRFileAux = fileSourcePath + ".aux.xml";

        String type = FilenameUtils.getExtension(fileSourcePath).toUpperCase();

        String textProj = "";
        if (type.matches("SHP|DXF|ASC|FLT|ADF|GRD|BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                Scanner scanner = new Scanner(new File(projectSourceFilePrj));
                textProj = scanner.nextLine();
                scanner.close();
            }
        } else if (new File(projectSourceRFileAux).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFileAux));
            textProj = scanner.useDelimiter("\\A").next();
            if (!textProj.contains("<WKT>") && !textProj.contains("<SRS>") &&
                    new File(projectSourceRFilePrj).exists()) {
                Scanner scanner2 = new Scanner(new File(projectSourceRFilePrj));
                textProj = scanner2.nextLine();
                scanner2.close();
            }
        } else if (new File(projectSourceRFilePrj).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFilePrj));
            textProj = scanner.nextLine();
            scanner.close();
        }

        String prjname = decodeProjDescription(textProj);

        // --- it extracts from proj register file all the info related
        // --- to the previous string (SRSDef). Ex.
        // --- "EPSG:32632 - WGS 84 UTM zone 32"
        if (!prjname.isEmpty()) {
            String[] srsAndUnit = SridLookupTable.getSrsAndUnitFromName(prjname);
            if (srsAndUnit == null) return PROJECTION_UNSPECIFIED;
            else return getPrefixedCode(srsAndUnit[0]) + " - " + srsAndUnit[1];
        } else {
            return PROJECTION_UNSPECIFIED;
        }
    }

    /**
     * Method to get SRID from a layer from Style or from auxiliary file. First
     * scans SRIDStyle, than auxiliary file or GeoTIFF tag. If SRID does not
     * exist, it returns 0.
     *
     * @param layer
     * @return SRID
     * @throws Exception
     */
    public static int SRID(Layer layer) throws Exception {
        String fileSourcePath = "";
        int projection = 0;
        String extension = "";
        SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
        final int oldSRID = sridStyle.getSRID();
        // if (layers.length == 1) {
        // First we check if a SRID (Spatial Reference Identifier)
        // code has been recorded by OJ (eg. Spatialite)
        // it excludes 0 from the range of search as it can
        // be consider as "no SRID"
        if (oldSRID > 0) {
            projection = oldSRID;
            // If no SRID has been identified. it checks
            // projection info into external auxiliary files (.prj,
            // aux.xml) or as Geotiff tag
        } else {// Check if selected layer is related to an image file
            if (isImageFileLayer(layer)) {
                FeatureCollection featureCollection = layer
                        .getFeatureCollectionWrapper();
                String sourcePathImage = null;
                for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    sourcePathImage = (String) feature
                            .getString(ImageryLayerDataset.ATTR_URI);
                    sourcePathImage = sourcePathImage.substring(5);
                    File f = new File(sourcePathImage);
                    String filePath = f.getAbsolutePath();
                    String filePath1 = filePath.replace("%20", " ");
                    fileSourcePath = filePath1;

                }
                extension = FileUtil.getExtension(fileSourcePath).toUpperCase();
                if ((extension.equals("TIF") || extension.equals("TIFF"))) {
                    // If TIFF file is a geotiff, it scans into
                    // embedded tag
                    if (ProjUtils.isGeoTIFF(fileSourcePath)) {

                        projection = Integer.parseInt(ProjUtils
                                .readSRIDFromGeoTiffFile(fileSourcePath));
                        // If the TIF file is not a GeiTIFF it looks
                        // for a proj code into aux files
                    } else {
                        projection = Integer.parseInt(ProjUtils
                                .readSRIDFromAuxiliaryFile(fileSourcePath));
                    }
                } else {
                    if (fileSourcePath != null) {
                        projection = Integer.parseInt(ProjUtils
                                .readSRIDFromAuxiliaryFile(fileSourcePath));
                    }
                }// Check if source file is is a file-based vector
            } else { // Only Vector files
                if (!isDataBaseLayer(layer)) {
                    DataSourceQuery dsq = layer.getDataSourceQuery();
                    String sourceClass = "";
                    String sourcePath = "";
                    String dsqSourceClass = dsq.getDataSource().getClass()
                            .getName();
                    if (sourceClass.equals("")) {
                        sourceClass = dsqSourceClass;
                    }
                    Object fnameObj = dsq.getDataSource().getProperties()
                            .get("File");
                    sourcePath = fnameObj.toString();
                    fileSourcePath = sourcePath;
                    projection = Integer.parseInt(ProjUtils
                            .readSRIDFromAuxiliaryFile(fileSourcePath));
                } else {

                    projection = 0;
                }
            }
        }

        return projection;

    }

    /**
     * Method to get SRID from a layer file from auxiliary files (.prj or .aux)
     * or GeoTIFFed tag. If the auxiliary file SRID does not exist, it returns
     * 0.
     *
     * @param layer
     * @return SRID
     * @throws Exception
     */
    public static int SRIDFromFile(Layer layer) throws Exception {
        String fileSourcePath = "";
        int SRID = 0;
        String extension = "";
        // First check if selected Layer has an image datasource file
        if (isImageFileLayer(layer)) {
            FeatureCollection featureCollection = layer
                    .getFeatureCollectionWrapper();
            String sourcePathImage = null;
            for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                sourcePathImage = (String) feature
                        .getString(ImageryLayerDataset.ATTR_URI);
                sourcePathImage = sourcePathImage.substring(5);
                File f = new File(sourcePathImage);
                String filePath = f.getAbsolutePath();
                String filePath1 = filePath.replace("%20", " ");
                fileSourcePath = filePath1;

            }
            extension = FileUtil.getExtension(fileSourcePath).toUpperCase();
            if ((extension.equals("TIF") || extension.equals("TIFF"))) {
                // If TIFF file is a geotiff, it scans into
                // embedded tag
                if (ProjUtils.isGeoTIFF(fileSourcePath)) {

                    SRID = Integer.parseInt(ProjUtils
                            .readSRIDFromGeoTiffFile(fileSourcePath));
                    // If the TIF file is not a GeiTIFF it looks
                    // for a proj code into aux files
                } else {
                    SRID = Integer.parseInt(ProjUtils
                            .readSRIDFromAuxiliaryFile(fileSourcePath));
                }
                // For all other image file types, not TIF
            } else {
                if (fileSourcePath != null) {
                    SRID = Integer.parseInt(ProjUtils
                            .readSRIDFromAuxiliaryFile(fileSourcePath));
                }
            }
            // Than check if source file is is a file-based vector
        } else {
            // Only Vector files, excluding databases:
            // OpenJUMP has different approches to detect
            // SRIDs for them
            if (!isDataBaseLayer(layer)) {
                DataSourceQuery dsq = layer.getDataSourceQuery();
                String sourceClass = "";
                String sourcePath = "";
                String dsqSourceClass = dsq.getDataSource().getClass()
                        .getName();
                if (sourceClass.equals("")) {
                    sourceClass = dsqSourceClass;
                }
                Object fnameObj = dsq.getDataSource().getProperties()
                        .get("File");
                sourcePath = fnameObj.toString();
                fileSourcePath = sourcePath;
                SRID = Integer.parseInt(ProjUtils
                        .readSRIDFromAuxiliaryFile(fileSourcePath));
            } else {
                SRID = 0;
            }
        }
        return SRID;
    }

    // Boolean. Selected layer is related to an image file
    private static boolean isImageFileLayer(Layer layer) {
        if (layer.getStyle(ReferencedImageStyle.class) != null
                && (layer.getDescription() != null)) {
            return true;
        } else {
            return false;
        }
    }

    // Boolean. Selected layer is related to a database
    private static boolean isDataBaseLayer(Layer layer) {
        DataSourceQuery dsq = layer.getDataSourceQuery();
        if (dsq == null || dsq.getDataSource() instanceof DataStoreDataSource) {
            return true;
        } else {
            return false;
        }
    }

}

