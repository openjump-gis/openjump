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
import java.util.List;

import com.vividsolutions.jump.workbench.model.Layerable;


public class CollectionOfLayerablesTransferable extends AbstractTransferable {
    public static final DataFlavor COLLECTION_OF_LAYERABLES_FLAVOR = new DataFlavor(Collection.class,
            "Collection of Layerables") {
            public boolean equals(DataFlavor that) {
                //Needed so #equals will return false for COLLECTION_OF_FEATURES_FLAVOR. [Jon Aquino]
                return super.equals(that) &&
                getHumanPresentableName().equals(that.getHumanPresentableName());
            }
        };

    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor, 
        //plainTextFlavor is deprecated, but JDK 1.3 needs it to paste to
        //non-Java apps (like Notepad). [Jon Aquino]
        DataFlavor.plainTextFlavor, COLLECTION_OF_LAYERABLES_FLAVOR
    };
    private Collection layerables;

    public CollectionOfLayerablesTransferable(Collection layerables) {
        super(flavors);
        this.layerables = new ArrayList(layerables);
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException {
        if (flavor.equals(COLLECTION_OF_LAYERABLES_FLAVOR)) {
            return Collections.unmodifiableCollection(layerables);
        }

        if (flavor.equals(DataFlavor.stringFlavor)) {
            return toString(new ArrayList(layerables));
        }

        if (flavor.equals(DataFlavor.plainTextFlavor)) {
            return new StringReader(toString(new ArrayList(layerables)));
        }

        throw new UnsupportedFlavorException(flavor);
    }

    private String toString(List layerables) {
        StringBuffer b = new StringBuffer();

        for (int i = 0; i < layerables.size(); i++) {
            Layerable layerable = (Layerable) layerables.get(i);

            if (i != 0) {
                b.append(", ");
            }

            b.append(layerable.getName());
        }

        return b.toString();
    }
}
