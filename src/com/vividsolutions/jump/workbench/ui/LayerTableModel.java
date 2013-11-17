
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

package com.vividsolutions.jump.workbench.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.UndoableCommand;

public class LayerTableModel extends ColumnBasedTableModel {
    private Layer layer;
    private ArrayList features = new ArrayList();
    private String sortedColumnName = null;
    private boolean sortAscending = false;

    private abstract class MyColumn extends Column {
        public MyColumn(String name, Class dataClass) {
            super(name, dataClass);
        }

        public Object getValueAt(int rowIndex) {
            return getValue(getFeature(rowIndex));
        }

        public void setValueAt(Object value, int rowIndex) {
            setValue(value, getFeature(rowIndex));
        }

        protected abstract Object getValue(Feature feature);

        protected abstract void setValue(Object value, Feature feature);
    }

    private Column fidColumn = new MyColumn("FID", Integer.class) {
        protected Object getValue(Feature feature) {
            return new Integer(feature.getID());
        }

        protected void setValue(Object value, Feature feature) {
            Assert.shouldNeverReachHere();
        }
    };

    private Column geomButtonColumn = new MyColumn(" ", null) {//button column [Jon Aquino]
        protected Object getValue(Feature feature) {
            return feature;
        }

        protected void setValue(Object value, Feature feature) {
            Assert.shouldNeverReachHere();
        }
    };

    private FeatureSchema schema;

    public LayerTableModel(final Layer layer) {
        this.layer = layer;

        layer.getLayerManager().addLayerListener(layerListener);
        initColumns(layer);
    }

    private LayerListener layerListener = new LayerListener() {
        public void categoryChanged(CategoryEvent e) {}
        public void featuresChanged(FeatureEvent e) {
            if (e.getLayer() != getLayer()) {
                return;
            }
            if (e.getType() == FeatureEventType.DELETED) {
                removeAll(e.getFeatures());
            }
            if (e.getType() == FeatureEventType.ATTRIBUTES_MODIFIED) {
                for (Iterator i = e.getFeatures().iterator(); i.hasNext();) {
                    Feature feature = (Feature) i.next();
                    int row = getFeatures().indexOf(feature);
                    if (row != -1) {
                        fireTableChanged(new TableModelEvent(LayerTableModel.this, row, row));
                    }
                }
            }
        }
        public void layerChanged(LayerEvent e) {
            if (e.getLayerable() != getLayer()) {
                return;
            }

            if (e.getType() == LayerEventType.METADATA_CHANGED) {
                //User may have changed the schema. [Jon Aquino]
                if (!schema.equals(layer.getFeatureCollectionWrapper().getFeatureSchema(), true)) {
                    initColumns(layer);
                    fireTableChanged(
                        new TableModelEvent(LayerTableModel.this, TableModelEvent.HEADER_ROW));
                }
            }

        }
    };

    private void initColumns(final Layer layer) {
        schema = layer.getFeatureCollectionWrapper().getFeatureSchema();
        ArrayList columns = new ArrayList();
        columns.add(geomButtonColumn);
        columns.add(fidColumn);

        for (int i = 0; i < schema.getAttributeCount(); i++) {
            if (schema.getAttributeType(i) == AttributeType.GEOMETRY) {
                continue;
            }

            final int j = i;
            columns
                .add(new MyColumn(schema.getAttributeName(i), schema.getAttributeType(i).toJavaClass()) {
                protected Object getValue(Feature feature) {
                  // MD - trapping bad index value here, since at this point it's too late to do anything about it
                  Object value = null;
                  try {
                    value = feature.getAttribute(j);
                  }
                  catch (ArrayIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                  }
                  return value;
                }

                protected void setValue(final Object value, final Feature feature) {
                    final Object oldValue = feature.getAttribute(j);
                    layer.getLayerManager().getUndoableEditReceiver().startReceiving();
                    try {
                        UndoableCommand command =
                            new UndoableCommand(I18N.get("ui.plugin.LayerTableModel.edit")+" " + schema.getAttributeName(j)) {
                            public void execute() {
                                Feature oldClone = (Feature)feature.clone();
                                feature.setAttribute(j, value);
                                layer.getLayerManager().fireFeaturesAttChanged(
                                        Arrays.asList(new Feature[] { feature }),
                                        FeatureEventType.ATTRIBUTES_MODIFIED,
                                        layer, Arrays.asList(new Feature[] { oldClone }));
                            }
                            public void unexecute() {
                                Feature oldClone = (Feature)feature.clone();
                                feature.setAttribute(j, oldValue);
                                layer.getLayerManager().fireFeaturesAttChanged(
                                        Arrays.asList(new Feature[] { feature }),
                                        FeatureEventType.ATTRIBUTES_MODIFIED,
                                        layer, Arrays.asList(new Feature[] { oldClone }));
                            }
                        };
                        command.execute();
                        layer.getLayerManager().getUndoableEditReceiver().receive(
                            command.toUndoableEdit());
                    } finally {
                        layer.getLayerManager().getUndoableEditReceiver().stopReceiving();
                    }
                }
            });
        }
        setColumns(columns);
    }

