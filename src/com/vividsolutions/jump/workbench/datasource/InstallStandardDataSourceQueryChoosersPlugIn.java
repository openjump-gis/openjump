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
package com.vividsolutions.jump.workbench.datasource;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.io.*;
import com.vividsolutions.jump.io.datasource.DataSource;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;
import com.vividsolutions.jump.io.geojson.GeoJSONReader;
import com.vividsolutions.jump.io.geojson.GeoJSONWriter;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import java.awt.Component;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFileChooser;

import org.openjump.core.ui.DatasetOptionsPanel;
import org.openjump.core.ui.swing.ComboBoxComponentPanel;
import org.openjump.core.ui.swing.factory.field.ComboBoxFieldComponentFactory;
import org.openjump.swing.factory.field.FieldComponentFactory;

/**
 * Adds to the JUMP Workbench the UIs for opening and saving files with the
 * basic file formats.
 */
public class InstallStandardDataSourceQueryChoosersPlugIn extends
    AbstractPlugIn {

  private void addFileDataSourceQueryChoosers(JUMPReader reader,
      JUMPWriter writer, final String description,
      final WorkbenchContext context, Class readerWriterDataSourceClass) {

    DataSourceQueryChooserManager chooserManager = DataSourceQueryChooserManager
        .get(context.getBlackboard());

    chooserManager
        .addLoadDataSourceQueryChooser(new LoadFileDataSourceQueryChooser(
            readerWriterDataSourceClass, description,
            extensions(readerWriterDataSourceClass), context) {
          protected void addFileFilters(JFileChooser chooser) {
            super.addFileFilters(chooser);
            InstallStandardDataSourceQueryChoosersPlugIn
                .addCompressedFileFilter(description, chooser);
          }
        });

    // quick fix to not register readers only
    if (writer != null) {
      if (readerWriterDataSourceClass != StandardReaderWriterFileDataSource.Shapefile.class) {
        chooserManager
            .addSaveDataSourceQueryChooser(new SaveFileDataSourceQueryChooser(
                readerWriterDataSourceClass, description,
                extensions(readerWriterDataSourceClass), context));
      } else {
        // if we write ESRI Shapefiles, we add an option for the Charset
        chooserManager
            .addSaveDataSourceQueryChooser(new SaveFileDataSourceQueryChooser(
                readerWriterDataSourceClass, description,
                extensions(readerWriterDataSourceClass), context) {

              private JComponent comboboxFieldComponent;

              protected Map<String,Object> toProperties(File file) {
                HashMap<String,Object> properties = new HashMap<>(super.toProperties(file));
                String charsetName = Charset.defaultCharset().name();
                if (comboboxFieldComponent instanceof ComboBoxComponentPanel) {
                  charsetName = (String) ((ComboBoxComponentPanel) comboboxFieldComponent)
                      .getSelectedItem();
                }
                properties.put(DataSource.CHARSET_KEY, charsetName);

                return properties;
              }

              protected Component getSouthComponent1() {
                boolean showCharsetSelection = false;
                Object showCharsetSelectionObject = PersistentBlackboardPlugIn
                    .get(context.getBlackboard())
                    .get(DatasetOptionsPanel.BB_DATASET_OPTIONS_SHOW_CHARSET_SELECTION);
                if (showCharsetSelectionObject instanceof Boolean) {
                  showCharsetSelection = ((Boolean) showCharsetSelectionObject)
                      .booleanValue();
                }
                if (showCharsetSelection) {
                  FieldComponentFactory fieldComponentFactory = new ComboBoxFieldComponentFactory(
                      context,
                      I18N.get("org.openjump.core.ui.io.file.DataSourceFileLayerLoader.charset")
                          + ":", Charset.availableCharsets().keySet().toArray());
                  comboboxFieldComponent = fieldComponentFactory
                      .createComponent();
                  fieldComponentFactory.setValue(comboboxFieldComponent,
                      Charset.defaultCharset().name());
                  return comboboxFieldComponent;
                } else {
                  return new Component() {
                  };
                }
              }
            });
      }
    }
  }

  public static String[] extensions(Class readerWriterDataSourceClass) {
    String[] exts = null;

    try {
      exts = ((StandardReaderWriterFileDataSource) readerWriterDataSourceClass
          .newInstance()).getExtensions();
    } catch (Exception e) {
      Assert.shouldNeverReachHere(e.toString());
    }

    return exts;
  }

  public void initialize(final PlugInContext context) throws Exception {
    addFileDataSourceQueryChoosers(new JMLReader(), new JMLWriter(),
        "JUMP GML", context.getWorkbenchContext(),
        StandardReaderWriterFileDataSource.JML.class);

    new GMLDataSourceQueryChooserInstaller()
        .addLoadGMLFileDataSourceQueryChooser(context);
    new GMLDataSourceQueryChooserInstaller()
        .addSaveGMLFileDataSourceQueryChooser(context);

    addFileDataSourceQueryChoosers(new FMEGMLReader(), new FMEGMLWriter(),
        "FME GML", context.getWorkbenchContext(),
        StandardReaderWriterFileDataSource.FMEGML.class);

    addFileDataSourceQueryChoosers(new WKTReader(), new WKTWriter(), "WKT",
        context.getWorkbenchContext(),
        StandardReaderWriterFileDataSource.WKT.class);

    addFileDataSourceQueryChoosers(new ShapefileReader(),
        new ShapefileWriter(), "ESRI Shapefile", context.getWorkbenchContext(),
        StandardReaderWriterFileDataSource.Shapefile.class);

    addFileDataSourceQueryChoosers(new GeoJSONReader(), new GeoJSONWriter(),
        "GeoJSON", context.getWorkbenchContext(),
        StandardReaderWriterFileDataSource.GeoJSON.class);
  }

  // Should be public (used in some external plugins)
  public static void addCompressedFileFilter(final String description, JFileChooser chooser) {
    chooser
        .addChoosableFileFilter(GUIUtil.createFileFilter(
            I18N.get("datasource.InstallStandardDataSourceQueryChoosersPlugIn.compressed")
                + " " + description, new String[] { "zip", "gz" }));
  }
}
