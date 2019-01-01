package com.vividsolutions.jump.workbench.ui.plugin.test;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;

public class TestColorThemingPlugIn extends AbstractPlugIn {

	public void initialize(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addMainMenuPlugin(
			this,
			new String[] { "Tools", "Test" },
			getName(),
			false,
			null,
			null);
		randomTrianglesPlugIn.setCities(cities);
	}

	private List<String> cities =
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
				"Hawaii");

	private RandomTrianglesPlugIn randomTrianglesPlugIn =
		new RandomTrianglesPlugIn();

	public boolean execute(PlugInContext context) throws Exception {
		ArrayList<String> names = new ArrayList<>();
        names.addAll(ColorScheme.discreteColorSchemeNames());
        names.addAll(ColorScheme.rangeColorSchemeNames());
		Collections.reverse(names);
		for (String colorScheme : names) {
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
		Map<Object,BasicStyle> attributeToStyleMap = new HashMap<>();
		for (String city : cities) {
			attributeToStyleMap.put(city, new BasicStyle(colorScheme.next()));
		}

		layer.getBasicStyle().setEnabled(false);
		ColorThemingStyle themeStyle = new ColorThemingStyle("City", attributeToStyleMap, new BasicStyle(Color.gray));
		themeStyle.setEnabled(true);
		layer.addStyle(themeStyle);
		layer.setVisible(false);
	}

}
