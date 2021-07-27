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

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.JUMPVersion;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.Blackboard;
import com.vividsolutions.jump.util.LangUtil;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.util.Timer;
import com.vividsolutions.jump.util.commandline.CommandLine;
import com.vividsolutions.jump.util.commandline.Param;
import com.vividsolutions.jump.util.commandline.ParamSpec;
import com.vividsolutions.jump.util.commandline.ParseException;
import com.vividsolutions.jump.workbench.driver.DriverManager;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;
import com.vividsolutions.jump.workbench.ui.ErrorDialog;
import com.vividsolutions.jump.workbench.ui.SplashPanelV2;
import com.vividsolutions.jump.workbench.ui.SplashWindow;
import com.vividsolutions.jump.workbench.ui.WorkbenchFrame;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;
import com.vividsolutions.jump.workbench.ui.network.ProxySettingsOptionsPanel;
import com.vividsolutions.jump.workbench.ui.plugin.PersistentBlackboardPlugIn;
import com.vividsolutions.jump.workbench.ui.plugin.skin.InstallSkinsPlugIn;

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
      splashImage = IconLoader.icon("splash7.png");
    }
    return splashImage;
  }

  private static ArrayList<Image> appIcons() {
    ArrayList<Image> iconlist = new ArrayList<>();

    // let's get festive
    if (SplashPanelV2.itsThatTimeAgain()) {
      ImageIcon ovl = IconLoader.icon("hat.png");
      // seems like win7 wants even smaller one for internal frames
      iconlist.add(SplashPanelV2.gimmick(
          SplashPanelV2.gimmick(new ImageIcon(new BufferedImage(14, 14,
              BufferedImage.TYPE_INT_ARGB)), ovl, 0.48f, -1, -1),
          IconLoader.icon("oj_16_bottomright.png"), 1f, -2, -2).getImage());
      iconlist.add(SplashPanelV2.gimmick(
          SplashPanelV2.gimmick(new ImageIcon(new BufferedImage(16, 16,
              BufferedImage.TYPE_INT_ARGB)), ovl, 0.54f, -1, -1),
          IconLoader.icon("oj_16_bottomright.png"), 1f, 0, 0).getImage());
      iconlist.add(SplashPanelV2.gimmick(IconLoader.icon("oj_24.png"), ovl,
          .12f, 19, 10).getImage());
      iconlist.add(SplashPanelV2.gimmick(IconLoader.icon("oj_32.png"), ovl,
          .15f, 26, 14).getImage());
      iconlist.add(SplashPanelV2.gimmick(IconLoader.icon("oj_48.png"), ovl,
          .25f, 37, 21).getImage());
      iconlist.add(SplashPanelV2.gimmick(IconLoader.icon("oj_256.png"), ovl,
          1.4f, 194, 113).getImage());

    } else {
      // seems like win7 wants even smaller one for internal frames
      // so let's use just another different one
      iconlist.add(IconLoader.image("oj_kang_14.v2.png"));
      // that's the default for window icons
      iconlist.add(IconLoader.image("oj_16_Kplain2oj.png"));
      iconlist.add(IconLoader.image("oj_24.png"));
      iconlist.add(IconLoader.image("oj_32.png"));
      iconlist.add(IconLoader.image("oj_48.png"));
      iconlist.add(IconLoader.image("oj_256.png"));
    }

    return iconlist;
  }

  // icons for frame and desktop purposes eg. alt+tab listing
  public static final ArrayList<Image> APP_ICONS = appIcons();
  public static final ImageIcon APP_ICON = new ImageIcon(APP_ICONS.get(0));
  
  // -- don't change the following strings, they define command line option names
  public static final String PROPERTIES_OPTION = "properties";
  public static final String DEFAULT_PLUGINS = "default-plugins";
  public static final String EXTS_DIRECTORY_OPTION = "extensions-directory";
  public static final String LIMIT_LOOKUP_OPTION = "limit-ext-lookup";
  public static final String JARS_DIRECTORY_OPTION = "jars-directory";
  public static final String I18N_FILE = "i18n";
  public static final String I18NPREFIX = JUMPWorkbench.class.getPackage().getName()+".";
  public static final String STATE_OPTION = "state";

  // Added by STanner to allow I18N to have access to this
  public static String I18N_SETLOCALE = "";

  private static final Class<?> progressMonitorClass = SingleLineProgressMonitor.class;

  // <<TODO:REFACTORING>> Move images package under
  // com.vividsolutions.jump.workbench
  // to avoid naming conflicts with other libraries. [Jon Aquino]
  private static JUMPWorkbench workbench;
  private static CommandLine commandLine;
  private final WorkbenchContext context = new JUMPWorkbenchContext(this);
  private WorkbenchFrame frame;
  private final DriverManager driverManager = new DriverManager(frame);
  private final WorkbenchProperties properties;
  private final PlugInManager plugInManager;
  private final Blackboard blackboard = new Blackboard();

  /**
   * @param o a window to decorate with icon
   */
  public static void setIcon(Object o) {
    // attach the right icon, depending on
    // - availability of method setIconImages (java 1.5 vs. 1.6),
    // several icons for different sizes
    // - underlying object type (JFrame, JInternalFrame, others? )
    // let's go
    if (o instanceof JFrame) {
      JFrame frame = (JFrame) o;
      try {
        // case java 1.6+
        Class<?>[] types = { List.class };
        Method method = JFrame.class.getMethod("setIconImages", types);

        Object[] params = { APP_ICONS };
        method.invoke(frame, params);
      } catch (Exception e) {
        // case java 1.5-, is really bad with transparent pngs, so we stick with
        // the old gif
        frame.setIconImage(APP_ICON.getImage());
      }
    } else if (o instanceof JInternalFrame) {
      JInternalFrame f = (JInternalFrame) o;
      f.setFrameIcon(getIcon());
    }
  }

  /**
   * getter for the frame icon
   */
  public static ImageIcon getIcon() {
    return APP_ICON;
  }

  /**
   * @param s a visible SplashWindow to close when initialization is complete and
   *         the WorkbenchFrame is opened
   */
  public JUMPWorkbench(String title, final Component s, TaskMonitor monitor) throws Exception {

    frame = new WorkbenchFrame(title, context);
    frame.addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        s.setVisible(false);
      }
    });

    boolean defaultFileExists = false; // [sstein 6.July.2008] new
    File defaultFile = null;
    if (commandLine.hasParam(DEFAULT_PLUGINS)) {
      defaultFile = new File(commandLine.getParam(DEFAULT_PLUGINS).getArg(0));
      if (defaultFile.exists()) {
        defaultFileExists = true;
        // [sstein 6.July.2008] disabled to enable loading of two properties
        // files
        // properties = new WorkbenchPropertiesFile(defaultFile, frame);
      } else {
        Logger.warn("Default plugins file does not exist: "
                + defaultFile);
      }
    }
    boolean propertiesFileExists = false; // [sstein 6.July.2008] new
    File propertiesFile = null;
    if (commandLine.hasParam(PROPERTIES_OPTION)) {
      propertiesFile = new File(commandLine.getParam(PROPERTIES_OPTION)
          .getArg(0));
      if (propertiesFile.exists()) {
        propertiesFileExists = true;
      } else {
        Logger.warn("Properties file does not exist: " + propertiesFile);
      }
    }
    
    // list the properties files to use
    List<File> files = new ArrayList<>();
    if (defaultFileExists)
      files.add(defaultFile);
    if (propertiesFileExists)
      files.add(propertiesFile);
    // build our wbprops from the files
    properties = new WorkbenchPropertiesFile(files, frame);
    
    // -- end new
    
