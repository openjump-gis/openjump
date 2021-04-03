package com.vividsolutions.wms;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.Logger;



/*
 * Style of a WMSLayer. It is made of a name, a title and a legend URL.
 * @author Marco Antonio Fuentelsaz P?rez
 * 
 */
public class MapStyle {

    /** Style name */
    private String name;

    /** Style title */
    private String title;

    /** URL associated to th estyle legend */
    private String urlLegend;

    /** Format associated to th elegend */
    private String formatLegend;

    /** */
    private boolean selected;

    /** Legend icon */
    private Icon legendIcon;

    /** */
    private MapLayer layer;

    /** Flag to indicate if the legend icon has been loaded or not */
    private boolean loadedIcon;
    
    private int width;
    private int height;

    /**
     * @param name name of this MapStyle
     * @param title title of this MapStyle
     * @param urlLegend url String of the legend for this MapStyle
     * @param formatLegend image format of the legend
     */
    public MapStyle( String name, String title, String urlLegend, String formatLegend ) {
        this.name = name;
        this.title = title;
        setUrlLegend(urlLegend);
        this.formatLegend = formatLegend;
        this.selected = false;
    }

   /**
    * @param name name of this MapStyle
    * @param title title of this MapStyle
    * @param urlLegend url String of the legend for this MapStyle
    * @param formatLegend image format of the legend
    * @param w width of the image containing the legend
    * @param h height of the image containing the legend
    */
    public MapStyle( String name, String title, String urlLegend, String formatLegend, int w, int h ) {
        this.name = name;
        this.title = title;
        setUrlLegend(urlLegend);
        this.formatLegend = formatLegend;
        this.selected = false;
        this.width = w;
        this.height = h;
    }

    /**
     * @return the width of the legend image
     */
    public int getWidth() {
        return width;
    }

   /**
     * @return the height of the image legend
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * @return the name of the style
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name of the style
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return the legend title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the legend title
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @return url legend as a String
     */
    public String getUrlLegend() {
        return urlLegend;
    }

    /**
     * @param newURLLegend the URL of the legend
     */
    public void setUrlLegend( String newURLLegend ) {
        this.urlLegend = newURLLegend;
    }

    /**
     * @return the image format of the legend
     */
    public String getFormatLegend() {
        return formatLegend;
    }

    /**
     * @param formatLegend image format of the legend
     */
    public void setFormatLegend( String formatLegend ) {
        this.formatLegend = formatLegend;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Load the legend icon at first request, not before
     * 
     * @return an Icon containing the legend
     */
    public Icon getLegendIcon() {
        if (!loadedIcon) {
            loadIconFromLegendURL();
        }
        return legendIcon;
    }

    /**
     * Loads the WMS style legend icon on request
     */
    private void loadIconFromLegendURL() {
        URL selectedUrl = null;
        try {
            selectedUrl = new URL(urlLegend);
        } catch (MalformedURLException e) {
            Logger.error(e);
        }

        if (selectedUrl != null) {
            BufferedImage image;
            try {
                image = ImageIO.read(selectedUrl);
                legendIcon = new ImageIcon(image);
                loadedIcon = true;
            } catch (IOException e) {
                Logger.error(e);
            }

        } else {
            loadedIcon = false;
        }
    }

    /**
     * @return true if this MapStyle is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected whether this MapStyle is selected or not
     * @param check if check, don't unselect the style if the layer has only one style
     */
    public void setSelected( boolean selected, boolean check ) {
        if (check) {
            if (this.selected && !selected && layer.getStyles().size() == 1)
                return;
            if (this.selected && !selected) {
                for( MapStyle style : layer.getStyles()) {
                    if (!style.equals(this)) {
                        style.setSelected(true, false);
                    }
                }
            }
        }

        this.selected = selected;
    }

    /**
     * 
     */
    public void fireStyleChanged() {
        layer.setSelectedStyle(this);
    }

    @Override
    public boolean equals( Object other ) {
        if (other == this)
            return true;
        if (!(other instanceof MapStyle))
            return false;
        return getName().equals(((MapStyle) other).getName());
    }

    /**
     * @param layer MapLayer associated to this MapStyle
     */
    public void setLayer( MapLayer layer ) {
        this.layer = layer;
    }
}
