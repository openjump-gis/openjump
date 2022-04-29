package org.openjump.sextante.gui.additionalResults;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.namespace.QName;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.math.plot.PlotPanel;
import org.math.plot.plots.Plot;
import org.openjump.core.apitools.IOTools;
import org.openjump.core.ui.io.file.FileNameExtensionFilter;
import org.openjump.core.ui.util.LayerableUtil;
import org.openjump.sextante.core.ObjectAndDescription;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.ui.FeatureCollectionPanel;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.HTMLPanel;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

import it.betastudio.adbtoolbox.libs.DxfExport;


/**
 * Moved export action to a separate class
 * @author Giuseppe Aruta [2020-06-11]
 */



public class AdditionalResultsIO {

	private final static String sSaved = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.RasterImageLayerPropertiesPlugIn.file.saved");
	private final static String SCouldNotSave = I18N.getInstance().get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Could-not-save-selected-result");


	static WorkbenchFrame frame = JUMPWorkbench.getInstance().getFrame();
	static WorkbenchContext wContext = frame.getContext();



	public static void save(TreePath m_Path) {
		FileNameExtensionFilter filter;
		final File file;
		if (m_Path != null) {
			try {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
						.getLastPathComponent();
				final ObjectAndDescription oad = (ObjectAndDescription) node
						.getUserObject();
				final Component c = (Component) oad.getObject();
				if (c instanceof FeatureCollectionPanel) {
					final FeatureCollectionPanel panel = (FeatureCollectionPanel) c;
					final FeatureCollection fcoll = panel
							.getFeatureCollection();
					final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
							"Comma-Separated Values (csv)", "csv");
					final FileNameExtensionFilter filter3 = new FileNameExtensionFilter(
							"JUMP Markup Language (JML)", "jml");
					filter = new FileNameExtensionFilter(
							"ESRI Shapefile (SHP)", "shp");
					final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
					if (!LayerableUtil.isMixedGeometryType(fcoll)) {
						fc.setFileFilter(filter);
					}
					fc.setFileFilter(filter3);
					fc.setFileFilter(filter2);
					fc.addChoosableFileFilter(filter2);
					final int returnVal = fc
							.showSaveDialog(frame);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						if (fc.getFileFilter().equals(filter3)) {
							file = new File(fc.getSelectedFile() + ".jml");
							IOTools.saveJMLFile(fcoll, file.getAbsolutePath());
							saved(file);
						} else if (fc.getFileFilter().equals(filter)) {
							file = new File(fc.getSelectedFile() + ".shp");
							IOTools.saveShapefile(fcoll, file.getAbsolutePath());
							saved(file);
						} else if (fc.getFileFilter().equals(filter2)) {
							final JTable table = panel.getTable();
							file = new File(fc.getSelectedFile() + ".csv");
							IOTools.saveCSV(table, file.getAbsolutePath());
							saved(file);
						}
					}

				} else if (c instanceof JScrollPane) {
					final JScrollPane pane = (JScrollPane) c;
					final Component view = pane.getViewport().getView();
					if (view instanceof JTextPane || view instanceof JLabel
							|| view instanceof JTextArea) {
						final JTextPane text = (JTextPane) pane.getViewport()
								.getView();
						final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
								"html");
						filter = new FileNameExtensionFilter("HTML", "html");
						fc.setFileFilter(filter);
						fc.addChoosableFileFilter(filter);
						final int returnVal = fc
								.showSaveDialog(frame);
						fc.getWidth();
						fc.getHeight();
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							try {
								file = new File(fc.getSelectedFile() + ".html");
								file.getParent();
								final FileWriter fileWriter = new FileWriter(
										file);
								final BufferedWriter bufferedWriter = new BufferedWriter(
										fileWriter);
								bufferedWriter.write(text.getText());
								bufferedWriter.close();
								saved(file);
							} catch (final Exception e) {
								notsaved();
							}
						}
					} else if (view instanceof JTable) {
						final JTable table = (JTable) pane.getViewport()
								.getView();
						filter = new FileNameExtensionFilter(
								"Comma-Separated Values (csv)", "csv");
						final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
								"csv");
						fc.setFileFilter(filter);
						fc.addChoosableFileFilter(filter);
						final int returnVal = fc
								.showSaveDialog(frame);
						fc.getWidth();
						fc.getHeight();
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							file = new File(fc.getSelectedFile() + ".csv");
							IOTools.saveCSV(table, file.getAbsolutePath());
						} else if (returnVal == JFileChooser.CANCEL_OPTION) {
							return;
						}
					}
					// [Giuseppe Aruta 2018-08-22] add generic save view to
					// image of JPanel in a JScrollPane
					else if (view instanceof JPanel) {
						final JPanel panel = (JPanel) pane.getViewport()
								.getView();
						final int w = panel.getWidth();
						final int h = panel.getHeight();
						final BufferedImage bi = new BufferedImage(w, h,
								BufferedImage.TYPE_INT_RGB);
						final Graphics2D g = bi.createGraphics();
						panel.paint(g);

						filter = new FileNameExtensionFilter(
								"Portable Network Graphics (png)", "png");
						final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
								"png");
						final FileNameExtensionFilter filter3 = new FileNameExtensionFilter(
								"Scalable Vector Graphics (svg)", "svg");
						fc.setFileFilter(filter);
						fc.setFileFilter(filter3);
						fc.addChoosableFileFilter(filter);
						final int returnVal = fc
								.showSaveDialog(frame);
						fc.getWidth();
						fc.getHeight();
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							if (fc.getFileFilter().equals(filter)) {
								try {
									file = new File(fc.getSelectedFile() + ".png");
									ImageIO.write(bi, "png", file);
									saved(file);
								} catch (final Exception e) {
									notsaved();

								}
							} else if (fc.getFileFilter().equals(filter3)) {
								file = new File(fc.getSelectedFile() + ".svg");
								file.getParent();
								saveAsSVG(panel,file);
								saved(file);
							}
						}
					}
				} else if (c instanceof PlotPanel) {
					final PlotPanel panel = (PlotPanel) c;
					filter = new FileNameExtensionFilter(
							"Portable Network Graphics (png)", "png");
					final FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
							"Drawing Interchange Format(dxf)", "dxf");
					final FileNameExtensionFilter filterSVG = new FileNameExtensionFilter(
							"Scalable Vector Graphics (svg)", "svg");
					final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting();
					if (oad.getDescription()
							.contains(
									I18N.getInstance().get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.Profile-Plot"))) {
						fc.setFileFilter(filter2);
					}
					fc.setFileFilter(filter);
					if(CheckSVGLibs()) {
						fc.setFileFilter(filterSVG);}
					fc.addChoosableFileFilter(filter);
					final int returnVal = fc
							.showSaveDialog(frame);
					fc.getWidth();
					fc.getHeight();
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						if (fc.getFileFilter().equals(filter)) {
							file = new File(fc.getSelectedFile() + ".png");
							file.getParent();
							panel.toGraphicFile(file);
							saved(file);
						} else if (fc.getFileFilter().equals(filter2)) {
							file = new File(fc.getSelectedFile() + ".dxf");
							file.getParent();
							double[][] pointsOfProfile = null;
							for (final Plot plot : panel.getPlots()) {
								pointsOfProfile = plot.getData();
							}
							frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
							exportToDxf(file.getAbsolutePath(), pointsOfProfile);
							frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
							saved(file);
						} else if (fc.getFileFilter().equals(filterSVG)) {
							file = new File(fc.getSelectedFile() + ".svg");
							file.getParent();
							saveAsSVG(panel.plotCanvas,file);
							saved(file);
						}
					}

				} else if (c instanceof JTable) {
					final JTable table = (JTable) c;
					filter = new FileNameExtensionFilter(
							"Comma-Separated Values (csv)", "csv");
					final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
							"csv");
					fc.setFileFilter(filter);
					fc.addChoosableFileFilter(filter);
					final int returnVal = fc
							.showSaveDialog(frame);
					fc.getWidth();
					fc.getHeight();
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						file = new File(fc.getSelectedFile() + ".csv");
						IOTools.saveCSV(table, file.getAbsolutePath());
					} else if (returnVal == JFileChooser.CANCEL_OPTION) {
						return;
					}
				} else if (c instanceof HTMLPanel) {
					final HTMLPanel panel = (HTMLPanel) c;
					final String text = panel.lastString();
					filter = new FileNameExtensionFilter("HTML", "html");
					final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
							"html");
					fc.setFileFilter(filter);
					fc.addChoosableFileFilter(filter);
					final int returnVal = fc
							.showSaveDialog(frame);
					fc.getWidth();
					fc.getHeight();
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						try {
							file = new File(fc.getSelectedFile() + ".html");
							file.getParent();
							final FileWriter fileWriter = new FileWriter(file);
							final BufferedWriter bufferedWriter = new BufferedWriter(
									fileWriter);
							bufferedWriter.write(text);
							bufferedWriter.close();
							saved(file);
						} catch (final Exception e) {
							notsaved();

						}
					}
				} else if (c instanceof JPanel) {
					final JPanel panel = (JPanel) c;
					final int w = panel.getWidth();
					final int h = panel.getHeight();
					final BufferedImage bi = new BufferedImage(w, h,
							BufferedImage.TYPE_INT_RGB);
					final Graphics2D g = bi.createGraphics();
					panel.paint(g);
					filter = new FileNameExtensionFilter(
							"Portable Network Graphics (png)", "png");

					final FileNameExtensionFilter filterSVG = new FileNameExtensionFilter(
							"Scalable Vector Graphics (svg)", "svg"); 
					final JFileChooser fc = new GUIUtil.FileChooserWithOverwritePrompting(
							"png");
					fc.setFileFilter(filter);
					if(CheckSVGLibs()) {
						fc.setFileFilter(filterSVG);}
					fc.addChoosableFileFilter(filter);
					final int returnVal = fc
							.showSaveDialog(frame);
					fc.getWidth();
					fc.getHeight();
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						if (fc.getFileFilter().equals(filter)) {

							try {
								file = new File(fc.getSelectedFile() + ".png");
								ImageIO.write(bi, "png", file);
								saved(file);
							} catch (final Exception e) {
								notsaved();
							}
						} else if (fc.getFileFilter().equals(filterSVG)) {
							file = new File(fc.getSelectedFile() + ".svg");
							file.getParent();
							saveAsSVG(panel,file);
							saved(file);
						}
					}

				}
			} catch (final Exception e) {
			}
		}
	}

	protected static void saved(File file) {
		JUMPWorkbench.getInstance().getFrame()
		.setStatusMessage(sSaved + " :" + file.getAbsolutePath());
	}

	protected static void notsaved() {
		JOptionPane.showMessageDialog(null, SCouldNotSave, I18N.getInstance().get(SCouldNotSave),
				JOptionPane.WARNING_MESSAGE);
	}

	public static void Logger(Class<?> plugin, Exception e) {
		JUMPWorkbench
		.getInstance()
		.getFrame()
		.warnUser(
				plugin.getSimpleName() + " Exception: " + e.toString());
		Logger.error(plugin.getName() + " Exception: ", e);
	}



	public static  void saveAsSVG(Component component, 
			File selFile) throws Exception {
		SVGGraphics2D svgGenerator = drawSvgGraphics(component);
		try {
			FileOutputStream fos = new FileOutputStream(selFile, false);
			OutputStreamWriter out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			svgGenerator.stream(out, true);
			out.close();
		}
		catch (Exception e) {
			wContext.getWorkbench().getFrame().handleThrowable(e);
		}
	}



	/**
	 * Draws the selected component (assumed to be a Component) into the
	 * provided SVGGraphics2D object.
	 *
	 * @param component the component to draw the svg graphic on
	 */
	private static SVGGraphics2D drawSvgGraphics(Component component) {

		// Get a SVGDOMImplementation and create an XML document
		DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
		String svgNS = "http://www.w3.org/2000/svg";
		Document svgDocument = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(svgDocument);

		svgGenerator.setSVGCanvasSize(component.getSize());
		component.paintAll(svgGenerator);
		svgGenerator.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
		svgGenerator.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_DEFAULT); 

		svgGenerator.scale(0.90/0.96, 0.90/0.96); 
		return svgGenerator;
	}

	public final static String HEIGHT = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");
	public final static String WIDTH = I18N.getInstance().get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance");

	public static Integer round100(Integer b) {
		return b - (b % 100);
	}

	// [Giuseppe Aruta 2018-3-14] The following code derives from AdbToolbox
	// Raster>Topography>Section code.
	// see also class it.betastudio.adbtoolbox.libs.DxfExport
	// [Giuseppe Aruta 2018-3-22] Enhenced dxf output

	public static void exportToDxf(String fileName, double[][] points) {

		try {

			double minX = Double.MAX_VALUE;
			double maxX = -minX;
			double minY = Double.MAX_VALUE;
			double maxY = -minY;

			// Find max and min vals
			for (final double[] point : points) {
				if (point[0] < minX) {
					minX = point[0];
				}
				if (point[0] > maxX) {
					maxX = point[0];
				}
				if (point[1] < minY) {
					minY = point[1];
				}
				if (point[1] > maxY) {
					maxY = point[1];
				}
			}
			final DecimalFormatSymbols dfs = new DecimalFormatSymbols(
					Locale.ENGLISH);
			DecimalFormat twoPlaces = null;

			final String twoPlacesS = "0.00";
			twoPlaces = new DecimalFormat(twoPlacesS, dfs);

			final int pointsCount = points.length;

			final int txtHight = 14;
			final int txtHight2 = 22;

			// min elevation of the grid, 200m below the min elevation of the
			// profile
			final int b = ((int) minY - 99) / 100 * 100;
			final double baseElev = b - 100;

			// max elevation of the grid, 200m above the max elevation of the
			// profile
			final int a = ((int) maxY + 99) / 100 * 100;
			final double topElev = a + 200;

			final int sepSpacing = 10 * txtHight;
			final String layNameProf = "PROFILE";
			final String baseSectionLayer = "BASE";
			final String partialValuesLayer = "PARTIAL_VALUES";
			final String gridLayer = "GRID";
			final String layNameText = "GRID_TEXT";

			// Main points coords
			final double sep1Y = baseElev - txtHight * 2;
			final double sep2Y = sep1Y - sepSpacing;
			final double sep3Y = topElev;
			final double leg5Y = sep3Y + sepSpacing;
			final double legX = minX - (txtHight * 30);

			final DxfExport dxfExp = new DxfExport();

			// Write header
			// --------------------------------------------------------
			dxfExp.writeHeader(legX, sep3Y, maxX, maxY);

			// Write tables
			dxfExp.writeStartSec();
			dxfExp.writeTablesStart();

			dxfExp.writeTableStart();
			dxfExp.writeVPort((maxX + legX) / 2, (maxY + sep3Y) / 2, 0, 0, 1, 1);
			dxfExp.writeTableEnd();

			dxfExp.writeTableStart();
			dxfExp.writeAppId();
			dxfExp.writeTableEnd();

			dxfExp.writeTableStart();
			dxfExp.writeLayersStart();
			dxfExp.writeLayer(layNameProf, 5);
			dxfExp.writeLayer(baseSectionLayer, 0);
			dxfExp.writeLayer(partialValuesLayer, 8);
			dxfExp.writeLayer(gridLayer, 9);
			dxfExp.writeLayer(layNameText, 7);
			dxfExp.writeTableEnd();

			dxfExp.writeEndSec();

			// Write section
			dxfExp.writeStartSec();
			dxfExp.writeEntStart();
			dxfExp.writePolyline(layNameProf, points);

			// Write legend

			final Task selectedTask = wContext.getTask();
			String unitsDistLabel = "";

			if (selectedTask.getProperties().containsKey(
					new QName(Task.PROJECT_UNIT_KEY))) {
				unitsDistLabel = " ["
						+ selectedTask.getProperty(
								new QName(Task.PROJECT_UNIT_KEY)).toString()
						+ "]";
			} else {
				unitsDistLabel = "";
			}

			// Text of X axe
			dxfExp.writeText(layNameText, 0, 0, 0, (maxX - minX) / 2, sep2Y, 0,
					txtHight2, 0, 0, 2, WIDTH + unitsDistLabel);

			dxfExp.writeText(partialValuesLayer, 0, 0, 0, (maxX - minX) / 2,
					leg5Y, 0, txtHight2, 0, 0, 2, HEIGHT + unitsDistLabel);
			// Text of Y axe
			dxfExp.writeText(layNameText, 0, 0, 0, minX - 200,
					(topElev + baseElev) / 2, 0, txtHight2, 90, 0, 2, HEIGHT
					+ unitsDistLabel);
			dxfExp.writeText(layNameText, 0, 0, 0, maxX + 200,
					(topElev + baseElev) / 2, 0, txtHight2, 270, 0, 2, HEIGHT
					+ unitsDistLabel);

			// Write interpoints labels and ticks
			double p1x = 0;
			double p1y = 0;
			double p2x = 0;
			double p2y = 0;
			double alPt1x = 0;
			double alPt1y = 0;
			double alPt2y = 0;
			String labelText = null;

			int interPointsCount = 0;
			// Count interpoints
			double[] interStepD = null;

			interStepD = new double[1];
			// Define 100 meters for length/height steps
			interStepD[0] = 100D;
			interPointsCount = (int) ((maxX - minX) / interStepD[0]) + 2;

			// Prepare x positions
			final double[] interPointsDists = new double[interPointsCount];
			final double[] interPointsElev = new double[interPointsCount];
			for (int ip = 0; ip < interPointsCount; ip++) {

				if (ip < interPointsCount - 1) {
					interPointsDists[ip] = interStepD[0] * ip;
				} else {
					interPointsDists[ip] = maxX;
				}

			}

			// Prepare points IDs
			final int[] interPointsIds = new int[interPointsCount];
			int ipId = 0;
			for (int p = 1; p < pointsCount; p++) {
				if (points[p][0] >= interPointsDists[ipId]) {
					if (Math.abs(points[p][0] - interPointsDists[ipId]) <= Math
							.abs(interPointsDists[ipId] - points[p - 1][0])) {
						interPointsIds[ipId] = p;
					} else {
						interPointsIds[ipId] = p - 1;
					}
					ipId++;
				}
			}

			// Boh
			if (interPointsIds[interPointsCount - 1] == 0) {
				interPointsIds[interPointsCount - 1] = pointsCount - 1;
			}

			// Prepare y positions
			ipId = 0;
			for (int p = 1; p < pointsCount; p++) {
				if (points[p][0] >= interPointsDists[ipId]) {
					if (Math.abs(points[p][0] - interPointsDists[ipId]) <= Math
							.abs(interPointsDists[ipId] - points[p - 1][0])) {
						interPointsIds[ipId] = p;
					} else {
						interPointsIds[ipId] = p - 1;
					}

					interPointsElev[ipId] = (interPointsDists[ipId] - points[p - 1][0])
							/ (points[p][0] - points[p - 1][0])
							* (points[p][1] - points[p - 1][1])
							+ points[p - 1][1];
					ipId++;
				}
			}

			for (int ip = 0; ip < interPointsCount; ip++) { // OKKIO

				if (baseElev + interPointsDists[ip] < topElev
						|| baseElev + interPointsDists[ip] == topElev) {

					// Grid - orizontal lines every 100 m
					p1x = maxX;
					p1y = interPointsDists[ip] + baseElev;
					p2x = minX;
					p2y = interPointsDists[ip] + baseElev;
					dxfExp.writeLineType(gridLayer, "DOTTINY", p1x, p1y, p2x,
							p2y);

					// Heights (Y) - text on right and left part of the profile
					// every 100m
					alPt1x = minX - 100;// sep1Y + txtHight;
					final double alPt1ax = maxX + txtHight;//
					alPt1y = interPointsDists[ip] + baseElev;
					labelText = twoPlaces.format(interPointsDists[ip]
							+ baseElev);
					dxfExp.writeText(layNameText, 0, 0, 0, alPt1x, alPt1y, 0,
							txtHight, 0, 0, 2, labelText);
					dxfExp.writeText(layNameText, 0, 0, 0, alPt1ax, alPt1y, 0,
							txtHight, 0, 0, 2, labelText);

					// Heights (Y) - small tracks on right and left part of the
					// profile
					alPt1x = minX;
					alPt1y = interPointsDists[ip] + baseElev;
					final double alPt2x = minX - (txtHight / 2.0);
					dxfExp.writeLine(baseSectionLayer, alPt1x, alPt1y, alPt2x,
							alPt1y);
					final double alPt2x2 = maxX + (txtHight / 2.0);
					dxfExp.writeLine(baseSectionLayer, maxX, alPt1y, alPt2x2,
							alPt1y);

				}
			}

			for (int ip = 0; ip < interPointsCount; ip++) { // OKKIO

				// Grid - vertical lines every 100 m
				p1x = interPointsDists[ip];
				p1y = topElev;// interPointsElev[ip];
				p2x = interPointsDists[ip];
				p2y = baseElev;// minY;
				dxfExp.writeLineType(gridLayer, "DOTTINY", p1x, p1y, p2x, p2y);

				// Widths (X axe) - texts below the X line, every 100 m
				alPt1x = interPointsDists[ip];
				alPt1y = sep1Y - 4 * txtHight;
				labelText = twoPlaces.format(interPointsDists[ip]);
				dxfExp.writeText(layNameText, 0, 0, 0, alPt1x, alPt1y, 0,
						txtHight, 90, 0, 2, labelText);

				// Widths (X axe) - small tracks above the texts
				alPt1x = interPointsDists[ip];
				alPt1y = baseElev;
				alPt2y = baseElev - (txtHight / 2.0);
				dxfExp.writeLine(baseSectionLayer, alPt1x, alPt1y, alPt1x,
						alPt2y);

				// Partial heights - texts per every 100 width
				alPt1x = interPointsDists[ip];
				alPt1y = sep3Y + txtHight;
				labelText = twoPlaces.format(points[interPointsIds[ip]][1]);
				dxfExp.writeText(partialValuesLayer, 0, 0, 0, alPt1x, alPt1y,
						0, txtHight, 90, 0, 2, labelText);

				// Partial heights - small tracks
				alPt1y = sep3Y;
				alPt2y = sep3Y + (txtHight / 2.0);
				dxfExp.writeLine(partialValuesLayer, alPt1x, alPt1y, alPt1x,
						alPt2y);

			}

			// Width - X line of the profile
			dxfExp.writeLine(baseSectionLayer, minX, baseElev, maxX, baseElev);
			// Height - Y lines of the profile
			dxfExp.writeLine(baseSectionLayer, minX, baseElev, minX, topElev);
			dxfExp.writeLine(baseSectionLayer, maxX, baseElev, maxX, topElev);
			// Partial heights width line
			dxfExp.writeLine(partialValuesLayer, minX, sep3Y, maxX, sep3Y);
			// Finalize DXF
			dxfExp.writeEnding();
			final int ret = dxfExp.exportDxf(fileName);
			if (ret == 0) {
				return;
			} else {
				return;
			}
		} catch (final Exception ex) {
			wContext.getWorkbench().getFrame()
			.warnUser("Errore durante l'esportazione: ");
			return;
		}
	}

	public static boolean CheckSVGLibs() {
		ClassLoader cl = JUMPWorkbench.getInstance().getPlugInManager().getClassLoader();
		Class c = null, c2 = null;
		try {
			c = cl.loadClass("org.apache.batik.dom.GenericDOMImplementation");
			c2 = cl.loadClass("org.apache.batik.svggen.SVGGraphics2D");
		}
		catch (ClassNotFoundException e) {

			Logger(JUMPWorkbench.getInstance().getPlugInManager().getClass(), e);
		}
		if (c == null || c2 == null) {
			JUMPWorkbench
			.getInstance()
			.getFrame()
			.warnUser( " not initialized because batik is missing.");
			return false;
		} else {
			return true;}
	}


}
