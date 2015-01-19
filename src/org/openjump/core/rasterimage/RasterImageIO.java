package org.openjump.core.rasterimage;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.Viewport;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import org.openjump.core.rasterimage.TiffTags.TiffMetadata;

/**
 *
 * @author deluca
 */
public class RasterImageIO {
    
    public ImageAndMetadata loadImage(WorkbenchContext wbContext, String fileNameOrURL, Stats stats, Envelope viewPortEnvelope, Resolution requestedRes)
            throws IOException, NoninvertibleTransformException, FileNotFoundException, TiffTags.TiffReadingException, Exception {

        if (fileNameOrURL.toLowerCase().endsWith(".jpg")
                || fileNameOrURL.toLowerCase().endsWith(".gif")
                || fileNameOrURL.toLowerCase().endsWith(".png")) {
             
            BufferedImage bImage = javax.media.jai.JAI.create("fileload", fileNameOrURL).getAsBufferedImage();
            
            if(stats == null) {
                stats = Stats.defaultRGBStats();
            }
            
            Envelope envelope = getGeoReferencing(fileNameOrURL, true, new Point(bImage.getWidth(), bImage.getHeight()));
            double cellSize = (envelope.getMaxX() - envelope.getMinX()) / bImage.getWidth();
            return new ImageAndMetadata(bImage,
                    new Metadata(
                            envelope, envelope,
                            new Point(bImage.getWidth(), bImage.getHeight()),
                            new Point(bImage.getWidth(), bImage.getHeight()),
                            cellSize, cellSize, Double.NaN, stats));         
            
        } else if (fileNameOrURL.toLowerCase().endsWith(".tif") || fileNameOrURL.toLowerCase().endsWith(".tiff")) {
            
            // Get nodata value. Uses TIFFTAG_GDAL_NODATA (42113)
//            InputStream input = createInputStream((new File(filenameOrURL)).toURI());
//            SeekableStream ss = SeekableStream.wrapInputStream(input, true);
//            XTIFFDirectory dir = XTIFFDirectory.create(ss, 0);
//            XTIFFField fieldNoData = dir.readField(42113);
//            
//            XTIFFField fieldWidth = dir.readField(XTIFF.TIFFTAG_IMAGE_WIDTH);
//            XTIFFField fieldHeight = dir.readField(XTIFF.TIFFTAG_IMAGE_LENGTH);
//            XTIFFField fieldCellSizeX = dir.readField(XTIFF.TIFFTAG_X_RESOLUTION);
            
//            int width = fieldWidth.getAsInt(0);
//            int height = fieldHeight.getAsInt(0);
//            double xCellSize = 1; // TODO
            
            TiffMetadata tiffMetadata = TiffTags.readMetadata(new File(fileNameOrURL));
             
            int imgWidth = tiffMetadata.getColsCount();
            int imgHeight = tiffMetadata.getRowsCount();
            
            Envelope imageEnvelope = getGeoReferencing(fileNameOrURL, true, new Point (imgWidth, imgHeight));
            
            Overviews overviews = OverviewsUtils.getOverviews(new File(fileNameOrURL), imageEnvelope);
            
            return TiffUtils.readImage(new File(fileNameOrURL), viewPortEnvelope, requestedRes, overviews, stats);

         } else if (fileNameOrURL.toLowerCase().endsWith(".flt")){

            GridFloat gf = new GridFloat(fileNameOrURL);
            gf.readGrid();
            
            Envelope imageEnvelope = new Envelope(
                    gf.getXllCorner(),
                    gf.getXllCorner() + gf.getnCols() * gf.getCellSize(),
                    gf.getYllCorner(),
                    gf.getYllCorner() + gf.getnRows() * gf.getCellSize());
            
            stats = new Stats(1);
            stats.setStatsForBand(0, gf.getMinVal(), gf.getMaxVal(), gf.getMeanVal(), gf.getStDevVal());
            
            return new ImageAndMetadata(
                    gf.getBufferedImage(),
                    new Metadata(imageEnvelope, imageEnvelope,
                            new Point(gf.getnCols(), gf.getnRows()),
                            new Point(gf.getnCols(), gf.getnRows()),
                            gf.getCellSize(), gf.getCellSize(), gf.getNoData(),
                            stats));


         } else if (fileNameOrURL.toLowerCase().endsWith(".asc")){

            GridAscii ga = new GridAscii(fileNameOrURL);
            ga.readGrid();

            Envelope imageEnvelope = new Envelope(
                    ga.getXllCorner(),
                    ga.getXllCorner() + ga.getnCols() * ga.getCellSize(),
                    ga.getYllCorner(),
                    ga.getYllCorner() + ga.getnRows() * ga.getCellSize());
            
            BufferedImage pImage = ga.getBufferedImage();
            
            stats = new Stats(1);
            stats.setStatsForBand(0, ga.getMinVal(), ga.getMaxVal(), ga.getMeanVal(), ga.getStDevVal());
            
            return new ImageAndMetadata(
                    pImage,
                    new Metadata(imageEnvelope, imageEnvelope,
                            new Point(ga.getnCols(), ga.getnRows()),
                            new Point(ga.getnCols(), ga.getnRows()),
                            ga.getCellSize(), ga.getCellSize(), ga.getNoData(),
                            stats));

         }
         //logger.printError("unsupported image format"); 
         return null;
    }
    
