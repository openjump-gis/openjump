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

package com.vividsolutions.wms.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.wms.*;

/**
 * The executable WMS Viewer.
 */
public class WMSViewer extends JFrame implements ActionListener, MouseListener {

  private JTextField serverUrlField;
  private JButton connectButton;
  private JButton disconnectButton;
  private JList layerList;
  private JTextField xMinField;
  private JTextField yMinField;
  private JTextField xMaxField;
  private JTextField yMaxField;
  private JComboBox srsCombo;
  private JButton getImageButton;
  private JComboBox formatCombo;
  private ImageCanvas canvas;
  private JLabel mapLabel;
  private String zoomMode;
  private WMService service;
  private boolean connected;

  /**
   * Constructs a WMSViewer object.
   */
  public WMSViewer() {
    super( I18N.get("com.vividsolutions.wms.ui.WMSViewer.wms-viewer") );
    this.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    getContentPane().setLayout( new BorderLayout() );

    // create and layout the top panel (server connect)
    JPanel topPanel = new JPanel();
    topPanel.setLayout( new FlowLayout() );
    topPanel.add( new JLabel( I18N.get("com.vividsolutions.wms.ui.WMSViewer.server-string")) );
    serverUrlField = new JTextField( I18N.get("com.vividsolutions.wms.ui.WMSViewer.wms-url"), 30 );
    topPanel.add( serverUrlField );
    connectButton = new JButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.connect") );
    connectButton.setActionCommand( "connect" );
    connectButton.addActionListener( this );
    topPanel.add( connectButton );
    disconnectButton = new JButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.disconnect") );
    disconnectButton.setActionCommand( "disconnect" );
    disconnectButton.addActionListener( this );
    disconnectButton.setEnabled( false );
    topPanel.add( disconnectButton );
    getContentPane().add( topPanel, BorderLayout.NORTH );

    // create and layout the left side panel (layer list)
    layerList = new JList();
    layerList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    getContentPane().add( new JScrollPane( layerList ), BorderLayout.WEST );

    // create and layout the right side panel (bounding box, pan and zoom controls)
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout( new BorderLayout() );
    JPanel thePanel = new JPanel();
    thePanel.setLayout( new GridLayout( 5, 2 ) );
    thePanel.add( new JLabel( "X-Min:" ) );
    xMinField = new JTextField( "1375470", 7 );
    thePanel.add( xMinField );
    thePanel.add( new JLabel( "Y-Min:" ) );
    yMinField = new JTextField( "701069", 7 );
    thePanel.add( yMinField );
    thePanel.add( new JLabel( "X-Max:" ) );
    xMaxField = new JTextField( "1401720", 7 );
    thePanel.add( xMaxField );
    thePanel.add( new JLabel( "Y-Max:" ) );
    yMaxField = new JTextField( "714194", 7 );
    thePanel.add( yMaxField );
    thePanel.add( new JLabel( "SRS:" ) );
    srsCombo = new JComboBox();
    thePanel.add( srsCombo );
    rightPanel.add( thePanel, BorderLayout.NORTH );
    thePanel = new JPanel();
    thePanel.setLayout( new GridLayout( 3, 1 ) );
    ButtonGroup bg = new ButtonGroup();
    JRadioButton rb = new JRadioButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.pan") );
    rb.setActionCommand( "pan" );
    rb.addActionListener( this );
    rb.setSelected( true );
    zoomMode = "pan";
    thePanel.add( rb );
    bg.add( rb );
    rb = new JRadioButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.zoom-in") );
    rb.setActionCommand( "zoomIn" );
    rb.addActionListener( this );
    thePanel.add( rb );
    bg.add( rb );
    rb = new JRadioButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.zoom-out"));
    rb.setActionCommand( "zoomOut" );
    rb.addActionListener( this );
    thePanel.add( rb );
    bg.add( rb );
    rightPanel.add( thePanel, BorderLayout.SOUTH );
    getContentPane().add( rightPanel, BorderLayout.EAST );


    // create and layout the bottom panel (get image control)
    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout( new FlowLayout() );
    formatCombo = new JComboBox();
    bottomPanel.add( formatCombo );
    getImageButton = new JButton( I18N.get("com.vividsolutions.wms.ui.WMSViewer.get-image") );
    getImageButton.setActionCommand( "getImage" );
    getImageButton.addActionListener( this );
    getImageButton.setEnabled( false );
    bottomPanel.add( getImageButton );
    getContentPane().add( bottomPanel, BorderLayout.SOUTH );

    // create the center panel
    canvas = new ImageCanvas();
    canvas.addMouseListener( this );
    getContentPane().add( canvas, BorderLayout.CENTER );

    this.pack();
    this.setVisible( true );
  }