//    File extensionsDirectory = null;
//    List<File> moreDirs = new ArrayList<>();
//    if (commandLine.hasOption(PLUG_IN_DIRECTORY_OPTION)) {
//      // we support multiple -plug-in-directory definitions, where the first is set as default (for mainly for )
//      // and all others and contained jar/zip files get added to classpath below
//      // this mainly helps when run during development where lib/plus/ & lib/ext/ are different folders
//      Iterator<String> paths = commandLine.getAllArguments(PLUG_IN_DIRECTORY_OPTION);
//
//      while (paths.hasNext()) {
//        String path = paths.next();
//        if (extensionsDirectory == null) {
//          // first entry get's default
//          extensionsDirectory = new File(path);
//          Logger.debug("Set plugin-dir -> "+path);
//          continue;
//        }
//        // rest get's added to classloader
//        File dir = new File(path);
//        if (!dir.exists()) {
//          Logger.error("given parameter "+PLUG_IN_DIRECTORY_OPTION+" '"+path+"' does not exist.");
//          continue;
//        }
//        Logger.debug("Add plugin-dir -> "+path);
//        moreDirs.add(dir);
//      }
//    } else {
//      // add default "lib/ext/"
//      extensionsDirectory = new File("lib/ext");
//    }

//    if (extensionsDirectory != null && !extensionsDirectory.exists()) {
//      // Added further information so that debug user will know where
//      // it is actually looking for as the extension directory. [Ed Deen]
//      Logger.error("Extensions directory does not exist: "
//              + extensionsDirectory + " where homedir = ["
//              + System.getProperty("user.dir") + "]");
//      extensionsDirectory = null;
//    }

    if (commandLine.hasParam(STATE_OPTION)) {
      File option = new File(commandLine.getParam(STATE_OPTION).getArg(0));
      if (option.isDirectory()) {
        PersistentBlackboardPlugIn.setPersistenceDirectory(option.getPath());
      }
      if (option.isFile()) {
        PersistentBlackboardPlugIn.setFileName(option.getName());
        PersistentBlackboardPlugIn.setPersistenceDirectory(option
            .getAbsoluteFile().getParent());
      }
    }

    // create plugin manager
    plugInManager = new PlugInManager(context, monitor);
    // add secondary extension folders (mainly for dev where we have lib/ext/ & lib/plus/)
