
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

package com.vividsolutions.jump.workbench.ui.plugin.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

public class RandomTrianglesPlugIn extends AbstractPlugIn {

	private GeometryFactory geometryFactory = new GeometryFactory();
	private WKTReader wktReader = new WKTReader(geometryFactory);
	private List cities =
		Arrays.asList(
				"Alabama",
				"Alaska",
				"Arizona",
				"Arkansas",
				"California",
				"Colorado",
				"Connecticut",
				"Delaware",
				"Florida",
				"Georgia",
				"Hawaii",
				"Idaho",
				"Illinois",
				"Indiana",
				"Iowa",
				"Kansas",
				"Kentucky",
				"Louisiana",
				"Maine",
				"Maryland",
				"Massachusetts",
				"Michigan",
				"Minnesota",
				"Mississippi",
				"Missouri",
				"Montana",
				"Nebraska",
				"Nevada",
				"New Hampshire",
				"New Jersey",
				"New Mexico",
				"New York",
				"North Carolina",
				"North Dakota",
				"Ohio",
				"Oklahoma",
				"Oregon",
				"Pennsylvania",
				"Rhode Island",
				"South Carolina",
				"South Dakota",
				"Tennessee",
				"Texas",
				"Utah",
				"Vermont",
				"Virginia",
				"Washington",
				"West Virginia",
				"Wisconsin",
				"Wyoming");

	public RandomTrianglesPlugIn() {
	}
	
	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addLayerViewMenuItem(
			this,
			new String[] {MenuNames.TOOLS, MenuNames.TOOLS_GENERATE },
			getName());
	}

	public boolean execute(PlugInContext context)
		throws ParseException, IOException {
		return execute(context, 50);
	}

	public boolean execute(PlugInContext context, int layerSize)
		throws ParseException, IOException {
		//    String inputValue = JOptionPane.showInputDialog(
		//        context.getWorkbenchFrame(),
		//        "Number of layers to generate (Default = 2):", getName(), JOptionPane.QUESTION_MESSAGE);
		//    if (inputValue == null) { return false; } //User hit Cancel [Jon Aquino]
		//    int n;
		//    try {
		//      n = Integer.parseInt(inputValue);
		//    }
		//    catch (NumberFormatException e) {
		//      n = 2;
		//    }
		int n = 1;

		for (int i = 0; i < n; i++) {
			generateLayer(context, layerSize);
		}

		return true;
	}

	private void generateLayer(PlugInContext context, int size)
		throws ParseException, IOException {

		FeatureSchema featureSchema = new FeatureSchema();
		featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);
		featureSchema.addAttribute("City", AttributeType.STRING);
		featureSchema.addAttribute("A_Date", AttributeType.DATE);

		//Put GEOMETRY in this unusual position to test robustness of
		//AttributeTableModel [Jon Aquino]
		//    featureSchema.addAttribute("Geometry", AttributeType.GEOMETRY);
		featureSchema.addAttribute("B_Integer", AttributeType.INTEGER);
		featureSchema.addAttribute("C_Double", AttributeType.DOUBLE);
		featureSchema.addAttribute("D_Long", AttributeType.LONG);
		featureSchema.addAttribute("E_Boolean", AttributeType.BOOLEAN);
		featureSchema.addAttribute("F_Code", AttributeType.STRING);
		featureSchema.addAttribute("G_Code", AttributeType.STRING);
		featureSchema.addAttribute("H_Code", AttributeType.STRING);
		featureSchema.addAttribute("I_Code", AttributeType.STRING);
		featureSchema.addAttribute("J_Code", AttributeType.STRING);
		featureSchema.addAttribute("K_Code", AttributeType.STRING);
		featureSchema.addAttribute("L_Code", AttributeType.STRING);
		featureSchema.addAttribute("M_Code", AttributeType.STRING);
		featureSchema.addAttribute("N_Code", AttributeType.STRING);
		featureSchema.addAttribute("O_Code", AttributeType.STRING);
		featureSchema.addAttribute("P_Code", AttributeType.STRING);

		FeatureCollection featureCollection = new FeatureDataset(featureSchema);
		addFeature(cornerSquare(), featureCollection);

		for (int i = 0; i < size; i++) {
			addFeature(randomTriangle(), featureCollection);
		}

		Layer layer =
			context.addLayer(
				StandardCategoryNames.WORKING,
				I18N.getInstance().get("ui.test.RandomTriangle.random-triangles"),
				featureCollection);
		layer.setDescription("ABCDE");
	}

	private Geometry cornerSquare() throws ParseException {
		return wktReader.read(
			"POLYGON ((-50 -50, 50 -50, 50 50, -50 50, -50 -50))");
	}

	private void addFeature(
		Geometry geometry,
		FeatureCollection featureCollection) {
		Feature feature =
			new BasicFeature(featureCollection.getFeatureSchema());
		feature.setAttribute("Geometry", geometry);
		feature.setAttribute("City", cities.get((int) Math.floor(Math.random() * cities.size())));
		feature.setAttribute("A_Date", new Date());
		feature.setAttribute("B_Integer", (int)(Math.random() * 100000));
		feature.setAttribute("C_Double", Math.random() * 100000);
		feature.setAttribute("D_Long", (long)(Math.random() * 1000000000000L));
		feature.setAttribute("E_Boolean", Math.random() > 0.5);
		feature.setAttribute("F_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("G_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("H_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("I_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("J_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("K_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("L_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("M_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("N_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("O_Code", "" + (int)(Math.random() * 100000));
		feature.setAttribute("P_Code", "" + (int)(Math.random() * 100000));

        if (Math.random() > 0.8) {
            feature.setAttribute("E_Boolean", null);
        }
		if (Math.random() > 0.8) {
			feature.setAttribute("F_Code", null);
		}

		featureCollection.add(feature);
	}

	private Geometry randomTriangle() {
		int perturbation = 30;

		int x = (int) (Math.random() * 700);
		int y = (int) (Math.random() * 700);
		Coordinate firstPoint = perturbedPoint(x, y, perturbation);

		return geometryFactory.createPolygon(
			geometryFactory.createLinearRing(
				new Coordinate[] {
					firstPoint,
					perturbedPoint(x, y, perturbation),
					perturbedPoint(x, y, perturbation),
					firstPoint }),
			null);
	}

	private Coordinate perturbedPoint(int x, int y, int perturbation) {
		return new Coordinate(
			x + (Math.random() * perturbation),
			y + (Math.random() * perturbation));
	}

	public void setCities(List cities) {
		this.cities = cities;
	}

}
