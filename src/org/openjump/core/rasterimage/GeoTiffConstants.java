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

    //Here are all of the TIFF tags (and their owners) that are used to store GeoTIFF information of any type. It is very unlikely that any other tags will be necessary in the future (since most additional information will be encoded as a GeoKey).

    public final static int    ModelPixelScaleTag     = 33550 ,//(SoftDesk)
                                ModelTransformationTag = 34264 ,//(JPL Carto Group)
                                ModelTiepointTag       = 33922 ,//(Intergraph)
                                GeoKeyDirectoryTag     = 34735 ,//(SPOT)
                                GeoDoubleParamsTag     = 34736 ,//(SPOT)
                                GeoAsciiParamsTag      = 34737 ,//(SPOT)
                              //Obsoleted Implementation:                             
                                IntergraphMatrixTag = 33920 ;//(Intergraph) -- Use ModelTransformationTag.
  

}
