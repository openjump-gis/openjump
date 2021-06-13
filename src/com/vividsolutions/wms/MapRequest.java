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

package com.vividsolutions.wms;

import static org.openjump.util.UriUtil.urlEncode;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openjump.util.UriUtil;

import com.vividsolutions.jump.workbench.Logger;

/**
 * Represents all of the parameters of a getMap request from a WMS server.
 * @author Chris Hodgson chodgson@refractions.net
 */
public class MapRequest extends AbstractWMSRequest{

    private int imgWidth;
    private int imgHeight;
    private List<String> layerNames;
    private BoundingBox bbox;
    private boolean transparent;
    private String format;
    private MapStyle style;
    private String moreParameters;
    
    /**
     * Creates a new MapRequest.
     * @param service the WMService which this MapRequest will use
     */
    public MapRequest(WMService service) {
        super(service);
        imgWidth = 100;
        imgHeight = 100;
        layerNames = new ArrayList<>();
        bbox = service.getCapabilities().getTopLayer().getBoundingBox();
        transparent = false;
        format = null;
    }


    /**
     * Returns the format of this request.
     * This may be a string such as GIF, JPEG, or PNG for a WMS 1.0 server, or
     * a mime-type string in the case of a WMS 1.1 server. It may also be null if
     * the format has not yet been set.
     * @return the string representing the format of this request
     */
    public String getFormat() {
        return format;
    }

    /**
     * Gets the width of the requested image, in pixels.
     * The default image width is 100.
     * @return the width of the requested image
     */
    public int getImageWidth() {
        return imgWidth;
    }

    /**
     * Gets the height of the requested image, in pixels.
     * The default image height is 100.
     * @return the height of the requested image
     */
    public int getImageHeight() {
        return imgHeight;
    }

    /**
     * Returns the list of layers to be requested. Each item in the
     * list should be a String which is the name of a layer.
     * @return the list of layer names to be requested
     */
    public List<String> getLayerNames() {
        return Collections.unmodifiableList(layerNames);
    }

    /**
     * Gets the BoundingBox of the image being requested.
     * @return the BoundingBox of the image being requested
     */
    public BoundingBox getBoundingBox() {
        return bbox;
    }

    /**
     * Gets whether or not a transparent image is being requested.
     * @return true if a transparent image is being requested, false otherwise
     */
    public boolean getTransparent() {
        return transparent;
    }

    /**
     * Sets the format of this request. The format must be a string which is in
     * the list of supported formats as provided by getSupportedFormatList()
     * (not necessarily the same String object, but the same sequence of characters).
     * This will be an unformatted string for a WMS 1.0 server (GIF, JPEG, PNG) or
     * a mime-type string for a WMS 1.1 server (image/gif, image/jpeg, image/png).
     * If the format specified is not in the list, an IllegalArgumentException
     * will be thrown.
     *
     * @param format a format string which is in the list of supported formats
     * @throws IllegalArgumentException if the specified format isn't in the list of supported formats
     * @see MapImageFormatChooser
     *
     */
    public void setFormat( String format ) throws IllegalArgumentException {
        // <<TODO:UNCOMMENT>> Temporarily commented out, until mapserver is fixed [Chris Hodgson]
        // Temporarily removing the requirement that the requested format 
        // be in the list of supported formats, in order to work around a Mapserver bug. 
        // String[] formats = service.getCapabilities().getMapFormats();
        // for( int i = 0; i < formats.length; i++ ) {
        //     if( formats[i].equals( format ) ) {
        this.format = format;
        return;
        // }
        //throw new IllegalArgumentException();
    }

    public void setStyle(MapStyle style) {
        this.style = style;
    }

    public void setMoreParameters(String moreParameters) {
        this.moreParameters = moreParameters;
    }

    /**
     * Sets the width of the image being requested.
     * @param imageWidth the width of the image being requested
     */
    public void setImageWidth( int imageWidth ) {
        this.imgWidth = imageWidth;
    }

    /**
     * Sets the height of the image being requested.
     * @param imageHeight the height of the image being requested
     */
    public void setImageHeight( int imageHeight ) {
        this.imgHeight = imageHeight;
    }

