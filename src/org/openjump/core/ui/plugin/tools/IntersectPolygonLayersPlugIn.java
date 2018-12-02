/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
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
 * Stefan Steiniger
 * perriger@gmx.de
 */

package org.openjump.core.ui.plugin.tools;

import java.util.*;

import com.vividsolutions.jts.algorithm.locate.SimplePointInAreaLocator;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import org.openjump.core.geomutils.algorithm.IntersectGeometries;

import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
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
 * @version 4 May 2008
 * 
 * Merges/Intersects two polygon layers into one layer. It therefore calculates
 * all geometric intersections between the polygons. Afterwards the attributes
 * are transferred. The later step assumes that a new created intersection
 * polygon has at max only one correspondent polygon per layer.
 * TODO : abstract methods to be able to intersect not only two polygon layers
 * TODO: translate Error messages
 * 
 */
public class IntersectPolygonLayersPlugIn extends ThreadedBasePlugIn {

	private final static String LAYER1 = GenericNames.LAYER_A;
	private final static String LAYER2 = GenericNames.LAYER_B;
	private final static String sTRANSFER = I18N
			.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Transfer-attributes");
	private String sDescription = "Intersects all geometries of two layers that contain both polygons. Note: The Planar Graph function provides similar functionality.";
	private Layer layer1 = null;
	private Layer layer2 = null;
	private boolean exceptionThrown = false;
	private boolean transferAtt = true;

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuPlugin(
				this,
				new String[] {MenuNames.TOOLS, MenuNames.TOOLS_ANALYSIS},
				this.getName(),
				false,
				null,
				new MultiEnableCheck().add(
						new EnableCheckFactory(context.getWorkbenchContext())
								.createTaskWindowMustBeActiveCheck()).add(
						new EnableCheckFactory(context.getWorkbenchContext())
								.createAtLeastNLayersMustExistCheck(1)));

