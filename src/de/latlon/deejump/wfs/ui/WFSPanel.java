/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */
package de.latlon.deejump.wfs.ui;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;


import org.deegree.datatypes.QualifiedName;
import org.deegree.model.spatialschema.GMLGeometryAdapter;
import org.deegree.model.spatialschema.Geometry;
import org.deegree.model.spatialschema.GeometryFactory;
import org.deegree.ogcwebservices.wfs.capabilities.WFSFeatureType;
import org.saig.core.gui.swing.sldeditor.util.FormUtils;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

import de.latlon.deejump.wfs.DeeJUMPException;
import de.latlon.deejump.wfs.auth.LoginDialog;
import de.latlon.deejump.wfs.auth.UserData;
import de.latlon.deejump.wfs.client.AbstractWFSWrapper;
import de.latlon.deejump.wfs.client.WFSClientHelper;
import de.latlon.deejump.wfs.client.WFServiceWrapper_1_0_0;
import de.latlon.deejump.wfs.client.WFServiceWrapper_1_1_0;
import de.latlon.deejump.wfs.deegree2mods.XMLFragment;
import de.latlon.deejump.wfs.i18n.I18N;
import de.latlon.deejump.wfs.plugin.WFSPlugIn;

/**
 * This is a panel which contains other basic GUIs for accessing Features of a WFS.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: edso $
 * 
 */
public class WFSPanel extends JPanel {

    private static final long serialVersionUID = 8204179552311582569L;

    // Constants for spatial search criteria type
    // also used by child panels
    /** Search uses no spatial criteria */
    public static final String NONE = "NONE";

    /** Search uses bounding box as spatial criteria */
    public static final String BBOX = "BBOX";

    /** Search uses a selected (GML) geometry as spatial criteria */
    public static final String SELECTED_GEOM = "SELECTED_GEOM";

    static File lastDirectory;

    /**
     * The standard geometry type name (used when getting schemata and creating filters with spatial clauses
     */
    public static final String GEOMETRY_PROPERTY_NAME = "GEOM";

    /** The panel containing the interface for attribute-based search */
    protected PropertyCriteriaPanel attributeResPanel;

    /** The panel containing the interface for geometry-based search */
    protected SpatialCriteriaPanel spatialResPanel;

    String[] attributeNames = new String[] {};

    protected QualifiedName[] geoProperties;

    private QualifiedName geoProperty;

    AbstractWFSWrapper wfService;

    protected RequestPanel requestTextArea;

    protected JTextArea responseTextArea;

    protected JComboBox serverCombo;

    // private JButton okButton;

    private JTabbedPane tabs;

    private JComboBox featureTypeCombo;
    
    private List<Component> advancedTabs = new ArrayList<Component>();

    // use deegree Envelope
    /** The envelope of the current bounding box */
    private org.deegree.model.spatialschema.Envelope envelope;

    // private GMLGeometry gmlBbox;
    private Geometry selectedGeom;

    protected String srs = "EPSG:4326";

    protected PropertySelectionPanel propertiesPanel;

    private JButton capabilitiesButton;

    protected String wfsVersion;

    public String getWfsVersion() {
      return wfsVersion;
    }

    public void setWfsVersion(String wfsVersion) {
      this.wfsVersion = wfsVersion;
    }

    protected WFSOptions options;

    WFSPanelButtons controlButtons;

    private JTextArea xmlPane;

    private UserData logins;

    private WorkbenchContext context;
    
    /**
     * @param context
     * @param urlList
     *            the list of servers
     */
    public WFSPanel( WorkbenchContext context ) {
        this.context = context;
        initGUI();
        this.options = new WFSOptions();
    }
    
    private void addAdvancedTab( String name, Component component){
      component.setName(name);
      advancedTabs.add(component);
    }
    
    private void showAdvancedTabs(){
      // add adv tabs
      for (Component component : advancedTabs) {
        tabs.add(component);
      }
      GUIUtil.centreOnWindow(SwingUtilities.windowForComponent(this));
    }
    
    private void hideAdvancedTabs(){
      // remove advanced tabs, all after tab 1
      while (tabs.getTabCount()>1){
        int i = tabs.getTabCount()-1;
        tabs.removeTabAt(i);;
      }
    }