  /**
   * Processes actions from the buttons on the interface
   * @param actionEvent the event to process
   */
  public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
    String command = actionEvent.getActionCommand();
    if( command.equals( "connect" ) ) {
      try {
        // service = new WMService( "http://hydra/mapserv/mapserv?map=/home/chodgson/www/demo/demo.map&" );
        // service = new WMService( "http://slkapps2.env.gov.bc.ca/servlet/com.esri.wms.Esrimap?" );
        service = new WMService( serverUrlField.getText() );
        service.initialize();
        // DEBUG: System.out.print( serv.getTopLayer().toString() );

        // setup the layerList
        Iterator it = service.getCapabilities().getTopLayer().getLayerList().iterator();
        DefaultListModel lm = new DefaultListModel();
        while( it.hasNext() ) {
          lm.addElement( ((MapLayer)it.next()).getName() );
        }
        layerList.setModel( lm );

        // setup the srsCombo
        it = service.getCapabilities().getTopLayer().getSRSList().iterator();
        DefaultComboBoxModel cm = new DefaultComboBoxModel();
        while( it.hasNext() ) {
          cm.addElement( it.next() );
        }
        srsCombo.setModel( cm );

        // setup the formatCombo
        String[] formats = service.getCapabilities().getMapFormats();
        cm = new DefaultComboBoxModel();
        for( int i = 0; i < formats.length; i++  ) {
          cm.addElement( formats[i] );
        }
        formatCombo.setModel( cm );

        connectButton.setEnabled( false );
        disconnectButton.setEnabled( true );
        getImageButton.setEnabled( true );
        connected = true;
      } catch( IOException ioe ) {
        // failed to connect and retrieve capabilities

      }
    } else if ( command.equals( "disconnect" ) ) {
      layerList.setModel( new DefaultListModel() );
      srsCombo.setModel( new DefaultComboBoxModel() );
      formatCombo.setModel( new DefaultComboBoxModel() );
      getImageButton.setEnabled( false );
      disconnectButton.setEnabled( false );
      connectButton.setEnabled( true );
      canvas.setImage( null );
    } else if( command.equals( "getImage" ) ) {
        MapRequest req = service.createMapRequest();
        ArrayList<String> layerNames = new ArrayList<String>();
        for (Object o : layerList.getSelectedValues()) layerNames.add(o.toString());
        req.setImageSize( canvas.getWidth(), canvas.getHeight() );
        req.setFormat( (String)formatCombo.getSelectedItem() );
        req.setLayerNames(layerNames);
        // req.setBoundingBox( new BoundingBox( "EPSG:42102", 1100000, 430000, 1120000, 450000 ) );
        req.setBoundingBox( new BoundingBox( (String)srsCombo.getSelectedItem(),
                                              Float.parseFloat( xMinField.getText() ),
                                              Float.parseFloat( yMinField.getText() ),
                                              Float.parseFloat( xMaxField.getText() ),
                                              Float.parseFloat( yMaxField.getText() ) ) );
        Image mapImage;
        try {
//          System.out.println("Url: " + req.getURL());
          mapImage = req.getImage();
        }
        catch (Exception e) {
          e.printStackTrace(System.err);
          return;
        }
        canvas.setImage( mapImage );
        canvas.repaint();
    } else if( command.equals( "pan" ) ) {
      zoomMode = "pan";
    } else if( command.equals( "zoomIn" ) ) {
      zoomMode = "zoomIn";
    } else if( command.equals( "zoomOut" ) ) {
      zoomMode = "zoomOut";
    }
  }

  public void mouseClicked( java.awt.event.MouseEvent mouseEvent ) {
    if( mouseEvent.getComponent() == canvas ) {
      int x = mouseEvent.getX();
      int y = mouseEvent.getY();
      float xMin = Float.parseFloat( xMinField.getText() );
      float yMin = Float.parseFloat( yMinField.getText() );
      float xMax = Float.parseFloat( xMaxField.getText() );
      float yMax = Float.parseFloat( yMaxField.getText() );
      float mapWidth = xMax - xMin;
      float mapHeight = yMax - yMin;
      int imgWidth = canvas.getWidth();
      int imgHeight = canvas.getHeight();
      float xCenter = xMin +((((float)x)/(float)imgWidth) * mapWidth);
      float yCenter = yMax - ((((float)y)/(float)imgHeight) * mapHeight);
      if( zoomMode.equals( "zoomIn" ) ) {
        mapWidth /= 2;
        mapHeight /= 2;
      } else if( zoomMode.equals( "zoomOut" ) ) {
        mapWidth *= 2;
        mapHeight *= 2;
      }
      xMin = xCenter - mapWidth/2;
      yMin = yCenter - mapHeight/2;
      xMax = xCenter + mapWidth/2;
      yMax = yCenter + mapHeight/2;
      xMinField.setText( "" + xMin );
      yMinField.setText( "" + yMin );
      xMaxField.setText( "" + xMax );
      yMaxField.setText( "" + yMax );

      getImageButton.doClick();
    }
  }

  public void mouseEntered( java.awt.event.MouseEvent mouseEvent ) {
  }

  public void mouseExited( java.awt.event.MouseEvent mouseEvent ) {
  }

  public void mousePressed( java.awt.event.MouseEvent mouseEvent ) {
  }

  public void mouseReleased( java.awt.event.MouseEvent mouseEvent ) {
  }


  /**
   * Runs the viewer aplication
   */
  public static void main( String args[] ) {
    WMSViewer viewer = new WMSViewer();
  }

}
