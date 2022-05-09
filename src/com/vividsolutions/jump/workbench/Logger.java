package com.vividsolutions.jump.workbench;

import static org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_OUT;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.vividsolutions.jump.workbench.ui.plugin.GenerateLogPlugIn;

/**
 * a generalized logger interface for OJ package usage currently based on Log4j v2
 * 
 * TODO:
 * -implement class:line logging for legacy log4j using code
 * 
 * @author ed
 *
 */
public class Logger {

  private static boolean initialized = false;

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
    // run our init() before first call to log4j 2 to configure the Root Logger
    init();

    // get caller
    StackTraceElement element = getCaller(calledFrom);

    // get Logger for caller
    org.apache.logging.log4j.core.Logger logger = getLogger(element);

    // In Log4j 2, "DEBUG.isMoreSpecificThan(DEBUG)" is true, and "DEBUG.isMoreSpecificThan(INFO)" is false
    boolean debugEnabled = !logger.getLevel().isMoreSpecificThan(Level.INFO);

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

    logger.log(logLevel.getEquivalent(), msg + msgAppend, t);
  }

  private static void init() {
    if (initialized) {
      return;
    }

    LoggerContext context = getLoggerContext();
    Configuration configuration = context.getConfiguration();
    org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();

    if (configuration instanceof DefaultConfiguration) {
      // get Log4j2's default fallback appender - DefaultConsole-1 is used for Log4j2's StatusLogger
      String defaultAppenderName = "DefaultConsole-2";
      Appender defaultAppender = configuration.getAppender(defaultAppenderName);

      ConsoleAppender fallbackConsoleAppender =
              ConsoleAppender.newBuilder()
                      .setTarget(SYSTEM_OUT)
                      .setLayout(PatternLayout.newBuilder().withPattern("[%p] %d{HH:mm:ss.SSS} %m%n").build())
                      .setName("FallbackConsoleAppender")
                      .build();
      fallbackConsoleAppender.start();

      // Log Level must be adjusted on The Root Logger's LoggerConfig for configuration inheritance to work properly
      configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);

      rootLogger.removeAppender(defaultAppender);
      rootLogger.addAppender(fallbackConsoleAppender);
    }

    initialized = true;
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

  // return the Logger for the element, or the Root Logger if element == null
  private static org.apache.logging.log4j.core.Logger getLogger(StackTraceElement element) {
    return element == null ? getLoggerContext().getRootLogger() : getLogger(element.getClassName());
  }

  private static org.apache.logging.log4j.core.Logger getLogger(String className) {
    return getLoggerContext().getLogger(className);
  }

  private static LoggerContext getLoggerContext() {
    return (LoggerContext) LogManager.getContext(false);
  }

  /**
   * get current file appenders, mainly for display purposes e.g. in
   * {@link GenerateLogPlugIn}
   * 
   * @return files list
   */
  public static List<File> getLogFiles() {
    return getLoggerContext().getLoggers().stream()
            .flatMap(logger -> logger.getAppenders().values().stream())
            .filter(appender -> appender instanceof FileAppender)
            .map(appender -> (FileAppender) appender)
            .map(FileAppender::getFileName)
            .distinct()
            .map(File::new)
            .collect(Collectors.toList());
  }

  /**
   * setting current log level for the root logger
   *
   * @param levelString a string representing the LogLevel
   */
  public static void setLevel(String levelString) {
    LogLevel logLevel = LogLevel.valueOf(levelString.toUpperCase());

    // initialize the Root Logger if needed, or else there might be no Logger to configure
    init();

    // Log Level must be adjusted on The Root Logger's LoggerConfig for configuration inheritance to work properly
    getLoggerContext().getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(logLevel.getEquivalent());
    getLoggerContext().getRootLogger().setLevel(logLevel.getEquivalent());

    info(new MessageFormat("Setting log level to {0}").format(new Object[]{logLevel}));
  }

  /**
   * @return the current log level for the calling class
   */
  public static LogLevel getLevel() {
    // get caller
    StackTraceElement element = getCaller(new Exception().getStackTrace()[0]);

    org.apache.logging.log4j.core.Logger logger = getLogger(element);

    return LogLevel.fromEquivalent(logger.getLevel());
  }

  private static boolean isLoggerLevelEnabled(LogLevel level) {
    // get caller, this time 2 stack entries away
    StackTraceElement element = getCaller(new Exception().getStackTrace()[1]);

    org.apache.logging.log4j.core.Logger logger = getLogger(element);

    // In Log4j 2, "DEBUG.isMoreSpecificThan(DEBUG)" is true, and "DEBUG.isMoreSpecificThan(INFO)" is false
    return level.getEquivalent().isMoreSpecificThan(logger.getLevel());
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
    OFF(Level.OFF),
    FATAL(Level.FATAL),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    public static LogLevel fromEquivalent(Level equivalent) {
      return valueOf(equivalent.name());
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
