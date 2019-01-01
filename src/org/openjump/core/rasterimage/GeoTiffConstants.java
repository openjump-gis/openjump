/*
 * Created on 11.04.2006 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2434 $
 *  $Date: 2006-09-12 12:31:50 +0200 (Di, 12 Sep 2006) $
 *  $Id: GeoTiffConstants.java 2434 2006-09-12 10:31:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.rasterimage;

/**
 * TODO: comment class
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2006),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2434 $
 * 
 */
public class GeoTiffConstants {

    // Here are all of the TIFF tags (and their owners) that are used to store GeoTIFF information of any type.
    // It is very unlikely that any other tags will be necessary in the future
    // (since most additional information will be encoded as a GeoKey).
    // GeoKeys are taken from http://gis.ess.washington.edu/data/raster/drg/docs/geotiff.txt

    public final static int
            ModelPixelScaleTag     = 33550 ,//(SoftDesk)
            ModelTransformationTag = 34264 ,//(JPL Carto Group)
            ModelTiepointTag       = 33922 ,//(Intergraph)
            GeoKeyDirectoryTag     = 34735 ,//(SPOT)
            GeoDoubleParamsTag     = 34736 ,//(SPOT)
            GeoAsciiParamsTag      = 34737 ,//(SPOT)
            //Obsoleted Implementation:
            IntergraphMatrixTag    = 33920; //(Intergraph) -- Use ModelTransformationTag.


    public final static int UNDEFINED      = 0;

    public final static int USERDEFINED    = 32767;

    public final static int ModelTypeProjected = 1, ModelTypeGeographic = 2, ModelTypeGeocentric = 3;

    public final static int RasterPixelIsArea  = 1, RasterPixelIsPoint = 2;

    public final static int
            Linear_Meter = 9001,
            Linear_Foot = 9002,
            Linear_Foot_US_Survey = 9003,
            Linear_Foot_Modified_American = 9004,
            Linear_Foot_Clarke = 9005,
            Linear_Foot_Indian = 9006,
            Linear_Link = 9007,
            Linear_Link_Benoit = 9008,
            Linear_Link_Sears = 9009,
            Linear_Chain_Benoit = 9010,
            Linear_Chain_Sears = 9011,
            Linear_Yard_Sears = 9012,
            Linear_Yard_Indian = 9013,
            Linear_Fathom = 9014,
            Linear_Mile_International_Nautical = 9015;

    public final static int
            Angular_Radian = 9101,
            Angular_Degree = 9102,
            Angular_Arc_Minute = 9103,
            Angular_Arc_Second = 9104,
            Angular_Grad = 9105,
            Angular_Gon = 9106,
            Angular_DMS = 9107,
            Angular_DMS_Hemisphere = 9108;

    // GeoKeys taken from http://gis.ess.washington.edu/data/raster/drg/docs/geotiff.txt
    public final static int
            GTModelTypeGeoKey              = 1024, /* Section 6.3.1.1 Codes  */
            GTRasterTypeGeoKey             = 1025, /* Section 6.3.1.2 Codes  */
            GTCitationGeoKey               = 1026, /* documentation */

            GeographicTypeGeoKey           = 2048, /* Section 6.3.2.1 Codes     */
            GeogCitationGeoKey             = 2049, /* documentation             */
            GeogGeodeticDatumGeoKey        = 2050, /* Section 6.3.2.2 Codes     */
            GeogPrimeMeridianGeoKey        = 2051, /* Section 6.3.2.4 codes     */
            GeogLinearUnitsGeoKey          = 2052, /* Section 6.3.1.3 Codes     */
            GeogLinearUnitSizeGeoKey       = 2053, /* meters                    */
            GeogAngularUnitsGeoKey         = 2054, /* Section 6.3.1.4 Codes     */

            GeogAngularUnitSizeGeoKey      = 2055, /* radians                   */
            GeogEllipsoidGeoKey            = 2056, /* Section 6.3.2.3 Codes     */
            GeogSemiMajorAxisGeoKey        = 2057, /* GeogLinearUnits           */
            GeogSemiMinorAxisGeoKey        = 2058, /* GeogLinearUnits           */
            GeogInvFlatteningGeoKey        = 2059, /* ratio                     */
            GeogAzimuthUnitsGeoKey         = 2060, /* Section 6.3.1.4 Codes     */
            GeogPrimeMeridianLongGeoKey    = 2061, /* GeogAngularUnit           */

            ProjectedCSTypeGeoKey          = 3072,  /* Section 6.3.3.1 codes   */
            PCSCitationGeoKey              = 3073,  /* documentation           */
            ProjectionGeoKey               = 3074,  /* Section 6.3.3.2 codes   */
            ProjCoordTransGeoKey           = 3075,  /* Section 6.3.3.3 codes   */
            ProjLinearUnitsGeoKey          = 3076,  /* Section 6.3.1.3 codes   */
            ProjLinearUnitSizeGeoKey       = 3077,  /* meters                  */
            ProjStdParallelGeoKey          = 3078,  /* GeogAngularUnit */
            ProjStdParallel2GeoKey         = 3079,  /* GeogAngularUnit */
            ProjOriginLongGeoKey           = 3080,  /* GeogAngularUnit */
            ProjOriginLatGeoKey            = 3081,  /* GeogAngularUnit */
            ProjFalseEastingGeoKey         = 3082,  /* ProjLinearUnits */
            ProjFalseNorthingGeoKey        = 3083,  /* ProjLinearUnits */
            ProjFalseOriginLongGeoKey      = 3084,  /* GeogAngularUnit */
            ProjFalseOriginLatGeoKey       = 3085,  /* GeogAngularUnit */
            ProjFalseOriginEastingGeoKey   = 3086,  /* ProjLinearUnits */
            ProjFalseOriginNorthingGeoKey  = 3087,  /* ProjLinearUnits */
            ProjCenterLongGeoKey           = 3088,  /* GeogAngularUnit */
            ProjCenterLatGeoKey            = 3089,  /* GeogAngularUnit */
            ProjCenterEastingGeoKey        = 3090,  /* ProjLinearUnits */
            ProjCenterNorthingGeoKey       = 3091,  /* ProjLinearUnits */
            ProjScaleAtOriginGeoKey        = 3092,  /* ratio   */
            ProjScaleAtCenterGeoKey        = 3093,  /* ratio   */
            ProjAzimuthAngleGeoKey         = 3094,  /* GeogAzimuthUnit */
            ProjStraightVertPoleLongGeoKey = 3095,  /* GeogAngularUnit */

            VerticalCSTypeGeoKey           = 4096,   /* Section 6.3.4.1 codes   */
            VerticalCitationGeoKey         = 4097,   /* documentation */
            VerticalDatumGeoKey            = 4098,   /* Section 6.3.4.2 codes   */
            VerticalUnitsGeoKey            = 4099;   /* Section 6.3.1.3 codes   */

}
