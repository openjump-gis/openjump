/*
 * Created on Nov 28, 2005
 *
 */
package com.vividsolutions.jump.workbench.ui.plugin.imagery;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openjump.core.ui.plugin.file.open.OpenFileWizard;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.imagery.ImageryLayerDataset;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageFactoryFileLayerLoader;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageStyle;
import com.vividsolutions.jump.workbench.imagery.ReferencedImagesLayer;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardDialog;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

/**
 * Simple Image Layer Management UI. Allows the user to add / remove images, and
 * view some metadata.
 * 
 * @author David Zwiers, Vivid Solutions. created May 8, 2006
 */
public class ImageLayerManagerPlugIn extends AbstractPlugIn {

  public ImageLayerManagerPlugIn() {
    super(I18N
        .get("ui.plugin.imagery.ImageLayerManagerPlugIn.Image-Layer-Manager"));
  }

  public Icon getIcon() {
    return IconLoader.icon("maps-edit_16.png");
  }

  public static EnableCheck createEnableCheck(final WorkbenchContext context) {
    MultiEnableCheck mec = new MultiEnableCheck();

    mec.add(new EnableCheckFactory(context)
        .createExactlyNLayersMustBeSelectedCheck(1));
    mec.add(new EnableCheck() {
      public String check(JComponent component) {
        return context.getLayerNamePanel().getSelectedLayers()[0]
            .getStyle(ReferencedImageStyle.class) == null ? I18N
            .get("ui.plugin.imagery.ImageLayerManagerPlugIn.Layer-must-be-an-Imagery-layer")
            : null;
      }
    });
    return mec;
  }

  public boolean execute(PlugInContext context) throws Exception {
    JDialog dlg = new ImageLayerManagerDialog(context);

    dlg.pack();
    dlg.setModal(true);
    GUIUtil.centre(dlg, context.getWorkbenchFrame());
    dlg.setVisible(true);

    return false;
  }

  private static class FeaturePrinter {
    private Feature instance;

    public FeaturePrinter(Feature i) {
      instance = i;
    }

    public String toString() {
      String val = (instance == null) ? "   " : (String) instance
          .getAttribute(ImageryLayerDataset.ATTR_URI);
      return val;
    }
  }

  private static class ImageLayerManagerDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 3199060497667524101L;
    private PlugInContext context;
    private Layer layer;
    private Vector images;
    private JList imagesPaths;
    private JTextArea metadata;
    private JScrollPane metadataScrollPane;

    public ImageLayerManagerDialog(PlugInContext context) {
      super(context.getWorkbenchFrame());
      this.context = context;
      setTitle(I18N
          .get("ui.plugin.imagery.ImageLayerManagerDialog.Image-Layer-Manager"));

      layer = context.getSelectedLayer(0);

      updateImages();

      initialize();
      loadMetadata();

      imagesPaths.setSelectedIndex(0);
    }

    private void updateImages() {
      // clone it
      images = new Vector();

      for (Iterator i = layer.getFeatureCollectionWrapper().getFeatures()
          .iterator(); i.hasNext();) {
        images.add(new FeaturePrinter((Feature) i.next()));
      }

    }

    private void initialize() {
      JPanel mainPanel = createMainPanel();
      JPanel buttonPanel = createButtonPanel();
      JPanel dialogPanel = new JPanel(new BorderLayout());

      dialogPanel.add(mainPanel, BorderLayout.CENTER);
      dialogPanel.add(buttonPanel, BorderLayout.EAST);

      getContentPane().add(dialogPanel);
    }

