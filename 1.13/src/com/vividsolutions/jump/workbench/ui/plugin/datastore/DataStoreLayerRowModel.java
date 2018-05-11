package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreLayer;
import java.util.LinkedHashMap;
import org.netbeans.swing.outline.RowModel;

/**
 * A custom RowModel to use in a Swing Outline, used to display a Datasource information
 * for a specific database schema and table name:
 * geometric column info, editable WHERE clause, caching and limit
 * @author nicolas Ribot
 */
public class DataStoreLayerRowModel implements RowModel {

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Boolean.class;
            case 3:
                return Integer.class;
            default:
                assert false;
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Geometry");
            case 1:
                return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Where");
            case 2:
                return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Caching");
            case 3:
                return I18N.get("jump.workbench.ui.plugin.datastore.AddDatastoreLayerPanel.Max-Features");
            default:
                assert false;
        }
        return "";
    }

    @Override
    public Object getValueFor(Object node, int column) {
        if (node instanceof LinkedHashMap || node instanceof String) {
            switch (column) {
                case 0:
                    return new String("");
                case 1:
                    return new String("");
                case 2:
                    return Boolean.FALSE;
                case 3:
                    return new Integer(0);
                default:
                    assert false;
            }
        } else {
            DataStoreLayer ds = (DataStoreLayer) node;
            switch (column) {
                case 0:
                    return ds.getGeoCol().toString();
                case 1:
                    return ds.getWhere();
                case 2:
                    return ds.isCaching();
                case 3:
                    return ds.getLimit();
                default:
                    assert false;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return (column >= 1);
    }

    @Override
    public void setValueFor(Object node, int column, Object value) {
//        System.out.println("nodea: " + node + " col: " + column + "val: " + value);

        if (node instanceof DataStoreLayer) {
            DataStoreLayer l = (DataStoreLayer) node;
            switch (column) {
                case 1:
                    l.setWhere((String) value);
                    break;
                case 2:
                    l.setCaching(!l.isCaching());
                    break;
                case 3:
                    l.setLimit((Integer) value);
                    break;
                default:
                    assert false;
            }
        }
    }

}
