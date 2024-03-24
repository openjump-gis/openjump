package com.vividsolutions.jump.workbench.imagery.openjpeg;

import com.sun.media.jai.codec.SeekableStream;
import com.vividsolutions.jump.workbench.Logger;
import org.geotiff.image.jai.GeoTIFFDirectory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.openjump.core.rasterimage.Metadata;
import org.openjump.core.rasterimage.Resolution;
import org.openjump.core.rasterimage.Stats;
import org.w3c.dom.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Try to find GeoJP2 geo-reference information in image metadata
 */
public class GeoJP2 {
  private final String location;
  private Metadata metadata = null;

  GeoJP2(String location) {
    this.location = location;
    init();
  }

  public Metadata getMetadata() {
    return metadata;
  }

  private void init() {
    ImageInputStream iis;
    Map<String, Object> map = new LinkedHashMap<>();
    try {
      URI uri = new URI(location);
      File file = new File(uri);

      iis = ImageIO.createImageInputStream(file);
      Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
      while (readers.hasNext()) {
        ImageReader reader = readers.next();
        System.out.println("reader " + reader);
        reader.setInput(iis, true);
        // read metadata of first image
        try {
          IIOMetadata metadata = reader.getImageMetadata(0);
          if (metadata == null) {
            continue;
          }
          String[] names = metadata.getMetadataFormatNames();
          System.out.println(Arrays.toString(names));
          for (String name : names) {
            System.out.println("Format name: " + name);
            Node tree = metadata.getAsTree(name);
            toMap(map, tree, "", "@");
            for (Map.Entry<String, Object> e : map.entrySet()) {
              if (e.getValue() != null && e.getValue() instanceof double[]) {
                System.out.println(e.getKey() + ": " + Arrays.toString((double[]) e.getValue()));
              } else {
                System.out.println(e.getKey() + ": " + e.getValue());
              }
            }
          }
        } catch(Exception e) {
          Logger.warn(e);
        } finally {
          reader.dispose();
        }
      }
      if (map.get("/sun/JPEG2000HeaderBox/Width") == null) return;
      if (map.get("/sun/JPEG2000HeaderBox/Height") == null) return;
      if (map.get("/geotif/Tiepoints") == null /*&& map.get("/geotif/TransformationMatrix") == null*/) return;
      if (map.get("/geotif/PixelScale") == null /*&& map.get("/geotif/TransformationMatrix") == null*/) return;
      int width = Integer.parseInt(map.get("/sun/JPEG2000HeaderBox/Width").toString());
      int height = Integer.parseInt(map.get("/sun/JPEG2000HeaderBox/Height").toString());
      metadata = setMetadata(
          (double[])map.get("/geotif/Tiepoints"),
          (double[])map.get("/geotif/PixelScale"),
          width, height, Integer.parseInt(map.get("/sun/JPEG2000HeaderBox/NumComponents").toString()));
      iis.close();
    } catch (IOException | URISyntaxException e) {
      Logger.warn(e);
    }
  }

  private Metadata setMetadata(double[] tiePoints, double[] scale, int width, int height, int bands) {
    Coordinate pixelOffset = new Coordinate(tiePoints[0], tiePoints[1], tiePoints[2]);
    Coordinate tiePoint = new Coordinate(tiePoints[3], tiePoints[4], tiePoints[5]);
    Resolution pixelScale;
    if (scale.length == 2 || scale[2] == 0) {
      pixelScale = new Resolution(scale[0],scale[1]);
    } else {
      pixelScale = new Resolution(scale[0],scale[1],scale[2]);
    }
    Coordinate upperLeft, lowerRight;
    upperLeft = new Coordinate(
        tiePoint.x - (pixelOffset.x * pixelScale.getX()),
        tiePoint.y - (pixelOffset.y * pixelScale.getY())
    );
    lowerRight = new Coordinate(
        upperLeft.x + (width * pixelScale.getX()),
        upperLeft.y - (height * pixelScale.getY())
    );
    Envelope env = new Envelope(upperLeft, lowerRight);
    return new Metadata(env, env, new Point(width, height), new Point(width, height),
        pixelScale.getX(), pixelScale.getY(), Double.NaN, new Stats(bands));
  }

  private void toMap(Map<String,Object> map, Node node, String prefix, String attPrefix) throws IOException {
    // simplify input keys
    if (node.getNodeName().equals("com_sun_media_imageio_plugins_jpeg2000_image_1.0")) prefix = prefix + "/sun";
    else if (node.getNodeName().equals("OtherBoxes")) prefix = prefix;
    else if (node.getNodeName().equals("JPEG2000HeaderSuperBox")) prefix = prefix;
    else if (node.getNodeName().equals("OptionalBoxes")) prefix = prefix;
    else if (node.getNodeName().equals("javax_imageio_1.0")) prefix = prefix + "/javax";
    else prefix = prefix + "/" + node.getNodeName();
    String value = node.getNodeValue();
    if (prefix.equals("/com_sun_media_imageio_plugins_jpeg2000_image_1.0/OtherBoxes/JPEG2000UUIDBox/UUID")) {
      byte[] bytes = toByteArray(value);
      map.put(prefix, new String(bytes));
      System.out.println(new String(bytes));
    } else if (prefix.equals("/sun/JPEG2000UUIDBox/Data")) {
      byte[] bytes = toByteArray(value);
      System.out.println(bytes.length);
      GeoTIFFDirectory geoTIFFDirectory = new GeoTIFFDirectory(SeekableStream.wrapInputStream(
          new ByteArrayInputStream(toByteArray(value)), true), 0);
      map.put(prefix, geoTIFFDirectory);
      map.put("/geotif/Tiepoints", geoTIFFDirectory.getTiepoints());
      map.put("/geotif/PixelScale", geoTIFFDirectory.getPixelScale());
      map.put("/geotif/TransformationMatrix", geoTIFFDirectory.getTransformationMatrix());
      map.put("/geotif/Compression", geoTIFFDirectory.getCompression());
      map.put("/geotif/ImageType", geoTIFFDirectory.getImageType());
    } else {
      map.put(prefix, value);
    }
    NamedNodeMap attributes = node.getAttributes();
    for (int i = 0 ; i < attributes.getLength() ; i++) {
      Node attribute = attributes.item(i);
      map.put(prefix + "/" + attPrefix + attribute.getNodeName(), attribute.getNodeValue());
    }
    NodeList nodes = node.getChildNodes();
    for (int i = 0 ; i < nodes.getLength() ; i++) {
      Node n = nodes.item(i);
      toMap(map, n, prefix, attPrefix);
    }
  }

  private byte[] toByteArray(String string) {
    String[] tokens = string.split(" ");
    byte[] bytes = new byte[tokens.length];
    for (int j = 0 ; j < tokens.length ; j++) {
      bytes[j] = Byte.parseByte(tokens[j]);
    }
    return bytes;
  }

}
