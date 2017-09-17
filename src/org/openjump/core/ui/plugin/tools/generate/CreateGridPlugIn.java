
package org.openjump.core.ui.plugin.tools.generate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.task.TaskMonitorUtil;
import com.vividsolutions.jump.task.TaskMonitorV2Util;
import com.vividsolutions.jump.util.Timer;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * Create a grid or graticule for the map.
 * @author micha&euml;l michaud
 */
public class CreateGridPlugIn extends AbstractUiPlugIn implements ThreadedPlugIn{

    private final static String EXTENT            = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.extent");
    private final static String ALL_LAYERS        = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.all-layers");
    private final static String SELECTED_LAYERS   = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.selected-layers");
    private final static String SELECTED_FEATURES = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.selected-features");
    private final static String VIEW              = I18N.get("ui.MenuNames.VIEW");
    private final static String SYNCHRONIZE       = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.synchronize");
    private final static String DX                = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.dx");
    private final static String DY                = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.dy");
    private final static String CREATE_POINTS     = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.points");
    private final static String CREATE_LINES      = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.lines");
    private final static String CREATE_POLYS      = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.polygons");
    private final static String POINT_GRID        = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.point-grid");
    private final static String LINE_GRID         = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.line-grid");
    private final static String POLY_GRID         = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.poly_grid");
    private final static String NULL_EXTENT       = I18N.get("org.openjump.core.ui.plugin.tools.generate.CreateGridPlugIn.null-extent");
    
    //boolean use_selection = false;
    String extent = ALL_LAYERS;
    boolean synchronize = true;
    double dx = 1000;
    double dy = 1000;
    boolean createPoints = false;
    boolean createLines  = true;
    boolean createPolys  = false;
    
    TaskMonitor taskMonitor = null;

    public CreateGridPlugIn() {
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuItem(
            new String[] { MenuNames.TOOLS, MenuNames.TOOLS_GENERATE},
            this,
            new JMenuItem(getName()+"...", IconLoader.icon("create_grid.gif")),
            createEnableCheck(context.getWorkbenchContext()), -1);
    }

