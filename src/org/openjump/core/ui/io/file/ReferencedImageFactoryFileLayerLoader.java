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
package org.openjump.core.ui.io.file;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openjump.core.ui.util.TaskUtil;
import org.openjump.util.UriUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.io.CompressedFile;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactory;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.ReferencedImageLayer;

public class ReferencedImageFactoryFileLayerLoader extends
  AbstractFileLayerLoader {

  private WorkbenchContext workbenchContext;

  private ReferencedImageFactory imageFactory;

  // supportFileExtensions are added to the end of the list of the extension listed by
  // ReferencedImageFactory.getExtensions() double entries removed
  public ReferencedImageFactoryFileLayerLoader(
    WorkbenchContext workbenchContext, ReferencedImageFactory imageFactory,
    String[] supportFileExtensions) {
    // create with empty exts list set later
    super( imageFactory.getDescription(),
            new ArrayList() );
    this.imageFactory = imageFactory;
    this.workbenchContext = workbenchContext;
    // remove double entries; factory exts first, delivered after
    HashSet exts = new HashSet(Arrays.asList( imageFactory.getExtensions() ));
    if ( supportFileExtensions instanceof String[] ) 
        exts.addAll(Arrays.asList( supportFileExtensions ));
    this.addFileExtensions( new ArrayList(exts) );
  }

  public boolean open(TaskMonitor monitor, URI uri, Map<String, Object> options) throws Exception{
    LayerManager layerManager = workbenchContext.getLayerManager();

    layerManager.setFiringEvents(false);
    Layer layer = createLayer(layerManager, uri);
    layerManager.setFiringEvents(true);
    
    Feature feature;
    feature = createFeature(imageFactory, uri.toString(),
            getImageryLayerDataset(layer));

    // only add layer if no exception occured
    Category category = TaskUtil.getSelectedCategoryName(workbenchContext);
    category.add(0, layer);
        
    layer.getFeatureCollectionWrapper().add(feature);
    // setFeatureCollectionModified(false) to solve BUG ID: 3424399
    layer.setFeatureCollectionModified(false);
    String imageFilePath = (String)feature.getAttribute(ImageryLayerDataset.ATTR_FILE);
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
    ReferencedImageStyle irs = (ReferencedImageStyle)layer.getStyle(ReferencedImageStyle.class);
    return irs.getImageryLayerDataset();
  }

  private Feature createFeature(ReferencedImageFactory referencedImageFactory,
    String uri, ImageryLayerDataset imageryLayerDataset) throws Exception {

    Feature feature = new BasicFeature(ImageryLayerDataset.getSchema());
    feature.setAttribute(ImageryLayerDataset.ATTR_FILE, uri);
    feature.setAttribute(ImageryLayerDataset.ATTR_FORMAT,
      referencedImageFactory.getTypeName());
    feature.setAttribute(ImageryLayerDataset.ATTR_FACTORY,
      referencedImageFactory.getClass().getName());
    feature.setGeometry(new GeometryFactory().createPoint((Coordinate)null));
    imageryLayerDataset.createImage(feature);
    return feature;
  }

  private Layer createLayer(LayerManager layerManager, URI uri) {
    String layerName = CompressedFile.createLayerName(uri);

    Layer layer = new ReferencedImageLayer(layerName, Color.black, new FeatureDataset(
      ImageryLayerDataset.getSchema()), layerManager);
    layer.setEditable(true);
    layer.getBasicStyle().setEnabled(false);
    layer.getBasicStyle().setRenderingFill(false);
    layer.addStyle(new ReferencedImageStyle());
    return layer;
  }
}
