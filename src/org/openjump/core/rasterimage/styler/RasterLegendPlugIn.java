package org.openjump.core.rasterimage.styler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;
import org.openjump.core.apitools.LayerTools;
import org.openjump.core.rasterimage.RasterImageLayer;
import org.openjump.core.rasterimage.RasterSymbology;
import org.openjump.core.rasterimage.styler.ui.ColorsLabelLegendComponent;
import org.openjump.core.rasterimage.styler.ui.GradientLabelLegendComponent;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.plugin.layer.pirolraster.RasterImageContextMenu;
import org.openjump.core.ui.swing.DetachableInternalFrame;

import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.sun.media.jai.codecimpl.TIFFCodec;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedPlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Plugin for displaying the raster (ASC, FLT formats) legend. The menu is
 * actived only if a raster layer is selected.
 * 
 * [Giuseppe Aruta 28/08/2018] Reactivated with option to save legend to image
 * TODO Internazionalize
 * 
 * @author GeomaticaEAmbiente
 */
public class RasterLegendPlugIn implements ThreadedPlugIn {

    private final String sSaved = I18N
            .get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
    private final String sShowLegend = I18N
            .get("org.openjump.core.ui.plugin.style.LegendPlugIn");
    private final String SCouldNotSave = I18N
            .get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");
    private final String SAVE = I18N
            .get("deejump.plugin.SaveLegendPlugIn.Save");
    private final String CLOSE = I18N
            .get("ui.plugin.imagery.ImageLayerManagerDialog.Close");

    @Override
    public void initialize(PlugInContext context) throws Exception {

        /* Add item to pop-up menu, only for rasters */
        final JPopupMenu menu = RasterImageContextMenu.getInstance(context);
        context.getFeatureInstaller().addPopupMenuPlugin(menu, this, getName(),
                false, getIcon(),
                createEnableCheck(context.getWorkbenchContext()));

    }

    JScrollPane scrollPane = new JScrollPane();
    WorkbenchContext wcontext = JUMPWorkbench.getInstance().getContext();

    public static RasterImageLayer rasterImageLayer = new RasterImageLayer();

