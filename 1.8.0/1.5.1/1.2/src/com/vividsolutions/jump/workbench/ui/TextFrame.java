
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.apache.log4j.Logger;


public class TextFrame extends JInternalFrame {
	private static Logger LOG = Logger.getLogger(TextFrame.class);
    BorderLayout borderLayout1 = new BorderLayout();
    private OKCancelPanel okCancelPanel = new OKCancelPanel();
    protected JPanel scrollPanePanel = new JPanel();
    JScrollPane scrollPane = new JScrollPane();
    GridBagLayout gridBagLayout = new GridBagLayout();
    private JEditorPane editorPane = new JEditorPane();
    private ErrorHandler errorHandler;

    public TextFrame(ErrorHandler errorHandler) {
        this(false, errorHandler);
    }

    public TextFrame(boolean showingButtons, ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

        try {
            jbInit();
            okCancelPanel.setVisible(showingButtons);
        } catch (Exception e) {
            errorHandler.handleThrowable(e);
        }
    }

    public OKCancelPanel getOKCancelPanel() {
        return okCancelPanel;
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        this.setResizable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setSize(500, 300);
        scrollPanePanel.setLayout(gridBagLayout);
        editorPane.setBackground(UIManager.getColor("inactiveCaptionBorder"));
        editorPane.setText("jEditorPane1");
        editorPane.setContentType("text/html");
        getContentPane().add(getOKCancelPanel(), BorderLayout.SOUTH);
        this.getContentPane().add(scrollPanePanel, BorderLayout.CENTER);
        scrollPanePanel.add(scrollPane,
            new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0,
                1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        scrollPane.getViewport().add(editorPane, null);
    }

    public void clear() {
        setText("");
    }

    public void setText(final String s) {
        try {
            editorPane.setText(s);
            editorPane.setCaretPosition(0);
        } catch (Throwable t) {
            LOG.error(s);
            errorHandler.handleThrowable(t);
        }
    }

    public String getText() {
        return editorPane.getText();
    }
}
