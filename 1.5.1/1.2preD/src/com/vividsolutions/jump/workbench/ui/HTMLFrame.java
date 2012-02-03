
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;

import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * At the bottom-left corner is an MS-Access-style record navigator for
 * cycling through the history of documents. A new document is created when
 * #createNewDocument is called.
 * <P>
 * Methods can be called regardless of whether or not the current thread is the
 * AWT event dispatching thread.
 */

//Rather inefficient -- uses JEditorPane#setText rather than
//HTMLDocument#insertBeforeEnd. But #insertBeforeEnd is buggy (see Java Bug
//4496801) [Jon Aquino].
public class HTMLFrame extends JInternalFrame{
    
    private WorkbenchFrame workbenchFrame;
    private BorderLayout borderLayout1 = new BorderLayout();

    protected boolean alwaysOnTop = false;
    private boolean notifyingUser = false;
    private JButton button = null;

    /**
     * Do not use this constructor. It is here to satisfy JBuilder's GUI
     * designer when it opens WorkbenchFrame.
     */
    public HTMLFrame() {}
    private HTMLPanel panel = new HTMLPanel() {
        protected void setEditorPaneText() {
            super.setEditorPaneText();
            notifyUser();            
        } 
    };
    public HTMLFrame(final WorkbenchFrame workbenchFrame) {
        this.workbenchFrame = workbenchFrame;
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        } 
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent e) {
                try {
                    workbenchFrame.removeInternalFrame(HTMLFrame.this);
                } catch (Exception x) {
                    workbenchFrame.handleThrowable(x);
                }
            }

            public void internalFrameOpened(InternalFrameEvent e) {
                panel.getRecordPanel().updateAppearance();
            }
        });
        setSize(500, 300);
    }

    public void setCurrentIndex(int index) {
        panel.setCurrentIndex(index);
    }

    public void setTitle(String s) {
        super.setTitle(s);
    }

    public void setBackgroundColor(Color color) {
        panel.setBackgroundColor(color);
    }

    public Color getBackgroundColor() {
        return panel.getBackgroundColor();
    }

    public int getRecordCount() {
        return panel.getRecordCount();
    }

    public int getCurrentIndex() {
        return panel.getCurrentIndex();
    }

    public void createNewDocument() {
        panel.createNewDocument();
    }

    public void setRecordNavigationControlVisible(boolean visible) {
        panel.setRecordNavigationControlVisible(visible);
    }

    /**
     * @deprecated Use #createNewDocument instead.
     */
    public void clear() {
        panel.createNewDocument();
    }

    /**
     *  Brings the output window to the front. Adds it to the desktop if
     *  necessary.
     */
    public void surface() {
        if (!workbenchFrame.hasInternalFrame(this)) {
            workbenchFrame.addInternalFrame(this, alwaysOnTop, true);
        }

        workbenchFrame.activateFrame(this);

        if (isIcon()) {
           try {
                 setIcon(false);
            } catch (PropertyVetoException e) {
                workbenchFrame.log(StringUtil.stackTrace(e));
            }
        }

        moveToFront();
    }

    /**
     *@param  level  1, 2, 3, ...
     */
    public void addHeader(int level, String text) {
        panel.addHeader(level, text);
    }

    public void addField(String label, String value) {
        panel.addField(label, value);
    }

    public void addField(String label, String value, String units) {
        panel.addField(label, value, units);
    }

    /**
     * Appends a line of non-HTML text to the frame.  Text is assumed to be non-HTML, and is
     * HTML-escaped to avoid control-char conflict.
     * @param text
     */
    public void addText(String text) {
        panel.addText(text);
    }

    /**
     * Appends HTML text to the frame.
     * @param html the HTML to append
     */
    public void append(final String html) {
        panel.append(html);
    }



    public void setButton(JButton button) {
        this.button = button;
    }

    private void setButtonHighlighted(boolean highlighted) {
        if (button == null) {
            return;
        }

        button.setIcon(highlighted ? IconLoader.icon("Frame2.gif") : IconLoader.icon("Frame.gif"));
    }

    private void notifyUser() {
        if (notifyingUser) {
            return;
        }
        
        if (button == null) {
            return;
        }        

        notifyingUser = true;
        new Timer(500, new ActionListener() {
            private int tickCount = 0;

            public void actionPerformed(ActionEvent e) {
                tickCount++;
                setButtonHighlighted((tickCount % 2) == 0);

                if (tickCount == 8) {
                    Timer timer = (Timer) e.getSource();
                    timer.stop();
                    notifyingUser = false;
                    setButtonHighlighted(!isSelected());
                }
            }
        }).start();
    }





    private void jbInit() throws Exception {
        setTitle("Output");
        this.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameActivated(InternalFrameEvent e) {
                this_internalFrameActivated(e);
            }
        });
        this.getContentPane().setLayout(borderLayout1);
        getContentPane().add(panel, BorderLayout.CENTER);        
        this.setResizable(true);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        panel.getOKButton().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    okButton_actionPerformed(e);
                }
            });        
    }



    public void scrollToTop() {
        panel.scrollToTop();
    }

    void okButton_actionPerformed(ActionEvent e) {
        doDefaultCloseAction();
    }

    void this_internalFrameActivated(InternalFrameEvent e) {
        setButtonHighlighted(false);
    }
}
