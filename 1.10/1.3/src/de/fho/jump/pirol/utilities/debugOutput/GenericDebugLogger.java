/*
 * Created on 17.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author$
 *  $Rev$
 *  $Date$
 *  $Id$
 */
package de.fho.jump.pirol.utilities.debugOutput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

import org.openjump.io.PropertiesHandler;

import de.fho.jump.pirol.utilities.settings.PirolPlugInSettings;

/**
 * Class to handle debugging outputs. It is a singleton and keeps track of local debug settings and personal log levels. 
 * Configuration changes should not be done at runtime, but in the properties file ("debugging.properties") in the 
 * "[HOME]/.OpenJump_PIROL/config" directory. This file will be created when the logger is used the first time and filled 
 * with default values. For information on these values, please see commenting in the java source code. 
 *
 * @author Ole Rahn, Stefan Ostermann
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 * 
 * @see de.fho.jump.pirol.utilities.PirolPlugInSettings
 * @see PersonalLogger
 */
public final class GenericDebugLogger {

    private static GenericDebugLogger logger = null;
    
    /**
     * user Id for the logger object -> messages from this user are always displayed
     */
    private final static String loggerUserId = "GenericDebugLogger";
    
    /**
     * Prefix for the username to store/read from the properties file
     */
    private final static String userFlagPrefix = "showMessagesOf_";
    
    protected PropertiesHandler properties = null;
    protected static final String propertiesFile = "debugging.properties";
    
    // default values: only the most important messages will be put out
    /**
     * The logLevel specifies which kinds of messages will be put out. A message will
     * be put out, if its severity is greater or equal to the log level (or if the user's
     * log messages are enabled)
     * <pre>
     * logLevel severity sheme:
     * 0  - debug   		- just an output for debugging purposes
     * 1  - warning 		- something that might not be good happened
     * 2  - minor error 	- an error that won't have influence on the results occured
     * 3  - error       	- an error that may invalidate the current results occured
     * 4  - severe error	- an error that may invalidate the current and future results or may crash the VM, etc. 
     * </pre> 
     */
    protected int logLevel = 1; // errors with effect on the results, only //sstein: set from 3 to 1
    protected final static String KEY_LOGLEVEL = "logLevel";
    /**Constant {@link #logLevel} for debugging purposes.*/
    public final static int SEVERITY_DEBUG 			= 0;
    /**Constant {@link #logLevel} for something that might not be good 
     * happened.*/
    public final static int SEVERITY_WARNING		= 1;
    /**Constant {@link #logLevel} for an error that won't have influence on 
     * the results occured.*/
    public final static int SEVERITY_MINORERROR 	= 2;
    /**Constant {@link #logLevel} for an error that may invalidate the 
     * current results occured.*/
    public final static int SEVERITY_ERROR 			= 3;
    /**Constant {@link #logLevel} for an error that may invalidate the 
     * current and future results or may crash the VM, etc..*/
    public final static int SEVERITY_SEVEREERROR	= 4;
    
    protected final static String[] severityLevels = new String[]{"DEBUG", "WARNING", "MIN.ERROR", "ERROR", "SEV.ERROR"};
    
    /**
     * format the output string so that eclipse supports jumping into the 
     * correct file and line number when clicking on the output.
     */
    protected boolean eclipseFriendlyOutput = true;
    protected final static String KEY_ECLIPSEFRIENDLYOUTPUT = "eclipseFriendlyOutput";
    
    /**
     * wether or not to print time stamps in the messages
     */
    protected boolean printTimeStamp = false;
    protected final static String KEY_PRINTTIMESTAMPS = "printTimeStamps";
    
    /**
     * wether or not to print file name and line number in code
     */
    protected boolean printFileAndLine = true;
    protected final static String KEY_PRINTFILEANDLINE = "printFileAndLineNumber";
    
    /**
     * print additional line break before output of new messages?
     */
    protected boolean printNewLineFirst = false;
    protected final static String KEY_PRINTNEWLINEFIRST = "printNewLineFirst";
    
    /**
     * print short class names instead of class name plus the whole package path?
     */
    protected boolean printShortClassNames = true;
    protected final static String KEY_PRINTSHORTCLASSNAMES = "printShortClassNames";
    
