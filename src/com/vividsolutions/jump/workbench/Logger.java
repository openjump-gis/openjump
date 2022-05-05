package com.vividsolutions.jump.workbench;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

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

  private static boolean initialized = false;

  private static void init(){
    if (initialized)
      return;

    // just in case log4j init failed add a default console appender for us to see errors printed
    org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
    if (!rootLogger.getAllAppenders().hasMoreElements()) {
      rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%p] %d{HH:mm:ss.SSS} %m%n"),"System.out"));
    }
    initialized = true;
  }

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
   * @param msg message to log
   * @param t throwable to log
   * @param logLevel log level of the message
   * @param calledFrom Exception stacktrace
   */

  public static void log(String msg, Throwable t, LogLevel logLevel, StackTraceElement calledFrom) {
    log(msg, t, logLevel.getEquivalent(), calledFrom);
  }

  private static void log(String msg, Throwable t, Level logLevel,
      StackTraceElement calledFrom) {

    // get caller
    StackTraceElement element = getCaller(calledFrom);

    org.apache.log4j.Logger logger = null;

    if (element != null) {
      logger = org.apache.log4j.Logger.getLogger(element.getClassName());
    }

    // run our init() after first call to log4j to give it the possibility to read log4j.xml first
    init();

    // what's the current log level?
    Level loggerLevel = logger.getEffectiveLevel();

    // only append code:line during debugging
    String msgAppend = "";
    if (element != null && !loggerLevel.isGreaterOrEqual(Level.INFO))
      msgAppend = " at " + element + "";

    // empty log messages provoke this error line for devs to fix the cause
    if (msg!=null && msg.isEmpty()) {
      error("Logger: string message was empty but not null at "+element);
    }

    // throw error on empty log entries
    if (t == null && msg == null)
        throw new IllegalArgumentException(
            "Logger: either message or throwable must be given. "+element);

    // use throwable's data if null message was given
    if (msg == null) {
      msg = t.getMessage();
      if (msg == null || msg.isEmpty() )
        msg = t.getClass().getName();
    }

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
   * setting current log level for the root logger (internally use
   * org.apache.log4j.Level.toLevel)
   *
   * @param levelString a string representing the LogLevel
   */
  public static void setLevel(String levelString) {
    Level level = org.apache.log4j.Level.toLevel(levelString);
    if (level.equals(Level.DEBUG) && !levelString.equalsIgnoreCase("debug"))
      throw new IllegalArgumentException("unknown log verbosity level.");

    org.apache.log4j.Logger.getRootLogger().setLevel(level);

    info(new MessageFormat("Setting log level to {0}").format(new Object[]{level}));
  }

  /**
   * @return the current log level for the calling class
   */
  public static LogLevel getLevel() {
    // get caller
    StackTraceElement element = getCaller(new Exception().getStackTrace()[0]);
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(element
        .getClassName());
    return LogLevel.fromEquivalent(logger.getEffectiveLevel());
  }

  private static boolean isLoggerLevelEnabled(Level level) {
    // get caller, this time 2 stack entries away
    StackTraceElement element = getCaller(new Exception().getStackTrace()[1]);
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(element
        .getClassName());
    return level.isGreaterOrEqual(logger.getEffectiveLevel());
  }

  public static boolean isFatalEnabled() {
    return isLoggerLevelEnabled(Level.FATAL);
  }

  public static boolean isErrorEnabled() {
    return isLoggerLevelEnabled(Level.ERROR);
  }

  public static boolean isWarnEnabled() {
    return isLoggerLevelEnabled(Level.WARN);
  }

  public static boolean isInfoEnabled() {
    return isLoggerLevelEnabled(Level.INFO);
  }

  public static boolean isDebugEnabled() {
    return isLoggerLevelEnabled(Level.DEBUG);
  }

  public static boolean isTraceEnabled() {
    return isLoggerLevelEnabled(Level.TRACE);
  }

  public enum LogLevel {
    OFF(Level.OFF),
    FATAL(Level.FATAL),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    public static LogLevel fromEquivalent(Level equivalent) {
      return valueOf(equivalent.toString());
    }

    private final Level equivalent;

    LogLevel(Level equivalent) {
      this.equivalent = equivalent;
    }

    private Level getEquivalent() {
      return equivalent;
    }

  }

}
