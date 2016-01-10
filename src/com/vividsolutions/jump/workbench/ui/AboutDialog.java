
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
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.plugin.AboutPlugIn;

/**
 * Displays an About Dialog (Splash Screen).
 */
//<<TODO:AESTHETICS>> Modify the image so that the green and red pieces have
//a smooth sinusoidal interface. [Jon Aquino]
//<<TODO:AESTHETICS>> The lettering on the image is a bit blocky. Fix. [Jon Aquino]
public class AboutDialog extends JDialog {
    
    private final static String FILESEP = System.getProperty("file.separator");
    private static AboutDialog aboutDialog;
    JPanel buttonPanel = new JPanel();
    JButton okButton = new JButton();
    private JTabbedPane jTabbedPane1 = new JTabbedPane();
    private JPanel infoPanel = new JPanel();
    
    private JScrollPane aboutScroll, extScroll;
  
    private JLabel lblJavaVersion = new JLabel();
    private JLabel lblOSVersion = new JLabel();
    private JLabel lblMaxMemory = new JLabel();
    private JLabel lblTotalMemory = new JLabel();
    private JLabel lblCommittedMemory = new JLabel();
    //private JLabel lblFreeMemory = new JLabel();
    private JLabel lblUserDir = new JLabel();
    private JPanel pnlButtons = new JPanel();
    private JButton btnGC = new JButton();
    
    private WorkbenchContext wbc;

    public static AboutDialog instance(WorkbenchContext context) {
    	final String INSTANCE_KEY = AboutDialog.class.getName() + " - INSTANCE";
        if (context.getWorkbench().getBlackboard().get(INSTANCE_KEY) == null) {
            aboutDialog = new AboutDialog(context.getWorkbench().getFrame());
            context.getWorkbench().getBlackboard().put(INSTANCE_KEY, aboutDialog);
        }
        aboutDialog = (AboutDialog) context.getWorkbench().getBlackboard().get(INSTANCE_KEY);
        //GUIUtil.centreOnWindow(aboutDialog);
        return aboutDialog;
    }

    private ExtensionsAboutPanel extensionsAboutPanel;

    private AboutDialog(WorkbenchFrame frame) {
        super(frame, I18N.get("ui.AboutDialog.about-jump"), true);
        try {
            setIconImage(AboutPlugIn.ICON.getImage());
        } catch (NoSuchMethodError e) {
            // IGNORE: this is 1.5 missing setIconImage()
        }

        wbc = frame.getContext().getWorkbench().getContext();

        extensionsAboutPanel = new ExtensionsAboutPanel(frame.getContext().getWorkbench().getPlugInManager());

        try {
            jbInit();
            pack();
            setPreferredSize(new Dimension(this.getWidth(), frame.getHeight() - 200));
            GUIUtil.centreOnWindow(this);
            this.addComponentListener(new ResizeMe());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        setLayout(new BorderLayout());

        /* About Panel *******************************************************/
        JPanel aboutPanel = new JPanel(new GridBagLayout());

        ImageIcon splash = JUMPWorkbench.splashImage();
        JPanel splashPanel =
                new SplashPanelV2(splash, I18N.get("ui.AboutDialog.version")+" " + JUMPVersion.CURRENT_VERSION);
        aboutPanel.add(splashPanel,new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTH,GridBagConstraints.NONE,
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
            URL url = ClassLoader.getSystemResource("readme.txt"); // "ÿ \u069e/test.txt"

            if (url == null)
                throw new FileNotFoundException(
                        "readme.txt missing in ojhome/.");
            urlstring = URLDecoder.decode(url.toString(), "UTF8");
            // System.out.println(URLDecoder.decode(url.toString(), "UTF8") +
            // "-> ÿ \u069e/test.txt");
            FileInputStream file = new FileInputStream(new File(url.toURI()));
            DataInputStream in = new DataInputStream(file);
            byte[] b = new byte[in.available()];
            in.readFully(b);
            in.close();
            result = new String(b, 0, b.length, "ISO-8859-1");
        } catch (Exception e) {
            // this is normal in development where readme.txt is located in 
            // etc/readme.txt, add 'project_folder/etc' to classpath to circumvent
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < e.getStackTrace().length; i++)
                buf.append(e.getStackTrace()[i] + "\n");

            result = e + "\n\n" + buf;
        }
        
        JTextArea readme = new JTextArea(/*urlstring +"\n\n"+*/ result ) ;
        // ensure monospaced typo
        readme.setFont(new Font("Monospaced",Font.PLAIN,12));
        readme.setEditable(false);
        //readme.setAutoscrolls(false);
        // pad text away from the border
        readme.setBorder( BorderFactory.createEmptyBorder(20,20,20,20) );
        JPanel readmeP = new JPanel(); 
        readmeP.add(readme);
        
        aboutPanel.add(readmeP,new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,GridBagConstraints.BOTH,
                new Insets(20, 0, 5, 0), 0, 0));
        JPanel aboutP2 = new JPanel(new BorderLayout());
        aboutP2.add(aboutPanel, BorderLayout.CENTER);
        aboutScroll = new JScrollPane(aboutP2);
        aboutScroll.setBorder(BorderFactory.createEmptyBorder());

