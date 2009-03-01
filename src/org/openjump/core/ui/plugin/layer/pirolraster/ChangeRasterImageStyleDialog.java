/*
 * Created on 03.07.2005
 *
 * CVS information:
 *  $Author: LBST-PF-3\orahn $
 *  $Date: 2006-09-12 12:57:25 +0000 (Di, 12 Sep 2006) $
 *  $ID$
 *  $Rev: 2446 $
 *  $Id: ChangeRasterImageStyleDialog.java 2446 2006-09-12 12:57:25Z LBST-PF-3\orahn $
 *  $Log: ChangeRasterImageStyleDialog.java,v $
 *
 */
package org.openjump.core.ui.plugin.layer.pirolraster;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.ui.swing.DialogTools;
import org.openjump.core.ui.swing.OkCancelButtonPanel;
import org.openjump.core.ui.swing.ValueChecker;
import org.openjump.core.ui.swing.listener.OKCancelListener;

import com.vividsolutions.jump.I18N;


/**
 * 
 * Dialog that show controlls to customize the appearance og a
 * RasterImage layer. 
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2446 $
 * [sstein] - 22.Feb.2009 - modified to work in OpenJUMP
 */
public class ChangeRasterImageStyleDialog extends JDialog {

    private static final long serialVersionUID = -8476427365953412168L;

    private javax.swing.JPanel jContentPane = null;
	
	protected OkCancelButtonPanel okCancelPanel = new OkCancelButtonPanel();
	protected OKCancelListener okCancelListener = null;

    private RasterImageLayer rasterImageLayer = null;
    
	/**
	 * This is the default constructor
	 */
	public ChangeRasterImageStyleDialog(RasterImageLayer rasterImageLayer, Frame parent, String title, boolean modal) {
		super(parent, title, modal);
		this.rasterImageLayer = rasterImageLayer;
		initialize();
	}
	/**
	 * This method initializes this
	 */
	private void initialize() {
		this.setSize(500,530);
		this.setContentPane(getJContentPane());
		
		okCancelListener = new OKCancelListener(this);
		this.okCancelPanel.addActionListener(this.okCancelListener);
		
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(15);
		this.jContentPane.setLayout(borderLayout);
		this.jContentPane.add(DialogTools.getPanelWithLabels(I18N.get("org.openjump.core.ui.plugin.layer.pirolraster.ChangeRasterImageStyleDialog.Change-RasterImage-Style-Dialog-intro-text"), 65), BorderLayout.NORTH); //$NON-NLS-1$
		
		JPanel controllsPanel = new RasterImageLayerControllPanel(this.rasterImageLayer);
		this.jContentPane.add(controllsPanel, BorderLayout.CENTER);
		this.jContentPane.add(this.okCancelPanel, BorderLayout.SOUTH);
		this.okCancelListener.addValueChecker((ValueChecker)controllsPanel);
		
		this.jContentPane.doLayout();
	}
	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJContentPane() {
		if(jContentPane == null) {
			jContentPane = new javax.swing.JPanel();
			BorderLayout bl = new BorderLayout();
			bl.setHgap(15);
			bl.setVgap(15);
			jContentPane.setLayout(bl);
		}
		return jContentPane;
	}
	
	/**
	 *@see OKCancelListener#wasOkClicked()
	 */
    public boolean wasOkClicked() {
        return okCancelListener.wasOkClicked();
    }
}
