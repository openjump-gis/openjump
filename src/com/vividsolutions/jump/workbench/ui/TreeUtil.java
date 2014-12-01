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

import com.vividsolutions.jts.util.Assert;

import com.vividsolutions.jump.util.Block;
import com.vividsolutions.jump.util.StringUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


public class TreeUtil {
    public static TreeCellRenderer createSimpleTreeCellRenderer(ImageIcon icon) {
        return createSimpleTreeCellRenderer(null, icon, new JTree().getFont());
    }

    /**
     * @param text null to get the text by calling #toString on the value
     */
    public static TreeCellRenderer createSimpleTreeCellRenderer(
        final String text, final ImageIcon icon, final Font font) {
        return new DefaultTreeCellRenderer() {

                {
                    setOpenIcon(icon);
                    setClosedIcon(icon);
                    setLeafIcon(icon);

                    //Transparent. [Jon Aquino]
                    setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
                }

                public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {
                    Component component = super.getTreeCellRendererComponent(tree,
                            (text == null) ? value : text, sel, expanded, leaf,
                            row, hasFocus);
                    component.setFont(font);

                    return component;
                }
            };
    }

    public static void visit(TreeModel model, Visitor visitor) {
        Stack path = new Stack();
        path.push(model.getRoot());
        visit(model, path, visitor);
    }

    /**
     * Visit the path and all subpaths.
     */
    public static void visit(TreeModel model, TreePath path, Visitor visitor) {
        Stack stack = new Stack();
        stack.addAll(Arrays.asList(path.getPath()));
        visit(model, stack, visitor);
    }

    private static void visit(TreeModel model, Stack path, Visitor visitor) {
        visitor.visit(path);

        for (int i = 0; i < model.getChildCount(path.peek()); i++) {
            path.push(model.getChild(path.peek(), i));
            visit(model, path, visitor);
            path.pop();
        }
    }

    public static TreeModelEvent createTreeModelEvent(final Object source,
        final Object node, final TreeModel model) {
        TreePath path = findTreePath(node, model);
        Assert.isTrue(path != null,
            "Cannot find node in TreeModel: " + node + "\n" + dump(model));

        TreePath parentPath = path.getParentPath();

        return new TreeModelEvent(source, parentPath,
            new int[] {
                model.getIndexOfChild(parentPath.getLastPathComponent(), node)
            }, new Object[] { node });
    }

    public static String dump(TreeModel model) {
        final StringBuffer result = new StringBuffer();
        visit(model,
            new Visitor() {
                public void visit(Stack path) {
                    result.append((StringUtil.repeat(' ', path.size() * 2) +
                        path.lastElement() + "\n"));
                }
            });

        return result.toString();
    }

    /**
     * @return null if the node is not in the tree model
     */
    public static TreePath findTreePath(final Object node, final TreeModel model) {
        final TreePath[] treePath = new TreePath[] { null };
        visit(model,
            new Visitor() {
                public void visit(Stack path) {
                    if (path.peek() != node) {
                        return;
                    }

                    treePath[0] = new TreePath(path.toArray());
                }
            });

        return treePath[0];
    }

    public static boolean contains(TreeModel model, final Object node) {
        final boolean[] result = new boolean[] { false };
        visit(model,
            new Visitor() {
                public void visit(Stack path) {
                    if (path.peek() == node) {
                        result[0] = true;
                    }
                }
            });

        return result[0];
    }

    public static List lastPathComponents(TreePath[] paths) {
        ArrayList lastPathComponents = new ArrayList();

        for (int i = 0; i < paths.length; i++) {
            lastPathComponents.add(paths[i].getLastPathComponent());
        }

        return lastPathComponents;
    }

    public static void expandAll(final JTree tree, TreePath path) {
        expand(tree, path, new Block() {
            public Object yield(Object node) {
                return Boolean.TRUE;
            }
        });
    }

    public static void expand(final JTree tree, TreePath path, final Block expandNodeCondition) {
        visit(tree.getModel(), path,
            new Visitor() {
                public void visit(Stack path) {
                    if (!((Boolean)expandNodeCondition.yield(path.peek())).booleanValue()) { return; }
                    tree.expandPath(findTreePath(path.peek(), tree.getModel()));
                }
            });
    }

    public static Collection nodes(TreePath path, TreeModel model) {
        final ArrayList nodes = new ArrayList();
        visit(model, path,
            new Visitor() {
                public void visit(Stack path) {
                    nodes.add(path.peek());
                }
            });

        return nodes;
    }

    public static interface Visitor {
        public void visit(Stack path);
    }
}
