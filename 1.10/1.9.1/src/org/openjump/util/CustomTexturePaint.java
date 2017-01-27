//$HeadURL: https://sushibar/svn/deegree/base/trunk/resources/eclipse/svn_classfile_header_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.openjump.util;



import static com.vividsolutions.jump.I18N.get;
import static com.vividsolutions.jump.I18N.getMessage;
import static java.awt.Color.black;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import java.awt.Graphics;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import com.vividsolutions.jump.workbench.Logger;

/**
 * <code>CustomTexturePaint</code> is a helper to work around Java2XML
 * limitations.
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class CustomTexturePaint implements Paint {



    private TexturePaint texturePaint;

    private URL url;

    /**
     * 
     */
    public File svg;

    /**
     * 
     */
    public CustomTexturePaint() {
        BufferedImage img = new BufferedImage(300, 20, TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(black);
        g.drawString(get("org.openjump.util.CustomTexturePaint.no-image-chosen"), 25, 10);
        g.dispose();
        texturePaint = new TexturePaint(img, new Rectangle2D.Float(0, 0, img.getWidth(), img.getHeight()));
    }

    /**
     * @param url
     */
    public CustomTexturePaint(URL url) {
        try {
            setUrl(url.toExternalForm());
        } catch (IOException e) {
            // ignore IOs
            Logger.error("Could not load texture from URL '" + url + "'", e);
            BufferedImage img = new BufferedImage(300, 20, TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.setColor(black);
            g.drawString(get("org.openjump.util.CustomTexturePaint.no-image-chosen"), 25, 10);
            g.dispose();
            texturePaint = new TexturePaint(img, new Rectangle2D.Float(0, 0, img.getWidth(), img.getHeight()));
        }
    }

    /**
     * @param url
     * @throws IOException
     */
    public void setUrl(String url) throws IOException {
        this.url = new URL(url);
        BufferedImage img = ImageIO.read(this.url);
        if (img == null) {
            throw new IOException(getMessage("org.openjump.util.CustomTexturePaint.the-url-does-not-point-to-an-image",
                    new Object[] { url }));
        }
        texturePaint = new TexturePaint(img, new Rectangle2D.Float(0, 0, img.getWidth(), img.getHeight()));
    }

    /**
     * @return the image's URL
     */
    public String getUrl() {
        return url == null ? null : url.toExternalForm();
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints) {
        return texturePaint.createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    public int getTransparency() {
        return texturePaint.getTransparency();
    }

    @Override
    public boolean equals(Object other) {
        // boy is this bad...
        if (other instanceof CustomTexturePaint) {
            return true;
        }

        return super.equals(other);
    }

}
