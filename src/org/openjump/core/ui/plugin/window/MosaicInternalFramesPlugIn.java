/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Micha&euml;l Michaud.
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
 * Michael Michaud
 * michael.michaud@free.fr
 */

package org.openjump.core.ui.plugin.window;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import org.openjump.core.ui.images.IconLoader;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;
import org.openjump.core.ui.swing.DetachableInternalFrame;

/**
 * A plugin to layout opened internal frames as a mosaic.
 * 
 * @author Michael Michaud
 * @version 0.1 (2008-04-06)
 * @since 1.2F
 */
public class MosaicInternalFramesPlugIn extends AbstractUiPlugIn {
    
    public static final ImageIcon ICON = IconLoader.icon("application_mosaic.png");

    public MosaicInternalFramesPlugIn() {
        super(I18N.getInstance().get("org.openjump.core.ui.plugin.window.MosaicInternalFramesPlugIn.Mosaic"),
            ICON);
    }

    @Override
    public void initialize(PlugInContext context) throws Exception {

        super.initialize(context);

        final JMenuItem jmi = context.getFeatureInstaller().addMainMenuItem(
            new String[]{MenuNames.WINDOW}, this, Integer.MAX_VALUE);

    }

    @Override
    public EnableCheck getEnableCheck() {
        EnableCheckFactory checkFactory = EnableCheckFactory.getInstance(workbenchContext);
        return new MultiEnableCheck()
            .add(checkFactory.createWindowWithLayerViewPanelMustBeActiveCheck());
    }

    @Override
	public boolean execute(PlugInContext context) throws Exception{

        // we only provide non detached internal frames for mosaic'ing
        ArrayList<JInternalFrame> iframeList = new ArrayList<JInternalFrame>();
        for (JInternalFrame iframe : context.getWorkbenchFrame().getInternalFrames()) {
            if (!(iframe instanceof DetachableInternalFrame && ((DetachableInternalFrame)iframe).isDetached())) {
                iframeList.add(iframe);
            }
        }
        
        JInternalFrame[] iframes = new JInternalFrame[0];
        iframes = iframeList.toArray(iframes);
        // number of opened internal frames 
        int nbFrames = iframes.length;
        int n = nbFrames;
        for( int i = 0 ; i < nbFrames; ++i) {
            if(iframes[i].isIcon()) --n;
        }
        // give some place for iconified internal frames
        // the necessary place may vary with the look and field
        // 30 will let a white space for window look and field
        int iconified_frame_strip = 0;
        if(n != nbFrames) {
            iconified_frame_strip = 30;
        }
        // compute column number
        if(n == 0) return true;
        int nColumns = (int)Math.sqrt(n), nLines; 
        if(n != nColumns*nColumns) {
            ++nColumns;
        }
        // compute line number
        if((n-1)/nColumns+1 < nColumns) {
            nLines = nColumns-1;
        }
        else nLines = nColumns;
        int dx = context.getWorkbenchFrame().getDesktopPane().getWidth()/nColumns;
        int dy = context.getWorkbenchFrame().getDesktopPane().getHeight()/nLines - iconified_frame_strip;
        int k = 0;
        for( int i = 0 ; i < nColumns; ++i) {
            for( int j = 0; j < nColumns && k < n; ++j, ++k) {
                iframes[i*nColumns+j].setBounds(j*dx, i*dy, dx, dy);
            }
        }

        return true;

    }

}
