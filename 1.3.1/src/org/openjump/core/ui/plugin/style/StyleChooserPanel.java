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
import static javax.swing.BorderFactory.createEmptyBorder;
import static org.openjump.util.SLDImporter.getPossibleColorThemingStyleNames;
import static org.openjump.util.SLDImporter.getRuleNamesWithGeometrySymbolizers;
import static org.openjump.util.SLDImporter.getRuleNamesWithTextSymbolizers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openjump.core.ui.swing.SelectFromListPanel;
import org.w3c.dom.Document;

/**
 * <code>StyleChooserPanel</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class StyleChooserPanel extends JPanel implements ListSelectionListener {

    private static final long serialVersionUID = 7546547595382784628L;

    private Document doc;

    private SelectFromListPanel type;

    private SelectFromListPanel select;

    private static final String LABEL = get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Label-Styles");

    private static final String BASIC = get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Basic-Styles");

    private static final String THEMING = get("org.openjump.core.ui.plugin.style.ImportSLDPlugIn.Color-Theming-Styles");

    /**
     * @param doc
     */
    public StyleChooserPanel(Document doc) {
        this.doc = doc;
        type = new SelectFromListPanel("none");
        select = new SelectFromListPanel("none");
        type.list.addListSelectionListener(this);
        Vector<String> types = new Vector<String>();
        types.add(BASIC);
        types.add(LABEL);
        types.add(THEMING);

        type.list.setListData(types);

        setLayout(new GridBagLayout());
        setBorder(createEmptyBorder(2, 2, 2, 2));
        GridBagConstraints gb = new GridBagConstraints();
        gb.insets = new Insets(2, 2, 2, 2);
        gb.gridx = 0;
        gb.gridy = 0;

        add(type, gb);
        ++gb.gridx;
        add(select, gb);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (type.list.getSelectedValue().equals(LABEL)) {
            select.list.setListData(new Vector<String>(getRuleNamesWithTextSymbolizers(doc)));
        }
        if (type.list.getSelectedValue().equals(BASIC)) {
            select.list.setListData(new Vector<String>(getRuleNamesWithGeometrySymbolizers(doc)));
        }
        if (type.list.getSelectedValue().equals(THEMING)) {
            select.list.setListData(new Vector<String>(getPossibleColorThemingStyleNames(doc)));
        }
    }

    /**
     * @return the selected String or null, if none was selected
     */
    public String getSelectedStyle() {
        return (String) select.list.getSelectedValue();
    }

}
