package com.vividsolutions.jump.workbench;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.plugin.GenerateLogPlugIn;

/**
 * a generalized logger interface for OJ package usage currently based on log4j
 * v1.2
 * 
 * @author ed
 *
 */
public class Logger {

  public static void fatal(String msg) {
    log(msg, null, Level.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(String msg) {
    log(msg, null, Level.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(String msg) {
    log(msg, null, Level.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(String msg) {
    log(msg, null, Level.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(String msg) {
    log(msg, null, Level.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(String msg) {
    log(msg, null, Level.TRACE, new Exception().getStackTrace()[0]);
  }

  public static void fatal(Throwable t) {
    log(null, t, Level.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(Throwable t) {
    log(null, t, Level.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(Throwable t) {
    log(null, t, Level.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(Throwable t) {
    log(null, t, Level.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(Throwable t) {
    log(null, t, Level.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(Throwable t) {
    log(null, t, Level.TRACE, new Exception().getStackTrace()[0]);
  }

  public static void fatal(String msg, Throwable t) {
    log(msg, t, Level.FATAL, new Exception().getStackTrace()[0]);
  }

  public static void error(String msg, Throwable t) {
    log(msg, t, Level.ERROR, new Exception().getStackTrace()[0]);
  }

  public static void warn(String msg, Throwable t) {
    log(msg, t, Level.WARN, new Exception().getStackTrace()[0]);
  }

  public static void info(String msg, Throwable t) {
    log(msg, t, Level.INFO, new Exception().getStackTrace()[0]);
  }

  public static void debug(String msg, Throwable t) {
    log(msg, t, Level.DEBUG, new Exception().getStackTrace()[0]);
  }

  public static void trace(String msg, Throwable t) {
    log(msg, t, Level.TRACE, new Exception().getStackTrace()[0]);
  }

  /**
   * log msg, throwable with log level from one stack before the given
   * StackTraceElement code location
   * 
   * @param msg
   * @param t
   * @param logLevel
   */
  public static void log(String msg, Throwable t, Level logLevel,
      StackTraceElement calledFrom) {
    // get caller
    StackTraceElement element = getCaller(calledFrom);

    org.apache.log4j.Logger logger = null;

    if (element != null) {
      logger = org.apache.log4j.Logger.getLogger(element.getClassName());
    }

    // what's the current log level?
    Level loggerLevel = logger.getEffectiveLevel();

    // only append code:line during debugging
    String msgAppend = "";
    if (element != null && !loggerLevel.isGreaterOrEqual(Level.INFO))
      msgAppend = " at " + element + "";

    logger.log(logLevel, msg + msgAppend, t);
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

  /**
   * get current file appenders, mainly for display purposes eg. in
   * {@link GenerateLogPlugIn}
   * 
   * @return files list
   */
  public static List<File> getLogFiles() {
    List files = new ArrayList<File>();
    Enumeration<Category> loggers = org.apache.log4j.LogManager
        .getCurrentLoggers();
    while (loggers.hasMoreElements()) {
      Category logger = (Category) loggers.nextElement();
      Enumeration<Appender> apps = logger.getAllAppenders();

      while (apps.hasMoreElements()) {
        Appender app = (Appender) apps.nextElement();
        System.out.println(app.getName());
        if (app instanceof FileAppender) {
          // System.out.println(app);
          files.add(new File(((FileAppender) app).getFile()));
        }
      }
    }

    return files;
  }

  /**
   * setting current log level for the root logger
   * 
   * @param levelString
   */
  public static void setLevel(String levelString) {
    Level level = org.apache.log4j.Level.toLevel(levelString);
    if (level.equals(Level.DEBUG) && !levelString.equalsIgnoreCase("debug"))
      throw new IllegalArgumentException("unknown log verbosity level.");

    org.apache.log4j.Logger.getRootLogger().setLevel(level);

    info(I18N.getMessage("setting-log-level-to-{0}", level));
  }

  /**
   * @return the current log level for the calling class
   */
  public static Level getLevel() {
    // get caller
    StackTraceElement element = getCaller(new Exception().getStackTrace()[0]);
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(element
        .getClassName());
    return logger.getEffectiveLevel();
  }

  /**
   * @return the lo4j logger for the calling class
   */
  public static org.apache.log4j.Logger getLogger() {
    // get caller
    StackTraceElement element = getCaller(new Exception().getStackTrace()[0]);
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(element
        .getClassName());
    return logger;
  }

}
