/*
 * Created on 30.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2446 $
 *  $Date: 2006-09-12 14:57:25 +0200 (Di, 12 Sep 2006) $
 *  $Id: StatisticOverViewDialog.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 */
package org.openjump.core.ui.plugin.tools.statistics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.ui.OKCancelPanel;


/**
 * Dialog to show the results of the quick statistical overview (means, deviation, etc.) in a table, that 
 * allows to identify features, that hold min. and max.values in the map. 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2446 $
 * modified: [sstein] 16.Feb.2009 to work with OpenJUMP
 */
public class StatisticOverViewDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 6715515050854850413L;


    /**
     *@param parentFrame the parent frame of this dialog box
     *@param title title of this dialog box
     *@param modal whether the dialog box is modal or not
     *@throws java.awt.HeadlessException
     */
    public StatisticOverViewDialog(Frame parentFrame, String title, boolean modal, Feature[] features)
            throws HeadlessException {
        super(parentFrame, title, modal);

        StatisticOverViewTableModel tableModel = new StatisticOverViewTableModel(features);
        
        this.setupGUI(tableModel);
    }
    
    private void setupGUI(StatisticOverViewTableModel tableModel){
        JPanel content = new JPanel();
        
        BorderLayout bl = new BorderLayout();
        bl.setHgap(5);
        bl.setVgap(5);
        content.setLayout(bl);
        
        //-- [sstein] replace line by line below
        JTable table = new JTable(tableModel);
        table.setRowHeight(22);

        JScrollPane scrollPane = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(500, 300));
        
        content.add(scrollPane, BorderLayout.CENTER);

        OKCancelPanel okButtonPanel = new OKCancelPanel();
        okButtonPanel.addActionListener(this);
        
		content.add(okButtonPanel,BorderLayout.SOUTH);
		
        content.doLayout();
        
        this.getContentPane().add(content);
        this.pack();
    }

    public void actionPerformed(ActionEvent arg0) {
        this.setVisible(false);
        this.dispose();
    }


}
