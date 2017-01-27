package org.openjump.core.ui.plugin.tools.generate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import org.openjump.core.ui.plugin.AbstractThreadedUiPlugIn;

import javax.swing.*;
import java.util.*;

/**
 * Common attributes and methods for LinearReferencing
 */
public abstract class AbstractLinearReferencingPlugIn  extends AbstractThreadedUiPlugIn {

    private static final String KEY = AbstractLinearReferencingPlugIn.class.getName();

    protected String DESCRIPTION;

    protected String DISTANCE_UNIT;
    protected String MAP_UNIT;
    protected String MAP_UNIT_TOOLTIP;
    protected String LINESTRING_FRACTION;
    protected String LINESTRING_FRACTION_TOOLTIP;

    protected String DISTANCE_AND_OFFSET;
    protected String DISTANCE;
    protected String DISTANCE_TOOLTIP;
    protected String OFFSET;
    protected String OFFSET_TOOLTIP;

    protected String REPEAT;
    protected String REPEAT_DISTANCE;
    protected String ADD_END_POINT;

    protected String EMPTY_RESULT;


    protected boolean map_unit = true;
    protected boolean linestring_fraction = false;
    protected double distance = 0;
    protected double offset = 0;
    protected boolean repeat = false;
    protected double repeat_distance = 0;
    protected boolean add_end_point = false;


    public AbstractLinearReferencingPlugIn(String s, ImageIcon icon) {
        super(s, icon);
    }

    public boolean execute(PlugInContext context) throws Exception {

        DESCRIPTION         = I18N.get(KEY + ".description");

        DISTANCE_UNIT       = I18N.get(KEY + ".distance-unit");
        MAP_UNIT            = I18N.get(KEY + ".map-unit");
        MAP_UNIT_TOOLTIP    = I18N.get(KEY + ".map-unit-tooltip");
        LINESTRING_FRACTION = I18N.get(KEY + ".linestring-fraction");
        LINESTRING_FRACTION_TOOLTIP    = I18N.get(KEY + ".linestring-fraction-tooltip");

        DISTANCE_AND_OFFSET = I18N.get(KEY + ".distance-and-offset");
        DISTANCE            = I18N.get(KEY + ".distance");
        DISTANCE_TOOLTIP    = I18N.get(KEY + ".distance-tooltip");
        OFFSET              = I18N.get(KEY + ".offset");
        OFFSET_TOOLTIP      = I18N.get(KEY + ".offset-tooltip");

        REPEAT              = I18N.get(KEY + ".repeat");
        REPEAT_DISTANCE     = I18N.get(KEY + ".repeat-distance");
        ADD_END_POINT       = I18N.get(KEY + ".add-end-point");

        EMPTY_RESULT        = I18N.get(KEY + ".empty-result");
        return true;
    }

    protected void setPointsAlong(FeatureCollection dataset, String layerName, String path, Geometry geometry) {

        LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(geometry);
        double length = geometry.getLength();
        double dist = linestring_fraction ? distance * length : distance;
        int count = 0;
        double delta = 0;
        double sign = distance == 0 ? 1.0 : Math.signum(distance);
        if (repeat) {
            // give to delta the same sign as distance (and 1 if distance = 0)
            sign = distance == 0 ? Math.signum(repeat_distance) : Math.signum(distance);
            delta = linestring_fraction ?
                    sign*Math.abs(repeat_distance)*length :
                    sign*Math.abs(repeat_distance);
        }
        while (Math.abs(dist) <= length) {
            Coordinate c = lengthIndexedLine.extractPoint(dist, offset);
            Feature feature = new BasicFeature(dataset.getFeatureSchema());
            feature.setGeometry(geometry.getFactory().createPoint(c));
            feature.setAttribute("LAYER", layerName);
            feature.setAttribute("PATH", path);
            feature.setAttribute("NUM", count++);
            feature.setAttribute("DISTANCE", dist);
            feature.setAttribute("OFFSET", offset);
            dataset.add(feature);
            if (add_end_point) {
                if (repeat && Math.abs(dist) < length && Math.abs(dist+delta) > length) {
                    dist = distance == 0 ? length : Math.signum(distance)*length;
                    continue;
                } else if (!repeat) {
                    dist = distance == 0 ? length : Math.signum(distance)*length;
                    continue;
                }
            }
            if (!repeat || delta == 0) break;
            dist += delta;
        }
    }

}
