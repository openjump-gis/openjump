




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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.openjump.util.UriUtil;

/**
 * Represents the capabilities WMS XML.
 * @author Chris Hodgson chodgson@refractions.net
 */
public class Capabilities {

  private MapLayer topLayer;
  private String title;
  private ArrayList mapFormats;
  private WMService service;
  private String getMapURL, featureInfoURL;
  private ArrayList infoFormats;
  
  /** 
   * Creates a new WMS Capabilities object. Should generally only be used by the Parser.
   * @param service the WMService to which these Capabilites belong
   * @param title the title of this WMService
   * @param topLayer the top MapLayer of the entire layer tree
   * @param mapFormats the Collection of supported formats 
   */  
  public Capabilities(WMService service, String title, MapLayer topLayer,
          Collection mapFormats, Collection infoFormats) {
    this.service = service;
    this.title = title;
    this.topLayer = topLayer;
    this.mapFormats = new ArrayList( mapFormats );
    this.infoFormats = new ArrayList(infoFormats);
    this.getMapURL = service.getServerUrl();
    this.featureInfoURL = service.getServerUrl();
  }
  
  public Capabilities(WMService service, String title, MapLayer topLayer,
          Collection mapFormats, Collection infoFormats, String getMapURL, String featureInfoURL) {
      this(service, title, topLayer, mapFormats, infoFormats);
      this.getMapURL = getMapURL;
      this.featureInfoURL = featureInfoURL;
    }

  /**
   * Gets a reference to the service which these Capabilities describe.
   * @return the WMService which these Capabilities describe
   */
  public WMService getService() {
    return service;
  }
  
  /**
   * Gets the top layer for these Capabilities.
   * @return the top MapLayer for these Capabilities
   */
  public MapLayer getTopLayer() {
    return topLayer;
  }
  
  /**
   * Get a MapLayer by name
   */
   public MapLayer getMapLayerByName(String name) {
       return getMapLayerByName(topLayer, name);
   }
   
   private MapLayer getMapLayerByName(MapLayer mapLayer, String name) {
       String mapName = mapLayer.getName();
       // name is not mandatory for layers which are just containers for sublayers 
       if (mapName != null && mapName.equals(name)) return mapLayer;
       for (MapLayer layer : mapLayer.getSubLayerList()) {
           MapLayer r = getMapLayerByName(layer, name);
           if (r != null) return r;
       }
       return null;
   }
  
  /**
   * Gets the title of the Capabilities.
   * @return the title of the map described by these Capabilities
   */
  public String getTitle() {
    return this.title;
  }
  
  public String getGetMapURL() {
      return getMapURL;
  }
  
  public String getFeatureInfoURL() {
    String serviceUrl = service.getServerUrl();
    // reuse servers auth if there is none in the url
    // already and the server is the same
    if (UriUtil.isURL(featureInfoURL) && UriUtil.urlGetUser(featureInfoURL).isEmpty()
        && UriUtil.urlGetHost(featureInfoURL).equals(
            UriUtil.urlGetHost(serviceUrl)))
      return UriUtil.urlAddUserInfo(featureInfoURL, service.getServerUrlAsUrl()
          .getUserInfo());
    
    // try serverurl if featinfo url is empty
    return UriUtil.isURL(featureInfoURL) ? featureInfoURL : serviceUrl;
  }
  
  public void setGetMapURL(String url) {
      getMapURL = url;
  }
  
  /**
   * Gets a copy of the list of formats supported by this getMap requests for this map.
   * @return an array containing the formats supported by getMap requests for this map
   */
  public String[] getMapFormats() {
    String[] formats = new String[mapFormats.size()];
    Iterator it = mapFormats.iterator();
    int i = 0;
    while( it.hasNext() ) {
      formats[i++] = (String)it.next();
    }
    return formats;
  }
  
    public String getInfoFormat() {
        String format = "text/plain";
        if (!infoFormats.contains(format)) {
            Iterator it = infoFormats.iterator();
            if (it.hasNext()) {
                format = (String) it.next();
            }
        }
        return format;
    }

}
