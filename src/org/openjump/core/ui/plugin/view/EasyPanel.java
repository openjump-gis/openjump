/*
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2009 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida 32548
 * USA
 *
 * (850)862-7321
 */

package org.openjump.core.ui.plugin.view;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.toolbox.ToolboxDialog;


public class EasyPanel extends JPanel {

    public final static String EZ_LIST_KEY = EasyPanel.class.getName()+"EZ_LIST_KEY";

    //private final String UNUSED_BUTTON_NAME = "Right Click to Assign Button F";
    //private final String RIGHT_CLICK_MENU = "Map Right Click";

    private String UNUSED_BUTTON_NAME = I18N.get("org.openjump.core.ui.plugin.view.EasyPanel.Right-Click-to-Assign-Button-F");
    private String RIGHT_CLICK_MENU = I18N.get("org.openjump.core.ui.plugin.view.EasyPanel.Map-Right-Click");
    
    private JPanel buttonPanel = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();
    private final int MAX_BUTTONS = 10;
    private CustomButton[] buttons = new CustomButton[MAX_BUTTONS];
    private CustomButton activeButton = null;
	private JPopupMenu masterPopup = new JPopupMenu();
	private JMenu rightClickMenu = new JMenu(RIGHT_CLICK_MENU);
	private ArrayList<JMenuItem> menuItemList = new ArrayList<JMenuItem>();
	private ArrayList<String> menuNameList = new ArrayList<String>();
	private ToolboxDialog toolbox;
	
    public EasyPanel(ToolboxDialog toolbox) {
    	this.toolbox = toolbox;
        this.add(buttonPanel); 
        buttonPanel.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(MAX_BUTTONS);
        addButtons();
        populatePopupMenu(toolbox.getContext());
        recallButtonAssignments();
        toolbox.pack();
    }
    
