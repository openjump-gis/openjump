package com.vividsolutions.jump.workbench;

import static org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_OUT;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import com.vividsolutions.jump.workbench.ui.plugin.GenerateLogPlugIn;

/**
 * a generalized logger interface for OJ package usage currently based on log4j
 * v1.2
 * 
 * TODO: - move to commons-logging to be more implementation independent
 * -implement class:line logging for legacy log4j using code
 * 
 * @author ed
 *
 */
public class Logger {

  private static boolean log4j1Initialized = false;
  private static boolean log4j2Initialized = false;

  private static void initLog4j1(){
    if (log4j1Initialized)
      return;

    // just in case log4j init failed add a default console appender for us to see errors printed
    org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
    if (!rootLogger.getAllAppenders().hasMoreElements()) {
      rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%p] %d{HH:mm:ss.SSS} %m%n"),"System.out"));
    }
    log4j1Initialized = true;
  }

  public static void fatal(String msg) {
    log(msg, null, LogLevel.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(String msg) {
    log(msg, null, LogLevel.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(String msg) {
    log(msg, null, LogLevel.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(String msg) {
    log(msg, null, LogLevel.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(String msg) {
    log(msg, null, LogLevel.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(String msg) {
    log(msg, null, LogLevel.TRACE, new Exception().getStackTrace()[0]);
  }

  public static void fatal(Throwable t) {
    log(null, t, LogLevel.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(Throwable t) {
    log(null, t, LogLevel.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(Throwable t) {
    log(null, t, LogLevel.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(Throwable t) {
    log(null, t, LogLevel.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(Throwable t) {
    log(null, t, LogLevel.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(Throwable t) {
    log(null, t, LogLevel.TRACE, new Exception().getStackTrace()[0]);
  }

  public static void fatal(String msg, Throwable t) {
    log(msg, t, LogLevel.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(String msg, Throwable t) {
    log(msg, t, LogLevel.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(String msg, Throwable t) {
    log(msg, t, LogLevel.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(String msg, Throwable t) {
    log(msg, t, LogLevel.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(String msg, Throwable t) {
    log(msg, t, LogLevel.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(String msg, Throwable t) {
    log(msg, t, LogLevel.TRACE, new Exception().getStackTrace()[0]);
  }

  /**
   * log msg, throwable with log level from one stack before the given
   * StackTraceElement code location
   * 
   * @param msg message to log
   * @param t throwable to log
   * @param logLevel log level of the message
   * @param calledFrom Exception stacktrace
   */

  public static void log(String msg, Throwable t, LogLevel logLevel, StackTraceElement calledFrom) {
    // run our initLog4j2() before first call to log4j 2 to configure the Root Logger
    initLog4j2();

    // get caller
    StackTraceElement element = getCaller(calledFrom);

    org.apache.log4j.Logger logger1 = null;
    org.apache.logging.log4j.core.Logger logger2 = null;

    if (element != null) {
      logger1 = org.apache.log4j.Logger.getLogger(element.getClassName());
      logger2 = getLogger(element.getClassName());
    }

    // run our initLog4j1() after first call to log4j 1 to give it the possibility to read log4j.xml first
    initLog4j1();

    // In Log4j 2, "DEBUG.isMoreSpecificThan(DEBUG)" is true, and "DEBUG.isMoreSpecificThan(INFO)" is false
    boolean debugEnabled = !logger2.getLevel().isMoreSpecificThan(Level.INFO);

    // only append code:line during debugging
    String msgAppend = "";
    if (element != null && debugEnabled) {
      msgAppend = " at " + element + "";
    }

    // empty log messages provoke this error line for devs to fix the cause
    if (msg !=null && msg.isEmpty()) {
      error("Logger: string message was empty but not null at "+element);
    }

    // throw error on empty log entries
    if (t == null && msg == null) {
      throw new IllegalArgumentException(
          "Logger: either message or throwable must be given. "+element);
    }

    // use throwable's data if null message was given
    if (msg == null) {
      msg = t.getMessage();
      if (msg == null || msg.isEmpty() ) {
        msg = t.getClass().getName();
      }
    }

    logger1.log(logLevel.getLog4j1Equivalent(), msg + msgAppend, t);
    logger2.log(logLevel.getLog4j2Equivalent(), msg + msgAppend, t);
  }

  private static void initLog4j2() {
    if (log4j2Initialized) {
      return;
    }

    LoggerContext context = getLoggerContext();
    Configuration configuration = context.getConfiguration();

    if (configuration instanceof DefaultConfiguration) {
      String defaultAppenderName = "DefaultConsole-2";
      org.apache.logging.log4j.core.Appender defaultAppender = configuration.getAppender(defaultAppenderName);

      org.apache.logging.log4j.core.appender.ConsoleAppender fallbackConsoleAppender =
              org.apache.logging.log4j.core.appender.ConsoleAppender.newBuilder()
                      .setTarget(SYSTEM_OUT)
                      .setLayout(org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                              .withPattern("[%p] %d{HH:mm:ss.SSS} %m%n").build())
                      .setName("FallbackConsoleAppender")
                      .build();
      fallbackConsoleAppender.start();

      // Log Level must be adjusted on The Root Logger's LoggerConfig for configuration inheritance to work properly
      configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);

      context.getRootLogger().removeAppender(defaultAppender);
      context.getRootLogger().addAppender(fallbackConsoleAppender);

    }
    log4j2Initialized = true;
  }

  private static StackTraceElement getCaller(StackTraceElement calledFrom) {
    if (calledFrom == null)
      throw new IllegalArgumentException();

    StackTraceElement[] stack = new Exception().getStackTrace();
    boolean seenCaller = false;
    // run up the stack until we are one below the calling code (calledFrom)
    for (StackTraceElement element : stack) {
      // we saw calledFrom in the previous run, hence this must be our origin
      if (seenCaller)
        return element;
      seenCaller = element.equals(calledFrom);
    }

    return null;
  }

  private static org.apache.logging.log4j.core.Logger getLogger(String className) {
    return getLoggerContext().getLogger(className);
  }

  private static LoggerContext getLoggerContext() {
    return (LoggerContext) LogManager.getContext(false);
  }

  /**
   * get current file appenders, mainly for display purposes eg. in
   * {@link GenerateLogPlugIn}
   * 
   * @return files list
   */
  public static List<File> getLogFiles() {
    List files = new ArrayList<File>();
    Enumeration<org.apache.log4j.Logger> loggers = org.apache.log4j.LogManager
        .getCurrentLoggers();
    org.apache.log4j.Logger rootlogger = org.apache.log4j.LogManager
        .getRootLogger();
    // combine all loggers to one list to iterate over
    List<org.apache.log4j.Logger> list = Collections.list(loggers);
    list.add(rootlogger);
    for (org.apache.log4j.Logger logger : list) {

//      System.out.println(logger.getName());
      Enumeration<Appender> apps = logger.getAllAppenders();

      while (apps.hasMoreElements()) {
        Appender app = (Appender) apps.nextElement();
//        System.out.println(app.getName());
        if (app instanceof FileAppender) {
//          System.out.println(app);
          files.add(new File(((FileAppender) app).getFile()));
        }
      }
    }

    return files;
  }

  /**
   * setting current log level for the root logger
   *
   * @param levelString a string representing the LogLevel
   */
  public static void setLevel(String levelString) {
    LogLevel logLevel = LogLevel.valueOf(levelString.toUpperCase());

    org.apache.log4j.Logger.getRootLogger().setLevel(logLevel.getLog4j1Equivalent()); // TODO remove Log4j 1 here

    // initialize the Root Logger if needed, or else there might be no Logger to configure
    initLog4j2();

    LoggerContext loggerContext = getLoggerContext();
    // Log Level must be adjusted on The Root Logger's LoggerConfig for configuration inheritance to work properly
    loggerContext.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(logLevel.getLog4j2Equivalent());
    loggerContext.getRootLogger().setLevel(logLevel.getLog4j2Equivalent());

    info(new MessageFormat("Setting log level to {0}").format(new Object[]{logLevel}));
  }

  /**
   * @return the current log level for the calling class
   */
  public static LogLevel getLevel() {
    // get caller
    StackTraceElement element = getCaller(new Exception().getStackTrace()[0]);

    org.apache.logging.log4j.core.Logger logger2 = getLogger(element.getClassName());

    return LogLevel.fromLog4j2Equivalent(logger2.getLevel());
  }

  private static boolean isLoggerLevelEnabled(LogLevel level) {
    // get caller, this time 2 stack entries away
    StackTraceElement element = getCaller(new Exception().getStackTrace()[1]);

    org.apache.logging.log4j.core.Logger logger = getLogger(element.getClassName());

    // In Log4j 2, "DEBUG.isMoreSpecificThan(DEBUG)" is true, and "DEBUG.isMoreSpecificThan(INFO)" is false
    return level.getLog4j2Equivalent().isMoreSpecificThan(logger.getLevel());
  }

  public static boolean isFatalEnabled() {
    return isLoggerLevelEnabled(LogLevel.FATAL);
  }

  public static boolean isErrorEnabled() {
    return isLoggerLevelEnabled(LogLevel.ERROR);
  }

  public static boolean isWarnEnabled() {
    return isLoggerLevelEnabled(LogLevel.WARN);
  }

  public static boolean isInfoEnabled() {
    return isLoggerLevelEnabled(LogLevel.INFO);
  }

  public static boolean isDebugEnabled() {
    return isLoggerLevelEnabled(LogLevel.DEBUG);
  }

  public static boolean isTraceEnabled() {
    return isLoggerLevelEnabled(LogLevel.TRACE);
  }

  public enum LogLevel {
    OFF(org.apache.log4j.Level.OFF, Level.OFF),
    FATAL(org.apache.log4j.Level.FATAL, Level.FATAL),
    ERROR(org.apache.log4j.Level.ERROR, Level.ERROR),
    WARN(org.apache.log4j.Level.WARN, Level.WARN),
    INFO(org.apache.log4j.Level.INFO, Level.INFO),
    DEBUG(org.apache.log4j.Level.DEBUG, Level.DEBUG),
    TRACE(org.apache.log4j.Level.TRACE, Level.TRACE),
    ALL(org.apache.log4j.Level.ALL, Level.ALL);

    public static LogLevel fromLog4j1Equivalent(org.apache.log4j.Level equivalent) {
      return valueOf(equivalent.toString());
    }

    public static LogLevel fromLog4j2Equivalent(Level equivalent) {
      return valueOf(equivalent.name());
    }

    private final org.apache.log4j.Level log4j1Equivalent;
    private final Level log4j2Equivalent;

    LogLevel(org.apache.log4j.Level log4j1Equivalent, Level log4j2Equivalent) {
      this.log4j1Equivalent = log4j1Equivalent;
      this.log4j2Equivalent = log4j2Equivalent;
    }

    private org.apache.log4j.Level getLog4j1Equivalent() {
      return log4j1Equivalent;
    }

    private Level getLog4j2Equivalent() {
      return log4j2Equivalent;
    }

  }

}
