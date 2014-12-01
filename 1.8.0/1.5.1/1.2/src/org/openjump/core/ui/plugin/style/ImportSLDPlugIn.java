//$HeadURL: https://sushibar/svn/deegree/base/trunk/resources/eclipse/svn_classfile_header_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2007 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.openjump.core.ui.plugin.style;

import static com.vividsolutions.jump.I18N.get;
import static com.vividsolutions.jump.workbench.ui.MenuNames.LAYER;
import static com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn.get;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import static org.apache.log4j.Logger.getLogger;
import static org.openjump.util.SLDImporter.importSLD;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.openjump.core.ccordsys.srid.SRIDStyle;
import org.w3c.dom.Document;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.util.Range.RangeTreeMap;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.SquareVertexStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

/**
 * <code>ImportSLDPlugIn</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class ImportSLDPlugIn extends AbstractPlugIn {

    private static final Logger LOG = getLogger(ImportSLDPlugIn.class);

    @Override
    public void initialize(PlugInContext context) throws Exception {
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory(context.getWorkbenchContext());

        EnableCheck enableCheck = new MultiEnableCheck().add(
                enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck()).add(
                enableCheckFactory.createExactlyNLayerablesMustBeSelectedCheck(1, Layerable.class));

        context.getFeatureInstaller().addMainMenuItem(this, new String[] { LAYER },
                get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.name"), false, null, enableCheck);
    }

    // avoiding redundant code with reflection...
    private void checkStyle(Class<? extends Style> c, Layer l) {
        if (l.getStyle(c) == null) {
            try {
                Style s = c.newInstance();
                s.setEnabled(false);
                l.addStyle(s);
            } catch (InstantiationException e) {
                // ignore
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        Blackboard bb = get(context.getWorkbenchContext());
        String fileName = (String) bb.get("ImportSLDPlugin.filename");

        JFileChooser chooser = new JFileChooser();
        if (fileName != null) {
            chooser.setCurrentDirectory(new File(fileName).getParentFile());
        }
        int res = chooser.showOpenDialog(context.getWorkbenchFrame());
        if (res == APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            bb.put("ImportSLDPlugin.filename", f.getAbsoluteFile().toString());
            Layer l = context.getSelectedLayer(0);
            DocumentBuilderFactory dbf = newInstance();
            dbf.setNamespaceAware(true);

            Document doc = dbf.newDocumentBuilder().parse(f);
            List<Style> styles = importSLD(doc);
            l.setStyles(styles);
            if (l.getStyle(SRIDStyle.class) == null) {
                l.addStyle(new SRIDStyle());
            }
            if (l.getStyle(ColorThemingStyle.class) == null) {
                ColorThemingStyle.get(l);
            }
            checkStyle(LabelStyle.class, l);
            checkStyle(BasicStyle.class, l);
            if (l.getVertexStyle() == null) {
                checkStyle(SquareVertexStyle.class, l);
            }

            ColorThemingStyle cts = (ColorThemingStyle) l.getStyle(ColorThemingStyle.class);
            if (cts.getDefaultStyle() == null) {
                cts.setDefaultStyle(l.getBasicStyle());
            }

            FeatureSchema fs = l.getFeatureCollectionWrapper().getFeatureSchema();

            String a = cts.getAttributeName();

            AttributeType t = fs.getAttributeType(a);
            Class<?> c = t.toJavaClass();

            try {
                if (cts.getAttributeValueToLabelMap().keySet().iterator().next() instanceof Range) {
                    RangeTreeMap map = new RangeTreeMap();
                    RangeTreeMap labelMap = new RangeTreeMap();

                    Map<?, ?> oldMap = cts.getAttributeValueToBasicStyleMap();
                    Map<?, ?> oldLabelMap = cts.getAttributeValueToLabelMap();

                    if (c.equals(Integer.class)) {
                        for (Object k : cts.getAttributeValueToBasicStyleMap().keySet()) {
                            Range r = (Range) k;
                            Range newRange = new Range(Integer.valueOf((String) r.getMin()), r.isIncludingMin(),
                                    Integer.valueOf((String) r.getMax()), r.isIncludingMax());
                            map.put(newRange, oldMap.get(r));
                            labelMap.put(newRange, oldLabelMap.get(r));
                        }
                    }

                    if (c.equals(Double.class)) {
                        for (Object k : cts.getAttributeValueToBasicStyleMap().keySet()) {
                            Range r = (Range) k;
                            Range newRange = new Range(Double.valueOf((String) r.getMin()), r.isIncludingMin(), Double
                                    .valueOf((String) r.getMax()), r.isIncludingMax());
                            map.put(newRange, oldMap.get(r));
                            labelMap.put(newRange, oldLabelMap.get(r));
                        }
                    }

                    cts.setAttributeValueToBasicStyleMap(map);
                    cts.setAttributeValueToLabelMap(labelMap);

                    return false;
                }
            } catch (Exception e) {
                LOG.debug("Unknown error: ", e);
                // ignore, probably no elements in the map
            }

            if (c.equals(Integer.class)) {
                Map<Integer, Style> map = new TreeMap<Integer, Style>();
                Map<?, ?> oldMap = cts.getAttributeValueToBasicStyleMap();
                Map<Integer, String> labelMap = new TreeMap<Integer, String>();
                for (Object key : oldMap.keySet()) {
                    Style s = (Style) oldMap.get(key);
                    map.put(Integer.valueOf((String) key), s);
                    labelMap.put(Integer.valueOf((String) key), (String) key);
                }
                cts.setAttributeValueToBasicStyleMap(map);
                cts.setAttributeValueToLabelMap(labelMap);
            }

            if (c.equals(Double.class)) {
                Map<Double, Style> map = new TreeMap<Double, Style>();
                Map<?, ?> oldMap = cts.getAttributeValueToBasicStyleMap();
                Map<Double, String> labelMap = new TreeMap<Double, String>();
                for (Object key : oldMap.keySet()) {
                    Style s = (Style) oldMap.get(key);
                    map.put(Double.valueOf((String) key), s);
                    labelMap.put(Double.valueOf((String) key), (String) key);
                }
                cts.setAttributeValueToBasicStyleMap(map);
                cts.setAttributeValueToLabelMap(labelMap);
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.name");
    }

}
