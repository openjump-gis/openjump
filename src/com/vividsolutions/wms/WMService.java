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

import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JOptionPane;

import net.iharder.Base64;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;

/**
 * Represents a remote WMS Service.
 *
 * @author Chris Hodgson chodgson@refractions.net
 */
public class WMService {
    
    public static final String WMS_1_0_0 = "1.0.0";

    public static final String WMS_1_1_0 = "1.1.0";
    
    public static final String WMS_1_1_1 = "1.1.1";
    
    public static final String WMS_1_3_0 = "1.3.0";
    
    
  private String serverUrl;
  private String wmsVersion = WMS_1_1_1;
  private Capabilities cap;
  // timeouts in ms
  private int TIMEOUT_OPEN = 5000;
  private int TIMEOUT_READ = TIMEOUT_OPEN;
  
  /**
   * Constructs a WMService object from a server URL.
   * @param serverUrl the URL of the WMS server
   * @param wmsVersion 
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
    * @throws IOException
    */
    public void initialize() throws IOException {
        initialize(false);
    }
  
  /**
   * Connect to the service and get the capabilities.
   * This must be called before anything else is done with this service.
   * @param alertDifferingURL alert the user if a different GetMap URL is available
   * @throws IOException 
   */
	public void initialize(boolean alertDifferingURL) throws IOException {
	    // [UT]
	    String req = "request=capabilities&WMTVER=1.0";
	    IParser parser = new ParserWMS1_1();
	    if( WMS_1_0_0.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.0.0&REQUEST=GetCapabilities";
	    	parser = new ParserWMS1_0();
	    } else if( WMS_1_1_0.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.1.0&REQUEST=GetCapabilities";
	    	parser = new ParserWMS1_1();
	    } else if ( WMS_1_1_1.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetCapabilities";
	    	parser = new ParserWMS1_1();
	    } else if ( WMS_1_3_0.equals( wmsVersion) ){
	    	req = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities";
	    	parser = new ParserWMS1_3();
	    }
        
        try {
            String requestUrlString = this.serverUrl + req;
            URL requestUrl = new URL( requestUrlString );
            URLConnection con = requestUrl.openConnection();
            con.setConnectTimeout(TIMEOUT_OPEN);
            con.setReadTimeout(TIMEOUT_READ);
            if(requestUrl.getUserInfo() != null) {
                con.setRequestProperty("Authorization", "Basic " +
                        Base64.encodeBytes(requestUrl.getUserInfo().getBytes()));
                con.setRequestProperty("Host", requestUrl.getHost());
            }
            //Parser p = new Parser();
            cap = parser.parseCapabilities( this, con.getInputStream() );
            String url1 = cap.getService().getServerUrl();
            String url2 = cap.getGetMapURL();
            if(!url1.equals(url2)){
                //if the difference is only in credentials then use url1 else ask from user
                if(!new URL(url1).equals(new URL(url2)) && alertDifferingURL) {
                    int resp = showConfirmDialog(null, I18N.getMessage("com.vididsolutions.wms.WMService.Other-GetMap-URL-Found",
                            new Object[]{url2}), null, YES_NO_OPTION);
                    if(resp == NO_OPTION) {
                        cap.setGetMapURL(url1);
                    }
                } else {
                    //changed 24.06.2011 (Wilfried Hornburg, LGLN) url1 --> url2; original: cap.setGetMapURL(url1);
                    //revert to url1, following Jukka's advice a discussion is on-going on JPP mailing list
                    cap.setGetMapURL(url1);
                }
            }
        } catch ( FileNotFoundException e ){
            JOptionPane.showMessageDialog( null, I18N.getMessage( "com.vividsolutions.wms.WMService.WMS-Not-Found",
                                                                  new Object[] { e.getLocalizedMessage() } ),
                                           I18N.get( "com.vividsolutions.wms.WMService.Error" ),
                                           JOptionPane.ERROR_MESSAGE );
            throw e;
        } catch (final WMSException e){
            ErrorDialog.show(null, "WMS Error", e.getMessage(), e.getSource());
            throw e;
        } catch ( IOException e ) {
            JOptionPane.showMessageDialog( null, I18N.getMessage( "com.vividsolutions.wms.WMService.WMS-IO-Error",
                                                                  new Object[] { e.getClass().getSimpleName(), e.getLocalizedMessage() } ),
                                           I18N.get( "com.vividsolutions.wms.WMService.Error" ),
                                           JOptionPane.ERROR_MESSAGE );
            throw e;
        }
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
  	    // [UT] 04.02.2005 changed
  	    MapRequest mr = new MapRequest( this );
  	    mr.setVersion( this.wmsVersion );
        return mr;
	}
      
  	public String getVersion(){
  	    return wmsVersion;
	}
}
