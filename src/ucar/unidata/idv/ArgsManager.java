/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv;


import org.w3c.dom.Element;
import org.w3c.dom.Node;


import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;


import ucar.unidata.util.PatternFileFilter;

import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import visad.VisADException;


import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * This manages the parsing and processing of the command line
 * arguments to the IDV.  If an IDV application needs to add
 * custom command line arguments then you need to do:<ul>
 * <li> Overwrite the factory method,
 * {@link IdvBase#doMakeArgsManager(String[])},
 *  in your derived {@link IntegratedDataViewer} class.
 * <li> This new method should  return your own subclass of ArgsManager.
 * <li> This subclass should  overwrite the {@link #parseArg(String,String[],int)}
 * method.
 * </ul>
 *
 * @author IDV development team
 */
public class ArgsManager extends IdvManager {

    /** usage message */
    public static final String USAGE_MESSAGE =
        "Usage: IntegratedDataViewer  <args> <bundle/script files, e.g., .xidv, .zidv, .isl>";


    /** How many times to try to run the initial runnables */
    public static final int RUNNABLE_MAX_TRIES = 30;


    /** Keep around the original command line arguments */
    String[] originalArgs;

    /** For later when we have the -fixedtime argument */
    String fixedTimeString;

    /** The image server port */
    int imageServerPort = -1;

    /** property file name when running in image server mode */
    String imageServerPropertyFile;


    /** The one instance of the idv port */
    int oneInstancePort = -1;

    /** flag for not running in one instance mode */
    boolean noOneInstance = false;

    /** This holds the preprocessed version of the command line arguments */
    String[] commandLineArgs;


    /** Are we running in off screen mode */
    private boolean isOffScreen = false;

    /** Should we list out the resources for debugging */
    protected boolean listResources = false;


    /** Is the isl scripting interactiveor batch */
    private boolean islInteractive = false;


    /** Should we load plugins */
    boolean pluginsOk = true;

    /**
     *  If we are in scripting mode then this list contains
     * Runnables that will be run when  the display has been finished.
     */
    List initRunnables = new ArrayList();

    /**
     *  If there is a default bundle file do we evaluate it.
     *  Turned off by the -nodefault argument.
     */
    private boolean doDefaultBundle = true;




    /**
     * Remove all default bundles
     */
    boolean doClearAllBundles = false;


    /**
     *  Set by the -default command line argument. Overrides the normal
     *  default bundles.
     */
    String defaultBundle;

    /**
     *  Do we read in the preferences
     *  Turned off by the -nopreferences argument.
     */
    boolean doPreferences = true;

    /**
     *  This holds the command line arguments that should be written out
     *  in saved jnlp files (e.g., .properties)
     */
    protected List persistentCommandLineArgs = new ArrayList();

    /**
     *  Given by the "-user" argument. Alternative user path for bundles,  resources, etc.
     */
    String defaultUserDirectory = null;

    /**
     *  Set by the -chooser argument
     */
    boolean showChooserOnInit = false;


    /**
     *  Created when we see the -test command line argument. Turns on testings.
     */
    public boolean testMode = false;


    /**
     *  Created when we see the -testeval command line argument.
     */
    public boolean testEval = false;


    /**
     *  Created when we see the -trace command line argument.
     */
    boolean traceMode = false;


    /** The name of the test archive when we are in test archive writing mode */
    public String testArchive;

    /** The name of the test directory when we are in test archive writing mode */
    public String testDir;


    /** Should any guis be shown */
    protected boolean noGui = false;


    /** Jython code that should be evaluated at startup time */
    protected String jythonCode = null;


    /**
     *  A list (String) of the data sources (files and/or urls) passed in
     *  on the command line with the ARG_DATA argument.
     */
    List initDataFiles = new ArrayList();

    /**
     *  A list (String) of the bundle sources (files and/or urls) passed in
     *  on the command line.
     */
    public List argXidvFiles = new ArrayList();

    /**
     *  A list (String) of the display xml  sources (files and/or urls) passed in
     *  on the command line.
     */
    List argDisplayXmlFiles = new ArrayList();

    /** A list  of the base 64 encoded in line display xml files */
    public List argDisplayB64Xml = new ArrayList();


    /**
     * A list  of the base 64 encoded in line bundles.
     * This argument is mostly used for the jnlp files that hold a bundle
     * From the  IdvConstants.ARG_B64BUNDLE argument.
     */
    public List b64Bundles = new ArrayList();

    /** List of plugins */
    public List plugins = new ArrayList();

    /** List of plugins */
    public List installPlugins = new ArrayList();

    /**
     * Command line jnlp files. You can start up the IDV with jnlp files as arguments.
     * The idv parses the jnlp xml and extracts out any embedded b64 bundles and evaluates
     * them.
     */
    List jnlpFiles = new ArrayList();

    /** Shuold the embedded bundles in jnlp files be printed out */
    protected boolean printJnlpBundles = false;

    /** List of isl files */
    public List scriptingFiles = new ArrayList();



    /**
     * Command line capture (.cpt) files. These hold the results of a previous event
     * capture and can be replayed  from the command line argument.
     */
    List captureFiles = new ArrayList();


    /** A list (String) of initial xml chooser  catalog sources (not used for now) */
    List initCatalogs = new ArrayList();

    /**
     * A list of command line property files.
     * These are defined with the  {@link IdvConstants.ARG_B64BUNDLE}
     */
    List propertyFiles = new ArrayList();

    /**
     * You can override the application sitepath through the command line.
     * This are defined with the  {@link IdvConstants.ARG_SITEPATH}
     */
    String sitePathFromArgs = null;

    /**
     * Holds the  list of parameter names.
     * <p>
     * You can specify a set of initial parameter/display pairs
     * that will automatically be created with every data source
     * that gets loaded.
     * This are defined with the  {@link IdvConstants.ARG_DISPLAY}
     */
    List initParams = new ArrayList();

    /**
     * Holds the  list of display names.
     * <p>
     * You can specify a set of initial parameter/display pairs
     * that will automatically be created with every data source
     * that gets loaded.
     * This are defined with the  {@link IdvConstants.ARG_DISPLAY}
     */
    List initDisplays = new ArrayList();


    /**
     * Holds the datasource/parameter/dispay template bundle uri
     */
    List displayTemplates = new ArrayList();


    /**
     * Holds  override property names.
     * <p>
     * You can override the property values from the idv property files
     * by specifying -Dname=value on the command line.
     */

    List argPropertyNames = new ArrayList();

    /**
     * Holds  override property values.
     * <p>
     * You can override the property values from the idv property files
     * by specifying -Dname=value on the command line.
     */
    List argPropertyValues = new ArrayList();


    /**
     * A list (String) of rbi xml sources for adding to the list of
     * resource bundle files
     */
    List argRbiFiles = new ArrayList();

    /**
     *  Command line argument of the hostname we should connect to
     * for collaboration.
     */
    String collabHostName = null;

    /**
     *  Command line argument of the port on the host we should connect to
     * for collaboration.
     */
    int collabPort = ucar.unidata.collab.Server.DEFAULT_PORT;

    /**
     * Should the collaboration server be started up.
     * This are defined with the  {@link IdvConstants.ARG_SERVER}
     */
    boolean doCollabServer = false;


    /** Holds the set files arguments */
    protected List fileMappingIds = new ArrayList();

    /** Holds the set files arguments */
    protected List fileMappingFiles = new ArrayList();


    /**
     * Create the manager with the given idv and command line arguments.
     * This just sets some state, it does not yet parse the arguments.
     * The IDV needs to call {@link #parseArgs()}
     *
     * @param idv The IDV
     * @param args Command line arguments
     */
    public ArgsManager(IntegratedDataViewer idv, String[] args) {
        super(idv);
        String[] tmpargs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            tmpargs[i] = args[i].trim();
        }
        originalArgs    = tmpargs;
        commandLineArgs = preprocessArgs(originalArgs);
        //TODO:        commandLineArgs = preprocessArgs(args);
    }


    /**
     * Get the very original command line arguments. The ones before we pre-process them.
     *
     * @return The command line args
     */
    public String[] getOriginalArgs() {
        return originalArgs;
    }

    /**
     * Method to return the initial catalogs
     *
     * @return List of urls for the catalogs to show at start up time
     */
    public List getInitCatalogs() {
        return initCatalogs;
    }

    /**
     * Called by the IDV when its initialization is complete.
     * This method will popup the chooser manager if needed,
     * run any initRunnables, and run any event capture files.
     */
    protected void initDone() {
        //If we have no scripting files to run we will turn on islInteractive
        //so the user can load and evaluate isl files interactively
        if (scriptingFiles.size() == 0) {
            islInteractive = true;
        } else {
            if ( !islInteractive) {
                doDefaultBundle = false;
                getStateManager().setRunningIsl(true);
                initRunnables.add(new Runnable() {
                    public void run() {
                        getImageGenerator().processScriptFiles(
                            scriptingFiles);
                    }
                });
            } else {
                Misc.run(new Runnable() {
                    public void run() {
                        Misc.sleep(100);
                        getImageGenerator().processScriptFiles(
                            scriptingFiles);
                    }
                });
            }
        }

        if (showChooserOnInit || (initCatalogs.size() > 0)) {
            getIdvChooserManager().show();
        }

        //Wait 1/10 second and start checking to see if the displays are done.
        //When done then run the runnables
        if (initRunnables.size() > 0) {
            //Turn off error dialogs, etc. 
            LogUtil.setTestMode(true);
            Runnable runnablesRunnable = new Runnable() {
                public void run() {
                    runInitRunnables();
                }
            };
            Misc.runInABit(100, runnablesRunnable);
        }


        //Now check for capture files
        for (int i = 0; i < captureFiles.size(); i++) {
            getCollabManager().runCaptureFile(captureFiles.get(i).toString());
        }
    }


    /**
     * Run the set of initial runnables
     */
    private void runInitRunnables() {
        //        waitUntilDisplaysAreDone(getIdvUIManager());
        for (int i = 0; i < initRunnables.size(); i++) {
            Runnable runnable = (Runnable) initRunnables.get(i);
            runnable.run();
        }
        getIdv().exit(0);
    }



    /**
     * Helper method to determine if the given  filename is a display xml file
     *
     * @param name The file name
     * @return Is the file a display xml file
     */
    public static boolean isDisplayXmlFile(String name) {
        return name.toLowerCase().endsWith(".dxml");
    }

    /**
     * Return a list of file filters that match on all of the types of bundles
     *
     * @return list of bundle file filters
     */
    public List<PatternFileFilter> getBundleFileFilters() {
        return (List<PatternFileFilter>) Misc.newList(getXidvFileFilter(),
                FILTER_JNLP, FILTER_ISL, getZidvFileFilter());
    }



    /**
     * Get the file filter to be used for a regular xidv bundle file
     *
     * @return bundle file filter
     */
    public PatternFileFilter getXidvFileFilter() {
        return FILTER_XIDV;
    }

    /**
     * Get the file filter to be used for a  zidv bundle file
     *
     * @return bundle file filter
     */
    public PatternFileFilter getZidvFileFilter() {
        return FILTER_ZIDV;
    }

    /**
     * Get the file filter that matches both xidv and zidv files
     *
     * @return file filter
     */
    public PatternFileFilter getXidvZidvFileFilter() {
        return FILTER_XIDVZIDV;
    }


    /**
     * Helper method to determine if the given  filename is a xidv bundle file
     *
     * @param name The file name
     * @return Is the file a bundle file
     */
    public boolean isXidvFile(String name) {
        return IOUtil.hasSuffix(
            name,
            getXidvFileFilter().getPreferredSuffix()) || IOUtil.hasSuffix(
                name, SUFFIX_XIDV);
    }

    /**
     * Is the given file a bindle
     *
     * @param name the file to check
     *
     * @return is a bundle or not
     */
    public boolean isBundleFile(String name) {
        return isXidvFile(name) || isZidvFile(name);
    }


    /**
     * Helper method to determine if the given  filename is an isl file
     *
     * @param name The file name
     * @return Is the file a bundle file
     */
    public boolean isIslFile(String name) {
        return IOUtil.hasSuffix(name, SUFFIX_ISL);
    }

    /**
     * is file a zidv file
     *
     * @param name file
     *
     * @return is zidv
     */
    public boolean isZidvFile(String name) {
        return IOUtil.hasSuffix(
            name,
            getZidvFileFilter().getPreferredSuffix()) || IOUtil.hasSuffix(
                name, SUFFIX_ZIDV);
    }


    /**
     * Helper method to determine if the given  filename is a resource bundle file
     *
     * @param name The file name
     * @return Is the file a resource bundle file
     */

    public boolean isRbiFile(String name) {
        return IOUtil.hasSuffix(name, SUFFIX_RBI);
    }


    /**
     * Helper method to determine if the given  filename is a jnlp file
     *
     * @param name The file name
     * @return Is the file a jnlp file
     */
    public boolean isJnlpFile(String name) {
        return IOUtil.hasSuffix(name, SUFFIX_JNLP);
    }


    /**
     * Should we create any  GUIs
     *
     * @return The no gui flag
     */
    public boolean getNoGui() {
        return noGui;
    }




    /**
     *  Print out the command line usage message and exit
     *
     * @param err The usage message
     */
    public void usage(String err) {
        String msg = USAGE_MESSAGE;
        msg = msg + "\n" + getUsageMessage();
        LogUtil.userErrorMessage(err + "\n" + msg);
        getIdv().exit(0);
    }


    /**
     * Utility to format the usage message line
     *
     * @param arg arg
     * @param desc desc
     *
     * @return Formatted message
     */
    protected String msg(String arg, String desc) {
        return "\t" + arg + "  " + desc + "\n";
    }


    /**
     * Is isl interactive mode
     *
     * @return isl interactive
     */
    public boolean getIslInteractive() {
        return islInteractive;
    }

    /**
     *     Return the command line usage message. Can be overwritten by derived classes
     *     to add in their own usage message.
     *    @return The usage message
     */
    protected String getUsageMessage() {
        return msg(ARG_HELP, "(this message)")
               + msg(ARG_PROPERTIES, "<property file>")
               + msg("-Dpropertyname=value", "(Define the property value)")
               + msg(ARG_INSTALLPLUGIN, "<plugin jar file or url to install>")
               + msg(ARG_PLUGIN, "<plugin jar file, directory, url for this run>")
               + msg(ARG_NOPLUGINS, "Don't load plugins")
               + msg(ARG_CLEARDEFAULT, "(Clear the default bundle)")
               + msg(ARG_NODEFAULT, "(Don't read in the default bundle file)")
               + msg(ARG_DEFAULT, "<.xidv file>")
               + msg(ARG_BUNDLE, "<bundle file or url>")
               + msg(ARG_B64BUNDLE, "<base 64 encoded inline bundle>")
               + msg(ARG_SETFILES, "<datasource pattern> <semi-colon delimited list of files> (Use the list of files for the bundled datasource)")
               + msg(ARG_ONEINSTANCEPORT, "<port number> (Check if another version of the IDV is running. If so pass command line arguments to it and shutdown)")
               + msg(ARG_NOONEINSTANCE, "(Don't do the one instance port)")
               + msg(ARG_NOPREF, "(Don't read in the user preferences)")
               + msg(ARG_USERPATH, "<user directory to use>")
               + msg(ARG_SITEPATH, "<url path to find site resources>")
               + msg(ARG_NOGUI, "(Don't show the main window gui)")
               + msg(ARG_DATA, "<data source> (Load the data source)")
               + msg(ARG_DISPLAY, "<parameter> <display>")
               + msg("<scriptfile.isl>", "(Run the IDV script in batch mode)")
               + msg(ARG_SCRIPT, "<jython script file to evaluate>")
               + msg(ARG_B64ISL, "<base64 encoded inline isl> This will run the isl in interactive mode")
               + msg(ARG_ISLINTERACTIVE, "run any isl files in interactive mode")
               + msg(ARG_IMAGE, "<image file name> (create a jpeg image and then exit)")
               + msg(ARG_MOVIE, "<movie file name> (create a quicktime movie and then exit)")
               + msg(ARG_IMAGESERVER, "<port number or .properties file> (run the IDV in image generation server mode. Support http requests on the given port)")
               + msg(ARG_CATALOG, "<url to a chooser catalog>")
               + msg(ARG_CONNECT, "<collaboration hostname to connect to>")
               + msg(ARG_SERVER, "(Should the IDV run in collaboration server mode)")
               + msg(ARG_PORT, "<Port number collaboration server should listen on>")
               + msg(ARG_CHOOSER, "(show the data chooser on start up) ")
               + msg(ARG_PRINTJNLP, "(Print out any embedded bundles from jnlp files)")
               + msg(ARG_CURRENTTIME, "<dttm> (Override current time for ISL processing)")
               + msg(ARG_LISTRESOURCES, "<list out the resource types")
               + msg(ARG_DEBUG, "(Turn on debug print)")
               + msg(ARG_MSG_DEBUG, "(Turn on language pack debug)")
               + msg(ARG_MSG_RECORD, "<Language pack file to write missing entries to>")
               + msg(ARG_TRACE, "(Print out trace messages)")
               + msg(ARG_NOERRORSINGUI, "(Don't show errors in gui)")
               + msg(ARG_TRACEONLY, "<trace pattern> (Print out trace messages that match the pattern)");
    }



    /**
     *  Check for any .jnlp files in the command line. If there are any then
     *  recurse and process the command line arguments in the jnlp file.
     *
     * @param args The incoming argument array
     * @return The preprocessed argument array
     */
    protected String[] preprocessArgs(String[] args) {
        List argList = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (isJnlpFile(arg)) {
                try {
                    Element root   = XmlUtil.getRoot(arg, getClass());
                    List arguments = XmlUtil.findDescendants(root,
                                         "argument");
                    for (int jnlpArgIdx = 0; jnlpArgIdx < arguments.size();
                            jnlpArgIdx++) {
                        String value = XmlUtil.getChildText(
                                           (Node) arguments.get(jnlpArgIdx));
                        if (value != null) {
                            argList.add(value);
                        }
                    }
                } catch (Exception exc) {
                    getIdv().logException(
                        "Processing command line jnlp file", exc);
                }
            } else {
                argList.add(arg);
            }
        }

        return StringUtil.listToStringArray(argList);
    }


    /**
     * Parse the command line arguments.
     *
     * @throws Exception When something untoward happens
     */
    public void parseArgs() throws Exception {
        parseArgs(commandLineArgs);
    }


    /**
     *  Parse the given argument array. If there are any errors
     * then call usage (which will exit).
     *
     * @param args The command line arguments
     *
     * @throws Exception When something untoward happens
     */
    protected void parseArgs(String[] args) throws Exception {
        int idx = 0;
        //        for (int i=0;i<args.length;i++) {
        //       LogUtil.consoleMessage ("Arg["+i+"] = " + args[i] +"\n");
        //        }

        while (idx < args.length) {
            String arg = args[idx];
            idx++;
            idx = parseArg(arg, args, idx);
        }
        //If we are in off screen mode (e.g., from an isl script) then don't have the LogUtil
        //show dialogs, etc.
        if (isOffScreen) {
            LogUtil.setTestMode(true);
        }

    }


    /**
     * A utility method for checking the argument array.
     *  If arg not equal to lookingFor then simply  return false.
     *  Else make sure that the given args array has at least howManyMore
     *  entries ahead of the given idx. If not then fail by calling usage.
     *
     * @param arg The current value in the args array
     * @param lookingFor The flag  we are looking for
     * @param args The full args array
     * @param idx The index of the current arg
     * @param howManyMore If arg==lookingFor then this is how many
     *                    more values the lookingFor flag requires.
     *                    If there are not that many left in the args
     *                    array then call usage and exit
     * @return Does the given arg match the given lookingFor value
     */
    protected boolean checkArg(String arg, String lookingFor, String[] args,
                               int idx, int howManyMore) {
        if ( !arg.equals(lookingFor)) {
            return false;
        }
        if ((idx + (howManyMore - 1)) >= args.length) {
            usage("No value given for " + lookingFor + "  argument");
        }
        return true;
    }


    /**
     *  Check the argument given by the arg parameter. The idx parameter
     *  points to the next unprocessed entry in the args array. If the argument
     *  requires  one or more values in the args array then increment idx accordingly.
     *  Return idx.
     *
     * @param arg The current argument we are looking at
     * @param args The full args array
     * @param idx The index into args that we are looking at
     * @return The idx of the last value in the args array we look at.
     *         i.e., if the flag arg does not require any further values
     *         in the args array then don't increment idx.  If arg requires
     *         one more value then increment idx by one. etc.
     *
     * @throws Exception When something untoward happens
     */
    protected int parseArg(String arg, String[] args, int idx)
            throws Exception {

        if (checkArg(arg, ARG_DATA, args, idx, 1)) {
            initDataFiles.add(args[idx++]);
        } else if (arg.startsWith("-D")) {
            persistentCommandLineArgs.add(arg);
            List l = StringUtil.split(arg.substring(2), "=");
            if (l.size() != 2) {
                usage("Invalid property:" + arg);
            }
            argPropertyNames.add(l.get(0));
            argPropertyValues.add(l.get(1));
        } else if (arg.equals(ARG_MAINCLASS)) {
            //This is handled in DefaultIdv
            idx++;
        } else if (arg.equals(ARG_SERVER)) {
            doCollabServer = true;
        } else if (arg.equals(ARG_PRINTJNLP)) {
            printJnlpBundles = true;
        } else if (checkArg(arg, ARG_SETFILES, args, idx, 2)) {
            fileMappingIds.add(args[idx++]);
            fileMappingFiles.add(StringUtil.split(args[idx++], ";"));
        } else if (checkArg(arg, ARG_CURRENTTIME, args, idx, 1)) {
            String dttm = args[idx++];
            Date   date = StringUtil.parseDate(dttm);
            if (date == null) {
                usage("Bad date format:" + dttm);
            }
            Misc.setCurrentTime(date);
        } else if (checkArg(arg, ARG_CONNECT, args, idx, 1)) {
            collabHostName = args[idx++];
        } else if (checkArg(arg, ARG_PORT, args, idx, 1)) {
            collabPort = new Integer(args[idx++]).intValue();
        } else if (checkArg(arg, ARG_CATALOG, args, idx, 1)) {
            initCatalogs.add(args[idx++]);
        } else if (arg.equals(ARG_NOPLUGINS)) {
            pluginsOk = false;
        } else if (checkArg(arg, ARG_INSTALLPLUGIN, args, idx, 1)) {
            installPlugins.add(args[idx++]);
        } else if (checkArg(arg, ARG_PLUGIN, args, idx, 1)) {
            plugins.add(args[idx++]);
        } else if (checkArg(arg, ARG_BUNDLE, args, idx, 1)) {
            argXidvFiles.add(args[idx++]);
        } else if (checkArg(arg, ARG_B64BUNDLE, args, idx, 1)) {
            b64Bundles.add(args[idx++]);
        } else if (checkArg(arg, ARG_PROPERTIES, args, idx, 1)) {
            String argValue = args[idx++];
            persistentCommandLineArgs.add(ARG_PROPERTIES);
            persistentCommandLineArgs.add(argValue);
            propertyFiles.add(argValue);
        } else if (checkArg(arg, ARG_SITEPATH, args, idx, 1)) {
            sitePathFromArgs = args[idx++];
            persistentCommandLineArgs.add(ARG_SITEPATH);
            persistentCommandLineArgs.add(sitePathFromArgs);
        } else if (arg.equals(ARG_CHOOSER)) {
            persistentCommandLineArgs.add(ARG_CHOOSER);
            showChooserOnInit = true;
        } else if (checkArg(arg, ARG_IMAGESERVER, args, idx, 1)) {
            String tmp = args[idx++];
            if (tmp.endsWith(".properties")) {
                imageServerPropertyFile = tmp;
            } else {
                imageServerPort = new Integer(tmp).intValue();
            }
            setIsOffScreen(true);
        } else if (arg.equals(ARG_NOONEINSTANCE)) {
            noOneInstance = true;
        } else if (checkArg(arg, ARG_ONEINSTANCEPORT, args, idx, 1)) {
            oneInstancePort = new Integer(args[idx++]).intValue();
            persistentCommandLineArgs.add(arg);
            persistentCommandLineArgs.add("" + oneInstancePort);
        } else if (checkArg(arg, ARG_IMAGE, args, idx, 1)) {
            setIsOffScreen(true);
            final String imageName = args[idx++];
            initRunnables.add(new Runnable() {
                public void run() {
                    getImageGenerator().captureImage(imageName);
                }
            });
        } else if (checkArg(arg, ARG_MOVIE, args, idx, 1)) {
            setIsOffScreen(true);
            final String imageName = args[idx++];
            initRunnables.add(new Runnable() {
                public void run() {
                    getImageGenerator().captureMovie(imageName);
                }
            });
        } else if (checkArg(arg, ARG_USERPATH, args, idx, 1)) {
            String argValue = args[idx++];
            File   f        = new File(argValue);
            if ( !f.exists()) {
                if ( !GuiUtils.askYesNo(
                        "User directory",
                        "Given -userpath directory does not exist. Do you want to create it?")) {
                    return idx;
                }
            } else if ( !f.isDirectory()) {
                usage("Given -userpath argument must be a directory");
            }
            persistentCommandLineArgs.add(ARG_USERPATH);
            persistentCommandLineArgs.add(argValue);
            defaultUserDirectory = argValue;
        } else if (arg.startsWith(ARG_NOPREF)) {
            persistentCommandLineArgs.add(arg);
            doPreferences = false;
        } else if (checkArg(arg, ARG_DEFAULT, args, idx, 1)) {
            defaultBundle = args[idx++];
        } else if (checkArg(arg, ARG_FIXEDTIME, args, idx, 1)) {
            fixedTimeString = args[idx++];
        } else if (arg.equals(ARG_DEBUG)) {
            LogUtil.setDebugMode(true);
        } else if (arg.equals(ARG_CLEARDEFAULT)) {
            doClearAllBundles = true;
        } else if (arg.equals("-2d")) {
            ucar.unidata.view.geoloc.MapProjectionDisplay.force2D = true;
        } else if (arg.equals(ARG_NODEFAULT)) {
            persistentCommandLineArgs.add(arg);
            doDefaultBundle = false;
        } else if (arg.equals(ARG_NOGUI)) {
            persistentCommandLineArgs.add(arg);
            noGui = true;
        } else if (checkArg(arg, ARG_CODE, args, idx, 1)) {
            jythonCode = args[idx++];
        } else if (checkArg(arg, ARG_SCRIPT, args, idx, 1)) {
            jythonCode = IOUtil.readContents(args[idx++]);
        } else if (isJnlpFile(arg)) {
            jnlpFiles.add(arg);
        } else if (arg.toLowerCase().endsWith(SUFFIX_CPT)) {
            captureFiles.add(arg);
        } else if (checkArg(arg, ARG_ISLFILE, args, idx, 1)) {
            scriptingFiles.add(args[idx++]);
            if ( !islInteractive) {
                setIsOffScreen(true);
            }
        } else if (arg.equals(ARG_LISTRESOURCES)) {
            listResources = true;
        } else if (checkArg(arg, ARG_B64ISL, args, idx, 1)) {
            scriptingFiles.add("b64:" + args[idx++]);
            islInteractive = true;
            setIsOffScreen(false);
        } else if (arg.equals(ARG_ISLINTERACTIVE)) {
            islInteractive = true;
            setIsOffScreen(false);
        } else if (isIslFile(arg)) {
            scriptingFiles.add(arg);
            if ( !islInteractive) {
                setIsOffScreen(true);
            }
        } else if (isXidvFile(arg)) {
            argXidvFiles.add(arg);
        } else if (isZidvFile(arg)) {
            argXidvFiles.add(arg);
        } else if (isRbiFile(arg)) {
            argRbiFiles.add(arg);
        } else if (checkArg(arg, ARG_DXML, args, idx, 1)) {
            argDisplayB64Xml.add(args[idx++]);
        } else if (isDisplayXmlFile(arg)) {
            argDisplayXmlFiles.add(arg);
        } else if (checkArg(arg, ARG_TEMPLATE, args, idx, 3)) {
            displayTemplates.add(args[idx++]);
            displayTemplates.add(args[idx++]);
            displayTemplates.add(args[idx++]);
        } else if (checkArg(arg, ARG_DISPLAY, args, idx, 2)) {
            initParams.add(args[idx++]);
            initDisplays.add(args[idx++]);
        } else if (arg.equals(ARG_HELP)) {
            System.err.println(USAGE_MESSAGE);
            System.err.println(getUsageMessage());
            getIdv().exit(0);
        } else if (arg.equals(ARG_NOERRORSINGUI)) {
            LogUtil.setShowErrorsInGui(false);
        } else if (arg.equals(ARG_TESTEVAL)) {
            testEval = true;
            testMode = true;
        } else if (checkArg(arg, ARG_TEST, args, idx, 2)) {
            testMode    = true;
            testArchive = args[idx++];
            testDir     = args[idx++];
        } else if (arg.equals(ARG_MSG_DEBUG)) {
            Msg.setShowDebug(true);
        } else if (checkArg(arg, ARG_MSG_RECORD, args, idx, 1)) {
            Msg.recordMessages(new File(args[idx++]));
        } else if (arg.equals(ARG_TRACE)) {
            traceMode = true;
        } else if (checkArg(arg, ARG_TRACEONLY, args, idx, 1)) {
            ucar.unidata.util.Trace.addOnly(args[idx++]);
            traceMode = true;
        } else {
            usage("Unknown argument:" + arg);
        }
        return idx;
    }



    /**
     * Gets called by the IDV to process the set of initial files, e.g.,
     * default bundles, command line bundles, jnlp files, etc.
     *
     * @throws VisADException When something untoward happens
     * @throws RemoteException When something untoward happens
     */
    protected void processInitialBundles()
            throws VisADException, RemoteException {
        if (doClearAllBundles) {
            getResourceManager().clearDefaultBundles();
            doDefaultBundle = false;
        }

        //Process any default bundles
        if ( !getIsOffScreen() && doDefaultBundle) {
            showWaitCursor();
            getIdvUIManager().splashMsg("Loading Defaults");
            LogUtil.message("Loading  defaults");
            if (defaultBundle != null) {
                String defaultXml = IOUtil.readContents(defaultBundle,
                                        (String) null);
                if (defaultXml != null) {
                    getPersistenceManager().decodeXml(defaultXml, false,
                            "Default bundle", false);
                }
            } else {
                ResourceCollection rc = getResourceManager().getResources(
                                            IdvResourceManager.RSC_BUNDLES);
                for (int i = 0; i < rc.size(); i++) {
                    String bundleXml = rc.read(i);
                    if (bundleXml == null) {
                        continue;
                    }
                    getPersistenceManager().decodeXml(bundleXml, false,
                            "Default bundle", false);
                    //Once we load one we quit
                    break;
                }
            }
            LogUtil.clearMessage("Loading  defaults");
            showNormalCursor();
        }


        //Now run through any command lines .xidv script files
        showWaitCursor();
        processXidvFiles(argXidvFiles);
        processBase64Bundles(b64Bundles);
        processJnlpFiles(jnlpFiles);

        for (int i = 0; i < argDisplayXmlFiles.size(); i++) {
            String displayFile = (String) argDisplayXmlFiles.get(i);
            String xml = IOUtil.readContents(displayFile, getIdvClass(),
                                             null);
            if (xml != null) {
                ControlDescriptor.processDisplayXml(getIdv(), xml);
            } else {
                //Tell the user.
            }
        }

        for (int i = 0; i < argDisplayB64Xml.size(); i++) {
            String b64Xml = (String) argDisplayB64Xml.get(i);
            String xml    = new String(XmlUtil.decodeBase64(b64Xml));
            ControlDescriptor.processDisplayXml(getIdv(), xml);
        }
        showNormalCursor();

    }

    /**
     * Process the base 64 encoded bundles
     *
     * @param bundles List of bundles
     */
    private void processBase64Bundles(List bundles) {
        for (int i = 0; i < bundles.size(); i++) {
            getPersistenceManager().loadB64Bundle((String) bundles.get(i));
        }
    }

    /**
     * Process the jnlp files
     *
     * @param jnlpFiles jnlp files
     */
    private void processJnlpFiles(List jnlpFiles) {
        for (int i = 0; i < jnlpFiles.size(); i++) {
            LogUtil.message("Loading bundle");
            showWaitCursor();
            getPersistenceManager().decodeJnlpFile((String) jnlpFiles.get(i));
            showNormalCursor();
            LogUtil.clearMessage("Loading bundle");
        }

    }


    /**
     * Process the bundles
     *
     * @param files The bundles
     */
    private void processXidvFiles(List files) {
        String lastFileName = null;
        for (int i = 0; i < files.size(); i++) {
            lastFileName = (String) files.get(i);
            LogUtil.message("Loading  bundle:" + lastFileName);
            getPersistenceManager().decodeXmlFile(lastFileName, false);
            LogUtil.clearMessage("Loading  bundle:" + lastFileName);
        }
        getPersistenceManager().setCurrentFileName(lastFileName);
    }


    /**
     * Process the command line argument we got passed form another instance of the idv
     *
     * @param args command line args
     */
    public void processInstanceArgs(String[] args) {
        List    jnlpFiles     = new ArrayList();
        List    xidvFiles     = new ArrayList();
        List    base64Bundles = new ArrayList();
        List    dataSources   = new ArrayList();
        List    islFiles      = new ArrayList();


        boolean gotOne        = false;
        for (int idx = 0; idx < args.length; idx++) {
            String arg     = args[idx];
            int    nextIdx = idx + 1;
            if (isJnlpFile(arg)) {
                jnlpFiles.add(arg);
                gotOne = true;
            } else if (isIslFile(arg)) {
                islFiles.add(arg);
            } else if (isXidvFile(arg)) {
                xidvFiles.add(arg);
                gotOne = true;
            } else if (checkArg(arg, ARG_BUNDLE, args, nextIdx, 1)) {
                argXidvFiles.add(args[++idx]);
                gotOne = true;
            } else if (checkArg(arg, ARG_B64BUNDLE, args, nextIdx, 1)) {
                base64Bundles.add(args[++idx]);
                gotOne = true;
            } else if (checkArg(arg, ARG_ISLFILE, args, nextIdx, 1)) {
                scriptingFiles.add(args[++idx]);
            } else if (arg.equals(ARG_DATA)) {
                dataSources.add(args[++idx]);
            }
        }

        if (gotOne) {
            boolean[] ok =
                getPreferenceManager().getDoRemoveBeforeOpening(null);
            if ( !ok[0]) {
                return;
            }
            if (ok[1]) {
                getIdv().removeAllDataSources();
                getIdv().removeAllDisplays();
            }
        }

        processXidvFiles(xidvFiles);
        processJnlpFiles(jnlpFiles);
        processBase64Bundles(base64Bundles);

        if (islFiles.size() > 0) {
            islInteractive = true;
            Misc.run(getImageGenerator(), "processScriptFiles", islFiles);
        }


        if (dataSources.size() > 0) {
            getIdv().loadDataFiles(dataSources);
        }

    }

    /**
     * Get the host name for the collab server
     *
     * @return collab server name
     */
    public String getCollabHostName() {
        return collabHostName;
    }


    /**
     * Should we start up a collab server
     *
     * @return start up a collab server
     */
    public boolean getDoCollabServer() {
        return doCollabServer;
    }

    /**
     * Port for collab server
     *
     * @return collab server port
     */
    public int getCollabPort() {
        return collabPort;
    }


    /**
     * the fixed time index string
     *
     * @return fixed time string - for later
     */
    public String getFixedTimeString() {
        return fixedTimeString;
    }

    /**
     * running in offscreen mode
     *
     * @return is offscreen
     */
    public boolean getIsOffScreen() {
        return isOffScreen;
    }

    /**
     * set running in offscreen mode
     *
     * @param v offscreen
     */
    public void setIsOffScreen(boolean v) {
        isOffScreen = v;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isScriptingMode() {
        return !islInteractive && (scriptingFiles.size() > 0);
    }


}
