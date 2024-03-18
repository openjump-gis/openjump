package com.vividsolutions.jump.workbench.imagery.openjpeg;


import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.graphic.WorldFile;
import com.vividsolutions.jump.workbench.model.Disposable;
import com.vividsolutions.jump.workbench.ui.Viewport;
import de.digitalcollections.openjpeg.Info;
import de.digitalcollections.openjpeg.OpenJpeg;
import de.digitalcollections.openjpeg.imageio.OpenJp2ImageReader;
import de.digitalcollections.openjpeg.imageio.OpenJp2ImageReaderSpi;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A {@link ReferencedImage} for ECW files
 */
public class OpenJpegImage implements ReferencedImage, Disposable {

  private File location;
  private Envelope imageEnvInModelCoordinate = new Envelope();
  private Envelope previousViewportEnvInModelCoordinates;
  private Envelope subImageInViewportCoordinates;
  //private double previousViewportScale = 0.0;
  private int alpha = 255;
  private double[] pyramid_resolutions;
  protected BufferedImage cachedImage = null;


  public OpenJpegImage(String location) throws Exception {
    init(location);
  }

  private void init(String location) throws Exception {

    ImageReader reader = null;

    try {
      URI uri = new URI(location);
      this.location = new File(uri);
      Info info = new OpenJpeg().getInfo(this.location.toPath());
      imageEnvInModelCoordinate = getEnvelope(location, info.getNativeSize().width, info.getNativeSize().height);
      Logger.info("Image envelope in model coordinates : " + imageEnvInModelCoordinate);

      int numResolutions = info.getNumResolutions();
      reader = new OpenJp2ImageReaderSpi().createReaderInstance();
      ImageInputStream iis = ImageIO.createImageInputStream(this.location);
      reader.setInput(iis);
      pyramid_resolutions = new double[numResolutions];
      for (int i = 0; i < numResolutions; i++) {
        pyramid_resolutions[i] = imageEnvInModelCoordinate.getWidth() / reader.getWidth(i);
      }
    } finally {
      if (reader != null) {
        reader.dispose();
      }
    }
  }

