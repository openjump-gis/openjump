/* *****************************************************************************
 The Open Java Unified Mapping Platform (OpenJUMP) is an extensible, interactive
 GUI for visualizing and manipulating spatial features with geometry and
 attributes. 

 Copyright (C) 2007  Revolution Systems Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 For more information see:
 
 http://openjump.org/

 ******************************************************************************/
package org.openjump.core.ui.plugin.file.open;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import org.openjump.core.ui.io.file.FileLayerLoader;
import org.openjump.core.ui.io.file.Option;
import org.openjump.core.ui.swing.factory.field.FieldComponentFactoryRegistry;
import org.openjump.swing.factory.field.FieldComponentFactory;
import org.openjump.swing.listener.ValueChangeEvent;
import org.openjump.swing.listener.ValueChangeListener;
import org.openjump.swing.util.SpringUtilities;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.ui.InputChangedListener;
import com.vividsolutions.jump.workbench.ui.wizard.WizardPanel;

public class SelectFileOptionsPanel extends JPanel implements WizardPanel {
  private static final long serialVersionUID = -3105562554743126639L;

  public static final String KEY = SelectFileOptionsPanel.class.getName();

  public static final String TITLE = I18N.get(KEY);

  public static final String FILE_TYPE = I18N.get(KEY + ".file-type");

  public static final String INSTRUCTIONS = I18N.get(KEY + ".instructions");

  public static final String USE_SAME_SETTINGS_FOR = I18N.get(KEY
    + ".use-same-settings-for");

  private JPanel mainPanel;

  private Set<InputChangedListener> listeners = new LinkedHashSet<InputChangedListener>();

  private OpenFileWizardState state;

  private WorkbenchContext workbenchContext;

  public SelectFileOptionsPanel(WorkbenchContext workbenchContext) {
    super(new BorderLayout());
    this.workbenchContext = workbenchContext;
    JPanel scrollPanel = new JPanel(new BorderLayout());

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    scrollPanel.add(mainPanel, BorderLayout.NORTH);
    JScrollPane scrollPane = new JScrollPane(scrollPanel);
    add(scrollPane, BorderLayout.CENTER);
  }

  public OpenFileWizardState getState() {
    return state;
  }

  public void setState(OpenFileWizardState state) {
    this.state = state;
  }

  public void enteredFromLeft(Map dataMap) {
    mainPanel.removeAll();
    for (Entry<FileLayerLoader, Set<URI>> entry : state.getFileLoaderFiles()
      .entrySet()) {
      FileLayerLoader fileLayerLoader = entry.getKey();
      List<Option> optionFields = fileLayerLoader.getOptionMetadata();
      if (!optionFields.isEmpty()) {
        Set<URI> files = entry.getValue();
        addLoader(fileLayerLoader, optionFields, files);
      }
    }
  }

  private void addLoader(final FileLayerLoader loader,
    final List<Option> optionFields, final Set<URI> files) {
    final JPanel panel = new JPanel();
    final String description = loader.getDescription();
    panel.setBorder(BorderFactory.createTitledBorder(description));

    ActionListener useSameListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        panel.removeAll();
        if (((JCheckBox)e.getSource()).isSelected()) {
          addSameSettingsFields(loader, panel, description, files,
            optionFields, this);
        } else {
          addIndividualSettingsFields(loader, panel, description, files,
            optionFields, this);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
        fireInputChanged();
      }

    };
    addIndividualSettingsFields(loader, panel, description, files,
      optionFields, useSameListener);
    mainPanel.add(panel);
  }

  public void addIndividualSettingsFields(final FileLayerLoader loader,
    JPanel panel, String description, Set<URI> files,
    List<Option> optionFields, ActionListener useSameListener) {
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    if (files.size() > 1) {
      JPanel samePanel = new JPanel(new SpringLayout());
      samePanel.add(new JLabel(USE_SAME_SETTINGS_FOR + description));
      JCheckBox useSameField = new JCheckBox();
      useSameField.setSelected(false);
      samePanel.add(useSameField);
      useSameField.addActionListener(useSameListener);
      SpringUtilities.makeCompactGrid(samePanel, 1, 2, 5, 5, 5, 5);
      samePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(samePanel);
    }
    for (final URI file : files) {
      JPanel filePanel = new JPanel(new SpringLayout());
      Map<String, Object> options = state.getOptions(file);
      filePanel.setBorder(BorderFactory.createTitledBorder(state.getFileName(file)));
      for (Option option : optionFields) {
        final String name = option.getName();
        String label = I18N.get(loader.getClass().getName() + "."
          //+ name.replaceAll(" ", "-"));
          + name);
        filePanel.add(new JLabel(label));

        String type = option.getType();
        FieldComponentFactory factory = FieldComponentFactoryRegistry.getFactory(
          workbenchContext, type);
        ValueChangeListener fieldListener = new ValueChangeListener() {
          public void valueChanged(ValueChangeEvent event) {
            Object value = event.getValue();
            state.setOption(file, name, value);
            fireInputChanged();
          }
        };
        JComponent field = factory.createComponent(fieldListener);
        factory.setValue(field, options.get(name));
        // [mmichaud 2009-09-13]
        // init field component and wizard state with the option's defaultValue
        // the defaultValue may be null
        if (option.getDefault() != null) {
            factory.setValue(field, option.getDefault());
            state.setOption(file, name, factory.getValue(field));
        }
        // end
        filePanel.add(field);
        SpringUtilities.makeCompactGrid(filePanel,
          filePanel.getComponentCount() / 2, 2, 5, 5, 5, 5);

      }
      filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.add(filePanel);
    }
  }

  public void addSameSettingsFields(final FileLayerLoader loader, JPanel panel,
    String description, Set<URI> files, List<Option> optionFields,
    ActionListener useSameListener) {
    panel.setLayout(new SpringLayout());
    panel.add(new JLabel(USE_SAME_SETTINGS_FOR + description));
    JCheckBox useSameField = new JCheckBox();
    useSameField.setSelected(true);
    useSameField.addActionListener(useSameListener);
    panel.add(useSameField);

    for (Option option : optionFields) {
      final String label = option.getName();
      panel.add(new JLabel(label));

      String type = option.getType();
      FieldComponentFactory factory = FieldComponentFactoryRegistry.getFactory(
        workbenchContext, type);
      panel.add(factory.createComponent(new ValueChangeListener() {
        public void valueChanged(ValueChangeEvent event) {
          Object value = event.getValue();
          state.setOption(loader, label, value);
          fireInputChanged();
        }
      }));
      state.setOption(loader, option.getName(), null);
    }

    SpringUtilities.makeCompactGrid(panel, 1 + optionFields.size(), 2, 5, 5, 5,
      5);
  }

  public void exitingToRight() throws Exception {
  }

  public String getID() {
    return KEY;
  }

  public String getInstructions() {
    return INSTRUCTIONS;
  }

  public String getNextID() {
    return state.getNextPanel(KEY);
  }

  public String getTitle() {
    return TITLE;
  }

  public boolean isInputValid() {
    return state.hasRequiredOptions();
  }

  public void add(InputChangedListener listener) {
    listeners.add(listener);
  }

  public void remove(InputChangedListener listener) {
    listeners.remove(listener);
  }

  private void fireInputChanged() {
    for (InputChangedListener listener : listeners) {
      listener.inputChanged();
    }
  }
}
