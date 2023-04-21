package org.openjump.core.rasterimage;

import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.xml.sax.SAXException;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.Viewport;

/**
 *
 * @author deluca
 */
public class RasterImageIO {

	public ImageAndMetadata loadImage(
			String fileNameOrURL, Stats stats, Envelope viewPortEnvelope,
			Resolution requestedRes) throws Exception {

		if (fileNameOrURL.toLowerCase().endsWith(".jpg")
				|| fileNameOrURL.toLowerCase().endsWith(".gif")
				|| fileNameOrURL.toLowerCase().endsWith(".png")
				|| fileNameOrURL.toLowerCase().endsWith(".bmp")
				|| fileNameOrURL.toLowerCase().endsWith(".jp2")) {

			BufferedImage bImage;
			try {
				// Try with ImageIO
				bImage = ImageIO.read(new File(fileNameOrURL));
			} catch (Exception ex) {
				// Try with JAI
				bImage = JAI.create("fileload", fileNameOrURL)
						.getAsBufferedImage();
			}

			if (stats == null) {

				ParameterBlock pb = new ParameterBlock();
				pb.addSource(bImage); // The source image
				pb.add(null); // null ROI means whole image
				pb.add(1); // check every pixel horizontally
				pb.add(1); // check every pixel vertically

				// Mean
				RenderedImage meanImage = JAI.create("mean", pb, null);
				double[] mean = (double[]) meanImage.getProperty("mean");

				int nCols = bImage.getWidth();
				int nRows = bImage.getHeight();
				int nBands = bImage.getData().getNumBands();
				long nCells = nCols * nRows;

				// StdDev
				double[] stdDev = new double[nBands];
				DataBuffer dataBuffer = bImage.getData().getDataBuffer();
				for (int r = 0; r < nRows; r++) {
					for (int c = 0; c < nCols; c++) {
						for (int b = 0; b < nBands; b++) {

							double val = Math.pow(dataBuffer.getElemDouble(b)
									- mean[b], 2);
							stdDev[b] += val;
						}
					}
				}

				for (int b = 0; b < nBands; b++) {
					stdDev[b] = Math.sqrt(stdDev[b] / nCells);
				}

				// Max and min
				pb = new ParameterBlock();
				pb.addSource(bImage);

				RenderedOp op = JAI.create("extrema", pb);
				double[][] extrema = (double[][]) op.getProperty("extrema");

				stats = new Stats(nBands);
				for (int b = 0; b < nBands; b++) {
					stats.setStatsForBand(b, extrema[0][b], extrema[1][b],
							mean[b], stdDev[b]);
				}

			}
			 Envelope envelope = getGeoReferencing(fileNameOrURL, true,
			 	new Point(bImage.getWidth(), bImage.getHeight()));
			double cellSize = (envelope.getMaxX() - envelope.getMinX())
					/ bImage.getWidth();
			return new ImageAndMetadata(bImage, new Metadata(envelope,
					envelope, new Point(bImage.getWidth(), bImage.getHeight()),
					new Point(bImage.getWidth(), bImage.getHeight()), cellSize,
					cellSize, Double.NaN, stats));

    } else if (fileNameOrURL.toLowerCase().endsWith(".tif") || fileNameOrURL.toLowerCase().endsWith(".tiff")) {

      File tiffFile = new File(fileNameOrURL);

//			TiffMetadata tiffMetadata = TiffTags.readMetadata(new File(
//					fileNameOrURL));
//
//			int imgWidth = tiffMetadata.getColsCount();
//			int imgHeight = tiffMetadata.getRowsCount();
//
//			Envelope imageEnvelope = tiffMetadata.getEnvelope();
      // Envelope imageEnvelope = getGeoReferencing(fileNameOrURL, true,
      // new Point(imgWidth, imgHeight));

//			Overviews overviews = OverviewsUtils.getOverviews(new File(
//					fileNameOrURL), imageEnvelope);

    //  Envelope imageEnvelope = TiffUtilsV2.getEnvelope(tiffFile);
     // Overviews overviews = OverviewsUtils.getOverviews(new File(fileNameOrURL), imageEnvelope);

    //  return TiffUtilsV2.readImage(tiffFile, viewPortEnvelope, requestedRes, overviews, stats);
      
      // [Giuseppe Aruta 2020-sept-22] added new method that remove overviews and gets metadata
      // directly from GeoRasterImage or using JAI
      return TiffUtilsV2.readImageAndMetadata(tiffFile, viewPortEnvelope, requestedRes, stats);
    } else if (fileNameOrURL.toLowerCase().endsWith(".flt")) {

			GridFloat gf = new GridFloat(fileNameOrURL);
			gf.readGrid(null);

			Envelope imageEnvelope = new Envelope(gf.getXllCorner(),
					gf.getXllCorner() + gf.getnCols() * gf.getCellSize(),
					gf.getYllCorner(), gf.getYllCorner() + gf.getnRows()
							* gf.getCellSize());

			stats = new Stats(1);
			stats.setStatsForBand(0, gf.getMinVal(), gf.getMaxVal(),
					gf.getMeanVal(), gf.getStDevVal());

			return new ImageAndMetadata(gf.getBufferedImage(), new Metadata(
					imageEnvelope, imageEnvelope, new Point(gf.getnCols(),
							gf.getnRows()), new Point(gf.getnCols(),
							gf.getnRows()), gf.getCellSize(), gf.getCellSize(),
					gf.getNoData(), stats));

		} else if (fileNameOrURL.toLowerCase().endsWith(".asc")
				|| fileNameOrURL.toLowerCase().endsWith(".txt")) {

			GridAscii ga = new GridAscii(fileNameOrURL);
			ga.readGrid(null);

			Envelope imageEnvelope = new Envelope(ga.getXllCorner(),
					ga.getXllCorner() + ga.getnCols() * ga.getCellSize(),
					ga.getYllCorner(), ga.getYllCorner() + ga.getnRows()
							* ga.getCellSize());

			BufferedImage pImage = ga.getBufferedImage();

			stats = new Stats(1);
			stats.setStatsForBand(0, ga.getMinVal(), ga.getMaxVal(),
					ga.getMeanVal(), ga.getStDevVal());

			return new ImageAndMetadata(pImage, new Metadata(imageEnvelope,
					imageEnvelope, new Point(ga.getnCols(), ga.getnRows()),
					new Point(ga.getnCols(), ga.getnRows()), ga.getCellSize(),
					ga.getCellSize(), ga.getNoData(), stats));

		}
		// logger.printError("unsupported image format");
		return null;
	}

