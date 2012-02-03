/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) 2008 Integrated Systems Analysts, Inc.
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
 * Integrated Systems Analysts, Inc.
 * 630C Anchors St., Suite 101
 * Fort Walton Beach, Florida
 * USA
 *
 * (850)862-7321
 * www.ashs.isa.com
 */

package org.openjump.core.ui.plugin.queries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.SelectionManager;

public class SearchAllAttributes extends AbstractPlugIn
{
	private final static String SEARCHFOR = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.search-for"); 
	private final static String SEARCHALLATTRIBUTES = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.search-all-attributes");
	private final static String INCLUDEGEOMETRY = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.include-geometry");
	private final static String MATCHOR = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.match-any-search-word");
	private final static String MATCHAND = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.match-all-search-words");
	private final static String MATCHHINT = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.all-search-words-must-be-ina-single-attribute");
	private final static String CASESENSITIVE = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.case-sensitive");
	private final static String WHOLEWORD = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.whole-word");
	private final static String SIDEBARTEXT = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.search-for-text-in-any-attribute");
	private final static String REGULAREXPRESSIONS = I18N.get("org.openjump.core.ui.plugin.queries.SearchAllAttributes.regular-expressions"); 
//		+" and select matching map objects.\n\n"
//		+"Uses Java Pattern matcher which supports:\n"
//		+". - Match any character\n"
//		+"^ - Beginning of a line\n"
//		+"$ - End of a line\n"
//		+"and others.  See the Java documentation.";

	private boolean includeGeometry = false;
	private String searchString = "";
	private int patternCaseOption = Pattern.CASE_INSENSITIVE;
	private boolean wholeWord = false;
	private boolean regularExpressions = false;
	
		
	private boolean multiWordMatchAnd = true;

	public String getName() { return SEARCHALLATTRIBUTES; };

	public void initialize(PlugInContext context) throws Exception
	{
		context.getFeatureInstaller().addMainMenuItem(this,
				new String[] { MenuNames.TOOLS,MenuNames.TOOLS_QUERIES}, getName()+"...", false, null, 
				null);
	}

	public boolean execute(final PlugInContext context) throws Exception
	{
		includeGeometry = false;
		reportNothingToUndoYet(context);
		MultiInputDialog dialog = new MultiInputDialog(
				context.getWorkbenchFrame(), getName(), true);
		setDialogValues(dialog, context);
		GUIUtil.centreOnWindow(dialog);
		dialog.setVisible(true);
		if (! dialog.wasOKPressed()) { return false; }
		getDialogValues(dialog);
		searchInAttributes(context, searchString);
		return true;
	}


	private void setDialogValues(MultiInputDialog dialog, PlugInContext context)
	{
		dialog.setSideBarDescription(SIDEBARTEXT);
		dialog.addTextField(SEARCHFOR, searchString, 16, null, "Search target");
		dialog.addCheckBox(CASESENSITIVE, (patternCaseOption == 0));  
		dialog.addCheckBox(WHOLEWORD, wholeWord);  
		//dialog.addSeparator();
		dialog.addRadioButton(MATCHAND, "MatchOptions", multiWordMatchAnd, MATCHHINT);
		dialog.addRadioButton(MATCHOR, "MatchOptions", !multiWordMatchAnd, MATCHHINT);
		dialog.addCheckBox(INCLUDEGEOMETRY, includeGeometry);  
		dialog.addCheckBox(REGULAREXPRESSIONS, regularExpressions);  
	}


	private void getDialogValues(MultiInputDialog dialog) 
	{
		searchString = dialog.getText(SEARCHFOR);
		wholeWord = dialog.getCheckBox(WHOLEWORD).isSelected();
		multiWordMatchAnd = dialog.getRadioButton(MATCHAND).isSelected();
		if (dialog.getCheckBox(CASESENSITIVE).isSelected())
			patternCaseOption = 0;
		else
			patternCaseOption = Pattern.CASE_INSENSITIVE;
		includeGeometry = dialog.getCheckBox(INCLUDEGEOMETRY).isSelected();
		regularExpressions = dialog.getCheckBox(REGULAREXPRESSIONS).isSelected();
	}


//	public void run(TaskMonitor monitor, PlugInContext context)
//	throws Exception
//	{
//		searchInAttributes(context, searchString);
//
//	}

	private void searchInAttributes(PlugInContext context, String searchString) {
		SelectionManager selectionManager = context.getLayerViewPanel().getSelectionManager();
		LayerManager layerManager = context.getLayerManager();
		String[] searchStrings = searchString.split(" ");
		int nwords = searchStrings.length;
		Pattern[] patterns = new Pattern[nwords];
		String quote = "\\Q";
		String endQuote = "\\E";
		if (regularExpressions) {
			quote = "";
			endQuote = "";
		}			
		for (int k=0; k<nwords; k++) {
			String regex;
			if (wholeWord)
				regex = "\\b"+quote+searchStrings[k]+endQuote+"\\b";
			else
				regex = quote+searchStrings[k]+endQuote;
			patterns[k] = Pattern.compile(regex,patternCaseOption);
		}
		ArrayList layerList = new ArrayList(layerManager.getVisibleLayers(false));         		
		for (Iterator j = layerList.iterator(); j.hasNext();) {
			Layer layer = (Layer) j.next();
			HashSet<Feature> selectedFeatures = new HashSet<Feature>();
			for (Iterator iter = layer.getFeatureCollectionWrapper().iterator(); iter.hasNext();) {
				Feature f = (Feature) iter.next();
				String attribString = null;
				int n = f.getAttributes().length;
				for (int i=0; i<n; i++) {
					Object attribute = (Object) f.getAttribute(i);
					if (!includeGeometry && attribute instanceof Geometry) 
						continue;
					try {
						attribString = attribute.toString();
						boolean select;
						if (multiWordMatchAnd)
							select = true;
						else
							select = false;
						for (int k = 0; k < nwords; k++) {
							patterns[k].matcher(attribString).reset();
							if (multiWordMatchAnd) {
								select = select
										&& (patterns[k].matcher(attribString)
												.find());
							} else {
								select = select
										|| (patterns[k].matcher(attribString)
												.find());
							}
						}
						if (select) {
							selectedFeatures.add(f);
						}
					} catch (NullPointerException ex) {};
				}
			}
			if (selectedFeatures.size() > 0)
				selectionManager.getFeatureSelection().selectItems(layer, selectedFeatures);
		}
	}


}
