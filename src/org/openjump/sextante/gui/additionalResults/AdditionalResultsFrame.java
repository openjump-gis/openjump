package org.openjump.sextante.gui.additionalResults;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.openjump.core.ui.swing.DetachableInternalFrame;
import org.openjump.sextante.core.ObjectAndDescription;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class AdditionalResultsFrame extends DetachableInternalFrame {

	/**
	 * Flexible generic frame for prompting the results in several objects. This
	 * frame is a refactoring of Sextante
	 * es.unex.sextante.gui.additionalResults.AdditionalResultsDialog from
	 * library SextanteGUI.jar. Methods to prompting AdditionalResultsFrame are
	 * located to the class AdditionalResults
	 * 
	 * @author Giuseppe Aruta [2017-12-12]
	 */

	// Main components of a AdditionalResultsFrame

	private final String name = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result-viewer");
	private final String sMenu = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Menu");
	private final String sRemove = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Remove");
	private final String sRename = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Rename");
	private final String sSave = I18N
			.get("deejump.plugin.SaveLegendPlugIn.Save");
	private final String sWriteName = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Write-name");
	private final String sChangeName = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Change-name");
	private final String sProcessing = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Processing");
	private final String sResult = I18N
			.get("org.openjump.sextante.gui.additionalResults.AdditionalResultsPlugIn.Result");
	private final String SAVE = I18N
			.get("deejump.plugin.SaveLegendPlugIn.Save");
	private final String CLOSE = I18N
			.get("ui.plugin.imagery.ImageLayerManagerDialog.Close");
	public final static String HEIGHT = I18N
			.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.values");
	public final static String WIDTH = I18N
			.get("org.openjump.core.ui.plugin.raster.ProfileGraphTool.2d-distance");

	private static final long serialVersionUID = 1L;
	private JSplitPane jSplitPane;
	private JTree jTree;
	private TreePath m_Path;
	private JScrollPane jScrollPane;
	private JMenuItem menuItemSave;
	private JPopupMenu popupMenu;
	private JMenuItem menuItemRemove;
	private JMenuItem menuItemRename;

	public JSplitPane getSplitPanel() {
		return jSplitPane;
	}

	// --da rimuovere
	// final protected OKCancelApplyPanel okCancelApplyPanel = new
	// OKCancelApplyPanel();

	public AdditionalResultsFrame(final ArrayList<?> components) {

		initGUI(components);
		setTitle(name);
		setResizable(true);
		setClosable(true);
		setIconifiable(true);
		setMaximizable(true);
		setPreferredSize(new Dimension(900, 700));
		setSize(900, 700);
		setLayer(JLayeredPane.MODAL_LAYER);
	}

	public Icon getColorIcon() {
		final ImageIcon icon = new ImageIcon(getClass().getResource(
				"application_view.png"));
		return GUIUtil.toSmallIcon(icon);
	}

	private boolean initGUI(final ArrayList<?> components) {

		final JPanel panel = new JPanel();
		final BorderLayout thisLayout = new BorderLayout();
		panel.setLayout(thisLayout);
		setContentPane(panel);

		if (components.size() == 0) {
			return false;
		}
		try {
			{
				setPreferredSize(new Dimension(900, 450));
				this.setSize(new Dimension(900, 450));
				{
					jSplitPane = new JSplitPane();
					jSplitPane.setDividerSize(9);
					jSplitPane.setContinuousLayout(true);
					jSplitPane.setOneTouchExpandable(true);
					jSplitPane.setDividerLocation(200);
					panel.add(jSplitPane, BorderLayout.CENTER);

					panel.add(getOKSavePanel(), BorderLayout.SOUTH);

					{
						jTree = new JTree();
						jTree.setCellRenderer(new AdditionalResultsTreeCellRenderer());

						final MouseListener ml = new MouseAdapter() {
							@Override
							public void mousePressed(MouseEvent e) {

								m_Path = jTree.getPathForLocation(e.getX(),
										e.getY());
								final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
										.getLastPathComponent();
								if (node.getUserObject() instanceof ObjectAndDescription) {
									showComponent();
									if ((e.getButton() == MouseEvent.BUTTON3)
											&& (m_Path != null)) {

										showPopupMenu(e);
									}
								}
							}
						};
						jTree.addMouseListener(ml);

						fillTree(components);

						if (components.size() > 0) {
							final DefaultMutableTreeNode node = findNode((ObjectAndDescription) components
									.get(components.size() - 1));
							final DefaultTreeModel model = (DefaultTreeModel) jTree
									.getModel();
							final TreePath path = new TreePath(
									model.getPathToRoot(node));
							jTree.setSelectionPath(path);
							jTree.scrollPathToVisible(path);
							m_Path = path;
							showComponent();
						}
						jScrollPane = new JScrollPane(
								jTree,
								ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
								ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

						jScrollPane.setPreferredSize(new Dimension(200, 450));
						// jScrollPane.setMinimumSize(new Dimension(200, 450));
						jScrollPane.setMaximumSize(new Dimension(200, 450));
					}
					{
						jSplitPane.add(jScrollPane, JSplitPane.LEFT);
					}
				}
			}

			popupMenu = new JPopupMenu(sMenu);
			menuItemSave = new JMenuItem(sSave);
			menuItemSave.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent evt) {
					if (m_Path != null) {
						try {
							final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
									.getLastPathComponent();
							if (node.getUserObject() instanceof ObjectAndDescription) {
								AdditionalResultsIO.save(m_Path);
							}
						} catch (final Exception e) {
						}
					}
				}
			});
			popupMenu.add(menuItemSave);
			menuItemRemove = new JMenuItem(sRemove);
			menuItemRemove.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent evt) {
					if (m_Path != null) {
						try {
							final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
									.getLastPathComponent();
							if (node.getUserObject() instanceof ObjectAndDescription) {
								remove();
							}
						} catch (final Exception e) {
						}
					}
					;
				}
			});
			popupMenu.add(menuItemRemove);
			menuItemRename = new JMenuItem(sRename);
			menuItemRename.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent evt) {
					if (m_Path != null) {
						try {
							final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
									.getLastPathComponent();
							if (node.getUserObject() instanceof ObjectAndDescription) {
								rename();
							}
						} catch (final Exception e) {
						}
					}
					;
				}

			});
			popupMenu.add(menuItemRename);

			panel.updateUI();
			return true;
		} catch (final Exception e) {
			Logger(this.getClass(), e);
			return false;
		}

	}

	public Component getLeftPanel() {
		return jSplitPane.getLeftComponent();

	}

	public Component getRightPanel() {
		return jSplitPane.getRightComponent();
	}

	public JPopupMenu getPopupMen() {
		return popupMenu;
	}

	protected void showPopupMenu(final MouseEvent e) {
		jTree.setSelectionPath(m_Path);

		menuItemSave.setEnabled(true);
		popupMenu.show(e.getComponent(), e.getX(), e.getY());

	}

	private void rename() {

		if (m_Path != null) {
			try {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
						.getLastPathComponent();
				final ObjectAndDescription oad = (ObjectAndDescription) node
						.getUserObject();
				final String sName = oad.getDescription();
				final JOptionPane pane = new JOptionPane();
				pane.setMessage(sWriteName);
				pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
				pane.setWantsInput(true);
				pane.setInitialSelectionValue(sName);
				pane.setInputValue(sName);
				final JDialog dlg = pane.createDialog(null, sChangeName);
				dlg.setModal(true);
				dlg.setVisible(true);
				final String sNewName = pane.getInputValue().toString().trim();

				if ((sNewName != null) && (sNewName.length() != 0)) {
					oad.setDescription(sNewName);
				}
				update();
			} catch (final Exception e) {
			}
		}

	}

	protected void remove() {
		if (m_Path != null) {
			try {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
						.getLastPathComponent();
				final ObjectAndDescription oad = (ObjectAndDescription) node
						.getUserObject();
				AdditionalResults.removeComponent(oad);
				update();
			} catch (final Exception e) {
			}
		}

	}

	protected void showComponent() {
		if (m_Path != null) {
			try {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) m_Path
						.getLastPathComponent();
				final ObjectAndDescription oad = (ObjectAndDescription) node
						.getUserObject();
				final Component c = (Component) oad.getObject();
				c.setMinimumSize(new Dimension(300, 200));
				jSplitPane.setRightComponent(c);
			} catch (final Exception e) {
				Logger(this.getClass(), e);
			}
		}
	}

	public void fillTree(final ArrayList<?> components) {
		DefaultMutableTreeNode node;
		final DefaultMutableTreeNode mainNode = new DefaultMutableTreeNode(
				sProcessing);
		final DefaultMutableTreeNode componentsNode = new DefaultMutableTreeNode(
				sResult);

		for (int i = 0; i < components.size(); i++) {
			node = new DefaultMutableTreeNode(components.get(i));
			componentsNode.add(node);
		}
		mainNode.add(componentsNode);
		jTree.setModel(new DefaultTreeModel(mainNode));
	}

	public void update() {
		if (!initGUI(AdditionalResults.getComponents())) {
			dispose();
			setVisible(false);
		}
	}

	private DefaultMutableTreeNode findNode(final ObjectAndDescription oad) {
		Object ob;
		final DefaultTreeModel data = (DefaultTreeModel) jTree.getModel();
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) data
				.getRoot();
		DefaultMutableTreeNode node = null;
		if (root != null) {
			for (final Enumeration<?> e = root.breadthFirstEnumeration(); e
					.hasMoreElements();) {
				final DefaultMutableTreeNode current = (DefaultMutableTreeNode) e
						.nextElement();
				ob = current.getUserObject();
				if (ob instanceof ObjectAndDescription) {
					if (ob == oad) {
						node = current;
						break;
					}
				}
			}
		}
		return node;
	}

	public static void Logger(Class<?> plugin, Exception e) {
		final Logger LOG = Logger.getLogger(plugin);
		JUMPWorkbench
		.getInstance()
		.getFrame()
		.warnUser(
				plugin.getSimpleName() + " Exception: " + e.toString());
		LOG.error(plugin.getName() + " Exception: ", e);
	}

	protected JPanel getOKSavePanel() {
		final JPanel okPanel = new JPanel();
		final JButton saveButton = new JButton(SAVE) {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(100, 25);
			}
		};
		final JButton closeButton = new JButton(CLOSE) {
			private static final long serialVersionUID = 2L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(100, 25);
			}
		};

		saveButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AdditionalResultsIO.save(m_Path);
				return;
			}
		});
		closeButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				dispose();

				return;
			}
		});
		okPanel.add(saveButton, BorderLayout.WEST);
		okPanel.add(closeButton, BorderLayout.EAST);
		return okPanel;

	}





	public static JUMPWorkbench workbench = JUMPWorkbench
			.getInstance();

}