    /**
     * Sets the width and height of the image being requested.
     * @param imageWidth the width of the image being requested
     * @param imageHeight the height of the image being requested
     */
    public void setImageSize( int imageWidth, int imageHeight ) {
        this.imgWidth = imageWidth;
        this.imgHeight = imageHeight;
    }

    /**
     * Sets the layers to be requested. Each item in the list should be a string
     * which corresponds to the name of a layer. The order of the list is
     * important as the layers are rendered in the same order they are listed.
     * @param layerNames an ordered List of the names of layers to be displayed
     */
    public void setLayerNames( List<String> layerNames ) {
        this.layerNames = layerNames;
    }

    /**
     * Sets the BoundingBox of the image being requested.
     * @param bbox the BoundingBox of the image being requested
     */
    public void setBoundingBox( BoundingBox bbox ) {
        this.bbox = bbox;
    }

    /**
     * Sets whether or not to request an image with a transparent background.
     * Requesting a transparent background doesn't guarantee that the resulting
     * image will actually have a transparent background. Not all servers
     * support transparency, and not all formats support transparency.
     * @param transparent true to request a transparent background, false otherwise.
     */
    public void setTransparent( boolean transparent ) {
        this.transparent = transparent;
    }

    /**
     * Returns a String containing the string representations of each item in the
     * list (as provided by toString()), separated by commas.
     * @param list the list to be returned as a coma-separated String
     * @return a comma-separted String of the items in the list
     */
    //[UT] 02.05.2005 made static and public
    public static String listToString( List<String> list ) {
        Iterator<String> it = list.iterator();
        StringBuilder buf = new StringBuilder();
        while( it.hasNext() ) {
            String layer = it.next();
            buf.append( layer );
            if( it.hasNext() ) {
                buf.append( "," );
            }
        }
        return buf.toString();
    }

   /**
    * @return the URL for this request
    * @throws MalformedURLException if there is a problem building the URL for some reason
    */
   public URL getURL() throws MalformedURLException {
       String ver = "REQUEST=map&WMTVER=1.0";
       if ( WMService.WMS_1_1_0.equals( version )){
           ver = "REQUEST=GetMap&SERVICE=WMS&VERSION=1.1.0";
       } else if ( WMService.WMS_1_1_1.equals( version ) ){
           ver = "REQUEST=GetMap&SERVICE=WMS&VERSION=1.1.1";
       } else if ( WMService.WMS_1_3_0.equals( version ) ){
           ver = "REQUEST=GetMap&SERVICE=WMS&VERSION=1.3.0";
       }

       StringBuilder urlBuf = new StringBuilder(UriUtil.urlMakeAppendSafe(service.getCapabilities().getGetMapURL()));
       urlBuf.append(ver + "&WIDTH=" + imgWidth + "&HEIGHT=" + imgHeight);
       urlBuf.append( "&LAYERS=" + urlEncode(listToString( layerNames )) );

       if( transparent ) {
           urlBuf.append( "&TRANSPARENT=TRUE" );
       }
       if( format != null ) {
         urlBuf.append( "&FORMAT=" + urlEncode(format) );
       }
       if( bbox != null ) {
           urlBuf.append( "&" + bbox.getBBox(version));
           if( bbox.getSRS() != null && !bbox.getSRS().equals( "LatLon" ) ) {
               if (version.compareTo(WMService.WMS_1_3_0) < 0) {
                   urlBuf.append( "&SRS=" + bbox.getSRS() );
               } else {
                   urlBuf.append( "&CRS=" + bbox.getSRS() );
               }
           }
       }
       // [UT] some style info is *required*, so add this to be spec conform
       //urlBuf.append( "&STYLES=" );
       if (style == null) urlBuf.append("&STYLES=");
       else {
         urlBuf.append("&STYLES=").append(urlEncode(style.getName()));
       }
       if (moreParameters != null && moreParameters.length()>0) {
         if (!moreParameters.startsWith("&"))
           urlBuf.append("&");
         urlBuf.append(urlEncode(moreParameters));
       }

       Logger.trace(urlBuf.toString());
       return new URL( urlBuf.toString() );
   }

}
