package com.vividsolutions.jump.workbench.model;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;

import javax.xml.namespace.QName;

/**
 * A georeferenced Layerable is a Layerable wich is displayed according to
 * its position relative to the earth (rather than relative to the screen).
 * Most layers (Vector Layers, Referenced Image Layers and WMS Layers are
 * GeoreferencedLayerables.
 */
public interface GeoreferencedLayerable extends Layerable {

  Envelope getEnvelope();

  DataSourceQuery getDataSourceQuery();

  QName getSRS();

}
