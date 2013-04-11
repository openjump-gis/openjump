
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

package com.vividsolutions.jump.workbench.ui.task;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.ui.AnimatedClockPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;


public class TaskMonitorDialog extends JDialog implements TaskMonitor {
    JPanel mainPanel = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JPanel labelPanel = new JPanel();
    private JButton cancelButton = new JButton();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private ErrorHandler errorHandler;
    private boolean cancelled;
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private JLabel taskProgressLabel = new JLabel();
    private JLabel subtaskProgressLabel = new JLabel();
    private String taskProgress = "";
    private String subtaskProgress = "";
    private Timer timer = new Timer(500,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateLabels();
                }
            });
    private AnimatedClockPanel clockPanel = new AnimatedClockPanel();

    public TaskMonitorDialog(Frame frame, ErrorHandler errorHandler) {
        this(frame, errorHandler, true);
    }

    public TaskMonitorDialog(Frame frame, ErrorHandler errorHandler, boolean modal) {
        super(frame, I18N.get("ui.task.TaskMonitorDialog.busy"), modal);
        this.errorHandler = errorHandler;

        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setSize(400, 100);
        addWindowListener(new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    cancelButton.setEnabled(true);
                }
            });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void jbInit() throws Exception {
        mainPanel.setLayout(gridBagLayout1);
        cancelButton.setText(I18N.get("ui.task.TaskMonitorDialog.cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancelButton_actionPerformed(e);
                }
            });
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentShown(ComponentEvent e) {
                    this_componentShown(e);
                }

                public void componentHidden(ComponentEvent e) {
                    this_componentHidden(e);
                }
            });
        this.getContentPane().setLayout(gridBagLayout2);
        labelPanel.setLayout(gridBagLayout3);
        subtaskProgressLabel.setText(I18N.get("ui.task.TaskMonitorDialog.subtask-progress-goes-here"));
        taskProgressLabel.setText(I18N.get("ui.task.TaskMonitorDialog.task-progress-goes-here"));
        getContentPane().add(mainPanel,
            new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(15, 0, 15, 15), 0, 0));
        mainPanel.add(labelPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        labelPanel.add(taskProgressLabel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        labelPanel.add(subtaskProgressLabel,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(cancelButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        this.getContentPane().add(clockPanel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(4, 4, 4, 4), 0, 0));
    }

    void cancelButton_actionPerformed(ActionEvent e) {
        cancelButton.setEnabled(false);
        cancelled = true;
    }

    void this_componentHidden(ComponentEvent e) {
        clockPanel.stop();
        timer.stop();
    }

    void this_componentShown(ComponentEvent e) {
        cancelled = false;
        updateLabels();
        cancelButton.setVisible(false);
        timer.start();
        clockPanel.start();
    }

    private void updateLabels() {
        taskProgressLabel.setText(taskProgress);
        subtaskProgressLabel.setText(subtaskProgress);
    }

    public void setRefreshRate(int millisecondDelay) {
        //This feature was requested by Georgi Kostadinov. [Jon Aquino]
        timer.setDelay(millisecondDelay);
    }

    public void report(final String description) {
        this.taskProgress = description;
        subtaskProgress = "";
    }

    public void report(int subtasksDone, int totalSubtasks,
        String subtaskDescription) {
        subtaskProgress = "";
        subtaskProgress += subtasksDone;

        if (totalSubtasks != -1) {
            subtaskProgress += (" / " + totalSubtasks);
        }

        subtaskProgress += (" " + subtaskDescription);
    }

    public void allowCancellationRequests() {
        cancelButton.setVisible(true);
    }

    public void report(Exception exception) {
        errorHandler.handleThrowable(exception);
    }

    public boolean isCancelRequested() {
        return cancelled;
    }
}
