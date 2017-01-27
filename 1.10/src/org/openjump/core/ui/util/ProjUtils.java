package org.openjump.core.ui.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.openjump.core.rasterimage.GeoTiffConstants;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.FileUtil;

public class ProjUtils {

	private static final String PROJECTION_UNSPECIFIED = I18N
			.get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.unknown_projection");

	/*
	 * Giuseppe Aruta <23_3_2015> This class is used to recognize file
	 * projection. It first scans the file to find projection metadata
	 * (currently only GeoTIFF). If the Geotiff file has not projection tag or
	 * it is not a Geotiff file, it scans to find projection info - first to an
	 * <file>.aux.xml - than to <file>.proj file. This class only checks the
	 * presence of defined projection sub-codes located at the beginning of
	 * OGC.WKT or ESRI.WKT projection codes. Currently this code works only
	 * with: org.openjump.core.ui.plugin.raster.RasterImageLayerProperty.class
	 */

	/**
	 * It gets projection metadata from a geotiff file
	 * 
	 * @param fileSourcePath
	 * @return <String> projection code as string
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public static String getGeoTiffProjection(String fileSourcePath)
			throws IOException {
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
					if (tiffField.getTag() == constants.GeoAsciiParamsTag) {
						prjname = tiffField.getStringValue().replaceAll(
								"[\\t\\n\\r\\_\\|]", " ");
						break;
					}
				}
			} else {
				prjname = getFileProjection(fileSourcePath);
			}
		} catch (Exception ex) {
			prjname = getFileProjection(fileSourcePath);
		}
		return prjname;
	}

	/**
	 * It gets projection code as string from an auxiliary file (AUX.XML or PRJ
	 * file)
	 * 
	 * @param fileSourcePath
	 * @return <String> projection code as string
	 * @throws IOException
	 */
	public static String getFileProjection(String fileSourcePath)
			throws IOException {
		String projectSourceFilePrj = "";
		String projectSourceFileAux = "";
		String textProj = "";
		String prjname = "";
		Scanner scanner;
		int pos = fileSourcePath.lastIndexOf('.');
		projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
		projectSourceFileAux = fileSourcePath + ".aux.xml";
		if ((new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				int start = textProj.indexOf('[');
				int end = textProj.indexOf(',', start);
				prjname = textProj.substring(start + 2, end - 1).replaceAll(
						"[\\t\\n\\r\\_]", " ");
			} else {
				scanner = new Scanner(new File(projectSourceFilePrj));
				textProj = scanner.nextLine();
				scanner.close();
				int start = textProj.indexOf('[');
				int end = textProj.indexOf(',', start);
				prjname = textProj.substring(start + 2, end - 1).replaceAll(
						"[\\t\\n\\r\\_]", " ");
			}
		} else if ((new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				int start = textProj.indexOf('[');
				int end = textProj.indexOf(',', start);
				prjname = textProj.substring(start + 2, end - 1).replaceAll(
						"[\\t\\n\\r\\_]", " ");
			} else {
				prjname = PROJECTION_UNSPECIFIED;
			}
		} else if (!(new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFilePrj));
			textProj = scanner.nextLine();
			scanner.close();
			int start = textProj.indexOf('[');
			int end = textProj.indexOf(',', start);
			prjname = textProj.substring(start + 2, end - 1).replaceAll(
					"[\\t\\n\\r\\_]", " ");
		} else if (!(new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			prjname = PROJECTION_UNSPECIFIED;
		}
		return prjname;
	}

	/**
	 * It returns the path name of the auxiliary file where a projection code is
	 * located
	 * 
	 * @param fileSourcePath
	 * @return <String> path name of projection auxiliary file
	 * @throws IOException
	 */
	public static String getFileProjectionPath(String fileSourcePath)
			throws IOException {
		String projectSourceFilePrj = "";
		String projectSourceFileAux = "";
		String textProj = "";
		String filename = "";
		Scanner scanner;
		int pos = fileSourcePath.lastIndexOf('.');
		projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
		projectSourceFileAux = fileSourcePath + ".aux.xml";
		if ((new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				filename = projectSourceFileAux;
			} else {
				filename = projectSourceFilePrj;
			}
		} else if ((new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				filename = projectSourceFileAux;
			} else {
				filename = "";
			}
		} else if (!(new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			filename = projectSourceFilePrj;
		} else if (!(new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			filename = "";
		}
		return filename;
	}

	/**
	 * Check if selected file is a GeoTIFF. This java code comes from Deegree
	 * project
	 * 
	 * @param fileSourcePath
	 * @return
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
				System.out
						.println("Can't check EPSG codes, make sure it is ok!");
			}
			return true;
		}
	}

	/**
	 * Check if selected auxiliary (AUX.XML or PRJ) file has projection code
	 * 
	 * @param fileSourcePath
	 * @return
	 * @throws IOException
	 */
	public static boolean hasProjectionFile(String fileSourcePath)
			throws IOException {
		String projectSourceFilePrj = "";
		String projectSourceFileAux = "";
		String textProj = "";
		Scanner scanner;
		int pos = fileSourcePath.lastIndexOf('.');
		projectSourceFilePrj = fileSourcePath.substring(0, pos) + ".prj";
		projectSourceFileAux = fileSourcePath + ".aux.xml";
		if ((new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				return true;

			} else {

				scanner = new Scanner(new File(projectSourceFilePrj));
				textProj = scanner.nextLine();
				scanner.close();
				if (textProj.contains("PROJCS") || textProj.contains("GEOGCS"))
					return true;
			}
		} else if ((new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFileAux));
			textProj = scanner.useDelimiter("\\A").next();
			scanner.close();
			if (textProj.contains("<WKT>") || textProj.contains("<SRS>")) {
				return true;

			} else {
				return false;
			}
		} else if (!(new File(projectSourceFileAux).exists())
				& (new File(projectSourceFilePrj).exists())) {
			scanner = new Scanner(new File(projectSourceFilePrj));
			textProj = scanner.nextLine();
			scanner.close();
			if (textProj.contains("PROJCS") || textProj.contains("GEOGCS")) {
				return true;
			} else {
				return false;
			}

		} else if (!(new File(projectSourceFileAux).exists())
				& !(new File(projectSourceFilePrj).exists())) {
			return false;
		}
		return false;
	}

	/**
	 * Check if selected file has a projection code somewhere (as a GeoTIFF tag
	 * or included into an AUX.XML or PRJ auziliary files)
	 * 
	 * @param fileSourcePath
	 * @return
	 * @throws IOException
	 */
	public static boolean hasProjection(String fileSourcePath)
			throws IOException {
		String extension = FileUtil.getExtension(fileSourcePath);
		if ((extension.equals("tif") || extension.equals("tiff")
				|| extension.equals("TIF") || extension.equals("TIFF"))) {
			if (isGeoTIFF(fileSourcePath)) {
				return true;
			} else {
				if (hasProjectionFile(fileSourcePath))
					return true;
			}
		} else if (hasProjectionFile(fileSourcePath))
			return true;
		return false;
	}
}
