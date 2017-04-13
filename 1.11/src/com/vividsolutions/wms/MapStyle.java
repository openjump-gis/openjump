package com.vividsolutions.wms;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.workbench.Logger;



/*
 * Estilo asociado a una capa. Se caracteriza por tener asociado un
 * nombre, un estilo y una leyenda
 * @author Marco Antonio Fuentelsaz P?rez
 * 
 */
public class MapStyle {

    /** Nombre asociado al estilo */
    private String name;

    /** Titulo asociado al estilo */
    private String title;

    /** URL asociado a la leyenda */
    private String urlLegend;

    /** Formato asociado a la leyenda */
    private String formatLegend;

    /** */
    private boolean selected;

    /** Legend icon */
    private Icon legendIcon;

    /** */
    private MapLayer layer;

    /** Flag to indicate if the legend icon have been loaded or not */
    private boolean loadedIcon;
    
    private int width;
    private int height;

    /**
     * @param name
     * @param title
     * @param urlLegend
     * @param formatLegend
     */
    public MapStyle( String name, String title, String urlLegend, String formatLegend ) {
        this.name = name;
        this.title = title;
        setUrlLegend(urlLegend);
        this.formatLegend = formatLegend;
        this.selected = false;
    }
    
    public MapStyle( String name, String title, String urlLegend, String formatLegend, int w, int h  ) {
        this.name = name;
        this.title = title;
        setUrlLegend(urlLegend);
        this.formatLegend = formatLegend;
        this.selected = false;
        this.width = w;
        this.height = h;
    }

    /**
     * @return
     */
    public int getWidth() {
        return width;
    }
   /**
     * @return
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @return
     */
    public String getUrlLegend() {
        return urlLegend;
    }

    /**
     * @param newURLLegend
     */
    public void setUrlLegend( String newURLLegend ) {
        this.urlLegend = newURLLegend;
    }

    /**
     * @return
     */
    public String getFormatLegend() {
        return formatLegend;
    }

    /**
     * @param formatLegend
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
     * @return
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
     * @return
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected
     * @param check
     */
    public void setSelected( boolean selected, boolean check ) {
        if (check) {
            if (this.selected && !selected && layer.getStyles().size() == 1)
                return;
            if (this.selected && !selected) {
                for( Iterator<MapStyle> iter = layer.getStyles().iterator(); iter.hasNext(); ) {
                    MapStyle element = iter.next();
                    if (!element.equals(this))
                        element.setSelected(true, false);

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
     * @param layer
     */
    public void setLayer( MapLayer layer ) {
        this.layer = layer;
    }
}
