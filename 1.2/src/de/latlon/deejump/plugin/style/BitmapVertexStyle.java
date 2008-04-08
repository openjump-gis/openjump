package de.latlon.deejump.plugin.style;

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
import java.net.MalformedURLException;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import com.sun.media.jai.codec.MemoryCacheSeekableStream;
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
     * @param fileName
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
        // don't use this, use this.shape
        // this.polygon.xpoints = new int [] { (int) p.getX()};
        // this.polygon.ypoints = new int [] { (int) p.getY() };
        // this.polygon.npoints=1;
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
        if (fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    @Override
    public void setLineColor(Color c) {
        super.setLineColor(c);
        if (fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        if (fileName.toLowerCase().endsWith(".svg")) {
            setFileName(fileName);
        }
    }

    /**
     * @param col
     * @return a #rrggbb string
     */
    public static String toHexColor(Color col) {
        if (col == null) {
            col = Color.black;
        }
        String scol = Integer.toHexString(col.getRGB() & 0xffffff);
        while (scol.length() < 6) {
            scol = "0" + scol;
        }

        return "#" + scol;
    }

    // due to the lack of xpaths, this is VERY crude
    /**
     * @param file
     * @param stroke
     * @param fill
     * @return the new svg code
     * @throws IOException
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
     * @param fileName
     * @param stroke
     * @param fill
     * @param size
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
                trc.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(size));
                trc.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(size));
            }
            trc.transcode(input, output);
            bos.close();
            ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
            MemoryCacheSeekableStream mcss = new MemoryCacheSeekableStream(is);
            RenderedOp rop = JAI.create("stream", mcss);
            return rop.getAsBufferedImage();
        } catch (TranscoderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param fileName
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
