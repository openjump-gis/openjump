// ----------------------------------------------------------- ZoomToWMSPLugIn
/*
 * With this PlugIn you can zoom on a OpenJUMP WMS-Layer to available BoundingBoxes receiving from
 * the WMS-Server ( right-click on the WMS-Layer and then ZoomToWMS...). Also you can get all
 * BoundingBox informations receiving from the WMS-Server.
 * 
 * This PlugIn works together with extended code in com.vividsolutions.wms.MapLayer.class and
 * com.vividsolutions.wms.Parser.class
 * 
 * So you have to exchange these two files with my extended files!
 * 
 * The extension for this PlugIn is WMSSupportExtension.class
 * 
 * For more information please contact:
 * 
 * University of Applied Sciences Department of Geomatics Dipl.-Ing. Uwe Dalluege Hebebrandstr. 1
 * 22297 Hamburg Germany Tel.: +49 40 42875 - 5335 oder 5353 oder 5313 Fax: +49 40 42875 - 5409
 * E-Mail: uwe.dalluege@rzcn.haw-hamburg.de Url: http://www.haw-hamburg.de/geomatik
 * 
 * Last change: 29.11.2005
 */
package org.openjump.core.ui.plugin.wms;

import java.lang.reflect.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.wms.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.plugin.*;
import com.vividsolutions.jump.util.*;

public class ZoomToWMSPlugIn extends AbstractPlugIn 
{
    PlugInContext context;

    Object[][] values = null;

    String[] columnNames = null;

    JTable infoTable = null;

    TableColumnModel tcm = null;

    JPanel jp = null;

    JScrollPane infoTableSc = null;

    public void initialize( PlugInContext context ) throws Exception {
        this.context = context;

        EnableCheckFactory enableCheckFactory = new EnableCheckFactory( context
            .getWorkbenchContext() );

        EnableCheck enableCheck = new MultiEnableCheck().add(
            enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck() ).add(
            enableCheckFactory.createExactlyNLayerablesMustBeSelectedCheck( 1, WMSLayer.class ) );

        context.getFeatureInstaller()
        //			.addMainMenuItemWithJava14Fix ( this, new String [ ] { "View" },
			.addMainMenuItemWithJava14Fix( this, new String[] { MenuNames.VIEW },
                I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" )
                    + "{pos:8}", false, null, enableCheck ); //enableCheck );
        // Add PlugIn to WMSPopupMenu
        context.getFeatureInstaller().addPopupMenuItem(
            context.getWorkbenchFrame().getWMSLayerNamePopupMenu(), this,
            I18N.get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.zoom-to-wms-layer" ), false,
            null, enableCheck );
        
    } // End initialize ( )

    public boolean execute( PlugInContext context ) throws Exception {
        this.context = context;
        ArrayList mapLayerOfChoosenLayers = getMapLayerOfChoosenLayers( context );
        String selectedSRS = getSelectedSRS( context );

        Hashtable boundingBoxesForSRS = getBoundingBoxesForSRS( mapLayerOfChoosenLayers,
            selectedSRS );
        zoomToBoundingBox( context, boundingBoxesForSRS, selectedSRS );

        return true;
    } // End execute ( )
    
    public String getName() {
        return "ZoomToWMS";
    }

        WMSLayer[] getSelectedWMSLayer( PlugInContext context ) {
        Collection listWMS = context.getLayerNamePanel().selectedNodes( WMSLayer.class );
        Object[] obWMSLayer = listWMS.toArray();

        int anzSelectedWMSLayer = Array.getLength( obWMSLayer );

        if ( anzSelectedWMSLayer <= 0 )
            return null;

        WMSLayer[] wmsLayer = new WMSLayer[anzSelectedWMSLayer];

        for (int i = 0; i < anzSelectedWMSLayer; i++) {
            wmsLayer[i] = (WMSLayer) obWMSLayer[i];
        }

        return wmsLayer;
    } // End getSelectedWMSLayer ( )

        String[] getSelectedWMSLayerNames( PlugInContext context ) {
        WMSLayer[] wmsLayer = getSelectedWMSLayer( context );

        if ( wmsLayer == null )
            return null;

        int anzSelectedWMSLayer = Array.getLength( wmsLayer );

        String[] selectedWMSLayerNames = new String[anzSelectedWMSLayer];

        for (int i = 0; i < anzSelectedWMSLayer; i++) {
            selectedWMSLayerNames[i] = wmsLayer[i].getName();
        }
// ------------------------------ ZoomToWMSPLugIn getSelectedWMSLayerNames ( )
        return selectedWMSLayerNames;
    } // End getSelectedWMSLayerNames ( )
    
