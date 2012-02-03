package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.feature.Feature;

public abstract class AbstractFeatureTextWriter {
    private String description;
    private String shortDescription;
    private boolean wrapping;
    public AbstractFeatureTextWriter(boolean wrapping, String shortDescription, String description) {
        this.wrapping = wrapping;
        this.shortDescription = shortDescription;
        this.description = description;
    }
    public abstract String write(Feature feature);
    /**
     * Returns a short (2-3 letters) description to display on the button.
     */
    public String getShortDescription() { return shortDescription; }
    /**
     * Returns a description to display on the tooltip.
     */
    public String getDescription() { return description; }
    /**
     * Returns whether to wrap the text.
     */
    public boolean isWrapping() { return wrapping; }
}