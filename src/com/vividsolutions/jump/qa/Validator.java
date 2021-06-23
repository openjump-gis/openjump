
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

package com.vividsolutions.jump.qa;

import java.util.*;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.IsSimpleOp;
import org.locationtech.jts.operation.valid.*;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.geom.Angle;
import com.vividsolutions.jump.geom.CoordUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CoordinateArrays;


/**
 * Performs basic JTS validation, and additional validation like checking polygon
 * orientation.
 */
public class Validator {

    private boolean checkingBasicTopology = true;
    private boolean checkingPolygonOrientation = false;
    private boolean checkingGeometriesSimple = false;
    private boolean checkingMinSegmentLength = false;
    private boolean checkingMinAngle = false;
    private boolean checkingMinPolygonArea = false;
    private boolean checkingNoRepeatedConsecutivePoints = false;
    private boolean checkingNoHoles = false;
    private double minSegmentLength = 0;
    private double minAngle = 0;
    private double minPolygonArea = 0;
    private Collection<String> disallowedGeometryClassNames = new ArrayList<>();
    private RepeatedPointTester repeatedPointTester = new RepeatedPointTester();

    //<<TODO:REFACTORING>> Move this class and associated classes to JTS [Jon Aquino]
    public Validator() {
    }

    /**
     * Sets whether basic JTS validation should be performed
     * @param checkingBasicTopology whether basic JTS validation should be performed
     */
    public void setCheckingBasicTopology(boolean checkingBasicTopology) {
        this.checkingBasicTopology = checkingBasicTopology;
    }

    /**
     * Sets whether consecutive points are not allowed to be the same
     * @param checkingNoRepeatedConsecutivePoints whether consecutive points are
     * not allowed to be the same
     */
    public void setCheckingNoRepeatedConsecutivePoints(
        boolean checkingNoRepeatedConsecutivePoints) {
        this.checkingNoRepeatedConsecutivePoints = checkingNoRepeatedConsecutivePoints;
    }

    /**
     * Sets whether polygons are not allowed to have holes
     * @param checkingNoHoles whether polygons are not allowed to have holes
     */
    public void setCheckingNoHoles(boolean checkingNoHoles) {
        this.checkingNoHoles = checkingNoHoles;
    }

    /**
     * Sets whether polygon orientation should be checked
     * @param checkingPolygonOrientation whether to enforce the constraint that
     * polygon shells should be oriented clockwise and holes should be oriented
     * counterclockwise
     */
    public void setCheckingPolygonOrientation(
        boolean checkingPolygonOrientation) {
        this.checkingPolygonOrientation = checkingPolygonOrientation;
    }

    /**
     * Sets the segment length below which the minimum-segment-length check
     * will raise a validation error.
     * @param minSegmentLength the threshold used by the minimum-segment-length
     * check
     * @see #setCheckingMinSegmentLength(boolean)
     */
    public void setMinSegmentLength(double minSegmentLength) {
        this.minSegmentLength = minSegmentLength;
    }

    /**
     * Sets the angle below which the minimum-angle check
     * will raise a validation error.
     * @param minAngle the threshold used by the minimum-angle check, in degrees
     * @see #setCheckingMinAngle(boolean)
     */
    public void setMinAngle(double minAngle) {
        this.minAngle = minAngle;
    }

    /**
     * Sets the area below which the minimum-polygon-area check will raise a
     * validation error.
     * @param minPolygonArea the threshould used by the minimum-polygon-area check
     * @see #setCheckingMinPolygonArea(boolean)
     */
    public void setMinPolygonArea(double minPolygonArea) {
        this.minPolygonArea = minPolygonArea;
    }
    
    /**
     * Sets whether to enforce the constraint that Geometries must be simple
     * @param checkingGeometriesSimple whether to enforce the constraint that
     * Geometries must be simple
     * @since OpenJUMP 1.6
     */
    public void setCheckingGeometriesSimple(boolean checkingGeometriesSimple) {
        this.checkingGeometriesSimple = checkingGeometriesSimple;
    }

    /**
     * Sets whether minimum segment length should be checked.
     * @param checkingMinSegmentLength whether to enforce the constraint that
     * segment length should be no less than the minimum
     * @see #setMinSegmentLength(double)
     */
    public void setCheckingMinSegmentLength(boolean checkingMinSegmentLength) {
        this.checkingMinSegmentLength = checkingMinSegmentLength;
    }

    /**
     * Sets whether minimum angle should be checked.
     * @param checkingMinAngle whether to enforce the constraint that
     * angle should be no less than the minimum
     * @see #setMinAngle(double)
     */
    public void setCheckingMinAngle(boolean checkingMinAngle) {
        this.checkingMinAngle = checkingMinAngle;
    }

    /**
     * Sets whether minimum polygon area should be checked.
     * @param checkingMinPolygonArea whether to enforce the constraint that
     * area should be no less than the minimum, for single polygons and polygon
     * elements of GeometryCollections (including MultiPolygons)
     * @see #setMinPolygonArea(double)
     */
    public void setCheckingMinPolygonArea(boolean checkingMinPolygonArea) {
        this.checkingMinPolygonArea = checkingMinPolygonArea;
    }

