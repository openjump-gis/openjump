package org.openjump.core.ui.plugin.datastore.transaction;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.openjump.core.ui.plugin.datastore.WritableDataStoreDataSource;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerAdapter;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.TaskFrame;
import com.vividsolutions.jump.workbench.ui.plugin.WorkbenchContextReference;

/**
 * Panel displaying current uncommitted edits and including the commit button.
 */
public class TransactionManagerPanel extends JPanel  implements WorkbenchContextReference {

    private static final String KEY = TransactionManagerPanel.class.getName();



    final DataStoreTransactionManager transactionManager;
    final ErrorHandler errorHandler;
    final JTextArea textArea;
    LayerListener layerListener;
    WorkbenchContext context;

    public TransactionManagerPanel(DataStoreTransactionManager transactionManager,
                                   ErrorHandler errorHandler, WorkbenchContext context) {
        this.transactionManager = transactionManager;
        this.errorHandler = errorHandler;
        textArea = new JTextArea(12,32);
        init(context);
    }

    private void init(WorkbenchContext context) {
        this.context = context;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        textArea.setFont(textArea.getFont().deriveFont(11f));
        this.add(new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2,4,2,4);
        c.gridx = 0;
        c.gridy = 0;

        JLabel experimental = new JLabel("<html><font size='6' color='red'>Experimental</font><html>");
        c.gridwidth = 2;
        panel.add(experimental, c);

        JButton inspectButton = new JButton(I18N.get(KEY + ".inspect"));
        inspectButton.setToolTipText(I18N.get(KEY + ".inspect-tooltip"));
        inspectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transactionManager.inspect(getTaskFrame());
            }
        });
        c.gridx = 1;
        c.gridy += 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(inspectButton, c);

        JButton updateButton = new JButton(I18N.get(KEY + ".update"));
        updateButton.setToolTipText(I18N.get(KEY + ".update-tooltip"));
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transactionManager.update(getTaskFrame());
            }
        });
        c.gridx = 0;
        c.gridy += 1;
        panel.add(updateButton, c);

        JButton commitButton = new JButton(I18N.get(KEY + ".commit"));
        commitButton.setToolTipText(I18N.get(KEY + ".commit-tooltip"));
        commitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    transactionManager.commit();
                    updateTextArea(JUMPWorkbench.getInstance().getContext().getTask());
                } catch(Exception ex) {
                    errorHandler.handleThrowable(ex);
                }
            }
        });
        c.gridx = 1;
        //c.gridy = 1;
        panel.add(commitButton, c);

        this.add(panel);

        //updateListener(context.getTask());
    }

    /**
     * Remove the layerListener displaying feature events in the text area
     * create a new one and add it to the TaskFrame associated to task.
     * This method keep TransactionManagerPanel in sync with current TaskFrame.
     */
    public void updateListener(final Task task) {
        if (task == null) return;
        for (JInternalFrame iframe : JUMPWorkbench.getInstance().getFrame().getInternalFrames()) {
            if (iframe instanceof TaskFrame) {
                ((TaskFrame)iframe).getTask().getLayerManager().removeLayerListener(layerListener);
            }
        }
        layerListener = new LayerAdapter() {
            public void featuresChanged(FeatureEvent e) {
                Layer layer = e.getLayer();
                Collection layers = transactionManager.getLayers();
                if (layers.contains(layer)) {
                    DataSource dataSource = layer.getDataSourceQuery().getDataSource();
                    if (dataSource instanceof WritableDataStoreDataSource) {
                        updateTextArea(task);
                    }
                    else {
                        Logger.error(I18N.get(KEY + ".layer-with-irrelevant-datastore-datasource"));
                    }
                }
            }
            public void layerChanged(LayerEvent e) {
                if (e.getType() == LayerEventType.REMOVED) {
                    updateTextArea(task);
                }
            }
        };
        for (JInternalFrame iframe : JUMPWorkbench.getInstance().getFrame().getInternalFrames()) {
            if (iframe instanceof TaskFrame) {
                if (((TaskFrame)iframe).getTask() == task) {
                    LayerManager manager = ((TaskFrame)iframe).getTask().getLayerManager();
                    // In case of cloned windows, add the listener only once
                    manager.removeLayerListener(layerListener);
                    manager.addLayerListener(layerListener);
                    //((TaskFrame)iframe).getTask().getLayerManager().addLayerListener(layerListener);
                }
            }
        }
        updateTextArea(task);
    }

    void updateTextArea(Task task) {
        textArea.setText("");
        for (Layer layer : transactionManager.getLayers()) {
            // @TODO is it safe to use != ?
            if (transactionManager.getTask(layer) != task) continue;
            DataSource source = layer.getDataSourceQuery().getDataSource();
            if (source instanceof WritableDataStoreDataSource) {
                int c = 0, m = 0, s = 0;
                for (Evolution evo : ((WritableDataStoreDataSource)source).getUncommittedEvolutions()) {
                    if (evo.getType() == Evolution.Type.CREATION) c++;
                    if (evo.getType() == Evolution.Type.SUPPRESSION) s++;
                    if (evo.getType() == Evolution.Type.MODIFICATION) m++;
                }
                if (c+m+s>0) textArea.append(layer.getName()+":\n");
                if (c>0) textArea.append(I18N.getMessage(KEY + ".creations", c) + "\n");
                if (s>0) textArea.append(I18N.getMessage(KEY + ".suppressions", s) + "\n");
                if (m>0) textArea.append(I18N.getMessage(KEY + ".modifications", m) + "\n");
            }
        }
    }

    TaskFrame getTaskFrame() {
        return context.getWorkbench().getFrame().getActiveTaskFrame();
    }

    public void setWorkbenchContext(WorkbenchContext context) {
        this.context = context;
    }

}
