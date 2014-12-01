//$HeadURL$
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
import static java.io.File.createTempFile;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.xml.parsers.DocumentBuilderFactory.newInstance;
import static org.openjump.core.ui.plugin.style.ImportSLDPlugIn.importSLD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.model.Layerable;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;

/**
 * <code>ImportArcMapStylePlugIn</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class ImportArcMapStylePlugIn extends AbstractPlugIn {

    @Override
    public void initialize(PlugInContext context) throws Exception {
        EnableCheckFactory enableCheckFactory = new EnableCheckFactory(context.getWorkbenchContext());

        EnableCheck enableCheck = new MultiEnableCheck().add(
                enableCheckFactory.createWindowWithLayerManagerMustBeActiveCheck()).add(
                enableCheckFactory.createExactlyNLayerablesMustBeSelectedCheck(1, Layerable.class));

        context.getFeatureInstaller().addMainMenuItem(this, new String[] { LAYER },
                get("org.openjump.core.ui.plugin.style.ImportArcMapStylePlugIn.name"), false, null, enableCheck);
    }

    private static File findArcMap2SLD(WorkbenchFrame wbframe, Blackboard bb) throws IOException, InterruptedException {
        String arcmap2sld = (String) bb.get("ArcMapStylePlugin.toollocation");
        if (arcmap2sld == null) {
            File tmp = createTempFile("amtsldreg", null);
            ProcessBuilder pb = new ProcessBuilder("regedit", "/e", tmp.toString(),
                    "HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion");
            pb.start().waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tmp), "UTF-16"));
            String s;
            while ((s = in.readLine()) != null) {
                if (s.startsWith("\"ProgramFilesDir\"=\"")) {
                    s = s.split("=")[1];
                    s = s.substring(1, s.length() - 1);
                    arcmap2sld = s + "\\i3mainz\\ArcMap2SLD_Full_Setup\\ArcGIS_SLD_Converter.exe";
                    break;
                }
            }
            in.close();
            tmp.delete();
        }

        JFileChooser chooser = new JFileChooser();

        File am2sld = arcmap2sld == null ? null : new File(arcmap2sld);
        if (am2sld == null || !am2sld.exists()) {
            showMessageDialog(wbframe,
                    get("org.openjump.core.ui.plugin.style.ImportArcMapStylePlugIn.Must-Select-Location-Of-Tool"),
                    get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Question"), INFORMATION_MESSAGE);
            if (arcmap2sld != null) {
                chooser.setSelectedFile(new File(arcmap2sld));
            }

            int res = chooser.showOpenDialog(wbframe);
            if (res == APPROVE_OPTION) {
                am2sld = chooser.getSelectedFile();
                if (!am2sld.exists()) {
                    return null;
                }
                bb.put("ArcMapStylePlugin.toollocation", am2sld.getAbsoluteFile().toString());
            } else {
                return null;
            }
        }

        return am2sld;
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        Blackboard bb = get(context.getWorkbenchContext());
        WorkbenchFrame wbframe = context.getWorkbenchFrame();

        String fileName = (String) bb.get("ArcMapStylePlugin.filename");

        File am2sld = findArcMap2SLD(wbframe, bb);
        if (am2sld == null) {
            return false;
        }

        ProcessBuilder pb = new ProcessBuilder(am2sld.toString());
        pb.start().waitFor(); // unfortunately, the code seems to always be
        // zero

        showMessageDialog(wbframe,
                get("org.openjump.core.ui.plugin.style.ImportArcMapStylePlugIn.Must-Select-Location-Of-SLD"),
                get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Question"), INFORMATION_MESSAGE);

        JFileChooser chooser = new JFileChooser();

        if (fileName != null) {
            chooser.setCurrentDirectory(new File(fileName).getParentFile());
        }

        int res = chooser.showOpenDialog(context.getWorkbenchFrame());
        if (res == APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.exists()) {
                return false;
            }
            bb.put("ArcMapStylePlugin.filename", f.getAbsoluteFile().toString());

            DocumentBuilderFactory dbf = newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(f);

            importSLD(doc, context);
        }

        return false;
    }

    @Override
    public String getName() {
        return get("org.openjump.core.ui.plugin.style.ImportArcMapStylePlugIn.name");
    }

}