    public static Raster loadRasterData(String filenameOrURL, Rectangle subset) throws IOException {

        if (filenameOrURL.toLowerCase().endsWith(".gif")  || filenameOrURL.toLowerCase().endsWith(".png")
                 || filenameOrURL.toLowerCase().endsWith(".tif") || filenameOrURL.toLowerCase().endsWith(".tiff")) {
            
            RenderedOp renderedOp = JAI.create("fileload", filenameOrURL);
            return renderedOp.getAsBufferedImage(subset, null).getData();
            
            //return renderedOp.copyData();
            
//            javax.media.jai.PlanarImage pImage = javax.media.jai.JAI.create("fileload", filenameOrURL);
//            return pImage.copyData();

        } else if (filenameOrURL.toLowerCase().endsWith(".jpg")){
        	 
            return null;
            
//            PlanarImage pimage;
//            BufferedImage image = ImageIO.read(new File(filenameOrURL));
//            pimage = PlanarImage.wrapRenderedImage(image);
//            return pimage.copyData();


        } else if (filenameOrURL.toLowerCase().endsWith(".flt")){

            GridFloat gf = new GridFloat(filenameOrURL);
            gf.readGrid();

            DataBuffer dataBuffer = new DataBufferFloat(gf.getFloatArray(), gf.getnCols()*gf.getnRows());

            return Raster.createWritableRaster(RasterFactory.createBandedSampleModel(
                    DataBuffer.TYPE_FLOAT, gf.getnCols(), gf.getnRows(), 1), dataBuffer, new java.awt.Point(0,0));


         } else if (filenameOrURL.toLowerCase().endsWith(".asc")){

            GridAscii ga = new GridAscii(filenameOrURL);
            ga.readGrid();

            DataBuffer dataBuffer = new DataBufferFloat(ga.getFloatArray(), ga.getnCols()*ga.getnRows());
            
            return Raster.createWritableRaster(RasterFactory.createBandedSampleModel(
                    DataBuffer.TYPE_FLOAT, ga.getnCols(), ga.getnRows(), 1), dataBuffer, new java.awt.Point(0,0));


         }
         return null;
     }
    
    public static Double readCellValue(String fileNameOrURL, Coordinate coordinate, int band) throws NoninvertibleTransformException, Exception {
        
        Point imageDims = getImageDimensions(fileNameOrURL);
        
        Envelope envelope = getGeoReferencing(fileNameOrURL, true, new Point(imageDims.x, imageDims.y));
        double cellSizeX = (envelope.getMaxX() - envelope.getMinX()) / imageDims.x;
        double cellSizeY = (envelope.getMaxY() - envelope.getMinY()) / imageDims.y;
        
        Point colRow = fromCoordinateToCell(coordinate, new Coordinate(envelope.getMinX(), envelope.getMinY()), imageDims.y, cellSizeX, cellSizeY);
        
        return readCellValue(fileNameOrURL, colRow.x, colRow.y, band);
        
    }
    
