/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI for
 * visualizing and manipulating spatial features with geometry and attributes.
 * 
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * For more information, contact:
 * 
 * Vivid Solutions Suite #1A 2328 Government Street Victoria BC V8T 5G5 Canada
 * 
 * (250)385-6040 www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.openjump.core.ui.plugin.view.ViewOptionsPlugIn;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.io.datasource.DataSourceQuery;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.CategoryEvent;
import com.vividsolutions.jump.workbench.model.FeatureEvent;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEvent;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.model.LayerListener;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.EditSelectedFeaturePlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

/**
 * Implements an AttributeTable panel. Table-size changes are absorbed by the
 * last column. Rows are striped for non-editable table.
 */

public class AttributeTablePanel extends JPanel {
	
	/**
	 * The property name of the columns width map in the project file (resides in the data-source subtree).
	 */
	public static final String ATTRIBUTE_COLUMNS_WIDTH_MAP = "AttributeColumnsWidthMap";
    public static final String DATE_FORMAT_KEY = ViewOptionsPlugIn.DATE_FORMAT_KEY;

    private static SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");

    private Blackboard blackboard;

    ImageIcon nullObject = IconLoader.icon("null1.png");
    ImageIcon nullString = IconLoader.icon("null1.png");

    public static interface FeatureEditor {

        void edit(PlugInContext context, Feature feature, Layer layer)
                throws Exception;

    }

    private FeatureEditor featureEditor = new FeatureEditor() {

        public void edit(PlugInContext context, Feature feature, final Layer myLayer)
                throws Exception {
            new EditSelectedFeaturePlugIn() {

				@Override
                protected Layer layer(PlugInContext context) {
                    //Hopefully nobody will ever delete or rename the
                    // superclass' #layer method.
                    //[Jon Aquino]
                    return myLayer;
                    //Name "myLayer" because we don't want the
                    //superclass' "layer" [Jon Aquino 2004-03-17]
                }
            }.execute(context, feature, myLayer.isEditable());
        }
    };

    private GridBagLayout gridBagLayout1 = new GridBagLayout();

    private class MyTable extends JTable {

        public MyTable() {
            //We want table-size changes to be absorbed by the last column.
            //By default, AUTO_RESIZE_LAST_COLUMN will not achieve this
            //(it works for column-size changes only). But I am overriding
            //#sizeColumnsToFit (for J2SE 1.3) and
            //JTableHeader#getResizingColumn (for J2SE 1.4)
            //#so that it will work for table-size changes. [Jon Aquino]
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            GUIUtil.doNotRoundDoubles(this);
//            System.out.println("blackboard " + blackboard);
//            System.out.println("workbenchContext " + workbenchContext);
            blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
            DateFormat formatter;
            try {
                formatter = blackboard.get(DATE_FORMAT_KEY) == null ?
                        DEFAULT_DATE_FORMAT :
                        new SimpleDateFormat(blackboard.get(DATE_FORMAT_KEY).toString());
            } catch (IllegalArgumentException e) {
                formatter = DEFAULT_DATE_FORMAT;
            }
            setDefaultEditor(Date.class, new FlexibleDateParser.CellEditor(formatter));
        }

        //Row-stripe colour recommended in
        //Java Look and Feel Design Guidelines: Advanced Topics [Jon Aquino]
        private final Color LIGHT_GRAY = new Color(230, 230, 230);

        private GeometryCellRenderer geomCellRenderer = new GeometryCellRenderer();
        
