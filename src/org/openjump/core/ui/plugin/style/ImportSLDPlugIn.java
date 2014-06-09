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
import static com.vividsolutions.jump.I18N.getMessage;
import static com.vividsolutions.jump.workbench.ui.MenuNames.LAYER;
import static com.vividsolutions.jump.workbench.ui.MenuNames.STYLE;
import static com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn.get;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import static org.apache.log4j.Logger.getLogger;
import static org.openjump.util.SLDImporter.getBasicStyle;
import static org.openjump.util.SLDImporter.getColorThemingStyle;
import static org.openjump.util.SLDImporter.getLabelStyle;
import static org.openjump.util.SLDImporter.getRuleNames;
import static org.openjump.util.SLDImporter.getVertexStyle;

import java.awt.Component;
import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.swing.SelectFromListPanel;
import org.w3c.dom.Document;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.Range.RangeTreeMap;
import com.vividsolutions.jump.util.Range;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog.Validator;
import com.vividsolutions.jump.workbench.ui.OKCancelDialog;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;

/**
 * <code>ImportSLDPlugIn</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class ImportSLDPlugIn extends AbstractPlugIn {

    private static Logger LOG = getLogger(ImportSLDPlugIn.class);
    
    public static final ImageIcon ICON = IconLoader.icon("sld_in_16.png");

    @Override
    public void initialize(PlugInContext context) throws Exception {
        FeatureInstaller featureInstaller =
            new FeatureInstaller(context.getWorkbenchContext());
	    EnableCheck enableCheck =
	        createEnableCheck(context.getWorkbenchContext());
	    JPopupMenu popupMenu =
	        context.getWorkbenchFrame().getLayerNamePopupMenu();
	    featureInstaller.addPopupMenuItem(popupMenu, this, new String[]{STYLE},
		    this.getName(), false, ICON, enableCheck);
        featureInstaller.addMainMenuItem(this, new String[] {LAYER},
            get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.name"),
            false, ICON, enableCheck);
    }

    private static String fixAttribute(Layer l, WorkbenchFrame frame, String old) {
        FeatureSchema fs = l.getFeatureCollectionWrapper().getFeatureSchema();
        if (!fs.hasAttribute(old)) {
            if (old.indexOf(':') != -1) {
                old = old.substring(old.indexOf(':'));
            }
            if (!fs.hasAttribute(old)) {
                old = old.toUpperCase();
            }
        }
        return chooseAttribute(l, old, frame);
    }

    private static void setStyles(Layer l, BasicStyle bs, VertexStyle vs, LabelStyle ls, ColorThemingStyle cts,
            WorkbenchFrame frame) {
        if (bs != null) {
            bs.setEnabled(true);
            l.removeStyle(l.getBasicStyle());
            l.addStyle(bs);
        }

        if (vs != null) {
            vs.setEnabled(true);
            l.removeStyle(l.getVertexStyle());
            l.addStyle(vs);
        }

        if (ls != null) {
            ls.setAttribute(fixAttribute(l, frame, ls.getAttribute()));
            ls.setEnabled(true);
            l.removeStyle(l.getLabelStyle());
            l.addStyle(ls);
        }

        if (cts != null) {
            cts.setAttributeName(fixAttribute(l, frame, cts.getAttributeName()));
            try {
                fixColorThemingAttributeMap(l, cts, frame);
                cts.setDefaultStyle((BasicStyle) cts.getAttributeValueToBasicStyleMap().values().iterator().next());
                cts.setEnabled(true);
                l.removeStyle(l.getStyle(ColorThemingStyle.class));
                l.addStyle(cts);
            } catch (NumberFormatException e) {
                showMessageDialog(frame, getMessage(
                        "org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Number-Error-reading-styles",
                        new Object[] { e.getLocalizedMessage() }), get("com.vividsolutions.wms.WMService.Error"),
                        ERROR_MESSAGE);
            }
        }
    }

    // this method contains the hacks to fix the color theming styles
    private static void fixColorThemingAttributeMap(Layer l, ColorThemingStyle cts, WorkbenchFrame frame) {
        FeatureSchema fs = l.getFeatureCollectionWrapper().getFeatureSchema();

        String a = cts.getAttributeName();

        try {
            AttributeType t = fs.getAttributeType(a);
            Class<?> c = t.toJavaClass();

            try {
                if (cts.getAttributeValueToLabelMap().keySet().iterator().next() instanceof Range) {
                    LOG.debug("Color theming values are ranges.");
                    //RangeTreeMap<Range, BasicStyle> map = new RangeTreeMap<Range, BasicStyle>();
                    //RangeTreeMap<Range, String> labelMap = new RangeTreeMap<Range, String>();
                    RangeTreeMap<Object, BasicStyle> map = new RangeTreeMap<Object, BasicStyle>();
                    RangeTreeMap<Object, String> labelMap = new RangeTreeMap<Object, String>();

                    //Map<Range, BasicStyle> oldMap = cts.getAttributeValueToBasicStyleMap();
                    //Map<Range, String> oldLabelMap = cts.getAttributeValueToLabelMap();
                    Map<Object, BasicStyle> oldMap = cts.getAttributeValueToBasicStyleMap();
                    Map<Object, String> oldLabelMap = cts.getAttributeValueToLabelMap();

                    if (c.equals(Integer.class)) {
                        LOG.debug("Color theming values are ranges of integers.");
                        for (Object k : cts.getAttributeValueToBasicStyleMap().keySet()) {
                            Range r = (Range) k;
                            Range newRange = new Range(Integer.valueOf((String) r.getMin()), r.isIncludingMin(),
                                    Integer.valueOf((String) r.getMax()), r.isIncludingMax());
                            map.put(newRange, oldMap.get(r));
                            labelMap.put(newRange, oldLabelMap.get(r));
                        }
                    }

                    if (c.equals(Double.class)) {
                        LOG.debug("Color theming values are ranges of doubles.");
                        for (Object k : cts.getAttributeValueToBasicStyleMap().keySet()) {
                            Range r = (Range) k;
                            Range newRange = new Range(Double.valueOf((String) r.getMin()), r.isIncludingMin(), Double
                                    .valueOf((String) r.getMax()), r.isIncludingMax());
                            map.put(newRange, oldMap.get(r));
                            labelMap.put(newRange, oldLabelMap.get(r));
                        }
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Color theming values are ranges of java class " + c + ".");
                    }

                    cts.setAttributeValueToBasicStyleMap(map);
                    cts.setAttributeValueToLabelMap(labelMap);

                    return;
                }
            } catch (NumberFormatException e) {
                throw e;
            } catch (Exception e) {
                LOG.debug("Unknown error: ", e);
                // ignore, probably no elements in the map
                return;
            }

            if (c.equals(Integer.class)) {
                LOG.debug("Color theming values are integers.");
                Map<Object, BasicStyle> map = new TreeMap<Object, BasicStyle>();
                Map<?, ?> oldMap = cts.getAttributeValueToBasicStyleMap();
                Map<Object, String> labelMap = new TreeMap<Object, String>();
                for (Object key : oldMap.keySet()) {
                    BasicStyle s = (BasicStyle) oldMap.get(key);
                    map.put(Integer.valueOf((String) key), s);
                    labelMap.put(Integer.valueOf((String) key), (String) key);
                }
                cts.setAttributeValueToBasicStyleMap(map);
                cts.setAttributeValueToLabelMap(labelMap);
            }

            if (c.equals(Double.class)) {
                LOG.debug("Color theming values are doubles.");
                Map<Object, BasicStyle> map = new TreeMap<Object, BasicStyle>();
                Map<?, ?> oldMap = cts.getAttributeValueToBasicStyleMap();
                Map<Object, String> labelMap = new TreeMap<Object, String>();
                for (Object key : oldMap.keySet()) {
                    BasicStyle s = (BasicStyle) oldMap.get(key);
                    map.put(Double.valueOf((String) key), s);
                    labelMap.put(Double.valueOf((String) key), (String) key);
                }
                cts.setAttributeValueToBasicStyleMap(map);
                cts.setAttributeValueToLabelMap(labelMap);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Color theming values are of java type " + c + ".");
            }

            return;
        } catch (NumberFormatException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            showMessageDialog(frame, getMessage(
                    "org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Error-reading-styles", new Object[] { e
                            .getLocalizedMessage() }), get("com.vividsolutions.wms.WMService.Error"), ERROR_MESSAGE);
            LOG.debug("Probably unknown attribute name: ", e);
            return;
        }
    }

    private static String chooseAttribute(Layer l, String def, WorkbenchFrame frame) {
        final SelectFromListPanel panel = new SelectFromListPanel("none");
        Vector<String> list = new Vector<String>();

        FeatureSchema fs = l.getFeatureCollectionWrapper().getFeatureSchema();
        for (int i = 0; i < fs.getAttributeCount(); ++i) {
            list.add(fs.getAttributeName(i));
        }

        if (list.size() == 1) {
            return list.firstElement();
        }

        panel.list.setListData(list);
        if (list.contains(def)) {
            panel.list.setSelectedValue(def, true);
        }

        OKCancelDialog dlg = new OKCancelDialog(frame,
                get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Select-Attribute"), true, panel,
                new Validator() {
                    public String validateInput(Component component) {
                        return panel.list.getSelectedValue() == null ? get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Must-Select-Attribute")
                                : null;
                    }
                });

        dlg.setVisible(true);

        return dlg.wasOKPressed() ? (String) panel.list.getSelectedValue() : null;
    }

    /**
     * Executes the plugin part once you have a SLD document and a PlugIn
     * context. It can be called from other plugins that aquire SLD from
     * elsewhere.
     * 
     * @param doc
     * @param context
     */
    public static void importSLD(Document doc, PlugInContext context) {
        Layer l = context.getSelectedLayer(0);
        LinkedList<String> rules = getRuleNames(doc);

        if (rules.isEmpty()) {
            showMessageDialog(context.getWorkbenchFrame(),
                    get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.No-Styles-Found"),
                    get("com.vividsolutions.wms.WMService.Error"), INFORMATION_MESSAGE);
            return;
        }

        if (rules.size() == 1) {
            setStyles(l, getBasicStyle(rules.peek(), doc), getVertexStyle(rules.peek(), doc), getLabelStyle(rules
                    .peek(), doc), getColorThemingStyle(rules.peek(), doc), context.getWorkbenchFrame());
            return;
        }

        OKCancelDialog dlg;
        do {
            final StyleChooserPanel panel = new StyleChooserPanel(doc);

            dlg = new OKCancelDialog(context.getWorkbenchFrame(),
                    get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Choose-Style"), true, panel,
                    new Validator() {
                        public String validateInput(Component component) {
                            return panel.getSelectedStyle() == null ? get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Must-Select-Style")
                                    : null;
                        }
                    });

            dlg.setVisible(true);

            if (dlg.wasOKPressed()) {
                setStyles(l, getBasicStyle(panel.getSelectedStyle(), doc),
                        getVertexStyle(panel.getSelectedStyle(), doc), getLabelStyle(panel.getSelectedStyle(), doc),
                        getColorThemingStyle(panel.getSelectedStyle(), doc), context.getWorkbenchFrame());
            }
        } while (dlg.wasOKPressed()
                && showConfirmDialog(context.getWorkbenchFrame(),
                        get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Select-Another-Style"),
                        get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Question"), YES_NO_OPTION) == YES_OPTION);
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
            DocumentBuilderFactory dbf = newInstance();
            dbf.setNamespaceAware(true);

            Document doc = dbf.newDocumentBuilder().parse(f);

            importSLD(doc, context);
        }

        return false;
    }

    @Override
    public String getName() {
        return get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.name");
    }
    
    /**
     * @param workbenchContext
     * @return the enable check
     */
    public EnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory ecf = new EnableCheckFactory(workbenchContext);
        MultiEnableCheck mec = new MultiEnableCheck()
            .add(ecf.createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(ecf.createExactlyNLayerablesMustBeSelectedCheck(1, Layerable.class));
        return mec;
    }

}
