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
package com.vividsolutions.jump.workbench;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.commandline.CommandLine;
import com.vividsolutions.jump.util.commandline.OptionSpec;
import com.vividsolutions.jump.util.commandline.ParseException;
import com.vividsolutions.jump.workbench.driver.DriverManager;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.ui.SplashPanel;
import com.vividsolutions.jump.workbench.ui.SplashWindow;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.openjump.OpenJumpConfiguration;

/**
 * This class is responsible for setting up and displaying the main JUMP
 * workbench window.
 */

public class JUMPWorkbench {
	private static ImageIcon splashImage;
	public static ImageIcon splashImage() {
		// Lazily initialize it, as it may not even be called (e.g. EZiLink),
		// and we want the splash screen to appear ASAP [Jon Aquino]
		if (splashImage == null) {
			splashImage = IconLoader.icon("splash.png");
			//splashImage = IconLoader.icon(I18N.get("splash.png"));
		}
		return splashImage;
	}

	private static final ImageIcon APP_ICON = IconLoader.icon("app-icon.gif");
	public static final String VERSION_TEXT = I18N.get("JUMPWorkbench.version.number");
	//-- dont change the following strings 
	public final static String PROPERTIES_OPTION = "properties";
	public final static String PLUG_IN_DIRECTORY_OPTION = "plug-in-directory";
	public final static String I18N_FILE = "i18n";
	public static final String INITIAL_PROJECT_FILE = "project";
    public static final String STATE_OPTION = "state";
	
	// Added by STanner to allow I18N to have access to this
	public static String I18N_SETLOCALE = "";
	
	private static Class progressMonitorClass = SingleLineProgressMonitor.class;

	//<<TODO:REFACTORING>> Move images package under
	// com.vividsolutions.jump.workbench
	//to avoid naming conflicts with other libraries. [Jon Aquino]
	private CommandLine commandLine;
	private WorkbenchContext context = new JUMPWorkbenchContext(this);
	private WorkbenchFrame frame;
	private DriverManager driverManager = new DriverManager(frame);
	private WorkbenchProperties dummyProperties = new WorkbenchProperties() {
		public List getPlugInClasses() {
			return new ArrayList();
		}
        
        public List getPlugInClasses(ClassLoader classLoader) {
			return new ArrayList();
		}

		public List getInputDriverClasses() {
			return new ArrayList();
		}

		public List getOutputDriverClasses() {
			return new ArrayList();
		}

		public List getConfigurationClasses() {
			return new ArrayList();
		}
	};

	private WorkbenchProperties properties = dummyProperties;
	private PlugInManager plugInManager;
	private Blackboard blackboard = new Blackboard();

