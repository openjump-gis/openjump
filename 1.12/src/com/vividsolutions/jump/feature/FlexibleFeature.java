package com.vividsolutions.jump.feature;

import java.security.InvalidParameterException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.util.FlexibleDateParser;

/**
 * a FlexibleFeature based on {@link BasicFeature} originally used by the
 * GeoJSON reader. currently adding 
 * - "autoextends" by returning null for undefined attribs
 * - lazy conversion of attributes (see {@link #getAttribute(int)})
 *   currently types String, Date, Time, Timestamp
 */
public class FlexibleFeature extends BasicFeature {
  private FlexibleFeatureSchema featureSchema;

  public FlexibleFeature(FlexibleFeatureSchema featureSchema) {
    super(featureSchema);
    this.featureSchema = featureSchema;
  }

  @Override
  public Object getAttribute(int i) {
    if (i < 0)
      throw new InvalidParameterException("index must be greater or equal zero");

    Object attrib = null;

    Object[] attributes = super.getAttributes();
    // only grab attrib if stack holds it already
    if (i < attributes.length)
      attrib = attributes[i];

    // OJ does not allow null geoms!
    if (i == featureSchema.getGeometryIndex()) {
      // create and set an empty geom
      if (attrib == null) {
        attrib = featureSchema.createEmptyGeometry();
        setGeometry((Geometry) attrib);
      }
    }
    // enforce String if schema says so
    else if (featureSchema.getAttributeType(i).equals(AttributeType.STRING) && attrib != null
        && !(attrib instanceof String)) {
      attrib = String.valueOf(attrib);
    }
    // enforce date object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.DATE) && attrib != null
        && !AttributeType.DATE.toJavaClass().isInstance(attrib)) {
      try {
        Date d;
        // the celleditor replaces us w/ a java.util.Date object
        if (java.util.Date.class.isInstance(attrib))
          d = (java.util.Date) attrib;
        else
          d = FlexibleDateParser.getDefaultInstance().parse(attrib.toString(), false);
        attrib = (d==null) ? null : new java.sql.Date(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } catch (ParseException e) {
        // TODO: we should find a way to tell the user
        attrib = null;
      }
    }
    // enforce time object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.TIME) && attrib != null
        && !AttributeType.TIME.toJavaClass().isInstance(attrib)) {
      try {
        Date d;
        // the celleditor replaces us w/ a java.util.Date object
        if (java.util.Date.class.isInstance(attrib))
          d = (java.util.Date) attrib;
        else
          d = FlexibleDateParser.getDefaultInstance().parse(attrib.toString(), false);
        attrib = (d==null) ? null : new java.sql.Time(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } catch (ParseException e) {
        // TODO: we should find a way to tell the user
        attrib = null;
      }
    }
    // enforce timestamp object if not converted already
    else if (featureSchema.getAttributeType(i).equals(AttributeType.TIMESTAMP) && attrib != null
        && !AttributeType.TIMESTAMP.toJavaClass().isInstance(attrib)) {
      try {
        Date d;
        // the celleditor replaces us w/ a java.util.Date object
        if (java.util.Date.class.isInstance(attrib))
          d = (java.util.Date) attrib;
        else
          d = FlexibleDateParser.getDefaultInstance().parse(attrib.toString(), false);
        attrib = (d==null) ? null : new java.sql.Timestamp(d.getTime());
        // update the attribute object, so the conversion does not happen on
        // every getAttrib()
        setAttribute(i, attrib);
      } catch (ParseException e) {
        // TODO: we should find a way to tell the user
        attrib = null;
      }
    }

    return attrib;
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
    return (Geometry) getAttribute(featureSchema.getGeometryIndex());
  }

  /**
   * TODO: the method shouldn't be used anyway, still maybe we will have to
   * implement it later
   */
  @Override
  public Object[] getAttributes() {
    throw new UnsupportedOperationException("currently not implemented");
  }

}
