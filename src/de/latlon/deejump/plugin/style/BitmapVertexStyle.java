package de.latlon.deejump.plugin.style;

import static org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_HEIGHT;
import static org.apache.batik.transcoder.SVGAbstractTranscoder.KEY_WIDTH;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * <code>BitmapVertexStyle</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date: 2008-02-14 14:37:00 +0100 (Thu, 14 Feb
 *          2008) $
 */
public class BitmapVertexStyle extends VertexStyle {

    private Image image;

    private Point2D point;

    private String fileName;

    /**
     * 
     */
    public BitmapVertexStyle() {
        // for java2xml
    }

    /**
     * @param fileName the name of the file describing this BitmapVertexStyle
     */
    public BitmapVertexStyle(String fileName) {
        // init with a this.shape = poly with 1 point (?)
        super(null);
        if (fileName == null) {
            throw new NullPointerException("Image URL cannot be null.");
        }
        setFileName(fileName);
    }

    @Override
    public void paint(Graphics2D g, Point2D p) {
        this.point = p;
        render(g);
    }

    @Override
    protected void render(Graphics2D g) {
        g.drawImage(image, (int) point.getX() - ((image.getWidth(null)) / 2), (int) point.getY()
                - ((image.getHeight(null)) / 2), null);

    }

    /**
     * @return the image
     */
    public Image getImage() {
        return image;
    }

    /**
     * @return the image
     */
    public String getFileName() {
        return fileName;
    }

    @Override
    public void setFillColor(Color c) {
        super.setFillColor(c);
        if (fileName != null && fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    @Override
    public void setLineColor(Color c) {
        super.setLineColor(c);
        if (fileName != null && fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        if (fileName != null && fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    /**
     * @param color the color to encode
     * @return a #rrggbb string
     */
    public static String toHexColor(Color color) {
        if (color == null) {
            color = Color.black;
        }
        String scol = Integer.toHexString(color.getRGB() & 0xffffff);
        while (scol.length() < 6) {
            scol = "0" + scol;
        }

        return "#" + scol;
    }

    // due to the lack of xpaths, this is VERY crude
    /**
     * @param file the svg file
     * @param stroke hex value of the stroke color to use
     * @param fill hex value of the fill color to use
     * @return the new svg code
     * @throws IOException if an IOException occurs
     */
    public static StringBuffer updateSVGColors(File file, String stroke, String fill) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        StringWriter sout = new StringWriter((int) file.length());
        PrintWriter out = new PrintWriter(sout);

        String s;
        while ((s = in.readLine()) != null) {
            s = s.replace("fill:#000000", "fill:" + fill);
            s = s.replace("fill:black", "fill:" + fill);
            s = s.replace("stroke:#000000", "stroke:" + stroke);
            s = s.replace("stroke:black", "stroke:" + stroke);
            out.println(s);
        }

        out.close();
        in.close();

        return sout.getBuffer();
    }

    /**
     * @param fileName svg file name
     * @param stroke hex value of the stroke color to use
     * @param fill hex value of the fill color to use
     * @param size image size
     * @return a SVG image with black colors overwritten with the given colors
     */
    public static BufferedImage getUpdatedSVGImage(String fileName, String stroke, String fill, int size) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(size * size * 4);
        TranscoderOutput output = new TranscoderOutput(bos);

        PNGTranscoder trc = new PNGTranscoder();
        try {
            Reader in = new StringReader(updateSVGColors(new File(fileName), stroke, fill).toString());
            TranscoderInput input = new TranscoderInput(in);
            if (size > 0) {
                trc.addTranscodingHint(KEY_HEIGHT, (float) size);
                trc.addTranscodingHint(KEY_WIDTH, (float) size);
            }
            trc.transcode(input, output);
            bos.close();
            ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
            MemoryCacheSeekableStream mcss = new MemoryCacheSeekableStream(is);
            RenderedOp rop = JAI.create("stream", mcss);
            return rop.getAsBufferedImage();
        } catch (Exception e) {
            Logger.error(e);
        }

        return null;
    }

    /**
     * @param fileName file name to use
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;

        if (fileName.toLowerCase().endsWith(".svg")) {
            image = getUpdatedSVGImage(fileName, toHexColor(getLineColor()), toHexColor(getFillColor()), getSize());
        } else {
            image = Toolkit.getDefaultToolkit().getImage(fileName);
        }
    }

}
