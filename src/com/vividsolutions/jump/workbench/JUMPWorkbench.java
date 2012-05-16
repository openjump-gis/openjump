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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.openjump.OpenJumpConfiguration;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
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

	private static ArrayList<Image> appIcons(){
		ArrayList iconlist = new ArrayList();
	    iconlist.add(IconLoader.image("oj_16_Kplain2oj.png"));
	    iconlist.add(IconLoader.image("oj_24.png"));
	    iconlist.add(IconLoader.image("oj_32.png"));
	    iconlist.add(IconLoader.image("oj_48.png"));
	    iconlist.add(IconLoader.image("oj_256.png"));
	    //java.util.Collections.reverse(iconlist);
	    return iconlist;
	}
	
	// for java 1.5-
	public static final ImageIcon APP_ICON  = IconLoader.icon("app-icon.gif");
	// for java 1.6+
	public static final ArrayList APP_ICONS = appIcons();
	
	//public static final String VERSION_TEXT = I18N.get("JUMPWorkbench.version.number");
	//-- dont change the following strings 
	public final static String PROPERTIES_OPTION = "properties";
	public final static String DEFAULT_PLUGINS = "default-plugins";
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
	private static CommandLine commandLine;
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
	 * @param o
	 *                  a window to decorate with icon
	 */
	public static void setIcon ( Object o ) {
		// attach the right icon, depending on 
		//  - availability of method setIconImages (java 1.5 vs. 1.6), several icons for different sizes
		//  - underlying object type (JFrame, JInternalFrame, others? )
		// let's go
		if ( o instanceof JFrame ) {
			JFrame f = (JFrame) o;

			try{
				// case java 1.6+
			    Class[] types = {java.util.List.class};
			    java.lang.reflect.Method method = 
			        JFrame.class.getMethod("setIconImages",types);
			
			    Object[] params = {APP_ICONS};
			    method.invoke( f,params );
			    //System.out.println("jep");
			
			}catch( Exception e ) {
				// case java 1.5-, is really bad with transparent pngs, so we stick with the old gif
			    f.setIconImage((Image)APP_ICON.getImage());
			    //System.out.println("noe");
			}
		}
		else if ( o instanceof javax.swing.JInternalFrame ) {
			//System.out.println("internal");
			javax.swing.JInternalFrame f = (javax.swing.JInternalFrame) o;
			f.setFrameIcon(getIcon());
		}
	}

	private static ImageIcon icon ;
	
	public static ImageIcon getIcon(){
		// java 1.5 is really bad with transparent pngs, so we stick with the old gif
		if ( ! ( icon instanceof ImageIcon ) ) {
			Double jre_version = Double.parseDouble( System.getProperty("java.version").substring(0,3) );
			if ( jre_version < 1.6 ) {
				icon = APP_ICON;
			} else {
				icon = new ImageIcon();
				icon.setImage((Image)APP_ICONS.get(0));
			}
		}
		return icon;
	}
	
	/**
	 * @param s
	 *                  a visible SplashWindow to close when initialization is
	 *                  complete and the WorkbenchFrame is opened
	 */
	public JUMPWorkbench(String title, String[] args,
			final JWindow s, TaskMonitor monitor) throws Exception {

		frame = new WorkbenchFrame(title, context);
		frame.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				s.setVisible(false);
			}
		});
		
		boolean defaultFileExists = false; //[sstein 6.July.2008] new
		File defaultFile = null;
		if (commandLine.hasOption(DEFAULT_PLUGINS)) {
			defaultFile = new File(commandLine.getOption(
					DEFAULT_PLUGINS).getArg(0));
			if (defaultFile.exists()) {
				defaultFileExists = true;
				//[sstein 6.July.2008] disabled to enable loading of two properties files
				//properties = new WorkbenchPropertiesFile(defaultFile, frame);
			} else {
				System.out.println("JUMP: Warning: Default plugins file does not exist: "
								+ defaultFile);
			}
		}
		boolean propertiesFileExists = false; //[sstein 6.July.2008] new
		File propertiesFile = null;
		if (commandLine.hasOption(PROPERTIES_OPTION)) {
			propertiesFile = new File(commandLine.getOption(
					PROPERTIES_OPTION).getArg(0));
			if (propertiesFile.exists()) {
				//[sstein 6.July.2008] disabled to enable loading of two properties files
				//properties = new WorkbenchPropertiesFile(propertiesFile, frame);
				propertiesFileExists = true;
			} else {
				System.out.println("JUMP: Warning: Properties file does not exist: "
								+ propertiesFile);
			}
		}
		//-- [sstein 6.July.2008] start new
		if((defaultFileExists) && (propertiesFileExists)){
			properties = new WorkbenchPropertiesFile(defaultFile, propertiesFile, frame);
		}
		else if(defaultFileExists){
			properties = new WorkbenchPropertiesFile(defaultFile, frame);
		}
		else if(propertiesFileExists){
			properties = new WorkbenchPropertiesFile(propertiesFile, frame);
		}
		//-- end new
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
    long start = PlugInManager.secondsSince(0);
    try {
      // first fetch parameters, locale might be changed with -i18n switch
      parseCommandLine(args);
      // load i18n specified in command line ( '-i18n translation' )
      if (commandLine.hasOption(I18N_FILE)) {
        I18N_SETLOCALE = commandLine.getOption(I18N_FILE).getArg(0);
        // initialize I18N
        I18N.loadFile(I18N_SETLOCALE);
      }

      // Init the L&F before instantiating the progress monitor [Jon Aquino]
      initLookAndFeel();
      // setFont to switch fonts if defaults cannot display current language
      // [ede]
      // early change the default font definition of the jre if necessary, the first
      // internationalized string shown is 'JUMPWorkbench.version' on splashpanel
      setFont();

      ProgressMonitor progressMonitor = (ProgressMonitor) progressMonitorClass
          .newInstance();
      SplashPanel splashPanel = new SplashPanel(splashImage(),
          I18N.get("ui.AboutDialog.version") + " "
              + JUMPVersion.CURRENT_VERSION);
      splashPanel.add(progressMonitor, new GridBagConstraints(0, 10, 1, 1, 1,
          0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 10), 0, 0));

      main(args, I18N.get("JUMPWorkbench.jump"), new JUMPConfiguration(),
          splashPanel, progressMonitor);
      System.out.println("OJ start took " +PlugInManager.secondsSince(start)+ "s alltogether.");
    } catch (Throwable t) {
      WorkbenchFrame.showThrowable(t, null);
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
			// disabled by ede 09/2011
			//System.setProperty("org.xml.sax.driver","org.apache.xerces.parsers.SAXParser");  	
			
			// already initialized in main() above [ede]
			//initLookAndFeel();
			SplashWindow splashWindow = new SplashWindow(splashComponent);
			splashWindow.setVisible(true);

			taskMonitor.report(I18N.get("JUMPWorkbench.status.create"));
			JUMPWorkbench workbench = new JUMPWorkbench(title, args, splashWindow, taskMonitor);

			taskMonitor.report(I18N.get("JUMPWorkbench.status.configure-core"));
			setup.setup(workbench.context);
			//must wait until after setup initializes the persistent blackboard to recall settings
			WorkbenchFrame frame = workbench.getFrame();
			taskMonitor.report(I18N.get("JUMPWorkbench.status.restore-state"));
			frame.restore();

			taskMonitor.report(I18N.get("JUMPWorkbench.status.load-extensions"));
			workbench.context.getWorkbench().getPlugInManager().load();
			OpenJumpConfiguration.postExtensionInitialization(workbench.context);
			workbench.getFrame().setVisible(true);
		} catch (Throwable t) {
			WorkbenchFrame.showThrowable(t, null);
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

	// this is in preparation that we might want to support more fonts in the
	// future
	private static Font[] loadFonts() throws Exception {
		Font font = Font.createFont(Font.TRUETYPE_FONT, Class.class.getClass()
				.getResource("/language/fonts/code2000.ttf").openStream());
		// since 1.6 we could register the font and use it by name
		// but using the font directly makes us 1.5 compatible
		// GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont( font
		// );
		return new Font[] { font };
	}

	private static boolean setFont() throws Exception {
		String test = I18N.get("ui.MenuNames.FILE");
		boolean replaced = false;
		Font font = null;

		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			// loop over fontuires entries
			if (value instanceof javax.swing.plaf.FontUIResource) {
				FontUIResource fold = ((javax.swing.plaf.FontUIResource) value);

				// can default font display test sentence?
				if (fold.canDisplayUpTo(test) != -1) {

					// fetch replacement candidate
					if (font == null)
						font = loadFonts()[0];

					// copy attributes
					Map attrs = fold.getAttributes();
					// remove family attribute
					java.text.AttributedCharacterIterator.Attribute fam = null;
					for (Iterator iterator = attrs.keySet().iterator(); iterator
							.hasNext();) {
						fam = (java.text.AttributedCharacterIterator.Attribute) iterator
								.next();
						if (fam.toString().equals(
								"java.awt.font.TextAttribute(family)")) {
							break;
						}
					}
					if (fam != null)
						attrs.remove(fam);
					// create the new fontuires
					FontUIResource fnew = new javax.swing.plaf.FontUIResource(
							font.deriveFont(attrs));

					// check if new font can display and set
					if (fnew.canDisplayUpTo(test) == -1) {
						UIManager.put(key, fnew);
						replaced = true;
					}
				}
			}

		}

		/*
		 * // show all registered available fonts GraphicsEnvironment e =
		 * GraphicsEnvironment.getLocalGraphicsEnvironment(); for (String foo :
		 * e.getAvailableFontFamilyNames()){ System.out.println(foo); }
		 */

		// replaced any?
		return replaced;
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

	private static void parseCommandLine(String[] args) throws WorkbenchException {
		//<<TODO:QUESTION>> Notify MD: using CommandLine [Jon Aquino]
		commandLine = new CommandLine('-');
		commandLine.addOptionSpec(new OptionSpec(PROPERTIES_OPTION, 1));
		commandLine.addOptionSpec(new OptionSpec(DEFAULT_PLUGINS, 1));
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

	private static void addProperties(WorkbenchProperties oldProperties,  WorkbenchProperties newProperties) throws Exception{
		oldProperties.getPlugInClasses().addAll(newProperties.getPlugInClasses());			
		oldProperties.getInputDriverClasses().addAll(newProperties.getInputDriverClasses());	
		oldProperties.getOutputDriverClasses().addAll(newProperties.getOutputDriverClasses());	
		oldProperties.getConfigurationClasses().addAll(newProperties.getConfigurationClasses());
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