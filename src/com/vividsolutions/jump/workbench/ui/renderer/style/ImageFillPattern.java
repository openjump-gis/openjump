package com.vividsolutions.jump.workbench.ui.renderer.style;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;


/**
 * You can set the alpha by calling #setColor (only the alpha will be read)
 */
public class ImageFillPattern extends BasicFillPattern {
    private static final String FILENAME_KEY = "FILENAME";
    private static final String CLASS_KEY = "CLASS";

    /**
     * @param resourceName name of a resource associated with the given class
     * (e.g. the name of a .png, .gif, or .jpg file in the same package as the class) 
     */
    public ImageFillPattern(Class c, String resourceName) {
        super(new Blackboard().putAll(CollectionUtil.createMap(
                    new Object[] {
                        BasicFillPattern.COLOR_KEY, Color.black, CLASS_KEY, c,
                        FILENAME_KEY, resourceName
                    })));
    }

    /**
     * Parameterless constructor for Java2XML
     */
    public ImageFillPattern() {
    }

    public BufferedImage createImage(Blackboard properties) {
        ImageIcon imageIcon = new ImageIcon(((Class) properties.get(CLASS_KEY)).getResource(
                    properties.get(FILENAME_KEY).toString()));
        BufferedImage bufferedImage = new BufferedImage(imageIcon.getIconWidth(),
                imageIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                ((Color) getProperties().get(COLOR_KEY)).getAlpha() / 255f));
        g.drawImage(imageIcon.getImage(), 0, 0, null);

        return bufferedImage;
    }
}
