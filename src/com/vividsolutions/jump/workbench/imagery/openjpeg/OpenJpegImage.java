package com.vividsolutions.jump.workbench.imagery.openjpeg;


import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImage;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.graphic.WorldFile;
import com.vividsolutions.jump.workbench.model.Disposable;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.AlphaSetting;
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
public class OpenJpegImage implements ReferencedImage, AlphaSetting, Disposable {

  private File location;
  private Envelope imageEnvInModelCoordinate = new Envelope();
  private Envelope previousViewportEnvInModelCoordinates;
  private Envelope subImageInViewportCoordinates;
  private int alpha = 255;
  private double[] pyramid_x_resolutions;
  private double[] pyramid_y_resolutions;
  protected BufferedImage cachedImage = null;


  public OpenJpegImage(String location) throws Exception {
    init(location);
  }

  private void init(String location) throws Exception {

    ImageReader reader = null;

    try {
      Logger.info("Init image reader for " + location);
      URI uri = new URI(location);
      this.location = new File(uri);
      Info info = new OpenJpeg().getInfo(this.location.toPath());
      imageEnvInModelCoordinate = getEnvelope(location, info.getNativeSize().width, info.getNativeSize().height);
      Logger.info("Image envelope in model coordinates : " + imageEnvInModelCoordinate);

      int numResolutions = info.getNumResolutions();
      reader = new OpenJp2ImageReaderSpi().createReaderInstance();
      ImageInputStream iis = ImageIO.createImageInputStream(this.location);
      reader.setInput(iis);
      pyramid_x_resolutions = new double[numResolutions];
      pyramid_y_resolutions = new double[numResolutions];
      for (int i = 0; i < numResolutions; i++) {
        pyramid_x_resolutions[i] = imageEnvInModelCoordinate.getWidth() / reader.getWidth(i);
        pyramid_y_resolutions[i] = imageEnvInModelCoordinate.getHeight() / reader.getHeight(i);
      }
    } finally {
      if (reader != null) {
        reader.dispose();
      }
    }
  }

  private Envelope getEnvelope(String location, int width, int height) throws URISyntaxException {
    // Try to retrieve envelope
    // 1/ from .j2w world file
    // 2/ from .tab georeference file
    // 3/ from GeoJP2 metadata (limited capabilities)
    // 4/ from GMLJP2 metadata (limited capabilities)
    // 5/ from the file name (supposing it follows french BDORTHO convention
    URI uri = new URI(location);
    String path = new File(uri).getPath();
    Logger.info("File path: "+ path);

    // Try to find georeference from a j2w file
    String j2wLocation = changeExtension(path, ".j2w");
    String tabLocation = changeExtension(path, ".tab");
    if (new File(j2wLocation).exists()) {
      Logger.info("Georeference from : " + j2wLocation);
      WorldFile wf = WorldFile.create(location);
      return new Envelope(
          wf.getXUpperLeft() - wf.getXSize() / 2,
          wf.getXUpperLeft() - wf.getXSize() / 2 + wf.getXSize() * width,
          wf.getYUpperLeft() - wf.getYSize() / 2,
          wf.getYUpperLeft() - wf.getYSize() / 2 + wf.getYSize() * height
      );
    }
    else if (new File(tabLocation).exists()) {
      Logger.info("Georeference from : " + tabLocation);
      return envelopeFromTab(tabLocation);
    }
    else {
      GeoJP2 geoJP2 = new GeoJP2(location);
      if (geoJP2.getMetadata() != null) {
        Logger.info("Georeference from GeoJP2");
        return geoJP2.getMetadata().getActualEnvelope();
      }
      GMLJP2 gmlJP2 = new GMLJP2(location);
      if (gmlJP2.getMetadata() != null) {
        Logger.info("Georeference from GMLJP2");
        return gmlJP2.getMetadata().getActualEnvelope();
      }
      // Last chance : try to find georeference from the file name where the name follows the BDORTHO convention
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
    }
    return new Envelope(0, width, 0, height);
  }

  private String changeExtension(String path, String newExt) {
    return path.replace(".jp2", newExt)
        .replace(".JP2", newExt);
  }

  private Envelope envelopeFromTab(String tabLocation) {
    Envelope envelope = null;
    // Try to find georeference from a tab file
    if (new File(tabLocation).exists()) {
      Logger.info("Find tab file : " + tabLocation);
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(Files.newInputStream(Paths.get(tabLocation))))) {
        String line;
        String[] tt1, tt2, tt3 = null, tt4;
        while (null != (line = br.readLine())) {
          if (line.trim().equals("Type \"RASTER\"")) {
            tt1 = br.readLine().split("[() ,]");
            tt2 = br.readLine().split("[() ,]");
            tt3 = br.readLine().split("[() ,]");
            tt4 = br.readLine().split("[() ,]");
            envelope = new Envelope(
                Double.parseDouble(tt1[1].trim()), Double.parseDouble(tt2[1].trim()),
                Double.parseDouble(tt4[2].trim()), Double.parseDouble(tt1[2].trim()));
          }
        }
      } catch (IOException e) {
        Logger.error(e);
      }
    }
    return envelope;
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
    // if nothing has changed, no reason to rerender the whole shebang
    // this is mainly the case when OJ lost and regained focus
    if (cachedImage == null ||
        !viewportEnvInModelCoordinates.equals(previousViewportEnvInModelCoordinates)) {
      try {
        int level = 0;
        for (int i = 0; i < pyramid_x_resolutions.length; i++) {
          if (pyramid_x_resolutions[i] * viewportScale > 1) break;
          level = i;
        }
        // Calculate the real world image edges
        Envelope subImageInModelCoordinate = imageEnvInModelCoordinate.intersection(viewportEnvInModelCoordinates);
        // Transformation from full image to model (world coordinates)
        AffineTransformation image2modelAffineTransformation =
            new AffineTransformation(
                pyramid_x_resolutions[level], 0, imageEnvInModelCoordinate.getMinX(),
                0, -pyramid_y_resolutions[level], imageEnvInModelCoordinate.getMaxY());
        // Using image2model transformation compute subIlage coordinates in full image coordinate system
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
        long t0 = System.currentTimeMillis();
        BufferedImage br = reader.read(level, param);
        Logger.debug("Read image " + br.getWidth() + "x" + br.getHeight() + " (level=" + level + "): " +
                (System.currentTimeMillis()-t0) + " s");
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f * alpha / 255));
        g.drawImage(br,
            (int) subImageInViewportCoordinates.getMinX(),
            (int) subImageInViewportCoordinates.getMinY(),
            (int) subImageInViewportCoordinates.getMaxX(),
            (int) subImageInViewportCoordinates.getMaxY(),
            0, 0, br.getWidth(), br.getHeight(),
            Color.BLACK, null);
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
      Composite composite = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f * alpha / 255));
      g.drawImage(cachedImage,
          (int) subImageInViewportCoordinates.getMinX(),
          (int) subImageInViewportCoordinates.getMinY(),
          (int) subImageInViewportCoordinates.getMaxX(),
          (int) subImageInViewportCoordinates.getMaxY(),
          0, 0, cachedImage.getWidth(), cachedImage.getHeight(),
          Color.BLACK, null);
      g.setComposite(composite);
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