    private void initGUI() {
  
      this.setLayout(new GridBagLayout());
  
      JPanel urlPanel = new JPanel();
      urlPanel.setLayout(new GridBagLayout());
  
      // combo box for WFS URLs
      serverCombo = createServerCombo();
  
      String txt = I18N.get("FeatureResearchDialog.wfsService");
      serverCombo.setBorder(BorderFactory.createTitledBorder(
          BorderFactory.createEmptyBorder(20, 5, 5, 5), txt));
      txt = I18N.get("FeatureResearchDialog.wfsServiceToolTip");
      serverCombo.setToolTipText(txt);
  
      // reset serverlist on triple rightmouseclick (for testing purposes)
      serverCombo.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 3) {
            String[] urls = WFSPlugIn.createUrlList(true);
            serverCombo.setModel(new JComboBox(urls).getModel());
          }
          super.mouseClicked(e);
        }
      });
  
      FormUtils.addRowInGBL(urlPanel, 0, 0, serverCombo);
  
      // connect and capabilities button
      JButton connecButton = new JButton(
          I18N.get("FeatureResearchDialog.connect"));
      connecButton.setAlignmentX(0.5f);
      connecButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            reinitService((String) serverCombo.getSelectedItem());
          } catch (DeeJUMPException e1) {
            context.getWorkbench().getFrame().handleThrowable(e1);
          }
        }
      });
  
      capabilitiesButton = new JButton(
          I18N.get("FeatureResearchDialog.capabilities"));
      capabilitiesButton.setEnabled(false);
      capabilitiesButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          createXMLFrame(WFSPanel.this, wfService.getCapabilitesAsString());
        }
      });
  
      JButton loginButton = new JButton(I18N.get("FeatureResearchDialog.login"));
      loginButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          performLogin();
        }
      });
  
      loginButton.setEnabled(context != null);
  
      JButton saveButton = new JButton(I18N.get("General.save"));
      if (context != null) {
        saveButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            JFileChooser jfc = new JFileChooser();
            if (lastDirectory != null) {
              jfc.setCurrentDirectory(lastDirectory);
            }
            int i = jfc.showSaveDialog(WFSPanel.this);
            if (i == APPROVE_OPTION) {
              try {
                String txt = WFSClientHelper.createResponseStringfromWFS(getWfService()
                    .getGetFeatureURL(), getRequest());
                FileWriter fw = new FileWriter(jfc.getSelectedFile());
                fw.write(txt);
                fw.close();
                lastDirectory = jfc.getSelectedFile().getParentFile();
              } catch (Exception e) {
                showMessageDialog(WFSPanel.this, e.getMessage(), "Error!",
                    ERROR_MESSAGE);
                e.printStackTrace();
              }
  
            }
  
          }
        });
      }
  
      JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
      // version buttons
      p.add(createVersionButtons(new String[] { "1.0.0", "1.1.0" }));
      FormUtils.addRowInGBL(urlPanel, 1, 0, p);
  
      JPanel innerPanel = new JPanel();
      innerPanel.add(connecButton);
      innerPanel.add(capabilitiesButton);
      innerPanel.add(loginButton);
      if (context != null) {
        innerPanel.add(saveButton);
      }
      FormUtils.addRowInGBL(urlPanel, 2, 0, innerPanel);
  
      featureTypeCombo = createFeatureTypeCombo();
      // featureTypeCombo.setVisible( false );
      featureTypeCombo.setEnabled(false);
      FormUtils.addRowInGBL(urlPanel, 3, 0, featureTypeCombo);
  
      // FIXME what's this???
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  
      tabs = new JTabbedPane() {
        @Override
        public Component add(Component component) {
          super.add(component.getName(), component);
          return component;
        }
      };
  
      JPanel keepNorth = new JPanel(new GridBagLayout());
      keepNorth.add(urlPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1,
          GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,
              0, 0, 0), 0, 0));
      tabs.add(I18N.get("FeatureResearchDialog.wfsConnection"), keepNorth);
  
      attributeResPanel = new PropertyCriteriaPanel(this, featureTypeCombo);
      keepNorth = new JPanel(new GridBagLayout());
      keepNorth.add(attributeResPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1,
          GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,
              0, 0, 0), 0, 0));
      addAdvancedTab(I18N.get("FeatureResearchDialog.attributeSearch"), keepNorth);
  
      propertiesPanel = new PropertySelectionPanel(this);
      addAdvancedTab(I18N.get("FeatureResearchDialog.properties"),
          propertiesPanel);
  
      spatialResPanel = new SpatialCriteriaPanel(this);
      addAdvancedTab(I18N.get("FeatureResearchDialog.spatialSearch"),
          spatialResPanel);
  
      requestTextArea = new RequestPanel(this);
      addAdvancedTab(I18N.get("FeatureResearchDialog.request"), requestTextArea);
  
      addAdvancedTab(I18N.get("FeatureResearchDialog.response"),
          createResponseTextArea());
  
      add(tabs, new GridBagConstraints(0, 0, 1, 1, 1, 1,
          GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0,
              0), 0, 0));
    }

    /** Initializes the FeatureType combo box of the AttributeResearchPanel */

    private void refreshGUIs() {

        String[] featTypes = null;
        requestTextArea.setRequestText( "" );
        responseTextArea.setText( "" );
        try {
            featTypes = wfService.getFeatureTypes();
            Arrays.sort( featTypes );
            featureTypeCombo.setModel( new javax.swing.DefaultComboBoxModel( featTypes ) );

            controlButtons.okButton.setEnabled( true );
            capabilitiesButton.setEnabled( true );

            featureTypeCombo.setEnabled( true );
            // featureTypeCombo.setVisible( true );
            attributeResPanel.setFeatureTypeComboEnabled( true );

        } catch ( Exception e ) {
            context.getWorkbench().getFrame().handleThrowable(e, this);
            //e.printStackTrace();

            // reset featuretype list
            featureTypeCombo.setModel( new javax.swing.DefaultComboBoxModel( new String[0] ) );
            attributeResPanel.setFeatureTypeComboEnabled( false );

            hideAdvancedTabs();
        }

        if ( featTypes != null && featTypes.length > 0 ) {
            try {
                attributeNames = wfService.getProperties( featTypes[0] );

                geoProperties = wfService.getGeometryProperties( featTypes[0] );

                propertiesPanel.setProperties( attributeNames, geoProperties );

                spatialResPanel.resetGeoCombo( geoProperties );

                // /hmmm repeated code...
                WFSFeatureType ft = wfService.getFeatureTypeByName( featTypes[0] );
                // TODO: UT could support other srs, but not doing it now
                if ( ft != null ) {
                    String[] crs = new String[] { ft.getDefaultSRS().toString() };
                    srs = crs[0];
                    spatialResPanel.setCrs( crs );
                }
                
                showAdvancedTabs();
            } catch ( Exception e ) {
                e.printStackTrace();

                hideAdvancedTabs();

                controlButtons.okButton.setEnabled( true );

                JOptionPane.showMessageDialog( this, "Could not get DescribeFeatureType for '" + featTypes[0]
                                                     + "' from WFS server at \n'" + wfService.getBaseWfsURL() + "'\n"
                                                     + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
            }
        }

    }

    
    protected void performLogin() {
        LoginDialog dialog = new LoginDialog( (Dialog) this.getTopLevelAncestor(),
                                              I18N.get( "FeatureResearchDialog.login" ),
                                              serverCombo.getSelectedItem().toString() );

        if (dialog.wasCancelled()) return;
        
        String pass = dialog.getPassword();
        String user = dialog.getName();

        if ( user == null || user.equals( "" ) ) {
            JOptionPane.showMessageDialog( this, I18N.get( "WFSPanel.userEmpty" ) );
            return;
        }

        if ( pass == null || pass.equals( "" ) ) {
            JOptionPane.showMessageDialog( this, I18N.get( "WFSPanel.passEmpty" ) );
            return;
        }

        logins = new UserData( user, pass );
        // logins = new UserData(user, pass);
        context.getBlackboard().put( "LOGINS", logins );

        try {
            reinitService( (String) serverCombo.getSelectedItem() );
        } catch ( DeeJUMPException ex ) {
            String msg = I18N.get( "WFSPanel.loginFailed" ) + "\n" + ex.getLocalizedMessage() + "\n"
                         + I18N.get( "WFSPanel.loginFailed2" );
            JOptionPane.showMessageDialog( this, msg );
            ex.printStackTrace();
            return;
        }

        JOptionPane.showMessageDialog( this, I18N.get( "WFSPanel.loginSuccessful" ) );
    }

    // Gh 15.11.05
    private JComboBox createServerCombo() {
      List<String>  servers = new ArrayList<String>(Arrays.asList(WFSPlugIn.createUrlList(false)));
  
      if (wfService != null) {
        servers.add(0, wfService.getCapabilitiesURL());
      }

      final ExtensibleComboBox extensibleComboBox = new ExtensibleComboBox(servers.toArray());
      extensibleComboBox.setSelectedIndex(0);

      return extensibleComboBox;
    }

    JTabbedPane getTabs() {
        return this.tabs;
    }

    protected void createXMLFrame( final Component parent, String txt ) {
        if ( txt == null )
            txt = "";

        // try to beautify the XML
        XMLFragment doc = new XMLFragment();
        try {
            doc.load( new StringReader( txt ), "http://www.systemid.org" );
            txt = doc.getAsPrettyString();
        } catch ( SAXException e ) {
            // ignore and use the old text
        } catch ( IOException e ) {
            // ignore and use the old text
        }

        if ( xmlPane == null ) {
            xmlPane = new JTextArea( txt );
        } else {
            xmlPane.setText( txt );
        }

        // ta.setLineWrap( true );
        JScrollPane sp = new JScrollPane( xmlPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED );
//        sp.setMaximumSize( new Dimension( 600, 400 ) );
//        sp.setPreferredSize( new Dimension( 800, 400 ) );

        String[] opts = new String[] { I18N.get( "closeAndSave" ), I18N.get( "OK" ) };
        
        JOptionPane pane = new JOptionPane(sp, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.YES_NO_OPTION, null,
            opts, opts[1]);
        pane.selectInitialValue();
        JDialog dialog = pane.createDialog(parent,  I18N.get( "FeatureResearchDialog.capabilities" ));
        GUIUtil.centreOnScreen(dialog);

        dialog.show();
        dialog.dispose();
        
//        int i = JOptionPane.showOptionDialog( parent, sp, I18N.get( "FeatureResearchDialog.capabilities" ),
//                                              JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[1] );

        if ( opts[0].equals(pane.getValue()) ) { // save our capabilities!
            WFSPanel.saveTextToFile( parent, xmlPane.getText() );
        }
    }

    protected void reinitService( String url )
                            throws DeeJUMPException {
        wfService = "1.1.0".equals( this.wfsVersion ) ? new WFServiceWrapper_1_1_0( logins, url )
                                                     : new WFServiceWrapper_1_0_0( logins, url );
        refreshGUIs();
    }

    private Component createVersionButtons( String[] versions ) {
        JPanel p = new JPanel();

        p.add( new JLabel( I18N.get( "FeatureResearchDialog.version" ) ) );
        ButtonGroup bg = new ButtonGroup();
        for ( int i = 0; i < versions.length; i++ ) {
            final JRadioButton b = new JRadioButton( versions[i] );
            b.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    wfsVersion = b.getText();
                    // automatically switch to the default format for the version
                    if ( options != null ) {
                        if ( wfsVersion.equals( "1.0.0" ) ) {
                            options.setSelectedOutputFormat( options.getOutputFormats()[0] );
                        } else {
                            options.setSelectedOutputFormat( options.getOutputFormats()[1] );
                        }
                    }
                }
            } );
            bg.add( b );
            if ( i == 0 ) {// first is clicked
                b.doClick();
            }
            p.add( b );
        }
        return p;
    }

    private JComboBox createFeatureTypeCombo() {
        String[] start = { "            " };
        JComboBox tmpFeatureTypeCombo = new JComboBox( start );

        Border border = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder(20, 5, 5, 5), I18N.get( "FeatureResearchDialog.featureType" ) );

        tmpFeatureTypeCombo.setBorder( border );
        tmpFeatureTypeCombo.addActionListener( new java.awt.event.ActionListener() {
            public void actionPerformed( java.awt.event.ActionEvent evt ) {

                JComboBox combo = (JComboBox) evt.getSource();
                String selectFt = (String) combo.getSelectedItem();
                try {
                    attributeNames = wfService.getProperties( selectFt );

                    attributeResPanel.refreshPanel();
                    geoProperties = getGeoProperties();
                    propertiesPanel.setProperties( attributeNames, geoProperties );
                    spatialResPanel.resetGeoCombo( geoProperties );
                    // /hmmm repeated code...
                    WFSFeatureType ft = wfService.getFeatureTypeByName( selectFt );
                    if ( ft != null ) {
                        // UT could support other srrs, but not doing it now
                        String[] crs = new String[] { ft.getDefaultSRS().toString() };
                        srs = crs[0];
                        spatialResPanel.setCrs( crs );
                    }
                    requestTextArea.setRequestText( "" );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog( WFSPanel.this, "Error loading schema: " + e.getMessage() );
                }
            }
        } );

        return tmpFeatureTypeCombo;
    }

    private JComponent createResponseTextArea() {

        JPanel p = new JPanel();
        p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );

        responseTextArea = new JTextArea();
        responseTextArea.setLineWrap( true );
        responseTextArea.setWrapStyleWord( true );
        responseTextArea.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        JScrollPane jsp = new JScrollPane( responseTextArea );

        p.add( jsp );

        return p;
    }

    /** Creates a GetFeature request by concatenation of xlm elements */
    private StringBuffer concatenateRequest() {

        StringBuffer sb = new StringBuffer();
        if ( wfService == null ) {// not inited yet
            return sb;
        }
        sb.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

        final String outputFormat = options.getSelectedOutputFormat();

        sb.append( "<wfs:GetFeature xmlns:ogc=\"http://www.opengis.net/ogc\" " );
        if ( logins != null && logins.getUsername() != null && logins.getPassword() != null ) {
            sb.append( "user=\"" + logins.getUsername() + "\" password=\"" + logins.getPassword() + "\" " );
        }
        sb.append( "xmlns:gml=\"http://www.opengis.net/gml\" " );
        sb.append( "xmlns:wfs=\"http://www.opengis.net/wfs\" service=\"WFS\" " );
        sb.append( "version=\"" ).append( wfService.getServiceVersion() ).append( "\" " );
        sb.append( "maxFeatures=\"" ).append( options.getMaxFeatures() ).append( "\" " );
        sb.append( "outputFormat=\"" ).append( outputFormat ).append( "\">" ).append( "<wfs:Query " );

        String ftName = (String) featureTypeCombo.getSelectedItem();
        QualifiedName ft = wfService.getFeatureTypeByName( ftName ).getName();

        if ( ft.getPrefix() != null && ft.getPrefix().length() > 0 ) {
            sb.append( "xmlns:" ).append( ft.getPrefix() ).append( "=\"" ).append( ft.getNamespace() );
            sb.append( "\" " );
        }
        sb.append( "srsName=\"" ).append( srs ).append( "\" " );
        sb.append( "typeName=\"" );

        String prefix = ft.getPrefix();
        if ( prefix != null && prefix.length() > 0 ) {
            sb.append( prefix ).append( ":" );
        }
        sb.append( ft.getLocalName() ).append( "\">" );

        sb.append( propertiesPanel.getXmlElement() );

        String spatCrit = attributeResPanel.getSpatialCriteria();

        int listSize = attributeResPanel.getListSize();

        String[] filterTags = new String[] { "", "" };

        if ( listSize > 0 || !NONE.equals( spatCrit ) ) {
            filterTags = createStartStopTags( "Filter" );
        }
        sb.append( filterTags[0] );

        boolean includesSpatialClause = !NONE.equals( spatCrit );
        if ( includesSpatialClause && listSize > 0 ) {
            sb.append( WFSPanel.createStartStopTags( "And" )[0] );
        }

        if ( BBOX.equals( spatCrit ) ) {
            sb.append( createBboxGml() );
        } else if ( SELECTED_GEOM.equals( spatCrit ) ) {
            sb.append( spatialResPanel.getXmlElement() );
        }

        sb.append( attributeResPanel.getXmlElement() );
        if ( includesSpatialClause && listSize > 0 ) {
            sb.append( WFSPanel.createStartStopTags( "And" )[1] );
        }

        sb.append( filterTags[1] );
        sb.append( "</wfs:Query></wfs:GetFeature>" );
        return sb;
    }

      /**
     * Creates the XML fragment containing a bounding box filter
     * 
     * @return the XML fragment containing a bounding box filter
     */
    private StringBuffer createBboxGml() {
  
      if (envelope == null)
        return new StringBuffer();
  
      // StringBuffer sb = new StringBuffer( 500 );
      //
      // QualifiedName ft = getFeatureType();
      // QualifiedName qn = getChosenGeoProperty();
  
      // if ( envelope != null ) {
      // sb.append( "<ogc:BBOX>" ).append( "<ogc:PropertyName>" ).append(
      // ft.getPrefix() ).append( ":" );
      // sb.append( qn.getLocalName() ).append( "</ogc:PropertyName>" ).append(
      // "<gml:Box><gml:coord>" );
      // sb.append( "<gml:X>" ).append( envelope.getMinX() ).append( "</gml:X>"
      // ).append( "<gml:Y>" );
      // sb.append( envelope.getMinY() ).append( "</gml:Y>" ).append(
      // "</gml:coord><gml:coord>" );
      // sb.append( "<gml:X>" ).append( envelope.getMaxX() ).append( "</gml:X>"
      // ).append( "<gml:Y>" );
      // sb.append( envelope.getMaxY() ).append( "</gml:Y>" ).append(
      // "</gml:coord></gml:Box></ogc:BBOX>" );
      // }
  
      if (getWfsVersion().equals("1.0.0")) {
        return GMLGeometryAdapter.exportAsBox(envelope);
      } else {
        return GMLGeometryAdapter.exportAsEnvelope(envelope);
      }
  
    }

    /**
     * @param compo
     * @param txt
     */
    public static void saveTextToFile( Component compo, String txt ) {

        JFileChooser jfc = new JFileChooser();
        if ( lastDirectory != null ) {
            jfc.setCurrentDirectory( lastDirectory );
        }
        int i = jfc.showSaveDialog( compo );
        if ( i == JFileChooser.APPROVE_OPTION ) {
            try {

                FileWriter fw = new FileWriter( jfc.getSelectedFile() );
                fw.write( txt );
                fw.close();
                lastDirectory = jfc.getSelectedFile().getParentFile();
            } catch ( Exception e ) {
                JOptionPane.showMessageDialog( compo, e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE );
                e.printStackTrace();
            }

        }

    }

    /**
     * Convenience method to create XML tags mit "ogc" namespace. For example an input like MyTag will return
     * <code>{"<ogc:MyTag>", "</ogc:MyTag>"}</code>
     * 
     * @param tagName
     *            the tag name
     * @return a String[] with start and stop tags
     */
    protected static final String[] createStartStopTags( String tagName ) {
        String[] tags = new String[] { "<ogc:" + tagName + ">", "</ogc:" + tagName + ">" };
        return tags;
    }

    /**
     * @return the geometry property name
     */
    public QualifiedName getChosenGeoProperty() {
        return geoProperty;
    }

    /**
     * @param geoProp
     */
    public void setGeoProperty( QualifiedName geoProp ) {
        this.geoProperty = geoProp;
    }

    /**
     * @return the geometry property names
     */
    public QualifiedName[] getGeoProperties() {
        return this.wfService.getGeometryProperties( (String) featureTypeCombo.getSelectedItem() );
    }

    /**
     * Returns the currently chosen feature type
     * 
     * @return the name of the currently chosen feature type
     */
    public QualifiedName getFeatureType() {
        String s = (String) featureTypeCombo.getSelectedItem();
        QualifiedName qn = null;
        WFSFeatureType ft = wfService.getFeatureTypeByName( s );
        if ( ft != null ) {
            qn = ft.getName();
        }
        return qn;
    }

    /**
     * @return the client
     */
    public AbstractWFSWrapper getWfService() {
        return this.wfService;
    }

    /**
     * Returns the currently selected geometry that serves as basis for spatial operation operations
     * 
     * @return the currently selected geometry
     */
    protected Geometry getSelectedGeometry() {
        return this.selectedGeom;
    }

    /**
     * @return the current list of servers
     */
    public List<String> getWFSList() {
        LinkedList<String> list = new LinkedList<String>();

        String sel = serverCombo.getSelectedItem().toString();
        list.add( sel );

        for ( int i = 0; i < serverCombo.getItemCount(); ++i ) {
            String s = (String) serverCombo.getItemAt( i );
            if ( s != null && !sel.equals( s ) )
                list.add( s );
        }
        return list;
    }

    protected void setResponseText( String txt ) {
        responseTextArea.setText( txt );
        responseTextArea.setCaretPosition( 0 );
        // activate it, it's the last one
        tabs.setSelectedIndex(tabs.getTabCount()-1);
    }

    /**
     * @return the request
     */
    public String getRequest() {
        String t = requestTextArea.getText();
        if ( t == null || t.length() == 0 ) {
            t = concatenateRequest().toString();
        }
        return t.replaceAll( "\n", "" );
    }

    protected String createRequest() {
        String t = concatenateRequest().toString();
        return t.replaceAll( "\n", "" );
    }

    protected void setTabsVisible( boolean visible ) {
        tabs.setVisible( visible );
    }

    protected WFSOptions getOptions() {
        return this.options;
    }

    protected String getResponse() {
        return this.responseTextArea.getText();
    }

    /**
     * @return the srs
     */
    public String getGMLGeometrySRS() {
        return this.srs;
    }

    /**
     * @param env
     */
    public void setEnvelope( Envelope env ) {
      // TODO: respect SRS
      this.envelope = GeometryFactory.createEnvelope(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), null);
    }

    /**
     * @param geom
     */
    public void setComparisonGeometry( Geometry geom ) {
        this.selectedGeom = geom;
    }

}
