package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreConnection;
import com.vividsolutions.jump.datastore.DataStoreException;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.datastore.ConnectionManager;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class PasswordPrompter {
    public DataStoreConnection getOpenConnection(
            ConnectionManager connectionManager,
            ConnectionDescriptor connectionDescriptor, Component parentComponent)
            throws Exception {
        return connectionManager
                .getOpenConnection(promptForPasswordIfNecessary(
                        connectionDescriptor, connectionManager,
                        parentComponent));
    }

    private ConnectionDescriptor promptForPasswordIfNecessary(
            ConnectionDescriptor connectionDescriptor,
            ConnectionManager connectionManager, Component parentComponent)
            throws DataStoreException {
        String passwordParameterName = ConnectionDescriptor
                .passwordParameterName(connectionDescriptor.getParameterList()
                        .getSchema());
        if (passwordParameterName == null) {
            return connectionDescriptor;
        }
        // To know if a password has been entered correctly, check if the
        // connection is open rather then if the password has been set,
        // as the user may have set a password that is incorrect.
        // [Jon Aquino 2005-03-11]
        /*
         * if
         * (!connectionManager.getConnection(connectionDescriptor).isClosed()) {
         * return connectionDescriptor; }
         */
        // The above isn't going to work anymore, because we are now saving
        // passwords. Just check whether the password is null for now.
        // Of course this doesn't handle the case in which a password
        // is incorrect or changed. Need to design a generic way to identify an
        // error as being a password error, without being specific to Oracle,
        // SDE, etc. [Jon Aquino 2005-03-17]
        if (connectionDescriptor.getParameterList().getParameterString(
                passwordParameterName) != null) {
            return connectionDescriptor;
        }
        connectionDescriptor.getParameterList().setParameter(
                passwordParameterName,
                promptForPassword(identifier(connectionDescriptor),
                        parentComponent));
        return connectionDescriptor;
    }

    /**
     * @return a username or other brief description of this
     *         ConnectionDescriptor
     */
    private String identifier(ConnectionDescriptor connectionDescriptor) {
        for (Iterator i = Arrays.asList(
                connectionDescriptor.getParameterList().getSchema().getNames())
                .iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (name.toLowerCase().matches("user.*")) {
                return connectionDescriptor.getParameterList().getParameter(
                        name).toString();
            }
        }
        return connectionDescriptor.getParameterList()
                .getParameter(
                        connectionDescriptor.getParameterList().getSchema()
                                .getNames()[0]).toString();
    }

    /**
     * @param identifier
     *            username or other description of what the password is for
     */
    private String promptForPassword(String identifier, Component parent) {
        Assert.isTrue(SwingUtilities.isEventDispatchThread());
        final JPasswordField passwordField = new JPasswordField(15);
        final JDialog dialog = createDialog(I18N.get("jump.workbench.ui.plugin.datastore.PasswordPrompter.Password"), parent);
        dialog.getContentPane().setLayout(new GridBagLayout());
        dialog.getContentPane().add(
                new JLabel(I18N.get("jump.workbench.ui.plugin.datastore.PasswordPrompter.Enter-password-for")+" " + identifier + ": "),
                new GridBagConstraints(0, 0, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(2, 2, 2, 2), 0, 0));
        dialog.getContentPane().add(
                passwordField,
                new GridBagConstraints(0, 1, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(2, 2, 2, 2), 0, 0));
        dialog.getContentPane().add(
                new JButton(I18N.get("jump.workbench.ui.plugin.datastore.PasswordPrompter.OK")) {
                    {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                dialog.setVisible(false);
                            }
                        });
                        dialog.getRootPane().setDefaultButton(this);
                    }
                },
                new GridBagConstraints(0, 2, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(2, 2, 2, 2), 0, 0));
        dialog.pack();
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        return new String(passwordField.getPassword());
    }

    private JDialog createDialog(String title, Component parent) {
        return window(parent) instanceof Frame ? new JDialog(
                (Frame) window(parent), title, true) : new JDialog(
                (Dialog) window(parent), title, true);
    }

    private Window window(Component component) {
        return component instanceof Window ? (Window) component
                : SwingUtilities.windowForComponent(component);
    }

    public static void main(String[] args) throws InterruptedException,
            InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JFrame frame = new JFrame();
                GUIUtil.centreOnScreen(frame);
                new PasswordPrompter().promptForPassword("scott", frame);
            }
        });
    }
}