		@Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (!isEditButtonColumn(column)) {
                JComponent renderer = (JComponent) super.getCellRenderer(row,
                        column);
                // Get the prefered date formatter from the PersistentBlackboard
                Blackboard blackBoard = PersistentBlackboardPlugIn.get(workbenchContext);
                DateFormat _formatter;
                try {
                    _formatter = blackboard.get(DATE_FORMAT_KEY) == null ?
                            DEFAULT_DATE_FORMAT :
                            new SimpleDateFormat(blackboard.get(DATE_FORMAT_KEY).toString());
                } catch (IllegalArgumentException e) {
                    _formatter = DEFAULT_DATE_FORMAT;
                }
                // We need a final formatter to be used in innerClass
                final DateFormat formatter = _formatter;

                setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullObject);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(formatter.format(value));
                    }
                });
                // Set default editor here too, as we want date display and date editing
                // to be synchronized
                setDefaultEditor(Date.class, new FlexibleDateParser.CellEditor(formatter));
                setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });
                setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
                    public void setValue(Object value) {
                        if (value == null) {
                            setIcon(nullString);
                            setHorizontalAlignment(SwingConstants.CENTER);
                        }
                        else setText(value.toString());
                    }
                });

				if (AttributeTablePanel.this.getModel().getLayer().isEditable()
						&& !AttributeTablePanel.this.getModel()
							.isCellEditable(row, column))
					// Shade readonly cells light gray
					renderer.setBackground(LIGHT_GRAY);
				else {
					// If not editable, use row striping, as recommended in
					// Java Look and Feel Design Guidelines: Advanced Topics
					// [Jon Aquino]
					renderer.setBackground((AttributeTablePanel.this.getModel()
							.getLayer().isEditable() || ((row % 2) == 0)) ? Color.white
							: LIGHT_GRAY);
				}
				return (TableCellRenderer) renderer;
            }
            return geomCellRenderer;
        }
    };

    private ImageIcon buildPartlyEmptyIcon(ImageIcon icon) {
      ImageIcon empty = buildEmptyIcon(icon);
      // build mask
      BufferedImage mask = new BufferedImage(icon.getIconWidth(),
          icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = mask.createGraphics();
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, mask.getWidth(), mask.getHeight());
      g.setColor(Color.BLACK);
      g.fillRect(mask.getWidth()/2-1, 0, mask.getWidth(), mask.getHeight());
      // overlay half-red onto normal icon
      icon = GUIUtil.overlay(icon, empty, 0, 0, 1F, mask);
      return icon;
    }
    
    private ImageIcon buildEmptyIcon(ImageIcon icon) {
      ImageIcon gray = GUIUtil.toGrayScale(icon);
      // build red
      BufferedImage red = new BufferedImage(icon.getIconWidth(),
          icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = red.createGraphics();
      g.setColor(Color.PINK);
      g.fillRect(0, 0, red.getWidth(), red.getHeight());
      // build red empty
      ImageIcon empty = GUIUtil.overlay(new ImageIcon(red), gray, 0, 0, 1F, null);
      return empty;
    }
   
    private JButton buildIconButton(ImageIcon icon, Color color) {
      JButton b = new JButton(icon);
      // order matters, set color, then area, then opaque
      if (color != null)
        b.setBackground(color);
      b.setContentAreaFilled(false);
      b.setOpaque(true);
      return b;
    }
   
    private JButton buildIconButton( ImageIcon icon ){
      icon = GUIUtil.pad(icon, 2);
      return buildIconButton(icon, null);
    }
    
   private JButton buildEmptyIconButton( ImageIcon icon ){
     icon = GUIUtil.pad(icon, 2);
     JButton b = buildIconButton(buildEmptyIcon(icon));
     return b;
   }
   
   private JButton buildPartlyEmptyIconButton( ImageIcon icon ){
     icon = GUIUtil.pad(icon, 2);
     JButton b = buildIconButton(buildPartlyEmptyIcon(icon));
     return b;
   }
   
    private boolean isPartlyEmpty(Geometry g) {
      if (g.isEmpty())
        return false;
  
      for (int i = 0; i < g.getNumGeometries(); i++) {
        Geometry inner = g.getGeometryN(i);
        if (inner.isEmpty())
          return true;
      }
      return false;
    }
    
   private class GeometryCellRenderer implements TableCellRenderer 
   {
    private ImageIcon gc = IconLoader.icon("EditGeometryCollection.gif");
    private ImageIcon point = IconLoader.icon("EditPoint.gif");
    private ImageIcon mpoint = IconLoader.icon("EditMultiPoint.gif");
    private ImageIcon line = IconLoader.icon("EditLineString.gif");
    private ImageIcon mline = IconLoader.icon("EditMultiLineString.gif");
    private ImageIcon poly = IconLoader.icon("EditPolygon.gif");
    private ImageIcon mpoly = IconLoader.icon("EditMultiPolygon.gif");
    private ImageIcon lring = IconLoader.icon("EditLinearRing.gif");

    private JButton buttonPoint = buildIconButton(point);
    private JButton buttonMultiPoint = buildIconButton(mpoint);
    private JButton buttonLineString = buildIconButton(line);
    private JButton buttonMultiLineString = buildIconButton(mline);
    private JButton buttonPolygon = buildIconButton(poly);
    private JButton buttonMultiPolygon = buildIconButton(mpoly);
    private JButton buttonGC = buildIconButton(gc);
    private JButton buttonLinearRing = buildIconButton(lring);
    
    private JButton buttonPointEmpty = buildEmptyIconButton(point);
    private JButton buttonMultiPointEmpty = buildEmptyIconButton(mpoint);
    private JButton buttonLineStringEmpty = buildEmptyIconButton(line);
    private JButton buttonMultiLineStringEmpty = buildEmptyIconButton(mline);
    private JButton buttonPolygonEmpty = buildEmptyIconButton(poly);
    private JButton buttonMultiPolygonEmpty = buildEmptyIconButton(mpoly);
    private JButton buttonGCEmpty = buildEmptyIconButton(gc);
    private JButton buttonLinearRingEmpty = buildEmptyIconButton(lring);
    
    private JButton buttonMultiPointPartlyEmpty = buildPartlyEmptyIconButton(mpoint);
    private JButton buttonMultiLineStringPartlyEmpty = buildPartlyEmptyIconButton(mline);
    private JButton buttonMultiPolygonPartlyEmpty = buildPartlyEmptyIconButton(mpoly);
    private JButton buttonGCPartlyEmpty = buildPartlyEmptyIconButton(gc);

    GeometryCellRenderer()
    {
      String text = I18N.get("ui.AttributeTablePanel.feature.view-edit");
      JButton[] buttons = new JButton[] { buttonPoint, buttonMultiPoint,
          buttonLineString, buttonMultiLineString, buttonPolygon,
          buttonMultiPolygon, buttonGC, buttonLinearRing, buttonPointEmpty,
          buttonMultiPointEmpty, buttonLineStringEmpty,
          buttonMultiLineStringEmpty, buttonPolygonEmpty,
          buttonMultiPolygonEmpty, buttonGCEmpty, buttonLinearRingEmpty };
      for (JButton button : buttons) {
        button.setToolTipText(text);
      }
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) 
    {
      Feature f = (Feature) value;
      Geometry g = f.getGeometry();

      if (g instanceof com.vividsolutions.jts.geom.LinearRing)
        return g.isEmpty() ? buttonLinearRingEmpty : buttonLinearRing;
      if (g instanceof com.vividsolutions.jts.geom.Point)
        return g.isEmpty() ? buttonPointEmpty : buttonPoint;
      if (g instanceof com.vividsolutions.jts.geom.MultiPoint)
        return g.isEmpty() ? buttonMultiPointEmpty
            : isPartlyEmpty(g) ? buttonMultiPointPartlyEmpty : buttonMultiPoint;
      if (g instanceof com.vividsolutions.jts.geom.LineString)
        return g.isEmpty() ? buttonLineStringEmpty : buttonLineString;
      if (g instanceof com.vividsolutions.jts.geom.MultiLineString)
        return g.isEmpty() ? buttonMultiLineStringEmpty
            : isPartlyEmpty(g) ? buttonMultiLineStringPartlyEmpty
                : buttonMultiLineString;
      if (g instanceof com.vividsolutions.jts.geom.Polygon)
        return g.isEmpty() ? buttonPolygonEmpty : buttonPolygon;
      if (g instanceof com.vividsolutions.jts.geom.MultiPolygon)
        return g.isEmpty() ? buttonMultiPolygonEmpty
            : isPartlyEmpty(g) ? buttonMultiPolygonPartlyEmpty
                : buttonMultiPolygon;

      return g.isEmpty() ? buttonGCEmpty
          : isPartlyEmpty(g) ? buttonGCPartlyEmpty : buttonGC;
    }
  }

    private MyTable table;

    private TableCellRenderer headerRenderer;

    private LayerNameRenderer layerListCellRenderer;

    private ArrayList listeners = new ArrayList();

    private WorkbenchContext workbenchContext;
    private Layer layer;
    private HashMap columnsWidthMap;

    public AttributeTablePanel(final LayerTableModel model, boolean addScrollPane,
            final WorkbenchContext workbenchContext) {
        this(workbenchContext);
        // this panel is exactly for this layer
        this.layer = model.getLayer();
        if (addScrollPane) {
          remove(table);
          remove(table.getTableHeader());
          JScrollPane scrollPane = new JScrollPane();
          scrollPane.getViewport().add(table);
          this.add(scrollPane, new GridBagConstraints(0, 2, 1, 1, 1, 1,
              GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
                  0, 0), 0, 0));
        }
        updateGrid(model.getLayer());
        model.getLayer().getLayerManager().addLayerListener(
                new LayerListener() {

                    public void categoryChanged(CategoryEvent e) {
                    }

                    public void featuresChanged(FeatureEvent e) {
                    }

                    public void layerChanged(LayerEvent e) {
                        if (e.getLayerable() != model.getLayer()) { return; }
                        if (e.getType() == LayerEventType.METADATA_CHANGED) {
                            //If layer becomes editable, apply row striping
                            // and remove gridlines,
                            //as recommended in Java Look and Feel Design
                            // Guidelines: Advanced Topics [Jon Aquino]
                            updateGrid(model.getLayer());
                            repaint();
                        }
                    }
                });
        try {
            JList list = new JList();
            list.setBackground(new JLabel().getBackground());
            layerListCellRenderer.getListCellRendererComponent(list, model
                    .getLayer(), -1, false, false);
            table.setModel(model);
            model.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                  if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                        //Structure changed (LayerTableModel specifies
                        // HEADER_ROW).
                        //Add this listener after the table adds its listeners
                        //(in table.setModel above) so that this listener will
                        // initialize the column
                        //widths after the table re-adds the columns. [Jon
                        // Aquino]
                        initColumnWidths();
                    }
                }
            });
            layerListCellRenderer.getLabel().setFont(
                    layerListCellRenderer.getLabel().getFont()
                            .deriveFont(Font.BOLD));
            model.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    updateLabel();
                }
            });
            updateLabel();
            //this.workbenchContext = workbenchContext;
            table.setSelectionModel(new SelectionModelWrapper(this));
            table.getTableHeader().setDefaultRenderer(headerRenderer);
            initColumnWidths();
            setToolTips();
            setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0,
                    new FeatureInfoWriter().sidebarColor(model.getLayer())));
            table.getTableHeader().addMouseListener(new MouseAdapter() {

				@Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column < 0) { return; }
                        if (isEditButtonColumn(column)) { return; }
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            model.sort(table.getColumnName(column));
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
            table.addMouseListener(new MouseAdapter() {

				@Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int column = table.columnAtPoint(e.getPoint());
                        int row = table.rowAtPoint(e.getPoint());
                        if (isEditButtonColumn(column)) {
                            PlugInContext context = new PlugInContext(
                                    workbenchContext, null, model.getLayer(),
                                    null, null);
                            model.getLayer().getLayerManager()
                                    .getUndoableEditReceiver().startReceiving();
                            try { 
                                featureEditor.edit(context, model
                                        .getFeature(row), model.getLayer());
                            } finally {
                                model.getLayer().getLayerManager()
                                        .getUndoableEditReceiver()
                                        .stopReceiving();
                            }
                            return;
                        }
                    } catch (Throwable t) {
                        workbenchContext.getErrorHandler().handleThrowable(t);
                    }
                }
            });
            // pressing a key starts the cell editing mode, but it did not 
            // clear the selection and kept delete feature action possible,
            // which was quite dangerous. Now, the selection is cleared
            table.addKeyListener(new java.awt.event.KeyListener(){
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        if (!layer.isEditable()) return;
                        if (e.isControlDown() || e.isAltDown()) return;
                        if (e.getKeyCode() == KeyEvent.VK_DELETE) return;
                        if (e.getKeyCode() == KeyEvent.VK_SHIFT) return; 
                        if (e.getKeyCode() == KeyEvent.VK_CONTROL) return;
                        // if layer is editable and the user pressed a key 
                        // without ctrl or alt pressed. If this key is not
                        // delete or shift or ctrl, the table will enter in 
                        // edition mode. Before that, clear the selection to
                        // avoid the risk to delete the selection while editing
                        table.getSelectionModel().clearSelection();
                    }
                    public void keyReleased(java.awt.event.KeyEvent e) {
                    }
                    public void keyTyped(java.awt.event.KeyEvent e) {
                    }
            });

        } catch (Throwable t) {
            workbenchContext.getErrorHandler().handleThrowable(t);
        }
    }

    private AttributeTablePanel(final WorkbenchContext workbenchContext) {
      layerListCellRenderer = new LayerNameRenderer();
      layerListCellRenderer.setCheckBoxVisible(false);
      layerListCellRenderer.setProgressIconLabelVisible(false);
      this.workbenchContext = workbenchContext;
      blackboard = PersistentBlackboardPlugIn.get(workbenchContext);
      table = new MyTable();
      headerRenderer = new TableCellRenderer() {

          private Icon clearIcon = IconLoader.icon("Clear.gif");

          private Icon downIcon = IconLoader.icon("Down.gif");

          private TableCellRenderer originalRenderer = table.getTableHeader()
                  .getDefaultRenderer();

          private Icon upIcon = IconLoader.icon("Up.gif");

          public Component getTableCellRendererComponent(JTable table,
                                                         Object value, boolean isSelected, boolean hasFocus, int row,
                                                         int column) {
              JLabel label = (JLabel) originalRenderer
                      .getTableCellRendererComponent(table, value, isSelected,
                              hasFocus, row, column);
              if ((getModel().getSortedColumnName() == null)
                      || !getModel().getSortedColumnName().equals(
                      table.getColumnName(column))) {
                  label.setIcon(clearIcon);
              } else if (getModel().isSortAscending()) {
                  label.setIcon(upIcon);
              } else {
                  label.setIcon(downIcon);
              }
              label.setHorizontalTextPosition(SwingConstants.LEFT);
              return label;
          }
      };
      try {
        jbInit();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private void updateGrid(Layer layer) {
        table.setShowGrid(layer.isEditable());
    }

    private boolean isEditButtonColumn(int column) {
        return getModel().getColumnName(0).equals(table.getColumnName(column));
    }

    private void updateLabel() {//[sstein] change for translation
    	if (getModel().getRowCount() == 1) {
    		 layerListCellRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                            + getModel().getRowCount() + " "
							+ I18N.get("ui.AttributeTablePanel.feature") + ")");
    	} else {
    		 layerListCellRenderer.getLabel().setText(
                    getModel().getLayer().getName() + " ("
                            + getModel().getRowCount() + " "
                            + I18N.get("ui.AttributeTablePanel.features") + ")");
    	}
    }

    public LayerTableModel getModel() {
        return (LayerTableModel) table.getModel();
    }

    public JTable getTable() {
        return table;
    }

    public void addListener(AttributeTablePanelListener listener) {
        listeners.add(listener);
    }

    void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        this.add(layerListCellRenderer, new GridBagConstraints(0, 0, 2, 1, 1.0,
                0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        this.add(table.getTableHeader(), new GridBagConstraints(0, 1, 1, 1, 0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
         this.add(table, new GridBagConstraints(0, 2, 1, 1, 0, 0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
                        0, 0, 0, 0), 0, 0)); 
    }

    private void initColumnWidths() {
      GUIUtil.chooseGoodColumnWidths(table);
      int editButtonWidth = 16;
      table.getColumnModel().getColumn(0).setMinWidth(editButtonWidth);
      table.getColumnModel().getColumn(0).setMaxWidth(editButtonWidth);
      table.getColumnModel().getColumn(0).setPreferredWidth(editButtonWidth);
  
      // reset to possibly saved column widths
      if (layer.getDataSourceQuery() instanceof DataSourceQuery) {
        HashMap savedWidthMap = (HashMap) layer.getDataSourceQuery()
            .getDataSource().getProperties().get(ATTRIBUTE_COLUMNS_WIDTH_MAP);
        if (savedWidthMap instanceof HashMap)
          columnsWidthMap = savedWidthMap;
      }
      changeColumnWidths(table.getColumnModel(), false);
  
      // add the Listener for changes
      table.getColumnModel().addColumnModelListener(
          new TableColumnModelListener() {
            final MyTable mytable = table;
  
            public void columnAdded(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnRemoved(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnMoved(TableColumnModelEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), false);
            }
  
            public void columnMarginChanged(ChangeEvent e) {
              changeColumnWidths((TableColumnModel) e.getSource(), true);
            }
  
            public void columnSelectionChanged(ListSelectionEvent e) {
              // do nothing
            }
          });
    }
	
    /**
     * This method handle the changes on the TableColumnModel. The width of all
     * columns will be stored in the datasource properties. Later this can be
     * saved within the projectfile.
     * 
     * @param columnModel
     * @param override
     *          ignore datasource defaults, e.g. if user manually resizes col in
     *          gui
     */
    private void changeColumnWidths(TableColumnModel columnModel, boolean override) {
  
      // init col widths memory map
      if (!(columnsWidthMap instanceof HashMap))
        columnsWidthMap = new HashMap(columnModel.getColumnCount());
      // loop over table cols and restore if entry found
      for (int i = 0; i < columnModel.getColumnCount(); i++) {
        Integer savedWidth = (Integer) columnsWidthMap.get(columnModel.getColumn(
            i).getHeaderValue());
        Integer curWidth = columnModel.getColumn(i).getWidth();
        // get or add new entry, override signal user resizes
        if (savedWidth != null && !override)
          columnModel.getColumn(i).setPreferredWidth(savedWidth);
        else
          columnsWidthMap
              .put(columnModel.getColumn(i).getHeaderValue(), curWidth);
      }
  
      // and finaly save the map to the projects properties
      if (layer.getDataSourceQuery() == null)
        return;
      layer.getDataSourceQuery().getDataSource().getProperties()
          .put(ATTRIBUTE_COLUMNS_WIDTH_MAP, columnsWidthMap);
    }

    private void setToolTips() {
        table.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == -1) { return; }
                table.setToolTipText(table.getColumnName(column) + " ["
                        + getModel().getLayer().getName() + "]");
            }
        });
    }

    /**
     * Called when the user creates a new selection, rather than adding to the
     * existing selection
     */
    private void fireSelectionReplaced() {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            AttributeTablePanelListener listener = (AttributeTablePanelListener) i
                    .next();
            listener.selectionReplaced(this);
        }
    }

    private static class SelectionModelWrapper implements ListSelectionModel {

        private AttributeTablePanel panel;

        private ListSelectionModel selectionModel;

        public SelectionModelWrapper(AttributeTablePanel panel) {
            this.panel = panel;
            selectionModel = panel.table.getSelectionModel();
        }

        public void setAnchorSelectionIndex(int index) {
            selectionModel.setAnchorSelectionIndex(index);
        }

        public void setLeadSelectionIndex(int index) {
            selectionModel.setLeadSelectionIndex(index);
        }

        public void setSelectionInterval(int index0, int index1) {
            selectionModel.setSelectionInterval(index0, index1);
            panel.fireSelectionReplaced();
        }

        public void setSelectionMode(int selectionMode) {
            selectionModel.setSelectionMode(selectionMode);
        }

        public void setValueIsAdjusting(boolean valueIsAdjusting) {
            selectionModel.setValueIsAdjusting(valueIsAdjusting);
        }

        public int getAnchorSelectionIndex() {
            return selectionModel.getAnchorSelectionIndex();
        }

        public int getLeadSelectionIndex() {
            return selectionModel.getLeadSelectionIndex();
        }

        public int getMaxSelectionIndex() {
            return selectionModel.getMaxSelectionIndex();
        }

        public int getMinSelectionIndex() {
            return selectionModel.getMinSelectionIndex();
        }

        public int getSelectionMode() {
            return selectionModel.getSelectionMode();
        }

        public boolean getValueIsAdjusting() {
            return selectionModel.getValueIsAdjusting();
        }

        public boolean isSelectedIndex(int index) {
            return selectionModel.isSelectedIndex(index);
        }

        public boolean isSelectionEmpty() {
            return selectionModel.isSelectionEmpty();
        }

        public void addListSelectionListener(ListSelectionListener x) {
            selectionModel.addListSelectionListener(x);
        }

        public void addSelectionInterval(int index0, int index1) {
            selectionModel.addSelectionInterval(index0, index1);
        }

        public void clearSelection() {
            selectionModel.clearSelection();
        }

        public void insertIndexInterval(int index, int length, boolean before) {
            selectionModel.insertIndexInterval(index, length, before);
        }

        public void removeIndexInterval(int index0, int index1) {
            selectionModel.removeIndexInterval(index0, index1);
        }

        public void removeListSelectionListener(ListSelectionListener x) {
            selectionModel.removeListSelectionListener(x);
        }

        public void removeSelectionInterval(int index0, int index1) {
            selectionModel.removeSelectionInterval(index0, index1);
        }
    }

    public Collection getSelectedFeatures() {
        ArrayList selectedFeatures = new ArrayList();
        if (getModel().getRowCount() == 0) {
        	return selectedFeatures;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            selectedFeatures.add(getModel().getFeature(selectedRows[i]));
        }
        return selectedFeatures;
    }

    public LayerNameRenderer getLayerNameRenderer() {
        return layerListCellRenderer;
    }
    public void setFeatureEditor(FeatureEditor featureEditor) {
        this.featureEditor = featureEditor;
    }
}