package com.vividsolutions.jump.workbench.ui.plugin.datastore;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openjump.core.ui.plugin.file.open.JFCWithEnterAction;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.datastore.DataStoreDriver;
import com.vividsolutions.jump.parameter.ParameterList;
import com.vividsolutions.jump.parameter.ParameterListSchema;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.datasource.LoadFileDataSourceQueryChooser;
import com.vividsolutions.jump.workbench.datastore.ConnectionDescriptor;
import com.vividsolutions.jump.workbench.registry.Registry;
import com.vividsolutions.jump.workbench.ui.ValidatingTextField;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

public class ConnectionDescriptorPanel extends JPanel
{
  private WorkbenchContext context;
  private ParameterListSchema schema = new ParameterListSchema(
      new String[] {}, new Class[] {});

  private static int createdConnectionCount = 0;

  private JTextField nameText = new JTextField();
  private JComboBox driverComboBox = null;

  private List editComponentList = new ArrayList();

  private void updateMainPanel(ParameterList parameterList,Blackboard bb) {
    mainPanel.removeAll();
    editComponentList.clear();
    addEditComponent(0, I18N.get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Name"), nameText);
    addEditComponent(1, I18N.get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Driver"), driverComboBox);
    for (int i = 0; i < schema.getNames().length; i++) {
      String name = schema.getNames()[i];
      editComponentList.add(createEditComponent(name,
          schema.getClasses()[i], parameterList.getParameter(name),bb));
      addEditComponent(i + 2, name, (Component) editComponentList
                       .get(editComponentList.size() - 1));
    }
    revalidate();
    repaint();
  }

  private void addEditComponent(int i, String name, Component editComponent) {
    mainPanel.add(new JLabel(name), new GridBagConstraints(0, i, 1, 1, 0,
        0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(2, 2, 2, 2), 0, 0));
    mainPanel.add(editComponent, new GridBagConstraints(1, i, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
        2, 2, 2), 0, 0));
    }

    private Component createEditComponent(String name, Class parameterClass,
            Object parameter, Blackboard bb) {
        Component editComponent = parameterClassHandler(name, parameterClass).createEditComponent(bb);
        if (parameter != null) {
            parameterClassHandler(name, parameterClass).setParameter(
                    editComponent, parameter);
        }
        return editComponent;
    }

    private ParameterClassHandler parameterClassHandler(String name,
            Class parameterClass) {
        if (name.equalsIgnoreCase("Password")) {
            return PASSWORD_HANDLER;
        }
        return (ParameterClassHandler) parameterClassToHandlerMap
                .get(parameterClass);
    }

    private interface ParameterClassHandler {

        Component createEditComponent(Blackboard bb);

        void setParameter(Component component, Object parameter);

        Object getParameter(Component component);
    }

    private static final ParameterClassHandler PASSWORD_HANDLER = new ParameterClassHandler() {
        public Component createEditComponent(Blackboard bb) {
            return new JPasswordField(20);
        }

        public void setParameter(Component component, Object parameter) {
            ((JPasswordField) component).setText((String) parameter);
        }

        public Object getParameter(Component component) {
            // The JavaDoc is not clear about *how* #getPassword is any more
            // secure than #getText [Jon Aquino 2005-03-07]
            return new String(((JPasswordField) component).getPassword());
        }
    };

    private static class JMPFileChooser extends JPanel {
      Blackboard bb;
      JFileChooser chooser;
      JTextField strFile;
      File f = null;
  
      public JMPFileChooser(Blackboard bb) {
        this.bb = bb;
        chooser = new JFCWithEnterAction();
  
        final JMPFileChooser th = this;
  
        strFile = new JTextField(20);
        JButton open = new JButton();
        open.setText("...");
        open.setMargin(new Insets(0, 0, 0, 0));
  
        strFile.addFocusListener(new FocusListener() {
  
          public void focusGained(FocusEvent e) {
            // do nothing
          }
  
          public void focusLost(FocusEvent e) {
            // check for a new value ...
            String t = strFile.getText();
            if (t == null) {
              th.f = null;
              return;
            }
            t = t.trim();
            if (t == null || "".equals(t)) {
              th.f = null;
              return;
            }
  
            th.f = new File(t);
          }
  
        });
  
        open.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (f != null) {
              chooser.setSelectedFile(f);
              th.setToolTipText(f.toString());
            }
            int selection = chooser.showOpenDialog(th);
            if (selection == JFileChooser.APPROVE_OPTION) {
              th.f = chooser.getSelectedFile();
              if (th.f == null) {
                strFile.setText("");
              } else {
                strFile.setText(th.f.toString());
              }
            }
          }
        });
  
