/*----------------    FILE HEADER  ------------------------------------------

Copyright (C) 2001 by:
lat/lon GmbH
http://www.lat-lon.de

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Contact:

Andreas Poth
lat/lon GmbH
Meckenheimer Allee 176
53115 Bonn
Germany
E-Mail: poth@lat-lon.de

 ---------------------------------------------------------------------------*/
package de.latlon.deejump.wfs.auth;

import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import de.latlon.deejump.wfs.i18n.I18N;

/**
 * 
 * 
 * @author <a href="mailto:poth@lat-lon.de">Andreas Poth </a>
 * @author last edited by: $Author: ap $
 * 
 * @version 1.1, $Revision: 1.1 $, $Date: 2005/08/10 20:15:25 $
 * 
 * @since 1.1
 */
public class LoginDialog extends JDialog {
    
    private static final long serialVersionUID = 6706308764756321466L;

    protected JTextField nameField = null;    
    protected JPasswordField passwordField = null; 
    protected String name = null;
    protected String password = null;
    private JTextField wfsField = null;
    
    /**
     * sets up the login dialog with a name and an owner Frame. Dialog is shown modally immediately!
     * 
     * @param owner owner of the dialog
     * @param title title to display in the header
     * @param serverName 
     */
    public LoginDialog( Dialog owner, String title, String serverName ) {
        super(owner, title);
        initGUI(serverName);
        setModal(true);
        setLocationRelativeTo(owner);
        setVisible( true );
    }
    
    private void initGUI(String serverName) {
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0;
        gb.gridy = 0;
        gb.insets = new Insets(5, 2, 0, 2);
        getContentPane().add(new JLabel(I18N.get("LoginDialog.service")), gb);
        ++gb.gridy;
        getContentPane().add(new JLabel(I18N.get("LoginDialog.username")), gb);
        ++gb.gridy;
        getContentPane().add(new JLabel(I18N.get("LoginDialog.password")), gb);

        gb.gridy = 0;
        ++gb.gridx;
        gb.fill = GridBagConstraints.HORIZONTAL;

        wfsField = new JTextField(serverName);
        getContentPane().add(wfsField, gb);
        ++gb.gridy;
        nameField = new JTextField();
        getContentPane().add(nameField, gb);
        ++gb.gridy;
        passwordField = new JPasswordField();
        getContentPane().add(passwordField, gb);

        JButton ok = new JButton(I18N.get("LoginDialog.ok"));
        ok.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    name = nameField.getText();
                    password = new String(passwordField.getPassword());
                    dispose();
                }
            }
        );
        
        gb.insets = new Insets(5, 2, 5, 2);
        gb.fill = GridBagConstraints.NONE;
        ++gb.gridy;
        gb.gridx = 0;
        getContentPane().add(ok, gb);
        
        JButton cancel = new JButton(I18N.get("LoginDialog.cancel"));
        cancel.addActionListener(
             new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     dispose();
                 }
             }
         );
        
        gb.gridx = 1;
        getContentPane().add(cancel, gb);

        getRootPane().setDefaultButton(ok);
        pack();
        setResizable(false);
        
        addComponentListener(new ComponentAdapter(){
            @Override
            public void componentShown( ComponentEvent e ) {
                nameField.grabFocus();
            }
        });
    }

    
    /**
     * @return the name of the user if the dialog has been quit
     * with OK. Otherwise <tt>null</tt> will be returned.
     * 
     * Not sure why it is overridden here, probably a mistake...
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * @return the password of the user if the dialog has been quit
     * with OK. Otherwise <tt>null</tt> will be returned
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * @return the wfs URL
     */
    public String getWfsUrl() {
        return wfsField.getText();
    }

}
