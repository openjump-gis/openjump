package com.vividsolutions.jump.workbench.ui.toolbox;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.JTextComponent;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.OrderedMap;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerComboBox;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;


/**
 * Stores the state of the component and its descendants on the Blackboard of
 * the LayerManager (if any) of the current JInternalFrame. Thus, when the
 * user switches JInternalFrames, the component will change its state to reflect
 * the new context.
 * <p>
 * Designed to be trivial to use: simply instantiate one if you want to use one.
 * (Make sure the toolbox's Components are initialized first).
 * Note that not all Components are supported; currently the ToolboxStateManager
 * will save the state for the following components only: JTextComponent,
 * JToggleButton (including JCheckBox), JComboBox.
 */
public class ToolboxStateManager {
    private static int keyIndex = 0;
    private OrderedMap componentClassToStrategyMap = new OrderedMap() {

            {
                put(JTextComponent.class,
                    new Strategy() {
                        protected void addActionListener(
                            ActionListener actionListener, Component component) {
                            ((JTextComponent) component).getDocument()
                             .addDocumentListener(GUIUtil.toDocumentListener(
                                    actionListener));
                        }

                        protected Object getToolboxValue(Component component) {
                            return ((JTextComponent) component).getText();
                        }

                        protected void setToolboxValue(Object value,
                            Component component) {
                            ((JTextComponent) component).setText((String) value);
                        }
                    });
                put(JToggleButton.class,
                    new Strategy() {
                        protected void addActionListener(
                            ActionListener actionListener, Component component) {
                            ((JToggleButton) component).addActionListener(actionListener);
                        }

                        protected Object getToolboxValue(Component component) {
                            return new Boolean(((JToggleButton) component).isSelected());
                        }

                        protected void setToolboxValue(Object value,
                            Component component) {
                            ((JToggleButton) component).setSelected(((Boolean) value).booleanValue());
                        }
                    });
                put(LayerComboBox.class,
                    new Strategy() {
                        protected void addActionListener(
                            ActionListener actionListener, Component component) {
                            ((LayerComboBox) component).getModel()
                             .addListDataListener(GUIUtil.toListDataListener(
                                    actionListener));
                        }

                        protected Object getDefaultValue(Object initialToolboxValue,
                            Component component) {
                            LayerManager layerManager = (LayerManager)LangUtil.ifNull(((WorkbenchFrame) SwingUtilities
                                 .getAncestorOfClass(WorkbenchFrame.class,
                                    component)).getContext().getLayerManager(), new LayerManager());
                            return new Object[] {
                                layerManager.size() > 0 ? layerManager.iterator().next() : null,
                                layerManager
                            };
                        }

                        protected Object getToolboxValue(Component component) {
                            return new Object[] {
                                ((LayerComboBox) component).getSelectedItem(),
                                ((LayerComboBox) component).getLayerManager()
                            };
                        }

                        protected void setToolboxValue(Object value,
                            Component component) {
                            ((LayerComboBox) component).setLayerManager(((LayerManager) ((Object[]) value)[1]));
                            ((LayerComboBox) component).setSelectedItem(((Object[]) value)[0]);
                        }
                    });
                put(JComboBox.class,
                    new Strategy() {
                        protected void addActionListener(
                            ActionListener actionListener, Component component) {
                            ((JComboBox) component).addActionListener(actionListener);
                            ((JComboBox) component).getModel()
                             .addListDataListener(GUIUtil.toListDataListener(
                                    actionListener));
                        }

                        protected Object getToolboxValue(Component component) {
                            Vector items = new Vector();

                            for (int i = 0;
                                    i < ((JComboBox) component).getItemCount();
                                    i++) {
                                items.add(((JComboBox) component).getItemAt(i));
                            }

                            return new Object[] {
                                ((JComboBox) component).getSelectedItem(), items
                            };
                        }

                        protected void setToolboxValue(Object value,
                            Component component) {
                            ((JComboBox) component).setModel(new DefaultComboBoxModel(
                                    ((Vector) ((Object[]) value)[1])));
                            ((JComboBox) component).setSelectedItem(((Object[]) value)[0]);
                        }
                    });
            }
        };

    private WorkbenchContext workbenchContext;
    private Blackboard dummyBlackboard = new Blackboard();

    public ToolboxStateManager(ToolboxDialog toolbox) {
        this(toolbox, new HashMap());
    }

    public ToolboxStateManager(ToolboxDialog toolbox, Map customComponentClassToStrategyMap) {
        this.workbenchContext = toolbox.getContext();
        componentClassToStrategyMap.putAll(customComponentClassToStrategyMap);            
        monitor(toolbox);                
    }

    private Blackboard getBlackboard() {
        return (workbenchContext.getLayerManager() == null) ? dummyBlackboard
                                                            : workbenchContext.getLayerManager()
                                                                              .getBlackboard();
    }

    private ToolboxStateManager monitor(Component component) {
        //Skip CursorTools! [Jon Aquino 1/12/2004]
        if (null != SwingUtilities.getAncestorOfClass(WorkbenchToolBar.class, component)) {
            return this;
        }
        for (Iterator i = componentClassToStrategyMap.keyList().iterator();
                i.hasNext();) {
            Class componentClass = (Class) i.next();

            if (componentClass.isInstance(component)) {
                ((Strategy) componentClassToStrategyMap.get(componentClass)).monitor(component, this);
                //Don't go any deeper [Jon Aquino]
                return this;
            }
        }

        if (component instanceof Container) {
            for (int i = 0; i < ((Container) component).getComponentCount();
                    i++) {
                monitor(((Container) component).getComponent(i));
            }
        }

        return this;
    }

    public static abstract class Strategy {
        private boolean updating = false;

        public void monitor(final Component component, final ToolboxStateManager manager) {
            final String key = ToolboxStateManager.class.getName() + " - " +
                ++keyIndex;
            final Object initialValue = getToolboxValue(component);
            final Block updateBlackboardBlock = new Block() {
                public Object yield() {
                    updating = true;

                    try {
                        setToolboxValue(manager.getBlackboard().get(key,
                                getDefaultValue(initialValue, component)),
                            component);
                    } finally {
                        updating = false;
                    }                
                    return null;    
                }
            };        
            ActionListener actionListener = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (updating) {
                            return;
                        }

                        manager.getBlackboard().put(key, getToolboxValue(component));
                    }
                };
            updateBlackboardBlock.yield();
            actionListener.actionPerformed(null);
            addActionListener(actionListener, component);
            GUIUtil.addInternalFrameListener(manager.workbenchContext.getWorkbench()
                                                                     .getFrame()
                                                                     .getDesktopPane(),
                new InternalFrameAdapter() {
                    public void internalFrameActivated(InternalFrameEvent e) {
                        updateBlackboardBlock.yield();
                    }

                    public void internalFrameDeactivated(InternalFrameEvent e) {
                        //Handles case: one task frame, and it gets closed. [Jon Aquino]
                        updateBlackboardBlock.yield();
                    }

                });
        }

        protected Object getDefaultValue(Object initialToolboxValue,
            Component component) {
            return initialToolboxValue;
        }

        protected abstract void addActionListener(
            ActionListener actionListener, Component component);

        protected abstract Object getToolboxValue(Component component);

        protected abstract void setToolboxValue(Object value, Component component);
    }
}
