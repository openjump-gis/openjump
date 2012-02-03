/* This file is *not* under GPL or any other public license
 * Copyright 2005 Ugo Taddei 
 */
package de.latlon.deejump.plugin.manager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Extension;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;

public class ExtensionManagerDialog extends JDialog {

    private PlugInManager manager;

    private WorkbenchContext workbenchContext;
    
    private List remoteExtensions;

    private List installedExtensions;

    private JTextArea descripTextArea;

    private static final String extensionCatalogFile = "extensioncatalog.xml";
    
    private String extensionSite;
    
    private boolean okClicked = false;

    private final MouseListener descriptionDisplayListener = new MouseAdapter() {
        /*public void mouseClicked(MouseEvent e) {
            CatalogExtensionPanel cep = (CatalogExtensionPanel) e
                    .getSource();
            setDescriptionText(cep.getExtensionText());
        }*/
        
        public void mouseEntered(MouseEvent e) {
            ExtensionPanel cep = (ExtensionPanel) e
                .getSource();
            setDescriptionText(cep.getExtensionText());
        }
    };

    private JPanel managePanel;
    
    private JPanel installPanel;
    
    private JPanel newVersionsPanel;

    public ExtensionManagerDialog(JFrame parent, WorkbenchContext workbenchContext, String extensionSite)
            throws Exception {
        super(parent,  I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Extension-Manager"));
        setSize(350, 500);
        setResizable(false);
        setModal( true );
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                //
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.workbenchContext = workbenchContext;
        this.manager = workbenchContext.getWorkbench().getPlugInManager();
        
        this.extensionSite = extensionSite;
//FIXME remove extensionCatalogFile class memebr
        //TODO check if URL is valid...
        URL catalog = new URL( this.extensionSite + extensionCatalogFile ); 
            //ExtensionManagerDialog.class.getResource( extensionCatalogFile );

        setRemoteExtensions( readCatalog(catalog));
        setInstalledExtensions( listInstalledExtensions() );
        synchronizeExtensions();
        initGUI();
//        setVisible(true);
    }

    private ExtensionManagerDialog(JFrame parent, List  fakeInstalledExtensions)
            throws HeadlessException {
        super(parent,  I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Extension-Manager"));
        setSize(350, 500);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                //
            }
        });
//        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        extensionSite = "http://";
        URL catalog = ExtensionManagerDialog.class
                .getResource("plugincatalog.xml");

        setRemoteExtensions( readCatalog(catalog) );
        setInstalledExtensions( fakeInstalledExtensions );
        synchronizeExtensions();

