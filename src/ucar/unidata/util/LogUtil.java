/*
 * $Id: LogUtil.java,v 1.98 2007/08/21 11:31:24 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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





package ucar.unidata.util;


import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.lang.management.*;




/**
 * A collection of utilities for doing logging and user messaging
 *
 * @author IDV development team
 */


import java.lang.reflect.InvocationTargetException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;


/**
 * Class LogUtil
 *
 *
 * @author Unidata development team
 */
public class LogUtil {

    /** The default category for loggin error messages */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(LogUtil.class.getName());

    /** Keeps track of the last error message */
    private static String lastErrorMessage = "";

    /** Keeps track of the time of the last error message */
    private static long lastErrorTime = 0;

    /**
     * When the application is running in test mode this is set
     * to true so no gui dialogs are shown
     */
    private static boolean testMode = false;

    private static boolean showErrorsInGui = true;

    /** When in test mode this is the list of exceptions that get thrown */
    private static ArrayList exceptions = new ArrayList();

    /** When in test mode this is the list of messages for errors */
    private static ArrayList msgs = new ArrayList();


    /** A debug flag. */
    private static boolean debug = false;



    /** Used for buffering stderr/stdout */
    private static ByteArrayOutputStream outputBuffer;

    /** Used for buffering stderr/stdout */
    private static PrintStream originalErr;

    /** Used for buffering stderr/stdout */
    private static PrintStream originalOut;

    /** The window the error console is in */
    private static JFrame consoleWindow;

    /** The text area that shows the errors in the error console */
    private static JTextArea consoleText;




    /**
     * private ctor so no one can instantiate a LogUtil
     */
    private LogUtil() {}


    /**
     * Helper  that creates a log category. We have this here so we can always
     * add in some logging facility later.
     *
     * @param name The name of the log instance
     *
     * @return The wrapper around the logger
     */
    public static LogCategory getLogInstance(String name) {
        return new LogCategory(name);
    }


    /**
     * Class LogCategory serves as a wrapper around some (undefined for now)
     * logging facility.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.98 $
     */
    public static class LogCategory {
        //        private Category category;

        /**
         * Create it
         *
         * @param name The name
         */
        public LogCategory(String name) {
            //            category = Category.getInstance(name);
        }

        /**
         * Log the debug message
         *
         * @param msg The message
         */
        public void debug(String msg) {
            if (debug) {
                consoleMessage(msg);
                System.err.println(msg);
            }
        }

        /**
         * Log the error message
         *
         * @param msg The message
         */
        public void error(String msg) {
            System.err.println("ERROR: " + msg);
        }


        /**
         * Log the error message
         *
         * @param msg The message
         */
        public void warn(String msg) {
            System.err.println("WARNING: " + msg);
        }

        /**
         * Is debug enabled?
         *
         * @return true if debug is enabled
         */
        public boolean isDebugEnabled() {
            return debug;
        }
    }


    /**
     * If we are in test mode have there been any errors logged.
     *
     * @return Any errors logged.
     */
    public static boolean anyErrors() {
        return (exceptions.size() > 0);
    }

    /**
     * Get the list of exceptions that were thrown when in test mode
     * @return List of exceptions from test mode
     */
    public static List getExceptions() {
        return exceptions;
    }

    /**
     * Get the list of messages for errors  when in test mode
     * @return List of error messages
     */
    public static List getMessages() {
        return msgs;
    }

    /**
     * Set the test mode. If true then we don't popup any error or message dialogs.
     * Instead we log the exceptions in a list that is dumped out later by any test
     * running code.
     *
     *
     * @param v The test mode flag
     */
    public static void setTestMode(boolean v) {
        LogUtil.testMode = v;
    }

    /**
     * Are we in test mode
     *
     * @return in test mode
     */
    public static boolean getTestMode() {
        return testMode;
    }

    public static boolean showGui() {
        return !getTestMode() && getShowErrorsInGui();
    }

    /**
     * set if we show the errors in the gui
     *
     * @param v The test mode flag
     */
    public static void setShowErrorsInGui(boolean v) {
        LogUtil.showErrorsInGui = v;
    }

