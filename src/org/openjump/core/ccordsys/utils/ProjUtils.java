package org.openjump.core.ccordsys.utils;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.io.FilenameUtils;
import org.openjump.core.rasterimage.GeoTiffConstants;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;

public class ProjUtils {

    private static final String PROJECTION_UNSPECIFIED = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.unknown_projection");
    private static final String USER_DEFINED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.User_defined");
    private static final String NOT_RECOGNIZED = I18N
            .get("org.openjump.core.ui.plugin.layer.LayerPropertiesPlugIn.Not_recognized");

    /*
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
        double[] doubles;
        FileSeekableStream fileSeekableStream = new FileSeekableStream(
                fileSourcePath);
        TIFFDirectory tiffDirectory = new TIFFDirectory(fileSeekableStream, 0);

        TIFFField[] availTags = tiffDirectory.getFields();

        for (TIFFField availTag : availTags) {
            if (availTag.getTag() == GeoTiffConstants.ModelTiepointTag) {
                doubles = availTag.getAsDoubles();
                if (doubles.length != 6) {
                    throw new Exception(
                            "unsupported value for ModelTiepointTag ("
                                    + GeoTiffConstants.ModelTiepointTag + ")");
                }
                if (doubles[0] != 0 || doubles[1] != 0 || doubles[2] != 0) {
                    if (doubles[2] == 0)
                        pixelOffset = new Coordinate(doubles[0], doubles[1]);
                    else
                        pixelOffset = new Coordinate(doubles[0], doubles[1],
                                doubles[2]);
                }
                if (doubles[5] == 0)
                    tiePoint = new Coordinate(doubles[3], doubles[4]);
                else
                    tiePoint = new Coordinate(doubles[3], doubles[4],
                            doubles[5]);

            } else if (availTag.getTag() == GeoTiffConstants.ModelPixelScaleTag) {
                // Karteneinheiten pro pixel x bzw. y
                doubles = availTag.getAsDoubles();
                if (doubles[2] == 0)
                    pixelScale = new Coordinate(doubles[0], doubles[1]);
                else
                    pixelScale = new Coordinate(doubles[0], doubles[1],
                            doubles[2]);
            } else {
            }
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
                List<TiffField> tiffFields = metadata.getAllFields();
                GeoTiffConstants constants = new GeoTiffConstants();
                for (TiffField tiffField : tiffFields) {
                    if (tiffField.getTag() == constants.GeoKeyDirectoryTag) {
                        String GeoDirTag = tiffField.getValueDescription();
                        prjname = GeoDirTag;
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
     * @return <String> projection srid code as string. eg "32632"
     * @throws IOException
     * @throws URISyntaxException
     */
    @SuppressWarnings("static-access")
    public static String readSRSFromGeoTiffFile(String fileSourcePath)
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
                List<TiffField> tiffFields = metadata.getAllFields();
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
                        prjname = getSRSFromWkt(part1.replaceAll(" ", ""));
                    } else {
                        prjname = "SRID: " + USER_DEFINED + " - " + GeoDirTag2;
                    }
                } else if (!GeoDirTag.contains("3072")
                        & GeoDirTag.contains("4096")) {
                    start = GeoDirTag.indexOf("4096");
                    ID = GeoDirTag.substring(start);
                    String[] parts = ID.split(",");
                    String part1 = parts[3];
                    if (!part1.contains("32767")) {
                        prjname = getSRSFromWkt(part1.replaceAll(" ", ""));
                    } else {
                        prjname = "SRID: " + USER_DEFINED + " - " + GeoDirTag2;
                    }
                } else if (!GeoDirTag.contains("3072")
                        & !GeoDirTag.contains("4096")
                        & GeoDirTag.contains("2048")) {
                    start = GeoDirTag.indexOf("2048");
                    ID = GeoDirTag.substring(start);
                    String[] parts = ID.split(",");
                    String part1 = parts[3];
                    if (!part1.contains("32767")) {
                        prjname = getSRSFromWkt(part1.replaceAll(" ", ""));
                    } else {
                        prjname = "SRID: " + USER_DEFINED + " - " + GeoDirTag2;
                    }

                } else if (!GeoDirTag.contains("4096")
                        & !GeoDirTag.contains("3072")
                        & !GeoDirTag.contains("2048")) {
                    if (!GeoDirTag2.isEmpty())
                        // It gets "0" which is a non defined projection
                        prjname = "SRID: " + NOT_RECOGNIZED + " - "
                                + GeoDirTag2;
                    else
                        prjname = readSRSFromAuxiliaryFile(fileSourcePath);

                }
            }

        } catch (Exception ex) {
            prjname = PROJECTION_UNSPECIFIED;
        }
        return prjname;
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
     * @param auxiliary
     *            file path
     * @return <String> SRID and Projection definition
     * @throws URISyntaxException
     * @throws IOException
     */

    public static String readSRSFromAuxiliaryFile(String fileSourcePath)
            throws URISyntaxException, IOException {
        InputStream is = ProjUtils.class.getResourceAsStream("srid.txt");
        InputStreamReader isr = new InputStreamReader(is);
        String projectSourceFilePrj = "";
        String projectSourceRFilePrj = "";
        String projectSourceRFileAux = "";
        String textProj = "";
        String prjname = "";
        String SRSDef = PROJECTION_UNSPECIFIED;
        Scanner scanner;
        // --- it reads an auxiliary file and decode a possible proj
        // --- definition to a simple string. Ex. "WGS 84 UTM Zone 32"
        int pos = fileSourcePath.lastIndexOf('.');
        // .shp, .dxf, .asc, .flt files
        projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
        // image files
        projectSourceRFilePrj = fileSourcePath + ".prj";
        projectSourceRFileAux = fileSourcePath + ".aux.xml";
        List<String> fileList = new ArrayList<String>();
        fileList.add(projectSourceFilePrj);
        fileList.add(projectSourceRFilePrj);
        fileList.add(projectSourceRFileAux);

        if (fileList.isEmpty())
            SRSDef = PROJECTION_UNSPECIFIED;

        String type = FilenameUtils.getExtension(fileSourcePath).toUpperCase();

        if (type.equals("SHP") || type.equals("DXF") || type.equals("ASC")
                || type.equals("FLT") || type.equals("ADF")
                || type.equals("GRD") || type.equals("BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                scanner = new Scanner(new File(projectSourceFilePrj));
                textProj = scanner.nextLine();
                scanner.close();
                prjname = decodeProjDescription(textProj);
            } else {
                SRSDef = PROJECTION_UNSPECIFIED;
            }
        }

        else {
            if ((new File(projectSourceRFileAux).exists())
                    & (new File(projectSourceRFilePrj).exists())) {
                scanner = new Scanner(new File(projectSourceRFileAux));
                textProj = scanner.useDelimiter("\\A").next();
                // scanner.close();
                if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
                    prjname = decodeProjDescription(textProj);
                } else {
                    scanner = new Scanner(new File(projectSourceRFilePrj));
                    textProj = scanner.nextLine();
                    scanner.close();
                    prjname = decodeProjDescription(textProj);
                }
            }
            if ((new File(projectSourceRFileAux).exists())
                    & !(new File(projectSourceRFilePrj).exists())) {
                scanner = new Scanner(new File(projectSourceRFileAux));
                textProj = scanner.useDelimiter("\\A").next();
                // scanner.close();
                if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
                    prjname = decodeProjDescription(textProj);
                } else {
                    SRSDef = PROJECTION_UNSPECIFIED;
                }
            } else if (!(new File(projectSourceRFileAux).exists())
                    & (new File(projectSourceRFilePrj).exists())) {
                scanner = new Scanner(new File(projectSourceRFilePrj));
                textProj = scanner.nextLine();
                // scanner.close();
                prjname = decodeProjDescription(textProj);
            }

            else if (!(new File(projectSourceRFileAux).exists())
                    & (!new File(projectSourceRFilePrj).exists())) {
                SRSDef = PROJECTION_UNSPECIFIED;

            }
        }
        // --- it extracts from proj register file all the info related
        // --- to the previous string (SRSDef). Ex.
        // --- "EPSG:32632 - WGS 84 UTM zone 32"
        if (!prjname.isEmpty()) {
            scanner = new Scanner(isr);
            try {
                while (scanner.hasNextLine()) {
                    scanner.useDelimiter("\\n");
                    String line = scanner.nextLine();
                    String line2 = line.replaceAll("[\\t\\n\\r\\_]", "");
                    if (line2.toLowerCase().contains(
                            "<" + prjname.toLowerCase() + ">")) {
                        int start = line2.indexOf('<');
                        int end = line2.indexOf('>', start);
                        String def = line2.substring(start + 1, end);
                        int srid = Integer.parseInt(def);
                        if (srid < 32768 || srid > 5999999) {
                            // EPSG code between 0 and 32767
                            SRSDef = "EPSG:"
                                    + line2.replaceAll("[<\\>]", " ")
                                            .replaceAll(";", " - ");
                            // ESRI codes range
                        } else if (srid > 37000 & srid < 202003) {
                            SRSDef = "ESRI:"
                                    + line2.replaceAll("[<\\>]", " ")
                                            .replaceAll(";", " - ");
                            // Other no EPSG or ESRI codes
                        } else {
                            SRSDef = "SRID:"
                                    + line2.replaceAll("[<\\>]", " ")
                                            .replaceAll(";", " - ");
                        }
                        break;
                    } else {
                        // --- If no SRSDef is recognized into the register, it
                        // --- returns a proj string into a more readable text
                        SRSDef = readableFormatWKTCode(getWktrojDefinition(textProj));
                    }
                }
                scanner.close();
            } catch (Exception e) {
                SRSDef = PROJECTION_UNSPECIFIED;
            }
        } else {
            SRSDef = PROJECTION_UNSPECIFIED;
        }
        return SRSDef;
    }

    /**
     * Method to show an OGC WKT string in a more readable style
     * 
     * @param String
     *            OGC WKT from auxiliary proj file
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
     * able to understand some WKT common aliases, like OGC WKT and ESRI WKTode.
     * For instance: "WGS 84 / UTM zone 32", "WGS 1984 UTM zone 32" and
     * "WGS_84_UTM_Zone_32" are converted to the same string
     * "WGS 84 UTM zone 32"
     * 
     * @param textProj
     *            <String> - OGC/ESRI/other WKT code
     * @return <String> - SRS definition
     */
    private static String decodeProjDescription(String textProj) {
        int start = textProj.indexOf("[\"");
        int end = textProj.indexOf("\",", start);
        String prjname = "";
        prjname = textProj.substring(start + 2, end);
        // The following set of replacements allows to "harmonize" OGC, ESRI and
        // few other WKT projection definitions
        prjname = prjname.replaceAll("_", " ").replace(" / ", " ")
                .replaceAll("\\bft US\\b", "(ftUS)")
                .replaceAll("\\bftUS\\b", "(ftUS)")
                .replaceAll("\\bft\\b", "(ft)").replace("feet", "ft")
                .replace("WGS 1984", "WGS 84")
                .replace("NAD 1983 UTM", "NAD83 UTM").replace("HARN", "(HARN)")
                .replace("\\bCSRS98\\b", "(CSRS98)").replace("CSRS", "(CSRS)")
                .replace("\\bNSRS2007\\b", "(NSRS2007)")
                .replace("\\bNAD27_76\\b", "NAD27(76)")
                .replace("\\bCGQ77\\b", " (CGQ77)")
                .replace("\\bED77\\b", "(ED77)")
                .replace("\\b1942 83\\b", "1942(83)")
                .replace("\\b1942 58\\b", "1942(58)")
                .replace("\\bSegara Jakarta\\b", "Segara (Jakarta)")
                .replace("\\bRome\\b", "(Rome)")
                .replace("\\bParis\\b", "(Paris)")
                .replace("\\bFerro\\b", "(Ferro)");

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
    private static String getWktrojDefinition(String textProj) {
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
     * @param auxiliary
     *            file path
     * @return <String> path name of projection auxiliary file
     * @throws IOException
     */
    public static String getAuxiliaryProjFilePath(String fileSourcePath)
            throws IOException {
        String projectSourceFilePrj = "";
        String projectSourceRFilePrj = "";
        String projectSourceRFileAux = "";
        String textProj = "";
        String filename = "";
        Scanner scanner;
        int pos = fileSourcePath.lastIndexOf('.');
        projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
        projectSourceRFileAux = fileSourcePath + ".aux.xml";
        projectSourceRFilePrj = fileSourcePath + ".prj";
        String type = FilenameUtils.getExtension(fileSourcePath).toUpperCase();

        if (type.equals("SHP") || type.equals("DXF") || type.equals("ASC")
                || type.equals("FLT") || type.equals("ADF")
                || type.equals("GRD") || type.equals("BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                filename = projectSourceFilePrj;
            } else {
                filename = "";
            }
        } else {

            if ((new File(projectSourceRFileAux).exists())
                    & (new File(projectSourceRFilePrj).exists())) {
                scanner = new Scanner(new File(projectSourceRFileAux));
                textProj = scanner.useDelimiter("\\A").next();
                scanner.close();
                if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
                    filename = projectSourceRFileAux;
                } else {
                    filename = projectSourceRFilePrj;
                }
            } else if ((new File(projectSourceRFileAux).exists())
                    & !(new File(projectSourceRFilePrj).exists())) {
                scanner = new Scanner(new File(projectSourceRFileAux));
                textProj = scanner.useDelimiter("\\A").next();
                scanner.close();
                if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
                    filename = projectSourceRFileAux;
                } else {
                    filename = "";
                }
            } else if (!(new File(projectSourceRFileAux).exists())
                    & (new File(projectSourceRFilePrj).exists())) {
                filename = projectSourceRFilePrj;
            } else if (!(new File(projectSourceRFileAux).exists())
                    & !(new File(projectSourceRFilePrj).exists())) {
                filename = "";
            }
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
     * searche querries like "NAD83 UTM zone 10N" or "26910" both return
     * "SRID:26910 - NAD83 UTM zone 10N".
     * 
     * @param <String>
     *            searchQuery
     * @return <String> SRID and Projection definition
     * @throws URISyntaxException
     * @throws IOException
     */

    public static String getSRSFromWkt(String searchQuery)
            throws URISyntaxException, IOException {

        InputStream is = ProjUtils.class.getResourceAsStream("srid.txt");
        InputStreamReader isr = new InputStreamReader(is);
        String SRSDef = "";
        Scanner scanner = null;
        try {
            scanner = new Scanner(isr);
            while (scanner.hasNextLine()) {
                scanner.useDelimiter("\\n");
                String line = scanner.nextLine();
                String line2 = line.replaceAll("[\\t\\n\\r\\_]", "");
                if (line2.toLowerCase().contains(
                        "<" + searchQuery.toLowerCase() + ">")) {
                    int start = line2.indexOf('<');
                    int end = line2.indexOf('>', start);
                    String def = line2.substring(start + 1, end);
                    int srid = Integer.parseInt(def);
                    if (srid < 37201) {
                        SRSDef = "EPSG:"
                                + line2.replaceAll("[<\\>]", " ").replaceAll(
                                        ";", " - ");
                    } else {
                        SRSDef = "ESRI:"
                                + line2.replaceAll("[<\\>]", " ").replaceAll(
                                        ";", " - ");
                    }
                    break;
                } else {

                    SRSDef = searchQuery;
                }
            }
        } finally {
            try {
                if (scanner != null)
                    scanner.close();
            } catch (Exception e) {
                System.err.println("Exception while closing scanner "
                        + e.toString());
            }
        }
        return SRSDef;
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

}
