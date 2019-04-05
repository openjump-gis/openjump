package org.openjump.core.ui.plugin.raster;

import com.vividsolutions.jump.I18N;

public class RasterMenuNames {

    /**
     * Many terminologies are used by several plugin. This calss allows to reorganize them
     * TODO create new lang strings
     */

    /**
     * Common to all the raster plugins
     */
    public static String Source_Layer = I18N
            .get("ui.GenericNames.Source-Layer");
    public static String Output_file = I18N
            .get("driver.DriverManager.file-to-save");

    public static String Choose_an_action = I18N
            .get("org.openjump.core.rasterimage.plugin.Choose-an-action");
    public static String Check_field = I18N
            .get("org.openjump.core.rasterimage.plugin.Check_value_into_the_field");
    public static String PROCESSING = I18N
            .get("jump.plugin.edit.NoderPlugIn.processing");

    /**
     * ManageDataPlugIn
     */

    public static String DATA_NAME = "Manage raster data";
    public static String CHANGE_NODATA_TIP = "Change both nodata values and reference number (nodata tag)";
    public static String CHANGE_INTERVAL_TO_NODATA_TIP = "Mask the raster using nodata value. This tool sets a the defined range of values of the raster  to nodata value. The values outside the range will be preserved";
    public static String EXTRACT_INTERVAL_TIP = "Extract a range of data. This tool extracts a defined range of values of the raster. The values outside the range will be set to nodata value";
    public static String EXTRACT_INTERVAL = "Extract a range of values";
    public static String RESET_NODATA_TAG_TIP = "Stretch raster changing only nodata reference value to min or to max value. This tool tries to repair a raster which has partial area coverd by nodata cells and where the tag has been accidentally deleted";
    public static String RESET_NODATA_TAG = "Stretch raster";
    public static String SET_DECIMAL = "Set data decimals";
    public static String SET_DECIMAL_TIP = "This tool allows to reduce/optimize the number of decimal of the data. Set to 0 to have data with integer values";
    public static String RESET_TO_MIN = "Stretch raster to min value";
    public static String RESET_TO_MAX = "Stretch raster to max value";
    public static String SINGLE_BAND_EXIST = "At least one single banded layer should exist";

    /**
     * HistogramPlugIn
     */

    public static String H_CONST_RANGE = "Divide the range with a constant interval";
    public static String H_ANALISYS_INTERVAL = "Define the range of data";
    public static String H_HIST_AS_LINE = "Show histogram as line";
    public static String H_UNIQUE_VALUES = "Continuous histogram /unique values";
    public static String H_AUTO = "Automatic";

    /**
     * CropWarpPlugIn
     */
    public static String C_NAME = "Crop/Warp raster";
    public static String C_TARGET_OBJECT = "Target Object:";
    public static String C_CROP_RASTER = "Crop raster to an envelope";
    public static String C_CROP_RASTER_TIP = "Crop a raster layer using as mask the envelope extension of selected geometries, of mask layer (vector, raster or wms) or of the current view";
    public static String C_WARP_RASTER = "Warp raster to an envelope";
    public static String C_WARP_RASTER_TIP = "Warp a raster layer to the envelope of selected geometries, of mask layer (vector, raster or wms) or of the current view";
    public static String C_CUT_LAYER = "Cutting layer: ";
    public static String C_WARP_LAYER = "Warping layer: ";
    public static String C_NO_INTERSECTION = "Target object doesn't intersect with the raster layer or it is empty (no geometry selected)";

    /**
     * KernelAnalysisPlugIn
     */

    public static String KERNEL_NAME = "Kernel Analysis";
    public static String KERNEL_DEFAULT = "Default kernels";

    /**
     * Vectorize 
     * 
     * 
     */
    public static String errorReadingRaster = "Error on reading raster";
    public static String errorWhileCalculating = "Error while calculating";
    public static String unablePerformance = "Unable to perform the calculation: parameters";
    public static String VectorizeToPolygon = "Create polygons";
    public static String VectorizeToLinestrings = "Create linestrings";
    public static String VectorizeToContours = "Create contours";
    public static String ExplodeMultipolygons = "Explode multipolygons";
    public static String ApplyStyle = "Apply a random color style to output";
    public static String contour_baseContour = "Base contour";
    public static String contour_distanceContours = "Distance contours";
    public static String contour_zeroElevation = "Zero elevation";
    public static String contour_minContour = "min contour";
    public static String contour_maxcontour = "max contour";
    public static String contour_contourNumber = "contour number";
    public static String contour_properties = "contours properties";
    public static String contour_range = "Renge elevation";
    public static String Value = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterQueryPlugIn.value");
}
