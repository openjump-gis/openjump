package com.vividsolutions.jump.workbench.ui.plugin.test;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

public class TestColorThemingPlugIn extends AbstractPlugIn {

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuItem(
			this,
			new String[] { "Tools", "Test" },
			getName(),
			false,
			null,
			null);
		randomTrianglesPlugIn.setCities(cities);
	}

	private List cities =
		Arrays.asList(
			new String[] {
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
				"Hawaii" });

	private RandomTrianglesPlugIn randomTrianglesPlugIn =
		new RandomTrianglesPlugIn();

	public boolean execute(PlugInContext context) throws Exception {
		ArrayList names = new ArrayList();
        names.addAll(ColorScheme.discreteColorSchemeNames());
        names.addAll(ColorScheme.rangeColorSchemeNames());
		Collections.reverse(names);
		for (Iterator i = names.iterator(); i.hasNext();) {
			String colorScheme = (String) i.next();
			execute(context, colorScheme);
		}
		return true;
	}

	private void execute(PlugInContext context, String colorSchemeName)
		throws ParseException, IOException {
		randomTrianglesPlugIn.execute(context, 500);
		Layer layer = context.getLayerManager().getLayer("Random Triangles");
		ColorScheme colorScheme = ColorScheme.create(colorSchemeName);
		layer.setName("(" + colorScheme.getColors().size() + ") " + colorSchemeName);
		Map<Object,BasicStyle> attributeToStyleMap = new HashMap<Object,BasicStyle>();
		for (Iterator i = cities.iterator(); i.hasNext(); ) {
			String city = (String) i.next();
			attributeToStyleMap.put(city, new BasicStyle(colorScheme.next()));
		}

		layer.getBasicStyle().setEnabled(false);
		ColorThemingStyle themeStyle = new ColorThemingStyle("City", attributeToStyleMap, new BasicStyle(Color.gray));
		themeStyle.setEnabled(true);
		layer.addStyle(themeStyle);
		layer.setVisible(false);
	}

}