    public static Double readCellValue(String filenameOrURL, int col, int row, int band) throws IOException {
        
         if (filenameOrURL.toLowerCase().endsWith(".gif")  || filenameOrURL.toLowerCase().endsWith(".png")
                 || filenameOrURL.toLowerCase().endsWith(".tif") || filenameOrURL.toLowerCase().endsWith(".tiff")) {

                RenderedOp renderedOp = javax.media.jai.JAI.create("fileload", filenameOrURL);
                Rectangle rectangle = new Rectangle(col, row, 1, 1);
                
                return renderedOp.getData(rectangle).getSampleDouble(col, row, band);
                
                //return pImage.copyData().getSampleDouble(col, row, 0); //copy data so we do not get a ref

                
         } else if (filenameOrURL.toLowerCase().endsWith(".jpg")){
        	 //PlanarImage pimage;

            RenderedOp renderedOp = javax.media.jai.JAI.create("fileload", filenameOrURL);
            Rectangle rectangle = new Rectangle(col, row, 1, 1);
                
            return renderedOp.getData(rectangle).getSampleDouble(col, row, 0);
                 
//            BufferedImage image = ImageIO.read(new File(filenameOrURL));
//            pimage = PlanarImage.wrapRenderedImage(image);
//            return pimage.copyData().getSampleDouble(col, row, 0);  //copy data so we do not get a ref


         } else if (filenameOrURL.toLowerCase().endsWith(".flt")){

            GridFloat gf = new GridFloat(filenameOrURL);
            return gf.readCellVal(col, row);

         } else if (filenameOrURL.toLowerCase().endsWith(".asc")){

            GridAscii ga = new GridAscii(filenameOrURL);
            return ga.readCellValue(col, row);

         }
         return null;        
        
        
    }
    
    static protected InputStream createInputStream(URI uri) throws IOException {
        Object in = createInput(uri);
        if (in instanceof String)
            in = new File((String) in);
        if (in instanceof File)
            in = new FileInputStream((File) in);
        return (InputStream) in;
    }
    
    static protected Object createInput(URI uri) throws IOException {
        return createInput(uri, null);
    }
    
    static protected Object createInput(URI uri, Object loader) throws IOException {

        Object input;
        if (CompressedFile.isArchive(uri) || CompressedFile.isCompressed(uri)) {
            InputStream src_is;
            src_is = CompressedFile.openFile(uri);
            src_is = new BufferedInputStream(src_is);
            input = src_is;
        } else {
            input = new File(uri);
        }

        if (loader == null) {
            return input;
        }

        if (loader instanceof ImageReaderSpi) {
            // how may i serve you today?
            Class[] clazzes = ((ImageReaderSpi) loader).getInputTypes();
            List<Class> intypes = clazzes != null ? Arrays.asList(clazzes) : new ArrayList();
            //System.out.println("GR in types: " + intypes);
            for (Class clazz : intypes) {
                // already reader compliant? off you f***
                if (clazz.isInstance(input))
                    return input;
                // want an ImageInputStream? try to build one..
                if (ImageInputStream.class.equals(clazz)) {
                    // this returns null if it can't build one from given input
                    ImageInputStream iis = ImageIO.createImageInputStream(input);
                    if (iis != null) { 
                        return iis;
                    }
                }
            }

            throw new IOException("Couldn't create an input for '" + uri
                + "' accepted by reader '" + loader + "'");
        }

        return input;
    }
    
