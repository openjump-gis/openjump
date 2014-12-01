package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.ui.EditTransaction;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.snap.SnapToFeaturesPolicy;

//Notes layer can now be saved with a task [LDB 12-15-2005]

public class NoteTool extends NClickTool {

  public static final String NOTES = I18N.get("ui.cursortool.NoteTool.notes");

  // Create DEFAULT_SCHEMA as a static attribute to be sure the schema is
  // initialized from NoteStyle class with internationalized attributes
  private static final FeatureSchema DEFAULT_SCHEMA = NoteStyle
      .createFeatureSchema();

  private LayerViewPanel panel;

  private abstract class Mode {

    private Feature noteFeature;

    public Mode(Feature noteFeature) {
      this.noteFeature = noteFeature;
    }

    public Coordinate location() {
      return noteFeature.getGeometry().getCoordinate();
    }

    public abstract void commit(String text);

    protected Feature getNoteFeature() {
      return noteFeature;
    }

    public abstract String initialText();
  }

  private class CreateMode extends Mode {

    public CreateMode(final Coordinate location) {
      super(new BasicFeature(layer().getFeatureCollectionWrapper()
          .getFeatureSchema()) {

        {
          setAttribute(NoteStyle.CREATED, new Date());
          setAttribute(NoteStyle.GEOMETRY,
              new GeometryFactory().createPoint(location));
        }
      });
    }

    public void commit(String text) {
      if (text.length() > 0) {
        disableAutomaticInitialZooming();
        getNoteFeature().setAttribute(NoteStyle.MODIFIED, new Date());
        getNoteFeature().setAttribute(NoteStyle.TEXT, text);
        EditTransaction transaction = new EditTransaction(
            Collections.EMPTY_LIST, getName(), layer(),
            isRollingBackInvalidEdits(), true, getPanel());
        transaction.createFeature(getNoteFeature());
        transaction.commit();
      }
    }

    public String initialText() {
      return "";
    }
  }

  private class EditMode extends Mode {

    public EditMode(Feature noteFeature) {
      super(noteFeature);
    }

    public void commit(final String text) {
      final Date modifiedDate = new Date();
      final Date oldModifiedDate = (Date) getNoteFeature().getAttribute(
          NoteStyle.MODIFIED);
      final String oldText = getNoteFeature().getString(NoteStyle.TEXT);
      execute(new UndoableCommand(getName()) {

        public void execute() {
          update(getNoteFeature(), text, modifiedDate, layer());
        }

        public void unexecute() {
          update(getNoteFeature(), oldText, oldModifiedDate, layer());
        }
      });
    }

    private void update(Feature noteFeature, String text, Date modifiedDate,
        Layer layer) {
      noteFeature.setAttribute(NoteStyle.MODIFIED, modifiedDate);
      noteFeature.setAttribute(NoteStyle.TEXT, text);
      layer.getLayerManager().fireFeaturesChanged(
          Collections.singleton(noteFeature),
          FeatureEventType.ATTRIBUTES_MODIFIED, layer);
    }

    public String initialText() {
      return getNoteFeature().getString(NoteStyle.TEXT);
    }
  }

  public NoteTool() {
    super(1);
    panel = getPanel();
    getSnapManager().addPolicies(
        Collections.singleton(new SnapToFeaturesPolicy()));
    textArea = NoteStyle.createTextArea();
    textArea.addFocusListener(new FocusAdapter() {

      public void focusLost(FocusEvent e) {
        if (panelContainsTextArea()) {
          boolean doit = (textArea.getText().trim().length() > 0);
          if (doit)
            getPanel().getLayerManager().getUndoableEditReceiver()
                .startReceiving();
          removeTextAreaFromPanel();
          if (doit)
            getPanel().getLayerManager().getUndoableEditReceiver()
                .stopReceiving();
        }
      }
    });
  }

  public String getName() {
    return I18N.get("ui.cursortool.NoteTool");
  }

  private JTextArea textArea;

  private Mode mode;

  public void deactivate() {
    if (panelContainsTextArea()) {
      boolean doit = (textArea.getText().trim().length() > 0);
      if (doit)
        getPanel().getLayerManager().getUndoableEditReceiver().startReceiving();
      removeTextAreaFromPanel();
      if (doit)
        getPanel().getLayerManager().getUndoableEditReceiver().stopReceiving();
    }
    super.deactivate();
  }

  public Cursor getCursor() {
    return cursor;
  }

