package org.openjump.core.rasterimage;

import java.awt.image.BufferedImage;

/**
 *
 * @author AdL
 */
public class ImageAndMetadata {

    public ImageAndMetadata(BufferedImage image, Metadata metadata) {
        this.image = image;
        this.metadata = metadata;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    private final BufferedImage image;
    private final Metadata metadata;

}