//    for (File dir : moreDirs) {
//      plugInManager.addExtensionDir(dir);
//    }

    // iterate over params, order matters, limit-ext-lookup is on/off switchable
    Iterator<Param> params = commandLine.getParams();
    while (params.hasNext()) {
      Param param = params.next();
      if (param.getSpec().matches(EXTS_DIRECTORY_OPTION)) {
        String folder = param.getArg(0);
        plugInManager.addExtensionsFolder(new File(folder));
      }
      else if (param.getSpec().matches(LIMIT_LOOKUP_OPTION)) {
        String trueFalse = param.getArg(0);
        plugInManager.setLimitExtensionLookup(trueFalse.toLowerCase().equals("true"));
      }
      else if (param.getSpec().matches(JARS_DIRECTORY_OPTION)) {
        String folder = param.getArg(0);
        plugInManager.addJarsFolder(new File(folder));
      }
    }

    // Load drivers before initializing the frame because part of the frame
    // initialization is the initialization of the driver dialogs. [Jon
    // Aquino]
    // The initialization of some plug-ins (e.g. LoadDatasetPlugIn) requires
    // that
    // the drivers be loaded. Thus load the drivers here, before the
    // plug-ins
    // are initialized.
    driverManager.loadDrivers(properties);
    workbench = this;
  }

  public static void main(String[] args) {
    long start = Timer.milliSecondsSince(0);
    try {
      // first fetch parameters, locale might be changed with -i18n switch
      parseCommandLine(args);
      // load i18n specified in command line ( '-i18n translation' )
      if (commandLine.hasParam(I18N_FILE)) {
        I18N_SETLOCALE = commandLine.getParam(I18N_FILE).getArg(0);
        // initialize I18N
        Locale loc = I18N.fromCode(I18N_SETLOCALE);
        I18N.applyToRuntime(loc);
        I18N.setLocale(loc);
      }

      // set user agent used by UrlConnection, if not set on cmdline
      if (System.getProperty("http.agent") == null)
        System.setProperty("http.agent", I18N.getInstance().get("JUMPWorkbench.jump") + " "
            + JUMPVersion.CURRENT_VERSION);

      if (commandLine.hasParam("help")) {
        printProperly(commandLine.printDoc());
        System.exit(0);
      } else if (commandLine.hasParam("version")) {
        printProperly(I18N.getInstance().get("JUMPWorkbench.jump") + " "
            + I18N.getInstance().get("ui.AboutDialog.version") + " "
            + JUMPVersion.CURRENT_VERSION);
        System.exit(0);
      } else if (commandLine.hasParam("print-properties")) {
        printProperties("args[]=" + Arrays.toString(args));
        System.exit(0);
      }
      
      // set logging level according to parameter
      if (commandLine.hasParam("verbosity")) {
        Param v = commandLine.getParam("verbosity");
        if (v.getNumArgs() < 1) {
          printProperly(I18N.getInstance().get(v.getSpec().getDesc()));
          System.exit(1);
        }
          
        Logger.setLevel(v.getArg(0));
      }
      
      // Init the L&F before instantiating the progress monitor [Jon Aquino]
      initLookAndFeel();
      // fix lnf (weird windows non-unicode locale bug)
      fixLookAndFeel();

      // setFont to switch fonts if defaults cannot display current language
      // early change the default font definition of the jre if necessary, the
      // first internationalized string shown is 'JUMPWorkbench.version' on
      // splashpanel
      if (setFont()) {
        Logger.info("New fonts have been loaded");
      }

      com.vividsolutions.jump.workbench.ui.ProgressMonitor progressMonitor =
              (com.vividsolutions.jump.workbench.ui.ProgressMonitor) progressMonitorClass
          .getDeclaredConstructor().newInstance();
      SplashPanelV2 splashPanel = new SplashPanelV2(splashImage(),
          I18N.getInstance().get("ui.AboutDialog.version") + " "
              + JUMPVersion.CURRENT_VERSION);
      splashPanel.addProgressMonitor(progressMonitor);

      main(I18N.getInstance().get("JUMPWorkbench.jump"), splashPanel, progressMonitor);
      Logger.info("OJ start took "
          + Timer.secondsSinceString(start) + "s alltogether.");

    } catch (final Throwable t) {
      try {
        SwingUtilities.invokeAndWait(() -> {
          try {
            initLookAndFeel();
          } catch (Exception e) {
            // fail silently
          }
          ErrorDialog.show(null,
              StringUtil.toFriendlyName(t.getClass().getSimpleName()),
              WorkbenchFrame.toMessage(t), StringUtil.stackTrace(t));
        });
      } catch (Throwable t2) {
        Logger.error(t2);
      }
      Logger.error(t);
      System.exit(1);
    }
  }

  /**
   * setupClass is specified as a String to prevent it from being loaded before
   * we display the splash screen, in case setupClass takes a long time to load.
   *
   * @param title
   *          application title
   * @param splashComponent
   *          a component to open until the workbench frame is displayed
   * @param taskMonitor
   *          notified of progress of plug-in loading
   * @throws Exception if an Exception occurs
   */
  static void main(String title, JComponent splashComponent,
      TaskMonitor taskMonitor) throws Exception {

    SplashWindow splashWindow = new SplashWindow(splashComponent);
    splashWindow.setVisible(true);

    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.create"));
    workbench = new JUMPWorkbench(title, splashWindow, taskMonitor);

    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.configure-core"));
    // first things first, make persistent data available early
    PersistentBlackboardPlugIn persistentBlackboard = new PersistentBlackboardPlugIn();
    persistentBlackboard.initialize(workbench.context.createPlugInContext());

    // this restores the saved laf, so it must be loaded early
    new InstallSkinsPlugIn().initialize(workbench.context.createPlugInContext());
    final WorkbenchFrame frame = workbench.getFrame();

    // restore proxy settings TODO: write a hook routine for this
    ProxySettingsOptionsPanel.restoreSystemSettings(PersistentBlackboardPlugIn
        .get(workbench.context.getWorkbench().getBlackboard()));

    // now setup our configuration
    // TODO: clean up and merge with postExtensionInitialization below or even better put into xml config
    JUMPConfiguration config = new JUMPConfiguration();
    config.setup(workbench.context);

    // load plugin/extensions via plugin mgr.
    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.load-extensions"));
    workbench.context.getWorkbench().getPlugInManager().load();
    
    // some more intialization 
    // TODO: clean up and merge with setup above or even better put into xml config
    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.initialize-datasources"));
    config.postExtensionInitialization(workbench.context);

    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.restore-state"));
    frame.restore();
    taskMonitor.report(I18N.getInstance().get("JUMPWorkbench.status.show-workbench"));
    frame.setVisible(true);
    // Activate SelectFeaturesTool cursor after opening a new session.
    // See also JUMPConfiguration.configureToolBar() where the select
    // button has been selected.
    SwingUtilities.invokeLater(() -> frame.getToolBar().reClickSelectedCursorToolButton());
  }

  private static void fixLookAndFeel() {
    // see mailing thread about background
    // http://www.mail-archive.com/jump-pilot-devel@lists.sourceforge.net/msg12735.html
    UIDefaults defaults = UIManager.getDefaults();
    Font ta_font = (Font) defaults.get("TextArea.font");
    if (ta_font.getSize() < 11) {
      UIManager.put("TextArea.font", ta_font.deriveFont(13f));
      Logger.info("Fix text area font size bug.");
    }
  }

  private static void initLookAndFeel() throws Exception {
    if (LangUtil.ifNull(System.getProperty("initLookAndFeel"), "true")
        .toString().equalsIgnoreCase("false")) {
      return;
    }
    // Apple stuff from Raj Singh's startup script [Jon Aquino 10/30/2003]
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("apple.awt.showGrowBox", "true");

    if (UIManager.getLookAndFeel() != null
        && UIManager.getLookAndFeel().getClass().getName()
            .equals(UIManager.getSystemLookAndFeelClassName())) {
      return;
    }
    String laf = System.getProperty("swing.defaultlaf");
    if (laf == null) {
      laf = UIManager.getSystemLookAndFeelClassName();
    }
    UIManager.setLookAndFeel(laf);
  }

  // this is in preparation that we might want to support more fonts in the
  // future
  private static Font[] loadFonts() throws Exception {
    Font font = Font.createFont(Font.TRUETYPE_FONT, Class.class
        .getResource("/language/fonts/code2000.ttf").openStream());
    // since 1.6 we could register the font and use it by name but using
    // the font directly makes us 1.5 compatible
    // GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    return new Font[] { font };
  }

  private static boolean setFont() throws Exception {
    String test = I18N.getInstance().get("ui.MenuNames.FILE");
    boolean replaced = false;
    Font font = null;

    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      // loop over font ui resources entries
      if (value instanceof javax.swing.plaf.FontUIResource) {
        FontUIResource fold = ((javax.swing.plaf.FontUIResource) value);

        // can default font display test sentence?
        if (fold.canDisplayUpTo(test) != -1) {

          // fetch replacement candidate
          if (font == null)
            font = loadFonts()[0];

          // copy attributes
          Map<TextAttribute,?> attrs = fold.getAttributes();
          // remove family attribute
          TextAttribute fam = null;
          for (TextAttribute textAttribute : attrs.keySet()) {
            if (textAttribute.toString().equals("java.awt.font.TextAttribute(family)")) {
              fam = textAttribute;
              break;
            }
          }
          if (fam != null)
            attrs.remove(fam);
          // create the new font ui resources
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

  public static CommandLine getCommandLine() {
    return commandLine;
  }

  /**
   * @return JUMPWorkbench instance
   */
  public static JUMPWorkbench getInstance() {
    return workbench;
  }

  private static void printProperties(String in) {
    Properties ps = System.getProperties();
    TreeSet<Object> v = new TreeSet<>(ps.keySet());
    StringBuilder out = new StringBuilder();
    for (Object key : v) {
      out.append(key).append("=").append(ps.getProperty(key.toString())).append("\n");
    }

    out.append("\n\napplying default lnf");
    
    // init lnf for UIDefaults, ignore errors
    try {
      initLookAndFeel();
      out.append(" - OK\n");
    } catch (Exception e) {
      out.append("\n\n").append(StringUtil.stackTrace(e)).append("\n\n");
    }

    out.append("\n\nUIDefaults\n\n");
    UIDefaults defaults = UIManager.getDefaults();
    for (Map.Entry<Object,Object> entry : defaults.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::toString))
            .collect(Collectors.toList())) {
      out.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
    }

    printProperly(in + "\n" + out.toString());
  }

  private static void parseCommandLine(String[] args) {
    commandLine = new CommandLine();
    commandLine.addParamSpec(new ParamSpec(PROPERTIES_OPTION, 1,
        "workbench property file (activate extensions and plugins)"));
    commandLine.addParamSpec(new ParamSpec(DEFAULT_PLUGINS, 1,
        "property file (default OpenJUMP extensions and plugins)"));
    commandLine.addParamSpec(new ParamSpec(EXTS_DIRECTORY_OPTION, 1,
        "extensions folder location, can be defined multiple times\n default './lib/ext'"));
    commandLine.addParamSpec(new ParamSpec(LIMIT_LOOKUP_OPTION, 1,
        "limit extension class lookup to jars in extension folders' root"));
    commandLine.addParamSpec(new ParamSpec(JARS_DIRECTORY_OPTION, 1,
        "adds jar files recursively to classpath, for development"));
    commandLine
        .addParamSpec(new ParamSpec(
            I18N_FILE,
            1,
            "switch language and number formatting by overriding system's default locale setting, e.g. en_US"));
    commandLine
        .addParamSpec(new ParamSpec(
            STATE_OPTION,
            1,
            "where to save workbench settings, default OJ_HOME folder or USER_HOME/.openjump/"));
    // add logging
    commandLine.addParamSpec(new ParamSpec(new String[] { "v", "verbosity" }, 1,
        "logging verbosity, either: off, error, warn, info, debug, trace, all"));
    // add help
    commandLine.addParamSpec(new ParamSpec(new String[] { "h", "help", "-help" }, 0,
        "show this help"));
    // add version
    commandLine.addParamSpec(new ParamSpec(new String[] { "version", "-version" },
        0, "show version information"));
    // show properties (for debugging purposes)
    commandLine.addParamSpec(new ParamSpec(new String[] { "p",
        "print-properties" }, 0, "print a list of runtime properties"));

    try {
      commandLine.parse(args);
    } catch (ParseException e) {
      printProperly(commandLine.printDoc(e));
      System.exit(1);
    }
  }

  public PlugInManager getPlugInManager() {
    return plugInManager;
  }

  // <<TODO>> Make some properties persistent using a #makePersistent(key)
  // method. [Jon Aquino]
  /**
   * Expensive data structures can be cached on the blackboard so that several
   * plug-ins can share them.
   */
  public Blackboard getBlackboard() {
    return blackboard;
  }

  //private static class VerticallyScrollingProgressMonitor extends
  //    ProgressMonitor {
  //  private static final int ROWS = 3;
  //  private final JLabel[] labels;
//
  //  public VerticallyScrollingProgressMonitor() {
  //    super(new JPanel(new GridLayout(ROWS, 1)));
//
  //    JPanel panel = (JPanel) getComponent();
  //    panel.setOpaque(false);
  //    labels = new JLabel[ROWS];
//
  //    for (int i = 0; i < ROWS; i++) {
  //      // " " not "", to give the label some height. [Jon Aquino]
  //      labels[i] = new JLabel(" ");
  //      labels[i].setFont(labels[i].getFont().deriveFont(Font.BOLD));
  //      panel.add(labels[i]);
  //    }
  //  }
//
  //  protected void addText(String s) {
  //    for (int i = 0; i < (ROWS - 1); i++) { // -1
  //      labels[i].setText(labels[i + 1].getText());
  //    }
//
  //    labels[ROWS - 1].setText(s);
  //  }
  //}

  private static class SingleLineProgressMonitor
          extends com.vividsolutions.jump.workbench.ui.ProgressMonitor {
    public SingleLineProgressMonitor() {
      super(new JLabel(" "));
      getComponent().setFont(getComponent().getFont()
          .deriveFont(Font.BOLD));
      ((JLabel) getComponent()).setHorizontalAlignment(JLabel.LEFT);
    }

    protected void addText(String s) {
      ((JLabel) getComponent()).setText(s);
    }
  }

  //private static class HorizontallyScrollingProgressMonitor extends
  //    ProgressMonitor {
  //  private static final String BUFFER = "   ";
  //  public HorizontallyScrollingProgressMonitor() {
  //    super(new JLabel(" "));
  //    getComponent().setFont(getComponent().getFont()
  //        .deriveFont(Font.BOLD));
  //    ((JLabel) getComponent()).setHorizontalAlignment(JLabel.RIGHT);
  //  }
  //  protected void addText(String s) {
  //    ((JLabel) getComponent()).setText(BUFFER + s
  //        + ((JLabel) getComponent()).getText());
  //  }
  //}

  /*
   * Helper method to print to GUI and Console (e.g. help text)
   */
  private static void printProperly(String text) {
    // we print it to gui and console, just to be sure
    // make sure lnf is set
    try {
      initLookAndFeel();
    } catch (Exception e) {
      // fail silently
    }
    JTextArea textArea = new JTextArea();
    JScrollPane scrollPane = new JScrollPane(textArea);
    textArea.setEditable(false);
    textArea.setText(text);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    JOptionPane pane = new JOptionPane(scrollPane,
        JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
        IconLoader.icon("oj_32.png"));
    JDialog d = pane.createDialog(I18N.getInstance().get("JUMPWorkbench.jump"));
    // some cute icons
    ArrayList<Image> l = new ArrayList<>(APP_ICONS);
    l.add(0, IconLoader.image("information_16x16.png"));
    d.setIconImages(l);
    // harmonize background color
    textArea.setBackground(new JLabel().getBackground());
    d.setResizable(true);
    d.pack();
    d.setVisible(true);
  }

}