    /**
     * print user names with every message?
     */
    protected boolean printUserNames = false;
    protected final static String KEY_PRINTUSERNAMES = "printUserNames";
    
    protected PrintStream stdOut;
    protected PrintStream stdErr;
    
    protected File logFile = null;
    /**
     * use a log file instead of printing messages to the console?
     */
    protected boolean useLogFile = true;
    protected final static String KEY_USELOGFILE = "useLogFile";
    
    /**
     * constructor is private, since we use the singleton pattern.
     */
    private GenericDebugLogger(){
        this.setOutputStream( System.out );
        this.setErrorStream( System.err );
        
        try {
            this.loadProperties();
        } catch (FileNotFoundException e) {
            this.printMinorError(GenericDebugLogger.loggerUserId,e.getMessage());
        } catch (IOException e) {
            this.printMinorError(GenericDebugLogger.loggerUserId, e.getMessage());
        }
        
        if (this.useLogFile){
            this.logFile = new File(PirolPlugInSettings.tempDirectory().getPath() + File.separator + "session.log" );
            FileOutputStream fos = null;
            try {
                if (!logFile.exists()){
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                fos = new FileOutputStream(logFile);
                PrintStream logFileStream = new PrintStream(fos);
                
                this.setOutputStream( logFileStream );
                this.setErrorStream( logFileStream );
            } catch (FileNotFoundException e) {
                this.printWarning(GenericDebugLogger.loggerUserId, "Problems using a log file: " + e.getMessage());
            }
             
        }
    }
    
    /**
     * load local configuration file, to check if there are saved directorties for
     * debugging outputs.
     * @throws IOException if the file with the given file name could not be accessed 
     */
    protected final void loadProperties() throws IOException{

        this.properties = new PropertiesHandler(GenericDebugLogger.propertiesFile);
        this.properties.load();
        
        this.logLevel = this.properties.getPropertyAsInt(GenericDebugLogger.KEY_LOGLEVEL, this.logLevel);
        this.printTimeStamp = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_PRINTTIMESTAMPS, this.printTimeStamp);
        this.printFileAndLine = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_PRINTFILEANDLINE, this.printFileAndLine);
        this.printNewLineFirst = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_PRINTNEWLINEFIRST, this.printNewLineFirst);
        this.printShortClassNames = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_PRINTSHORTCLASSNAMES, this.printShortClassNames);
        this.printUserNames = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_PRINTUSERNAMES, this.printUserNames);
        this.useLogFile = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_USELOGFILE, this.useLogFile);
        this.eclipseFriendlyOutput = this.properties.getPropertyAsBoolean(GenericDebugLogger.KEY_ECLIPSEFRIENDLYOUTPUT, this.eclipseFriendlyOutput);
        
        
        this.properties.store(null);

    }

    
    
    /**
     * check if the properties contain information on how to treat messages
     * from this user
     *@param user user id to check
     *@return true, if Properties contain information on this user that allow posting his/her messages
     */
    protected final boolean showMessagesOfUser(String user){
        if (user.equals(GenericDebugLogger.loggerUserId))
            return true;
        
        String userString = GenericDebugLogger.userFlagPrefix + user.toLowerCase();
        boolean allowMessagesFromUser = false;
        allowMessagesFromUser = this.properties.getPropertyAsBoolean(userString, allowMessagesFromUser);
        try {
            this.properties.store();
        } catch (IOException e) {
            this.printMinorError(GenericDebugLogger.loggerUserId, e.getMessage());
        }
        return allowMessagesFromUser;
    }
    
    /**
     * THE method to get an instance of this class
     *@return the logger
     */
    static final GenericDebugLogger getInstance(){
        if (GenericDebugLogger.logger == null)
            GenericDebugLogger.logger = new GenericDebugLogger();
        
        return GenericDebugLogger.logger;
    }
    
    protected final void printMessage(String user, int severity, String message){
        if (this.properties==null){
            try {
                this.loadProperties();
            } catch (IOException e) {
                this.printError(GenericDebugLogger.loggerUserId,"still can not load properties!");
                return;
            }
        }
        if (severity >= this.logLevel || this.showMessagesOfUser(user)){
            String outputString = this.printUserNames?"("+user+") ":"";
            
            outputString += GenericDebugLogger.severityLevels[severity] + " in " + this.getCallerString(new Throwable());
            
            if (this.printTimeStamp == true){
                Calendar date = Calendar.getInstance();
                outputString += "(" + date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE) + ":" + date.get(Calendar.SECOND) + "," + date.get(Calendar.MILLISECOND) + ")";
            }
            
            outputString += ": " + ((this.eclipseFriendlyOutput)?"\n\t":"") + message;
        
            if (this.printNewLineFirst == true)
                this.stdOut.println("---");
            
            if (severity < GenericDebugLogger.SEVERITY_MINORERROR)
                this.stdOut.println(outputString);
            else
                this.stdErr.println(outputString);
        }
    }
    
    protected final String getCallerString(Throwable t){
        String caller = "";
        String FileAndNumberSep = ":";
        StackTraceElement[] elements = t.getStackTrace();
        
        for (int i=0; i<elements.length; i++){
            if (elements[i].getClassName().equals(GenericDebugLogger.class.getName()))
                continue;
            if (elements[i].getClassName().equals(PersonalLogger.class.getName()))
                continue;
            
            if (this.printShortClassNames && 
            		elements[i].getClassName().indexOf(".")>-1 &&
            		!this.eclipseFriendlyOutput){
                caller = elements[i].getClassName().substring(elements[i].getClassName().lastIndexOf(".")+1);
            } else {
                caller = elements[i].getClassName();
            }
            // fill in method name
            caller += "."+ elements[i].getMethodName();// + "()";
            if (! this.eclipseFriendlyOutput) {
            	caller+="()";
            	FileAndNumberSep = ",";
            }
           	
            
            if (this.printFileAndLine  == true || this.eclipseFriendlyOutput){
                caller += "(" + elements[i].getFileName() + 
                FileAndNumberSep + elements[i].getLineNumber() +") ";
            }

            
            break;
        }
        
        if (caller.length() == 0)
            caller = "???";
        
        return caller;
    }
    
    
    protected final void printDebug(String user, String message){
        this.printMessage(user, GenericDebugLogger.SEVERITY_DEBUG, message);
    }
    
    protected final void printWarning(String user, String message){
        this.printMessage(user, GenericDebugLogger.SEVERITY_WARNING, message);
    }
    
    protected final void printMinorError(String user, String message){
        this.printMessage(user, GenericDebugLogger.SEVERITY_MINORERROR, message);
    }
    
    protected final void printError(String user, String message){
        this.printMessage(user, GenericDebugLogger.SEVERITY_ERROR, message);
    }
    
    protected final void printSevereError(String user, String message){
        this.printMessage(user, GenericDebugLogger.SEVERITY_SEVEREERROR, message);
    }
    
    
    
    /**
     * 
     *@return current log level
     */
    public final int getLogLevel() {
        return logLevel;
    }
    /**
     * 
     * @param logLevel
     */
    public final void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        this.properties.setProperty(GenericDebugLogger.KEY_LOGLEVEL, new Integer(logLevel).toString());
        try {
            this.properties.store();
        } catch (IOException e) {
            this.printError(GenericDebugLogger.loggerUserId, e.getMessage());
        }
    }
    /**
     * 
     * @return true or false //TODO specify the return value
     */
    public final boolean isPrintFileAndLine() {
        return printFileAndLine;
    }
    /**
     * 
     * @return true or false //TODO specify the return value
     */
    public final boolean isPrintNewLineFirst() {
        return printNewLineFirst;
    }
    /**
     * 
     * @return true or false //TODO specify the return value
     */
    public final boolean isPrintTimeStamp() {
        return printTimeStamp;
    }
    /**
     *@return File name of the file where logger configuration is stored
     */
    public final String getPropertiesFile() {
        return propertiesFile;
    }
    /**
     * Set the stream, where messages with a loglevel < SEVERITY_MINORERROR are put out.
     *@param out stream for debugging output
     */
    public final void setOutputStream(PrintStream out) {
        this.stdOut = out;
        this.stdOut.print("\n");
    }
    
    /**
     * Set the stream, where messages with a loglevel >= SEVERITY_MINORERROR  are put out.
     *@param err stream for debugging output
     */
    public final void setErrorStream(PrintStream err) {
        this.stdErr = err;
        this.stdErr.print("\n");
    }
}