        ArrayList getMapLayerOfChoosenLayers( PlugInContext context ) throws Exception {
        ArrayList mapLayerOfChoosenLayers = new ArrayList();
        ArrayList wmsLayerNames = new ArrayList();

        // Choosen Layers
        WMSLayer[] wmsLayer = getSelectedWMSLayer( context );

        if ( wmsLayer == null ) {
            JOptionPane.showMessageDialog( context.getWorkbenchFrame(), I18N
                .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.no-wms-layer-selected" ) );
            return null;
        }

        for (int i = 0; i < Array.getLength( wmsLayer ); i++) {
            java.util.List wmsList = wmsLayer[i].getLayerNames();

            for (int k = 0; k < wmsList.size(); k++) {
                String name = (String) wmsList.get( k );
                wmsLayerNames.add( name );
            }
        }

        // Get all available MapLayer
        WMService wmService = wmsLayer[0].getService();
        Capabilities cap = wmService.getCapabilities();
        MapLayer topLayer = cap.getTopLayer();
        //		ArrayList allLayer = topLayer.getLayerList ( );

        ArrayList allLayer = this.getAllMapLayer( context );

        for (int i = 0; i < allLayer.size(); i++) {
            MapLayer mL = (MapLayer) allLayer.get( i );
        }

        // Filter choosen Layer
        for (int i = 0; i < wmsLayerNames.size(); i++) {
            String name = (String) wmsLayerNames.get( i );

            for (int k = 0; k < allLayer.size(); k++) {
                MapLayer mapLayer = (MapLayer) allLayer.get( k );
                String mapLayerTitle = mapLayer.getTitle();
                String mapLayerName = mapLayer.getName();

                if ( mapLayerTitle != null
                    && mapLayerName != null ) {
                    if ( mapLayerTitle.indexOf( name ) >= 0
                        || mapLayerName.indexOf( name ) >= 0 )
                        mapLayerOfChoosenLayers.add( mapLayer );
                } else if ( mapLayerTitle != null ) {
                    if ( mapLayerTitle.indexOf( name ) >= 0 )
                        mapLayerOfChoosenLayers.add( mapLayer );
                } else if ( mapLayerName != null ) {
                    if ( mapLayerName.indexOf( name ) >= 0 )
                        mapLayerOfChoosenLayers.add( mapLayer );
                }
               
            } // End for k ...
        } // End for i ...
        return mapLayerOfChoosenLayers;
    } // End getMapLayerOfChoosenLayers ( )

    String getSelectedSRS( PlugInContext context ) {
        String selectedSRS = "0";
        // Choosen Layers
        WMSLayer[] wmsLayer = getSelectedWMSLayer( context );

        for (int i = 0; i < Array.getLength( wmsLayer ); i++) {
            // Choosen SRS
            selectedSRS = (String) wmsLayer[i].getSRS();
            selectedSRS = selectedSRS.toLowerCase();
            /*
             * if ( selectedSRS != null ) { // if ( selectedSRS.indexOf ( "4326" ) >= 0 )
             * selectedSRS = "LatLon"; } else { // Problems, when loading a task // selectedSRS =
             * "LatLon"; }
             */
        }

        return selectedSRS;
    } // End getSelectedSRS ( )

    Hashtable getBoundingBoxesForSRS( ArrayList mapLayerList, String srs ) {
        Hashtable boundingBoxesForSRS = new Hashtable();

        for (int i = 0; i < mapLayerList.size(); i++) {
            MapLayer mapLayer = (MapLayer) mapLayerList.get( i );

            // All BoundingBoxes
            ArrayList boundingBoxList = mapLayer.getAllBoundingBoxList();
            // LatLon BoundingBox (epsg:4326)
            BoundingBox latLonBoundingBox = mapLayer.getLatLonBoundingBox();

            if ( latLonBoundingBox != null )
                boundingBoxList.add( latLonBoundingBox );

            int anzBB = boundingBoxList.size();

            if ( anzBB == 0 ) {
                System.out.println( I18N
                    .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.no-bounding-box" )
                    + mapLayer.getTitle() );
                continue;
            } else {
                // If double key - count up.
                HashSet doppelt = new HashSet();
                int zaehler = 0;

                for (int k = 0; k < anzBB; k++) {
                    BoundingBox tmpBB = (BoundingBox) boundingBoxList.get( k );
                    String tmpSRS = tmpBB.getSRS().toLowerCase();
                    if ( tmpSRS.indexOf( "latlon" ) >= 0 )
                        tmpSRS = "epsg:4326";

                    if ( tmpSRS.equals( srs.toLowerCase() ) ) { // SRS found
                        String key = mapLayer.getTitle();
                        if ( !doppelt.add( key ) ) {
                            zaehler++;
                            key = key
                                + " (" + zaehler + ")";
                        }

                        boundingBoxesForSRS.put( key, tmpBB );
                    }
                }
            }
        }

        return boundingBoxesForSRS;
    } // End getBoundingBoxesForSRS ( )

    JComboBox makeComboBox( Hashtable boundingBoxesForSRS ) 
    {
        JComboBox comboBox = new JComboBox();
        if ( boundingBoxesForSRS.size() > 0 ) 
        {
            Object[] keys = boundingBoxesForSRS.keySet().toArray();
            Arrays.sort( keys );
            comboBox = new JComboBox( keys );
        } 
        else 
        {
            comboBox
                .addItem( I18N
                    .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.no-bounding-boxes-available" ) );
        }
        // ------------------------------------------ ZoomToWMSPLugIn makeComboBox ( )
        return comboBox;
    } // End makeComboBox ( )

    void zoomToBoundingBox( PlugInContext context, Hashtable boundingBoxesForSRS, String selectedSRS )
        throws Exception {
        JComboBox comboBox = makeComboBox( boundingBoxesForSRS );
        JPanel jp = new JPanel();
        JButton jb = new JButton( "?" );
        jb.setActionCommand( "showInfoTable" );
        jb.addActionListener( new AL() );

        String tmpLatLon = "";
        if ( selectedSRS.indexOf( "4326" ) >= 0 )
            tmpLatLon = " (LatLon)";

        jp.add( comboBox );
        jp.add( jb );

        int back = JOptionPane.showConfirmDialog( context.getWorkbenchFrame(), jp, I18N
            .get( "org.openjump.core.ui.plugin.wms.ZoomToWMSPlugIn.bounding-box-for" )
            + " " + selectedSRS + tmpLatLon, JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE );

        if ( back == JOptionPane.CANCEL_OPTION
            || back < 0 )
            return;

        // Get the BoundingBox from name
        BoundingBox selectedBB = (BoundingBox) boundingBoxesForSRS.get( comboBox.getSelectedItem() );
        if ( selectedBB == null )
            return;

        String tmpSRS = selectedBB.getSRS();
        if ( tmpSRS.toLowerCase().indexOf( "latlon" ) >= 0 )
            tmpSRS = "EPSG:4326";
        String message = tmpSRS
            + " (" + Math.round( selectedBB.getMinX() ) + ", " + Math.round( selectedBB.getMinY() )
            + ") (" + Math.round( selectedBB.getMaxX() ) + ", " + Math.round( selectedBB.getMaxY() )
            + ")";

        context.getWorkbenchFrame().setStatusMessage( message );

        // ------------------------------------- ZoomToWMSPLugIn zoomToBoundingBox ( )
        Coordinate min = new Coordinate( selectedBB.getMinX(), selectedBB.getMinY() );
        Coordinate max = new Coordinate( selectedBB.getMaxX(), selectedBB.getMaxY() );
        Envelope env = new Envelope( min, max );

        context.getLayerViewPanel().getViewport().zoom( env );
        context.getLayerViewPanel().fireSelectionChanged();

        JInternalFrame intFrame = context.getActiveInternalFrame();
        intFrame.updateUI();

    } // End zoomToBoundingBox ( )

    ArrayList getAllMapLayer( PlugInContext context ) throws Exception {
        WMSLayer[] wmsLayer = getSelectedWMSLayer( context );

        if ( wmsLayer == null
            || Array.getLength( wmsLayer ) == 0 )
            return null;

        // Get all available MapLayer
        WMService wmService = wmsLayer[0].getService();
        Capabilities cap = wmService.getCapabilities();
        MapLayer topLayer = cap.getTopLayer();
        ArrayList allMapLayer = topLayer.getLayerList();
        // ---------------------------------------- ZoomToWMSPLugIn getAllMapLayer ( )
        return allMapLayer;
    } // End getAllMapLayer ( )

    void showInformationTable( PlugInContext context ) throws Exception {

        this.values = getMapLayerInformationForTable( context );
        this.columnNames = MapLayerAttributes.getColumnNames();

        InfoTableModel itm = new InfoTableModel();

        this.infoTable = new JTable( itm );

        JTableHeader th = infoTable.getTableHeader();
        th.setReorderingAllowed( false );
        th.addMouseListener( new MASort() );

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

        //infoTable.setPreferredSize ( new java.awt.Dimension ( 700, 300 ) );

        this.infoTableSc = new JScrollPane( infoTable );
        infoTableSc.setPreferredSize ( new java.awt.Dimension( 735, 300 ) );

        JOptionPane.showMessageDialog( context.getWorkbenchFrame(), infoTableSc, "InfoTable",
            JOptionPane.INFORMATION_MESSAGE );
        // ---------------------------------- ZoomToWMSPLugIn showInformationTable ( )
    } // End showInformationTable

    public void sortTable( int sortAfter ) {
        MapLayerAttributes[] mapLayerAttributes = toMapLayerAttributesArray( values );

        MapLayerAttributes.setSortAfter( sortAfter );

        Arrays.sort( mapLayerAttributes );

        getMapLayerInformationForTable( mapLayerAttributes );
        infoTable.updateUI();
    } // End sortTable ( )

    Object[][] getMapLayerInformationForTable( PlugInContext context ) throws Exception {
        Object[][] mapLayerInformationForTable = null;

        MapLayerAttributes mapLayerAttr = new MapLayerAttributes();

        ArrayList mapLayerRows = new ArrayList();
        ArrayList mapLayerList = getAllMapLayer( context );

        if ( mapLayerList == null )
            return null;

        int anzLayer = mapLayerList.size();

        if ( anzLayer == 0 )
            return null;

        for (int i = 0; i < anzLayer; i++) {
            MapLayer mapLayer = (MapLayer) mapLayerList.get( i );
            mapLayerRows.addAll( mapLayerAttr.getMapLayerRows( mapLayer ) );
        }

        int anzRows = mapLayerRows.size();
        int anzColumns = Array.getLength( MapLayerAttributes.getColumnNames() );

        mapLayerInformationForTable = new Object[anzRows][anzColumns];

        for (int k = 0; k < anzRows; k++) {
            MapLayerAttributes mLA = (MapLayerAttributes) mapLayerRows.get( k );
            Object[] attrib = mLA.toObjectArray();

            for (int m = 0; m < Array.getLength( attrib ); m++) {
                mapLayerInformationForTable[k][m] = attrib[m];
            }
        }
        // ------------------------ ZoomToWMSPLugIn getMapLayerInformationForTable ( )
        return mapLayerInformationForTable;
    } // End getMapLayerInformationForTable ( a )

    String[][] getMapLayerInformationForTable( MapLayerAttributes[] mapLayerAttributesArray ) {

        int numRows = Array.getLength( mapLayerAttributesArray );
        int numCols = Array.getLength( MapLayerAttributes.getColumnNames() );

        String[][] mapLayerInformationForTable = new String[numRows][numCols];

        for (int k = 0; k < numRows; k++) {
            MapLayerAttributes mLA = mapLayerAttributesArray[k];
            Object[] attrib = mLA.toObjectArray();

            for (int m = 0; m < Array.getLength( attrib ); m++) {
                //				mapLayerInformationForTable [ k ] [ m ] = attrib [ m ];
                values[k][m] = attrib[m];
            }
        }

        return mapLayerInformationForTable;
    } // End getMapLayerInformationForTable ( b )

    MapLayerAttributes[] toMapLayerAttributesArray( Object[][] m ) {
        if ( m == null )
            return null;

        int numRows = m.length;
        int numCol = m[0].length;

        MapLayerAttributes[] mapLayerAttributesArray = new MapLayerAttributes[numRows];

        for (int i = 0; i < numRows; i++) {
            String title = (String) m[i][0];
            String name = (String) m[i][1];
            String srs = (String) m[i][2];
            double minx = ( (Double) m[i][3] ).doubleValue();
            double miny = ( (Double) m[i][4] ).doubleValue();
            double maxx = ( (Double) m[i][5] ).doubleValue();
            double maxy = ( (Double) m[i][6] ).doubleValue();

            mapLayerAttributesArray[i] = new MapLayerAttributes( title, name, srs, minx, miny,
                maxx, maxy );
        }
        // ----------------------------- ZoomToWMSPLugIn toMapLayerAttributesArray ( )
        return mapLayerAttributesArray;
    } // End toMapLayerAttributesArray ( )

    
    // s, what does this action listener do? Can't it hava proper name?
    public class AL implements ActionListener {
        public void actionPerformed( ActionEvent ae ) {
            if ( ae.getActionCommand().equals( "showInfoTable" ) ) {
                try {
                    showInformationTable( context );
                } catch (Exception e) {
                    System.out.println( "Error in AL" );
                }
            }
        }

    } // End AL

    public static class MapLayerAttributes implements Comparable {

        public static final int SORT_UP = 1;

        public static final int SORT_DOWN = -1;

        public static final int SORT_AFTER_TITLE = 1;

        public static final int SORT_AFTER_NAME = 2;

        public static final int SORT_AFTER_SRS = 3;

        public static final int SORT_AFTER_MINX = 4;

        public static final int SORT_AFTER_MINY = 5;

        public static final int SORT_AFTER_MAXX = 6;

        public static final int SORT_AFTER_MAXY = 7;

        public static int[] sortUpDown = { SORT_DOWN, SORT_DOWN, SORT_DOWN, SORT_DOWN, SORT_DOWN,
                                          SORT_DOWN, SORT_DOWN };

        public static int sortAfter;

        String srs, name, title;

        //		String minx, miny, maxx, maxy;

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


        public int compareTo( Object object ) { // Muss zum Sortieren überschrieben werden.
            int ret = 1;
            MapLayerAttributes mla = (MapLayerAttributes) object;

            if ( sortAfter == SORT_AFTER_TITLE ) {
                ret = title.compareTo( mla.title )
                    * sortUpDown[0];
            }

            else if ( sortAfter == SORT_AFTER_NAME ) {
                ret = name.compareTo( mla.name )
                    * sortUpDown[1];
            }

            else if ( sortAfter == SORT_AFTER_SRS ) {
                ret = srs.compareTo( mla.srs )
                    * sortUpDown[2];
            }

            else if ( sortAfter == SORT_AFTER_MINX ) {
                if ( minx > mla.minx ) {
                    ret = 1 * sortUpDown[3];
                } else {
                    ret = -1
                        * sortUpDown[3];
                }
            }

            else if ( sortAfter == SORT_AFTER_MINY ) {
                if ( miny > mla.miny ) {
                    ret = 1 * sortUpDown[4];
                } else {
                    ret = -1
                        * sortUpDown[4];
                }
            }

            else if ( sortAfter == SORT_AFTER_MAXX ) {
                if ( maxx > mla.maxx ) {
                    ret = 1 * sortUpDown[5];
                } else {
                    ret = -1
                        * sortUpDown[5];
                }
            }

            else if ( sortAfter == SORT_AFTER_MAXY ) {
                if ( maxy > mla.maxy ) {
                    ret = 1 * sortUpDown[6];
                } else {
                    ret = -1
                        * sortUpDown[6];
                }
            }

            return ret;

        } // End compareTo ( )

        double cutDouble( double value, int afterComma ) {
            double mulQuot = Math.pow( 10.d, afterComma );
            long tmp = (long) ( value * mulQuot );
            return tmp
                / mulQuot;
        }


        public static String[] getColumnNames() {
            String[] columNames = { "Title", "Name", "SRS", "MinX", "MinY", "MaxX", "MaxY" };
            return columNames;
        }


        ArrayList getMapLayerRows( MapLayer mapLayer ) {
            double minX, minY, maxX, maxY;

            String srs, name, title;
            String unknown = "Unknown";

            ArrayList mapLayerRows = new ArrayList();

            name = mapLayer.getName();
            if ( name == null )
                name = unknown;
            title = mapLayer.getTitle();
            if ( title == null )
                title = unknown;
/* I think this is not usefull [u.d., 05.11.29]
            BoundingBox latLonBB = mapLayer.getLatLonBoundingBox();

            if ( latLonBB == null ) {
                srs = unknown;
                minX = 0.;
                minY = 0.;
                maxX = 400.;
                maxY = 400.;
            } else {
                srs = "epsg:4326";
System.out.println ( "ZoomToWMS srs: " + srs + "  latLonBBMinX: " + latLonBB.getMinX() );
                minX = cutDouble( latLonBB.getMinX(), 1 );
                minY = cutDouble( latLonBB.getMinY(), 1 );
                maxX = cutDouble( latLonBB.getMaxX(), 1 );
                maxY = cutDouble( latLonBB.getMaxY(), 1 );
            }

            mapLayerRows.add( new MapLayerAttributes( title, name, srs, minX, minY, maxX, maxY ) );
*/
            
            ArrayList boundingBoxList = mapLayer.getAllBoundingBoxList();

            for (int i = 0; i < boundingBoxList.size(); i++) {
                BoundingBox bb = (BoundingBox) boundingBoxList.get( i );

                if ( bb == null ) {
                    srs = unknown;
                    minX = 0.;
                    minY = 0.;
                    maxX = 400.;
                    maxY = 400.;
                } else {
                    srs = bb.getSRS().toLowerCase();
                    /* not so good? [u.d., 05.11.29]
                    minX = cutDouble( bb.getMinX(), 2 );
                    minY = cutDouble( bb.getMinY(), 2 );
                    maxX = cutDouble( bb.getMaxX(), 2 );
                    maxY = cutDouble( bb.getMaxY(), 2 );
                    */
                    // better?
                    minX = bb.getMinX();
                    minY = bb.getMinY();
                    maxX = bb.getMaxX();
                    maxY = bb.getMaxY();
                }

                mapLayerRows
                    .add( new MapLayerAttributes( title, name, srs, minX, minY, maxX, maxY ) );
            } // End for bb

            return mapLayerRows;
        } // End getMapLayerRows ( )

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
            int anzColumns = Array.getLength( this.getColumnNames() );
            Object[] objectArray = new Object[anzColumns];

            objectArray[0] = this.getTitle();
            objectArray[1] = this.getName();
            objectArray[2] = this.getSRS();
            objectArray[3] = Double.valueOf( String.valueOf( this.getMinx() ) );
            objectArray[4] = Double.valueOf( String.valueOf( this.getMiny() ) );
            objectArray[5] = Double.valueOf( String.valueOf( this.getMaxx() ) );
            objectArray[6] = Double.valueOf( String.valueOf( this.getMaxy() ) );

            return objectArray;
        } // End toObjectArray ( )

        public static void setSortAfter( int sortAfter ) {
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_TITLE ) {
                MapLayerAttributes.sortUpDown[0] = -1
                    * MapLayerAttributes.sortUpDown[0];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_NAME ) {
                MapLayerAttributes.sortUpDown[1] = -1
                    * MapLayerAttributes.sortUpDown[1];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_SRS ) {
                MapLayerAttributes.sortUpDown[2] = -1
                    * MapLayerAttributes.sortUpDown[2];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_MINX ) {
                MapLayerAttributes.sortUpDown[3] = -1
                    * MapLayerAttributes.sortUpDown[3];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_MINY ) {
                MapLayerAttributes.sortUpDown[4] = -1
                    * MapLayerAttributes.sortUpDown[4];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_MAXX ) {
                MapLayerAttributes.sortUpDown[5] = -1
                    * MapLayerAttributes.sortUpDown[5];
            }
            if ( sortAfter == MapLayerAttributes.SORT_AFTER_MAXY ) {
                MapLayerAttributes.sortUpDown[6] = -1
                    * MapLayerAttributes.sortUpDown[6];
            }

            MapLayerAttributes.sortAfter = sortAfter;
        } // End setSortAfter ( )

    } // End MapLayerAttributes

    class InfoTableModel extends AbstractTableModel {

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

    } // End InfoTableModel

    class MASort extends MouseAdapter { // Mausadapter für Spaltensortierung

        public void mousePressed( MouseEvent me ) {
            if ( me.getButton() == MouseEvent.BUTTON3 ) {
                int viewColumn = tcm.getColumnIndexAtX( me.getX() );
                int column = infoTable.convertColumnIndexToModel( viewColumn );

                if ( column == 0 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_TITLE );
                } else if ( column == 1 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_NAME );
                }

                else if ( column == 2 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_SRS );
                }

                else if ( column == 3 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_MINX );
                }

                else if ( column == 4 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_MINY );
                }

                else if ( column == 5 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_MAXX );
                }

                else if ( column == 6 ) {
                    sortTable( MapLayerAttributes.SORT_AFTER_MAXY );
                }

            }

        }

    } // Ende MASpasKu


} // End ZoomToWMSPLugIn