        initGUI();
        setVisible(true);
    }

    private void setInstalledExtensions(List installedExt) {
        // must check if !null 
        this.installedExtensions = installedExt;

    }

    private void setRemoteExtensions(List remoteExt) {
        // must check if !null 
        this.remoteExtensions = remoteExt;

    }

    private void initGUI() {
        LayoutManager layout = new BoxLayout(this.getContentPane(),
                BoxLayout.PAGE_AXIS);
        getContentPane().setLayout(layout);

        getContentPane().add(createTabbedPane());
        getContentPane().add(createTextArea());
        getContentPane().add(createButtons());

    }

    private Component createTextArea() {
        descripTextArea = new JTextArea();
        descripTextArea.setRows(6);
        descripTextArea.setEditable(false);
        descripTextArea.setLineWrap(true);
        descripTextArea.setWrapStyleWord(false);
        /*descripTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory
                        .createLineBorder(Color.DARK_GRAY)));
*/
        JScrollPane pane = new JScrollPane(
                descripTextArea, 
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(
                BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 15, 20, 15),
                BorderFactory.createBevelBorder( BevelBorder.LOWERED )));
        return pane;
    }

    private void setDescriptionText(String txt) {
        descripTextArea.setText(txt);
        descripTextArea.setCaretPosition(0);
    }

    private Component createTabbedPane() {
        JTabbedPane panes = new JTabbedPane();
        panes.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
        panes.setPreferredSize(new Dimension(300, 400));
        
        managePanel = new JPanel();
        managePanel.setLayout( new BoxLayout( managePanel, BoxLayout.PAGE_AXIS));
        JScrollPane pane = new JScrollPane(
                managePanel, 
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panes.addTab(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Installed-Extensions"), pane);
        
        refreshExtensionsPanel( managePanel, installedExtensions, true );
        
        /*
        for (Iterator iter = installedExtensions.iterator(); iter.hasNext();) {
            CataloguedExtension ext = (CataloguedExtension) iter.next();
            CatalogExtensionPanel cep = new CatalogExtensionPanel(ext);
            ext.setInstalled( true );
            managePanel.add(cep);
            
            cep.addMouseListener( descriptionDisplayListener );
        }*/

        newVersionsPanel = new JPanel();
        panes.add(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.New-Versions"), newVersionsPanel);
        panes.setEnabledAt(1, false);

        JComponent c = new JComponent() {
           public Dimension getMaximumSize() {
            return new Dimension( 350,200);
        }  
        };
        
        installPanel = new JPanel();
        installPanel.setLayout( new BoxLayout( installPanel, BoxLayout.PAGE_AXIS));
        pane = new JScrollPane(
                installPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panes.add(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Install"), pane);
        
        refreshExtensionsPanel( installPanel, remoteExtensions, false );
        
        return panes;
    }

    private Component createButtons() {
        JComponent c = Box.createHorizontalBox();
        int bSize = 20;
        c.setBorder(BorderFactory.createEmptyBorder(bSize, bSize, bSize,
                        bSize));
        
        JButton button = new JButton(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Configure"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                String input = JOptionPane.showInputDialog( 
                        ExtensionManagerDialog.this, 
                        I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Catalog-site-URL"),
                        extensionSite );
                
                extensionSite = input != null ? input : extensionSite;
                //make sure it finishes as a dir
                extensionSite = extensionSite.endsWith( "/" ) ? extensionSite : extensionSite + "/";
                reReadCatalog();
            }
        });

        c.add(button);
        c.add( Box.createHorizontalStrut( 20 ));
        
        button = new JButton(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.OK"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                updateExtensions();
                okClicked = true;
                setVisible(false);

            }
        });

        c.add(button);
        c.add( Box.createHorizontalStrut( 20 ));
        
        button = new JButton(I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Cancel"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                updateExtensions();
                okClicked = false;
                setVisible(false);

            }
        });

        c.add(button);
        
        return c;
    }

    private void reReadCatalog() {
        try {
            URL u = new URL( extensionSite + extensionCatalogFile );
            setRemoteExtensions( readCatalog( u ) );
            setInstalledExtensions( listInstalledExtensions() );
            synchronizeExtensions();
            refreshExtensionsPanel( managePanel, installedExtensions, true );
            refreshExtensionsPanel( installPanel, remoteExtensions, false );
        } catch (MalformedURLException e1) {
            JOptionPane.showMessageDialog( ExtensionManagerDialog.this, 
            		I18N.get("deejump.pluging.manager.ExtensionManagerDialog.You-entered-a-malformed-URL"), I18N.get("deejump.pluging.manager.ExtensionManagerDialog.Disgraceful-Error"), JOptionPane.ERROR_MESSAGE );
        }
    }
    
    
    private List listInstalledExtensions() {
        if (manager == null) {
            return new ArrayList(0);
        }

        // make a copy, manager.getConfigurations() is unmodifiable
        List tmpInstalledExtensions = new ArrayList(manager.getConfigurations()
                .size());
        for (Iterator iter = manager.getConfigurations().iterator(); iter
                .hasNext();) {
            tmpInstalledExtensions.add( wrapExtension( (Extension)iter.next() ));
        }

        return tmpInstalledExtensions;
    }

    private ExtensionWrapper wrapExtension( Extension pluggedInExt ) {
        
        for (Iterator iter = remoteExtensions.iterator(); iter.hasNext();) {
            ExtensionWrapper remExt = (ExtensionWrapper) iter.next();
            
            if ( remExt.getName().equals( pluggedInExt.getName() )) {
                return remExt;
            }
        }
        
        final String desc = I18N.get("deejump.pluging.manager.ExtensionManagerDialog.This-extension-has-not-been-found-in-the-catalogue")+ "\n" +
        					I18N.get("deejump.pluging.manager.ExtensionManagerDialog.It-may-have-been-added-by-copying-into-the-lib/ext-directory");
        final String unknownCat = I18N.get("deejump.pluging.manager.ExtensionManagerDialog.unknown-category");
        final String unknownAuthor = I18N.get("deejump.pluging.manager.ExtensionManagerDialog.unknown-author");
        
        return new ExtensionWrapper( pluggedInExt.getName(), pluggedInExt.getName() + "(" + I18N.get("deejump.pluging.manager.ExtensionManagerDialog.uncatalogued") + ")",
                unknownAuthor, pluggedInExt.getVersion(), I18N.get("deejump.pluging.manager.ExtensionManagerDialog.unknown-JUMP-version"), 
                unknownCat, desc, new ArrayList(0));
    }

    private void synchronizeExtensions() {
    }
    private void _synchronizeExtensions() {

        List tmpRemoteExtensions = new ArrayList( remoteExtensions ); 
        for (Iterator iter = installedExtensions.iterator(); iter.hasNext();) {
//            Extension configuration = (Extension) iter.next();
            ExtensionWrapper configuration = (ExtensionWrapper) iter.next();
            
            for (Iterator iter2 = remoteExtensions.iterator(); iter2.hasNext();) {
                ExtensionWrapper ext = (ExtensionWrapper) iter2.next();
                
                if (configuration.getName().equals(ext.getName())) {

                    // remote ext match local, add to temp list
                    // temp list wil only list the remote extensions 
                    // that are not yet locally installed
                    
//                    tmpRemoteExtensions.remove(ext);
                }
            }
        }
        
        setRemoteExtensions( tmpRemoteExtensions );
    }

    
    private void refreshExtensionsPanel( JComponent extensionPanel, List extensionList, boolean selected ) {

        extensionPanel.removeAll();
        //FIXME are you sure the listeners are being removed too?
        for (Iterator iter = extensionList.iterator(); iter.hasNext();) {
            ExtensionWrapper ext = (ExtensionWrapper) iter.next();
            ext.setInstalled( selected );
            
            ExtensionPanel cep = new ExtensionPanel(ext);
            
            if ( extensionList == remoteExtensions ) {
                if( installedExtensions.contains( ext )) {
                    cep.setSelected( true );
                    cep.setEnabled( false );
                }
            }
            cep.addMouseListener( descriptionDisplayListener );
            extensionPanel.add(cep);
        }
    }
    
    public void updateExtensions( TaskMonitor monitor ) 
        throws Exception {

		List recentlyAddedExtensions = new ArrayList();
        //install remote extensions
        for (Iterator iter = remoteExtensions.iterator(); iter.hasNext();) {
            ExtensionWrapper ext = (ExtensionWrapper) iter.next();
            if ( ext.isInstalled() && !installedExtensions.contains( ext ) ) {//isInstalled means here: "is to be installed"

                ExtensionHelper.install( this, workbenchContext, ext, monitor );
                recentlyAddedExtensions.add( ext );   
            }
        }
        installedExtensions.addAll( recentlyAddedExtensions );
        
        
        //remove installed extensions
        for (Iterator iter = installedExtensions.iterator(); iter.hasNext();) {
            ExtensionWrapper ext = (ExtensionWrapper) iter.next();
            if ( !ext.isInstalled() ) {
                System.out.println("must :  manager.getPlugInDirectory()");
                System.out.println("pi dir: : " + manager.getPlugInDirectory());
                File pluginDir = 
//                    new File("C:/temp/");
                    manager.getPlugInDirectory();
                List fileList = Arrays.asList( pluginDir.listFiles() );
                
                ExtensionHelper.remove( fileList, ext, monitor );
            }
        }
    }

    public void setVisible( boolean vis ) {
        if ( vis ) {
            okClicked = false;
//            setInstalledExtensions( listInstalledExtensions() );
            System.out.println("listInstalledExtensions(): " + listInstalledExtensions());
//            synchronizeExtensions();
            //FIXME
            //there's a bug here making the installed and cataloge exts being wrongly clicked
            refreshExtensionsPanel( managePanel, installedExtensions, true);
            refreshExtensionsPanel( installPanel, remoteExtensions, false);
        } 
        super.setVisible( vis );
        
    }
    /**
     * Reads an extensions catalog from the XML at catalog, and builds list of
     * remotely available extensions
     * 
     * @param catalog
     */
    private List readCatalog(URL catalog) {
        
        CatalogParser catParser = new CatalogParser( catalog );
        return catParser.getExtensionList();
        
    }

    // test
    public static void main(String[] args) throws Exception {

        List resources = new ArrayList(1);
        resources.add("PrintLayoutAlpha0.1-i18n.jar");

        List remoteExt = new ArrayList(2);
        remoteExt.add(new ExtensionWrapper("Print Layout", " some", "ut",
                "123", "456", "cat", "desc", resources));

        resources.clear();
        resources.add("dummy_ext.jar");

        remoteExt.add(new ExtensionWrapper("Dummy extension", "The dummy extension", "ut",
                "123", "456", "cat", "desc 1", resources));
        final ExtensionManagerDialog managerDialog = new ExtensionManagerDialog(
                new JFrame(), remoteExt);

        managerDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    managerDialog.updateExtensions(null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }        
                System.exit(0);
            }
        });
        // managerDialog.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

    }

    public String getExtensionSite() {
        return extensionSite;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

}