    private void setAttributesOf(Feature feature, Feature attributes) {
//      [UT] 25.08.2005 the old clone is available here but not used! so use it!
        Feature oldClone = (Feature) feature.clone();
        for (int i = 0; i < feature.getSchema().getAttributeCount(); i++) {
            feature.setAttribute(i, attributes.getAttribute(i));
        }
        // remove this to include method with reference to old feature
        /*layer.getLayerManager().fireFeaturesChanged(
            Arrays.asList(new Feature[] { feature }),
            FeatureEventType.ATTRIBUTES_MODIFIED,
            layer);*/
        
        layer.getLayerManager().fireFeaturesAttChanged(
                Arrays.asList(new Feature[] { feature }),
                FeatureEventType.ATTRIBUTES_MODIFIED,
                layer, Arrays.asList(new Feature[] { oldClone }));
    }

    public Layer getLayer() {
        return layer;
    }

    public Feature getFeature(int row) {
        return (Feature) features.get(row);
    }

    public int getRowCount() {
        return features.size();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (!layer.isEditable()) {
            return false;
        }

        if (getColumn(columnIndex) == fidColumn) {
            return false;
        }

        if (getColumn(columnIndex) == geomButtonColumn) {
            return false;
        }

		FeatureSchema schema = 
			layer.getFeatureCollectionWrapper().getFeatureSchema();
		if (schema.isAttributeReadOnly(schema.getAttributeIndex(getColumn(
				columnIndex).getName())))
			return false;

        return true;
    }

    public void clear() {
        features.clear();
        fireTableChanged(new TableModelEvent(this));
    }

    //public void removeAll(Collection featuresToRemove) {
    //    //if (featuresToRemove.size() > 100) {
    //    //    removeAllFast(featuresToRemove);
    //    //    return;
    //    //}
    //    for (Iterator i = featuresToRemove.iterator(); i.hasNext();) {
    //        Feature feature = (Feature) i.next();
    //        int row = features.indexOf(feature);
    //        if (row == -1) {
    //            //A LayerTableModel might not have all the features in a layer
    //            //i.e. a FeatureInfo window, as opposed to a complete Attributes window. [Jon Aquino]
    //            continue;
    //        }
    //        features.remove(row);
    //        fireTableChanged(
    //            new TableModelEvent(
    //                this,
    //                row,
    //                row,
    //                TableModelEvent.ALL_COLUMNS,
    //                TableModelEvent.DELETE));
    //    }
    //}
    
    public void removeAll(Collection featuresToRemove) {
        List<Integer> idsToRemove = new ArrayList<Integer>();
        for (Iterator it = featuresToRemove.iterator() ; it.hasNext() ; ) {
            idsToRemove.add(((Feature)it.next()).getID());
        }
        Collections.sort(idsToRemove);
        ArrayList newFeatures = new ArrayList();
        for (Iterator it = features.iterator() ; it.hasNext() ; ) {
            Feature f = (Feature)it.next();
            if (Collections.binarySearch(idsToRemove, f.getID()) < 0) {
                newFeatures.add(f);
            }
        }
        features = newFeatures;
        fireTableChanged(new TableModelEvent(this));
    }

    public void addAll(Collection newFeatures) {
        int originalFeaturesSize = features.size();
        Collection newFeaturesOnly = new ArrayList(newFeatures);
        newFeaturesOnly.removeAll(features);
        features.addAll(newFeaturesOnly);

        if (sortedColumnName != null) {
            sort(sortedColumnName, sortAscending);
        }

        fireTableChanged(
            new TableModelEvent(
                this,
                originalFeaturesSize,
                features.size() - 1,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.INSERT));
    }

    /**
     * Facilitate garbage collection by releasing references.
     */
    public void dispose() {
        layer.getLayerManager().removeLayerListener(layerListener);
        features.clear();
    }

    public List getFeatures() {
        return Collections.unmodifiableList(features);
    }

    /**
     * @return null if the table has not yet been sorted
     */
    public String getSortedColumnName() {
        return sortedColumnName;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public void sort(String columnName) {
        sort(columnName, columnName.equals(sortedColumnName) ? (!sortAscending) : true);
    }

    public void sort(final String columnName, final boolean ascending) {
        this.sortAscending = ascending;
        this.sortedColumnName = columnName;

        final int column = indexOfColumn(columnName);
        Collections.sort(features, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ascendingCompare(o1, o2) * (ascending ? 1 : (-1));
            }

            private int ascendingCompare(Object o1, Object o2) {
                Feature f1 = (Feature) o1;
                Feature f2 = (Feature) o2;
                
                Object v1 = ((MyColumn) getColumn(column)).getValue(f1);
                Object v2 = ((MyColumn) getColumn(column)).getValue(f2);
                return compareValue(v1, v2);
            }
        });
    }

    private static int compareValue(Object o1, Object o2)
    {
      if (o1 == null && o2 == null) return 0;
      if (o1 == null) return -1;
      if (o2 == null) return 1;
      
      if (o1 instanceof Boolean) {
        return compareBoolean((Boolean) o1, (Boolean) o2);
      }
      else if (o1 instanceof Geometry) {
        return 0;  // for now - change to compare type
      }
      else if (o1 instanceof Comparable) {
        Comparable attribute1 = (Comparable) o1;
        Comparable attribute2 = (Comparable) o2;
        return attribute1.compareTo(attribute2);
      }
      return 0;
    }
    
    private static int compareBoolean(Boolean b1, Boolean b2)
    {
      boolean bool1 = b1.booleanValue();
      boolean bool2 = b2.booleanValue();
      if (bool1 == bool2) return 0;
      return bool1 ? 1 : -1;
    }
    
    public String getType(int column) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println(new JTable().getDefaultEditor(Date.class));
    }

}
