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

// Changed by Uwe Dalluege, uwe.dalluege@rzcn.haw-hamburg.de
// to differ between LatLonBoundingBox and BoundingBox
// 2005-07-29

package com.vividsolutions.wms;

import java.util.*;

import org.locationtech.jts.geom.Envelope;

/**
 * Represents a WMS Layer.
 *
 * @author Chris Hodgson chodgson@refractions.net
 * @author Uwe Dalluege, uwe.dalluege@rzcn.haw-hamburg.de
 * @author Michael Michaud m.michael.michaud@orange.fr
 */
public class MapLayer {

    // immutable members
    private MapLayer parent;
    private final String name;
    private final String title;
    private final List<String> srsList;
    // Default bounding box in geographic coordinates
    private final BoundingBox bbox;
    private final List<BoundingBox> boundingBoxList;
  
    // user modifiable members
    private List<MapLayer> subLayers;
    private List<MapStyle> styles;
    //private boolean enabled = false; // what is it for ? currently unused

    
    /**
     * Creates a new instance of MapLayer 
     */
    public MapLayer(String name, String title, Collection<String> srsList,
            Collection<MapLayer> subLayers, BoundingBox bbox, List<MapStyle> styles) {
        this.parent = null;
        this.name = name;
        this.title = title;
        this.srsList = new ArrayList<>( srsList );
        setStyles(styles); // includes some kind of initialization
        this.subLayers = new ArrayList<>( subLayers );
        for (MapLayer subLayer : subLayers) {
            subLayer.parent = this;
        }
        this.bbox = bbox;
        this.boundingBoxList = new ArrayList<>();
    }

    /**
     * Creates a new instance of MapLayer with boundingBoxList [uwe dalluege]
     */
    public MapLayer( String name, String title, Collection<String> srsList,
                     Collection<MapLayer> subLayers, BoundingBox bbox,
                     List<BoundingBox> boundingBoxList,List<MapStyle> styles ) {
  	    this ( name, title, srsList, subLayers, bbox, styles );
  	    this.boundingBoxList.addAll(boundingBoxList);
    }
  
  
    /**
     * @return All BoundingBoxes
     * If there is no BoundingBox for this MapLayer the parent-BoundingBox
     * will be taken.
     * [uwe dalluege]
     */  
    public List<BoundingBox> getAllBoundingBoxList ( ) {
        List<BoundingBox> boundingBoxes = getBoundingBoxList();
    	if ((boundingBoxes == null || boundingBoxes.size() == 0) && getParent() != null) {
    	    return getParent().getAllBoundingBoxList();
        } else {
    	    return boundingBoxes == null ? new ArrayList<>() : boundingBoxes;
        }
    }

  
    ///**
    // * Returns the number of direct sub-layers in this MapLayer.
    // * @return the number of direct sub-layers in this MapLayer
    // */
    //public int numSubLayers() {
    //    return subLayers.size();
    //}
  
    /**
     * Returns the sub-layer at the specified index.
     * @param n the index of the sub-layer to return
     * @return the MapLayer sub-layer at the specified index
     */
    public MapLayer getSubLayer( int n ) {
        return subLayers.get( n );
    }

    /**
     * Gets a copy of the list of the sublayers of this layer.
     * @return a copy of the Arraylist containing all the sub-layers of this layer
     */
    public List<MapLayer> getSubLayerList() {
        return new ArrayList<>(subLayers);
    }

    /**
     * Returns a list of all the layers in order of a root-left-right traversal of
     * the layer tree.
     * @return a list of all the layers in order of a root-left-right traversal of
     * the layer tree.
     */
    public List<MapLayer> getLayerList() {
        List<MapLayer> list = new ArrayList<>();
        list.add( this );
        for (MapLayer mapLayer : subLayers) {
            list.addAll(mapLayer.getLayerList());
        }
        return list;
    }

    /**
     * Gets the title of this MapLayer.
     * The title of a layer should be used for display purposes.
     * @return the title of this Layer
     */
    public String getTitle() {
        return title;
    }
  
    /**
     * Gets the name of this Layer.
     * The name of a layer is its 'back-end', ugly name, which generally 
     * shouldn't need to be used by others but is available anyway.
     * Layers which do not have any data associated with them, such as container
     * or grouping layers, might not have a name, in which case null will be
     * returned.
     * @return the name of the layer, or null if it doesn't have a name
     */
    public String getName() {
        return name;
    }
  
    /**
     * Gets the parent MapLayer of this MapLayer. 
     * @return the parent layer of this MapLayer, or null if the layer has no parent.
     */
    public MapLayer getParent() {
        return parent;
    }
  
    /**
     * Gets the LatLonBoundingBox for this layer.
     * If this layer doesn't have a LatLonBoundingBox specified, we recursively
     * ask the parent layer for its bounding box. The WMS spec says that each
     * layer should either have its own LatLonBoundingBox, or inherit one from
     * its parent, so this recursive call should be successful. If not, null is
     * returned. However, if a bounding box is returned, it will have the 
     * SRS string "LatLon". 
     * Note that the BoundingBox is not necessarily "tight".
     * @return the BoundingBox for this layer, or null if the BBox is unknown
     */
    public BoundingBox getBoundingBox() {
        if( bbox != null ) {
            return bbox;
        } 
        if( parent != null ) {
            return parent.getBoundingBox();
        }
        return null;
    }
  
