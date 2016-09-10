package org.openjump.core.ccordsys.utils;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.FileUtil;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.plugin.datastore.DataStoreDataSource;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.io.FilenameUtils;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.openjump.core.rasterimage.TiffTags;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
    public static SRSInfo getSRSInfoFromGeoTiff(String fileSourcePath)
            throws IOException, URISyntaxException, TiffTags.TiffReadingException, ImageReadException {
        return TiffTags.readMetadata(new File(fileSourcePath)).getSRSInfo();
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
     * @return SRSInfo and Projection definition
     * @throws URISyntaxException
     * @throws IOException
     */

    public static SRSInfo getSRSInfoFromAuxiliaryFile(String fileSourcePath)
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

        SRSInfo srsInfo = new SRSInfo();
        String textProj = "";
        if (type.matches("SHP|DXF|ASC|FLT|ADF|GRD|BIL")) {
            if (new File(projectSourceFilePrj).exists()) {
                Scanner scanner = new Scanner(new File(projectSourceFilePrj));
                textProj = scanner.nextLine();
                srsInfo = new SRSInfo().setSource(projectSourceFilePrj);
                scanner.close();
            }
        } else if (new File(projectSourceRFileAux).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFileAux));
            textProj = scanner.useDelimiter("\\A").next();
            if (!textProj.contains("<WKT>") && !textProj.contains("<SRS>") &&
                    new File(projectSourceRFilePrj).exists()) {
                Scanner scanner2 = new Scanner(new File(projectSourceRFilePrj));
                textProj = getWktProjDefinition(scanner2.nextLine());
                srsInfo = new SRSInfo().setSource(projectSourceRFileAux);
                scanner2.close();
            }
        } else if (new File(projectSourceRFilePrj).exists()) {
            Scanner scanner = new Scanner(new File(projectSourceRFilePrj));
            textProj = scanner.nextLine();
            srsInfo = new SRSInfo().setSource(projectSourceRFilePrj);
            scanner.close();
        }

        String prjName = decodeProjDescription(textProj);
        srsInfo.setDescription(prjName);
        srsInfo.complete();
        return srsInfo;
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
                .replace(",SPHEROID", ",<br>" + "SPHEROID")
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
     * Method to get SRID from a layer from Style or from auxiliary file. First
     * scans SRIDStyle, than auxiliary file or GeoTIFF tag. If SRID does not
     * exist, it returns 0.
     *
     * @param layer
     * @return SRID
     * @throws Exception
     */
    public static SRSInfo getSRSInfoFromLayerStyleOrSource(Layer layer) throws Exception {
        SRSInfo srsInfo = new SRSInfo();

        // First we check if a SRID != 0 has already been recorded for this Layer
        SRIDStyle sridStyle = (SRIDStyle) layer.getStyle(SRIDStyle.class);
        final int oldSRID = sridStyle.getSRID();
        if (oldSRID > 0) {
            srsInfo.setCode(Integer.toString(oldSRID));
            srsInfo.complete();
            // If no SRID has been identified, search for a SRID in geotiff tagg
            // or into an auxiliary file
        } else {
            srsInfo = getSRSInfoFromLayerSource(layer);
        }

        return srsInfo;

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
    public static SRSInfo getSRSInfoFromLayerSource(Layer layer) throws Exception {
        String fileSourcePath = "";
        SRSInfo srsInfo = new SRSInfo();
        // Raster layer case
        if (isImageFileLayer(layer)) {
            FeatureCollection featureCollection = layer
                    .getFeatureCollectionWrapper();
            String sourcePathImage = null;
            for (Iterator<?> i = featureCollection.iterator(); i.hasNext();) {
                Feature feature = (Feature) i.next();
                sourcePathImage = feature.getString(ImageryLayerDataset.ATTR_URI);
                if (sourcePathImage != null && sourcePathImage.length()>5) {
                    sourcePathImage = sourcePathImage.substring(5);
                    File f = new File(sourcePathImage);
                    if (f.exists()) {
                        fileSourcePath = f.getAbsolutePath().replace("%20", " ");
                        break;
                    }
                }
            }
            String extension = FileUtil.getExtension(fileSourcePath).toUpperCase();
            if ((extension.equals("TIF") || extension.equals("TIFF"))) {
                // If TIFF file is a geotiff, it scans into embedded tag
                if (ProjUtils.isGeoTIFF(fileSourcePath)) {
                    srsInfo = TiffTags.readMetadata(new File(fileSourcePath)).getSRSInfo();
                    // If the TIF file is not a GeoTIFF it looks
                    // for a proj code into aux files
                } else {
                    srsInfo = getSRSInfoFromAuxiliaryFile(fileSourcePath);
                }
            } else {
                srsInfo = getSRSInfoFromAuxiliaryFile(fileSourcePath);
            }
        }
        // Vector layer case
        else {
            if (!isDataBaseLayer(layer)) {
                DataSourceQuery dsq = layer.getDataSourceQuery();
                Object fnameObj = dsq.getDataSource().getProperties().get("File");
                fileSourcePath = fnameObj.toString();
                srsInfo = getSRSInfoFromAuxiliaryFile(fileSourcePath);
            }
        }
        return srsInfo;
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

