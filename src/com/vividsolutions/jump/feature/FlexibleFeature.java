package com.vividsolutions.jump.feature;

import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openjump.core.ui.util.GeometryUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jump.util.FlexibleDateParser;

/**
 * a FlexibleFeature based on {@link BasicFeature} originally used by the
 * GeoJSON reader. currently adding 
 * - "autoextends" by returning null for undefined attribs
 * - lazy conversion of attributes (see {@link #getAttribute(int)})
 *   currently String, Date, Time, Timestamp
 */
public class FlexibleFeature extends BasicFeature {
  //private static HashMap<FeatureSchema,FlexibleFeatureSchema> oldToFlexMap = new HashMap();

  public FlexibleFeature(FeatureSchema featureSchema) {
    super(featureSchema);
  }

  @Override
  public Object getAttribute(int i) {
    FeatureSchema featureSchema = getSchema();
    
    if (i < 0)
      throw new InvalidParameterException("index must be greater or equal zero");

    Object attrib = null;
    //System.out.println(i+"/"+featureSchema.getAttributeType(i));

    Object[] attributes = super.getAttributes();
    // only grab attrib if stack holds it already
    if (i < attributes.length)
      attrib = attributes[i];

    // OJ does not allow null geoms!
    if (i == featureSchema.getGeometryIndex()) {
      // create and set an empty geom
      if (attrib == null) {
        attrib = GeometryUtils.createEmptyGeometry(GeometryCollection.class, null);
        setGeometry((Geometry) attrib);
      }
    }
    // enforce String if schema says so
    else if (getSchema().getAttributeType(i).equals(AttributeType.STRING) && attrib != null
        && !(attrib instanceof String)) {
      attrib = String.valueOf(attrib);
    }
    // enforce date object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.DATE) && attrib != null
        && !AttributeType.DATE.toJavaClass().isInstance(attrib)) {
      Date d = parse(attrib);
      if (d!=null){
        attrib = new java.sql.Date(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } else {
        attrib = null;
      }
    }
    // enforce time object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.TIME) && attrib != null
        && !AttributeType.TIME.toJavaClass().isInstance(attrib)) {
      Date d = parse(attrib);
      if (d!=null){
        attrib = new java.sql.Time(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } else {
        attrib = null;
      }
    }
    // enforce timestamp object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.TIMESTAMP) && attrib != null
        && !AttributeType.TIMESTAMP.toJavaClass().isInstance(attrib)) {
      Date d = parse(attrib);
      if (d!=null){
        attrib = new java.sql.Timestamp(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } else {
        attrib = null;
      }
    }

    return attrib;
  }

  private Date parse( Object dateObject ){
    Date d = null;
    // the celleditor replaces time attribs w/ a java.util.Date object
    if (java.util.Date.class.isInstance(dateObject))
      d = (java.util.Date) dateObject;
    else {
      try {
        //FlexibleDateParser.getDefaultInstance().setVerbose(true);
        d = FlexibleDateParser.getDefaultInstance().parse(dateObject.toString(), true);
      } catch (ParseException e) {
        // TODO: we should find a way to tell the user
        e.printStackTrace();
      }
    }
    return d;
  }

  /**
   * TODO: the method shouldn't be used anyway, still maybe we will have to
   * implement it later
   */
  @Override
  public Object[] getAttributes() {
    throw new UnsupportedOperationException("currently not implemented");
  }

  /**
   * setting an attribute, fixing the underlying array in case the schema
   * changed inbetween
   */
  public void setAttribute(int attributeIndex, Object newAttribute) {
    FeatureSchema schema = super.getSchema();
    Object[] oldAttribs = super.getAttributes();
    // add fields if schema changed in between
    int diffCount = schema.getAttributeCount() - oldAttribs.length;
    if (diffCount > 0) {
      List attributes = new ArrayList(Arrays.asList(oldAttribs));
      attributes.addAll(Arrays.asList(new Object[diffCount]));
      super.setAttributes(attributes.toArray());
    }
    super.setAttribute(attributeIndex, newAttribute);
  }

  /**
   * setting the geometry by explicitly using the flexible setAttribute() method
   * above
   */
  public void setGeometry(Geometry geometry) {
    setAttribute(getSchema().getGeometryIndex(), geometry);
  }

  /**
   * getting the geometry by explicitly using the flexible getAttribute() method
   * above
   */
  public Geometry getGeometry() {
    return (Geometry) getAttribute(getSchema().getGeometryIndex());
  }

// disabled for now until flexschema can properly be edited via EditSchema plugin
//  @Override
//  public void setSchema(FeatureSchema schema) {
//    FlexibleFeatureSchema flexSchema;
//    // make sure to always use the same schema instance
//    // so changes pop up in all features using it
//    if (oldToFlexMap.containsKey(schema))
//      flexSchema = oldToFlexMap.get(schema);
//    else {
//      flexSchema = new FlexibleFeatureSchema(schema);
//      oldToFlexMap.put(schema, flexSchema);
//    }
//
//    super.setSchema(flexSchema);
//  }



}
