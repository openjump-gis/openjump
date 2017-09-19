
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.RefreshRated;
import com.vividsolutions.jump.task.TaskMonitorV2;
import com.vividsolutions.jump.workbench.ui.AnimatedClockPanel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;


public class TaskMonitorDialog extends JDialog implements TaskMonitorV2, RefreshRated {
    private JPanel mainPanel = new JPanel();
    private JPanel labelPanel = new JPanel();

    private JButton cancelButton = new JButton();
    boolean allowCancellation = false;

    private ErrorHandler errorHandler;
    private boolean cancelled;
 
    private JTextComponent taskProgressLabel;
    private Component separator;
    private JTextComponent subtaskProgressLabel;
    public String taskProgress = "";
    public String subtaskProgress = "";
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
          this.setLayout(new GridBagLayout());
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        addWindowListener(new WindowAdapter() {
                public void windowOpened(WindowEvent e) {
                    cancelButton.setEnabled(true);
                }
            });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void jbInit() throws Exception {
        mainPanel.setLayout(new GridBagLayout());
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

        labelPanel.setLayout(new GridBagLayout());

        taskProgressLabel = createWrapLabel("");
        taskProgressLabel.setText(I18N.get("ui.task.TaskMonitorDialog.task-progress-goes-here"));

        separator = Box.createRigidArea(new Dimension(10,10));
        
        subtaskProgressLabel = createWrapLabel("");
        subtaskProgressLabel.setText(I18N.get("ui.task.TaskMonitorDialog.subtask-progress-goes-here"));

        mainPanel.add(labelPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        labelPanel.add(taskProgressLabel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        labelPanel.add(separator,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        labelPanel.add(subtaskProgressLabel,
            new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(clockPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 10, 0));
        centerPanel.add(mainPanel,
            new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        setLayout(new GridBagLayout());
        add(centerPanel,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10), 0, 0));
        add(cancelButton,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 0), 0, 0));
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
        cancelButton.setVisible(allowCancellation);
        timer.start();
        clockPanel.start();
        updateLabels();
    }

    private void updateLabels() {
        if (cancelled)
          return;

        taskProgressLabel.setText(taskProgress);
        taskProgressLabel.setVisible(!taskProgress.isEmpty());
        
        separator.setVisible(!taskProgress.isEmpty() && !subtaskProgress.isEmpty());
        
        subtaskProgressLabel.setText(subtaskProgress);
        subtaskProgressLabel.setVisible(!subtaskProgress.isEmpty());
        pack();
    }

    public int getRefreshRate() {
      return timer.getDelay();
    }

    public void setRefreshRate(int millisecondDelay) {
        //This feature was requested by Georgi Kostadinov. [Jon Aquino]
        timer.setDelay(millisecondDelay);
    }

    public void report(final String description) {
        this.taskProgress = description;
        subtaskProgress = "";
    }

    public void report(long subtasksDone, long totalSubtasks,
        String subtaskDescription) {
        subtaskProgress = "";
        subtaskProgress += subtasksDone;

        if (totalSubtasks != -1) {
            subtaskProgress += (" / " + totalSubtasks);
        }

        subtaskProgress += (" " + subtaskDescription);
    }

    public void report(int subtasksDone, int totalSubtasks,
        String subtaskDescription) {
        report((long)subtasksDone, (long)totalSubtasks, subtaskDescription);
    }

    public void allowCancellationRequests() {
        allowCancellation = true;
        cancelButton.setVisible(true);
    }

    public void report(Exception exception) {
        errorHandler.handleThrowable(exception);
    }

    public boolean isCancelRequested() {
        return cancelled;
    }

    public void setTitle(String title){
      super.setTitle(title);
    }

    /**
     * a wrapping label workaround
     * @param text
     * @return
     */
    protected JTextComponent createWrapLabel(String text) {
      // set a default col width to break at
      JTextArea textArea = new JTextArea(0,30);
      textArea.setText(text);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setOpaque(false);
      textArea.setEditable(false);
      textArea.setFocusable(false);
      textArea.setBackground(UIManager.getColor("Label.background"));
      textArea.setFont(UIManager.getFont("Label.font"));
      textArea.setBorder(UIManager.getBorder("Label.border"));
      return textArea;
    }

    /**
     * testing layout and overall functionality
     */
    public static void main(String[] args) {
      final TaskMonitorDialog d = new TaskMonitorDialog(null, null);
      d.report("This is what we are doing.");
      //d.allowCancellationRequests();
      new Thread(new Runnable() {
        
        @Override
        public void run() {
          d.pack();
          d.show();
        }
      }).start();
      for (int i = 0; i < 1000; i++) {

        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        d.report(i, 1000, "irgendwas");
        String t = d.taskProgress;
        d.taskProgress += "palimipalim";
      }
    }
}
