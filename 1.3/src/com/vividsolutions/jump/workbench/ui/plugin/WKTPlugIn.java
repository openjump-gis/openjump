
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

package com.vividsolutions.jump.workbench.ui.plugin;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.Iterator;

import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.WKTReader;
import com.vividsolutions.jump.workbench.WorkbenchException;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.EnterWKTDialog;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
/**
 *  Base class for PlugIns that ask the user to enter Well-Known Text.
 */
public abstract class WKTPlugIn extends AbstractPlugIn {
    protected Layer layer;
    public WKTPlugIn() {}
    private void validate(FeatureCollection c, PlugInContext context) throws WorkbenchException {
        for (Iterator i = c.iterator(); i.hasNext();) {
            Feature f = (Feature) i.next();
            IsValidOp op = new IsValidOp(f.getGeometry());
            if (!op.isValid()) {
                if (context
                    .getWorkbenchContext()
                    .getWorkbench()
                    .getBlackboard()
                    .get(EditTransaction.ROLLING_BACK_INVALID_EDITS_KEY, false)
                    ) {
                    throw new WorkbenchException(op.getValidationError().getMessage());
                }
                context.getWorkbenchFrame().warnUser(
                    op.getValidationError().getMessage());
            }
        }
    }
    protected abstract Layer layer(PlugInContext context);
    public boolean execute(PlugInContext context) throws Exception {
        layer = layer(context);
        EnterWKTDialog d = createDialog(context);
        d.setVisible(true);
        return d.wasOKPressed();
    }
    protected abstract void apply(FeatureCollection c, PlugInContext context)
        throws WorkbenchException;
    protected EnterWKTDialog createDialog(final PlugInContext context) {
        final EnterWKTDialog d =
            new EnterWKTDialog(context.getWorkbenchFrame(), I18N.get("ui.plugin.WKTPlugIn.enter-well-known-text"), true);
        d.setSize(500, 400);
        d.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (d.wasOKPressed()) {
                        apply(d.getText(), context);
                    }
                    d.setVisible(false);
                } catch (Throwable t) {
                    context.getErrorHandler().handleThrowable(t);
                }
            }
        });
        GUIUtil.centreOnWindow(d);
        return d;
    }
    protected void apply(String wkt, PlugInContext context) throws Exception {
        StringReader stringReader = new StringReader(wkt);
        try {
            WKTReader wktReader = new WKTReader();
            FeatureCollection c = wktReader.read(stringReader);
            validate(c, context);
            apply(c, context);
        } finally {
            stringReader.close();
        }
    }
}