	/**
	 * @param s
	 *                  a visible SplashWindow to close when initialization is
	 *                  complete and the WorkbenchFrame is opened
	 */
	public JUMPWorkbench(String title, String[] args, ImageIcon icon,
			final JWindow s, TaskMonitor monitor) throws Exception {
		parseCommandLine(args);
		// load i18n specified in command line ( '-i18n translation' )
		if (commandLine.hasOption(I18N_FILE)) {
			I18N.loadFile(commandLine.getOption(I18N_FILE).getArg(0));
			I18N_SETLOCALE = commandLine.getOption(I18N_FILE).getArg(0);
		}
		frame = new WorkbenchFrame(title, icon, context);
		frame.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				s.setVisible(false);
			}
		});
		
		if (commandLine.hasOption(PROPERTIES_OPTION)) {
			File propertiesFile = new File(commandLine.getOption(
					PROPERTIES_OPTION).getArg(0));
			if (propertiesFile.exists()) {
				properties = new WorkbenchPropertiesFile(propertiesFile, frame);
			} else {
				System.out.println("JUMP: Warning: Properties file does not exist: "
								+ propertiesFile);
			}
		}

		File extensionsDirectory = null;
		if (commandLine.hasOption(PLUG_IN_DIRECTORY_OPTION)) {
			extensionsDirectory = new File(commandLine.getOption(
					PLUG_IN_DIRECTORY_OPTION).getArg(0));
			if (!extensionsDirectory.exists()) {
				System.out
						.println("JUMP: Warning: Extensions directory does not exist: "
								+ extensionsDirectory);
				extensionsDirectory = null;
			}
		} else {
			extensionsDirectory = new File("../lib/ext");
			if (!extensionsDirectory.exists()) {
				// Added further information so that debug user will know where 
				// it is actually looking for as the extension directory. [Ed Deen]
				System.out
				.println("JUMP: Warning: Extensions directory does not exist: "
						+ extensionsDirectory 
						+ " where homedir = [" + System.getProperty("user.dir") + "]");
				extensionsDirectory = null;
			}
		}
		if (commandLine.hasOption( INITIAL_PROJECT_FILE )) {
		    String task = commandLine.getOption( INITIAL_PROJECT_FILE ).getArg(0);
		    this.getBlackboard().put( INITIAL_PROJECT_FILE, task );
		}
		
        if(commandLine.hasOption(STATE_OPTION)) {
            File option = new File(commandLine.getOption(STATE_OPTION).getArg(0));
            if(option.isDirectory()) {
                PersistentBlackboardPlugIn.setPersistenceDirectory(option.getPath());
            }
            if(option.isFile()) {
                PersistentBlackboardPlugIn.setFileName(option.getName());
                PersistentBlackboardPlugIn.setPersistenceDirectory(option.getAbsoluteFile().getParent());
            }
        }
        
		plugInManager = new PlugInManager(context, extensionsDirectory, monitor);

		//Load drivers before initializing the frame because part of the frame
		//initialization is the initialization of the driver dialogs. [Jon
		// Aquino]
		//The initialization of some plug-ins (e.g. LoadDatasetPlugIn) requires
		// that
		//the drivers be loaded. Thus load the drivers here, before the
		// plug-ins
		//are initialized.
		driverManager.loadDrivers(properties);
	}

	public static void main(String[] args) {
		try {
			//Init the L&F before instantiating the progress monitor [Jon
			// Aquino]
			initLookAndFeel();
			ProgressMonitor progressMonitor = (ProgressMonitor) progressMonitorClass
					.newInstance();
			SplashPanel splashPanel = new SplashPanel(splashImage(), 
									I18N.get("JUMPWorkbench.version")+" "
									+ VERSION_TEXT /*+ JUMPVersion.CURRENT_VERSION*/); //[sstein] added but disabled JumpVersion
			splashPanel.add(progressMonitor, new GridBagConstraints(0, 10, 1,
					1, 1, 0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0,
					0));

			main(args, I18N.get("JUMPWorkbench.jump"), new JUMPConfiguration(), splashPanel,
					progressMonitor);
		} catch (Throwable t) {
			WorkbenchFrame.handleThrowable(t, null);
		}
	}

	/**
	 * setupClass is specified as a String to prevent it from being loaded
	 * before we display the splash screen, in case setupClass takes a long time
	 * to load.
	 * @param args main application arguments
	 * @param title application title
	 * @param setup an object implementing the Setup interface
	 *                 (e.g. JUMPConfiguration)
	 * @param splashComponent
	 *                  a component to open until the workbench frame is displayed
	 * @param taskMonitor
	 *                  notified of progress of plug-in loading
	 */
	public static void main(String[] args, String title, Setup setup,
			JComponent splashComponent, TaskMonitor taskMonitor) {
		try {
			//I don't know if we still need to specify the SAX driver [Jon
			// Aquino 10/30/2003]
			System.setProperty("org.xml.sax.driver",
					"org.apache.xerces.parsers.SAXParser");

			initLookAndFeel();
			SplashWindow splashWindow = new SplashWindow(splashComponent);
			splashWindow.setVisible(true);

			JUMPWorkbench workbench = new JUMPWorkbench(title, args, APP_ICON,
					splashWindow, taskMonitor);

			setup.setup(workbench.context);

			workbench.context.getWorkbench().getPlugInManager().load();
      OpenJumpConfiguration.postExtensionInitialization(workbench.context);
			workbench.getFrame().setVisible(true);
		} catch (Throwable t) {
			WorkbenchFrame.handleThrowable(t, null);
		}
	}

	private static void initLookAndFeel() throws Exception {
		if (LangUtil.ifNull(System.getProperty("initLookAndFeel"), "true")
				.toString().equalsIgnoreCase("false")) {
			return;
		}
		//Apple stuff from Raj Singh's startup script [Jon Aquino 10/30/2003]
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.showGrowBox", "true");
		if (UIManager.getLookAndFeel() != null
				&& UIManager.getLookAndFeel().getClass().getName().equals(
						UIManager.getSystemLookAndFeelClassName())) {
			return;
		}
		String laf = System.getProperty( "swing.defaultlaf" );
		if ( laf == null ){
		    laf = UIManager.getSystemLookAndFeelClassName();
		}
		UIManager.setLookAndFeel( laf );
	}

	public DriverManager getDriverManager() {
		return driverManager;
	}

	/**
	 * The properties file; not to be confused with the WorkbenchContext
	 * properties.
	 */
	public WorkbenchProperties getProperties() {
		return properties;
	}

	public WorkbenchFrame getFrame() {
		return frame;
	}

	public WorkbenchContext getContext() {
		return context;
	}

	private void parseCommandLine(String[] args) throws WorkbenchException {
		//<<TODO:QUESTION>> Notify MD: using CommandLine [Jon Aquino]
		commandLine = new CommandLine('-');
		commandLine.addOptionSpec(new OptionSpec(PROPERTIES_OPTION, 1));
		commandLine.addOptionSpec(new OptionSpec(PLUG_IN_DIRECTORY_OPTION, 1));
		commandLine.addOptionSpec(new OptionSpec(I18N_FILE, 1));
		//[UT] 17.08.2005 
		commandLine.addOptionSpec(new OptionSpec( INITIAL_PROJECT_FILE, 1));
        commandLine.addOptionSpec(new OptionSpec(STATE_OPTION, 1));
		
		try {
			commandLine.parse(args);
		} catch (ParseException e) {
			throw new WorkbenchException(
					"A problem occurred parsing the command line: "
							+ e.toString());
		}
	}

	public PlugInManager getPlugInManager() {
		return plugInManager;
	}

	//<<TODO>> Make some properties persistent using a #makePersistent(key)
	// method. [Jon Aquino]
	/**
	 * Expensive data structures can be cached on the blackboard so that several
	 * plug-ins can share them.
	 */
	public Blackboard getBlackboard() {
		return blackboard;
	}

	private static abstract class ProgressMonitor extends JPanel
			implements
				TaskMonitor {
		private Component component;

		public ProgressMonitor(Component component) {
			this.component = component;
			setLayout(new BorderLayout());
			add(component, BorderLayout.CENTER);
			setOpaque(false);
		}

		protected Component getComponent() {
			return component;
		}

		protected abstract void addText(String s);

		public void report(String description) {
			addText(description);
		}

		public void report(int itemsDone, int totalItems, String itemDescription) {
			addText(itemsDone + " / " + totalItems + " " + itemDescription);
		}

		public void report(Exception exception) {
			addText(StringUtil.toFriendlyName(exception.getClass().getName()));
		}

		public void allowCancellationRequests() {
		}

		public boolean isCancelRequested() {
			return false;
		}
	}

	private static class VerticallyScrollingProgressMonitor
			extends
				ProgressMonitor {
		private static int ROWS = 3;
		private JLabel[] labels;

		public VerticallyScrollingProgressMonitor() {
			super(new JPanel(new GridLayout(ROWS, 1)));

			JPanel panel = (JPanel) getComponent();
			panel.setOpaque(false);
			labels = new JLabel[ROWS];

			for (int i = 0; i < ROWS; i++) {
				//" " not "", to give the label some height. [Jon Aquino]
				labels[i] = new JLabel(" ");
				labels[i].setFont(labels[i].getFont().deriveFont(Font.BOLD));
				panel.add(labels[i]);
			}
		}

		protected void addText(String s) {
			for (int i = 0; i < (ROWS - 1); i++) { //-1
				labels[i].setText(labels[i + 1].getText());
			}

			labels[ROWS - 1].setText(s);
		}
	}

	private static class SingleLineProgressMonitor extends ProgressMonitor {
		public SingleLineProgressMonitor() {
			super(new JLabel(" "));
			((JLabel) getComponent()).setFont(((JLabel) getComponent())
					.getFont().deriveFont(Font.BOLD));
			((JLabel) getComponent()).setHorizontalAlignment(JLabel.LEFT);
		}

		protected void addText(String s) {
			((JLabel) getComponent()).setText(s);
		}
	}

	private static class HorizontallyScrollingProgressMonitor
			extends
				ProgressMonitor {
		private static final String BUFFER = "   ";

		public HorizontallyScrollingProgressMonitor() {
			super(new JLabel(" "));
			((JLabel) getComponent()).setFont(((JLabel) getComponent())
					.getFont().deriveFont(Font.BOLD));
			((JLabel) getComponent()).setHorizontalAlignment(JLabel.RIGHT);
		}

		protected void addText(String s) {
			((JLabel) getComponent()).setText(BUFFER + s
					+ ((JLabel) getComponent()).getText());
		}
	}
}