        // calculate initial height of biggest asset according to app window height
        // unless it is smaller then splash height plus offset
        int min_h = splashPanel.getPreferredSize().height + 60;
        int pref_h = aboutScroll.getPreferredSize().height; //wbc.getWorkbench().getFrame().getHeight() - 200;
        pref_h = pref_h < min_h ? min_h : pref_h;
        // fixed width splash width + 25px for scrollbar
        aboutScroll.setMinimumSize(new Dimension (splash.getIconWidth() + 25, min_h));
        jTabbedPane1.addTab(I18N.get("ui.AboutDialog.about"), aboutScroll);


        /* Info Panel ********************************************************/
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
        
        JLabel lbl_memmax = createLabel(I18N.get("ui.AboutDialog.maximum-memory"));
        lbl_memmax.setFont(lbl_java.getFont());
        panelAdd( lbl_memmax, infoPanel, 1, 2, GridBagConstraints.WEST);
        
        JLabel lbl_memtotal = createLabel(I18N.get("ui.AboutDialog.total-memory"));
        lbl_memtotal.setFont(lbl_java.getFont());
        panelAdd( lbl_memtotal, infoPanel, 1, 3, GridBagConstraints.WEST);
        
        JLabel lbl_memcom = createLabel(I18N.get("ui.AboutDialog.comitted-memory"));
        lbl_memcom.setFont(lbl_java.getFont());
        panelAdd( lbl_memcom, infoPanel, 1, 4, GridBagConstraints.WEST); 
        
        //JLabel lbl_memfree = createLabel(I18N.get("ui.AboutDialog.free-memory"));
        //lbl_memfree.setFont(lbl_java.getFont());
        //panelAdd( lbl_memfree, infoPanel, 1, 5, GridBagConstraints.WEST); 
        
        panelAdd(Box.createRigidArea(new Dimension(10, 10)), infoPanel, 1, 6,
            GridBagConstraints.WEST);
        
        JLabel lbl_userdir = createLabel( I18N.get("ui.AboutDialog.user-dir") );
        lbl_userdir.setFont(lbl_java.getFont());
        panelAdd( lbl_userdir, infoPanel, 1, 7, GridBagConstraints.WEST);
        
        panelAdd(Box.createRigidArea(new Dimension(10, 10)), infoPanel, 1, 8,
            GridBagConstraints.WEST);

        lblJavaVersion.setToolTipText("");
        lblJavaVersion.setText("x");
        panelAdd( lblJavaVersion, infoPanel, 2, 0, GridBagConstraints.WEST);

        lblOSVersion.setText("x"); 
        panelAdd( lblOSVersion, infoPanel, 2, 1, GridBagConstraints.WEST);

        lblMaxMemory.setText("x");
        panelAdd( lblMaxMemory, infoPanel, 2, 2, GridBagConstraints.WEST);
        
        lblTotalMemory.setText("x");
        panelAdd( lblTotalMemory, infoPanel, 2, 3, GridBagConstraints.WEST);

        lblCommittedMemory.setText("x");
        panelAdd( lblCommittedMemory, infoPanel, 2, 4, GridBagConstraints.WEST);

        //lblFreeMemory.setToolTipText("");
        //lblFreeMemory.setText("x");
        //panelAdd( lblFreeMemory, infoPanel, 2, 5, GridBagConstraints.WEST);
        
        lblUserDir.setText("x");
        panelAdd( lblUserDir, infoPanel, 2, 7, GridBagConstraints.WEST);

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
                    9,
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

        jTabbedPane1.addTab(I18N.get("ui.AboutDialog.info"), infoPanel);


        /* Extensions Panel **************************************************/
        extScroll = new JScrollPane(extensionsAboutPanel);
        extScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        extScroll.setBorder(BorderFactory.createEmptyBorder());
        jTabbedPane1.addTab(I18N.get("ui.AboutDialog.Extensions"), extScroll);

