package org.openjump.core.ui.plugin.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjump.core.geomutils.algorithm.IntersectGeometries;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.tools.AttributeMapping;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.GenericNames;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

/**
 * @author sstein
 * @url www.openjump.org
 * @curentdate 4 May 2008
 * @license GPL2
 * @todo abstract methods to be able to intersect not only two polygon layers
 * 
 * Merges/Intersects two polygon layers into one layer. It therefore calculates
 * all geometric intersections between the polygons. Afterwards the attributes
 * are transferred. The later step assumes that a new created intersection
 * polygon has at max only one correspondent polygon per layer.
 * 
 */

public class IntersectPolygonLayersPlugIn extends ThreadedBasePlugIn {

	private final static String LAYER1 = GenericNames.LAYER_A;
	private final static String LAYER2 = GenericNames.LAYER_B;
	private final static String sTRANSFER = I18N
			.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Transfer-attributes");
	private String sDescription = "Intersects all geometries of two layers that contain both polygons. Note: The Planar Graph function provides similar functionality.";
	private final static String sAccurracy = "Set calculation accuray in map units";
	// -- reset in execute to correct language
	private MultiInputDialog dialog;
	private Layer layer1, layer2;
	private String methodNameToRun;
	private boolean exceptionThrown = false;
	private PlugInContext context = null;
	private boolean transferAtt = true;
	private double accurracy = 0.01; 
	
	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuItem(
				this,
				new String[] { MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS },
				this.getName(),
				false,
				null,
				new MultiEnableCheck().add(
						new EnableCheckFactory(context.getWorkbenchContext())
								.createTaskWindowMustBeActiveCheck()).add(
						new EnableCheckFactory(context.getWorkbenchContext())
								.createAtLeastNLayersMustExistCheck(2)));

