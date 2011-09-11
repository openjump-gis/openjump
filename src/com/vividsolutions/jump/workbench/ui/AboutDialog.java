
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;

/**
 * Displays an About Dialog (Splash Screen).
 */
//<<TODO:AESTHETICS>> Modify the image so that the green and red pieces have
//a smooth sinusoidal interface. [Jon Aquino]
//<<TODO:AESTHETICS>> The lettering on the image is a bit blocky. Fix. [Jon Aquino]
public class AboutDialog extends JDialog {
    BorderLayout borderLayout2 = new BorderLayout();

    JPanel buttonPanel = new JPanel();
    JButton okButton = new JButton();
    private JTabbedPane jTabbedPane1 = new JTabbedPane();
    private JPanel infoPanel = new JPanel();
    private BorderLayout borderLayout3 = new BorderLayout();
    
    private JScrollPane aboutScroll;

    private JLabel lblJavaVersion, lblFreeMemory, lblTotalMemory, 
    				lblOSVersion, lblCommittedMemory;
    private JPanel pnlButtons = new JPanel();
    private JButton btnGC = new JButton();
    private SplashPanel splashPanel;
    
    private WorkbenchContext wbc;

    public static AboutDialog instance(WorkbenchContext context) {
        final String INSTANCE_KEY = AboutDialog.class.getName() + " - INSTANCE";
        //if (context.getWorkbench().getBlackboard().get(INSTANCE_KEY) == null) {
            AboutDialog aboutDialog = new AboutDialog(context.getWorkbench().getFrame());
            context.getWorkbench().getBlackboard().put(INSTANCE_KEY, aboutDialog);
            GUIUtil.centreOnWindow(aboutDialog);
        //}
        return (AboutDialog) context.getWorkbench().getBlackboard().get(INSTANCE_KEY);
    }

    private ExtensionsAboutPanel extensionsAboutPanel = new ExtensionsAboutPanel();

