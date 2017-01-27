package org.openjump.core.rasterimage;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.vividsolutions.jts.geom.Envelope;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.openjump.core.rasterimage.Overviews.OverviewLocation;

/**
 *
 * @author AdL
 */
public class OverviewsUtils {
    
    public static Overviews getOverviews(File tiffFile, Envelope envelope) throws IOException {

        Overviews overviews = new Overviews();
                
        // Check for internal overviews
        overviews = OverviewsUtils.addOverviews(tiffFile, envelope, OverviewLocation.INTERNAL, overviews);
        
        File ovrFile = new File(tiffFile.getParent(), tiffFile.getName() + ".ovr"); 
        if(ovrFile.exists()) {
            overviews = OverviewsUtils.addOverviews(ovrFile, envelope, OverviewLocation.EXTERNAL, overviews);
        }
        
        return overviews;
        
    }
    
    private static Overviews addOverviews(File tiffFile, Envelope envelope,
            OverviewLocation overviewLocation, Overviews overviews) throws IOException {
    
        ImageInputStream is = ImageIO.createImageInputStream(tiffFile);
        Iterator iterator = ImageIO.getImageReaders(is);
        if(iterator != null && iterator.hasNext()) {
            ImageReader reader = (ImageReader) iterator.next();
            reader.setInput(is);
            for(int i=0; i<reader.getNumImages(true); i++) {
                Resolution resolution = calcResolution(envelope, new java.awt.Point(reader.getWidth(i), reader.getHeight(i)));
                overviews.addOverview(new Overview(overviewLocation, resolution));
            }
        }
        
        return overviews;
    
    }
    
    private static Resolution calcResolution(Envelope envelope, java.awt.Point imageDims) {
        
        double xRes = envelope.getWidth() / imageDims.x;
        double yRes = envelope.getHeight() / imageDims.y;
        
        return new Resolution(xRes, yRes);

    }      
    
    /**
     * Modified from GeoTools' OverviewsEmbedder.java
     * @param tiffFile
     * @param compressionType
     * @param compressionRatio
     * @param overviewsCount
     * @param scaleAlgorithm
     * @throws IOException 
     */
    public static void createOverviews(File tiffFile, int overviewsCount) throws IOException {
        
        ParameterBlock pbjRead = new ParameterBlock();
        pbjRead.add(ImageIO.createImageInputStream(tiffFile));
        RenderedOp originalImage = JAI.create("ImageRead", pbjRead, null);

        
        OutputStream out = new FileOutputStream(new File(tiffFile.getParent(), tiffFile.getName() + ".ovr"));
        
        ParameterBlock paramBlock = new ParameterBlock();
        paramBlock.addSource(originalImage.createInstance());
        
        PlanarImage[] images = new PlanarImage[overviewsCount];
        for(int o=0; o<overviewsCount; o++) {
            
            int downsampleStep = (int) Math.pow(2, o+1);
            Double xScale = 1 / (double) downsampleStep;
            Double yScale = 1 / (double) downsampleStep;
            
            RenderedOp newImage = scaleAverage(paramBlock, xScale, yScale);
            images[o] = newImage.createInstance();
        }
        saveTiffJAI(out, images);

        
    }
    
    public static RenderedOp scaleAverage(ParameterBlock paramBlock, double xScale, double yScale) {
		// using filtered subsample operator to do a subsampling
        //ParameterBlockJAI pb = new ParameterBlockJAI("SubsampleAverage");
        //pb.addSource(src);
        paramBlock.removeParameters();
        paramBlock.add(xScale);
        paramBlock.add(yScale);

        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        return JAI.create("SubsampleAverage", paramBlock, qualityHints);
        
    }
    
    private static RenderedOp bicubic(ParameterBlock pb, int xScale, int yScale) {
        // using filtered subsample operator to do a subsampling
        //ParameterBlockJAI pb = new ParameterBlockJAI("filteredsubsample");
        //pb.addSource(src);
        pb.add(xScale);
        pb.add(yScale);
        pb.add(new float[] { 1.0f });
        pb.add(new InterpolationBicubic(2));
        return JAI.create("filteredsubsample", pb, new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE));
    }
    
    private static RenderedOp bilinear(ParameterBlock pb, double xScale, double yScale) {
        // using filtered subsample operator to do a subsampling
        //ParameterBlockJAI pb = new ParameterBlockJAI("filteredsubsample");
        //pb.addSource(src);
        pb.add(xScale);
        pb.add(yScale);
        pb.add(new float[] { 1.0f });
        pb.add(new InterpolationBilinear());
        return JAI.create("filteredsubsample", pb, new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE));
    }    
    
    private static RenderedOp subsample(PlanarImage src, int xScale, int yScale) {
        // using filtered subsample operator to do a subsampling
        ParameterBlockJAI pb = new ParameterBlockJAI("filteredsubsample");
        pb.addSource(src);
        pb.add(xScale);
        pb.add(yScale);
        pb.add(new float[] { 1.0f });
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("filteredsubsample", pb, new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE));
    }
    
    private static RenderedOp filteredSubsample(ParameterBlock pb, double xScale, double yScale) {
        // using filtered subsample operator to do a subsampling
        //ParameterBlockJAI pb = new ParameterBlockJAI("filteredsubsample");
        //pb.addSource(src);
        pb.add(xScale);
        pb.add(yScale);
        pb.add(lowPassFilter);
        pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("filteredsubsample", pb);
    }
    
    private static void saveTiffJAI(OutputStream out, PlanarImage[] images) throws IOException {
        
        TIFFEncodeParam param = new TIFFEncodeParam();
        param.setTileSize(512, 512);
        param.setWriteTiled(true);
        param.setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);

        ImageEncoder encoder = ImageCodec.createImageEncoder("TIFF", out, param);
        ArrayList list = new ArrayList();
        for (int i = 1; i < images.length; i++) {
            list.add(images[i]);
        }
        param.setExtraImages(list.iterator());
        encoder.encode(images[0]);
        out.close();
    }
    
    public enum CompressionType {
        LZW, JPEG
    }
    
    public enum ScaleAlgorithm {
        AVERAGE, SUBSAMPLE, BILINEAR, NEAREST_NEIGHBOUR, BICUBIC
    }
    
    private static final float[] lowPassFilter = new float[] {
            0.5F, 1.0F / 3.0F, 0.0F, -1.0F / 12.0F };
    
}