    /**
     * Do we show errors in gui
     *
     * @return show errors in gui
     */
    public static boolean getShowErrorsInGui() {
        return showErrorsInGui;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static boolean getInteractiveMode() {
        return !testMode;
    }


    /**
     * Set the debug mode
     * @param v the debug flag (true to print out debug messages)
     */
    public static void setDebugMode(boolean v) {
        debug = v;
    }


    /**
     * Set the debug mode
     * @return the debug flag (true to print out debug messages)
     */
    public static boolean getDebugMode() {
        return debug;
    }


    /**
     * Configure the logging
     */
    public static void configure() {

        /**
         * Properties[] props = getProperties("log4j.properties", LogUtil.class);
         * for (int i = 0; i < props.length; i++) {
         *   PropertyConfigurator.configure(props[i]);
         * }
         */
    }


    /**
     *  Find all properties files with the given name relative to the package
     *  of the given Class. For now this just returns the one Properties
     *  file found. In the future we will want to look at the user's local properties, site properties, etc.
     *
     *  @param filename The property filename to look for.
     *  @param origin Where to look.
     *
     *  @return The properties.
     */
    private static Properties[] getProperties(String filename, Class origin) {
        ArrayList   l = new ArrayList();
        InputStream s = null;
        try {
            s = IOUtil.getInputStream(filename, origin);
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Could not open  property file:" + filename);
        }

        Properties p = new Properties();
        try {
            p.load(s);
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Could not open  property file:" + filename + " Exception:"
                + exc);
        }
        l.add(p);
        Properties[] sa = new Properties[l.size()];
        for (int i = 0; i < l.size(); i++) {
            sa[i] = (Properties) l.get(i);
        }
        return sa;
    }






    /**
     * Log the given error message and exception
     *
     * @param msg The error message
     * @param exc The exception
     */
    public static void logException(String msg, Throwable exc) {
        printException(log_, msg, exc);
    }


    /**
     * print out the list of error messages and exceptions. Do not
     * show them  in the gui.
     *
     * @param errorMessages
     * @param exceptions
     */
    public static void printExceptionsNoGui(List errorMessages,
                                            List exceptions) {
        if (exceptions == null) {
            return;
        }
        for (int i = 0; i < exceptions.size(); i++) {
            Exception exc     = (Exception) exceptions.get(i);
            String    message = (String) errorMessages.get(i);
            logException(message, exc);
        }
    }


    /**
     * Print out the  error messages and exceptions. Do not
     * show them  in the gui.
     *
     * @param errorMessage The message
     * @param exceptions The exceptions
     */
    public static void printExceptionsNoGui(String errorMessage,
                                            List exceptions) {
        if (exceptions == null) {
            return;
        }
        for (int i = 0; i < exceptions.size(); i++) {
            Exception exc = (Exception) exceptions.get(i);
            logException(errorMessage, exc);
        }
    }



    /**
     * Print out the list of exceptions
     *
     * @param exceptions The exceptions
     */
    public static void printExceptions(List exceptions) {
        if (exceptions == null) {
            return;
        }
        List messages = new ArrayList();
        //Load up an array of empty messages
        for (int i = 0; i < exceptions.size(); i++) {
            messages.add(((Exception) exceptions.get(i)).getMessage());
        }
        printExceptions(messages, exceptions);
    }


    /**
     * This allows you to print to stderr even when we are buffering
     *
     * @param msg The text to print
     */
    public static void println(String msg) {
        if (originalErr != null) {
            originalErr.println(msg);
        } else {
            System.err.println(msg);
        }
    }


    /**
     * Start buffering stderr and stdout
     */
    public static void startOutputBuffer() {
        if (outputBuffer != null) {
            return;
        }
        outputBuffer = new ByteArrayOutputStream();
        originalErr  = System.err;
        originalOut  = System.out;
        System.setErr(new PrintStream(outputBuffer));
        System.setOut(new PrintStream(outputBuffer));
    }

    /**
     * Get the text that has been written to stderr/stdout when in buffering mode
     *
     * @param andClearIt Clear the buffer
     *
     * @return The buffer
     */
    public static String getOutputBuffer(boolean andClearIt) {
        if (outputBuffer == null) {
            return null;
        }
        String s = outputBuffer.toString();
        if (andClearIt) {
            outputBuffer.reset();
        }
        return s;
    }

    /**
     * Stop buffering stderr and stdout
     */
    public static void stopOutputBuffer() {
        outputBuffer = null;
        if (originalErr != null) {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }
    }



    /**
     * Print out the  list of error messages and exceptions.
     * If not in test mode then show them in a gui. If in test mode then
     * just print to stderr.
     *
     * @param errorMessages The error messages
     * @param exceptions The exceptions
     */
    public static void printExceptions(List errorMessages, List exceptions) {
        printExceptions("Errors have occured", errorMessages, exceptions);
    }

