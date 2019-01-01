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

package com.vividsolutions.jump.workbench.ui.addremove;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.InputChangedFirer;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * A JPanel that allows the user to move Object's back and forth between
 * two JList's.
 */
public class AddRemovePanel extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JPanel jPanel1 = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JButton removeButton = new JButton();
    JButton removeAllButton = new JButton();
    JButton addButton = new JButton();
    JButton moveUpButton = new JButton();
    JButton moveDownButton = new JButton();
    JButton addAllButton = new JButton();
    Border border1;
    Border border2;
    private JComponent rightLabel = new JLabel();
    private JComponent leftLabel = new JLabel();
    JScrollPane rightScrollPane = new JScrollPane();
    JScrollPane leftScrollPane = new JScrollPane();
    private AddRemoveList leftList = new DefaultAddRemoveList();
    private AddRemoveList rightList = new DefaultAddRemoveList();
    private InputChangedFirer inputChangedFirer = new InputChangedFirer();

    public AddRemovePanel(boolean showingUpDownButtons) {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!showingUpDownButtons) {
            jPanel1.remove(moveUpButton);
            jPanel1.remove(moveDownButton);
        }

        setLeftList(leftList);
        setRightList(rightList);
    }

    //The spacing between the buttons is according to the Java Look and Feel
    //Design Guidelines (http://java.sun.com/products/jlf/at/book/Idioms6.html#57112)
    //[Jon Aquino]
    private void jbInit() throws Exception {
        border1 = new EtchedBorder(EtchedBorder.RAISED, new Color(0, 0, 51), new Color(0, 0, 25));
        border2 = new EtchedBorder(EtchedBorder.RAISED, new Color(0, 0, 51), new Color(0, 0, 25));
        this.setLayout(gridBagLayout1);
        jPanel1.setLayout(gridBagLayout2);
        removeButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.remove"));
        removeAllButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.remove-all"));
        removeButton.setMargin(new Insets(0, 0, 0, 0));
        removeAllButton.setMargin(new Insets(0, 0, 0, 0));
        removeButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRBack.gif")));
        removeAllButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRRewind.gif")));
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        removeAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeAllButton_actionPerformed(e);
            }
        });
        addButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.add"));
        moveUpButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.move-up"));
        moveDownButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.move-down"));
        addAllButton.setToolTipText(I18N.get("ui.addremove.AddRemovePanel.add-all"));
        addButton.setMargin(new Insets(0, 0, 0, 0));
        moveUpButton.setMargin(new Insets(0, 0, 0, 0));
        moveDownButton.setMargin(new Insets(0, 0, 0, 0));
        addAllButton.setMargin(new Insets(0, 0, 0, 0));
        addButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRForward.gif")));
        moveUpButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRUp.gif")));
        moveDownButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRDown.gif")));
        addAllButton.setIcon(GUIUtil.toSmallIcon(IconLoader.icon("VCRFastForward.gif")));
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        moveUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveUpButton_actionPerformed(e);
            }
        });
        moveDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveDownButton_actionPerformed(e);
            }
        });

        addAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addAllButton_actionPerformed(e);
            }
        });
        jPanel1.setMaximumSize(new Dimension(31, 2147483647));
        this.add(
            rightScrollPane,
            new GridBagConstraints(
                34,
                12,
                1,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(4, 4, 4, 4),
                0,
                0));
        rightScrollPane.getViewport().add((JComponent) leftList, null);
        this.add(
            jPanel1,
            new GridBagConstraints(
                23,
                10,
                1,
                5,
                0.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.VERTICAL,
                new Insets(0, 4, 0, 4),
                0,
                0));
        jPanel1.add(
            removeAllButton,
            new GridBagConstraints(
                0,
                3,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 17, 0),
                0,
                0));
        jPanel1.add(
            removeButton,
            new GridBagConstraints(
                0,
                2,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 5, 0),
                0,
                0));
        jPanel1.add(
            addButton,
            new GridBagConstraints(
                0,
                0,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 5, 0),
                0,
                0));
        jPanel1.add(
            moveUpButton,
            new GridBagConstraints(
                0,
                4,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 5, 0),
                0,
                0));
        jPanel1.add(
            moveDownButton,
            new GridBagConstraints(
                0,
                5,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 11, 0),
                0,
                0));

        jPanel1.add(
            addAllButton,
            new GridBagConstraints(
                0,
                1,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(0, 0, 11, 0),
                0,
                0));
        this.add(
            leftScrollPane,
            new GridBagConstraints(
                12,
                12,
                2,
                1,
                1.0,
                1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(4, 4, 4, 4),
                0,
                0));
        leftScrollPane.getViewport().add((JComponent) leftList, null);
        rightScrollPane.getViewport().add((JComponent) rightList, null);
        setRightLabel(rightLabel);
        setLeftLabel(leftLabel);
    }

    public void add(InputChangedListener listener) {
        inputChangedFirer.add(listener);
    }

    /**
     * Updates the enabled state of the Component's.
     */
    public void updateEnabled() {
        addButton.setEnabled(!leftList.getSelectedItems().isEmpty());
        addAllButton.setEnabled(!leftList.getModel().getItems().isEmpty());
        removeButton.setEnabled(!rightList.getSelectedItems().isEmpty());
        removeAllButton.setEnabled(!rightList.getModel().getItems().isEmpty());
        moveUpButton.setEnabled(!itemsToMoveUp().isEmpty());
        moveDownButton.setEnabled(!itemsToMoveDown().isEmpty());
        inputChangedFirer.fire();
    }

    void addAllButton_actionPerformed(ActionEvent e) {
        for (Iterator i = leftList.getModel().getItems().iterator(); i.hasNext();) {
            Object item = i.next();
            rightList.getModel().add(item);
            leftList.getModel().remove(item);
        }

        updateEnabled();
    }

    void removeAllButton_actionPerformed(ActionEvent e) {
        for (Iterator i = rightList.getModel().getItems().iterator(); i.hasNext();) {
            Object item = i.next();
            rightList.getModel().remove(item);
            leftList.getModel().add(item);
        }

        updateEnabled();
    }

    void addButton_actionPerformed(ActionEvent e) {
        addSelected();
    }
    private void addSelected() {
        for (Iterator i = leftList.getSelectedItems().iterator(); i.hasNext();) {
            Object selectedItem = i.next();
            rightList.getModel().add(selectedItem);
            leftList.getModel().remove(selectedItem);
        }

        updateEnabled();
    }

    void moveUpButton_actionPerformed(ActionEvent e) {
        move(itemsToMoveUp(), -1);
    }

    private void move(Collection itemsToMove, int displacement) {
        Collection selectedItems = rightList.getSelectedItems();
        List items = new ArrayList(rightList.getModel().getItems());

        for (Iterator i = itemsToMove.iterator(); i.hasNext();) {
            Object item = i.next();
            int index = items.indexOf(item);
            items.remove(item);
            items.add(index + displacement, item);
        }

        rightList.getModel().setItems(items);
        rightList.setSelectedItems(selectedItems);
        updateEnabled();
    }

    void moveDownButton_actionPerformed(ActionEvent e) {
        move(itemsToMoveDown(), +1);
    }

    private Collection itemsToMoveUp() {
        return itemsToMoveUp(rightList.getModel().getItems(), rightList.getSelectedItems());
    }

    private Collection itemsToMoveDown() {
        return itemsToMoveDown(rightList.getModel().getItems(), rightList.getSelectedItems());
    }

    public static Collection itemsToMoveDown(List items, Collection selectedItems) {
        List reverseItems = new ArrayList(items);
        Collections.reverse(reverseItems);
        return itemsToMoveUp(reverseItems, selectedItems);
    }

    public static Collection itemsToMoveUp(List items, Collection selectedItems) {
        int firstUnselectedIndex = firstUnselectedIndex(items, selectedItems);

        if (firstUnselectedIndex == -1) {
            return new ArrayList();
        }

        ArrayList itemsToMoveUp = new ArrayList();

        for (int i = firstUnselectedIndex; i < items.size(); i++) {
            Object item = (Object) items.get(i);

            if (selectedItems.contains(item)) {
                itemsToMoveUp.add(item);
            }
        }

        return itemsToMoveUp;
    }

    private static int firstUnselectedIndex(List items, Collection selectedItems) {
        for (int i = 0; i < items.size(); i++) {
            Object item = (Object) items.get(i);

            if (!selectedItems.contains(item)) {
                return i;
            }
        }

        return -1;
    }

    void removeButton_actionPerformed(ActionEvent e) {
        removeSelected();
    }
    private void removeSelected() {
        for (Iterator i = rightList.getSelectedItems().iterator(); i.hasNext();) {
            Object selectedItem = i.next();
            rightList.getModel().remove(selectedItem);
            leftList.getModel().add(selectedItem);
        }
        updateEnabled();
    }

    public void setLeftText(String newLeftText) {
        if (leftLabel instanceof JLabel) {
            ((JLabel) leftLabel).setText(newLeftText);
        } else {
            Assert.shouldNeverReachHere();
        }
    }

    public void setRightLabel(JComponent rightLabel) {
        remove(rightLabel);
        this.rightLabel = rightLabel;
        add(
            rightLabel,
            new GridBagConstraints(
                34,
                10,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));
        initLabelSizes();
    }

    public void setLeftLabel(JComponent leftLabel) {
        remove(leftLabel);
        this.leftLabel = leftLabel;
        add(
            leftLabel,
            new GridBagConstraints(
                12,
                10,
                2,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0),
                0,
                0));
        initLabelSizes();
    }

    private void initLabelSizes() {
        //Ensure left and right sides have same width [Jon Aquino]
        Dimension d =
            new Dimension(
                (int) Math.max(
                    leftLabel.getPreferredSize().getWidth(),
                    rightLabel.getPreferredSize().getWidth()),
                (int) Math.max(
                    leftLabel.getPreferredSize().getHeight(),
                    rightLabel.getPreferredSize().getHeight()));
        leftLabel.setPreferredSize(d);
        rightLabel.setPreferredSize(d);
    }

    public void setRightList(AddRemoveList rightList) {
        rightScrollPane.getViewport().remove((JComponent) this.rightList);
        this.rightList = rightList;
        rightScrollPane.getViewport().add((JComponent) rightList, null);
        init(rightList, rightScrollPane);
        rightList.add(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    removeSelected();
                }
            }
        });
    }

    private void init(AddRemoveList list, JScrollPane scrollPane) {
        list.add(new InputChangedListener() {
            public void inputChanged() {
                updateEnabled();
            }
        });

        //A tip from Jason Ross to ensure that the left and right panels start
        //the same size and stay that way when the dialog is resized: simply make
        //sure their preferred sizes are initially the same. [Jon Aquino]
        scrollPane.setPreferredSize(new Dimension(10, 10));
        updateEnabled();
    }

    public void setLeftList(AddRemoveList leftList) {
        leftScrollPane.getViewport().remove((JComponent) this.leftList);
        this.leftList = leftList;
        leftScrollPane.getViewport().add((JComponent) leftList, null);
        init(leftList, leftScrollPane);
        leftList.add(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    addSelected();
                }
            }
        });
    }

    public void setRightText(String newRightText) {
        if (rightLabel instanceof JLabel) {
            ((JLabel) rightLabel).setText(newRightText);
        } else {
            Assert.shouldNeverReachHere();
        }
    }

    public List getLeftItems() {
        return leftList.getModel().getItems();
    }

    public List getRightItems() {
        return rightList.getModel().getItems();
    }

    public AddRemoveList getLeftList() {
        return leftList;
    }

    public AddRemoveList getRightList() {
        return rightList;
    }
}
