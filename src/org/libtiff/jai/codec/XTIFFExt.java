package org.libtiff.jai.codec;

public class XTIFFExt extends XTIFF {

    // Private TIFF Tags
    // (https://www.awaresystems.be/imaging/tiff/tifftags/private.html)
    // Added
    // a) to manage nodata and possible Metadata (currently managed by external
    // xml file
    // b) to keep name compatibility for geotiff tags and have a description

    /**
     * Name GDAL_NODATA
     * 
     * @Type ASCII
     * @Count N
     * @Description Used by the GDAL library, contains an ASCII encoded nodata
     *              or background pixel value.
     * 
     *              In the geospatial image processing field especially (and in
     *              other fields) it is common to use a special pixel value to
     *              mark geospatial areas for which no information is available.
     *              This is often called a "nodata" or "background" value.
     *              Applications often treat these pixels as transparent and
     *              they are often not included in spatial statistics for the
     *              image. Non-geospatial applications might still use the
     *              nodata value to track a special value that should be treated
     *              as transparent since currently TIFF palettes don't include
     *              an alpha value.
     * 
     *              The GDAL_NODATA tag is intended to keep track of what pixel
     *              value is being used for this background nodata value. It is
     *              ASCII encoded so that if the pixel value 255 was to be
     *              treated as nodata, then the tag would have the value "255".
     * 
     *              If this tag is absent there is assume to be no nodata value
     *              in effect for the image. If the image has more than one
     *              sample it is assumed that all samples have the same nodata
     *              value.
     * 
     *              This tag is currently only supported by the GDAL library.
     */
    public static final int GDAL_NODATA = 42113;
    /**
     * Name GDAL_METADATA
     * 
     * @Type ASCII
     * @Count N
     * @Description Used by the GDAL library, holds an XML list of name=value
     *              'metadata' values about the image as a whole, and about
     *              specific samples.
     * 
     *              Contains an XML fragment that looks like this example:
     * 
     *              <GDALMetadata> <Item name="TITLE">BV02021.CA NASA-FAO</Item>
     *              <Item name="IMAGETYPE">13, ARTEMIS NEWNASA</Item> <Item
     *              name="UNITS" sample="0">Meters (elevation)</Item> <Item
     *              name="OFFSET" sample="0" role="offset">0</Item> <Item
     *              name="SCALE" sample="0" role="scale">0.003</Item>
     *              </GDALMetadata>
     * 
     *              The <Item> names are the name of a metadata keyword, and the
     *              text contents of the Item are it's value. Normal XML
     *              escaping applies. The GDALMetadata tag may have zero or more
     *              Item sub-elements. Generally speaking the item names should
     *              be well behaved tokens (alpha-numeric + underscores) though
     *              that isn't strictly enforced. Also, normally the names are
     *              assumed to be unique.
     * 
     *              The "sample" attribute on an Item can be used to indicate a
     *              metadata item that applies only to one sample in a
     *              multi-sample TIFF image. Sample numbering starts from 0.
     * 
     *              The "role" attribute can be used to identify a specific
     *              semantic with a metadata item. GDAL has several specific
     *              pieces of metadata with particular interpretations and these
     *              are identified with role attributes. At a future date some
     *              of these specific roles may be documented here.
     * 
     *              This is an unregistered tag used only by the GDAL library
     *              and applications built on it to hold Metadata about
     *              geospatial datasets.
     */