        setBorder(BorderFactory.createLineBorder(getBackground(), 0));
        GridBagLayout gl = new GridBagLayout();
        setLayout(gl);
        setAlignmentX(0);
  
        GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0,
                0), 0, 0);
  
        add(strFile, c);
        gl.setConstraints(strFile, c);
  
        c = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        add(open, c);
        gl.setConstraints(open, c);
  
      }

      public JTextField getFileTextField() {
        return strFile;
      }
    }

    private Map parameterClassToHandlerMap = CollectionUtil
        .createMap(new Object[] { String.class, new ParameterClassHandler() {
  
          public Component createEditComponent(Blackboard bb) {
            return new JTextField(20);
          }
  
          public void setParameter(Component component, Object parameter) {
            ((JTextField) component).setText((String) parameter);
          }
  
          public Object getParameter(Component component) {
            return ((JTextField) component).getText();
          }
        }, File.class, new ParameterClassHandler() {
  
          public Component createEditComponent(Blackboard bb) {
            JMPFileChooser fileChooser = new JMPFileChooser(bb);
            if (bb.get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY) != null) {
              fileChooser.f = new File((String) bb
                  .get(LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY));
            }
            return fileChooser;
          }
  
          public void setParameter(Component component, Object parameter) {
            if (parameter != null) {
              File f = (File) parameter;
              ((JMPFileChooser) component).f = f;
              if (f != null && f.getParent() != null) {
                ((JMPFileChooser) component).bb.put(
                    LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY,
                    f.getParent());
                if (f.isFile()) {
                  int index = f.getName().lastIndexOf('.') + 1;
                  if (index > -1) {
                    final String suffix = f.getName().substring(index);
                    if (suffix != null && !"".equals(suffix.trim()))
                      ((JMPFileChooser) component).chooser
                          .setFileFilter(new FileFilter() {
                            public boolean accept(File f) {
                              return f != null
                                  && (f.isDirectory() || (f.isFile() && f
                                      .getName().endsWith(suffix)));
                            }
  
                            public String getDescription() {
                              return "";
                            }
                          });
                  }
                }
              }
            }
            if (parameter != null && component instanceof JMPFileChooser)
              ((JMPFileChooser) component).getFileTextField().setText(
                  parameter == null ? "" : ((File) parameter).toString());
          }
  
          public Object getParameter(Component component) {
            File f = ((JMPFileChooser) component).f;
            if (f != null && f.getParent() != null)
              ((JMPFileChooser) component).bb.put(
                  LoadFileDataSourceQueryChooser.FILE_CHOOSER_DIRECTORY_KEY,
                  f.getParent());
            return f;
          }
  
        }, Integer.class, new ParameterClassHandler() {
  
          public Component createEditComponent(Blackboard bb) {
            return new ValidatingTextField("", 5,
                ValidatingTextField.INTEGER_VALIDATOR);
          }
  
          public void setParameter(Component component, Object parameter) {
            ((JTextField) component).setText(((Integer) parameter).toString());
          }
  
          public Object getParameter(Component component) {
            return ((JTextField) component).getText().length() > 0 ? new Integer(
                ((JTextField) component).getText()) : null;
          }
        } });

    private JPanel mainPanel = new JPanel(new GridBagLayout());

    public ConnectionDescriptorPanel(Registry registry, final WorkbenchContext context) {
        super(new GridBagLayout());
        this.context = context;

        nameText = new JTextField("", 20);
        //nameText.setPreferredSize(new Dimension());

        driverComboBox = new JComboBox() {
            {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ParameterList oldParameterList = createParameterList();
                        schema = ((DataStoreDriver) getSelectedItem()).getParameterListSchema();
                        updateMainPanel(copyWherePossible(oldParameterList, schema),PersistentBlackboardPlugIn.get(context));
                    }
                });
                setRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list,
                            Object value, int index, boolean isSelected,
                            boolean cellHasFocus) {
                        return super.getListCellRendererComponent(list,
                                ((DataStoreDriver) value).getName(), index,
                                isSelected, cellHasFocus);
                    }
                });
            }
        };

        add(mainPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2), 0, 0));
        add(new JPanel(new GridBagLayout()), new GridBagConstraints(1, 1, 1, 1,
                1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        driverComboBox.setModel(new DefaultComboBoxModel(sort(new Vector(
                registry.getEntries(DataStoreDriver.REGISTRY_CLASSIFICATION)),
                new Comparator() {
                    public int compare(Object o1, Object o2) {
                        return compare((DataStoreDriver) o1,
                                (DataStoreDriver) o2);
                    }

                    public int compare(DataStoreDriver a, DataStoreDriver b) {
                        return a.getName().compareTo(b.getName());
                    }
                })));
        initializePreferredSize();
        driverComboBox.setSelectedIndex(0);
    }

    private void initializePreferredSize() {
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < driverComboBox.getItemCount(); i++) {
            driverComboBox.setSelectedIndex(i);
            maxWidth = Math.max(maxWidth, (int) getPreferredSize().getWidth());
            maxHeight = Math.max(maxHeight, (int) getPreferredSize()
                    .getHeight());
        }
        setPreferredSize(new Dimension(maxWidth, maxHeight));
    }

    private Vector sort(Vector collection, Comparator comparator) {
        Collections.sort(collection, comparator);
        return collection;
    }

    private String getValidConnectionName()
    {
      String fieldName = nameText.getText();
      if (fieldName == null || fieldName.trim().length() == 0) {
        return ((DataStoreDriver) driverComboBox.getSelectedItem()).getName() + " " + ++createdConnectionCount;
      }
      return fieldName.trim();
    }

    public ConnectionDescriptor getConnectionDescriptor() {
        return new ConnectionDescriptor(
            getValidConnectionName(),
            driverComboBox.getSelectedItem().getClass(),
            createParameterList());
    }

    private ParameterList createParameterList() {
        ParameterList parameterList = new ParameterList(schema);
        for (int i = 0; i < editComponentList.size(); i++) {
            parameterList.setParameter(schema.getNames()[i],
                    parameterClassHandler(schema.getNames()[i],
                            schema.getClasses()[i]).getParameter(
                            (Component) editComponentList.get(i)));
        }
        return parameterList;
    }

    public void setParameters(ConnectionDescriptor connDesc)
    {
      nameText.setText(connDesc.getName());
      int driverComboIndex = getDriverComboBoxIndex(connDesc.getDataStoreDriverClassName());
      if (driverComboIndex >= 0) {
        driverComboBox.setSelectedIndex(driverComboIndex);
        schema = ((DataStoreDriver) driverComboBox.getSelectedItem()).getParameterListSchema();
      }
      updateMainPanel(connDesc.getParameterList(), PersistentBlackboardPlugIn.get(context));
    }

    private int getDriverComboBoxIndex(String driverClassName)
    {
      for (int i = 0; i < driverComboBox.getItemCount(); i++) {
        DataStoreDriver driver = (DataStoreDriver) driverComboBox.getItemAt(i);
        if (driver.getClass().getName().equals(driverClassName)) {
          return i;
        }
      }
      return -1;
    }