    public static RasterImageLayer getLayer() {
        return rasterImageLayer;

    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {

        rasterImageLayer = (RasterImageLayer) LayerTools.getSelectedLayerable(
                context, RasterImageLayer.class);

        final RasterSymbology rasterStyler = rasterImageLayer.getSymbology();

        if (rasterStyler.getColorMapEntries_tm().size() > 40) {
            JOptionPane.showMessageDialog(context.getWorkbenchFrame(),
                    // bundle.getString("LegendDialog.More40Colors.message"),
                    "More than 40 colors", RasterStylesExtension.extensionName,
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        final TreeMap<Double, Color> colorMapEntries = rasterStyler
                .getColorMapEntries_tm();

        final String type = RasterLegendPlugIn.getLayer().getSymbology()
                .getColorMapType();

        if (type.equals(RasterSymbology.TYPE_INTERVALS)
                || type.equals(RasterSymbology.TYPE_SINGLE)) {
            scrollPane = new JScrollPane(getPanelInterval(colorMapEntries,
                    rasterImageLayer),
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        } else if (type.equals(RasterSymbology.TYPE_RAMP)) {
            scrollPane = new JScrollPane(getPanelGradient(colorMapEntries,
                    rasterImageLayer),
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        }

        scrollPane.setPreferredSize(new Dimension(300, 400));
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(scrollPane.getPreferredSize());
        final DetachableInternalFrame frame = new DetachableInternalFrame();
        frame.setTitle(getName() + " (" + rasterImageLayer.getName() + ")");
        frame.setResizable(true);
        frame.setClosable(true);
        frame.setIconifiable(true);
        frame.setMaximizable(true); // frame.setFrameIcon(ICON);
        frame.setSize(300, 500);
        frame.setLayer(JLayeredPane.PALETTE_LAYER);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(getOKSavePanel(frame), BorderLayout.SOUTH);
        panel.updateUI();
        frame.setContentPane(panel);

        /*     frame.add(panel, BorderLayout.NORTH);

             final JPanel okPanel = new JPanel();
             final JButton saveButton = new JButton(SAVE) {

                 private static final long serialVersionUID = 1L;

                 @Override
                 public Dimension getPreferredSize() {
                     return new Dimension(100, 25);
                 }
             };

             final JButton closeButton = new JButton(CLOSE) {
                 private static final long serialVersionUID = 2L;

                 @Override
                 public Dimension getPreferredSize() {
                     return new Dimension(100, 25);
                 }
             };

             saveButton.addActionListener(new java.awt.event.ActionListener() {
                 @Override
                 public void actionPerformed(ActionEvent e) {
                     save(scrollPane, rasterImageLayer);
                     // frame.dispose();
                     return;
                 }
             });

             closeButton.addActionListener(new java.awt.event.ActionListener() {

                 @Override
                 public void actionPerformed(ActionEvent e) {

                     frame.dispose();

                     return;
                 }
             });

             okPanel.add(saveButton, BorderLayout.WEST);
             okPanel.add(closeButton, BorderLayout.EAST);

             frame.add(okPanel, BorderLayout.SOUTH);*/
        frame.pack();
        context.getWorkbenchFrame().addInternalFrame(frame, true, true);

        return true;

    }

    protected JPanel getOKSavePanel(final DetachableInternalFrame frame) {
        final JPanel okPanel = new JPanel();
        final JButton saveButton = new JButton(SAVE) {

            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };
        final JButton closeButton = new JButton(CLOSE) {
            private static final long serialVersionUID = 2L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 25);
            }
        };

        saveButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save(scrollPane);
                // frame.dispose();
                return;
            }
        });
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                frame.dispose();

                return;
            }
        });
        okPanel.add(saveButton, BorderLayout.WEST);
        okPanel.add(closeButton, BorderLayout.EAST);
        return okPanel;

    }

    @Override
    public void run(TaskMonitor monitor, PlugInContext context)
            throws Exception {
    }

    private JPanel getPanelInterval(TreeMap<Double, Color> colorMapEntry_tm,
            RasterImageLayer rLayer) throws Exception {

        final ColorsLabelLegendComponent component = new ColorsLabelLegendComponent(
                colorMapEntry_tm, rLayer.getNoDataValue(), rLayer.getName());
        component.setPreferredSize(new Dimension(200, 400));

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(component);
        panel.setVisible(true);

        return panel;

    }

    private JPanel getPanelGradient(TreeMap<Double, Color> colorMapEntry,
            RasterImageLayer rLayer) throws Exception {

        final GradientLabelLegendComponent component = new GradientLabelLegendComponent(
                colorMapEntry, rLayer.getNoDataValue(), rLayer.getName());
        component.setPreferredSize(new Dimension(200, 400));
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(component);
        panel.setVisible(true);

        return panel;

    }

    @Override
    public String getName() {
        return sShowLegend;
    }

    public ImageIcon getIcon() {
        return IconLoader.icon("saig/addLegend.gif");
    }

    public static MultiEnableCheck createEnableCheck(
            final WorkbenchContext workbenchContext) {

        final MultiEnableCheck multiEnableCheck = new MultiEnableCheck();
        final EnableCheckFactory checkFactory = new EnableCheckFactory(
                workbenchContext);
        multiEnableCheck.add(checkFactory
                .createWindowWithLayerNamePanelMustBeActiveCheck());
        multiEnableCheck.add(checkFactory
                .createExactlyNLayerablesMustBeSelectedCheck(1,
                        RasterImageLayer.class));
        multiEnableCheck.add(checkFactory
                .createRasterImageLayerExactlyNBandsMustExistCheck(1));
        return multiEnableCheck;
    }

    public void save(JScrollPane pane) {
        FileNameExtensionFilter filter;
        final JPanel panel = (JPanel) pane.getViewport().getView();
        final int w = panel.getWidth();
        final int h = panel.getHeight();
        final BufferedImage bi = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = bi.createGraphics();
        panel.paint(g);

        filter = new FileNameExtensionFilter("Portable Network Graphics (png)",
                "png");
        final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
                "png");

        fc.setFileFilter(filter);
        fc.addChoosableFileFilter(filter);
        final int returnVal = fc.showSaveDialog(JUMPWorkbench.getInstance()
                .getFrame());
        fc.getWidth();
        fc.getHeight();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                final File file = new File(fc.getSelectedFile() + "-legend"
                        + ".png");
                ImageIO.write(bi, "png", file);

                saved(file);
            } catch (final Exception e) {
                notsaved();
                Logger(this.getClass(), e);
            }
        }
    }

    public double cellSizeX(Raster r, Envelope env) throws IOException {
        return env.getWidth() / r.getWidth();
    }

    public double cellSizeY(Raster r, Envelope env) throws IOException {
        return env.getHeight() / r.getHeight();
    }

    public static BufferedImage joinBufferedImage(BufferedImage img1,
            BufferedImage img2) {

        // do some calculate first
        final int offset = 5;
        final int wid = img1.getWidth() + img2.getWidth() + offset;
        final int height = Math.max(img1.getHeight(), img2.getHeight())
                + offset;
        // create a new buffer and draw two image into the new image
        final BufferedImage newImage = new BufferedImage(wid, height,
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = newImage.createGraphics();
        final Color oldColor = g2.getColor();
        // fill background
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, wid, height);
        // draw image
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth() + offset, 0);
        g2.dispose();
        return newImage;
    }

    public static void Logger(Class<?> plugin, Exception e) {
        final Logger LOG = Logger.getLogger(plugin);
        JUMPWorkbench
                .getInstance()
                .getFrame()
                .warnUser(
                        plugin.getSimpleName() + " Exception: " + e.toString());
        LOG.error(plugin.getName() + " Exception: ", e);
    }

    protected void saved(File file) {
        JUMPWorkbench.getInstance().getFrame()
                .setStatusMessage(sSaved + " :" + file.getAbsolutePath());
    }

    protected void notsaved() {
        JOptionPane.showMessageDialog(null, SCouldNotSave, I18N.get(getName()),
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Export an image to GeoTIFF file
     * 
     * @param outFileName
     *            output file name, ex C:/folder/filename.tif
     * @param layer
     *            input raster layer. Needed to calculate cell size, envelope
     *            and no data
     * @param image
     *            BufferedImage to save
     * @return
     */
    private boolean exportToGeoTIFFFile(String sFilename, double cellsizeX,
            double cellsizeY, double nodata, Envelope envelope,
            BufferedImage image) {
        try {
            final FileOutputStream tifOut = new FileOutputStream(sFilename);
            final TIFFEncodeParam param = new TIFFEncodeParam();
            param.setCompression(1);
            final TIFFField[] tiffFields = new TIFFField[3];

            tiffFields[0] = new TIFFField(33550, 12, 2, new double[] {
                    cellsizeX, cellsizeY });
            final String noDataS = Double.toString(nodata);
            final byte[] bytes = noDataS.getBytes();
            tiffFields[1] = new TIFFField(42113, 1, noDataS.length(), bytes);

            tiffFields[2] = new TIFFField(33922, 12, 6, new double[] { 0.0D,
                    0.0D, 0.0D, envelope.getMinX(), envelope.getMaxY(), 0.0D });
            param.setExtraFields(tiffFields);
            final TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec
                    .createImageEncoder("tiff", tifOut, param);
            encoder.encode(image);
            tifOut.close();

        } catch (final Exception e) {
            return false;
        }
        return true;
    }

}
