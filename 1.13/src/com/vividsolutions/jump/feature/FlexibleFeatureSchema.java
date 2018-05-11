package com.vividsolutions.jump.feature;

import org.openjump.core.ui.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.workbench.Logger;

/**
 *  a FlexibleFeatureSchema originally used by the GeoJSON reader.
 *  extends the basic {@link FeatureSchema} by
 *  - allow changing attrib types on the fly
 *  - creates empty geoms matching a previous set geomType
 */
public class FlexibleFeatureSchema extends FeatureSchema {
  Class geometryClass = null;
  GeometryFactory geometryFactory = new GeometryFactory();

  public FlexibleFeatureSchema() {
  }

  public FlexibleFeatureSchema(FeatureSchema featureSchema) {
    super(featureSchema);
  }

  public void setAttributeType(int attributeIndex, AttributeType type) {
    attributeTypes.set(attributeIndex, type);
  }

  public void setAttributeType(String name, AttributeType type) {
    setAttributeType(super.getAttributeIndex(name), type);
  }

  public void setGeometryType(Class clazz) {
    geometryClass = clazz;
  }

  public Class getGeometryType() {
    return geometryClass;
  }

  /**
   * creates an empty geometry matching the geom type set already or an empty
   * geom collection if that fails
   * 
   * @return geometry
   */
  public Geometry createEmptyGeometry() {
    if (geometryClass != null) {
      try {
        return GeometryUtils
            .createEmptyGeometry(geometryClass, geometryFactory);
      } catch (Exception e) {
        Logger.debug(e);
      }
    }

    return geometryFactory.createGeometryCollection(null);
  }

}