  protected void gestureFinished() throws Exception {
    reportNothingToUndoYet();
    Feature noteFeatureAtClick = noteFeature(getModelDestination());
    removeTextAreaFromPanel();
    mode = mode(noteFeatureAtClick, getModelDestination());

    // Since focusLost will be called after gestureFinished() is called
    // any testArea added here would be removed.
    // However the invokeLater will be called after any other calls to
    // removeTextAreaFromPanel()
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          addTextAreaToPanel(mode.location());
        } catch (NoninvertibleTransformException e) {
          getPanel().getContext().handleThrowable(e);
        }
      }
    });
  }

  private Mode mode(Feature noteFeatureAtClick, Coordinate click) {
    return noteFeatureAtClick == null ? (Mode) new CreateMode(click)
        : new EditMode(noteFeatureAtClick);
  }

  private Feature noteFeature(Coordinate click) {
    return noteFeature(new Envelope(click, new Coordinate(click.x
        - NoteStyle.WIDTH / scale(), click.y + NoteStyle.HEIGHT / scale())));
  }

  private Feature noteFeature(Envelope envelope) {
    return (Feature) firstOrNull(layer().getFeatureCollectionWrapper().query(
        envelope));
  }

  private Object firstOrNull(Collection items) {
    return !items.isEmpty() ? items.iterator().next() : null;
  }

  private double scale() {
    return getPanel().getViewport().getScale();
  }

  private void addTextAreaToPanel(Coordinate location)
      throws NoninvertibleTransformException {
    layer().setVisible(true);
    if (getPanel().getLayout() != null) {
      getPanel().setLayout(null);
    }
    textArea.setText(mode.initialText());
    textArea.setBackground(layer().getBasicStyle().getFillColor());
    getPanel().add(textArea);

    textArea.addCaretListener(new CaretListener() {
      public void caretUpdate(CaretEvent e) {
        JTextArea textArea = (JTextArea) e.getSource();
        int ht = textArea.getPreferredSize().height;
        int wt = textArea.getPreferredSize().width;
        if (ht < NoteStyle.HEIGHT)
          ht = NoteStyle.HEIGHT;
        if (wt < NoteStyle.WIDTH)
          wt = NoteStyle.WIDTH;
        int x = textArea.getBounds().x;
        int y = textArea.getBounds().y;
        textArea.setBounds(x, y, wt, ht);
      }
    });

    int ht = textArea.getPreferredSize().height;
    int wt = textArea.getPreferredSize().width;
    if (ht < NoteStyle.HEIGHT)
      ht = NoteStyle.HEIGHT;
    if (wt < NoteStyle.WIDTH)
      wt = NoteStyle.WIDTH;
    textArea.setBounds((int) getPanel().getViewport().toViewPoint(location)
        .getX(), (int) getPanel().getViewport().toViewPoint(location).getY(),
        wt, ht);

    // textArea.setBounds((int) getPanel().getViewport().toViewPoint(location)
    // .getX(), (int) getPanel().getViewport().toViewPoint(location)
    // .getY(), NoteStyle.WIDTH, NoteStyle.HEIGHT);
    textArea.requestFocus();
  }

  private boolean panelContainsTextArea() {
    return getPanel() != null
        && Arrays.asList(getPanel().getComponents()).contains(textArea);
  }

  private void removeTextAreaFromPanel() {
    if (!panelContainsTextArea()) {
      return;
    }
    mode.commit(textArea.getText().trim());
    getPanel().remove(textArea);
    getPanel().superRepaint();
  }

  private void disableAutomaticInitialZooming() {
    getPanel().setViewportInitialized(true);
  }

  public Layer layer() {
    LayerManager layerManager = getPanel().getLayerManager();
    if (layerManager.getLayer(NOTES) != null) {
      return layerManager.getLayer(NOTES);
    }
    Layer noteLayer = new Layer(NOTES, Color.yellow.brighter().brighter(),
    // new FeatureDataset(NoteStyle.createFeatureSchema()), layerManager);
        new FeatureDataset(DEFAULT_SCHEMA), layerManager);

    boolean firingEvents = layerManager.isFiringEvents();
    layerManager.setFiringEvents(false);
    try {
      noteLayer.addStyle(new NoteStyle());
      noteLayer.setEditable(true);
    } finally {
      layerManager.setFiringEvents(firingEvents);
    }
    layerManager.addLayer(StandardCategoryNames.SYSTEM, noteLayer);
    return noteLayer;
  }

  public Icon getIcon() {
    return icon;
  }

  protected Shape getShape() throws NoninvertibleTransformException {
    return null;
  }

  private ImageIcon icon = IconLoader.icon("sticky.png");

  private Cursor cursor = GUIUtil.createCursorFromIcon(icon.getImage());

}