    private void addButtons(){
	    GridBagConstraints gbc = new GridBagConstraints();
    	for (int i=0; i<MAX_BUTTONS; i++) {
    		buttons[i] = new CustomButton(i);
    	    buttonPanel.add( buttons[i],gbc);  
    	    MouseListener popupListener = new PopupListener(masterPopup);
    	    buttons[i].addMouseListener(popupListener);
    	    buttons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	buttonActionPerformed(e);
                }
            });
    	}
    }
    
    private void recallButtonAssignments() {
    	ArrayList buttonNameList = getPersistentButtonList(toolbox.getContext());
    	int buttonNumber = 0;
    	int n = 0;
    	for (Iterator i=buttonNameList.iterator(); i.hasNext();  ) {
    		String name = (String) i.next();
    		int index = menuNameList.indexOf(name);
    		if (index > -1) {
        		JMenuItem jMenuItemToSet = menuItemList.get(index);
        		buttons[buttonNumber].setMenuItem(jMenuItemToSet);
        		String buttonName = buttons[buttonNumber].getText();
        		buttons[buttonNumber].setText(buttonName + "  F" +(++n));
    		} else{
    			buttons[buttonNumber].setText(UNUSED_BUTTON_NAME+(++n));
    		}
    		buttonNumber++;
    	}   	
    }
    
    private void buttonActionPerformed(ActionEvent e) {
    	String name = e.getActionCommand();
    	CustomButton button = null;
    	for (int i=0; i<buttons.length; i++) {
    		if (buttons[i].getText().equalsIgnoreCase(name)) {
    			button = buttons[i];
    			break;
    		}
    	}
    	if (button != null) 
    		button.executeMenuItem();
     }
    
	private void populatePopupMenu(WorkbenchContext context) {
		JMenuBar jMenuBar = context.getWorkbench().getFrame().getJMenuBar();
		int menuCount = jMenuBar.getMenuCount();
		for (int i=0; i<menuCount; i++) {
			JMenu jMenuRef = jMenuBar.getMenu(i);
			masterPopup.add(populateMenu(jMenuRef));
		}
		//skip Layer Name right click for now
//		JPopupMenu jPopupMenu = context.getWorkbench().getFrame().getLayerNamePopupMenu();
//		MenuElement[] menuElement = jPopupMenu.getSubElements(); 
		
		JPopupMenu jPopupMenu = LayerViewPanel.popupMenu(); 
		int itemCount = jPopupMenu.getComponentCount(); //menuElement.length;
		for (int j=0; j<itemCount; j++) {
			if (jPopupMenu.getComponent(j) instanceof JMenuItem) {
				JMenuItem jMenuItemRef = (JMenuItem) jPopupMenu.getComponent(j);
				JMenuItem jMenuItem = new CustomJMenuItem(jMenuItemRef);
				rightClickMenu.add(jMenuItem);
				jMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						menuItemActionPerformed(e);
					}
				});
			} else {
				rightClickMenu.insertSeparator(j);
			}				
		}
		masterPopup.add(rightClickMenu);
	}
	
	private JMenuItem populateMenu(JMenu jMenuRef) {
		JMenu jMenu = new JMenu(jMenuRef.getText());
		int itemCount = jMenuRef.getItemCount();
		for (int j=0; j<itemCount; j++) {
			JMenuItem jMenuItemRef = jMenuRef.getItem(j);
			if (jMenuItemRef instanceof JMenu) {
				jMenu.add( populateMenu((JMenu) jMenuItemRef));  //Recurse
			} else {
				if (jMenuItemRef instanceof JMenuItem) {
					JMenuItem jMenuItem = new CustomJMenuItem(jMenuItemRef);
					menuNameList.add(jMenuItem.getText());
					menuItemList.add(jMenuItemRef);
					jMenu.add(jMenuItem);
					jMenuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							menuItemActionPerformed(e);
						}
					});
				} else {
					jMenu.insertSeparator(j);
				}
			}
		}
		return jMenu;
	}
	
	private void menuItemActionPerformed(ActionEvent e) {
		Object source = e.getSource();
		String name = e.getActionCommand();
		if (activeButton != null) {
			if (source instanceof CustomJMenuItem) {
				CustomJMenuItem jMenuItem = (CustomJMenuItem) source;
				JMenuItem referencedJMenuItem = jMenuItem.getReferencedJMenuItem();
				activeButton.setMenuItem(referencedJMenuItem);
			}
		}
        toolbox.pack();
	}
	
    private ArrayList getPersistentButtonList(WorkbenchContext context) {
        Blackboard blackboard = PersistentBlackboardPlugIn.get(context);
        if (blackboard.get(EZ_LIST_KEY) == null) {
        	ArrayList list = new ArrayList(MAX_BUTTONS);
            for (int i=0; i<MAX_BUTTONS; i++) {
            	list.add(new String(UNUSED_BUTTON_NAME+(i+1)));
            }
            blackboard.put(EZ_LIST_KEY, list);
        }
        ArrayList list =  (ArrayList) blackboard.get(EZ_LIST_KEY);
        return list; 
    }

    private void setPersistentButtonList(WorkbenchContext context, int index, String buttonName) { 
    	ArrayList buttonNameList = getPersistentButtonList(context);
    	if (index < MAX_BUTTONS)
    		buttonNameList.set(index,buttonName);
        Blackboard blackboard = PersistentBlackboardPlugIn.get(context);
        blackboard.put(EZ_LIST_KEY, buttonNameList);
    }


    class PopupListener extends MouseAdapter {
        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
         }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Component component = e.getComponent();
                popup.show(component, e.getX(), e.getY());
                if (component instanceof CustomButton) {
                	activeButton = (CustomButton) component;
                }
            }
        }
    }

    class CustomButton extends JButton { //implements ChangeListener{
    	private JMenuItem jMenuItem = null;
    	private int buttonNumber = 0;
    	 
    	public CustomButton(int buttonNumber) {
			super(UNUSED_BUTTON_NAME+(buttonNumber+1));
			this.buttonNumber = buttonNumber;
			this.setToolTipText("F"+(buttonNumber+1));
//			this.setMnemonic(KeyEvent.VK_F1+buttonNumber);  //Mnenoics are useless - need focus
		    toolbox.getContext().getWorkbench().getFrame()
		    	.addKeyboardShortcut(KeyEvent.VK_F1+buttonNumber,
		                0, 
		                new PlugIn() {  //inline a plugin
							public boolean execute(PlugInContext context) throws Exception {
								executeMenuItem();
								return false;
							}

							public String getName() {
								return null;
							}
							public void initialize(PlugInContext context) throws Exception {								
							}		    	
		   }
		                , new MultiEnableCheck());
		}
    	
		public void setMenuItem(JMenuItem jMenuItemToSet) {
    		jMenuItem = jMenuItemToSet;
			this.setText(jMenuItem.getText());
    		setPersistentButtonList(toolbox.getContext(), buttonNumber, jMenuItem.getText());
			//ChangeListener isn't fired unless you drop down a menu
//			if (jMenuItem != null) {
//				ChangeListener[] changeListeners = jMenuItem.getChangeListeners();
//				for (int i=0; i<changeListeners.length; i++) {
//					if (changeListeners[i] == this) { // instanceof CustomButton) {
//						jMenuItem.removeChangeListener(changeListeners[i]);
//					}
//				}
//			}
//    		this.setToolTipText(jMenuItem.getToolTipText());
//    		jMenuItem.addChangeListener(this);
     	}
		
		public void executeMenuItem() {
			if (jMenuItem != null) {
				//fire an event to run the EnableCheck
				Component component = ((JPopupMenu) jMenuItem.getParent()).getInvoker();
				if (component instanceof JMenu) {
					JMenu jMenu = (JMenu) component;
					MenuListener[] menuListeners = jMenu.getMenuListeners();
					for (int i=0; i<menuListeners.length; i++) {
						if (menuListeners[i] instanceof FeatureInstaller.JumpMenuListener) {
							((FeatureInstaller.JumpMenuListener) menuListeners[i]).menuSelected(null);							
						}
					}
				} else {
					JPopupMenu popupMenu = ((JPopupMenu) jMenuItem.getParent());   			
					PopupMenuListener[] listeners = popupMenu.getListeners(PopupMenuListener.class);
					for (int i=0; i<listeners.length; i++) {
						if (listeners[i] instanceof PopupMenuListener) {
							((PopupMenuListener) listeners[i])
								.popupMenuWillBecomeVisible(new PopupMenuEvent(popupMenu));
						}
					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (jMenuItem.isEnabled())
							jMenuItem.doClick();
						else {
							String toolTip = jMenuItem.getToolTipText();
							setToolTip(toolTip);           			
						}
					}});
			}
		}
		
    	public void setButtonEnabled(boolean enabled){
    		this.setEnabled(enabled);
    	}
//		public void stateChanged(ChangeEvent e) {
//			setButtonEnabled(jMenuItem.isEnabled());
//		}
		private void setToolTip(String toolTip) {
			this.setToolTipText(toolTip);
			toolbox.getContext().getWorkbench().getFrame().warnUser(toolTip);
		}
    }
    
    class CustomJMenuItem extends JMenuItem {
    	private JMenuItem referencedJMenuItem = null;
    	
    	public CustomJMenuItem(JMenuItem jMenuItem) {
    		super(jMenuItem.getText());
    		referencedJMenuItem = jMenuItem;
    	}
    	public JMenuItem getReferencedJMenuItem() {
    		return referencedJMenuItem;
    	}
    }

}
