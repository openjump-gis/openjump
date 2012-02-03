//$Header$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2005 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/exse/
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
 Aennchenstraﬂe 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 
 ---------------------------------------------------------------------------*/

package de.latlon.deejump.plugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerTreeModel;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.ColorPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;

/**
 * ... 
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author$
 * 
 * @version 2.0, $Revision$, $Date$
 * 
 * @since 2.0
 */

public class SaveLegendPlugIn extends AbstractPlugIn {

	public SaveLegendPlugIn() {
		//nothing
	}

	private String filename = null;
	
	public void initialize(PlugInContext context) throws Exception {
		
		context.getFeatureInstaller().addPopupMenuItem(
				context.getWorkbenchContext().getWorkbench().getFrame().getLayerNamePopupMenu(), 
				this, 
				this.getName()+"{pos:13}", 				
				false, 
				null, 
				null);		
	}    
	
	public boolean execute(PlugInContext context) throws Exception {
		
		final Layer layer = context.getSelectedLayer(0);
		
		LayerNamePanel layerPanel = context.getLayerNamePanel();
		if ( !(layerPanel instanceof TreeLayerNamePanel ) ) {
			return false;
			
		} 
		
		JTree newTree = 
			createLayerTree( layer, (TreeLayerNamePanel)layerPanel );
		
		saveLegend( context, newTree );
		
		return true;
	}

	private void saveLegend( PlugInContext context, JTree tree ) 
		throws IOException {
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setApproveButtonText( I18N.get("deejump.plugin.SaveLegendPlugIn.Save"));
		fileChooser.setDialogTitle( I18N.get("deejump.plugin.SaveLegendPlugIn.Save-legend-as-image-png"));
        
		
		JPanel p = new JPanel();
		p.add( tree );
		
		fileChooser.setAccessory( p );
		
        if (JFileChooser.APPROVE_OPTION == 
            	fileChooser.showOpenDialog(context.getWorkbenchFrame())) {
        
        	
        	// can only save if has a sizw -> put in a frame
        	JFrame f = new JFrame(I18N.get("deejump.plugin.SaveLegendPlugIn.Save-legend-as-image-png"));
    		f.getContentPane().add( tree );
    		f.pack();
//    		f.setVisible( true );
    		
        	saveComponentAsJPEG( tree, 
        				fileChooser.getSelectedFile().getAbsolutePath() );
        	
        	f.setVisible( false );
        	f.dispose();
    		
        }
        
	}

	private JTree createLayerTree( Layer layer, TreeLayerNamePanel treePanel){
		
		
		JTree tree = treePanel.getTree();
		
		DefaultMutableTreeNode rootNode = 
			new DefaultMutableTreeNode( layer.getName() );
				
		for (int j = 0; j < tree.getModel().getChildCount(layer); j++) {
			rootNode.add( 
					new DefaultMutableTreeNode(
							tree.getModel().getChild(layer, j) ) );
		}
		JTree newTree = new JTree( rootNode ); 
		newTree.setCellRenderer( createColorThemingValueRenderer() );
		
		return newTree;
	}
	
	public String getName() {
		return I18N.get("deejump.plugin.SaveLegendPlugIn.Save-legend");
	}
	
	public static void saveComponentAsJPEG(Component myComponent, 
			String filename)
	
		throws IOException {
		//-- [sstein, 06.08.2006]
		if (!filename.endsWith(".png")){
			filename=filename + ".png";
		}
		//--
		Dimension size = myComponent.getSize();
		BufferedImage myImage = 
			new BufferedImage(size.width, size.height,
			BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = myImage.createGraphics();
			myComponent.paint(g2);
			
			
			ImageIO.write( (RenderedImage)myImage, "PNG", new File(filename) );
			
	}
	
	private TreeCellRenderer createColorThemingValueRenderer() {
        return new TreeCellRenderer() {
            private JPanel panel = new JPanel(new GridBagLayout());
            private ColorPanel colorPanel = new ColorPanel();
            private JLabel label = new JLabel();
            {	
                panel.add(colorPanel, 
                		new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
                
                panel.add(label, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,5,0,0), 0, 0));
                
                panel.setBackground( Color.white );
                label.setBackground( Color.white );
                
            }
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
            	
            	String txt = ""; 
            	
            	DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
            	Object userObject = node.getUserObject();
            		
    			if ( userObject instanceof LayerTreeModel.ColorThemingValue ){
    				
    				LayerTreeModel.ColorThemingValue entry = 
    					(LayerTreeModel.ColorThemingValue)userObject;
            		
            		txt = entry.toString();
            		
            		BasicStyle style = entry.getStyle();
            		colorPanel.setLineColor(style.isRenderingLine()
            				? GUIUtil.alphaColor(style.getLineColor(), style
            						.getAlpha())
            					: GUIUtil.alphaColor(Color.BLACK, 0));
            		colorPanel.setFillColor(style.isRenderingFill()
            				? GUIUtil.alphaColor(style.getFillColor(), style
            						.getAlpha())
        						: GUIUtil.alphaColor(Color.BLACK, 0));
            
            	} else {
            		
            		txt = (String)userObject; 
                    colorPanel.setFillColor( Color.white );
            		colorPanel.setLineColor( Color.white );
            	}
            	
                label.setText( txt );
                
                
                return panel;
            }
        };
    }

}


/* ********************************************************************
Changes to this class. What the people have been up to:
$Log$
Revision 1.2  2006/08/06 16:48:28  mentaer
changed menu pos
improved SaveLegendPlugIn by adding file ending ".png"

Revision 1.1  2006/08/06 16:22:11  mentaer
added savelegend

Revision 1.1  2006/03/06 09:42:11  ut
latest changes (key pan, legend, etc)


********************************************************************** */