    public static Point getImageDimensions(String filenameOrURL) throws IOException {
        
        if (!filenameOrURL.toLowerCase().endsWith(".jpg") && !filenameOrURL.toLowerCase().endsWith(".flt") &&
                !filenameOrURL.toLowerCase().endsWith(".asc")){

            javax.media.jai.PlanarImage pImage = javax.media.jai.JAI.create("fileload", filenameOrURL);
            if (pImage != null) {
                return new Point(pImage.getWidth(), pImage.getHeight());
            }

        }else if(filenameOrURL.toLowerCase().endsWith(".flt")){

            GridFloat gf = new GridFloat(filenameOrURL);
            return new Point(gf.getnCols(), gf.getnRows());


        }else if(filenameOrURL.toLowerCase().endsWith(".asc")){

            GridAscii ga = new GridAscii(filenameOrURL);
            return new Point(ga.getnCols(), ga.getnRows());

        } else {
            
            BufferedImage image = image = ImageIO.read(new File(filenameOrURL));            
            return new Point(image.getWidth(), image.getHeight());
            
        }
        
        return null;
    }    
    
    public static Envelope getGeoReferencing(String fileName, boolean allwaysLookForTFWExtension,
            Point imageDimensions) throws IOException, NoninvertibleTransformException, Exception{
    
        double minx, maxx, miny, maxy;
        Envelope env = null;
      
        WorldFileHandler worldFileHandler = new WorldFileHandler(fileName, allwaysLookForTFWExtension);
      
        if (imageDimensions == null){
            //logger.printError("can not determine image dimensions");
            //context.getWorkbench().getFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
//            return null;
            throw new Exception(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.can-not-determine-image-dimensions"));
        }
      
        if (worldFileHandler.isWorldFileExistentForImage()!=null) {
            //logger.printDebug(PirolPlugInMessages.getString("worldfile-found"));
            env = worldFileHandler.readWorldFile(imageDimensions.x, imageDimensions.y);
        }
          
        if (env == null) {

            boolean isGeoTiff = false; 

            if ( fileName.toLowerCase().endsWith(".tif") || fileName.toLowerCase().endsWith(".tiff") ) {
                //logger.printDebug("checking for GeoTIFF");

                Coordinate tiePoint = null, pixelOffset = null, pixelScale = null;
                double[] doubles = null;

                FileSeekableStream fileSeekableStream = new FileSeekableStream(fileName);
                TIFFDirectory tiffDirectory = new TIFFDirectory(fileSeekableStream, 0);

                TIFFField[] availTags = tiffDirectory.getFields();
              
                for (int i=0; i<availTags.length; i++){
                    if (availTags[i].getTag() == GeoTiffConstants.ModelTiepointTag){
                        doubles = availTags[i].getAsDoubles();

                        if (doubles.length != 6){
                            //logger.printError("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
                            //context.getWorkbench().getFrame().warnUser("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
                            //break;
                            throw new Exception("unsupported value for ModelTiepointTag (" + GeoTiffConstants.ModelTiepointTag + ")");
                        }

                        if (doubles[0]!=0 || doubles[1]!=0 || doubles[2]!=0){
                            if (doubles[2]==0)
                                pixelOffset = new Coordinate(doubles[0],doubles[1]);
                            else
                                pixelOffset = new Coordinate(doubles[0],doubles[1],doubles[2]);
                        }

                        if (doubles[5]==0)
                            tiePoint = new Coordinate(doubles[3],doubles[4]);
                        else
                            tiePoint = new Coordinate(doubles[3],doubles[4],doubles[5]);

                        //logger.printDebug("ModelTiepointTag (po): " + pixelOffset);
                        //logger.printDebug("ModelTiepointTag (tp): " + tiePoint);
                    } else if (availTags[i].getTag() == GeoTiffConstants.ModelPixelScaleTag){
                        // Karteneinheiten pro pixel x bzw. y

                        doubles = availTags[i].getAsDoubles();

                        if (doubles[2]==0)
                            pixelScale = new Coordinate(doubles[0],doubles[1]);
                        else
                            pixelScale = new Coordinate(doubles[0],doubles[1],doubles[2]);

                        //logger.printDebug("ModelPixelScaleTag (ps): " + pixelScale);
                    } else {
                        //logger.printDebug("tiff field: " + availTags[i].getType() + ", "+ availTags[i].getTag()  + ", "+ availTags[i].getCount());
                    }

                }

                fileSeekableStream.close();

                if (tiePoint!=null && pixelScale!=null){
                    isGeoTiff = true;
                    Coordinate upperLeft = null, lowerRight = null;

                    if (pixelOffset==null){
                        upperLeft = tiePoint;
                    } else {
                        upperLeft = new Coordinate( tiePoint.x - (pixelOffset.x * pixelScale.x), tiePoint.y - (pixelOffset.y * pixelScale.y));
                    }

                    lowerRight = new Coordinate( upperLeft.x + (imageDimensions.x * pixelScale.x), upperLeft.y - (imageDimensions.y * pixelScale.y));

                    //logger.printDebug("upperLeft: " + upperLeft);
                    //logger.printDebug("lowerRight: " + lowerRight);

                    env = new Envelope(upperLeft, lowerRight);
                }

              }else if(fileName.toLowerCase().endsWith(".flt")){
                  isGeoTiff = true;
                  GridFloat gf = new GridFloat(fileName);

                  Coordinate upperLeft = new Coordinate(gf.getXllCorner(), gf.getYllCorner() + gf.getnRows() * gf.getCellSize());
                  Coordinate lowerRight = new Coordinate(gf.getXllCorner() + gf.getnCols() * gf.getCellSize(), gf.getYllCorner());

                  env = new Envelope(upperLeft, lowerRight);

              }else if(fileName.toLowerCase().endsWith(".asc")){
                  isGeoTiff = true;
                  GridAscii ga = new GridAscii(fileName);

                  Coordinate upperLeft = new Coordinate(ga.getXllCorner(), ga.getYllCorner() + ga.getnRows() * ga.getCellSize());
                  Coordinate lowerRight = new Coordinate(ga.getXllCorner() + ga.getnCols() * ga.getCellSize(), ga.getYllCorner());

                  env = new Envelope(upperLeft, lowerRight);
              }

            if (!isGeoTiff || env==null){
                //logger.printDebug(PirolPlugInMessages.getString("no-worldfile-found"));
//                context.getWorkbench().getFrame().warnUser(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
                throw new Exception(I18N.get("org.openjump.core.rasterimage.AddRasterImageLayerWizard.no-worldfile-found"));
//                WizardDialog d = new WizardDialog(
//                       context.getWorkbench().getFrame(),
//                       I18N.get("RasterImagePlugIn.34")
//                               + worldFileHandler.getWorldFileName()
//                               + I18N.get("RasterImagePlugIn.35"),
//                       context.getErrorHandler());
//                d.init(new WizardPanel[] { new RasterImageWizardPanel() });
//                //Set size after #init, because #init calls #pack. [Jon Aquino]
//                d.setSize(500, 400);
//                GUIUtil.centreOnWindow(d);
//                d.setVisible(true);
//
//                if (!d.wasFinishPressed()) {
//                    //logger.printWarning("user canceled");
//                    return null;
//                }
//                try {
//                    minx = Double.parseDouble((String) d
//                           .getData(RasterImageWizardPanel.MINX_KEY));
//                    maxx = Double.parseDouble((String) d
//                           .getData(RasterImageWizardPanel.MAXX_KEY));
//                    miny = Double.parseDouble((String) d
//                           .getData(RasterImageWizardPanel.MINY_KEY));
//                    maxy = Double.parseDouble((String) d
//                           .getData(RasterImageWizardPanel.MAXY_KEY));
//
//                    env = new Envelope(minx, maxx, miny, maxy);
//                    }
//                    catch(java.lang.NumberFormatException e) {
//                            Viewport viewport = context.getLayerViewPanel().getViewport();
//                        Rectangle visibleRect = viewport.getPanel().getVisibleRect();
//
//                        int visibleX1 = visibleRect.x;
//                        int visibleY1 = visibleRect.y;
//                        int visibleX2 = visibleX1 + visibleRect.width;
//                        int visibleY2 = visibleY1 + visibleRect.height;
//                            Coordinate upperLeftVisible = viewport.toModelCoordinate(new Point(0,0));
//                        Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2)); 
//                        env = new Envelope(upperLeftVisible.x, lowerRightVisible.x, upperLeftVisible.y, lowerRightVisible.y);
//                    }
//
//                 } 
//
//                // creating world file
//                worldFileHandler = new WorldFileHandler(fileName, allwaysLookForTFWExtension);
//                worldFileHandler.writeWorldFile(env, imageDimensions.x, imageDimensions.y);
            }
        }
        return env;
    }
    
    public static CellSizeXY getCellSize(String fileNameOrURL) throws NoninvertibleTransformException, Exception {
       
        Point imageDims = getImageDimensions(fileNameOrURL);
        
        Envelope envelope = getGeoReferencing(fileNameOrURL, true, new Point(imageDims.x, imageDims.y));
        double cellSizeX = (envelope.getMaxX() - envelope.getMinX()) / imageDims.x;
        double cellSizeY = (envelope.getMaxY() - envelope.getMinY()) / imageDims.y;
    
        return new RasterImageIO().new CellSizeXY(cellSizeX, cellSizeY);
        
    }
    
    public static Envelope getViewingEnvelope(Viewport viewport) throws NoninvertibleTransformException {
        
        Rectangle visible = viewport.getPanel().getVisibleRect();
        int visibleX1 = visible.x;
        int visibleY1 = visible.y;
        int visibleX2 = visibleX1 + visible.width;
        int visibleY2 = visibleY1 + visible.height;
        
        Coordinate upperLeftVisible = viewport.toModelCoordinate(new Point(visibleX1, visibleY1));
        Coordinate lowerRightVisible = viewport.toModelCoordinate(new Point(visibleX2, visibleY2));
        
        return new Envelope(upperLeftVisible, lowerRightVisible);
        
    }
    
    public static Rectangle getDrawingRectangle(int imgWidth, int imgHeight, Envelope wholeImageEnvelope,
            Envelope viewportEnvelope, Resolution subsetResolution) throws NoninvertibleTransformException{
        
        if(viewportEnvelope == null || viewportEnvelope.contains(wholeImageEnvelope)) {
            Rectangle rect = new Rectangle(0, 0, imgWidth, imgHeight);
            return rect;
        } else if (viewportEnvelope.intersects(wholeImageEnvelope)) {
            
            Coordinate upperLeftVisible = new Coordinate(viewportEnvelope.getMinX(), viewportEnvelope.getMaxY());
            Coordinate lowerRightVisible = new Coordinate(viewportEnvelope.getMaxX(), viewportEnvelope.getMinY());
            
            java.awt.Point upperLeft = fromCoordinateToCell(
                    upperLeftVisible, new Coordinate(
                            wholeImageEnvelope.getMinX(), wholeImageEnvelope.getMinY()), (int)imgHeight, subsetResolution.getX(), subsetResolution.getY());
            java.awt.Point lowerRight = fromCoordinateToCell(
                    lowerRightVisible, new Coordinate(
                            wholeImageEnvelope.getMinX(), wholeImageEnvelope.getMinY()), (int)imgHeight, subsetResolution.getX(), subsetResolution.getY());
            
            int xOffset = Math.max(0, upperLeft.x);
            int yOffset = Math.max(0, upperLeft.y);
            
            int width = lowerRight.x - upperLeft.x;
            int height = lowerRight.y - upperLeft.y;
            
            Rectangle rect = new Rectangle(xOffset, yOffset, width, height);
            return rect;
        } else {
            return null;
        }
        
    }
    
    protected Rectangle getVisibleImageCoordinatesOfImage( double imgWidth, double imgHeight, Envelope visible, Envelope imageEnv ){
        
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
        
        if (offset2VisibleX >= 0){
            xOffset = 0;
        } else {
            xOffset = (int)(-offset2VisibleX * scaleX);
        }
        
        if (offset2VisibleY >= 0){
            yOffset = 0;
        } else {
            yOffset = (int)(-offset2VisibleY * scaleY);
        }
        
        int width = (int)((maxVisibleX-minVisibleX) * scaleX);
        int height =  (int)((maxVisibleY-minVisibleY) * scaleY);
        
        if (width < imgWidth && height < imgHeight){ 
            width += 1;
            height += 1;
        }
        
        
        if (width <= 0 || height <= 0){
            return null;
        }
        
        return new Rectangle(xOffset, yOffset, width, height);
    }
    
    public static java.awt.Point fromCoordinateToCell(Coordinate coord, Coordinate lowerLeftCoord, int rowCount, double cellSizeX, double cellSizeY) {
        
        java.awt.Point point = new java.awt.Point();
        point.x = (int)Math.floor((coord.x - lowerLeftCoord.x) / cellSizeX);
        point.y = rowCount - (int)Math.floor((coord.y - lowerLeftCoord.y) / cellSizeY) - 1;

        return point; 
        
    }
    
    public static Coordinate fromCellToCoordinate(java.awt.Point cell, Coordinate lowerLeftCoord, double cellSize, int rowCount) {
        
        Coordinate coord = new Coordinate();
        coord.x = lowerLeftCoord.x + cell.x * cellSize + 0.5 * cellSize;
        coord.y = lowerLeftCoord.y + (rowCount - cell.y) * cellSize - 0.5 * cellSize;
        return coord;
        
    }

    public void writeImage(
            File outFile,
            Raster raster,
            Envelope envelope,
            double cellSize, double noData) throws FileNotFoundException, IOException {
        
        SampleModel sm = raster.getSampleModel();
        ColorModel colorModel = PlanarImage.createColorModel(sm);
        BufferedImage image = new BufferedImage(colorModel, (WritableRaster) raster, false, null);

        TIFFEncodeParam param = new TIFFEncodeParam();
        param.setCompression(TIFFEncodeParam.COMPRESSION_NONE);

        TIFFField[] tiffFields = new TIFFField[3];
        
        // Cell size
        tiffFields[0] = new TIFFField(GeoTiffConstants.ModelPixelScaleTag, TIFFField.TIFF_DOUBLE, 2, new double[]{cellSize, cellSize});        
        
        // No data
        String noDataS = Double.toString(noData);
        byte[] bytes = noDataS.getBytes();
        tiffFields[1] = new TIFFField(TiffTags.TIFFTAG_GDAL_NODATA, TIFFField.TIFF_BYTE, noDataS.length(), bytes);

        // Tie point
        tiffFields[2] = new TIFFField(GeoTiffConstants.ModelTiepointTag, TIFFField.TIFF_DOUBLE, 6,
                new double[]{0, 0, 0, envelope.getMinX(), envelope.getMaxY(), 0});
        
        param.setExtraFields(tiffFields);

        FileOutputStream tifOut = new FileOutputStream(outFile);
        TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec.createImageEncoder("tiff", tifOut, param);
        encoder.encode(image);
        tifOut.close();

        WorldFileHandler worldFileHandler = new WorldFileHandler(outFile.getAbsolutePath(), false);
        worldFileHandler.writeWorldFile(envelope, image.getWidth(), image.getHeight()); 
        
    }
    
    public static Resolution calcRequestedResolution(Viewport viewport) {
        
        double xRes = viewport.getEnvelopeInModelCoordinates().getWidth() / (double) viewport.getPanel().getVisibleRect().width;
        double yRes = viewport.getEnvelopeInModelCoordinates().getHeight() / (double) viewport.getPanel().getVisibleRect().height;
        
        Resolution requestedRes = new Resolution(xRes, yRes);
        return requestedRes;
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
    
        private final double cellSizeX;
        private final double cellSizeY;
        
    }
    
}