    private JPanel createMainPanel() {
      JPanel mainPanel = new JPanel(new GridBagLayout());

      imagesPaths = new JList();
      imagesPaths.setListData(images);
      imagesPaths.setBorder(BorderFactory.createLoweredBevelBorder());
      imagesPaths.setFont(context.getActiveInternalFrame().getFont());

      JScrollPane imagePathsScrollPane = new JScrollPane(imagesPaths);
      imagePathsScrollPane.setPreferredSize(new Dimension(400, 150));
      imagePathsScrollPane.setMinimumSize(new Dimension(400, 150));

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 1;
      gbc.insets = new Insets(2, 2, 2, 2);
      gbc.fill = GridBagConstraints.HORIZONTAL;

      imagesPaths.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          loadMetadata();
        }
      });

      imagesPaths.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          flashSelectedImages();
        }
      });

      mainPanel.add(imagePathsScrollPane, gbc);

      JLabel label = new JLabel();
      label.setText("Image Metadata");
      label.setFont(context.getActiveInternalFrame().getFont());
      label.setBackground(getBackground());
      gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.gridwidth = 1;
      gbc.insets = new Insets(2, 2, 2, 2);
      gbc.fill = GridBagConstraints.NONE;
      mainPanel.add(label, gbc);

      metadata = new JTextArea();
      metadata.setBackground(getBackground());
      // metadata.setBorder(BorderFactory.createLoweredBevelBorder());
      metadata.setAutoscrolls(true);
      metadata.setFont(context.getActiveInternalFrame().getFont());
      gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.gridx = 0;
      gbc.gridy = 2;
      gbc.weightx = 1;
      gbc.weighty = 1;
      gbc.gridwidth = 1;
      gbc.insets = new Insets(2, 2, 2, 2);
      gbc.ipadx = 4;
      gbc.ipady = 4;
      gbc.fill = GridBagConstraints.BOTH;
      metadata.setMargin(new Insets(4, 4, 4, 4));
      metadata.setEditable(false);

      metadataScrollPane = new JScrollPane(metadata);
      metadataScrollPane.setPreferredSize(new Dimension(400, 100));

      mainPanel.add(metadataScrollPane, gbc);

      return mainPanel;
    }

    private JPanel createButtonPanel() {
      int buttonNumber = 0;
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new GridBagLayout());

      JButton button = new JButton(
          I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Add") + "...",
          GUIUtil.toSmallIcon(IconLoader.icon("Plus.gif")));
      button.addActionListener(new AddButtonListener());
      buttonPanel.add(button, new GridBagConstraints(0, buttonNumber++, 1, 1,
          0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
          new Insets(2, 2, 2, 2), 0, 0));

      button = new JButton(
          I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Delete"),
          GUIUtil.toSmallIcon(IconLoader.icon("Delete.gif")));
      button.addActionListener(new DeleteButtonListener());
      buttonPanel.add(button, new GridBagConstraints(0, buttonNumber++, 1, 1,
          0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
          new Insets(2, 2, 2, 2), 0, 0));

      button = new JButton(
          I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Close"));
      button.addActionListener(new CloseButtonListener(this));
      buttonPanel.add(button, new GridBagConstraints(0, buttonNumber++, 1, 1,
          0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
          new Insets(2, 2, 2, 2), 0, 0));

      // filler
      buttonPanel.add(new Label(), new GridBagConstraints(0, buttonNumber++, 1,
          1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
          new Insets(2, 2, 2, 2), 0, 0));

      return buttonPanel;
    }

    private void flashSelectedImages() {
      GeometryFactory factory = new GeometryFactory();
      Object[] values = imagesPaths.getSelectedValues();
      Geometry[] geoms = new Geometry[values.length];

      if (values != null && values.length > 0) {
        for (int i = 0; i < values.length; i++) {
          FeaturePrinter current = (FeaturePrinter) values[i];
          geoms[i] = current.instance.getGeometry();
        }
      }

      try {
        GeometryCollection gc = factory.createGeometryCollection(geoms);
        context.getLayerViewPanel().flash(gc);
      } catch (Exception ex) {
        // ignored
      }
    }

    // @see ImageryLayerDataset.getSchema()
    private void loadMetadata() {
      StringBuffer buf = new StringBuffer();
      if (images.size() > 0) {
        Object[] values = imagesPaths.getSelectedValues();

        if (values != null && values.length > 0) {
          for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
              FeaturePrinter current = (FeaturePrinter) values[i];
              if (current.instance != null) {
                appendMetadata(current.instance, buf);
              }
            }
            buf.append("\n");
          }
        }
      }
      // add some spaces to ensure string is non-empty
      buf.append("\n  ");
      metadata.setText(buf.toString());
      // scroll to top of text area
      metadata.setCaretPosition(0);
    }

    private void appendMetadata(Feature imageFeat, StringBuffer buf) {
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Uri")
          + ": \t" + imageFeat.getAttribute(ImageryLayerDataset.ATTR_URI)
          + "\n");
      // buf.append("  "+I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Format")+": \t"
      // + imageFeat.getAttribute(ImageryLayerDataset.ATTR_FORMAT) + "\n");
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Factory")
          + ": \t" + imageFeat.getAttribute(ImageryLayerDataset.ATTR_FACTORY)
          + "\n");
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Type") + ": \t"
          + imageFeat.getAttribute(ImageryLayerDataset.ATTR_TYPE) + "\n");
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Loader")
          + ": \t" + imageFeat.getAttribute(ImageryLayerDataset.ATTR_LOADER)
          + "\n");
      appendEnvelope(imageFeat.getGeometry().getEnvelopeInternal(), buf);
    }

    private void appendEnvelope(Envelope env, StringBuffer buf) {
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Lower-Left")
          + ":  \t" + env.getMinX() + ", " + env.getMinY() + "\n");
      buf.append("  "
          + I18N.get("ui.plugin.imagery.ImageLayerManagerDialog.Upper-Right")
          + ": \t" + env.getMaxX() + ", " + env.getMaxY() + "\n");
    }

    private void deleteSelectedImages() {
      Object[] values = imagesPaths.getSelectedValues();
      for (int i = 0; values != null && i < values.length; i++) {
        if (values[i] != null) {
          FeaturePrinter current = (FeaturePrinter) values[i];
          // order matters here: current gets updated when manipulating the UI.
          if (current.instance != null) {
            Feature feat = current.instance;
            layer.getFeatureCollectionWrapper().remove(feat);
            // TODO: dispose feature image properly
          }
          images.remove(current);
          imagesPaths.setListData(images);
        }
      }
    }

    private void loadImages() {

      final WorkbenchFrame workbenchFrame = context.getWorkbenchFrame();
      final WorkbenchContext workbenchContext = context.getWorkbenchContext();

      final WizardDialog dialog = new WizardDialog(workbenchFrame, getName(),
          context.getErrorHandler());
      final OpenFileWizard wizard = new OpenFileWizard(workbenchContext,
          ReferencedImageFactoryFileLayerLoader.class);
      wizard.setLayer((ReferencedImagesLayer) layer);
      // OpenReferencedImageWizard wizard = new
      // OpenReferencedImageWizard(workbenchContext);

      wizard.initialize(workbenchContext, dialog);
      List<WizardPanel> panels = wizard.getPanels();
      String firstId = wizard.getFirstId();

      dialog.init(panels);
      dialog.setCurrentWizardPanel(firstId);
      dialog.pack();
      GUIUtil.centreOnWindow(dialog);
      dialog.setVisible(true);
      boolean result = dialog.wasFinishPressed();
      // [mmichaud 2013-11-09] threading image loading makes it faster and more responsive
      try {
          Thread t = new Thread() {
              public void run() {
                  try {
                    wizard.run(dialog,
                          new TaskMonitorDialog(workbenchFrame, context.getErrorHandler()));
                    updateImages();
                    imagesPaths.setListData(images);
                  } catch(Exception e) {throw new Error(e);}
              }
          };
          t.start();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    private class DeleteButtonListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
        deleteSelectedImages();
      }
    }

    private class AddButtonListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
        loadImages();
      }
    }

    private class CloseButtonListener implements ActionListener {
      private JDialog dialog;

      CloseButtonListener(JDialog dlg) {
        dialog = dlg;
      }

      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    }
  }

}
