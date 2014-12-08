package de.latlon.deejump.plugin.style;

import static de.latlon.deejump.plugin.style.VertexStylesFactory.BITMAP_STYLE;
import static de.latlon.deejump.plugin.style.VertexStylesFactory.CIRCLE_STYLE;
import static de.latlon.deejump.plugin.style.VertexStylesFactory.CROSS_STYLE;
import static de.latlon.deejump.plugin.style.VertexStylesFactory.SQUARE_STYLE;
import static de.latlon.deejump.plugin.style.VertexStylesFactory.STAR_STYLE;
import static de.latlon.deejump.plugin.style.VertexStylesFactory.TRIANGLE_STYLE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.workbench.ui.renderer.style.VertexStyle;
import com.vividsolutions.jump.workbench.ui.style.BasicStylePanel;

/**
 * <code>VertexStyleChooser</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class VertexStyleChooser extends JPanel {

    private static final long serialVersionUID = 7256506666365045855L;

    static final List<String> STYLE_NAMES;

    static {

        List<String> TEMP_STYLE_NAMES = new ArrayList<String>(5);
        TEMP_STYLE_NAMES.add(SQUARE_STYLE);
        TEMP_STYLE_NAMES.add(CIRCLE_STYLE);
        TEMP_STYLE_NAMES.add(TRIANGLE_STYLE);
        TEMP_STYLE_NAMES.add(CROSS_STYLE);
        TEMP_STYLE_NAMES.add(STAR_STYLE);
        TEMP_STYLE_NAMES.add(BITMAP_STYLE);

        STYLE_NAMES = Collections.unmodifiableList(TEMP_STYLE_NAMES);
    }

    private JComboBox pointTypeComboBox;

    private JButton bitmapChangeButton;

    private String currentFilename;

    // sizeSlider is initialized from DeeRenderingStylePanel
    // not sure it is still useful for VertexStyleChooser to
    // have its own slider (maybe for some old plugins)
    public JSlider sizeSlider;

    private boolean activateOwnSlider = false;

    private Blackboard blackboard;

    private BasicStylePanel stylePanel;

    /**
     * @param activateOwnSlider true if one need a chooser with its own size slider
     */
    public VertexStyleChooser(boolean activateOwnSlider) {
        super();
        initGUI();
        this.activateOwnSlider = activateOwnSlider;
    }

    protected void setBlackboard(Blackboard persistentBlackboard) {
        blackboard = persistentBlackboard;
    }

    protected void setStylePanel(DeeRenderingStylePanel stylePanel) {
        this.stylePanel = stylePanel;
    }

    private void initGUI() {
        pointTypeComboBox = new JComboBox();
        pointTypeComboBox.setEditable(false);
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.square"));
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.circle"));
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.triangle"));
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.cross"));
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.star"));
        pointTypeComboBox.addItem(I18N.get("deejump.ui.style.RenderingStylePanel.bitmap"));

        pointTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox comboBox = (JComboBox) e.getSource();
                String selectedItem = STYLE_NAMES.get(comboBox.getSelectedIndex());
                setSelectedStyle(selectedItem);
            }
        });

        bitmapChangeButton = new JButton(I18N.get("deejump.ui.style.RenderingStylePanel.bitmap-change"));
        bitmapChangeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                openFileChooser();
            }
        });
        if (sizeSlider == null) {
            sizeSlider = new JSlider(); // [sstein] init only if needed
        }
        sizeSlider.setBorder(BorderFactory.createTitledBorder("Point size: "));
        if (this.activateOwnSlider) {
            Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(new Integer(5), new JLabel("5"));
            labelTable.put(new Integer(10), new JLabel("10"));
            labelTable.put(new Integer(15), new JLabel("15"));
            labelTable.put(new Integer(20), new JLabel("20"));
            sizeSlider.setLabelTable(labelTable);
            sizeSlider.setEnabled(true);
            sizeSlider.setMajorTickSpacing(1);
            sizeSlider.setMajorTickSpacing(0);
            sizeSlider.setPaintLabels(true);
            sizeSlider.setMinimum(4);
            sizeSlider.setValue(4);
            sizeSlider.setMaximum(20);
            sizeSlider.setSnapToTicks(false);
            sizeSlider.setPreferredSize(new Dimension(130, 49));
        }
        JPanel oberstPanel = new JPanel();
        oberstPanel.add(new JLabel(I18N.get("deejump.ui.style.RenderingStylePanel.point-display-type")));
        oberstPanel.add(pointTypeComboBox);
        oberstPanel.add(bitmapChangeButton);
        JPanel sliderPanel = new JPanel(); // [sstein] always init although it
        // may not be needed
        sliderPanel.add(sizeSlider);
        setLayout(new BorderLayout());
        add(oberstPanel, BorderLayout.NORTH);
        if (this.activateOwnSlider) {
            add(sliderPanel, BorderLayout.CENTER);
        }

    }

    public void addActionListener(ActionListener actionListener) {
        pointTypeComboBox.addActionListener(actionListener);
        bitmapChangeButton.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        pointTypeComboBox.removeActionListener(actionListener);
        bitmapChangeButton.removeActionListener(actionListener);
    }

    public void addChangeListener(ChangeListener cl) {
        if (this.activateOwnSlider) {
            this.sizeSlider.addChangeListener(cl);
        }
    }

    public void removeChangeListener(ChangeListener cl) {
        if (this.activateOwnSlider) {
            this.sizeSlider.removeChangeListener(cl);
        }
    }

    boolean openFileChooser() {
        boolean imageIsLoaded = false;
        JFileChooser fileChooser = new JFileChooser();
        String f = (String) blackboard.get("VertexStyleChooser.last-location");
        if (f != null) {
            File dir = new File(f);
            while (!dir.isDirectory()) {
                dir = dir.getParentFile();
            }
            fileChooser.setCurrentDirectory(dir);
        }
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".png")
                        || file.getName().toLowerCase().endsWith(".gif")
                        || file.getName().toLowerCase().endsWith(".jpg")
                        || file.getName().toLowerCase().endsWith(".svg");
            }

            @Override
            public String getDescription() {
                return "*.png, *.gif, *.jpg, *.svg";
            }
        });
        int showFileChooser = fileChooser.showOpenDialog(this);
        if (showFileChooser == JFileChooser.APPROVE_OPTION) {
            String currentFilePath;
            currentFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            setCurrentFileName(currentFilePath);
            setSelectedStyle(BITMAP_STYLE);
            blackboard.put("VertexStyleChooser.last-location", currentFilePath);
            imageIsLoaded = true;
        }
        return imageIsLoaded;
    }

    protected void setCurrentFileName(String fileName) {
        currentFilename = fileName;
    }

    /**
     * @return the file name
     */
    public String getCurrentFileName() {
        return currentFilename;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.pointTypeComboBox.setEnabled(enabled);
        this.bitmapChangeButton.setEnabled(enabled);
        if (this.activateOwnSlider) {
            this.sizeSlider.setEnabled(enabled);
        }
    }

    /**
     * @return the selected vertex style
     */
    public VertexStyle getSelectedStyle() {
        String wellKnowName = STYLE_NAMES.get(this.pointTypeComboBox.getSelectedIndex());
        if (BITMAP_STYLE.equals(wellKnowName)) {
            wellKnowName = getCurrentFileName();
            if (wellKnowName == null) {
                // reset to the first style
                wellKnowName = STYLE_NAMES.get(0);
            }
        }
        VertexStyle vertexStyle = VertexStylesFactory.createVertexStyle(wellKnowName);
        vertexStyle.setSize(sizeSlider.getValue());
        vertexStyle.setFillColor(stylePanel.getBasicStyle().getFillColor());
        vertexStyle.setLineColor(stylePanel.getBasicStyle().getLineColor());
        vertexStyle.setFilling(stylePanel.getBasicStyle().isRenderingFill());
        return vertexStyle;
    }

    public void setSelectedStyle(String currentVertexStyle) {
        int nameIndex = STYLE_NAMES.indexOf(currentVertexStyle);
        if (nameIndex > -1 && nameIndex < STYLE_NAMES.size()) {
            this.pointTypeComboBox.setSelectedIndex(nameIndex);
        }
    }
}