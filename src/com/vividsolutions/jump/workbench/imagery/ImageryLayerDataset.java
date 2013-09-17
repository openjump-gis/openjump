package com.vividsolutions.jump.workbench.imagery;

/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;

import com.sun.media.jai.operator.ImageReadDescriptor;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoImageFactory;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class ImageryLayerDataset {
  private static String prefix = "IMG.";
  public static final String ATTR_GEOMETRY = prefix+"GEOMETRY";
  public static final String ATTR_URI = prefix + "URI";
  // public static final String ATTR_FORMAT = "IMAGEFORMAT";
  public static final String ATTR_FACTORY = prefix + "FACTORY";
  public static final String ATTR_ERROR = prefix + "ERROR";
  public static final String ATTR_TYPE = prefix + "TYPE";
  public static final String ATTR_LOADER = prefix + "LOADER";

  private Map<Feature, ReferencedImage> featureToReferencedImageMap = new WeakHashMap();

  public static FeatureSchema SCHEMA = new FeatureSchema() {
    {
      addAttribute(ATTR_GEOMETRY, AttributeType.GEOMETRY);
      addAttribute(ATTR_URI, AttributeType.STRING);
      // addAttribute(ATTR_FORMAT, AttributeType.STRING);
      addAttribute(ATTR_FACTORY, AttributeType.STRING);
      addAttribute(ATTR_ERROR, AttributeType.STRING);
      addAttribute(ATTR_TYPE, AttributeType.STRING);
      addAttribute(ATTR_LOADER, AttributeType.STRING);
    }
  };

  public static FeatureSchema getSchema() {
    return SCHEMA;
  }

  private void removeImage(Feature feature) {
    featureToReferencedImageMap.remove(feature);
  }

  private void addImage(Feature feature, ReferencedImage referencedImage) {
    featureToReferencedImageMap.put(feature, referencedImage);
  }

  public ReferencedImage referencedImage(Feature feature) throws Exception {
    if (!(feature.getString(ATTR_ERROR) == null || feature
        .getString(ATTR_ERROR).equals(""))) {
      return null;
    }
    if (!featureToReferencedImageMap.containsKey(feature)) {
      createImage(feature);
    }
    // Will be null if an exception occurs [Jon Aquino 2005-04-12]
    return (ReferencedImage) featureToReferencedImageMap.get(feature);
  }

  public void createImage(Feature feature) throws Exception {
    createImage(feature, this);
  }

  public static void createImage(Feature feature, ImageryLayerDataset ils)
      throws Exception {
    String factoryClassPath = (String) feature.getString(ATTR_FACTORY);
    String loaderClassPath = (String) feature.getString(ATTR_LOADER);
    String imageFilePath = (String) feature.getString(ATTR_URI);
    GeometryFactory geometryFactory = new GeometryFactory();

    ReferencedImageFactory imageFactory = createFeatureFactory(feature);
    ReferencedImage referencedImage = imageFactory.createImage(imageFilePath);

    ils.addImage(feature, referencedImage);
    Envelope env = referencedImage.getEnvelope();
    Geometry boundingBox = geometryFactory.toGeometry(env);
    // set dummy geometry (used to manipulate image)
    feature.setGeometry(boundingBox);
    // set an informational type value
    feature.setAttribute(ATTR_TYPE, referencedImage.getType());
  }

  public void dispose() {
    featureToReferencedImageMap.clear();
    featureToReferencedImageMap = null;
  }

  public static Feature saveFeatureError(Feature feature, Throwable t) {
    feature.setAttribute(ImageryLayerDataset.ATTR_ERROR,
        WorkbenchFrame.toMessage(t) + "\n\n" + StringUtil.stackTrace(t));
    return feature;
  }

  /**
   * set a features attributes saving the assigned ReferencedImageFactory
   * 
   * @param feature
   * @param imageFactory
   * @return
   */
  public static Feature saveFeatureFactory(Feature feature,
      ReferencedImageFactory imageFactory) {
    feature.setAttribute(ImageryLayerDataset.ATTR_FACTORY, imageFactory
        .getClass().getName());
    if (imageFactory instanceof GeoImageFactory) {
      Object loader = ((GeoImageFactory) imageFactory).getLoader();
      if (loader != null)
        feature.setAttribute(ImageryLayerDataset.ATTR_LOADER, loader.getClass()
            .getName());
    }
    return feature;
  }

  /**
   * create a ReferencedImageFactory from the attributes of the given feature
   * 
   * @param feature
   * @return
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static ReferencedImageFactory createFeatureFactory(Feature feature)
      throws ClassNotFoundException, InstantiationException,
      IllegalAccessException {
    String factoryClassPath = (String) feature.getString(ATTR_FACTORY);
    String loaderClassPath = (String) feature.getString(ATTR_LOADER);
    Class imageFactoryClass = Class.forName(factoryClassPath);
    ReferencedImageFactory imageFactory = (ReferencedImageFactory) imageFactoryClass
        .newInstance();

    // set preselected loader for GeoImageFactory explicitly
    if (!loaderClassPath.isEmpty() && imageFactory instanceof GeoImageFactory) {
      try {
        Class loaderClass = Class.forName(loaderClassPath);
        Object loader = loaderClass.newInstance();
        ((GeoImageFactory) imageFactory).setLoader(loader);
      } catch (ClassNotFoundException e) {
        // TODO: handle exception
        System.out.println("ILDS: TODO - show user warning - "+e);
      }
    }
    return imageFactory;
  }
  
  /**
   * shamelessly stolen from ImageIO-Ext's ImageIOUtilities
   * @param rOp
   */
  public static void disposeImage(RenderedImage rOp) {
    if (rOp != null) {
        if (rOp instanceof RenderedOp) {
            RenderedOp renderedOp = (RenderedOp) rOp;

            final int nSources = renderedOp.getNumSources();
            if (nSources > 0) {
                for (int k = 0; k < nSources; k++) {
                    Object source = null;
                    try {
                        source = renderedOp.getSourceObject(k);

                    } catch (ArrayIndexOutOfBoundsException e) {
                        // Ignore
                    }
                    if (source != null) {
                        if (source instanceof RenderedOp) {
                            disposeImage((RenderedOp) source);
                        } else if (source instanceof BufferedImage) {
                            ((BufferedImage) source).flush();
                            source = null;
                        }
                    }
                }
            } else {
                // get the reader
                Object imageReader = rOp.getProperty(ImageReadDescriptor.PROPERTY_NAME_IMAGE_READER);
                if (imageReader != null && imageReader instanceof ImageReader) {
                    final ImageReader reader = (ImageReader) imageReader;
                    final ImageInputStream stream = (ImageInputStream) reader.getInput();
                    try {
                        stream.close();
                    } catch (Throwable e) {
                        // swallow this
                    }
                    try {
                        reader.dispose();
                    } catch (Throwable e) {
                        // swallow this
                    }
                }
            }
            final Object roi = rOp.getProperty("ROI");
            if (roi != null && (roi instanceof ROI || roi instanceof RenderedImage)) {
                ROI roiImage = (ROI) roi;
                PlanarImage image = roiImage.getAsImage();
                if (image != null) {
                    image.dispose();
                    image = null;
                    roiImage = null;
                }
            }

            if (rOp instanceof PlanarImage) {
                ((PlanarImage) rOp).dispose();
            } else if (rOp instanceof BufferedImage) {
                ((BufferedImage) rOp).flush();
                rOp = null;
            }
        }
    }
}
}