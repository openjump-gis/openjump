package com.vividsolutions.jump.workbench.ui.renderer.style;

import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collection;


public class CustomFillPatternExamplePlugIn extends AbstractPlugIn {
    public void initialize(PlugInContext context) throws Exception {
        Collection customFillPatterns = (Collection) context.getWorkbenchContext()
                                                            .getWorkbench()
                                                            .getBlackboard()
                                                            .get(FillPatternFactory.CUSTOM_FILL_PATTERNS_KEY,
                new ArrayList());
        customFillPatterns.add(new WKTFillPattern(1, 10, "LINESTRING(3 3, 3 -3, -3 -3, 3 3)"));
        customFillPatterns.add(new ImageFillPattern(IconLoader.class,
                "Favorite.gif"));
    }
}