    public static final int GDAL_METADATA = 42112;
    /**
     * Name ModelPixelScaleTag
     * 
     * @Type DOUBLE
     * @Count 3
     * @Description Used in interchangeable GeoTIFF files. This tag is
     *              optionally provided for defining exact affine
     *              transformations between raster and model space. Baseline
     *              GeoTIFF files may use this tag or ModelTransformationTag,
     *              but shall never use both within the same TIFF image
     *              directory. This tag may be used to specify the size of
     *              raster pixel spacing in the model space units, when the
     *              raster space can be embedded in the model space coordinate
     *              system without rotation, and consists of the following 3
     *              values:
     * 
     *              ModelPixelScaleTag = (ScaleX, ScaleY, ScaleZ)
     * 
     *              where ScaleX and ScaleY give the horizontal and vertical
     *              spacing of raster pixels. The ScaleZ is primarily used to
     *              map the pixel value of a digital elevation model into the
     *              correct Z-scale, and so for most other purposes this value
     *              should be zero (since most model spaces are 2-D, with Z=0).
     */
    public static final int ModelPixelScaleTag = TIFFTAG_GEO_PIXEL_SCALE;
    /**
     * Name ModelTiepointTag
     * 
     * @Type DOUBLE
     * @Count N = 6*K, with K = number of tiepoints
     * @Description Originally part of Intergraph's GeoTIFF tags, but now used
     *              in interchangeable GeoTIFF files. This tag is also known as
     *              'GeoreferenceTag'. This tag stores raster->model tiepoint
     *              pairs in the order ModelTiepointTag = (...,I,J,K, X,Y,Z...)
     *              where (I,J,K) is the point at location (I,J) in raster space
     *              with pixel-value K, and (X,Y,Z) is a vector in model space.
     *              In most cases the model space is only two-dimensional, in
     *              which case both K and Z should be set to zero; this third
     *              dimension is provided in anticipation of future support for
     *              3D digital elevation models and vertical coordinate systems.
     */
    public static final int ModelTiepointTag = TIFFTAG_GEO_TIEPOINTS;
    /**
     * Name ModelTransformationTag
     * 
     * @Type DOUBLE
     * @Count N = 16
     * @Description Used in interchangeable GeoTIFF files.
     * 
     *              This tag is optionally provided for defining exact affine
     *              transformations between raster and model space. Baseline
     *              GeoTIFF files may use this tag or ModelPixelScaleTag, but
     *              shall never use both within the same TIFF image directory.
     * 
     *              This tag may be used to specify the transformation matrix
     *              between the raster space (and its dependent pixel-value
     *              space) and the (possibly 3D) model space.
     */

    public static final int ModelTransformationTag = TIFFTAG_GEO_TRANS_MATRIX;
    /**
     * Name GeoKeyDirectoryTag
     * 
     * @Type SHORT
     * @Count N >= 4
     * @Description Used in interchangeable GeoTIFF files.
     * 
     *              This tag is also know as 'ProjectionInfoTag' and
     *              'CoordSystemInfoTag'
     * 
     *              This tag may be used to store the GeoKey Directory, which
     *              defines and references the "GeoKeys".
     */
    public static final int GeoKeyDirectoryTag = TIFFTAG_GEO_KEY_DIRECTORY;
    /**
     * Name GeoDoubleParamsTag
     * 
     * @Type DOUBLE
     * @Count N
     * @Description Used in interchangeable GeoTIFF files.
     * 
     *              This tag is used to store all of the DOUBLE valued GeoKeys,
     *              referenced by the GeoKeyDirectoryTag. The meaning of any
     *              value of this double array is determined from the
     *              GeoKeyDirectoryTag reference pointing to it. FLOAT values
     *              should first be converted to DOUBLE and stored here.
     */
    public static final int GeoDoubleParamsTag = TIFFTAG_GEO_DOUBLE_PARAMS;
    /**
     * Name GeoAsciiParamsTag
     * 
     * @Type ASCII
     * @Count N
     * @Description Used in interchangeable GeoTIFF files.
     * 
     *              This tag is used to store all of the ASCII valued GeoKeys,
     *              referenced by the GeoKeyDirectoryTag. Since keys use offsets
     *              into tags, any special comments may be placed at the
     *              beginning of this tag. For the most part, the only keys that
     *              are ASCII valued are "Citation" keys, giving documentation
     *              and references for obscure projections, datums, etc.
     */
    public static final int GeoAsciiParamsTag = TIFFTAG_GEO_ASCII_PARAMS;
}