    /**
     * Sets the Geometry classes that are not allowed in the dataset that will
     * be validated.
     * @param disallowedGeometryClasses Geometry classes (Polygon.class, for
     * example) that are not allowed
     */
    public void setDisallowedGeometryClasses(Collection<Class> disallowedGeometryClasses) {
        disallowedGeometryClassNames.clear();

        for (Class clazz : disallowedGeometryClasses) {
            disallowedGeometryClassNames.add(clazz.getName());
        }
    }

    /**
     * Checks a collection of features.
     * @param features the Feature's to validate
     * @return a List of ValidationErrors; if all features are valid, the list
     * will be empty
     */
    public List validate(Collection features, TaskMonitor monitor) {
        monitor.allowCancellationRequests();
        int validatedFeatureCount = 0;
        monitor.report(I18N.getInstance().get("qa.Validator.validating"));

        List<Object> validationErrors = new ArrayList<>();
        int totalFeatures = features.size();

        for (Iterator i = features.iterator();
                i.hasNext() && !monitor.isCancelRequested();) {
            Feature feature = (Feature) i.next();
            validate(feature, validationErrors);
            validatedFeatureCount++;
            monitor.report(validatedFeatureCount, totalFeatures, "features");
        }

        return validationErrors;
    }

    protected void addIfNotNull(Object item, Collection<Object> collection) {
        if (item == null) {
            return;
        }
        collection.add(item);
    }

    /**
     * Checks a feature.
     * @param feature the Feature to validate
     * @param validationErrors a List of ValidationError's to add to if the feature
     * is not valid
     */
    protected void validate(Feature feature, List<Object> validationErrors) {
        addIfNotNull(validateGeometryClass(feature), validationErrors);

        if (checkingBasicTopology) {
            addIfNotNull(validateBasicTopology(feature), validationErrors);
        }

        if (checkingPolygonOrientation) {
            addIfNotNull(validatePolygonOrientation(feature), validationErrors);
        }

        if (checkingGeometriesSimple) {
            addIfNotNull(validateGeometriesSimple(feature), validationErrors);
        }

        if (checkingMinSegmentLength) {
            addIfNotNull(validateMinSegmentLength(feature), validationErrors);
        }

        if (checkingMinAngle) {
            addIfNotNull(validateMinAngle(feature), validationErrors);
        }

        if (checkingMinPolygonArea) {
            addIfNotNull(validateMinPolygonArea(feature), validationErrors);
        }

        if (checkingNoHoles) {
            addIfNotNull(validateNoHoles(feature), validationErrors);
        }

        if (checkingNoRepeatedConsecutivePoints) {
            addIfNotNull(validateNoRepeatedConsecutivePoints(feature),
                validationErrors);
        }
    }

    protected ValidationError validateGeometryClass(Feature feature) {
        //Match by class name rather than instanceof, which is less strict
        //(e.g. instanceof considers a MultiLineString to be a GeometryCollection)
        //[Jon Aquino]
        if (disallowedGeometryClassNames.contains(feature.getGeometry()
                                                             .getClass()
                                                             .getName())) {
            return new ValidationError(ValidationErrorType.GEOMETRY_CLASS_DISALLOWED,
                feature);
        }

        return null;
    }

    protected ValidationError validateBasicTopology(Feature feature) {
        TopologyValidationError error = (new IsValidOp(feature.getGeometry())).getValidationError();

        if (error != null) {
            return new BasicTopologyValidationError(error, feature);
        }

        return null;
    }

    protected ValidationError validateNoRepeatedConsecutivePoints(
        Feature feature) {
        if (repeatedPointTester.hasRepeatedPoint(feature.getGeometry())) {
            return new ValidationError(ValidationErrorType.REPEATED_CONSECUTIVE_POINTS,
                feature, repeatedPointTester.getCoordinate());
        }

        return null;
    }

    protected ValidationError validateGeometriesSimple(Feature feature) {
        // [mmichaud 2012-10-28] isSimple is now able to check geometries of
        // any dimension (with jts 1.12) and even GeometryCollection (jts 1.13)
        Geometry geometry = feature.getGeometry();
        IsSimpleOp simpleOp = new IsSimpleOp(geometry);
        if (!simpleOp.isSimple()) {
            return new ValidationError(ValidationErrorType.NONSIMPLE, feature, simpleOp.getNonSimpleLocation());
        } 
        else return null;
    }

