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

package com.vividsolutions.jump.workbench.ui.plugin.wms;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;
import com.vividsolutions.wms.MapImageFormatChooser;
import com.vividsolutions.wms.WMService;


public class URLWizardPanel extends JPanel implements WizardPanel {
    public static final String SERVICE_KEY = "SERVICE";
    public static final String FORMAT_KEY = "FORMAT";
    public static final String URL_KEY = "URL";
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();
    private Map dataMap;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JLabel urlLabel = new JLabel();
    private JComboBox urls;
    private JPanel fillerPanel = new JPanel();
//  [UT]
    public static final String VERSION_KEY = "WMS_VERSION";
    private String wmsVersion = WMService.WMS_1_1_1;
    private boolean lossyPreferred = true;
    
    public URLWizardPanel(String[] initialURLs, String wmsVersion) {
        try {
            this.wmsVersion = wmsVersion;
            urls = new JComboBox(initialURLs);
            urls.setEditable(true);
            urls.getEditor().selectAll();
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    public void remove(InputChangedListener listener) {
        inputChangedFirer.remove(listener);
    }

    void jbInit() throws Exception {
        urlLabel.setText("URL:");
        this.setLayout(gridBagLayout1);
        urls.setPreferredSize(new Dimension(300, 21));
        urlLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 3) {
                    urls.setSelectedItem("http://libcwms.gov.bc.ca/wmsconnector/com.esri.wsit.WMSServlet/ogc_layer_service");
                }
                super.mouseClicked(e);
            }
        });
        this.add(urlLabel,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(urls,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 4), 0, 0));
        this.add(fillerPanel,
            new GridBagConstraints(2, 10, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
//      [UT]
        this.add(createVersionButtons(
            new String[]{WMService.WMS_1_0_0, WMService.WMS_1_1_0, WMService.WMS_1_1_1}),
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
        
        //[UT] 20.10.2005 not added yet; need more testing
        /*
        this.add(createLossyCheckBox(),
                new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
        */
        
    }

    public String getInstructions() {
        return I18N.get("ui.plugin.wms.URLWizardPanel.please-enter-the-url-of-the-wms-server");
    }


    //
    // The WMService appends other parameters to the end of the URL
    //
    private String fixUrlForWMService(String url) {
        String fixedURL = url.trim();

        if ( fixedURL.indexOf( "?" ) == -1 ) {
            fixedURL = fixedURL + "?";
        } else {
            if ( fixedURL.endsWith( "?" ) ) {
                // ok
            } else {
                // it must have other parameters
                if ( !fixedURL.endsWith( "&" ) ) {
                    fixedURL = fixedURL + "&";
                }
            }
        }

        return fixedURL;
    }


    public void exitingToRight() throws IOException, WorkbenchException {
	LinkedList<String> list = new LinkedList<String>();
	String url = urls.getSelectedIndex() == -1 ? urls.getEditor().getItem().toString() :
	    urls.getItemAt(urls.getSelectedIndex()).toString();
		
	list.add(url);
	
	for(int i = 0; i < urls.getItemCount(); ++i)
	    if(i != urls.getSelectedIndex())
		list.add(urls.getItemAt(i).toString());
	
        dataMap.put(URL_KEY, list.toArray(new String[list.size()]));
//      [UT]
        //String ver = (String)dataMap.get(VERSION_KEY);
        
        url = fixUrlForWMService(url);
        //[UT] 20.04.2005 
        WMService service = new WMService( url, wmsVersion );
        //WMService service = new WMService( url );
        
        service.initialize();
        dataMap.put(SERVICE_KEY, service);
//[UT] 20.04.2005 added version
        MapImageFormatChooser formatChooser = new MapImageFormatChooser(wmsVersion);
        
        formatChooser.setPreferLossyCompression( false );
        formatChooser.setTransparencyRequired( true );

        String format = formatChooser.chooseFormat(service.getCapabilities()
                                                          .getMapFormats());
        
        if (format == null) {
            throw new WorkbenchException(I18N.get("ui.plugin.wms.URLWizardPanel.the-server-does-not-support-gif-png-or-jpeg-format"));
        }

        dataMap.put(FORMAT_KEY, format);
        dataMap.put(MapLayerWizardPanel.INITIAL_LAYER_NAMES_KEY, null);
        dataMap.put(VERSION_KEY, wmsVersion);

    }

    public void enteredFromLeft(Map dataMap) {
        this.dataMap = dataMap;
        urls.getEditor().selectAll();
    }

    public String getTitle() {
        return I18N.get("ui.plugin.wms.URLWizardPanel.select-uniform-resource-locator-url");
    }

    public String getID() {
        return getClass().getName();
    }

    public boolean isInputValid() {
        return true;
    }

    public String getNextID() {
        return MapLayerWizardPanel.class.getName();
    }
    //[UT] 10.01.2005 
    private Component createVersionButtons(String[] versions){
        JPanel p = new JPanel();
        
        ActionListener al = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JRadioButton jb = (JRadioButton)e.getSource();
                wmsVersion = jb.getText();
            }	
        };
        
        
        ButtonGroup group = new ButtonGroup();        
        JRadioButton[] buttons = new JRadioButton[ versions.length ];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JRadioButton(versions[i]);
            buttons[i].addActionListener(al);
            group.add(buttons[i]);
            p.add(buttons[i]);
            //click the last one
            if ( versions[i].equals( wmsVersion ) ){
                buttons[i].setSelected( true );
            }
        }
        
        return p;
    }
    
    //[UT] 20.10.2005 
    private Component createLossyCheckBox(){
        JPanel p = new JPanel();
        JCheckBox checkBox = new JCheckBox( "Preferr Lossy images", true );//I18N.get("ui.plugin.wms.URLWizardPanel.select-uniform-resource-locator-url") );
        checkBox.setToolTipText( "This will try to load JPEG images, if the WMS allows it." );
        checkBox.addActionListener( new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                lossyPreferred = ((JCheckBox)e.getSource()).isSelected();
            }
        });
        p.add( checkBox );
        return p;
    }
    
}