	public static Raster loadRasterData(String filenameOrURL, Rectangle subset)
			throws IOException {

		if (filenameOrURL.toLowerCase().endsWith(".gif")
				|| filenameOrURL.toLowerCase().endsWith(".png")) {
			RenderedOp renderedOp = JAI.create("fileload", filenameOrURL);
			return renderedOp.getAsBufferedImage(subset, null).getData();
		}
		else if (filenameOrURL.toLowerCase().endsWith(".tif")
				|| filenameOrURL.toLowerCase().endsWith(".tiff")) {

        return TiffUtilsV2.getRenderedOp(new File(filenameOrURL)).getAsBufferedImage(subset, null).getData();

		}  else if (filenameOrURL.toLowerCase().endsWith(".jpg")
				|| filenameOrURL.toLowerCase().endsWith(".bmp")
				|| filenameOrURL.toLowerCase().endsWith(".jp2")) {

			BufferedImage bImage;
			try {
				// Try with ImageIO
				bImage = ImageIO.read(new File(filenameOrURL));
			} catch (Exception ex) {
				// Try with JAI
				bImage = JAI.create("fileload", filenameOrURL)
						.getAsBufferedImage();
			}
			if (subset != null) {

				BufferedImage clipping = new BufferedImage(subset.width,
						subset.height, bImage.getType());
				Graphics2D area = (Graphics2D) clipping.getGraphics().create();
				area.drawImage(bImage, 0, 0, clipping.getWidth(),
						clipping.getHeight(), subset.x, subset.y, subset.x
								+ subset.width, subset.y + subset.height, null);
				area.dispose();

				return clipping.getData();
				// Raster raster = bImage.getData(subset);
				// WritableRaster wRaster =
				// raster.createCompatibleWritableRaster(subset);
				// wRaster.setRect(subset.x, subset.y, raster);
				// return wRaster;
			} else {
				return bImage.getData();
			}
			// PlanarImage pimage;
			// BufferedImage image = ImageIO.read(new File(filenameOrURL));
			// pimage = PlanarImage.wrapRenderedImage(image);
			// return pimage.copyData();

		} else if (filenameOrURL.toLowerCase().endsWith(".flt")) {

			GridFloat gf = new GridFloat(filenameOrURL);
			gf.readGrid(subset);

			DataBuffer dataBuffer = new DataBufferFloat(gf.getFloatArray(),
					gf.getFloatArray().length);

			int nCols = gf.getnCols();
			int nRows = gf.getnRows();
			if (subset != null) {
				nCols = subset.width;
				nRows = subset.height;
			}

			return Raster.createWritableRaster(RasterFactory
					.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols,
							nRows, 1), dataBuffer, new java.awt.Point(0, 0));

		} else if (filenameOrURL.toLowerCase().endsWith(".asc")
				|| filenameOrURL.toLowerCase().endsWith(".txt")) {

			GridAscii ga = new GridAscii(filenameOrURL);
			ga.readGrid(subset);

			int nCols = ga.getnCols();
			int nRows = ga.getnRows();
			if (subset != null) {
				nCols = subset.width;
				nRows = subset.height;
			}

			DataBuffer dataBuffer = new DataBufferFloat(ga.getFloatArray(),
					ga.getFloatArray().length);

			return Raster.createWritableRaster(RasterFactory
					.createBandedSampleModel(DataBuffer.TYPE_FLOAT, nCols,
							nRows, 1), dataBuffer, new java.awt.Point(0, 0));

		}
		return null;
	}

	public static Double readCellValue(String fileNameOrURL,
			Coordinate coordinate, int band) throws Exception {

		Point imageDims = getImageDimensions(fileNameOrURL);
		Envelope envelope = getGeoReferencing(fileNameOrURL,
				true, new Point(imageDims.x, imageDims.y));
		double cellSizeX = (envelope.getMaxX() - envelope.getMinX())
				/ imageDims.x;
		double cellSizeY = (envelope.getMaxY() - envelope.getMinY())
				/ imageDims.y;

		Point colRow = fromCoordinateToCell(coordinate,
				new Coordinate(envelope.getMinX(), envelope.getMinY()),
				imageDims.y, cellSizeX, cellSizeY);

		return readCellValue(fileNameOrURL, colRow.x, colRow.y, band);

	}

	public static Double readCellValue(String filenameOrURL, int col, int row,
			int band) throws IOException {
		RenderedOp  renderedOp;
		Rectangle rectangle = new Rectangle(col, row, 1, 1);
		if (filenameOrURL.toLowerCase().endsWith(".gif")
				|| filenameOrURL.toLowerCase().endsWith(".png")
				) {

			 renderedOp = javax.media.jai.JAI.create("fileload",
					filenameOrURL);
		 	return renderedOp.getData(rectangle)
					.getSampleDouble(col, row, band);
		}
		else if (filenameOrURL.toLowerCase().endsWith(".tif")
				|| filenameOrURL.toLowerCase().endsWith(".tiff")) {
			renderedOp=	TiffUtilsV2.getRenderedOp(new File(filenameOrURL));
			
			return renderedOp.getData(rectangle)
					.getSampleDouble(col, row, band);	
		

		} else if (filenameOrURL.toLowerCase().endsWith(".jpg")) {
			// PlanarImage pimage;

			 renderedOp = javax.media.jai.JAI.create("fileload",
					filenameOrURL);
			 

			return renderedOp.getData(rectangle)
					.getSampleDouble(col, row, band);

			// BufferedImage image = ImageIO.read(new File(filenameOrURL));
			// pimage = PlanarImage.wrapRenderedImage(image);
			// return pimage.copyData().getSampleDouble(col, row, 0); //copy
			// data so we do not get a ref
		} else if (filenameOrURL.toLowerCase().endsWith(".bmp")) {
			BufferedImage image = ImageIO.read(new File(filenameOrURL));
			PlanarImage pimage = PlanarImage.wrapRenderedImage(image);
			return pimage.copyData().getSampleDouble(col, row, 0);

		} else if (filenameOrURL.toLowerCase().endsWith(".jp2")) {
			BufferedImage image = ImageIO.read(new File(filenameOrURL));
			PlanarImage pimage = PlanarImage.wrapRenderedImage(image);
			return pimage.copyData().getSampleDouble(col, row, 0);

		} else if (filenameOrURL.toLowerCase().endsWith(".flt")) {

			GridFloat gf = new GridFloat(filenameOrURL);
			return gf.readCellVal(col, row);

		} else if (filenameOrURL.toLowerCase().endsWith(".asc")
				|| filenameOrURL.toLowerCase().endsWith(".txt")) {

			GridAscii ga = new GridAscii(filenameOrURL);
			return ga.readCellValue(col, row);

		}
		return null;

	}

	public static Point getImageDimensions(String filenameOrURL)
			throws IOException {
		if (filenameOrURL.toLowerCase().endsWith(".gif")
				|| filenameOrURL.toLowerCase().endsWith(".png")) {
			javax.media.jai.PlanarImage pImage = javax.media.jai.JAI.create(
					"fileload", filenameOrURL);
			if (pImage != null) {
				return new Point(pImage.getWidth(), pImage.getHeight());
			}
			
		} else if (filenameOrURL.toLowerCase().endsWith(".jpg") ||
				filenameOrURL.toLowerCase().endsWith(".jpeg")) {
			try {
				Dimension dimension = org.apache.commons.imaging.Imaging.getImageSize(new File(filenameOrURL));
				return new Point((int)dimension.getWidth(), (int)dimension.getHeight());
			} catch(ImageReadException e) {
				throw new IOException(e);
			}
		} else if (filenameOrURL.toLowerCase().endsWith(".tif")
				|| filenameOrURL.toLowerCase().endsWith(".tiff")) {
			RenderedOp  renderedOp;
			renderedOp=	TiffUtilsV2.getRenderedOp(new File(filenameOrURL));
		 
			if (renderedOp != null) {
				return new Point(renderedOp.getWidth(), renderedOp.getHeight());
			}

		} else if (filenameOrURL.toLowerCase().endsWith(".flt")) {

			GridFloat gf = new GridFloat(filenameOrURL);
			return new Point(gf.getnCols(), gf.getnRows());

		} else if (filenameOrURL.toLowerCase().endsWith(".asc")
				|| filenameOrURL.toLowerCase().endsWith(".txt")) {

			GridAscii ga = new GridAscii(filenameOrURL);
			return new Point(ga.getnCols(), ga.getnRows());

		} else {
			System.out.println("Filename " + filenameOrURL);
			BufferedImage image = ImageIO.read(new File(filenameOrURL));
			return new Point(image.getWidth(), image.getHeight());

		}

		return null;
	}

	
	
	/**
	 * Gets Envelope from the file. Check first into aux.xml and world file
	 * @param fileName file name
	 * @param alwaysLookForTFWExtension whether the method should read georeferencing
	 *                                  in an associated world file (tfw)
	 * @param imageDimensions
	 * @return Envelope envelope of the image in model coordinates
	 * @throws Exception if an Exception occurs
	 */
	public static Envelope getGeoReferencing(String fileName,
			boolean alwaysLookForTFWExtension, Point imageDimensions) throws Exception {
		Envelope env = null;
		WorldFileHandler worldFileHandler = new WorldFileHandler(fileName,
				alwaysLookForTFWExtension);
		if (imageDimensions == null) {
			throw new Exception(
					I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
		}
		//First check file.aux.xml
		String auxFile = fileName+".aux.xml";
		if ((new File(auxFile)).exists()) {
			env = worldFileHandler.auxFileEnvelope(auxFile, imageDimensions.x,imageDimensions.y);
		}
		//then check world file
		if (env==null & worldFileHandler.isWorldFileExistentForImage() != null) {
			env = worldFileHandler.readWorldFile(imageDimensions.x, imageDimensions.y);
		}
		if (env == null) {
			boolean isGeoTiff = false;
			if (fileName.toLowerCase().endsWith(".tif") || fileName.toLowerCase().endsWith(".tiff")) {
				isGeoTiff = true;
				env=TiffUtilsV2.getEnvelope(new File(fileName));
			} else if (fileName.toLowerCase().endsWith(".flt")) {
				isGeoTiff = true;
				GridFloat gf = new GridFloat(fileName);
				env =gf.getEnvelope();

			} else if (fileName.toLowerCase().endsWith(".asc")
					|| fileName.toLowerCase().endsWith(".txt")) {
				isGeoTiff = true;
				GridAscii ga = new GridAscii(fileName);
				env = ga.getEnvelope();
			}
			if (!isGeoTiff || env == null) {
				Logger.warn(I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
				return null;
			}
		}
		return env;
	} 
	
	 
	
	/**
	 * getGeoReferencing(String fileName) method since 04.21.2023
	 * @param fileName file name
	 * @param alwaysLookForTFWExtension whether the method should read georeferencing
	 *                                  in an associated world file (tfw)
	 * @param imageDimensions
	 * @return Envelope envelope of the image in model coordinates
	 * @throws Exception if an Exception occurs
	 */
	public static Envelope getGeoReferencing_old(String fileName,
			boolean alwaysLookForTFWExtension, Point imageDimensions) throws Exception {

		Envelope env = null;

		WorldFileHandler worldFileHandler = new WorldFileHandler(fileName,
				alwaysLookForTFWExtension);

		if (imageDimensions == null) {
			throw new Exception(
					I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
		}

		if (worldFileHandler.isWorldFileExistentForImage() != null) {
			env = worldFileHandler.readWorldFile(imageDimensions.x,
					imageDimensions.y);
		}

		if (env == null) {

			boolean isGeoTiff = false;

			if (fileName.toLowerCase().endsWith(".tif")
					|| fileName.toLowerCase().endsWith(".tiff")) {

				Coordinate tiePoint = null, pixelOffset = null, pixelScale = null;
				double[] doubles;

				FileSeekableStream fileSeekableStream = new FileSeekableStream(
						fileName);
				TIFFDirectory tiffDirectory = new TIFFDirectory(
						fileSeekableStream, 0);

				TIFFField[] availTags = tiffDirectory.getFields();

				for (TIFFField availTag : availTags) {
					if (availTag.getTag() == GeoTiffConstants.ModelTiepointTag) {
						doubles = availTag.getAsDoubles();
						if (doubles.length != 6) {
							// logger.printError("unsupported value for ModelTiepointTag ("
							// + GeoTiffConstants.ModelTiepointTag + ")");
							// context.getWorkbench().getFrame().warnUser("unsupported value for ModelTiepointTag ("
							// + GeoTiffConstants.ModelTiepointTag + ")");
							// break;
							throw new Exception(
									"unsupported value for ModelTiepointTag ("
											+ GeoTiffConstants.ModelTiepointTag
											+ ")");
						}
						if (doubles[0] != 0 || doubles[1] != 0
								|| doubles[2] != 0) {
							if (doubles[2] == 0)
								pixelOffset = new Coordinate(doubles[0],
										doubles[1]);
							else
								pixelOffset = new Coordinate(doubles[0],
										doubles[1], doubles[2]);
						}
						if (doubles[5] == 0)
							tiePoint = new Coordinate(doubles[3], doubles[4]);
						else
							tiePoint = new Coordinate(doubles[3], doubles[4],
									doubles[5]);

						// logger.printDebug("ModelTiepointTag (po): " +
						// pixelOffset);
						// logger.printDebug("ModelTiepointTag (tp): " +
						// tiePoint);
					} else if (availTag.getTag() == GeoTiffConstants.ModelPixelScaleTag) {
						// Karteneinheiten pro pixel x bzw. y
						doubles = availTag.getAsDoubles();
						if (doubles[2] == 0)
							pixelScale = new Coordinate(doubles[0], doubles[1]);
						else
							pixelScale = new Coordinate(doubles[0], doubles[1],
									doubles[2]);

						// logger.printDebug("ModelPixelScaleTag (ps): " +
						// pixelScale);
					} else {
						// logger.printDebug("tiff field: " +
						// availTags[i].getType() + ", "+ availTags[i].getTag()
						// + ", "+ availTags[i].getCount());
					}
				}

				fileSeekableStream.close();

				if (tiePoint != null && pixelScale != null) {
					isGeoTiff = true;
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

					// logger.printDebug("upperLeft: " + upperLeft);
					// logger.printDebug("lowerRight: " + lowerRight);

					env = new Envelope(upperLeft, lowerRight);
				}

			} else if (fileName.toLowerCase().endsWith(".flt")) {
				isGeoTiff = true;
				GridFloat gf = new GridFloat(fileName);

				Coordinate upperLeft = new Coordinate(gf.getXllCorner(),
						gf.getYllCorner() + gf.getnRows() * gf.getCellSize());
				Coordinate lowerRight = new Coordinate(gf.getXllCorner()
						+ gf.getnCols() * gf.getCellSize(), gf.getYllCorner());

				env = new Envelope(upperLeft, lowerRight);

			} else if (fileName.toLowerCase().endsWith(".asc")
					|| fileName.toLowerCase().endsWith(".txt")) {
				isGeoTiff = true;
				GridAscii ga = new GridAscii(fileName);

				Coordinate upperLeft = new Coordinate(ga.getXllCorner(),
						ga.getYllCorner() + ga.getnRows() * ga.getCellSize());
				Coordinate lowerRight = new Coordinate(ga.getXllCorner()
						+ ga.getnCols() * ga.getCellSize(), ga.getYllCorner());

				env = new Envelope(upperLeft, lowerRight);
			}

			if (!isGeoTiff || env == null) {
				// logger.printDebug(PirolPlugInMessages.getString("no-worldfile-found"));
				// context.getWorkbench().getFrame().warnUser(I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
				throw new Exception(
						I18N.getInstance().get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
				// WizardDialog d = new WizardDialog(
				// context.getWorkbench().getFrame(),
				// I18N.getInstance().get("RasterImagePlugIn.34")
				// + worldFileHandler.getWorldFileName()
				// + I18N.getInstance().get("RasterImagePlugIn.35"),
				// context.getErrorHandler());
				// d.init(new WizardPanel[] { new RasterImageWizardPanel() });
				// //Set size after #init, because #init calls #pack. [Jon
				// Aquino]
				// d.setSize(500, 400);
				// GUIUtil.centreOnWindow(d);
				// d.setVisible(true);
				//
				// if (!d.wasFinishPressed()) {
				// //logger.printWarning("user canceled");
				// return null;
				// }
				// try {
				// minx = Double.parseDouble((String) d
				// .getData(RasterImageWizardPanel.MINX_KEY));
				// maxx = Double.parseDouble((String) d
				// .getData(RasterImageWizardPanel.MAXX_KEY));
				// miny = Double.parseDouble((String) d
				// .getData(RasterImageWizardPanel.MINY_KEY));
				// maxy = Double.parseDouble((String) d
				// .getData(RasterImageWizardPanel.MAXY_KEY));
				//
				// env = new Envelope(minx, maxx, miny, maxy);
				// }
				// catch(java.lang.NumberFormatException e) {
				// Viewport viewport =
				// context.getLayerViewPanel().getViewport();
				// Rectangle visibleRect = viewport.getPanel().getVisibleRect();
				//
				// int visibleX1 = visibleRect.x;
				// int visibleY1 = visibleRect.y;
				// int visibleX2 = visibleX1 + visibleRect.width;
				// int visibleY2 = visibleY1 + visibleRect.height;
				// Coordinate upperLeftVisible = viewport.toModelCoordinate(new
				// Point(0,0));
				// Coordinate lowerRightVisible = viewport.toModelCoordinate(new
				// Point(visibleX2, visibleY2));
				// env = new Envelope(upperLeftVisible.x, lowerRightVisible.x,
				// upperLeftVisible.y, lowerRightVisible.y);
				// }
				//
				// }
				//
				// // creating world file
				// worldFileHandler = new WorldFileHandler(fileName,
				// allwaysLookForTFWExtension);
				// worldFileHandler.writeWorldFile(env, imageDimensions.x,
				// imageDimensions.y);
			}
		}
		return env;
	} 

	public static CellSizeXY getCellSize(String fileNameOrURL) throws Exception {

		Point imageDims = getImageDimensions(fileNameOrURL);

	 
	 Envelope envelope = getGeoReferencing(fileNameOrURL, true, new Point(
 				imageDims.x, imageDims.y));
		double cellSizeX = (envelope.getMaxX() - envelope.getMinX())
				/ imageDims.x;
		double cellSizeY = (envelope.getMaxY() - envelope.getMinY())
				/ imageDims.y;

		return new RasterImageIO().new CellSizeXY(cellSizeX, cellSizeY);

	}

	public static Double getNoData(String fileNameOrURL) throws IOException,
			ImageReadException {

		if (fileNameOrURL.toLowerCase().endsWith(".asc")
				|| fileNameOrURL.toLowerCase().endsWith(".txt")) {

			GridAscii gridAscii = new GridAscii(fileNameOrURL);
			gridAscii.readHeader();
			return gridAscii.getNoData();

		} else if (fileNameOrURL.toLowerCase().endsWith(".flt")) {

			GridFloat gf = new GridFloat(fileNameOrURL);
			return gf.getNoData();

		} else if (fileNameOrURL.toLowerCase().endsWith(".tif")) {

			TiffField field = TiffTags
					.readField(new File(fileNameOrURL), 42113);
			if (field.getFieldType() == FieldType.DOUBLE) {
				return field.getDoubleValue();
			} else if (field.getFieldType() == FieldType.FLOAT) {
				return field.getDoubleValue();
			} else if (field.getFieldType() == FieldType.ASCII) {
				return Double.parseDouble(field.getStringValue());
			}

		}
		return null;

	}

	public static Envelope getViewingEnvelope(Viewport viewport)
			throws NoninvertibleTransformException {

		Rectangle visible = viewport.getPanel().getVisibleRect();
		int visibleX1 = visible.x;
		int visibleY1 = visible.y;
		int visibleX2 = visibleX1 + visible.width;
		int visibleY2 = visibleY1 + visible.height;

		Coordinate upperLeftVisible = viewport.toModelCoordinate(new Point(
				visibleX1, visibleY1));
		Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(
				visibleX2, visibleY2));

		return new Envelope(upperLeftVisible, lowerRightVisible);

	}

	public static Rectangle getDrawingRectangle(int imgWidth, int imgHeight,
			Envelope wholeImageEnvelope, Envelope viewportEnvelope,
			Resolution subsetResolution) throws NoninvertibleTransformException {
		Rectangle rect = null;
		if (viewportEnvelope == null
				|| viewportEnvelope.contains(wholeImageEnvelope)) {
			rect = new Rectangle(0, 0, imgWidth, imgHeight);
		} else if (viewportEnvelope.intersects(wholeImageEnvelope)) {

			Coordinate upperLeftVisible = new Coordinate(
					viewportEnvelope.getMinX(), viewportEnvelope.getMaxY());
			Coordinate lowerRightVisible = new Coordinate(
					viewportEnvelope.getMaxX(), viewportEnvelope.getMinY());

			java.awt.Point upperLeft = fromCoordinateToCell(upperLeftVisible,
					new Coordinate(wholeImageEnvelope.getMinX(),
							wholeImageEnvelope.getMinY()), imgHeight,
					subsetResolution.getX(), subsetResolution.getY());
			java.awt.Point lowerRight = fromCoordinateToCell(lowerRightVisible,
					new Coordinate(wholeImageEnvelope.getMinX(),
							wholeImageEnvelope.getMinY()), imgHeight,
					subsetResolution.getX(), subsetResolution.getY());

			int xOffset = Math.max(0, upperLeft.x);
			int yOffset = Math.max(0, upperLeft.y);

			int width = lowerRight.x - upperLeft.x;
			int height = lowerRight.y - upperLeft.y;

			rect = new Rectangle(xOffset, yOffset, width, height);
		}
		return rect;
	}

	protected Rectangle getVisibleImageCoordinatesOfImage(double imgWidth,
			double imgHeight, Envelope visible, Envelope imageEnv) {

		double minVisibleX = Math.max(visible.getMinX(), imageEnv.getMinX());
		double minVisibleY = Math.max(visible.getMinY(), imageEnv.getMinY());

		double maxVisibleX = Math.min(visible.getMaxX(), imageEnv.getMaxX());
		double maxVisibleY = Math.min(visible.getMaxY(), imageEnv.getMaxY());

		double offset2VisibleX = imageEnv.getMinX() - visible.getMinX();
		double offset2VisibleY = visible.getMaxY() - imageEnv.getMaxY();

		double scaleX = imgWidth / imageEnv.getWidth();
		double scaleY = imgHeight / imageEnv.getHeight();

		int xOffset;
		int yOffset;

		if (offset2VisibleX >= 0) {
			xOffset = 0;
		} else {
			xOffset = (int) (-offset2VisibleX * scaleX);
		}

		if (offset2VisibleY >= 0) {
			yOffset = 0;
		} else {
			yOffset = (int) (-offset2VisibleY * scaleY);
		}

		int width = (int) ((maxVisibleX - minVisibleX) * scaleX);
		int height = (int) ((maxVisibleY - minVisibleY) * scaleY);

		if (width < imgWidth && height < imgHeight) {
			width += 1;
			height += 1;
		}

		if (width <= 0 || height <= 0) {
			return null;
		}

		return new Rectangle(xOffset, yOffset, width, height);
	}

	public static java.awt.Point fromCoordinateToCell(Coordinate coord,
			Coordinate lowerLeftCoord, int rowCount, double cellSizeX,
			double cellSizeY) {

		java.awt.Point point = new java.awt.Point();
		point.x = (int) Math.floor((coord.x - lowerLeftCoord.x) / cellSizeX);
		point.y = rowCount
				- (int) Math.floor((coord.y - lowerLeftCoord.y) / cellSizeY)
				- 1;

		return point;

	}

	public static Coordinate fromCellToCoordinate(java.awt.Point cell,
			Coordinate lowerLeftCoord, double cellSize, int rowCount) {

		Coordinate coord = new Coordinate();
		coord.x = lowerLeftCoord.x + cell.x * cellSize + 0.5 * cellSize;
		coord.y = lowerLeftCoord.y + (rowCount - cell.y) * cellSize - 0.5
				* cellSize;
		return coord;

	}

	public void writeImage(File outFile, Raster raster, Envelope envelope,
			CellSizeXY cellSize, double noData) throws IOException {

		// Delete old .xml.aux statistics file
		File auxXmlFile = new File(outFile.getParent(), outFile.getName()
				+ ".aux.xml");
		if (auxXmlFile.exists() && auxXmlFile.canWrite()) {
			try {
				auxXmlFile.delete();
			} catch (Exception ex) {
				ex.printStackTrace(System.out);
			}
		}

		SampleModel sm = raster.getSampleModel();
		ColorModel colorModel = PlanarImage.createColorModel(sm);
		BufferedImage image = new BufferedImage(colorModel,
				(WritableRaster) raster, false, null);

		TIFFEncodeParam param = new TIFFEncodeParam();
		param.setCompression(TIFFEncodeParam.COMPRESSION_NONE);

		TIFFField[] tiffFields = new TIFFField[3];

		// Cell size
		tiffFields[0] = new TIFFField(GeoTiffConstants.ModelPixelScaleTag,
				TIFFField.TIFF_DOUBLE, 2, new double[] { cellSize.cellSizeX,
						cellSize.cellSizeY });

		// No data
		String noDataS = Double.toString(noData);
		byte[] bytes = noDataS.getBytes();
		tiffFields[1] = new TIFFField(TiffTags.TIFFTAG_GDAL_NODATA,
				TIFFField.TIFF_BYTE, noDataS.length(), bytes);

		// Tie point
		tiffFields[2] = new TIFFField(GeoTiffConstants.ModelTiepointTag,
				TIFFField.TIFF_DOUBLE, 6, new double[] { 0, 0, 0,
						envelope.getMinX(), envelope.getMaxY(), 0 });

		param.setExtraFields(tiffFields);

		FileOutputStream tifOut = new FileOutputStream(outFile);
		TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec
				.createImageEncoder("tiff", tifOut, param);
		encoder.encode(image);
		tifOut.close();

		WorldFileHandler worldFileHandler = new WorldFileHandler(
				outFile.getAbsolutePath(), false);
		worldFileHandler.writeWorldFile(envelope, image.getWidth(),
				image.getHeight());

	}

	/**
	 * Enhanced method to save the TIF with an external TIF.AUX.XML file
	 * which contains statistics of raster and spatial reference system. 
	 * Worldfile is not saved as geographic position  is already stored 
	 * into the TIF file.
	 * Statistics are calculated from raster
	 * @param outFile output file
	 * @param raster a Raster
	 * @param envelope Envelope of the image to write
	 * @param cellSizeX cell width in model coordinates
	 * @param cellSizeY cell height in model coordinates
	 * @param noData value to use for nodata
	 * @param srsInfo SRSInfo associated to this Raster
	 * @throws IOException if an IOException occurs
	 * @throws TransformerConfigurationException if a TransformerConfigurationException occurs
	 * @throws ParserConfigurationException if a ParserConfigurationException occurs
	 * @throws TransformerException if a TransformerException occurs
	 * @throws SAXException if a SAXException occurs
	 */
	
	public void writeImage(File outFile, Raster raster, Envelope envelope,
			double cellSizeX, double cellSizeY, double noData, SRSInfo srsInfo) 
					throws IOException, TransformerConfigurationException, ParserConfigurationException, TransformerException, SAXException   {

		SampleModel sm = raster.getSampleModel();
		ColorModel colorModel = PlanarImage.createColorModel(sm);
		BufferedImage bufferedImage = new BufferedImage(colorModel,
				(WritableRaster) raster, false, null);

		TIFFEncodeParam param = new TIFFEncodeParam();
		param.setCompression(TIFFEncodeParam.COMPRESSION_NONE);

		TIFFField[] tiffFields = new TIFFField[3];

		// Cell size
		tiffFields[0] = new TIFFField(GeoTiffConstants.ModelPixelScaleTag,
				TIFFField.TIFF_DOUBLE, 2, new double[] { cellSizeX,
						cellSizeY});

		// No data
		String noDataS = Double.toString(noData);
		byte[] bytes = noDataS.getBytes();
		tiffFields[1] = new TIFFField(TiffTags.TIFFTAG_GDAL_NODATA,
				TIFFField.TIFF_BYTE, noDataS.length(), bytes);

		// Tie point
		tiffFields[2] = new TIFFField(GeoTiffConstants.ModelTiepointTag,
				TIFFField.TIFF_DOUBLE, 6, new double[] { 0, 0, 0,
						envelope.getMinX(), envelope.getMaxY(), 0 });

		param.setExtraFields(tiffFields);

		FileOutputStream tifOut = new FileOutputStream(outFile);
		TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec
				.createImageEncoder("tiff", tifOut, param);
		encoder.encode(bufferedImage);
		tifOut.close();
		 int bandCount = bufferedImage.getRaster().getNumBands();
		 double minValue[] = new double[bandCount];
		 double maxValue[] = new double[bandCount];
		 double sum[] = new double[bandCount];
		 double sumSquare[] = new double[bandCount];
		 long cellsCount[] = new long[bandCount];
	        
		 for(int b=0; b<bandCount; b++) {
		 	minValue[b] = Double.MAX_VALUE;
		 	maxValue[b] = -Double.MAX_VALUE;
		 }
	        
	        for(int r=0; r<bufferedImage.getHeight(); r++) {
	            Raster raster2 = bufferedImage.getData(new Rectangle(0, r, bufferedImage.getWidth(), 1));
	            for(int c=0; c<bufferedImage.getWidth(); c++) {
	                for(int b=0; b<bandCount; b++) {
	                    double value = raster2.getSampleDouble(c, r, b);
	                    if(value != noData && (float)value != (float)noData &&
	                            !Double.isNaN(value) && !Double.isInfinite(value)) {
	                        if(value < minValue[b]) minValue[b] = value;
	                        if(value > maxValue[b]) maxValue[b] = value;
	                        cellsCount[b]++;
	                        sum[b] += value;
	                        sumSquare[b] += value * value;
	                    }
	                }
	            }
	        }
	        Stats stats = new Stats(bandCount);
	        for(int b=0; b<bandCount; b++) {
	            double meanValue = sum[b] / cellsCount[b];
	            double stdDevValue = Math.sqrt(sumSquare[b] / cellsCount[b] - meanValue * meanValue);
	            stats.setStatsForBand(b, minValue[b], maxValue[b], meanValue, stdDevValue);
	        }
	    		File auxXmlFile = new File(outFile.getParent(), outFile.getName()
								+ ".aux.xml");
				GDALPamDataset gPam = new GDALPamDataset();
		    	gPam.writeStatisticsAndSRS(auxXmlFile, srsInfo, stats);
		}
	
	
	
	public static Resolution calcRequestedResolution(Viewport viewport) {

		double xRes = viewport.getEnvelopeInModelCoordinates().getWidth()
				/ viewport.getPanel().getVisibleRect().width;
		double yRes = viewport.getEnvelopeInModelCoordinates().getHeight()
				/ viewport.getPanel().getVisibleRect().height;

		return new Resolution(xRes, yRes);
	}

	public class CellSizeXY {

		public CellSizeXY(double cellSizeX, double cellSizeY) {
			this.cellSizeX = cellSizeX;
			this.cellSizeY = cellSizeY;
		}

		public double getCellSizeX() {
			return cellSizeX;
		}

		public double getCellSizeY() {
			return cellSizeY;
		}

		public double getAverageCellSize() {
			return (cellSizeX + cellSizeY) * 0.5;
		}

		private final double cellSizeX;
		private final double cellSizeY;

	}

}