    protected ValidationError validatePolygonOrientation(Feature feature) {
        return recursivelyValidate(feature.getGeometry(), feature,
            new RecursiveValidation() {
                public ValidationError validate(Geometry g, Feature f) {
                    Polygon polygon = (Polygon) g;

                    if (Orientation.isCCW(polygon.getExteriorRing()
                                                      .getCoordinates())) {
                        return new ValidationError(ValidationErrorType.EXTERIOR_RING_CCW,
                            f, polygon);
                    }

                    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                        if (!Orientation.isCCW(polygon.getInteriorRingN(i)
                                                           .getCoordinates())) {
                            return new ValidationError(ValidationErrorType.INTERIOR_RING_CW,
                                f, polygon);
                        }
                    }

                    return null;
                }

                public Class getTargetGeometryClass() {
                    return Polygon.class;
                }
            });
    }

    protected ValidationError validateNoHoles(Feature feature) {
        return recursivelyValidate(feature.getGeometry(), feature,
            new RecursiveValidation() {
                public ValidationError validate(Geometry g, Feature f) {
                    Polygon polygon = (Polygon) g;

                    if (polygon.getNumInteriorRing() > 0) {
                        return new ValidationError(ValidationErrorType.POLYGON_HAS_HOLES,
                            f, polygon.getInteriorRingN(0).getCoordinate());
                    }

                    return null;
                }

                public Class getTargetGeometryClass() {
                    return Polygon.class;
                }
            });
    }

    private ValidationError recursivelyValidate(Geometry geometry,
        Feature feature, RecursiveValidation validation) {
        if (geometry.isEmpty()) {
            return null;
        }

        if (geometry instanceof GeometryCollection) {
            return recursivelyValidateGeometryCollection((GeometryCollection) geometry,
                feature, validation);
        }

        if (!(validation.getTargetGeometryClass().isInstance(geometry))) {
            return null;
        }

        return validation.validate(geometry, feature);
    }

    private ValidationError recursivelyValidateGeometryCollection(
        GeometryCollection gc, Feature feature, RecursiveValidation validation) {
        for (int i = 0; i < gc.getNumGeometries(); i++) {
            ValidationError error = recursivelyValidate(gc.getGeometryN(i),
                    feature, validation);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    protected ValidationError validateMinSegmentLength(Feature feature) {
        List<Coordinate[]> coordArrays =
                CoordinateArrays.toCoordinateArrays(feature.getGeometry(), false);

        for (Coordinate[] coordArray : coordArrays) {
            ValidationError error = validateMinSegmentLength(coordArray, feature);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    protected ValidationError validateMinAngle(Feature feature) {
        List<Coordinate[]> coordArrays =
                CoordinateArrays.toCoordinateArrays(feature.getGeometry(), false);

        for (Coordinate[] coordArray : coordArrays) {
            ValidationError error = validateMinAngle(coordArray, feature);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    protected ValidationError validateMinPolygonArea(Feature feature) {
        return recursivelyValidate(feature.getGeometry(), feature,
            new RecursiveValidation() {
                public ValidationError validate(Geometry g, Feature f) {
                    Polygon polygon = (Polygon) g;

                    if (polygon.getArea() < minPolygonArea) {
                        return new ValidationError(ValidationErrorType.SMALL_AREA,
                            f, polygon);
                    }

                    return null;
                }

                public Class getTargetGeometryClass() {
                    return Polygon.class;
                }
            });
    }

    private ValidationError validateMinSegmentLength(Coordinate[] coordinates,
        Feature feature) {
        if (coordinates.length < 2) {
            return null;
        }

        for (int i = 1; i < coordinates.length; i++) { //Start at 1 [Jon Aquino]

            ValidationError error = validateMinSegmentLength(coordinates[i - 1],
                    coordinates[i], feature);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    private ValidationError validateMinAngle(Coordinate[] coordinates,
        Feature feature) {
        if (coordinates.length < 3) {
            return null;
        }

        boolean closed = coordinates[0].equals(coordinates[coordinates.length -
                1]);

        for (int i = (closed ? 1 : 2); i < coordinates.length; i++) {
            ValidationError error = validateMinAngle((i == 1)
                    ? coordinates[coordinates.length - 2] : coordinates[i - 2],
                    coordinates[i - 1], coordinates[i], feature);

            if (error != null) {
                return error;
            }
        }

        return null;
    }

    private ValidationError validateMinSegmentLength(Coordinate c1,
        Coordinate c2, Feature feature) {
        if (c1.distance(c2) < minSegmentLength) {
            return new ValidationError(ValidationErrorType.SMALL_SEGMENT,
                feature, CoordUtil.average(c1, c2));
        }

        return null;
    }

    private ValidationError validateMinAngle(Coordinate c1, Coordinate c2,
        Coordinate c3, Feature feature) {
        if (Angle.angleBetween(c2, c1, c3) < Angle.toRadians(minAngle)) {
            return new ValidationError(ValidationErrorType.SMALL_ANGLE,
                feature, c2);
        }

        return null;
    }

    /**
     * Used to recurse through GeometryCollections (including GeometryCollection subclasses)
     */
    private interface RecursiveValidation {
        /**
         * @param g the Geometry to validate
         * @param f used when constructing a ValidationError
         * @return a ValidationError if the validation fails; otherwise, null
         */
        ValidationError validate(Geometry g, Feature f);

        /**
         * @return the Geometry class that this RecursiveValidation can validate.
         */
        Class getTargetGeometryClass();
    }
}