    /**
     * Return the bounding box defined for this MapLayer in this SRS.
     * If not found, the bounding box is searched in this Layer's children,
     * then in its parents.
     * If not found, return the whole earth in LonLat SRS.
     */
    public BoundingBox getBoundingBox(String srs) {
        Envelope envelope = getBoundingBox(srs, this, new Envelope());
        MapLayer p = this;
        while (envelope.getMinX() > envelope.getMaxX() && p.getParent() != null) {
            p = p.getParent();
            for (BoundingBox bb : p.getBoundingBoxList()) {
                if (srs.equals(bb.getSRS())) {
                    // if this layer has a bounding box for this srs, return its envelope
                    envelope.expandToInclude(bb.getEnvelope());
                    return new BoundingBox(srs, envelope);
                }
            }
        }
        return new BoundingBox(srs, envelope);
    }
    
    /**
     * Return the envelope of this layer in the wished srs if a BoundingBox in
     * this srs exists. Else if, layer's children are scanned recursively.
     */
    public static Envelope getBoundingBox(String srs, MapLayer lyr, Envelope env) {
        for (BoundingBox bb : lyr.getBoundingBoxList()) {
            if (srs.equals(bb.getSRS())) {
                // if this layer has a bounding box for this srs, return its envelope
                env.expandToInclude(bb.getEnvelope());
                return env;
            }
        }
        // else include all the envelope of this layer's children
        for (MapLayer child : lyr.getSubLayerList()) {
            env.expandToInclude(getBoundingBox(srs, child, env));
        }
        return env;
    }
  
   
    /**
     * I think this name is better [uwe dalluege]
     * Gets the LatLonBoundingBox for this layer.
     * If this layer doesn't have a LatLonBoundingBox specified, we recursively
     * ask the parent layer for its bounding box. The WMS spec says that each
     * layer should either have its own LatLonBoundingBox, or inherit one from
     * its parent, so this recursive call should be successful. If not, null is
     * returned. However, if a bounding box is returned, it will have the 
     * SRS string "LatLon". 
     * Note that the BoundingBox is not necessarily "tight".
     * @return the BoundingBox for this layer, or null if the BBox is unknown
     */
    public BoundingBox getLatLonBoundingBox() {
        if( bbox != null ) {
            return bbox;
        } 
        if( parent != null ) {
            return parent.getBoundingBox();
        }
        return null;
    }
  
  
    /**
     * Gets the BoundingBoxList for this Layer
     * @return the BoundingBoxList containing the BoundingBoxes
     */
    public List<BoundingBox> getBoundingBoxList ( ) {// [uwe dalluege]
  	    return new ArrayList<>(boundingBoxList);
    }

  
    /**
     * Returns a copy of the list of supported SRS's. Each SRS is a string in the
     * format described by the WMS specification, such as "EPSG:1234".
     * @return a copy of the list of supported SRS's
     */
    public List<String> getSRSList() {
      return new ArrayList<>(srsList);
    }
    

    /**
     * @return a list of the SRS list of this MapLayer and its ancestors
     */
    public Collection<String> getFullSRSList() {
        // Change TreeSet to LinkedHashSet in order to preserve the natural order
        // with layer SRS first ans parent SRS second
        Set<String> fullSRSSet  = new LinkedHashSet<>(getSRSList());
        if (parent != null) fullSRSSet.addAll(parent.getFullSRSList());

        // refinement : if the layer has a non empty boundingBoxList use the srs
        // of the first bbox as it maybe the native one
        List<String> fullSRSList = new LinkedList<>(fullSRSSet);
        if (!boundingBoxList.isEmpty()) {
            String firstBBoxSrs = boundingBoxList.get(0).getSRS();
            if (fullSRSList.contains(firstBBoxSrs)) {
                fullSRSList.remove(firstBBoxSrs);
                fullSRSList.add(0, firstBBoxSrs);
            }
        }
        return fullSRSList;
    }
    
    /**
     * Sets the selected WMS layer style
     * 
     * @param selectedStyle set the WMS Style for this MapLayer
     */
    public void setSelectedStyle( MapStyle selectedStyle ) {
        for (MapStyle style : styles) {
            style.setSelected(false, false);
        }
        selectedStyle.setSelected(true, false);
    }

    /**
     * Gets the WMS layer style by name
     * 
     * @param styleName name of the WMS layer style
     * @return a MapStyle associated with this Layer
     */
    public MapStyle getStyle( String styleName ) {
        for (MapStyle style : styles) {
            if (style.getName().equals(styleName))
                return style;
        }
        return null;
    }
    
    public List<MapStyle> getStyles() {
        return styles;
    }
  
    /** 
     * @param sublayer WMS subLayers contained in this MapLayer
     */
    public void setSublayer(ArrayList<MapLayer> sublayer) {
        this.subLayers = sublayer;
    }
    
    /**
     * @param newStyles WMS MapStyles to be associated to this MapLayer
     */
    public void setStyles( List<MapStyle> newStyles ) {
        this.styles = newStyles;
        for( MapStyle style : styles ) {
            style.setLayer(this);
        }

        if (!styles.isEmpty()) {
            styles.get(0).setSelected(true, true);
        }
    }
    
    /**
     * Returns a somewhat nicely-formatted string representing all of the details of
     * this layer and its sub-layers (recursively).
     * @return a somewhat nicely-formatted string representing all of the details of
     * this layer and its sub-layers (recursively).
     */
    public String toString() {
        StringBuilder s = new StringBuilder( "WMSLayer {\n"
            + "  name: \"" + name + "\"\n"
            + "  title: \"" + title + "\"\n"
            + "  srsList: " + srsList.toString() + "\n"
            + "  subLayers: [\n" );
        for (MapLayer subLayer : subLayers) {
            s.append(subLayer.toString()).append(", ");
        }
        s.append( "  ]\n  bbox: " );
        if( bbox != null ) {
            s.append( bbox.toString() );
        } else {
            s.append( "null" );
        }
        s.append( "\n}\n" );
        return s.toString();
    }
}
