/*
 * Bassin Versant du Jaudy-Guindy-Bizien, 
 * Laboratoire RESO UMR ESO 6590 CNRS, Universit&euml; de Rennes 2
 * licence Cecill
 */

package org.openjump.sigle.utilities.geom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.geom.OpenJUMPMakeValidOp;
import com.vividsolutions.jump.workbench.Logger;

/**
 * This class can check if a FeatureCollection has only one Geometry Type and in
 * this case, return this Geometry Type.
 *
 * @author Erwan Bocher
 * @author Olivier Bedel
 * @version 2005-08-10
 * @author Giuseppe Aruta [2020-07-22] added two method to valid and to union by
 *         attribute a FeatureCollection
 */

public class FeatureCollectionUtil {

  // renvoie la dimension des entites de la featureCollection fc :
  // 0 pour si fc ne contient que des entites ponctuelles
  // 1 pour si fc ne contient que des entites lineaires
  // 2 pour si fc ne contient que des entites surfaciques
  // -1 si fc ne contient aucune entite, ou si elle contient des entites
  // ayant des dimensions differentes

  public static int getFeatureCollectionDimension(FeatureCollection fc) {
    int type = -1; // type des geometry des entites de la featurecollection
    // -1 correspond au type complexe
    // (plusieurs dimensions de geometry dans la meme featureCollection)

    if (fc.getFeatures().size() > 0) {
      Iterator i = fc.getFeatures().iterator();

      // initialisation des la variable type
      Feature f = (Feature) i.next();
      type = f.getGeometry().getDimension();
      // cas particulier des geometryCollection
      if (f.getGeometry() instanceof GeometryCollection) {
        GeometryCollection geomCol = (GeometryCollection) f.getGeometry();
        // on ne prend en compte que les geometryCollection non specialisees, ie pas les
        // multipoint, multilinstring ou multipolygon
        if (geomCol.getGeometryType().equalsIgnoreCase("GeometryCollection"))
          type = -1;
      }

      // on parcourt le reste des entites de la featureCollection
      while (i.hasNext() && type != -1) {
        f = (Feature) i.next();
        // si la geometrie de f est complexe, on marque le type comme complexe
        if (f.getGeometry() instanceof GeometryCollection) {
          GeometryCollection geomCol = (GeometryCollection) f.getGeometry();
          if (geomCol.getGeometryType().equalsIgnoreCase("GeometryCollection"))
            type = -1;
        }
        // si sa dimension ne correspond pas au
        // type precedent, on marque le type comme complexe
        if (f.getGeometry().getDimension() != type)
          type = -1;
      }
    }

    return type;
  }

  public static ArrayList getAttributesList(FeatureCollection fc) {

    ArrayList AttributesList = new ArrayList();
    FeatureSchema fs = fc.getFeatureSchema();

    for (int i = 0; i < fs.getAttributeCount() - 1; i++) {

      AttributesList.add(fs.getAttributeName(i));

    }

    return AttributesList;

  }

  /**
   * Method to make valid all the geometries of a FeatureCollection using the
   * class MakeValidOp.
   * 
   * A geometry is not valid if it is not simple: This condition occurs when any
   * of the following conditions are true: Incorrect ring orientation (polygon),
   * self-intersection rings (polygon) self-intersection path (polyline), unclosed
   * ring (polygon) (see also
   * https://desktop.arcgis.com/en/arcmap/latest/extensions/data-reviewer/finding-invalid-geometry.htm
   * for the complete list).
   * 
   * This method should be applied before merging features via
   * {@link #unionByAttributeValue(FeatureCollection, String) because not simple
   * geometries will be not processed
   * 
   * @param FeatureCollection fc
   */
  public static void validFeatureCollection(FeatureCollection fc) {
    OpenJUMPMakeValidOp makeValidOp = new OpenJUMPMakeValidOp();
    makeValidOp.setPreserveGeomDim(true);
    makeValidOp.setPreserveDuplicateCoord(false);
    for (Feature feature : fc.getFeatures()) {
      Geometry validGeom = makeValidOp.makeValid(feature.getGeometry());
      feature.setGeometry(validGeom);
    }
    // return fc;
  }

  /**
   * Merge features and their geometries in the FeatureCollection given when the
   * given attribute name contains identical values
   * 
   * @param featureCollection feature collection
   * @param attributeName attribute name
   * @throws Exception if an Exception occurred
   */
  public static void unionByAttributeValue(FeatureCollection featureCollection, String attributeName) throws Exception {
    FeatureSchema schema = featureCollection.getFeatureSchema();
    if (featureCollection.getFeatures().size() > 1 && featureCollection.getFeatures().get(0).getGeometry() != null) {
      featureCollection.getFeatures().get(0).getGeometry().getFactory();
    } else {
      Logger.error(I18N.getInstance().get("ui.plugin.analysis.DissolvePlugIn.needs-two-features-or-more"));
      // return null;
    }
    FeatureSchema newSchema;
    newSchema = schema;
    Map<Object, FeatureCollection> map = new HashMap<Object, FeatureCollection>();
    for (Feature feature : featureCollection.getFeatures()) {
      Object key = feature.getAttribute(attributeName);
      if (!map.containsKey(key)) {
        FeatureCollection fd = new FeatureDataset(featureCollection.getFeatureSchema());
        fd.add(feature);
        map.put(key, fd);
      } else {
        map.get(key).add(feature);
      }
    }
    featureCollection.removeAll(featureCollection.getFeatures());
    for (Iterator<Object> i = map.keySet().iterator(); i.hasNext();) {
      Object key = i.next();
      FeatureCollection fca = map.get(key);
      if (fca.size() > 0) {
        Feature feature = union(fca);
        feature.setAttribute(attributeName, key);
        Feature newFeature = new BasicFeature(newSchema);
        // Copy feature attributes in newFeature
        for (int j = 0, max = newSchema.getAttributeCount(); j < max; j++) {
          newFeature.setAttribute(j, feature.getAttribute(newSchema.getAttributeName(j)));
        }
        featureCollection.add(newFeature);
      }
    }
    // return featureCollection;
  }

  private static Feature union(FeatureCollection fc) {
    GeometryFactory factory = new GeometryFactory();
    Collection<Geometry> geometries = new ArrayList<Geometry>();
    for (Feature f : fc.getFeatures()) {
      Geometry g = f.getGeometry();
      geometries.add(g);
    }
    Geometry unioned = UnaryUnionOp.union(geometries);
    FeatureSchema schema = fc.getFeatureSchema();
    Feature feature = new BasicFeature(schema);
    if (geometries.size() == 0) {
      feature.setGeometry(factory.createGeometryCollection(new Geometry[] {}));
    } else {
      feature.setGeometry(unioned);
    }
    return feature;
  }

}