  private Envelope getEnvelope(String location, int width, int height) throws URISyntaxException, IOException {
    // TODO first option should be to read jp2 metadata, but the library has no option for that
    // Try to retrieve envelope
    // 1/ from jp2 metadata (limited capabilities)
    // 2/ from .j2w world file
    // 3/ from .tab georeference file
    // 4/ from the file name (supposing it follows french BDORTHO convention
    URI uri = new URI(location);
    GeoJP2 geoJP2 = new GeoJP2(location);
    if (geoJP2.getMetadata() != null) {
      return geoJP2.getMetadata().getActualEnvelope();
    }
    String path = new File(uri).getPath();
    Logger.info("Path: "+ path);
    // Try to find georeference from a j2w file
    String j2wLocation = path.replace(".jp2", ".j2w").replace(".JP2", ".j2w");
    if (new File(j2wLocation).exists()) {
      Logger.info("Find j2w file : " + j2wLocation);
      WorldFile wf = WorldFile.create(location);
      return new Envelope(
          wf.getXUpperLeft() - wf.getXSize() / 2,
          wf.getXUpperLeft() - wf.getXSize() / 2 + wf.getXSize() * width,
          wf.getYUpperLeft() - wf.getYSize() / 2,
          wf.getYUpperLeft() - wf.getYSize() / 2 + wf.getYSize() * height
      );
    }
    // Try to find georeference from a tab file
    String tabLocation = path.replace(".jp2", ".tab").replace(".JP2", ".tab");
    if (new File(tabLocation).exists()) {
      Logger.info("Find tab file : " + j2wLocation);
      BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(tabLocation))));
      String line;
      String[] tt1, tt2, tt3=null, tt4;
      while (null != (line = br.readLine())) {
        if (line.trim().equals("Type \"RASTER\"")) {
          tt1 = br.readLine().split("[() ,]");
          tt2 = br.readLine().split("[() ,]");
          tt3 = br.readLine().split("[() ,]");
          tt4 = br.readLine().split("[() ,]");
          return new Envelope(
              Double.parseDouble(tt1[1].trim()), Double.parseDouble(tt2[1].trim()),
              Double.parseDouble(tt4[2].trim()), Double.parseDouble(tt1[2].trim()));
        }
      }

    }
    // Try to find georeference from the file name where the name follows the BDORTHO convention
    Pattern pattern = Pattern.compile("(?i)\\d+-20\\d\\d-0*(\\d+)-0*(\\d+)-[0-9A-Z_]+-0M(\\d+)-([0-9A-Z_]+-)?E\\d+\\.jp2");
    Matcher matcher = pattern.matcher(new File(path).getName());
    Logger.info(matcher.toString());
    if (matcher.matches()) {
      Logger.info("File name matches BDORTHO pattern");
      double xmin = Double.parseDouble(matcher.group(1));
      double yMax = Double.parseDouble(matcher.group(2));
      double res = Double.parseDouble("0." + matcher.group(3));
      return new Envelope(xmin*1000, xmin*1000 + width*res, yMax*1000 - height*res, yMax*1000);
    }
    return new Envelope(0, width, 0, height);
  }

  @Override
  public Envelope getEnvelope() throws ReferencedImageException {
    return imageEnvInModelCoordinate;
  }

  public void paint(
      Feature f,
      Graphics2D g,
      Viewport viewport
  ) throws ReferencedImageException {

    OpenJp2ImageReader reader = null;
    final double viewportScale = viewport.getScale();
    Envelope viewportEnvInModelCoordinates = viewport.getEnvelopeInModelCoordinates();
    //Logger.info("--------------- new paint ----------------");

    // If image does not intersect viewport, return immediately
    if (!imageEnvInModelCoordinate.intersects(viewportEnvInModelCoordinates)) {
      cachedImage = null;
      previousViewportEnvInModelCoordinates = viewportEnvInModelCoordinates;
      return;
    }
    //viewport.getPanel().setDoubleBuffered(true);
    // if nothing has changed, no reason to rerender the whole shebang
    // this is mainly the case when OJ lost and regained focus
    if (cachedImage == null ||
        !viewportEnvInModelCoordinates.equals(previousViewportEnvInModelCoordinates)) {
      try {
        //Logger.info("viewport : " + viewportEnvInModelCoordinates);
        // only set view if viewport has changed
        int level = 0;
        for (int i = 0; i < pyramid_resolutions.length; i++) {
          if (pyramid_resolutions[i] * viewportScale > 1) break;
          level = i;
        }
        //Thread.sleep(100);
        // Here calculate the real world image edges
        Envelope subImageInModelCoordinate = imageEnvInModelCoordinate.intersection(viewportEnvInModelCoordinates);
        // Transformation from full image to model (world coordinates)
        AffineTransformation image2modelAffineTransformation =
            new AffineTransformation(
                pyramid_resolutions[level], 0, imageEnvInModelCoordinate.getMinX(),
                0, -pyramid_resolutions[level], imageEnvInModelCoordinate.getMaxY());
        // Using image2model transformation compute subIlage coordinates in full image coordinate system
        //viewport.zoom(viewportEnvInModelCoordinates);
        Envelope subImageInImageCoordinates = transform(
            subImageInModelCoordinate, image2modelAffineTransformation.getInverse());
        AffineTransform at = viewport.getModelToViewTransform();
        Point2D subImageULInViewportCoordinate = at.transform(
            new Point2D.Double(subImageInModelCoordinate.getMinX(), subImageInModelCoordinate.getMaxY()),
            new Point2D.Double());
        Point2D subImageLRInViewportCoordinate = at.transform(
            new Point2D.Double(subImageInModelCoordinate.getMaxX(), subImageInModelCoordinate.getMinY()),
            new Point2D.Double());
        subImageInViewportCoordinates = new Envelope(
            new Coordinate(subImageULInViewportCoordinate.getX(), subImageULInViewportCoordinate.getY()),
            new Coordinate(subImageLRInViewportCoordinate.getX(), subImageLRInViewportCoordinate.getY()));
        reader = (OpenJp2ImageReader)new OpenJp2ImageReaderSpi().createReaderInstance();
        ImageInputStream iis = ImageIO.createImageInputStream(this.location);
        reader.setInput(iis);
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(
            (int)subImageInImageCoordinates.getMinX(),
            (int)subImageInImageCoordinates.getMinY(),
            (int)subImageInImageCoordinates.getWidth(),
            (int)subImageInImageCoordinates.getHeight()));
        BufferedImage br = reader.read(level, param);
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f * alpha / 255));
        g.setTransform(new AffineTransform());
        g.drawImage(br,
            (int) subImageInViewportCoordinates.getMinX(),
            (int) subImageInViewportCoordinates.getMinY(),
            (int) subImageInViewportCoordinates.getMaxX(),
            (int) subImageInViewportCoordinates.getMaxY(),
            0, 0, br.getWidth(), br.getHeight(),
            Color.WHITE, null);
        g.setComposite(composite);
        cachedImage = br;
      } catch (Exception e) {
        throw new ReferencedImageException(e);
      } finally {
        previousViewportEnvInModelCoordinates = viewportEnvInModelCoordinates;
        if (reader != null) {
          reader.dispose();
        }
      }
    }
    else {
      g.drawImage(cachedImage,
          (int) subImageInViewportCoordinates.getMinX(),
          (int) subImageInViewportCoordinates.getMinY(),
          (int) subImageInViewportCoordinates.getMaxX(),
          (int) subImageInViewportCoordinates.getMaxY(),
          0, 0, cachedImage.getWidth(), cachedImage.getHeight(),
          Color.WHITE, null);
    }
    previousViewportEnvInModelCoordinates = viewportEnvInModelCoordinates;
  }

  private Envelope transform(Envelope env, AffineTransformation at) {
    Coordinate dstUL = new Coordinate();
    Coordinate dstLR = new Coordinate();
    at.transform(new Coordinate(env.getMinX(), env.getMaxY()), dstUL);
    at.transform(new Coordinate(env.getMaxX(), env.getMinY()), dstLR);
    return new Envelope(dstUL, dstLR);
  }

  public void close() {
    cachedImage = null;
  }

  public String getType() {
    return "JP2000";
  }

  public String getLoader() {
    try {
      return new OpenJp2ImageReaderSpi().createReaderInstance().getFormatName();
    } catch (IOException exception) {
      Logger.error(exception);
      return "";
    }
  }

  public int getAlpha() {
    return alpha;
  }

  public void setAlpha(int alpha) {
    this.alpha = alpha;
  }

  public void dispose() {
    close();
  }
}