    private AboutDialog(WorkbenchFrame frame) {
        super(frame, I18N.get("ui.AboutDialog.about-jump"), true);
        wbc = frame.getContext().getWorkbench().getContext();

        extensionsAboutPanel.setPlugInManager(frame.getContext().getWorkbench().getPlugInManager());
        this.splashPanel =
            new SplashPanel(JUMPWorkbench.splashImage(), I18N.get("ui.AboutDialog.version")+" " + JUMPVersion.CURRENT_VERSION);

        try {
            jbInit();
            pack();
            this.addComponentListener(new ResizeMe());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        this.setMinimumSize(new Dimension( 200, 200));
        Border border_0 = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        
        this.getContentPane().setLayout(borderLayout2);
        //this.setResizable(false);
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        infoPanel.setLayout(borderLayout3);

        infoPanel.setLayout( new GridBagLayout() );
        
        JLabel lbl_sysinfo = createLabel( I18N.get("ui.AboutDialog.system-info") );
        lbl_sysinfo.setFont(lbl_sysinfo.getFont().deriveFont( Font.ITALIC | Font.BOLD , 12f));
        lbl_sysinfo.setHorizontalAlignment(SwingConstants.LEFT);
        panelAdd( lbl_sysinfo, infoPanel, 0, 0, GridBagConstraints.CENTER);

        JLabel lbl_java = createLabel(I18N.get("ui.AboutDialog.java-version"));
        lbl_java.setFont(lbl_java.getFont().deriveFont( Font.ITALIC ));
        panelAdd( lbl_java, infoPanel, 1, 0, GridBagConstraints.WEST);

        JLabel lbl_os = createLabel(I18N.get("ui.AboutDialog.os"));
        lbl_os.setFont(lbl_java.getFont());
        panelAdd( lbl_os, infoPanel, 1, 1, GridBagConstraints.WEST);
        
        JLabel lbl_memtotal = createLabel(I18N.get("ui.AboutDialog.total-memory"));
        lbl_memtotal.setFont(lbl_java.getFont());
        panelAdd( lbl_memtotal, infoPanel, 1, 2, GridBagConstraints.WEST);
        
        JLabel lbl_memcom = createLabel(I18N.get("ui.AboutDialog.comitted-memory"));
        lbl_memcom.setFont(lbl_java.getFont());
        panelAdd( lbl_memcom, infoPanel, 1, 3, GridBagConstraints.WEST); 
        
        JLabel lbl_memfree = createLabel(I18N.get("ui.AboutDialog.free-memory"));
        lbl_memfree.setFont(lbl_java.getFont());
        panelAdd( lbl_memfree, infoPanel, 1, 4, GridBagConstraints.WEST); 
               
        lblJavaVersion = new JLabel();
        lblJavaVersion.setToolTipText("");
        lblJavaVersion.setText("x");
        panelAdd( lblJavaVersion, infoPanel, 2, 0, GridBagConstraints.WEST);
        lblOSVersion = new JLabel();
        lblOSVersion.setText("x"); 
        panelAdd( lblOSVersion, infoPanel, 2, 1, GridBagConstraints.WEST);
        lblTotalMemory = new JLabel();
        lblTotalMemory.setText("x");
        panelAdd( lblTotalMemory, infoPanel, 2, 2, GridBagConstraints.WEST);
        lblCommittedMemory = new JLabel();
        lblCommittedMemory.setText("x");
        panelAdd( lblCommittedMemory, infoPanel, 2, 3, GridBagConstraints.WEST);
        lblFreeMemory = new JLabel();
        lblFreeMemory.setToolTipText("");
        lblFreeMemory.setText("x");
        panelAdd( lblFreeMemory, infoPanel, 2, 4, GridBagConstraints.WEST);


        btnGC.setText(I18N.get("ui.AboutDialog.garbage-collect"));
        btnGC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnGC_actionPerformed(e);
            }
        });
        
        infoPanel.add(
                pnlButtons,
                new GridBagConstraints(
                    0,
                    5,
                    3,
                    1,
                    0.0,
                    0.0,
                    GridBagConstraints.CENTER,
                    GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0),
                    0,
                    0));
            pnlButtons.add(btnGC, null);
            
            
        JPanel aboutPanel = new JPanel();

        aboutPanel.setLayout(new GridBagLayout());
        
        aboutPanel.add(splashPanel,new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

/*
        // print classpath for debugging
        URL[] urls = ((java.net.URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
        for(int i=0; i< urls.length; i++)
            System.out.println(urls[i]);
*/
        String result;
        String urlstring = "";
        try {
        	URL url = ClassLoader.getSystemResource( "readme.txt" ); // "ÿ \u069e/test.txt"
        	if (url == null)
        		throw new FileNotFoundException( "readme.txt missing in ojhome/.");
        	urlstring = URLDecoder.decode(url.toString(), "UTF8");
        	//System.out.println(URLDecoder.decode(url.toString(), "UTF8") + "-> ÿ \u069e/test.txt");
            FileInputStream file = new FileInputStream (new File(url.toURI()));
            DataInputStream in = new DataInputStream (file);
            byte[] b = new byte[in.available()];
            in.readFully (b);
            in.close ();
            result = new String (b, 0, b.length, "ISO-8859-1");
            }
          catch (Exception e) {
        	  // this is normal in development where readme.txt is
        	  // located in /etc/readme.txt
        	  StringBuffer buf = new StringBuffer();
        	  for (int i = 0; i < e.getStackTrace().length; i++) 
				buf.append( e.getStackTrace()[i] + "\n" );
			
        	  result = e +"\n\n" + buf;
          }
        
        JTextArea readme = new JTextArea(/*urlstring +"\n\n"+*/ result ) ;
        readme.setFont((new JLabel()).getFont().deriveFont( 12f ));
        readme.setEditable(false);
        readme.setAutoscrolls(false);
        
        aboutPanel.add(readme,new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,GridBagConstraints.NONE,
                new Insets(20, 0, 0, 0), 0, 0));
        
        aboutScroll = new JScrollPane();
        aboutScroll.getViewport().add(aboutPanel);
        aboutScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        aboutScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // calculate initial height of biggest asset according to app window height
        // unless it is smaller then splash height plus offset
        int min_h = splashPanel.getPreferredSize().height + 60;
        int pref_h = wbc.getWorkbench().getFrame().getHeight() - 200;
        pref_h = pref_h < min_h ? min_h : pref_h;
        // fixed width splash width + 25px for scrollbar
        aboutScroll.setPreferredSize(new Dimension (splashPanel.getPreferredSize().width + 25, pref_h));
        jTabbedPane1.add(aboutScroll, I18N.get("ui.AboutDialog.about"));
        
        jTabbedPane1.addTab(I18N.get("ui.AboutDialog.info"), infoPanel);
        jTabbedPane1.addTab(I18N.get("ui.AboutDialog.Extensions"), extensionsAboutPanel);
        
        // add tabbedpane
        this.getContentPane().add(jTabbedPane1, BorderLayout.NORTH);

        // add ok button
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.add(okButton, null);
        jTabbedPane1.setBounds(0, 0, 0, 0);
        
        int w = this.getContentPane().getWidth() + 
        		( aboutScroll.getPreferredSize().width -
        		this.getContentPane().getWidth() );
        // set a minimumsize enforce by listener below
        this.setMinimumSize(new Dimension (w, 304));

    }

    private void panelAdd ( Component comp, JPanel panel, int cellx, int celly, int position){
    	panel.add(
                comp,
                new GridBagConstraints(
                    cellx,
                    celly,
                    1,
                    1,
                    0.0,
                    0.0,
                    position,
                    GridBagConstraints.NONE,
                    new Insets(0, 10, 0, 10),
                    0,
                    0));    	
    }
    
    private JLabel createLabel ( String text ){
    	JLabel label = new JLabel( text );
    	label.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );
    	return label;
    }
    
    public void setVisible(boolean b) {
        if (b) {
            DecimalFormat format = new DecimalFormat("###,###");
            lblJavaVersion.setText(System.getProperty("java.version"));
            lblOSVersion.setText(
                System.getProperty("os.name")
                    + " ("
                    + System.getProperty("os.version")
                    + ")");

            long totalMem = Runtime.getRuntime().totalMemory();
            long freeMem = Runtime.getRuntime().freeMemory();
            lblTotalMemory.setText(format.format(totalMem) + " bytes");
            lblCommittedMemory.setText(format.format(totalMem - freeMem) + " bytes");
            lblFreeMemory.setText(format.format(freeMem) + " bytes");
        }

        super.setVisible(b);
    }

    void okButton_actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    void btnGC_actionPerformed(ActionEvent e) {
        Runtime.getRuntime().gc();
        setVisible(true);
    }


	class ResizeMe extends ComponentAdapter {
		Dimension minSize = getMinimumSize();
		Rectangle bounds = getBounds();

		public void componentResized(ComponentEvent evt) {
			/*
			 * Let the user stretch the dialog vertically only. If the user is
			 * dragging on the top or left border, the system counts it as a
			 * move as well as a resize; we have to explicitly restore the
			 * origin position so that the user doesn't end up chasing the
			 * dialog around the screen like a drop of mercury.
			 */
			int oldX = bounds.x;
			int oldY = bounds.y;
			int oldHeight = bounds.height;
			int oldWidth = bounds.width;
			int newX = getX();
			int newY = getY();
			int newWidth = getWidth();
			int newHeight = getHeight();
			if (newHeight < minSize.height || getWidth() != oldWidth) {
				int diff = minSize.height - newHeight;
				if (diff > 0 && newY != oldY) {
					newY -= diff;
				}
				newHeight += Math.max(0, diff);
				setBounds(oldX, newY, oldWidth, newHeight);
			}
			bounds.setBounds(oldX, newY, oldWidth, newHeight);
			
			// resize readme field with scrollbars
			Dimension scold = aboutScroll.getPreferredSize();
			aboutScroll.setPreferredSize(new Dimension(scold.width, scold.height + (newHeight - oldHeight)));
			aboutScroll.revalidate();
		}

		public void componentMoved(ComponentEvent evt) {
			/*
			 * Store the dialog's new location if the user moved it by dragging
			 * the title bar, but not if the move event was a side effect of
			 * resizing.
			 */
			if (getWidth() != bounds.width || getHeight() != bounds.height) {
				setBounds(bounds);
				return;
			}
			bounds.setLocation(getX(), getY());
		}

	}

}