package org.openjump.core.ui.plugin.wms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.model.WMSLayer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.wms.BoundingBox;
import com.vividsolutions.wms.Capabilities;
import com.vividsolutions.wms.MapLayer;
import com.vividsolutions.wms.WMService;


public class ZoomToWMSLayerPlugIn extends AbstractPlugIn {

    public void initialize( PlugInContext context ) throws Exception {
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory( context
            .getWorkbenchContext() );


        EnableCheck enableCheck = new MultiEnableCheck()
            .add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck())
            .add(enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck());

        context.getFeatureInstaller()
			.addMainMenuPlugin( this, new String[] { MenuNames.VIEW },
                I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" )
                    + "{pos:9}", false, null, enableCheck );
        // Add PlugIn to WMSPopupMenu
        context.getFeatureInstaller().addPopupMenuPlugin(
            context.getWorkbenchFrame().getWMSLayerNamePopupMenu(), this,
            I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" ), false,
            null, enableCheck );
        
    }

    
    public boolean execute( PlugInContext context ) throws Exception {
        Envelope envelope = new Envelope();
        //String srs = null;
        //for (Object o : context.getLayerableNamePanel().selectedNodes(WMSLayer.class)) {
        //    WMSLayer layer = (WMSLayer)o;
        //    envelope.expandToInclude(layer.getEnvelope());
        //    srs = layer.getSRS();
        //}
        //if (envelope.getWidth() == 0.0 && envelope.getHeight() == 0.0) {
        //    context.getWorkbenchFrame().warnUser("No Bounding Box Available for " + srs);
        //    return false;
        //}

        List<MapLayer> mapLayerOfChoosenLayers = getMapLayerOfChoosenLayers( context );
        if (mapLayerOfChoosenLayers.size() == 0) {
            return false;
        }
        String selectedSRS = getSelectedSRS( context );
        Map<String,BoundingBox> boundingBoxesForSRS =
                getBoundingBoxesForSRS( mapLayerOfChoosenLayers, selectedSRS );
        zoomToBoundingBox( context, boundingBoxesForSRS, selectedSRS );

        //Logger.info("Zoom to " + context.getLayerableNamePanel().selectedNodes(WMSLayer.class) + " : "  + envelope);
        //context.getLayerViewPanel().getViewport().zoom(EnvelopeUtil.bufferByFraction(envelope, 0.03));
        return true;
    }
    
    public String getName() {
        return I18N.get("org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer");
    }

    public static final ImageIcon ICON = IconLoader.icon("zoom.gif");

    // Retrieve MapLayer(s) from OpenJUMP's WMSLayer
    private List<MapLayer> getMapLayerOfChoosenLayers( PlugInContext context ) throws Exception {
        List<MapLayer> mapLayerOfChoosenLayers = new ArrayList<MapLayer>();

        // Choosen Layers
        WMSLayer[] selectedWmsLayers = getSelectedWMSLayer( context );

        if ( selectedWmsLayers.length == 0 ) {
            JOptionPane.showMessageDialog( context.getWorkbenchFrame(), I18N
                    .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.no-wms-layer-selected" ) );
            return mapLayerOfChoosenLayers;
        }

        Set<MapLayer> allLayers = this.getAllMapLayer( context );

        // Filter choosen Layer
        // [mmichaud] search is based on layer's name : if layer's name has been changed
        // by the user for any reason, it does not work any more
        for (WMSLayer selectedLayer : selectedWmsLayers) {
            String name = selectedLayer.getName().replaceAll(" \\(\\d+\\)$","");
            for (MapLayer mapLayer : allLayers) {
                String mapLayerTitle = mapLayer.getTitle();
                String mapLayerName = mapLayer.getName();
                if (mapLayerTitle != null && mapLayerName != null) {
                    if (mapLayerTitle.contains(name) || mapLayerName.contains(name))
                        mapLayerOfChoosenLayers.add(mapLayer);
                } else if (mapLayerTitle != null && mapLayerTitle.contains(name)) {
                        mapLayerOfChoosenLayers.add(mapLayer);
                } else if (mapLayerName != null && mapLayerName.contains(name)) {
                        mapLayerOfChoosenLayers.add(mapLayer);
                }
            }
        }
        return mapLayerOfChoosenLayers;
    }

    private WMSLayer[] getSelectedWMSLayer( PlugInContext context ) {
        Collection nodes = context.getLayerNamePanel().selectedNodes( WMSLayer.class );
        List<WMSLayer> wmsLayerList = new ArrayList<>();
        for (Object o : nodes) {
            wmsLayerList.add((WMSLayer)o);
        }
        return wmsLayerList.toArray(new WMSLayer[0]);
    }

    private Set<MapLayer> getAllMapLayer( PlugInContext context ) throws Exception {
        WMSLayer[] wmsLayers = getSelectedWMSLayer( context );
        Set<MapLayer> set = new HashSet<>();
        for (WMSLayer layer : wmsLayers) {
            WMService wmService = layer.getService();
            Capabilities capabilities = wmService.getCapabilities();
            MapLayer topLayer = capabilities.getTopLayer();
            set.addAll(topLayer.getLayerList());
        }
        return set;
    }

    private String getSelectedSRS( PlugInContext context ) {
        String selectedSRS = "0";
        WMSLayer[] wmsLayers = getSelectedWMSLayer( context );
        for (WMSLayer wmsLayer : wmsLayers) {
            selectedSRS = wmsLayer.getSRS().toLowerCase();
        }
        //[MM] just return the SRS of the last WMSLayer ?
        return selectedSRS;
    }

    private Map<String,BoundingBox> getBoundingBoxesForSRS( List<MapLayer> mapLayerList, String srs ) {
        Map<String,BoundingBox> boundingBoxesForSRS = new HashMap<>();

        for (MapLayer mapLayer : mapLayerList) {

            // All BoundingBoxes
            List<BoundingBox> boundingBoxList = mapLayer.getAllBoundingBoxList();
            // LatLon BoundingBox (epsg:4326)
            BoundingBox latLonBoundingBox = mapLayer.getLatLonBoundingBox();

            if ( latLonBoundingBox != null && boundingBoxList.size() > 0) {
                boundingBoxList.add(latLonBoundingBox);
                Set<String> doppelt = new HashSet<>();
                for (BoundingBox bb : boundingBoxList) {
                    String bbSrs = bb.getSRS().toLowerCase();
                    if (bbSrs.contains("latlon")) bbSrs = "epsg:4326";

                    if (bbSrs.equalsIgnoreCase(srs)) { // SRS found
                        String key = mapLayer.getTitle();
                        if (!doppelt.add(key)) {
                            key = key + " (" + bbSrs + ")";
                        }
                        boundingBoxesForSRS.put(key, bb);
                    }
                }
            }
        }
        return boundingBoxesForSRS;
    }

    private void zoomToBoundingBox( PlugInContext context, Map<String,BoundingBox> boundingBoxesForSRS, String selectedSRS )
            throws Exception {
        JComboBox<String> comboBox = makeComboBox( boundingBoxesForSRS );
        JPanel jp = new JPanel();
        JButton jb = new JButton( "?" );
        jb.setActionCommand( "showInfoTable" );
        jb.addActionListener( new ShowInfoActionListener(context) );

        String tmpLatLon = "";
        if ( selectedSRS.contains( "4326" ) )
            tmpLatLon = " (LatLon)";

        jp.add( comboBox );
        jp.add( jb );

        int back = JOptionPane.showConfirmDialog( context.getWorkbenchFrame(), jp, I18N
                        .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.bounding-box-for" )
                        + " " + selectedSRS + tmpLatLon, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE );

        if ( back == JOptionPane.CANCEL_OPTION || back < 0 ) return;

        // Get the BoundingBox from name
        BoundingBox selectedBB = boundingBoxesForSRS.get( comboBox.getSelectedItem() );
        if ( selectedBB == null )
            return;

        String message = selectedBB.toString();

        context.getWorkbenchFrame().setStatusMessage( message );

        Coordinate min = new Coordinate( selectedBB.getWestBound(), selectedBB.getSouthBound() );
        Coordinate max = new Coordinate( selectedBB.getEastBound(), selectedBB.getNorthBound() );
        Envelope env = new Envelope( min, max );

        context.getLayerViewPanel().getViewport().zoom( env );
        context.getLayerViewPanel().fireSelectionChanged();

        context.getActiveInternalFrame().updateUI();
    }

    private JComboBox<String> makeComboBox( Map<String,BoundingBox> boundingBoxesForSRS ) {
        JComboBox<String> comboBox = new JComboBox<>();
        if ( boundingBoxesForSRS.size() > 0 ) {
            String[] keys = boundingBoxesForSRS.keySet().toArray(new String[0]);
            Arrays.sort( keys );
            comboBox = new JComboBox<>( keys );
        }
        else {
            comboBox.addItem( I18N
                    .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.no-bounding-boxes-available" ) );
        }
        return comboBox;
    }

    private class ShowInfoActionListener implements ActionListener {

        PlugInContext context;

        ShowInfoActionListener(PlugInContext context) {
            this.context = context;
        }

        public void actionPerformed( ActionEvent ae ) {
            if ( ae.getActionCommand().equals( "showInfoTable" ) ) {
                try {
                    showInformationTable( context );
                } catch (Exception e) {
                    Logger.error( "Error in ShowInfoActionListener" , e);
                }
            }
        }

    }

    private Object[][] values = null;
    private String[] columnNames = null;
    private JTable infoTable = null;
    private TableColumnModel tcm = null;
    private JScrollPane infoTableSc = null;

    void showInformationTable( PlugInContext context ) throws Exception {

        this.values = getMapLayerInformationForTable( context );
        this.columnNames = MapLayerAttributes.getColumnNames();

        ZoomToWMSLayerPlugIn.InfoTableModel itm =
                new ZoomToWMSLayerPlugIn.InfoTableModel();

        this.infoTable = new JTable( itm );

        JTableHeader th = infoTable.getTableHeader();
        th.setReorderingAllowed( false );
        th.addMouseListener( new ZoomToWMSLayerPlugIn.MASort() );

        tcm = infoTable.getColumnModel();

        TableColumn tc0 = tcm.getColumn( 0 );
        TableColumn tc1 = tcm.getColumn( 1 );
        TableColumn tc2 = tcm.getColumn( 2 );

        TableColumn tc3 = tcm.getColumn( 3 );
        TableColumn tc4 = tcm.getColumn( 4 );
        TableColumn tc5 = tcm.getColumn( 5 );
        TableColumn tc6 = tcm.getColumn( 6 );

        tc0.setMinWidth( 160 );
        tc1.setMinWidth( 120 );
        tc2.setMinWidth( 70 );

        tc3.setMinWidth( 90 );
        tc4.setMinWidth( 90 );
        tc5.setMinWidth( 90 );
        tc6.setMinWidth( 90 );

        th.setResizingAllowed( true );
        infoTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        this.infoTableSc = new JScrollPane( infoTable );
        infoTableSc.setPreferredSize ( new java.awt.Dimension( 735, 300 ) );

        JOptionPane.showMessageDialog( context.getWorkbenchFrame(), infoTableSc, "InfoTable",
                JOptionPane.INFORMATION_MESSAGE );
    }

    Object[][] getMapLayerInformationForTable( PlugInContext context ) throws Exception {
        Object[][] mapLayerInformationForTable;

        ZoomToWMSLayerPlugIn.MapLayerAttributes mapLayerAttr =
                new ZoomToWMSLayerPlugIn.MapLayerAttributes();

        List mapLayerRows = new ArrayList();
        Set<MapLayer> mapLayerSet = getAllMapLayer( context );

        if ( mapLayerSet == null )
            return null;

        int anzLayer = mapLayerSet.size();

        if ( anzLayer == 0 )
            return null;

        for (MapLayer mapLayer : mapLayerSet) {
            mapLayerRows.addAll( mapLayerAttr.getMapLayerRows( mapLayer ) );
        }

        int anzRows = mapLayerRows.size();
        int anzColumns = ZoomToWMSLayerPlugIn.MapLayerAttributes.getColumnNames().length;

        mapLayerInformationForTable = new Object[anzRows][anzColumns];

        for (int k = 0; k < anzRows; k++) {
            MapLayerAttributes mLA = (MapLayerAttributes) mapLayerRows.get( k );
            Object[] attrib = mLA.toObjectArray();

            for (int m = 0; m < attrib.length; m++) {
                mapLayerInformationForTable[k][m] = attrib[m];
            }
        }
        // ------------------------ ZoomToWMSLayerPLugIn getMapLayerInformationForTable ( )
        return mapLayerInformationForTable;
    }

    String[][] getMapLayerInformationForTable( MapLayerAttributes[] mapLayerAttributesArray ) {

        int numRows = mapLayerAttributesArray.length;
        int numCols = ZoomToWMSLayerPlugIn.MapLayerAttributes.getColumnNames().length;

        String[][] mapLayerInformationForTable = new String[numRows][numCols];

        for (int k = 0; k < numRows; k++) {
            ZoomToWMSLayerPlugIn.MapLayerAttributes mLA = mapLayerAttributesArray[k];
            Object[] attrib = mLA.toObjectArray();
            for (int m = 0; m < attrib.length; m++) {
                values[k][m] = attrib[m];
            }
        }

        return mapLayerInformationForTable;
    }


    private class InfoTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return columnNames.length;
        }
        public int getRowCount() {
            return values.length;
        }
        public String getColumnName( int col ) {
            return columnNames[col];
        }
        public Object getValueAt( int row, int col ) {
            return values[row][col];
        }
        public Class getColumnClass( int c ) {
            return getValueAt( 0, c ).getClass();
        }
        public boolean isCellEditable( int row, int col ) {
            return false;
        }
    }

    private class MASort extends MouseAdapter {

        public void mousePressed( MouseEvent me ) {
            if ( me.getButton() == MouseEvent.BUTTON3 ) {
                int viewColumn = tcm.getColumnIndexAtX( me.getX() );
                int column = infoTable.convertColumnIndexToModel( viewColumn );

                if ( column == 0 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_TITLE );
                } else if ( column == 1 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_NAME );
                }

                else if ( column == 2 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_SRS );
                }

                else if ( column == 3 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MINX );
                }

                else if ( column == 4 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MINY );
                }

                else if ( column == 5 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MAXX );
                }

                else if ( column == 6 ) {
                    sortTable( ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MAXY );
                }

            }

        }

    }


    private void sortTable( int sortAfter ) {
        ZoomToWMSLayerPlugIn.MapLayerAttributes[] mapLayerAttributes = toMapLayerAttributesArray( values );
        ZoomToWMSLayerPlugIn.MapLayerAttributes.setSortAfter( sortAfter );
        Arrays.sort( mapLayerAttributes );
        getMapLayerInformationForTable( mapLayerAttributes );
        infoTable.updateUI();
    }

    ZoomToWMSLayerPlugIn.MapLayerAttributes[] toMapLayerAttributesArray(Object[][] m) {
        if ( m == null )
            return null;

        int numRows = m.length;

        ZoomToWMSLayerPlugIn.MapLayerAttributes[] mapLayerAttributesArray = new ZoomToWMSLayerPlugIn.MapLayerAttributes[numRows];

        for (int i = 0; i < numRows; i++) {
            String title = (String) m[i][0];
            String name = (String) m[i][1];
            String srs = (String) m[i][2];
            double minx = (Double) m[i][3];
            double miny = (Double) m[i][4];
            double maxx = (Double) m[i][5];
            double maxy = (Double) m[i][6];

            mapLayerAttributesArray[i] = new ZoomToWMSLayerPlugIn.MapLayerAttributes( title, name, srs, minx, miny,
                    maxx, maxy );
        }
        return mapLayerAttributesArray;
    }


    private static class MapLayerAttributes implements Comparable {

        static final int SORT_UP = 1;
        static final int SORT_DOWN = -1;
        static final int SORT_AFTER_TITLE = 1;
        static final int SORT_AFTER_NAME = 2;
        static final int SORT_AFTER_SRS = 3;
        static final int SORT_AFTER_MINX = 4;
        static final int SORT_AFTER_MINY = 5;
        static final int SORT_AFTER_MAXX = 6;
        static final int SORT_AFTER_MAXY = 7;
        static int[] sortUpDown = { SORT_DOWN, SORT_DOWN, SORT_DOWN, SORT_DOWN, SORT_DOWN,
                SORT_DOWN, SORT_DOWN };

        static int sortAfter;

        String srs, name, title;

        double minx, miny, maxx, maxy;

        MapLayerAttributes() {
            this.srs = " ";
            this.name = "Unknown";
            this.title = "Unknown";

            this.minx = 0.;
            this.miny = 0.;
            this.maxx = 0.;
            this.maxy = 0.;
        }


        MapLayerAttributes( String title, String name, String srs, double minx, double miny,
                            double maxx, double maxy ) {
            this.title = title;
            this.name = name;
            this.srs = srs;

            this.minx = minx;
            this.miny = miny;
            this.maxx = maxx;
            this.maxy = maxy;
        }


        public int compareTo( Object object ) { // Sorting must be overwritten
            int ret = 1;
            MapLayerAttributes mla = (MapLayerAttributes) object;

            if ( sortAfter == SORT_AFTER_TITLE ) {
                ret = title.compareTo( mla.title ) * sortUpDown[0];
            }

            else if ( sortAfter == SORT_AFTER_NAME ) {
                ret = name.compareTo( mla.name ) * sortUpDown[1];
            }

            else if ( sortAfter == SORT_AFTER_SRS ) {
                ret = srs.compareTo( mla.srs ) * sortUpDown[2];
            }

            else if ( sortAfter == SORT_AFTER_MINX ) {
                if ( minx > mla.minx ) {
                    ret = 1 * sortUpDown[3];
                } else {
                    ret = -1 * sortUpDown[3];
                }
            }

            else if ( sortAfter == SORT_AFTER_MINY ) {
                if ( miny > mla.miny ) {
                    ret = 1 * sortUpDown[4];
                } else {
                    ret = -1 * sortUpDown[4];
                }
            }

            else if ( sortAfter == SORT_AFTER_MAXX ) {
                if ( maxx > mla.maxx ) {
                    ret = 1 * sortUpDown[5];
                } else {
                    ret = -1 * sortUpDown[5];
                }
            }

            else if ( sortAfter == SORT_AFTER_MAXY ) {
                if ( maxy > mla.maxy ) {
                    ret = 1 * sortUpDown[6];
                } else {
                    ret = -1 * sortUpDown[6];
                }
            }

            return ret;

        }

        double cutDouble( double value, int afterComma ) {
            double mulQuot = Math.pow( 10.d, afterComma );
            long tmp = (long) ( value * mulQuot );
            return tmp / mulQuot;
        }


        public static String[] getColumnNames() {
            return new String[]{ "Title", "Name", "SRS", "MinX", "MinY", "MaxX", "MaxY" };
        }


        List<MapLayerAttributes> getMapLayerRows( MapLayer mapLayer ) {
            double minX, minY, maxX, maxY;

            String srs, name, title;
            String unknown = "Unknown";

            List<MapLayerAttributes> mapLayerRows = new ArrayList<>();

            name = mapLayer.getName();
            if ( name == null )
                name = unknown;
            title = mapLayer.getTitle();
            if ( title == null )
                title = unknown;

            List<BoundingBox> boundingBoxList = mapLayer.getAllBoundingBoxList();

            for (BoundingBox bb : boundingBoxList) {

                if ( bb == null ) {
                    srs = unknown;
                    minX = 0.;
                    minY = 0.;
                    maxX = 400.;
                    maxY = 400.;
                } else {
                    srs = bb.getSRS().toLowerCase();
                    minX = bb.getWestBound();
                    minY = bb.getSouthBound();
                    maxX = bb.getEastBound();
                    maxY = bb.getNorthBound();
                }

                mapLayerRows.add(
                        new ZoomToWMSLayerPlugIn.MapLayerAttributes( title, name, srs, minX, minY, maxX, maxY ) );
            }

            return mapLayerRows;
        }

        String getName() {
            return this.name;
        }

        String getTitle() {
            return this.title;
        }

        String getSRS() {
            return this.srs;
        }

        double getMinx() {
            return this.minx;
        }

        double getMiny() {
            return this.miny;
        }

        double getMaxx() {
            return this.maxx;
        }

        double getMaxy() {
            return this.maxy;
        }


        Object[] toObjectArray() {
            int anzColumns = getColumnNames().length;
            Object[] objectArray = new Object[anzColumns];

            objectArray[0] = this.getTitle();
            objectArray[1] = this.getName();
            objectArray[2] = this.getSRS();
            objectArray[3] = Double.valueOf( String.valueOf( this.getMinx() ) );
            objectArray[4] = Double.valueOf( String.valueOf( this.getMiny() ) );
            objectArray[5] = Double.valueOf( String.valueOf( this.getMaxx() ) );
            objectArray[6] = Double.valueOf( String.valueOf( this.getMaxy() ) );

            return objectArray;
        }

        static void setSortAfter( int sortAfter ) {
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_TITLE ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[0] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[0];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_NAME ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[1] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[1];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_SRS ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[2] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[2];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MINX ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[3] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[3];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MINY ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[4] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[4];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MAXX ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[5] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[5];
            }
            if ( sortAfter == ZoomToWMSLayerPlugIn.MapLayerAttributes.SORT_AFTER_MAXY ) {
                ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[6] = -1
                        * ZoomToWMSLayerPlugIn.MapLayerAttributes.sortUpDown[6];
            }

            ZoomToWMSLayerPlugIn.MapLayerAttributes.sortAfter = sortAfter;
        }

    }
}