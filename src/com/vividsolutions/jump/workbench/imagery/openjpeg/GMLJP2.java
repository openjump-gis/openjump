package com.vividsolutions.jump.workbench.imagery.openjpeg;

import com.vividsolutions.jump.workbench.Logger;
import org.locationtech.jts.geom.Envelope;
import org.openjump.core.rasterimage.Metadata;
import org.openjump.core.rasterimage.Stats;
import org.openjump.util.XPathUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Try to find GMLJP2 geo-reference information in image metadata
 */
public class GMLJP2 {

  private final String location;
  private Metadata metadata = null;
  private static final Set<Integer> blockTerminators = new HashSet<Integer>() {{ add(0); add(7); }};

  GMLJP2(String location) {
    this.location = location;
    init();
  }

  public Metadata getMetadata() {
    return metadata;
  }

  private void init() {
    try {
      URI uri = new URI(location);
      metadata = readGmlMetadata(Paths.get(uri));
    } catch (URISyntaxException e) {
      Logger.warn(e);
    }
  }

  /**
   * Namespace context with gml namespaces.
   */
  private static final NamespaceContext NSCONTEXT = new NamespaceContext() {

    public String getNamespaceURI(String prefix) {
      return "http://www.opengis.net/gml";
    }

    public String getPrefix(String namespace) {
      return "gml";
    }

    public Iterator<String> getPrefixes(String namespace) {
      return new Iterator<String>() {
        boolean done = false;
        public boolean hasNext() {
          return !done;
        }
        public String next() {
          done = true;
          return "gml";
        }
        public void remove() {}
      };
    }
  };

  private Metadata readGmlMetadata(Path jp2Path) {
    Metadata metadata = null;
    ByteSequenceMatcher jp2cMatcher = new ByteSequenceMatcher(new int[]{0x63, 0x32, 0x70, 0x6A});
    ByteSequenceMatcher xmlTagMatcher = new ByteSequenceMatcher(new int[]{0x20, 0x6C, 0x6D, 0x78});
    if (jp2Path != null && Files.isReadable(jp2Path)) {
      try (FileChannel channel = FileChannel.open(jp2Path)) {
        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
        int currentByte;
        while (mappedByteBuffer.hasRemaining() && !jp2cMatcher.matches((currentByte = mappedByteBuffer.get()))) {
          if (xmlTagMatcher.matches(currentByte)) {
            String xmlString = extractBlock(mappedByteBuffer);
            if (!xmlString.contains("gml:FeatureCollection")) continue;
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
              System.out.println(xmlString);
              // optional, but recommended process XML securely, avoid attacks like XML External Entities (XXE)
              docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
              docBuilderFactory.setNamespaceAware(true);
              DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
              Document doc = builder.parse(new InputSource(new StringReader(xmlString.trim())));
              metadata = toMetadata(doc);
            } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException ex1) {
              Logger.error(ex1);
            }
            if (xmlString.contains("gml:FeatureCollection")) break;
          }
        }
      } catch (IOException ex2) {
        Logger.error(ex2);
      }
    }
    return metadata;
  }

  private Metadata toMetadata(Document xmlDoc) throws XPathExpressionException {
    Element root = xmlDoc.getDocumentElement();
    Element envNode = XPathUtils.getElement("//gml:boundedBy/gml:Envelope", root, NSCONTEXT);
    Element lowerCorner = XPathUtils.getElement("//gml:lowerCorner", root, NSCONTEXT);
    Element upperCorner = XPathUtils.getElement("//gml:upperCorner", root, NSCONTEXT);
    Element grid = XPathUtils.getElement("//gml:limits/gml:GridEnvelope/gml:high", root, NSCONTEXT);
    String srsName = "EPSG:" + envNode.getAttribute("srsName").split("EPSG:")[1];
    String[] lower = lowerCorner.getTextContent().split(" ");
    String[] upper = upperCorner.getTextContent().split(" ");
    String[] gridSize = grid.getTextContent().split(" ");
    double llx = Double.parseDouble(lower[0]);
    double lly = Double.parseDouble(lower[1]);
    double urx = Double.parseDouble(upper[0]);
    double ury = Double.parseDouble(upper[1]);
    int nbcol = Integer.parseInt(gridSize[0]) + 1;
    int nbrow = Integer.parseInt(gridSize[1]) + 1;
    // For geographic coordinates (ex. WGS4), axis ordering is generally lat/lon
    if (Math.abs(llx) + Math.abs(lly) + Math.abs(urx) + Math.abs(ury) < (2*180 + 2*90)) {
      llx = Double.parseDouble(lower[1]);
      lly = Double.parseDouble(lower[0]);
      urx = Double.parseDouble(upper[1]);
      ury = Double.parseDouble(upper[0]);
      //nbcol = Integer.parseInt(gridSize[1]) + 1;
      //nbrow = Integer.parseInt(gridSize[0]) + 1;
    }

    return new Metadata(new Envelope(llx, urx, lly, ury), new Envelope(llx, urx, lly, ury),
        new Point(nbcol, nbrow), new Point(nbcol, nbrow),
        (urx - llx) / nbcol, (ury - lly) / nbrow, Double.NaN, new Stats(3));
  }

  private String extractBlock(MappedByteBuffer buffer) throws IOException {
    StringBuilder builder = new StringBuilder();
    int current;
    while (!blockTerminators.contains(current = buffer.get())) {
      builder.append((char) current);
    }
    return builder.toString();
  }

  private static class ByteSequenceMatcher {
    private final int[] queue;
    private final int[] sequence;

    ByteSequenceMatcher(int[] sequenceToMatch) {
      sequence = sequenceToMatch;
      queue = new int[sequenceToMatch.length];
    }

    public boolean matches(int unsignedByte) {
      insert(unsignedByte);
      return isMatch();
    }

    private void insert(int unsignedByte) {
      System.arraycopy(queue, 0, queue, 1, sequence.length - 1);
      queue[0] = unsignedByte;
    }

    private boolean isMatch() {
      boolean result = true;
      for (int i = 0; i < sequence.length; i++) {
        result = (queue[i] == sequence[i]);
        if (!result)
          break;
      }
      return result;
    }
  }

}
