/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package com.vividsolutions.jump.workbench.imagery;

import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.openjump.core.ui.io.file.AbstractFileLayerLoader;
import org.openjump.core.ui.util.TaskUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Prioritized;

public class ReferencedImageFactoryFileLayerLoader extends
    AbstractFileLayerLoader implements Prioritized {

  protected WorkbenchContext workbenchContext;

  protected ReferencedImageFactory imageFactory;

  public ReferencedImageFactory getImageFactory() {
    return imageFactory;
  }

  // supportFileExtensions are added to the end of the list of the extension
  // listed by
  // ReferencedImageFactory.getExtensions() double entries removed
  public ReferencedImageFactoryFileLayerLoader(
      WorkbenchContext workbenchContext, ReferencedImageFactory imageFactory,
      String[] additionalSupportedFileExtensions) {
    // create with empty exts list set later
    super(imageFactory.getDescription(), new ArrayList());
    this.imageFactory = imageFactory;
    this.workbenchContext = workbenchContext;
    // remove double entries; factory exts first, delivered after
    HashSet exts = new HashSet(Arrays.asList(imageFactory.getExtensions()));
    if (additionalSupportedFileExtensions instanceof String[])
      exts.addAll(Arrays.asList(additionalSupportedFileExtensions));
    this.addFileExtensions(new ArrayList(exts));
  }

  public boolean open(TaskMonitor monitor, URI uri, Map<String, Object> options)
      throws Exception {
    LayerManager layerManager = workbenchContext.getLayerManager();
    Layer targetLayer = (Layer) options.get("LAYER");

    // if (options.containsKey("LAYER"))
    // layer = (Layer) options.get("LAYER");
    // else {
    Layer layer;
    if (targetLayer != null) {
      layer = targetLayer;
    } else {
      layerManager.setFiringEvents(false);
      layer = createLayer(layerManager, uri);
      layerManager.setFiringEvents(true);
    }
    Feature feature;
    feature = createImageFeature(imageFactory, uri,
        getImageryLayerDataset(layer));

    // only add layer if no exception occured
    if (targetLayer == null) {
      Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
      category.add(0, layer);
    }

    layer.getFeatureCollectionWrapper().add(feature);
    // setFeatureCollectionModified(false) to solve BUG ID: 3424399
    layer.setFeatureCollectionModified(false);
    String imageFilePath = (String) feature
        .getAttribute(ImageryLayerDataset.ATTR_URI);
    if (imageFactory.isEditableImage(uri.toString())) {
      layer.setSelectable(true);
      layer.setEditable(true);
      layer.setReadonly(false);
    } else {
      layer.setSelectable(false);
      layer.setEditable(false);
      layer.setReadonly(true);
    }
    return true;
  }

  private ImageryLayerDataset getImageryLayerDataset(Layer layer) {
    ReferencedImageStyle irs = (ReferencedImageStyle) layer
        .getStyle(ReferencedImageStyle.class);
    return irs.getImageryLayerDataset();
  }

  static private Feature createBaseFeature(
      ReferencedImageFactory referencedImageFactory, URI uri) {
    Feature feature = new ReferencedImageFeature(
        ImageryLayerDataset.getSchema());
    feature.setAttribute(ImageryLayerDataset.ATTR_URI, uri.toString());
    ImageryLayerDataset.saveFeatureImgAttribs(feature, referencedImageFactory);
    feature.setGeometry(new GeometryFactory().createPoint((Coordinate) null));
    return feature;
  }

  /**
   * try to create an image feature from the given basic feature
   * @param f_orig
   * @return
   */
  public static Feature createImageFeature(Feature f_orig,
      ImageryLayerDataset imageryLayerDataset) {
    
    Feature f_new = new ReferencedImageFeature(
        ImageryLayerDataset.getSchema());
    // copy attribs over to new feature
    ImageryLayerDataset.saveFeatureImgAttribs(f_new, f_orig);

    try {
      imageryLayerDataset.attachImage(f_new);
    } catch (Exception e) {
      e.printStackTrace();
      ImageryLayerDataset.saveFeatureError(f_new, e);
      // save a dummy geometry
      f_new.setGeometry(new GeometryFactory().createPoint(new Coordinate())); 
    }
    return f_new;
  }
  
  static public Feature createImageFeature(
      ReferencedImageFactory referencedImageFactory, URI uri,
      ImageryLayerDataset imageryLayerDataset) throws Exception {

    Feature feature = createBaseFeature(referencedImageFactory, uri);
    // attach the image
    imageryLayerDataset.attachImage(feature);
    return feature;
  }

  static public Collection createImageFeatures(
      final ReferencedImageFactory referencedImageFactory, URI[] uris,
      final ImageryLayerDataset imageryLayerDataset) {
    return CollectionUtil.collect(Arrays.asList(uris), new Block() {
      public Object yield(Object uri) {
        Feature feature = createBaseFeature(referencedImageFactory, (URI) uri);
        try {
          imageryLayerDataset.attachImage(feature);
        } catch (Exception e) {
          ImageryLayerDataset.saveFeatureError(feature, e);
        }
        return feature;
      }
    });
  }

  public static Layer createLayer(LayerManager layerManager, URI uri) {
    String layerName = CompressedFile.createLayerName(uri);

    Layer layer = new ReferencedImagesLayer(layerName, Color.black,
        new FeatureDataset(ImageryLayerDataset.getSchema()), layerManager);
    layer.setEditable(true);
    layer.getBasicStyle().setEnabled(false);
    layer.getBasicStyle().setRenderingFill(false);
    layer.addStyle(new ReferencedImageStyle());
    return layer;
  }

  public int getPriority() {
    return (imageFactory instanceof Prioritized) ? ((Prioritized) imageFactory)
        .getPriority() : Prioritized.NOPRIORITY;
  }

}