    public MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createTaskWindowMustBeActiveCheck())
            .add(checkFactory.createAtLeastNLayersMustExistCheck(1));
    }

    public Icon getIcon() {
        return IconLoader.icon("create_grid.gif");
    }
    
    @Override
    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(), getName(), true);
        initDialog(dialog, context);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) {
            return false;
        }
        getDialogValues(dialog);

        return true;
    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
      this.taskMonitor = monitor;
      TaskMonitorV2Util.setTitle(monitor, getName());
      monitor.allowCancellationRequests();
      createGrid(context);
    }

    private void initDialog(final MultiInputDialog dialog, PlugInContext context) {
        final JComboBox extentComboBox = dialog.addComboBox(
            EXTENT,
            extent,
            Arrays.asList(ALL_LAYERS, SELECTED_LAYERS, SELECTED_FEATURES, VIEW),
            null);
        final JCheckBox synchronizeCheckBox = dialog.addCheckBox(SYNCHRONIZE, synchronize);
        final JTextField dxField = dialog.addDoubleField(DX, dx, 12);
        final JTextField dyField = dialog.addDoubleField(DY, dy, 12);
        final JCheckBox outputPointsCheckBox = dialog.addCheckBox(CREATE_POINTS, createPoints);
        final JCheckBox outputLinesCheckBox  = dialog.addCheckBox(CREATE_LINES, createLines);
        final JCheckBox outputPolysCheckBox  = dialog.addCheckBox(CREATE_POLYS, createPolys);
        
        final DocumentListener dxListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
            }
            public void insertUpdate(DocumentEvent e) {
                dyField.setText(dxField.getText());
            }
            public void removeUpdate(DocumentEvent e) {
                dyField.setText(dxField.getText());
            }
        };
        dxField.getDocument().addDocumentListener(dxListener);
        dyField.setEditable(false);

        synchronizeCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (synchronizeCheckBox.isSelected()) {
                    dxField.getDocument().addDocumentListener(dxListener);
                    dyField.setText(dxField.getText());
                    dyField.setEditable(false);
                }
                else {
                    dxField.getDocument().removeDocumentListener(dxListener);
                    dyField.setEditable(true);
                }
            }
        });

        GUIUtil.centreOnWindow(dialog);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        extent       = dialog.getText(EXTENT);
        dx           = dialog.getDouble(DX);
        dy           = dialog.getDouble(DY);
        createPoints = dialog.getBoolean(CREATE_POINTS);
        createLines  = dialog.getBoolean(CREATE_LINES);
        createPolys  = dialog.getBoolean(CREATE_POLYS);
    }

    public void createGrid(PlugInContext context) {
      Envelope env = getEnvelope(context);
      if (env.isNull()) {
        context.getWorkbenchFrame().warnUser(NULL_EXTENT);
        return;
      }

      double xm = Math.floor(env.getMinX() / dx) * dx;
      double ym = Math.floor(env.getMinY() / dy) * dy;
      double xM = Math.ceil(env.getMaxX() / dx) * dx;
      double yM = Math.ceil(env.getMaxY() / dy) * dy;
      if (createPoints) {
        FeatureCollection fc = createPoints(context, xm, ym, xM, yM);
        if (!isCancelled())
          context.getLayerManager().addLayer(StandardCategoryNames.WORKING, POINT_GRID, fc);
      }
      if (createLines) {
        FeatureCollection fc = createLines(context, xm, ym, xM, yM);
        if (!isCancelled())
          context.getLayerManager().addLayer(StandardCategoryNames.WORKING, LINE_GRID, fc);
      }
      if (createPolys) {
        FeatureCollection fc = createPolys(context, xm, ym, xM, yM);
        if (!isCancelled())
          context.getLayerManager().addLayer(StandardCategoryNames.WORKING, POLY_GRID, fc);
      }
    }

    private Envelope getEnvelope(PlugInContext context) {
        Envelope env = new Envelope();
        if (extent.equals(ALL_LAYERS)) {
            env = context.getLayerManager().getEnvelopeOfAllLayers(true);
        }
        else if (extent.equals(SELECTED_LAYERS)) {
            Layer[] layers = context.getLayerNamePanel().getSelectedLayers();
            for (Layer layer : layers) env.expandToInclude(layer.getFeatureCollectionWrapper().getEnvelope());
            return env;
        }
        else if (extent.equals(SELECTED_FEATURES)) {
            Collection features = context.getLayerViewPanel().getSelectionManager().getFeaturesWithSelectedItems();
            for (Object o : features) {
                Feature feature = (Feature)o;
                env.expandToInclude(feature.getGeometry().getEnvelopeInternal());
            }
        }
            else if (extent.equals(VIEW)) {
            	env =context.getWorkbenchContext().getLayerViewPanel().getViewport().getEnvelopeInModelCoordinates();
                
        } else {
            Assert.shouldNeverReachHere();
        }
        return env;
    }

    private FeatureCollection createPoints(PlugInContext context, double xm, double ym, double xM, double yM) {
        TaskMonitorV2Util.report(taskMonitor,getName());
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute("NUM", AttributeType.STRING);
        FeatureCollection ds = new FeatureDataset(fs);
        GeometryFactory gf = new GeometryFactory();
        long numPoints = (long)( ((xM-xm+dx)/dx) * ((yM-ym+dy)/dy) );
        long curPoint = 0;
        for (double x = xm ; x <= xM && !isCancelled(); x+=dx) {
            for (double y = yM ; y >= ym && !isCancelled(); y-=dy) {
                Geometry point = gf.createPoint(new Coordinate(x, y));
                Feature f = new BasicFeature(fs);
                f.setGeometry(point);
                f.setAttribute("NUM", ""+x+"-"+y);
                ds.add(f);
                reportItems(++curPoint, numPoints, CREATE_POINTS);
            }
        }
        return ds;
    }

    private FeatureCollection createLines(PlugInContext context, double xm, double ym, double xM, double yM) {
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute("NUM", AttributeType.STRING);
        final long numLines = (long)((xM-xm)/dx + (yM-ym)/dy);
        FeatureCollection ds = new FeatureDataset(fs){
          @Override
          public void add(Feature feature) {
            super.add(feature);
            reportItems(getFeatures().size(), numLines, CREATE_LINES);
          }
        };
        GeometryFactory gf = new GeometryFactory();
        for (double x = xm ; x <= xM && !isCancelled(); x+=dx) {
            Geometry line = gf.createLineString(new Coordinate[]{
                    new Coordinate(x, yM), new Coordinate(x, ym)});
            Feature f = new BasicFeature(fs);
            f.setGeometry(line);
            f.setAttribute("NUM", ""+x);
            ds.add(f);
        }
        for (double y = yM ; y >= ym && !isCancelled(); y-=dy) {
            Geometry line = gf.createLineString(new Coordinate[]{
                    new Coordinate(xm, y), new Coordinate(xM, y)});
            Feature f = new BasicFeature(fs);
            f.setGeometry(line);
            f.setAttribute("NUM", ""+y);
            ds.add(f);
        }
        return ds;
    }

    private FeatureCollection createPolys(PlugInContext context, double xm, double ym, double xM, double yM) {
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute("NUM", AttributeType.STRING);
        final long numPolys = (long)(((xM-xm)/dx) * ((yM-ym)/dx));
        FeatureCollection ds = new FeatureDataset(fs){
          @Override
          public void add(Feature feature) {
            super.add(feature);
            reportItems(getFeatures().size(), numPolys, CREATE_POLYS);
          }
        };
        GeometryFactory gf = new GeometryFactory();
        for (double x = xm ; x <= xM-dx && !isCancelled(); x+=dx) {
            for (double y = yM ; y >= ym+dy && !isCancelled(); y-=dy) {
                Geometry poly = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                    new Coordinate(x, y),
                    new Coordinate(x+dx, y),
                    new Coordinate(x+dx, y-dy),
                    new Coordinate(x, y-dy),
                    new Coordinate(x, y),
                }), new LinearRing[0]);
                Feature f = new BasicFeature(fs);
                f.setGeometry(poly);
                f.setAttribute("NUM", ""+x+"-"+y);
                ds.add(f);
            }
        }
        return ds;
    }

    private long lastMsg = 0;
    private long interval = 300;
    // report only every <interval> millisecs
    private void reportItems(long itemsDone, long itemsTotal, String message ){
      long now = Timer.milliSecondsSince(0);
      // show status every .5s
      if (now - interval >= lastMsg) {
        lastMsg = now;
        TaskMonitorV2Util.report(this.taskMonitor, itemsDone, itemsTotal, message);
      }
    }

    private boolean isCancelled(){
      return taskMonitor.isCancelRequested();
    }
}