    /**
     * Print exceptions
     *
     * @param label label for the dialog
     * @param errorMessages  list of error messages
     * @param exceptions List of exceptions
     */
    public static void printExceptions(String label, List errorMessages,
                                       List exceptions) {
        if (exceptions == null) {
            return;
        }
        if (exceptions.size() == 1) {
            logException((String) errorMessages.get(0),
                         (Throwable) exceptions.get(0));
            return;
        }
        if (!showGui()) {
            printExceptionsNoGui(errorMessages, exceptions);
            return;
        }
        JComponent contents = LayoutUtil.topCenter(new JLabel(label),
                                  getMultiExceptionsPanel(errorMessages,
                                      exceptions));
        GuiUtils.showDialog("Errors", contents, getCurrentWindow());
    }


    /**
     * Get any exception that the given exception wraps (if it does).
     *
     * @param exc The possible exception containing exception
     * @return The wrapped exception or the given exception if it does not wrap one.
     */
    public static Throwable getInnerException(Throwable exc) {
        if (exc == null) {
            return null;
        }
        Throwable inner = null;
        if (exc instanceof WrapperException) {
            inner = ((WrapperException) exc).getException();
        } else if (exc instanceof InvocationTargetException) {
            inner = ((InvocationTargetException) exc).getTargetException();
        } else {
            Throwable cause = exc.getCause();
            if(cause!=null) {
                inner = cause;
            }
        }
        if (inner != null) {
            Throwable innerInner = getInnerException(inner);
            if (innerInner != null) {
                inner = innerInner;
            }
        }
        if (inner == null) {
            return exc;
        }
        return inner;
    }

    /**
     * Create the panel that shows multiple exceptions
     *
     * @param errorMessages The  error messages
     * @param exceptions The exceptions
     * @return The panel that shows the multiple exceptions
     */
    public static JPanel getMultiExceptionsPanel(List errorMessages,
            List exceptions) {
        final JTextArea tv = new JTextArea(15, 60);
        //        final JScrollPane sp = new JScrollPane (tv, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        final JScrollPane sp    = new JScrollPane(tv);
        ArrayList         comps = new ArrayList();
        for (int i = 0; i < exceptions.size(); i++) {
            Throwable exc     = (Throwable) exceptions.get(i);
            String    message = "";
            if ((errorMessages != null) && (i < errorMessages.size())) {
                message = (String) errorMessages.get(i);
            }
            if (message.length() == 0) {
                message = exc.getMessage();
            }

            JButton jb = new JButton("Details");
            jb.addActionListener(new ObjectListener(new ObjectArray(message,
                    exc)) {
                public void actionPerformed(ActionEvent ae) {
                    ObjectArray  oa           = (ObjectArray) theObject;
                    Throwable    theException = (Throwable) oa.getObject2();
                    StringBuffer stackMessage = new StringBuffer();
                    Throwable innerException =
                        getInnerException(theException);
                    if (innerException != null) {
                        stackMessage.append("Contained exception:"
                                            + innerException.getMessage()
                                            + "\n");
                        stackMessage.append(getStackTrace(innerException));
                        stackMessage.append("Exception thrown at:\n");
                    }
                    theException.printStackTrace();
                    stackMessage.append(theException.getMessage() + "\n");
                    stackMessage.append(getStackTrace(theException));
                    sp.getViewport().setViewPosition(new Point(0, 0));
                    String msg = stackMessage.toString();
                    consoleMessage(msg);
                    System.err.println(msg);
                    tv.setText(msg);
                    sp.getViewport().setViewPosition(new Point(0, 0));
                    tv.setCaretPosition(0);

                }
            });
            comps.add(jb);
            comps.add(new JLabel("  " + message + "  "));
        }
        JPanel jp = LayoutUtil.doLayout(LayoutUtil.getComponentArray(comps),
                                        2, LayoutUtil.WT_NY, LayoutUtil.WT_N);
        JScrollPane jpScroll = GuiUtils.makeScrollPane(LayoutUtil.top(jp),
                                   100, 100);
        jpScroll.setPreferredSize(new Dimension(100, 100));
        jpScroll.setSize(new Dimension(100, 100));
        JPanel contents = LayoutUtil.topCenter(jpScroll, sp);
        return contents;
    }