/*
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame();
        frame.getContentPane().add(
                new ConnectionDescriptorPanel(new Registry().createEntry(
                        DataStoreDriver.REGISTRY_CLASSIFICATION,
                        new PostgisDataStoreDriver()),null));
        frame.pack();
        frame.setVisible(true);
    }
//*/

    public String validateInput() {
        for (int i = 0; i < editComponentList.size(); i++) {
            Object parameter = parameterClassHandler(schema.getNames()[i],
                    schema.getClasses()[i]).getParameter(
                    (Component) editComponentList.get(i));
            //TODO: nicolas ribot, 19 fev 2015: password is not required for some databases
            if (!"Password".equals(schema.getNames()[i]) && (parameter == null || parameter.equals(""))) {
                return (I18N.get("jump.workbench.ui.plugin.datastore.ConnectionDescriptorPanel.Required-field-missing") + " " + schema.getNames()[i]);
            }
        }
        return null;
    }

    private ParameterList copyWherePossible(ParameterList oldParameterList,
            ParameterListSchema newSchema) {
        ParameterList newParameterList = new ParameterList(newSchema);
        for (int i = 0; i < newSchema.getNames().length; i++) {
            String name = newSchema.getNames()[i];
            newParameterList.setParameter(name, oldParameterList.getSchema()
                    .isValidName(name)
                    && newSchema.getClasses()[i] == oldParameterList
                            .getSchema().getClass(name) ? oldParameterList
                    .getParameter(name) : null);
        }
        return newParameterList;
    }
}