		this.sDescription = I18N
				.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.sDescrition");
	}

	public boolean execute(PlugInContext context) throws Exception {
		MultiInputDialog dialog = new MultiInputDialog(context
				.getWorkbenchFrame(), getName(), true);
		if(layer1 == null){
			layer1 = context.getCandidateLayer(0);
			layer2 = context.getCandidateLayer(0);
		}
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
				.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Intersect-Polygon-Layers") + "...";
	}

	public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
		monitor.allowCancellationRequests();
		FeatureCollection resultColl = runIntersectionNew(layer1
				.getFeatureCollectionWrapper(), layer2
				.getFeatureCollectionWrapper(), this.transferAtt, monitor,
				context);
		if ((resultColl != null) && (resultColl.size() > 0)) {
			context.addLayer(StandardCategoryNames.RESULT,
							I18N.get("ui.plugin.analysis.GeometryFunctionPlugIn.intersection") + "-" +
											layer1.getName() + "-" + layer2.getName(),
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
	 * extracts all lines of the polygons and creates new polygons 
	 * with the Polygonizer class. Afterwards it is checked which polygons have a
	 * correspondent in the input layers. If the polygon does not have it is removed
	 * otherwise the attributes are transferred. The later step assumes that a new created
	 * intersection polygon has at max only one correspondent polygon per layer.
	 * 
	 * @param fcA first FeatureCollection
	 * @param fcB second FeatureCollection
	 * @param transferAttributes Attributes should attributes be transfered?
	 * @param monitor can be null
	 * @param context can be null
	 * @return a FeatureCollection that contains the result of the intersection
	 *         (i.e. the new created features)
	 */
	private FeatureCollection runIntersectionNew(FeatureCollection fcA,
			FeatureCollection fcB, boolean transferAttributes,
			TaskMonitor monitor, PlugInContext context) {

		// put all geoms in a single list and prepare two indexes
		monitor.report("Extract linearComponents and create indexes");
		Collection<LineString> linearComponents = new ArrayList<>();
		for (Feature f : fcA.getFeatures()) {
			LinearComponentExtracter.getLines(f.getGeometry(), linearComponents);
		}
		for (Feature f : fcB.getFeatures()) {
			LinearComponentExtracter.getLines(f.getGeometry(), linearComponents);
		}

		// de-duplicate segments
		monitor.report("De-duplicate segments");
		Set<LineString> lines = new HashSet<>();
		GeometryFactory gf = new GeometryFactory();
		for (LineString linearComponent : linearComponents) {
			Coordinate[] coordinates = linearComponent.getCoordinates();
			for (int i = 1 ; i < coordinates.length ; i++) {
				LineString line;
				if (coordinates[i-1].x < coordinates[i].x) {
					line = gf.createLineString(new Coordinate[]{coordinates[i - 1], coordinates[i]});
				} else if (coordinates[i-1].x > coordinates[i].x) {
					line = gf.createLineString(new Coordinate[]{coordinates[i], coordinates[i-1]});
				} else if (coordinates[i-1].y < coordinates[i].y) {
					line = gf.createLineString(new Coordinate[]{coordinates[i-1], coordinates[i]});
				} else {
					line = gf.createLineString(new Coordinate[]{coordinates[i], coordinates[i-1]});
				}
				lines.add(line);
			}
		}
		linearComponents.clear();

		monitor.report("Merge segments into LineStrings");
		LineMerger merger = new LineMerger();
		merger.add(lines);
		lines.clear();
		linearComponents = merger.getMergedLineStrings();

		//-- calculate the intersections and use the Polygonizer
		monitor.report("Node the linework");
		Collection nodedLines = IntersectGeometries.nodeLines(linearComponents);
		linearComponents.clear();
	  Polygonizer polygonizer = new Polygonizer();
	  polygonizer.add(nodedLines);
		nodedLines.clear();

	  //-- get the Polygons
		monitor.report("Polygonize");
		Collection<Geometry> withoutIntersection = polygonizer.getPolygons();
		polygonizer = null;

		// -- get all intersecting features (usually there should be only one
		// corresponding feature per layer)
		// to avoid problems with spatial predicates we do the query for an
		// internal point of the result polygons
		// and apply an point in polygon test
		monitor.report("Attribute mapping");
		AttributeMapping mapping = new AttributeMapping(fcA.getFeatureSchema(),
				fcB.getFeatureSchema());
    SpatialIndex treeA = new STRtree();
    SpatialIndex treeB = new STRtree();
    for (Feature f : fcA.getFeatures()) {
      treeA.insert(f.getGeometry().getEnvelopeInternal(), f);
    }
    for (Feature f : fcB.getFeatures()) {
      treeB.insert(f.getGeometry().getEnvelopeInternal(), f);
    }

		// -- create the empty dataset with the final FeatureSchema
		FeatureCollection fd = new FeatureDataset(mapping.createSchema("Geometry"));
		// -- add the features and do the attribute mapping
		int count = 0;
		List<Integer> errorsInA = new ArrayList<>();
		List<Integer> errorsInB = new ArrayList<>();
		for (Geometry geom : withoutIntersection) {
			monitor.report(count++, withoutIntersection.size(), "polygon");
			boolean errorInA = false;
			boolean errorInB = false;
			Coordinate coord = geom.getInteriorPoint().getCoordinate();
			Envelope envelope = new Envelope(coord, coord);
			Feature f = new BasicFeature(fd.getFeatureSchema());
			Feature featureA = null;
			Feature featureB = null;
			// -- query Layer A ---
			List<Feature> candidatesA = treeA.query(envelope);
			int foundCountA = 0;
			for (Feature ftemp : candidatesA){
				if (SimplePointInAreaLocator.locate(coord, ftemp.getGeometry()) == Location.INTERIOR) {
					foundCountA++;
					featureA = ftemp;
				}
			}
			if (foundCountA > 1) {
				errorInA = true;
				errorsInA.add(f.getID());
			}
			// -- query Layer B ---
			List<Feature> candidatesB = treeB.query(envelope);
			int foundCountB = 0;
			for (Feature ftemp : candidatesB){
				if (SimplePointInAreaLocator.locate(coord, ftemp.getGeometry()) == Location.INTERIOR) {
					foundCountB++;
					featureB = ftemp;
				}
			}
			if (foundCountB > 1) {
				errorInB = true;
				errorsInB.add(f.getID());
			}
			if ((foundCountA > 0) || (foundCountB > 0)){ 
				// -- before mapping check and set for error values
				if (errorInA){
					featureA = resetFeatureValuesToNull(featureA);
				}
				if (errorInB){
					featureB = resetFeatureValuesToNull(featureB);
				}
				// -- do mapping
				mapping.transferAttributes(featureA, featureB, f);
				// -- set Geometry
				f.setGeometry((Geometry) geom.clone());
				fd.add(f);
			}
		}
		if (context != null) {
			if (errorsInA.size() > 0 || errorsInB.size() > 0) {
				String errorStrg = I18N.get("org.openjump.plugin.tools.IntersectPolygonLayersPlugIn.Found-more-than-one-source-feature-in-Layer");
				String layers = "";
				if (errorsInA.size() > 0) {
					layers = layers + " " + GenericNames.LAYER_A;
          context.getWorkbenchFrame().getOutputFrame().createNewDocument();
					context.getWorkbenchFrame().getOutputFrame().addText(
									"IntersectPolygonLayersPlugIn: " + errorStrg + ": " + GenericNames.LAYER_A + "." +
									"\n" +
									"Reason: The Layer contains probably objects that overlay each other. Will set polygon values of items with FID: " + errorsInA +
									" to NaN. Use " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfer-Attributes") +
									" or " + I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.Join-Attributes-Spatially") +
									" functions to obtain atributes from " + GenericNames.LAYER_A);
				}
				if (errorsInB.size() > 0) {
					layers = layers + " " + GenericNames.LAYER_B;
          context.getWorkbenchFrame().getOutputFrame().createNewDocument();
					context.getWorkbenchFrame().getOutputFrame().addText(
									"IntersectPolygonLayersPlugIn: " + errorStrg + ": " + GenericNames.LAYER_B + "." +
									"\n" +
									"Reason: The Layer contains probably objects that overlay each other. Will set polygon values of items with FID: " + errorsInB +
									" to NaN. Use " + I18N.get("org.openjump.sigle.plugin.SpatialJoinPlugIn.Transfer-Attributes") +
									" or " + I18N.get("org.openjump.core.ui.plugin.tools.JoinAttributesSpatiallyPlugIn.Join-Attributes-Spatially") +
									" functions to obtain atributes from " + GenericNames.LAYER_B);
				}
				context.getWorkbenchFrame().warnUser(errorStrg + layers);
			}
		}
		return fd;
	}
	
	/**
	 * All values are set to NaN.
	 * @param f feature to reset
	 */
	static Feature resetFeatureValuesToNull(Feature f){
		//-- work only on a copy so the original feature isn't changed
		Feature ftemp = f.clone(true);
		FeatureSchema fs = ftemp.getSchema();
		for (int i = 0; i < fs.getAttributeCount(); i++){
			AttributeType type = fs.getAttributeType(i); 
			if (!type.equals(AttributeType.GEOMETRY)){
				if(type.equals(AttributeType.DOUBLE)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.INTEGER)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.LONG)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.STRING)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.OBJECT)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.DATE)){
					ftemp.setAttribute(i, null);
				}
				if(type.equals(AttributeType.BOOLEAN)){
					ftemp.setAttribute(i, null);
				}
			}
		}
		return ftemp;
	}


	private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
		dialog.setSideBarDescription(sDescription);
		// Set initial layer values to the first and second layers in the layer list.
		// In #initialize we've already checked that the number of layers >= 2.
		// [Jon Aquino]
		dialog.addLayerComboBox(LAYER1, layer1, context.getLayerManager());
		dialog.addLayerComboBox(LAYER2, layer2, context.getLayerManager());
	}


	private void getDialogValues(MultiInputDialog dialog) {
		layer1 = dialog.getLayer(LAYER1);
		layer2 = dialog.getLayer(LAYER2);
	}

}
