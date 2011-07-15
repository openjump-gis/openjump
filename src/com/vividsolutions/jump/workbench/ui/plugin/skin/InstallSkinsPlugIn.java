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

package com.vividsolutions.jump.workbench.ui.plugin.skin;

import java.util.ArrayList;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.OptionsDialog;
import javax.swing.UIManager.LookAndFeelInfo;

/**
* 
* Installs custom 'look and feel' for UI via 'Skins'.
* 
*/

public class InstallSkinsPlugIn extends AbstractPlugIn {
    
    private static String SKINS = I18N.get("ui.plugin.skin.InstallSkinsPlugIn.skins");
    private static String DEFAULT = I18N.get("ui.plugin.skin.InstallSkinsPlugIn.default");
    
    private LookAndFeelProxy createProxy(final String name,
        final String lookAndFeelClassName) {
        return new LookAndFeelProxy() {
                public LookAndFeel getLookAndFeel() {
                    try {
                        return (LookAndFeel) Class.forName(lookAndFeelClassName)
                                                  .newInstance();
                    } catch (InstantiationException e) {
                        Assert.shouldNeverReachHere(e.toString());
                    } catch (IllegalAccessException e) {
                        Assert.shouldNeverReachHere(e.toString());
                    } catch (ClassNotFoundException e) {
                        Assert.shouldNeverReachHere(e.toString());
                    }

                    return null;
                }

                public String toString() {
                    return name;
                }
            };
    }

    public void initialize(PlugInContext context) throws Exception {
        SKINS = I18N.get("ui.plugin.skin.InstallSkinsPlugIn.skins");
        DEFAULT = I18N.get("ui.plugin.skin.InstallSkinsPlugIn.default");
        ArrayList skins = new ArrayList();
        skins.add(createProxy(DEFAULT,
                UIManager.getSystemLookAndFeelClassName()));
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			skins.add(createProxy(info.getName(), info.getClassName()));
		}
        context.getWorkbenchContext().getWorkbench().getBlackboard().put(SkinOptionsPanel.SKINS_KEY,
            skins);
        OptionsDialog.instance(context.getWorkbenchContext().getWorkbench()).addTab(
            SKINS,
            new SkinOptionsPanel(context.getWorkbenchContext().getWorkbench().getBlackboard(), context.getWorkbenchFrame()));                                    
    }
}