    /**
     * Return the stack trace of this calling thread
     *
     * @return  The stack trace
     */
    public static String getStackTrace() {
        return Misc.getStackTrace();
    }


    /**
     * Get the stack trace from the given exception
     *
     * @param exc The exception to get the trace from
     * @return The stack trace
     */
    public static String getStackTrace(Throwable exc) {
        return Misc.getStackTrace(exc);
    }



    /**
     *  Create (if needed) the Console window and show it
     */
    public static void showConsole() {
        if (consoleWindow == null) {
            checkConsole();
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    consoleText.setText("");
                }
            });
            JButton writeBtn = new JButton("Write to file");
            writeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String filename =
                        FileManager.getWriteFile(FileManager.FILTER_LOG,
                            FileManager.SUFFIX_LOG);
                    if (filename == null) {
                        return;
                    }
                    try {
                        IOUtil.writeFile(filename, consoleText.getText());
                    } catch (Exception exc) {
                        LogUtil.logException("Writing to file:" + filename,
                                             exc);
                    }

                }
            });


            JScrollPane sp =
                new JScrollPane(
                    consoleText,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JViewport vp = sp.getViewport();
            vp.setViewSize(new Dimension(300, 400));
            JPanel contents = LayoutUtil.centerBottom(
                                  sp,
                                  LayoutUtil.wrap(
                                      LayoutUtil.hflow(
                                          Misc.newList(clearBtn, writeBtn))));
            consoleWindow = GuiUtils.makeWindow("Console", contents, 10, 10);
        }
        consoleWindow.show();
    }


    /**
     *  Create the consoleText JTextArea if needed
     */
    private static void checkConsole() {
        if (consoleText == null) {
            consoleText = new JTextArea(10, 30);
            consoleText.setEditable(false);
        }
    }

    /**
     *  Append the given msg to the console text area. This will not show the console.
     *  To do that call showConsole.
     *
     * @param msg
     */
    public static void consoleMessage(String msg) {
        checkConsole();
        msg = Msg.msg(msg);
        consoleText.append(msg + "\n");
    }

    /**
     * DialogManager is an interface that allows an applcation to add its own buttons
     * to the error dialogs
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.98 $
     */
    public interface DialogManager {

        /**
         * Add any buttons to the given list that should show up in the error dialog
         *
         * @param dialog The dialog
         * @param buttonList List to put buttons into
         * @param msg The error message
         * @param exc The exception that was thrown
         */
        public void addErrorButtons(JDialog dialog, List buttonList,
                                    String msg, Throwable exc);
    }

    /** Allows applications to add their own LogUtil.DialogManager */
    private static DialogManager dialogManager;

    /**
     * Allows applications to add their own LogUtil.DialogManager
     *
     * @param manager The singleton dialog manager to use
     */
    public static void setDialogManager(DialogManager manager) {
        dialogManager = manager;
    }

    /**
     * A queue of the currently active windows.
     * We use this here so when a modal dialog is brought up that we use the most recent
     * window from this list as the parent window of the dialog
     */
    private static List currentWindows = new ArrayList();

    /**
     * Finds the most recent active window. If there are none then it returns null.
     *
     * @return The most recently active window.
     */
    public static Window getCurrentWindow() {
        synchronized (currentWindows) {
            if (currentWindows.size() == 0) {
                return null;
            }
            return (Window) currentWindows.get(currentWindows.size() - 1);
        }
    }


    /**
     * This registers a window listener on the given window to add and remove the
     * the  window to the list of active windows.
     *
     * @param w The window
     */
    public static void registerWindow(final Window w) {
        WindowListener windowListener = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //                    System.err.println("window closing");
                currentWindows.remove(w);
                e.getWindow().removeWindowListener(this);
            }

            public void windowActivated(WindowEvent e) {
                synchronized (currentWindows) {
                    if (currentWindows.contains(w)) {
                        currentWindows.remove(w);
                    }
                    currentWindows.add(w);
                }
            }

            public void windowClosed(WindowEvent e) {
                synchronized (currentWindows) {
                    //                    System.err.println("closed - before: " + currentWindows);
                    currentWindows.remove(w);
                    e.getWindow().removeWindowListener(this);
                    //                    System.err.println("closed - after: " + currentWindows);
                }
            }

            public void windowIconified(WindowEvent e) {
                synchronized (currentWindows) {
                    currentWindows.remove(w);
                }
            }
        };

        w.addWindowListener(windowListener);
    }



    /**
     * Show the given error message/exception in the gui (if not in test mode),
     * print it to the console and log it to the given Category
     *
     * @param log_ The LogCatgory to log the error to
     * @param xmsg The error message
     * @param exc The exception
     */
    public static void printException(LogCategory log_, String xmsg,
                                      Throwable exc) {
        printException(log_, xmsg, exc, (File) null);
    }


    /**
     * Print the exception
     *
     * @param log_ log category
     * @param xmsg message
     * @param exc exception
     * @param fileBytes if non-null then write to a tmp file and tell
     * the user where to find it.
     */
    public static void printException(LogCategory log_, String xmsg,
                                      Throwable exc, byte[] fileBytes) {
        File f = null;
        if (fileBytes != null) {
            f = CacheManager.getTmpFile("error");
            try {
                IOUtil.writeBytes(f, fileBytes);
            } catch (Exception fileException) {
                f = null;
            }
        }
        printException(log_, xmsg, exc, f);
    }


    /**
     * Print the exception
     *
     * @param log_ log category
     * @param xmsg message
     * @param originalException exception
     * @param file If non-null then this is the tmp file that was written
     * Tell the user about it.
     */
    public static void printException(LogCategory log_, String xmsg,
                                      Throwable originalException,
                                      File file) {


        xmsg = Msg.msg(xmsg);
        //      Misc.printStack("\n******* LogUtil", 10,null);

        Throwable exc        = originalException;
        Throwable wrappedExc = getInnerException(originalException);
        if (wrappedExc != null) {
            exc = wrappedExc;
        }
        String excMessage = exc.getMessage();
        //Add the message if its a wrapper exception
        if (originalException instanceof WrapperException) {
            String msg = originalException.getMessage();
            if ((excMessage != null) && (msg != null) && (msg.length() > 0)
                    && !msg.trim().equals(excMessage.trim())) {
                excMessage = msg + " " + excMessage;
            }
        }

        if (excMessage == null) {
            excMessage = "\n" + exc.getClass().getName();
        } else {
            excMessage = "\n" + excMessage;
        }

        //"An error has occurred:\n"
        String msg = xmsg + " " + excMessage;
        if (file != null) {
            msg = msg + "\n\n" + "View file at:" + file;
        }


        long time = System.currentTimeMillis();
        //If we have the same message and it has been less than 5 seconds
        //since the last one was shown then skip this one
        if (msg.equals(lastErrorMessage) && (time - lastErrorTime) < 1000) {
            return;
        }
        lastErrorMessage = msg;
        consoleMessage(msg);
        consoleMessage(getStackTrace(exc));

        if (log_ != null) {
            log_.error(msg);
        }


        if (showGui()) {

            JDialog tmpDialog;
            final JDialog dialog = GuiUtils.createDialog(getCurrentWindow(),
                                       "Error", true);

            final JPanel bottomPanel    = new JPanel(new BorderLayout());
            JButton      showConsoleBtn = new JButton("Show Console");
            List         buttonList     = new ArrayList();

            if (dialogManager != null) {
                dialogManager.addErrorButtons(dialog, buttonList, msg, exc);
            }

            final JButton detailsBtn  = new JButton("Show Details");
            JTextArea     detailsArea = new JTextArea(getStackTrace(exc));

            detailsArea.setBackground(new JPanel().getBackground());
            detailsArea.setEditable(false);
            JScrollPane sp = new JScrollPane(detailsArea);
            sp.setPreferredSize(new Dimension(500, 200));

            JButton         closeBtn     = new JButton("OK");
            final boolean[] showing      = { false };
            final JPanel    detailsPanel = LayoutUtil.center(sp);


            closeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    lastErrorTime = System.currentTimeMillis();
                    dialog.dispose();
                }
            });
            detailsBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (showing[0]) {
                        bottomPanel.removeAll();
                        detailsBtn.setText("Show Details");
                    } else {
                        bottomPanel.add(detailsPanel);
                        detailsBtn.setText("Hide Details");
                    }
                    showing[0] = !showing[0];
                    bottomPanel.invalidate();
                    dialog.pack();
                }
            });
            JComponent errorLbl = new JLabel(
                                      GuiUtils.getImageIcon(
                                          "/auxdata/ui/icons/Error.gif"));
            errorLbl = LayoutUtil.top(LayoutUtil.inset(errorLbl,
                    new Insets(8, 8, 8, 8)));

            buttonList.add(detailsBtn);
            buttonList.add(closeBtn);
            LayoutUtil.tmpInsets = new Insets(0, 4, 0, 4);
            JComponent buttons = LayoutUtil.doLayout(buttonList,
                                     buttonList.size(), LayoutUtil.WT_N,
                                     LayoutUtil.WT_N);

            buttons = LayoutUtil.inset(buttons, 5);
            JComponent messageComp =
                LayoutUtil.inset(getMessageComponent(msg),
                                 new Insets(8, 0, 8, 8));


            JComponent topPanel = LayoutUtil.leftCenter(errorLbl,
                                      messageComp);
            topPanel.setBackground(Color.red);


            JComponent contents = LayoutUtil.centerBottom(topPanel, buttons);
            contents = LayoutUtil.doLayout(new Component[] { contents,
                    bottomPanel }, 1, LayoutUtil.WT_Y, LayoutUtil.WT_Y);
            dialog.getContentPane().add(contents);
            dialog.pack();
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            Dimension dialogSize = dialog.getSize();
            dialog.setLocation(Math.max(0, screenSize.width / 2
                                        - dialogSize.width / 2), Math.max(0,
                                            screenSize.height / 2
                                            - dialogSize.height / 2));
            dialog.show();
            //            javax.swing.JOptionPane.showMessageDialog(
            //                null, contents, "Error", JOptionPane.ERROR_MESSAGE);
        }

        //        userErrorMessage(log_, msg);
        exc.printStackTrace(System.err);
        if (!showGui()) {
            exceptions.add(exc);
            msgs.add(xmsg);
        }
    }



    /**
     * Log the given error message/exception to the given LogCategory
     *
     * @param log_ The LogCategory to log the error to
     * @param xmsg The error message
     * @param exc The exception
     */
    public static void printExceptionNoGui(LogCategory log_, String xmsg,
                                           Throwable exc) {
        if (exc instanceof java.lang.reflect.InvocationTargetException) {
            exc = ((java.lang.reflect.InvocationTargetException) exc)
                .getTargetException();
        }
        consoleMessage(getStackTrace(exc));
        exc.printStackTrace(System.err);
        if (LogUtil.getTestMode()) {
            exceptions.add(exc);
            msgs.add(xmsg);
        }
    }

    /**
     *  Simply print the given message using the default logging LogCategory. The effect
     *  of this is to print the message to the stderr and to the console.
     *
     * @param msg
     */
    public static void printMessage(String msg) {
        log_.error(msg);
        consoleMessage(msg);
    }


    /**
     * Show an informational  message to the user. We have this here
     * so we can control when a  dialog is shown (we don't show them when in
     * test mode).
     *
     * @param msg The message
     */
    public static void userMessage(String msg) {
        userMessage(null, msg, false);
    }

    /**
     * Show an informational  message to the user. We have this here
     * so we can control when a  dialog is shown (we don't show them when in
     * test mode). Also log the message to the given  LogCategory (if not null).
     *
     * @param log_ The category to log to. May be null.
     * @param msg The message
     */
    public static void userMessage(LogCategory log_, String msg) {
        userMessage(log_, msg, false);
    }

    /**
     * Show an informational  message to the user. We have this here
     * so we can control when a  dialog is shown (we don't show them when in
     * test mode). Also log the message to the given LogCategory (if not null).
     *
     * @param log_ The category to log to. May be null.
     * @param msg The message
     * @param consoleMsg What to show in the error console
     */
    public static void userMessage(LogCategory log_, String msg,
                                   String consoleMsg) {
        userMessage(log_, msg);
        log_.error(consoleMsg);
    }

    /**
     * Show an informational  message to the user. We have this here
     * so we can control when a  dialog is shown (we don't show them when in
     * test mode). Also log the message to the given  LogCategory (if not null).
     *
     * @param log_ The category to log to. May be null.
     * @param msg The message
     * @param andLog  Should we also log it to the log_
     */
    public static void userMessage(LogCategory log_, String msg,
                                   boolean andLog) {
        msg = Msg.msg(msg);
        if (andLog && (log_ != null)) {
            log_.error(msg);
        }
        if (showGui()) {
            consoleMessage(msg);
            JLabel label = new JLabel(msg);
            GuiUtils.addModalDialogComponent(label);
            javax.swing.JOptionPane.showMessageDialog(getCurrentWindow(),
                    label);
            GuiUtils.removeModalDialogComponent(label);
        } else {
            if ( !(andLog && (log_ != null))) {
                System.err.println(msg);
            }
        }
    }


    /**
     * Get the jcomponent that displays the given message string
     *
     * @param msg message
     *
     * @return jcomponent that displays the message
     */
    private static JComponent getMessageComponent(String msg) {
        if (msg.startsWith("<html>")) {
            Component[] comps = GuiUtils.getHtmlComponent(msg, null, 500,
                                    400);
            return (JScrollPane) comps[1];
        }


        if (msg.length() < 50) {
            return new JLabel(msg);
        }
        List         lines = StringUtil.split(msg, "\n");
        StringBuffer sb    = new StringBuffer();
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            line = StringUtil.breakText(line, "\n", 50);
            sb.append(line + "\n");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setFont(textArea.getFont().deriveFont(Font.BOLD));
        textArea.setBackground(new JPanel().getBackground());
        textArea.setEditable(false);
        JScrollPane textSp = GuiUtils.makeScrollPane(textArea, 400, 200);
        textSp.setPreferredSize(new Dimension(400, 200));
        return textSp;
    }

    /**
     * Show the error dialog to the user
     *
     * @param log_  category to log to (if not null).
     * @param msg The message
     */
    public static void userErrorMessage(LogCategory log_, String msg) {
        msg = Msg.msg(msg);
        if (log_ != null) {
            log_.error(msg);
        }
        if (showGui()) {
            consoleMessage(msg);
            JComponent msgComponent = getMessageComponent(msg);
            GuiUtils.addModalDialogComponent(msgComponent);
            javax.swing.JOptionPane.showMessageDialog(getCurrentWindow(),
                    msgComponent, "Error", JOptionPane.ERROR_MESSAGE);
            GuiUtils.removeModalDialogComponent(msgComponent);
        } else {
            System.err.println(msg);
        }
    }



    /**
     * Show the given label in an error dialog. If for some reason we are in no gui mode then just print out the text of the label
     *
     * @param label label
     */
    public static void userErrorMessage(JLabel label) {
        if (showGui()) {
            GuiUtils.addModalDialogComponent(label);
            javax.swing.JOptionPane.showMessageDialog(getCurrentWindow(),
                                                      label, "Error", JOptionPane.ERROR_MESSAGE);
            GuiUtils.removeModalDialogComponent(label);
        } else {
            System.err.println(label.getText());
        }
    }

    /**
     * Show the error dialog to the user
     *
     * @param msg The message
     */
    public static void userErrorMessage(String msg) {
        userErrorMessage(null, msg);
    }


    /**
     * Show the error dialog to the user
     *
     * @param msg May be a String or a Component
     */
    public static void userErrorMessage(Object msg) {
        if (showGui()) {
            if ( !(msg instanceof Component)) {
                msg = new JLabel(msg.toString());
            }
            GuiUtils.addModalDialogComponent((Component) msg);
            javax.swing.JOptionPane.showMessageDialog(getCurrentWindow(),
                    msg, "Error", JOptionPane.ERROR_MESSAGE);
            GuiUtils.removeModalDialogComponent((Component) msg);
        } else {
            System.err.println(msg);
        }
    }


    /**
     * Holds components from the application GUIs that messages should be shown
     */
    private static ArrayList messageLogs = new ArrayList();

    /** The last message that was shown */
    private static String lastMessageString = "";


    /**
     * Add the given component into the list of components that show messages
     * from the message method calls
     *
     * @param t The text component
     */
    public static void addMessageLogger(JTextArea t) {
        messageLogs.add(t);
        //        Misc.printStack("LogUtil.addMessageLogger:" + messageLogs.size());
    }

    /**
     * Add the given component into the list of components that show messages
     * from the message method calls
     *
     * @param t The text component
     */
    public static void addMessageLogger(JLabel t) {
        messageLogs.add(t);
        //        Misc.printStack("LogUtil.addMessageLogger:" + messageLogs.size());
    }

    /**
     * Remove the given component from the list of message components
     *
     * @param t The component to remove
     */
    public static void removeMessageLogger(Object t) {
        messageLogs.remove(t);
        //        System.err.println("remove message logs:" + messageLogs.size());
    }




    /**
     * If the given message is the same as the last shown message
     * then clear all messages
     *
     * @param message The message to clear
     */
    public static void clearMessage(String message) {
        if ((lastMessageString != null)
                && message.equals(lastMessageString)) {
            message("");
        }
    }


    /**
     * Show the given string in all of the message components
     *
     * @param msg The message to show
     */
    public static void message(String msg) {

        msg               = Msg.msg(msg);
        lastMessageString = msg;
        for (int i = 0; i < messageLogs.size(); i++) {
            Object logger = messageLogs.get(i);
            if (logger instanceof JTextArea) {
                if (msg.trim().length() > 0) {
                    ((JTextArea) logger).append(msg + "\n");
                    ((JTextArea) logger).repaint();
                }
            } else if (logger instanceof JLabel) {
                ((JLabel) logger).setText(msg);
                ((JLabel) logger).repaint();
            }
        }
    }

    /** Helps to track down println calls */
    public static int printlncnt = 0;

    /** Helps to track down println calls */
    public static void tracePrintlns() {
        System.setErr(new java.io.PrintStream(new ByteArrayOutputStream()) {
            public void println(String s) {
                doit(s);
            }

            public void println(Object x) {
                doit("" + x);
            }

            private void doit(String s) {
                if (printlncnt > 0) {
                    System.out.println("Recurse:" + s);
                    super.println(s);
                    return;
                }
                printlncnt++;
                System.out.println("PRINT:" + s);
                Exception exc = new IllegalArgumentException("");
                exc.printStackTrace();
                printlncnt--;
            }
        });
    }

    /**
     * return the stack trace of all threads
     *
     * @param asHtml If true generate html instead of text
     *
     * @return stack dump
     */
    public static StringBuffer getStackDump(boolean asHtml) {
        return getStackDump(asHtml, false);
    }

    /**
     * return the stack trace of all threads
     *
     * @param asHtml If true generate html instead of text
     * @param onlyRunning Only show the running threads
     *
     * @return stack dump
     */
    public static StringBuffer getStackDump(boolean asHtml,
                                            boolean onlyRunning) {
        StringBuffer longSB = new StringBuffer();
        if (asHtml) {
            longSB.append("<pre>\n");
        }
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[]       ids        = threadBean.getAllThreadIds();
        StringBuffer blockedSB  = new StringBuffer();
        StringBuffer otherSB    = new StringBuffer();
        for (int i = 0; i < ids.length; i++) {
            ThreadInfo info = threadBean.getThreadInfo(ids[i],
                                  Integer.MAX_VALUE);
            if (info == null) {
                continue;
            }
            if (onlyRunning) {
                if (info.getThreadState() != Thread.State.RUNNABLE) {
                    continue;
                }
            }
            StackTraceElement[] stack = info.getStackTrace();
            String              extra = "";
            String              style = "";
            StringBuffer        sb    = otherSB;
            if (info.getThreadState() == Thread.State.WAITING) {
                extra = " on " + info.getLockName();
            } else if (info.getThreadState() == Thread.State.BLOCKED) {
                style = "  background-color:#cccccc; ";
                if (asHtml) {
                    extra = " on " + info.getLockName()
                            + " held by <a href=\"#id"
                            + info.getLockOwnerId() + "\">"
                            + info.getLockOwnerName() + " id:"
                            + info.getLockOwnerId() + "</a>";
                } else {
                    extra = " on " + info.getLockName() + " held by "
                            + info.getLockOwnerId() + info.getLockOwnerName()
                            + " id:" + info.getLockOwnerId();
                }
                sb = blockedSB;
            }
            if (asHtml) {
                sb.append("<a name=\"id" + ids[i] + "\"></a>");
                sb.append("<span style=\"" + style + "\">&quot;"
                          + info.getThreadName() + "&quot;" + " ID:" + ids[i]
                          + "  " + info.getThreadState() + extra
                          + "</span>\n");
            } else {
                sb.append("\"" + info.getThreadName() + "\"" + " ID:"
                          + ids[i] + "  " + info.getThreadState() + extra
                          + "\n");
            }
            String space = (asHtml
                            ? "&nbsp;&nbsp;&nbsp;&nbsp;"
                            : "    ");
            for (int stackIdx = 0; stackIdx < stack.length; stackIdx++) {
                sb.append(space);
                sb.append(stack[stackIdx] + "\n");
            }
            sb.append("\n\n");
        }
        longSB.append(blockedSB);
        longSB.append(otherSB);
        if (asHtml) {
            longSB.append("</pre>");
        }
        return longSB;
    }




}

