package com.vividsolutions.jump.workbench.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FeatureTextWriterRegistry {
    public FeatureTextWriterRegistry() {}
    private List featureTextWriters = new ArrayList();
    public void register(AbstractFeatureTextWriter featureTextWriter) {
        featureTextWriters.add(featureTextWriter);
    }
    public Iterator iterator() {
        return featureTextWriters.iterator();
    }
}
