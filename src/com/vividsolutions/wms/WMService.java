



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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Represents a remote WMS Service.
 *
 * @author Chris Hodgson chodgson@refractions.net
 */
public class WMService {
    
    public static final String WMS_1_0_0 = "1.0.0";

    public static final String WMS_1_1_0 = "1.1.0";
    
    public static final String WMS_1_1_1 = "1.1.1";
    
    
  private String serverUrl;
  private String wmsVersion = WMS_1_0_0;
  private Capabilities cap;
  
  /**
   * Constructs a WMService object from a server URL.
   * @param serverUrl the URL of the WMS server
   */
  public WMService( String serverUrl, String wmsVersion ) {
    this.serverUrl = serverUrl;   
    this.wmsVersion = wmsVersion;
    this.cap = null;
  }
  /**
   * Constructs a WMService object from a server URL.
   * @param serverUrl the URL of the WMS server
   */
  public WMService( String serverUrl ) {
    this.serverUrl = serverUrl;   
    this.cap = null;
  }

  /**
   * Connect to the service and get the capabilities.
   * This must be called before anything else is done with this service.
   */
	public void initialize() throws IOException {
//    [UT]
	    String req = "request=capabilities&WMTVER=1.0";
	    if( WMS_1_1_0.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.1.0&REQUEST=GetCapabilities";
	    } else if ( WMS_1_1_1.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetCapabilities";
	    }
	    
	    String requestUrlString = this.serverUrl + req;
	    URL requestUrl = new URL( requestUrlString );
	    InputStream inStream = requestUrl.openStream();
	    Parser p = new Parser();
	    cap = p.parseCapabilities( this, inStream );
  }


  /**
   * Gets the url of the map service.
   * @return the url of the WMService
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * Gets the title of the map service.
   * The service must have previously been initialized, otherwise null is returned.
   * @return the title of the WMService
   */
  public String getTitle() {
    return cap.getTitle();
  }

  /**
   * Gets the Capabilities for this service.
   * The service must have previously been initialized, otherwise null is returned.
   * @return a copy of the MapDescriptor for this service
   */
  public Capabilities getCapabilities() {
    return cap;
  }

  /**
   * Creates a new MapRequest object which can be used to retrieve a Map
   * from this service.
   * @return a MapRequest object which can be used to retrieve a map image
   *         from this service
   */
  	public MapRequest createMapRequest() {
//    [UT] 04.02.2005 changed
  	    MapRequest mr = new MapRequest( this );
  	    mr.setVersion( this.wmsVersion );
        return mr;
	}
      
  	public String getVersion(){
  	    return wmsVersion;
	}
}