        extScroll.addAncestorListener(new AncestorListener() {
          int h = 0, v = 0;

          @Override
          public void ancestorRemoved(AncestorEvent event) {
            h = extScroll.getHorizontalScrollBar().getValue();
            v = extScroll.getVerticalScrollBar().getValue();
          }

          @Override
          public void ancestorMoved(AncestorEvent event) {
            h = extScroll.getHorizontalScrollBar().getValue();
            v = extScroll.getVerticalScrollBar().getValue();
          }

          // reload if the tab is activated
          @Override
          public void ancestorAdded(AncestorEvent event) {

            extensionsAboutPanel.refresh();
            // restore scrollbar positions after refresh
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                extScroll.getHorizontalScrollBar().setValue(h);
                extScroll.getVerticalScrollBar().setValue(v);
              }
            });
          }
        });
        
        // add tabbedpane
        add(jTabbedPane1, BorderLayout.CENTER);

        /* OK Button */

        okButton.setText(I18N.get("ui.OKCancelPanel.ok"));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.add(okButton, null);
        jTabbedPane1.setBounds(0, 0, 0, 0);
        
        // set a minimumsize enforce by listener below
        // disabled because aboutScrollPanel has a minimum size now
        //this.setMinimumSize(new Dimension (w, h));

    }

    // info panel helper method
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
                    GridBagConstraints.BOTH,
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
        lblJavaVersion.setText(System.getProperty("java.vm.name") + " "
            + System.getProperty("java.version") + " ("
            + System.getProperty("os.arch") + ")");
        lblOSVersion.setText(System.getProperty("os.name") + " ("
            + System.getProperty("os.version") + ")");
  
        long maxMem = Runtime.getRuntime().maxMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        lblMaxMemory.setText(format.format(maxMem - 1) + " bytes ("
            + humanReadableByteCount(maxMem, false) + ")");
        lblTotalMemory.setText(format.format(totalMem) + " bytes ("
            + humanReadableByteCount(totalMem, false) + ")");
        lblCommittedMemory.setText(format.format(totalMem - freeMem) + " bytes ("
            + humanReadableByteCount(totalMem - freeMem, false) + ")");
        //lblFreeMemory.setText(format.format(freeMem) + " bytes ("
        //    + humanReadableByteCount(freeMem, false) + ")");
        lblUserDir.setText(formatDirNameForHtml(System.getProperty("user.dir"), 40));
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
        int last_x = getX();
        int last_y = getY();
        int last_w = getWidth();
        int last_h = getHeight();
        boolean initialized = false;
        
        public void componentResized(ComponentEvent evt) {
            // System.out.println( getX() + " cR " + getY());
            /*
             * Let the user stretch the dialog vertically only. If the user is
             * dragging on the top or left border, the system counts it as a
             * move as well as a resize; we have to explicitly restore the
             * origin position so that the user doesn't end up chasing the
             * dialog around the screen like a drop of mercury.
             */
            int oldX = last_x;
            int oldY = last_y;
            int oldHeight = last_h;
            int oldWidth = getMinimumSize().width; //last_w;
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

                // sanitize -0 locs, ignore locX changes to the right
                // (preserve wandering because width balances it out)
                newX = (newX < 0) ? 0 : (oldX > 0 ? oldX : newX);
                newY = (newY < 0) ? 0 : newY;
                setBounds(oldX, newY, oldWidth, newHeight);
            }
            // sanitize always (e.g. first show)
            else {
                // sanitize -0 locs
                newX = (newX <= 0) ? 0 : newX;
                newY = (newY <= 0) ? 0 : newY;
                setLocation(newX, newY);
            }

            // save current loc and dimension for next run
            memorize();
        }

        public void componentMoved(ComponentEvent evt) {
            //if (evt.equals(last_e)) return;
            // do not move if resized vertically to the left
            if (getWidth() != last_w)
                setLocation(last_x, getY());
            
            memorize();
        }

        private void resetScrollPositions(JScrollPane sp) {
          JScrollBar verticalScrollBar = sp.getVerticalScrollBar();
          JScrollBar horizontalScrollBar = sp.getHorizontalScrollBar();
          verticalScrollBar.setValue(verticalScrollBar.getMinimum());
          horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());
        }

        public void componentShown(ComponentEvent e) {

            // reset scrollpane on redisplay
            resetScrollPositions(aboutScroll);
            resetScrollPositions(extScroll);
            if (initialized) {
                // resize and locate according to new workbench position
                setPreferredSize(new Dimension(last_w, wbc.getWorkbench()
                    .getFrame().getHeight() - 200));
                pack();
            } else {
                initialized = true;
            }
            
                     
            memorize();
        }

        private void memorize() {
            last_x = getX();
            last_y = getY();
            last_w = getWidth();
            last_h = getHeight();
        }

    }
    
    public static String formatDirNameForHtml(String dir, int maxLength) {
        String filesep_regex = FILESEP.replaceAll("\\\\", "\\\\\\\\");
        String[] path = dir.split(filesep_regex);
        StringBuilder multiline = new StringBuilder("<html><body>");
        StringBuilder line = new StringBuilder(path[0]);
        for (int i = 1 ; i < path.length ; i++) {
            if (line.length() + path[i].length() > maxLength) {
                multiline.append(line).append(FILESEP + "<br>");
                line = new StringBuilder(path[i]);
            }
            else line.append(FILESEP).append(path[i]);
        }
        multiline.append(line);
        return multiline.append("</body></html>").toString();
    }

    /*
     * courtesy of 
     * http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}