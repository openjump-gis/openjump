/* This file is *not* under GPL or any other public license
 * Copyright 2005 Ugo Taddei 
 */
package de.latlon.deejump.plugin.manager;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.vividsolutions.jump.util.StringUtil;

public class ExtensionPanel extends JPanel {

    private ExtensionWrapper cataloguedExtension;
    private JCheckBox installCheck;
    
    public ExtensionPanel( ExtensionWrapper catExtension) {
        super();
        this.cataloguedExtension = catExtension;
        initGUI();
        final Dimension dim = new Dimension( 300, 40 );
        setPreferredSize( dim );
        setMinimumSize( dim );
        setMaximumSize( dim );
        
    }

    private void initGUI() {

        final String label = StringUtil.limitLength( cataloguedExtension.getTitle(), 30) + " (" + cataloguedExtension.getCategory() + ") ";
        
        installCheck = new JCheckBox( label, cataloguedExtension.isInstalled() );
        installCheck.addActionListener( new ActionListener(){
           
            public void actionPerformed(ActionEvent e) {
                System.out.println("label: " + label);
                cataloguedExtension.setInstalled( installCheck.isSelected() );
            }
           
        });
        final Dimension dim = new Dimension( 286, 36 );
        
        installCheck.setPreferredSize( dim );
        installCheck.setMinimumSize( dim );
        
        installCheck.setAlignmentX( 0.20f );
        /*JLabel jLabel = new JLabel( label );
        
        JPanel panel = new JPanel();
        panel.add( installCheck );
        panel.add( jLabel );
        */
        add( installCheck );
    }

    public String getExtensionText() {
        
        return this.cataloguedExtension.toString();
    }

    public void setEnabled( boolean on ) {
        super.setEnabled( on );
        for (int i = 0; i < getComponentCount(); i++) {
            getComponent( i ).setEnabled( on );
        }
    }
    
    /*public boolean isSelected() {
        return this.installCheck.isSelected();
    }
*/
    public void setSelected(boolean selected) {
        this.installCheck.setSelected( selected );
        cataloguedExtension.setInstalled( selected );
    }
    
    
    
}
