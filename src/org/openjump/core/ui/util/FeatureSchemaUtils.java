package org.openjump.core.ui.util;

import it.betastudio.adbtoolbox.libs.FileOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JFileChooser;

import org.openjump.core.ui.io.file.FileNameExtensionFilter;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerEventType;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class FeatureSchemaUtils {

    // [Giuseppe Aruta 2018-04-06] Utils to work with Feature Schema

    // Text file extension
    final static String EXTENSION = "schema";

    public static String GetSavedFileName;

    /**
     * save Feature schema of a selected Layer.class to external text file
     * 
     * @param layer
     * @return
     * @throws Exception
     */
    public static boolean saveSchema(Layer layer) throws Exception {
        String schemaString = "";
        final JFileChooser chooser = new GUIUtil.FileChooserWithOverwritePrompting();
        chooser.setFileSelectionMode(0);
        chooser.setDialogType(1);
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Layer schema", EXTENSION);
        chooser.setFileFilter(filter);
        File file = null;
        final String ext = EXTENSION;
        final int ret = chooser.showSaveDialog(JUMPWorkbench.getInstance()
                .getFrame());
        if (ret == 0) {
            file = chooser.getSelectedFile();
            String fileName = file.getAbsolutePath();
            if (!fileName.endsWith("." + ext)) {
                fileName = fileName + "." + ext;
            }
            FileOperations.setFile(file);
            final File filePrintfFormateStats = new File(fileName);
            GetSavedFileName = filePrintfFormateStats.getAbsolutePath();
            final PrintWriter pWriter = new PrintWriter(new FileOutputStream(
                    filePrintfFormateStats));
            final FeatureSchema featureSchema = layer
                    .getFeatureCollectionWrapper().getFeatureSchema();
            final int numAttributes = featureSchema.getAttributeCount();
            for (int index = 0; index < numAttributes; index++) {
                final String name = featureSchema.getAttributeName(index);
                final AttributeType type = featureSchema
                        .getAttributeType(index);

                schemaString = name + "\t" + type;
                pWriter.println(schemaString.trim());
            }
            pWriter.close();
        }
        return true;
    }

    private static String readFile(String filePath) {
        final StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    /**
     * load feature schema into a selected Layer.class from an external text
     * file
     * 
     * @param layer
     * @return
     * @throws Exception
     */
    public static boolean loadSchema(Layer layer) throws Exception {
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Layer schema", EXTENSION);
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        chooser.addChoosableFileFilter(filter);
        chooser.setFileSelectionMode(0);
        chooser.setDialogType(0);
        final int result = chooser.showOpenDialog(JUMPWorkbench.getInstance()
                .getFrame());
        String schemaString = "";
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            schemaString = readFile(file.getAbsolutePath());
            final FeatureSchema featureSchema = new FeatureSchema();
            boolean isSchema = (schemaString.length() > 0);
            if (isSchema) {
                int tabIndex = schemaString.indexOf("\t");
                int crIndex = schemaString.indexOf("\n");
                boolean endOfString = ((tabIndex < 0) || (crIndex < 0));
                while (!endOfString) {
                    final String name = schemaString.substring(0, tabIndex);
                    final String typeStr = schemaString.substring(tabIndex + 1,
                            crIndex);
                    AttributeType type = AttributeType.STRING;
                    if (typeStr.compareToIgnoreCase("STRING") == 0) {
                        type = AttributeType.STRING;
                    } else if (typeStr.compareToIgnoreCase("DOUBLE") == 0) {
                        type = AttributeType.DOUBLE;
                    } else if (typeStr.compareToIgnoreCase("INTEGER") == 0) {
                        type = AttributeType.INTEGER;
                    } else if (typeStr.compareToIgnoreCase("DATE") == 0) {
                        type = AttributeType.DATE;
                    } else if (typeStr.compareToIgnoreCase("GEOMETRY") == 0) {
                        type = AttributeType.GEOMETRY;
                    } else if (typeStr.compareToIgnoreCase("OBJECT") == 0) {
                        type = AttributeType.OBJECT;
                    } else if (typeStr.compareToIgnoreCase("BOOLEAN") == 0) {
                        type = AttributeType.BOOLEAN;
                    } else if (typeStr.compareToIgnoreCase("LONG") == 0) {
                        type = AttributeType.LONG;
                    } else {
                        isSchema = false;
                        break;
                    }
                    featureSchema.addAttribute(name, type);
                    schemaString = schemaString.substring(crIndex + 1);
                    tabIndex = schemaString.indexOf("\t");
                    crIndex = schemaString.indexOf("\n");
                    endOfString = ((tabIndex < 0) || (crIndex < 0));
                }
                isSchema = (featureSchema.getAttributeCount() > 0);
            }
            if (isSchema) {
                final FeatureSchema layerSchema = layer
                        .getFeatureCollectionWrapper().getFeatureSchema();
                final int numAttributes = featureSchema.getAttributeCount();
                boolean changedSchema = false;
                for (int index = 0; index < numAttributes; index++) {
                    final String name = featureSchema.getAttributeName(index);
                    final AttributeType type = featureSchema
                            .getAttributeType(index);

                    if (!layerSchema.hasAttribute(name)) {
                        if ((type == AttributeType.STRING)
                                || (type == AttributeType.DOUBLE)
                                || (type == AttributeType.INTEGER)
                                || (type == AttributeType.DATE)
                                || (type == AttributeType.OBJECT)
                                || (type == AttributeType.BOOLEAN)
                                || (type == AttributeType.LONG)) {
                            layerSchema.addAttribute(name, type);
                            changedSchema = true;
                        }
                    }
                }

                if (changedSchema) {
                    final List<Feature> layerFeatures = layer
                            .getFeatureCollectionWrapper().getFeatures();
                    for (int j = 0; j < layerFeatures.size(); j++) {
                        final Feature newFeature = new BasicFeature(layerSchema);
                        final Feature oldFeature = layerFeatures.get(j);
                        final int numAttribs = oldFeature.getAttributes().length;

                        for (int k = 0; k < numAttribs; k++) {
                            newFeature.setAttribute(k,
                                    oldFeature.getAttribute(k));
                        }
                        oldFeature.setSchema(newFeature.getSchema());
                        oldFeature.setAttributes(newFeature.getAttributes());
                    }
                    layer.setFeatureCollectionModified(true);
                    layer.fireLayerChanged(LayerEventType.METADATA_CHANGED);
                }
            }
        }
        return true;
    }
}
