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
package com.vividsolutions.jump.workbench.ui.plugin.clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.io.FUTURE_JTS_WKTWriter;
public class CollectionOfFeaturesTransferable extends AbstractTransferable {
    /** A java.util.Collection, not a FeatureCollection */
    public static final DataFlavor COLLECTION_OF_FEATURES_FLAVOR =
        new DataFlavor(Collection.class, "Collection of Features") {
        public boolean equals(DataFlavor that) {
                //Needed so #equals will return false for COLLECTION_OF_LAYERS_FLAVOR. [Jon Aquino]
    return super.equals(that)
        && getHumanPresentableName().equals(that.getHumanPresentableName());
        }
    };
    private static final DataFlavor[] flavors = { DataFlavor.stringFlavor,        //plainTextFlavor is deprecated, but JDK 1.3 needs it to paste to
        //non-Java apps (like Notepad). [Jon Aquino]
        DataFlavor.plainTextFlavor, COLLECTION_OF_FEATURES_FLAVOR };
    private Collection features;
    private FUTURE_JTS_WKTWriter writer = new FUTURE_JTS_WKTWriter();
    public CollectionOfFeaturesTransferable(Collection features) {
        super(flavors);
        this.features = new ArrayList(features);
    }
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor.equals(COLLECTION_OF_FEATURES_FLAVOR)) {
            return Collections.unmodifiableCollection(features);
        }
        if (flavor.equals(DataFlavor.stringFlavor)) {
            return toString(features);
        }
        if (flavor.equals(DataFlavor.plainTextFlavor)) {
            return new StringReader(toString(features));
        }
        throw new UnsupportedFlavorException(flavor);
    }
    private String toString(Collection features) {
        StringBuffer b = new StringBuffer();
        for (Iterator i = features.iterator(); i.hasNext();) {
            Feature feature = (Feature) i.next();
            //Not System.getProperty("line.separator"); otherwise, when you copy
            //into, say, Notepad, you get garbage characters at the end of each line 
            //(\r\r\n). [Jon Aquino]
            b.append(writer.writeFormatted(feature.getGeometry()) + "\n\n");
        }
        return b.toString();
    }
}
