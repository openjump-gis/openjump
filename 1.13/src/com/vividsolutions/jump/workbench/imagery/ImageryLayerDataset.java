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
import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoImageFactory;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

public class ImageryLayerDataset {
  // keeping attribute names short makes them survive even saving in SHP files
  private static String prefix = "IMG";
  // keep them at max. 8 chars length for compatibility
  public static final String ATTR_GEOMETRY = prefix + "_GEOM";
  public static final String ATTR_URI =      prefix + "_URI";
  public static final String ATTR_FACTORY =  prefix + "_FACT";
  public static final String ATTR_ERROR =    prefix + "ERROR";
  public static final String ATTR_TYPE =     prefix + "_TYPE";
  public static final String ATTR_LOADER =   prefix + "LOADR";

  // deprecated old field names, used to import old saved datasets
  public static final String OLD_ATTR_GEOMETRY = "GEOMETRY";
  public static final String OLD_ATTR_FILE = "IMAGEFILE";
  public static final String OLD_ATTR_FORMAT = "IMAGEFORMAT";
  public static final String OLD_ATTR_ERROR = "IMAGEERROR";
  public static final String OLD_ATTR_TYPE = "IMAGETYPE";
  public static final String OLD_ATTR_FACTORY = "IMAGEFACT";
  
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
    if (feature.getString(ATTR_ERROR) != null && !feature.getString(ATTR_ERROR).equals("")) {
      return null;
    }
    if (!featureToReferencedImageMap.containsKey(feature)) {
      attachImage(feature);
    }
    // Will be null if an exception occurs [Jon Aquino 2005-04-12]
    return featureToReferencedImageMap.get(feature);
  }

  public void attachImage(Feature feature) throws Exception {
    attachImage(feature, this);
  }

  public static void attachImage(Feature feature, ImageryLayerDataset ils)
      throws Exception {
    String imageFilePath = feature.getString(ATTR_URI);
    if (imageFilePath == null) {
      throw new Exception("Image file path in '" + ATTR_URI + "' attribute is null");
    }
    GeometryFactory geometryFactory = new GeometryFactory();

    ReferencedImageFactory imageFactory = createFeatureFactory(feature);
    ReferencedImage referencedImage = imageFactory.createImage(imageFilePath);

    ils.addImage(feature, referencedImage);
    // create a geometry if there is no valid one already (from saved jml)
    // valid is a 5 point (last coord same as first) closed polygon
    if (!(feature.getGeometry() instanceof Polygon)
        || feature.getGeometry().getNumPoints() != 5) {
      Envelope env = referencedImage.getEnvelope();
      Geometry boundingBox = geometryFactory.toGeometry(env);
      // set a polygon geometry (used to manipulate image)
      feature.setGeometry(boundingBox);
    }
    // set an informational type value
    feature.setAttribute(ATTR_TYPE, referencedImage.getType());
  }

  public void dispose() {
    if (featureToReferencedImageMap != null) {
      featureToReferencedImageMap.clear();
      featureToReferencedImageMap = null;
    }
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
  public static Feature saveFeatureImgAttribs(Feature feature,
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
   * copy img attributes from an imprint feature
   * @param feature
   * @param imprint
   * @return
   */
  public static Feature saveFeatureImgAttribs(Feature feature, Feature imprint) {
    if (ImageryLayerDataset.isOldImageFeature(imprint)) {
      // copy factory
      feature.setAttribute(ImageryLayerDataset.ATTR_FACTORY,
          imprint.getString(ImageryLayerDataset.OLD_ATTR_FACTORY));
      // convert old file to uri
      String imageFile = imprint.getString(ImageryLayerDataset.OLD_ATTR_FILE);
      if (imageFile != null) {
        feature.setAttribute(ImageryLayerDataset.ATTR_URI, new File(imageFile).toURI().toString());
      }
    }
    else if (ImageryLayerDataset.isNewImageFeature(imprint)) {
      // copy factory & loader
      feature.setAttribute(ImageryLayerDataset.ATTR_FACTORY,
          imprint.getString(ImageryLayerDataset.ATTR_FACTORY));
      feature.setAttribute(ImageryLayerDataset.ATTR_LOADER,
          imprint.getString(ImageryLayerDataset.ATTR_LOADER));
      // and src uri
      feature.setAttribute(ImageryLayerDataset.ATTR_URI,
         imprint.getString(ImageryLayerDataset.ATTR_URI));
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
    String factoryClassPath = feature.getString(ATTR_FACTORY);
    if (factoryClassPath == null) {
      throw new InstantiationException("Cannot instantiate ReferencedImageFactory: " +
        ATTR_FACTORY + " is null");
    }
    String loaderClassPath = feature.getString(ATTR_LOADER);
    if (factoryClassPath == null) {
      throw new InstantiationException("Cannot instantiate ReferencedImageFactory: " +
              ATTR_LOADER + " is null");
    }
    Class imageFactoryClass = Class.forName(factoryClassPath);
    ReferencedImageFactory imageFactory = (ReferencedImageFactory) imageFactoryClass
        .newInstance();

    // set preselected loader for GeoImageFactory explicitly
    if (!loaderClassPath.isEmpty() && imageFactory instanceof GeoImageFactory) {
      try {
        String loaderParam = "";
        if (loaderClassPath.contains("|")){
          String[] parts = loaderClassPath.split("|",1);
          loaderClassPath = parts[0];
          loaderParam = parts[1];
        }
        
        Class loaderClass = Class.forName(loaderClassPath);
        Object loader = null;
//        if (loaderParam.isEmpty())
          loader = loaderClass.newInstance();
//        else {
//          try {
//            Constructor c;
//            c = loaderClass.getDeclaredConstructor(Object.class);
//            loader = c.newInstance(loaderParam);
//          } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//          }
//        }

        ((GeoImageFactory) imageFactory).setLoader(loader);
      } catch (ClassNotFoundException e) {
        // TODO: handle exception
        System.out.println("ILDS: TODO - show user warning - "+e);
      }
    }
    return imageFactory;
  }
  
  public static boolean isImageFeature(Feature f) {
    return isNewImageFeature(f) || isOldImageFeature(f);
  }
  
  public static boolean isNewImageFeature(Feature f) {
    String[] attribs = new String[] { ATTR_URI, ATTR_FACTORY,
        ATTR_ERROR, ATTR_TYPE, ATTR_LOADER };
    for (String key : attribs) {
      if (!f.getSchema().hasAttribute(key))
        return false;
    }
    return true;
  }
  
  public static boolean isOldImageFeature(Feature f) {
    String[] attribs = new String[] { OLD_ATTR_FILE,
        OLD_ATTR_FORMAT, OLD_ATTR_ERROR, OLD_ATTR_TYPE, OLD_ATTR_FACTORY };
    for (String key : attribs) {
      if (!f.getSchema().hasAttribute(key))
        return false;
    }
    return true;
  }

}