		this.sDescription = I18N
				.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.sDescrition");
	}

	public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = new MultiInputDialog(context
				.getWorkbenchFrame(), getName(), true);
		setDialogValues(dialog, context);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (!dialog.wasOKPressed()) {
			return false;
		}
		getDialogValues(dialog);
		return true;
	}

	public String getName() {
		return I18N
				.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Intersect-Polygon-Layers");
	}

	public void run(TaskMonitor monitor, PlugInContext context)
			throws Exception {
		this.context = context;
		monitor.allowCancellationRequests();
		FeatureSchema featureSchema = new FeatureSchema();
		FeatureCollection resultColl = runIntersection(layer1
				.getFeatureCollectionWrapper(), layer2
				.getFeatureCollectionWrapper(), this.accurracy, this.transferAtt, monitor,
				context);
		if ((resultColl != null) && (resultColl.size() > 0)) {
			context.addLayer(StandardCategoryNames.WORKING, I18N.get("Result"),
					resultColl);
		}
		if (exceptionThrown)
			context
					.getWorkbenchFrame()
					.warnUser(
							I18N
									.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Error-while-executing-spatial-function"));
	}

	/**
	 * Merges/Intersects two polygon layers into one layer. It therefore
	 * calculates all geometric intersections between the polygons. Afterwards
	 * the attributes are transferred. The later step assumes that a new created
	 * intersection polygon has at max only one correspondent polygon per layer.
	 * 
	 * @param fcA
	 * @param fcB
	 * @param transferAttributes
	 *            should attributes be transfered?
	 * @param monitor
	 *            can be null
	 * @param context
	 *            can be null
	 * @return a FeatureCollection that contains the result of the intersection
	 *         (i.e. the new created features)
	 */
	private FeatureCollection runIntersection(FeatureCollection fcA,
			FeatureCollection fcB, double accurracy, boolean transferAttributes,
			TaskMonitor monitor, PlugInContext context) {
		FeatureCollection fd = null;
		// -- put all geoms in one list and calculate their intersections
		// i.e. iterate until pure
		ArrayList<Geometry> geomsToCheck = new ArrayList<Geometry>();
		for (Iterator iterator = fcA.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			geomsToCheck.add(f.getGeometry());
		}
		for (Iterator iterator = fcB.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			geomsToCheck.add(f.getGeometry());
		}
		// -- sort out the different geometry types
		for (Iterator iterator = geomsToCheck.iterator(); iterator.hasNext();) {
			Geometry geom = (Geometry) iterator.next();
			if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) {
				// everything is fine
			} else {
				// --
				if (context != null) {
					context
							.getWorkbenchFrame()
							.warnUser(
									I18N
											.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Geometry-no-Polygon-or-Multi-Polygon"));
				}
				// --
				return null;
			}
		}
		ArrayList<Geometry> withoutIntersection = IntersectGeometries
				.intersectPolygons(geomsToCheck, accurracy, monitor, context);
		if (transferAttributes == false) {
			fd = FeatureDatasetFactory.createFromGeometry(withoutIntersection);
			return fd;
		}
		if (monitor != null) {
			monitor.report(this.sTRANSFER);
		}
		// ===================== Transfer Attributes ==================
		// note: if we transfer attributes we need a unique set of
		// FeatureSchemas
		// Hence a feature that has a corespondant in only one layer should
		// still
		// have all attributes from both source layers.
		// ============================================================
		// -- put all original objects in trees for faster query
		SpatialIndex treeA = new STRtree();
		for (Iterator iterator = fcA.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			treeA.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		SpatialIndex treeB = new STRtree();
		for (Iterator iterator = fcB.iterator(); iterator.hasNext();) {
			Feature f = (Feature) iterator.next();
			treeB.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		// -- get all intersecting features (usually there should be only one
		// corresponding feature per layer)
		// to avoid problems with spatial predicates we do the query for an
		// internal point of the result polygons
		// and apply an point in polygon test
		AttributeMapping mapping = new AttributeMapping(fcA.getFeatureSchema(),
				fcB.getFeatureSchema());
		// -- create the empty dataset with the final FeatureSchema
		fd = new FeatureDataset(mapping.createSchema("Geometry"));
		// -- add the features and do the attribute mapping
		for (Iterator iterator = withoutIntersection.iterator(); iterator
				.hasNext();) {
			Geometry geom = (Geometry) iterator.next();
			Point pt = geom.getInteriorPoint();
			Feature f = new BasicFeature(fd.getFeatureSchema());
			Feature featureA = null;
			Feature featureB = null;
			// -- query Layer A ---
			List candidatesA = treeA.query(pt.getEnvelopeInternal());
			int foundCountA = 0;
			for (Iterator iterator2 = candidatesA.iterator(); iterator2
					.hasNext();)

			{
				Feature ftemp = (Feature) iterator2.next();
				if (ftemp.getGeometry().contains(pt)) {
					foundCountA++;
					featureA = ftemp;
				}
			}
			if (foundCountA > 1) {
				if (context != null) {
					context
							.getWorkbenchFrame()
							.warnUser(
									I18N
											.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Found-more-than-one-source-feature-in-Layer")
											+ " " + GenericNames.LAYER_A);
				}
			} else if (foundCountA == 0) {
				if (context != null) {
					// context.getWorkbenchFrame().warnUser("no corresponding
					// feature in Layer A");
				}
			}
			// -- query Layer B ---
			List candidatesB = treeB.query(pt.getEnvelopeInternal());
			int foundCountB = 0;
			for (Iterator iterator2 = candidatesB.iterator(); iterator2
					.hasNext();)

			{
				Feature ftemp = (Feature) iterator2.next();
				if (ftemp.getGeometry().contains(pt)) {
					foundCountB++;
					featureB = ftemp;
				}
			}
			if (foundCountB > 1) {
				if (context != null) {
					context
							.getWorkbenchFrame()
							.warnUser(
									I18N
											.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Found-more-than-one-source-feature-in-Layer")
											+ " " + GenericNames.LAYER_B);
				}
			} else if (foundCountB == 0) {
				if (context != null) {
					// context.getWorkbenchFrame().warnUser("no corresponding
					// feature in Layer B");
				}
			}
			// -- do mapping
			mapping.transferAttributes(featureA, featureB, f);
			// -- set Geometry
			f.setGeometry((Geometry) geom.clone());
			fd.add(f);
		}
		// --
		return fd;
	}

	private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
		dialog.setSideBarDescription(sDescription);
		// Set initial layer values to the first and second layers in the layer
		// list.
		// In #initialize we've already checked that the number of layers >= 2.
		// [Jon Aquino]
		dialog.addLayerComboBox(LAYER1, layer1, context.getLayerManager());
		dialog.addLayerComboBox(LAYER2, layer2, context.getLayerManager());
		//dialog.addDoubleField(sAccurracy, this.accurracy, 7);
		dialog.addCheckBox(sTRANSFER, this.transferAtt);

	}

	private void getDialogValues(MultiInputDialog dialog) {
		layer1 = dialog.getLayer(LAYER1);
		layer2 = dialog.getLayer(LAYER2);
		this.transferAtt = dialog.getBoolean(sTRANSFER);
		//this.accurracy = dialog.getDouble(sAccurracy);
	}

}
