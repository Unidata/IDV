/*
 * $Id: LogUtil.java,v 1.98 2007/08/21 11:31:24 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.awt.event.*;

import java.awt.image.BufferedImage;
import java.io.*;

import java.lang.management.*;




/**
 * A collection of utilities for doing logging and user messaging
 *
 * @author IDV development team
 */


import java.lang.reflect.InvocationTargetException;


import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.*;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;


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

    private static JFrame resultFrame;
    private static JTextArea resultArea;
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_WAIT_MILLIS = 1000; // 1 second
    private static final Random random = new Random();

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
         * Properties[] props = getProperties("log*j.properties", LogUtil.class);
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
        final JScrollPane sp = new JScrollPane (tv, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER );
       // final JScrollPane sp    = new JScrollPane(tv);
        ArrayList         comps = new ArrayList();
        for (int i = 0; i < exceptions.size(); i++) {
            Throwable exc     = (Throwable) exceptions.get(i);
            String    message = "";
            if ((errorMessages != null) && (i < errorMessages.size())) {
                message = (String) errorMessages.get(i);
            }
            if (message != null && message.length() == 0) {
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
        jpScroll.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
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
            /*
            JButton jsBtn = new JButton("run GEMINI");
            jsBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String filename = "/Users/yuanho/Downloads/";

                    if (filename == null) {
                        return;
                    }
                    runGemini(filename);
                }
            });
             */

            JScrollPane sp =
                new JScrollPane(
                    consoleText,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    HORIZONTAL_SCROLLBAR_NEVER);
            JViewport vp = sp.getViewport();
            vp.setViewSize(new Dimension(300, 400));
            JPanel contents = LayoutUtil.centerBottom(
                                  sp,
                                  LayoutUtil.wrap(
                                      LayoutUtil.hflow(
                                              Misc.newList(clearBtn, writeBtn))));
                                       //   Misc.newList(clearBtn, writeBtn, jsBtn))));
            consoleWindow = GuiUtils.makeWindow("Console", contents, 10, 10);
        }
        consoleWindow.setVisible(true);
    }

    /**
     * Sends an image to the Gemini API for analysis and returns the result.
     * Note: This is a hypothetical implementation and requires a valid Gemini API endpoint and key.
     *
     * @param image The local path to the image file.
     * @return A string containing the analysis from the Gemini API, or an error message.
     */
    public static String runGemini(BufferedImage image) {
        if (image == null) {
            String errorMsg = "Input BufferedImage cannot be null.";
            // log_.error(errorMsg);
            System.err.println(errorMsg);
            return errorMsg;
        }
        /* set up ADT Bulletin display area */
        if(resultArea == null) {
            resultArea = new JTextArea();
            resultArea.setEditable(false);
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resultArea.setText("");
                }
            });
            Font c = new Font("Courier", Font.BOLD, 12);

            resultFrame = new JFrame("GEMINI Results");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JScrollPane resultScroller = new JScrollPane(resultArea);
            resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel contents = LayoutUtil.centerBottom(
                    resultScroller,
                    LayoutUtil.wrap(
                            LayoutUtil.hflow(
                                    Misc.newList(clearBtn))));
            resultFrame.add(contents, BorderLayout.CENTER);
            resultFrame.setPreferredSize(new Dimension(400, 600));
            resultFrame.setFont(c);
        }

        try {
            // 1. Read image bytes and encode to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos); // Using JPEG format
            byte[] imageBytes = baos.toByteArray();

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 2. IMPORTANT: Replace with your actual Gemini API key.
            String apiKey = " ";
            // Updated to use the Gemini 2.5 Pro model
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;

            // 3. Construct the JSON payload based on the Gemini REST API documentation
            String jsonPayload = "{" +
                    "  \"contents\": [{" +
                    "    \"parts\": [" +
                    "      {\"text\": \"What is in this picture?\"}," +
                    "      {\"inline_data\": {" +
                    "        \"mime_type\": \"image/jpeg\"," +
                    "        \"data\": \"" + base64Image + "\"" +
                    "      }}" +
                    "    ]" +
                    "  }]" +
                    "}";

            // 4. Use Java's HttpClient to send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Process the response
            if (response.statusCode() == 200) {
                // NOTE: This is a simplified parser. For production, use a JSON library (e.g., Gson, Jackson).
                String responseBody = response.body();
                String out = extractTextFromGeminiResponse(responseBody);
                consoleMessage(out);
                return out;
            } else {
                String errorMsg = "Error from Gemini API: " + response.statusCode() + " " + response.body();
                log_.error(errorMsg);
                return errorMsg;
            }

        } catch (IOException | InterruptedException e) {
            logException("Failed to run Gemini analysis on " + image, e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return "Failed to process image for Gemini analysis.";
        }
    }

    /**
     * Analyzes an image using the Gemini API, with a robust retry mechanism for transient errors.
     *
     * @param image       The image to be analyzed.
     * @param instruction The prompt for the analysis.
     * @return The text result from Gemini or an error message.
     */
    public static String runGeminiOld(BufferedImage image, String instruction) {
        if (image == null) {
            String errorMsg = "Input BufferedImage cannot be null.";
            // log_.error(errorMsg);
            System.err.println(errorMsg);
            return errorMsg;
        }

        if(resultArea == null) {
            resultArea = new JTextArea();
            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            Font c = new Font("Courier", Font.BOLD, 12);
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resultArea.setText("");
                }
            });
            resultFrame = new JFrame("GEMINI Results");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JScrollPane resultScroller = new JScrollPane(resultArea);
            resultScroller.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
            resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel contents = LayoutUtil.centerBottom(
                    resultScroller,
                    LayoutUtil.wrap(
                            LayoutUtil.hflow(
                                    Misc.newList(clearBtn))));
            resultFrame.add(contents, BorderLayout.CENTER);
            //resultFrame.add(resultScroller, BorderLayout.CENTER);
            resultFrame.setPreferredSize(new Dimension(400, 600));
            resultFrame.setFont(c);
        }

        consoleMessage(". ");
        consoleMessage(". ");
        consoleMessage("INSTRUCTION: " + instruction);
        consoleMessage("> ");
        String msg = Msg.msg(instruction);
        resultArea.append(msg + "\n");
        resultArea.append("> "+ "\n");

        try {
            // 1. Convert BufferedImage to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            byte[] imageBytes = baos.toByteArray();

            // 2. Encode to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 3. Set API Key and URL (The URL does not change)
            String apiKey = " ";
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;

            // 4. Prepare the instruction for the JSON payload
            String prompt = (instruction == null || instruction.trim().isEmpty())
                    ? "Please Provide Summary of this weather map?" // Default instruction
                    : instruction;
            // Escape quotes in the instruction to prevent breaking the JSON structure.
            String escapedPrompt = prompt.replace("\"", "\\\"");

            // 5. Construct the JSON payload with the dynamic instruction
            String jsonPayload = "{" +
                    "  \"contents\": [{" +
                    "    \"parts\": [" +
                    // The user's instruction is placed here
                    "      {\"text\": \"" + escapedPrompt + "\"}," +
                    "      {\"inline_data\": {" +
                    "        \"mime_type\": \"image/jpeg\"," +
                    "        \"data\": \"" + base64Image + "\"" +
                    "      }}" +
                    "    ]" +
                    "  }]," +
                    "  \"generationConfig\": {" +
                    "    \"temperature\": 0.3," +
                    "    \"topK\": 32," +
                    "    \"topP\": 1.0," +
                    "    \"maxOutputTokens\": 8192," +
                    "    \"stopSequences\": []" +
                    "  }" +
                    "}";

            // 6. Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 7. Process the response
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String out = extractTextFromGeminiResponse(responseBody);
                consoleMessage(out);
                msg = Msg.msg(out);
                resultArea.append(msg + "\n");
                resultFrame.pack();
                resultFrame.setVisible(true);
                return out;
            } else {
                String errorMsg = "Error from Gemini API: " + response.statusCode() + " " + response.body();
                consoleMessage(errorMsg);
                msg = Msg.msg(errorMsg);
                resultArea.append(msg + "\n");
                resultFrame.pack();
                resultFrame.setVisible(true);
                System.err.println(errorMsg);
                return errorMsg;
            }

        } catch (IOException | InterruptedException e) {
            // logException("Failed to run Gemini analysis on the image.", e);
            System.err.println("Failed to process image for Gemini analysis: " + e.getMessage());
            Thread.currentThread().interrupt();
            return "Failed to process image for Gemini analysis.";
        }
    }
    /**
     * Analyzes an image using the Gemini API, with a robust retry mechanism for transient errors.
     *
     * @param image       The image to be analyzed.
     * @param instruction The prompt for the analysis.
     * @return The text result from Gemini or an error message.
     */
    public static String runGemini(BufferedImage image, String instruction) {
        if (image == null) {
            String errorMsg = "Input BufferedImage cannot be null.";
            System.err.println(errorMsg);
            return errorMsg;
        }

        initializeUI(); // Encapsulated UI setup for clarity
        updateUIWithMessage("INSTRUCTION: " + instruction + "\n> ");

        try {
            // 1. Prepare image data
            // 1. Convert BufferedImage to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            byte[] imageBytes = baos.toByteArray();

            // 2. Encode to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 2. Prepare API details
            String VALIDATION_URL = "https://generativelanguage.googleapis.com/v1beta/models";
            HttpClient client = HttpClient.newHttpClient();

            UserInfo userInfo = GeminiKeyValidator( VALIDATION_URL);

            String apiKey = userInfo.getPassword();
            //" "; // IMPORTANT: Do not hardcode keys in production
            // NOTE: The official public model is 'gemini-1.5-pro-latest'.
            // Using your string in case you have private access.
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;

            // 3. Construct JSON payload safely using a library
            String jsonPayload = buildJsonPayload(instruction, base64Image);

            // 4. Send request with retry logic

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            return sendRequestWithRetry(client, request);

        } catch (IOException e) {
            System.err.println("Failed to process image for Gemini analysis: " + e.getMessage());
            return "Failed to process image for Gemini analysis.";
        }
    }

    /**
     * A utility function to validate a Google Gemini API key.
     */
    public static UserInfo GeminiKeyValidator(String VALIDATION_URL) {
        HttpClient client = HttpClient.newHttpClient();
        AccountManager userAccountManager =
                AccountManager.getGlobalAccountManager();
        UserInfo userInfo = null;
        if (userAccountManager != null) {
            String host = "GOOGLE";
            userInfo =
                    userAccountManager.getAppKey("gemini", "<html>The server: <i>" + host
                            + "<i> requires a APP Key</html>") ;
            if (userInfo == null) {
                return null;
            }
            // 2. Construct the request URI
            URI validationUri = URI.create(VALIDATION_URL + "?key=" + userInfo.getPassword());
            // 3. Build the HTTP GET request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(validationUri)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            try {
                // 4. Send the request and get the response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 5. Interpret the response status code
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    // 200 OK is a definitive success
                    //return ValidationStatus.VALID;
                } else if (statusCode == 400) {
                    // 400 Bad Request is what Gemini often returns for invalid keys.
                    // We can check the response body for a more specific message.
                    if (response.body() != null && response.body().contains("API key not valid")) {
                        //need to remove this invalid key
                        userAccountManager.getTable().remove("gemini");
                        return null;
                    }
                } else {
                    // Any other non-200 code is considered an API or configuration error.
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                // 6. Handle network or interruption errors
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                }
                System.err.println("Validation failed due to a network or interruption error: " + e.getMessage());
                return null;
            }
        }

        return userInfo;
    }

    /**
     * A utility function to call Google Gemini API.
     */

    private static String sendRequestWithRetry(HttpClient client, HttpRequest request) {
        long waitMillis = INITIAL_WAIT_MILLIS;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                consoleMessage(String.format("Attempt %d of %d...", attempt + 1, MAX_RETRIES));
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String extractedText = extractTextFromGeminiResponse(response.body());
                    updateUIWithMessage(extractedText);
                    return extractedText;
                }

                // Check for retriable server errors (5xx)
                if (response.statusCode() >= 500 && response.statusCode() < 600) {
                    consoleMessage(String.format("Received server error %d. Retrying in ~%dms.", response.statusCode(), waitMillis));
                    // This is a retriable error, continue to the sleep and retry logic
                } else {
                    // Non-retriable error (e.g., 400 Bad Request, 401 Auth error). Stop immediately.
                    String errorMsg = "Error from Gemini API: " + response.statusCode() + " " + response.body();
                    System.err.println(errorMsg);
                    updateUIWithMessage(errorMsg);
                    return errorMsg;
                }

            } catch (IOException | InterruptedException e) {
                consoleMessage("Request failed due to network error or interruption. Retrying...");
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                }
            }

            // If we are not on the last attempt, wait before retrying
            if (attempt < MAX_RETRIES - 1) {
                try {
                    // Exponential backoff with jitter
                    long jitter = random.nextInt(500);
                    TimeUnit.MILLISECONDS.sleep(waitMillis + jitter);
                    waitMillis *= 2; // Double the wait time for the next attempt
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String errorMsg = "Retry wait was interrupted. Aborting.";
                    System.err.println(errorMsg);
                    updateUIWithMessage(errorMsg);
                    return errorMsg;
                }
            }
        }

        String finalErrorMsg = "Failed to get a response from Gemini after " + MAX_RETRIES + " attempts.";
        System.err.println(finalErrorMsg);
        updateUIWithMessage(finalErrorMsg);
        return finalErrorMsg;
    }

    /**
     * A utility function to output a Google Gemini result.
     */
    private static void updateUIWithMessage(String message) {
        //consoleMessage(message); // Log to console
        String msg = message; // Your Msg.msg() can go here if needed
        SwingUtilities.invokeLater(() -> {
            resultArea.append(msg + "\n");
            resultFrame.pack();
            resultFrame.setVisible(true);
        });
    }

    /**
     * A utility function to init a run Gemini UI.
     */
    private static void initializeUI() {
        if (resultArea == null) {
            resultArea = new JTextArea();
            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            Font c = new Font("Courier", Font.BOLD, 12);
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(ae -> resultArea.setText(""));
            resultFrame = new JFrame("GEMINI Results");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JScrollPane resultScroller = new JScrollPane(resultArea);
            resultScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            resultScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel contents = new JPanel(new BorderLayout()); // Simplified layout
            contents.add(resultScroller, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.add(clearBtn);
            contents.add(bottomPanel, BorderLayout.SOUTH);

            resultFrame.add(contents);
            resultFrame.setPreferredSize(new Dimension(400, 600));
            resultFrame.setFont(c);
            resultFrame.pack(); // Pack after adding components
        }
    }

    /**
     * A utility function to process Gemini response.
     */
    private static String extractTextFromGeminiResponse(String responseBody) {
        JSONParser parser = new JSONParser();
        try {
            // Step 1: Parse the string into a generic object
            Object obj = parser.parse(responseBody);

            // Step 2: Cast the object to a JSONObject
            JSONObject jsonResponse = (JSONObject) obj;

            // Step 3: Navigate the structure, casting each element as you go
            JSONArray candidates = (JSONArray) jsonResponse.get("candidates");
            JSONObject candidate = (JSONObject) candidates.get(0);
            JSONObject content = (JSONObject) candidate.get("content");
            JSONArray parts = (JSONArray) content.get("parts");
            JSONObject firstPart = (JSONObject) parts.get(0);

            return (String) firstPart.get("text");
        } catch (Exception e) {
            System.err.println("Failed to parse Gemini response: " + e.getMessage());
            return "Error: Could not parse the response from Gemini. Response Body: " + responseBody;
        }
    }

    /**
     * A utility function to generate JSON structure for Gemini API.
     */
    private static String buildJsonPayload(String instruction, String base64Image) {
        String prompt = (instruction == null || instruction.trim().isEmpty())
                ? "Please Provide Summary of this weather map?"
                : instruction;

        // --- Create the main payload object ---
        JSONObject payload = new JSONObject();

        // --- Create the 'contents' array ---
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONArray parts = new JSONArray();
        // Use .add() for older org.json library versions
        parts.add(textPart);
        parts.add(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contentsArray = new JSONArray();
        // Use .add() for older org.json library versions
        contentsArray.add(content);

        // Add the 'contents' array to the main payload
        payload.put("contents", contentsArray);

        // --- Create the 'generationConfig' object ---
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("topK", 32);
        generationConfig.put("topP", 1.0);
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("stopSequences", new JSONArray()); // An empty JSON array

        // Add the 'generationConfig' object to the main payload
        payload.put("generationConfig", generationConfig);

        // Return the final JSON string
        return payload.toString();
    }

    /**
     * Analyzes an image using the Gemini API, with a robust retry mechanism for transient errors.
     *
     * @param image       The image to be analyzed.
     * @param instruction The prompt for the analysis.
     * @return The text result from Gemini or an error message.
     */
    public static String runGemini(BufferedImage image, String instruction,BufferedImage exampleImage1, String example1Analysis,
                                   BufferedImage exampleImage2, String example2Analysis) {
        if (image == null) {
            String errorMsg = "Input BufferedImage cannot be null.";
            // log_.error(errorMsg);
            System.err.println(errorMsg);
            return errorMsg;
        }

        if(resultArea == null) {
            resultArea = new JTextArea();
            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            Font c = new Font("Courier", Font.BOLD, 12);
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resultArea.setText("");
                }
            });
            resultFrame = new JFrame("GEMINI Results");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JScrollPane resultScroller = new JScrollPane(resultArea);
            resultScroller.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
            resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            JPanel contents = LayoutUtil.centerBottom(
                    resultScroller,
                    LayoutUtil.wrap(
                            LayoutUtil.hflow(
                                    Misc.newList(clearBtn))));
            resultFrame.add(contents, BorderLayout.CENTER);
            //resultFrame.add(resultScroller, BorderLayout.CENTER);
            resultFrame.setPreferredSize(new Dimension(400, 600));
            resultFrame.setFont(c);
        }

        consoleMessage(". ");
        consoleMessage(". ");
        consoleMessage("INSTRUCTION: " + instruction);
        consoleMessage("> ");
        String msg = Msg.msg(instruction);
        resultArea.append(msg + "\n");
        resultArea.append("> "+ "\n");

        try {
            // 3. Set API Key and URL (The URL does not change)
            String apiKey = " ";
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;

            StringBuilder contentsJson = new StringBuilder();
            contentsJson.append("[");
            boolean hasPreviousContent = false;

            // Example 1
            if (exampleImage1 != null && example1Analysis != null && !example1Analysis.trim().isEmpty()) {
                ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
                ImageIO.write(exampleImage1, "jpeg", baos1);
                String base64Image1 = Base64.getEncoder().encodeToString(baos1.toByteArray());
                String escapedAnalysis1 = example1Analysis.replace("\"", "\\\"");

                contentsJson.append("{ \"role\": \"user\", \"parts\": [");
                contentsJson.append("{\"text\": \"What is in this picture?\"},");
                contentsJson.append("{\"inline_data\": {\"mime_type\": \"image/jpeg\", \"data\": \"").append(base64Image1).append("\"}}");
                contentsJson.append("]},");
                contentsJson.append("{ \"role\": \"model\", \"parts\": [");
                contentsJson.append("{\"text\": \"").append(escapedAnalysis1).append("\"}");
                contentsJson.append("]}");
                hasPreviousContent = true;
            }

            // Example 2
            if (exampleImage2 != null && example2Analysis != null && !example2Analysis.trim().isEmpty()) {
                if (hasPreviousContent) {
                    contentsJson.append(",");
                }
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ImageIO.write(exampleImage2, "jpeg", baos2);
                String base64Image2 = Base64.getEncoder().encodeToString(baos2.toByteArray());
                String escapedAnalysis2 = example2Analysis.replace("\"", "\\\"");

                contentsJson.append("{ \"role\": \"user\", \"parts\": [");
                contentsJson.append("{\"text\": \"What is in this picture?\"},");
                contentsJson.append("{\"inline_data\": {\"mime_type\": \"image/jpeg\", \"data\": \"").append(base64Image2).append("\"}}");
                contentsJson.append("]},");
                contentsJson.append("{ \"role\": \"model\", \"parts\": [");
                contentsJson.append("{\"text\": \"").append(escapedAnalysis2).append("\"}");
                contentsJson.append("]}");
                hasPreviousContent = true;
            }


            // 1. Convert BufferedImage to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            byte[] imageBytes = baos.toByteArray();

            // 2. Encode to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 4. Prepare the instruction for the JSON payload
            String prompt = (instruction == null || instruction.trim().isEmpty())
                    ? "Please Provide Summary of this weather map?" // Default instruction
                    : instruction;
            // Escape quotes in the instruction to prevent breaking the JSON structure.
            String escapedPrompt = prompt.replace("\"", "\\\"");

            if (hasPreviousContent) {
                contentsJson.append(",");
            }

            contentsJson.append("{ \"role\": \"user\", \"parts\": [");
            // The user's instruction is placed here
            contentsJson.append("{\"text\": \"").append(escapedPrompt).append("\"},");
            contentsJson.append("{\"inline_data\": {\"mime_type\": \"image/jpeg\", \"data\": \"").append(base64Image).append("\"}}");
            contentsJson.append("]}");

            contentsJson.append("]");

            // 5. Construct the JSON payload with the dynamic instruction
            String jsonPayload = "{\"contents\": " + contentsJson.toString() + "}";

            // 6. Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 7. Process the response
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String out = extractTextFromGeminiResponse(responseBody);
                consoleMessage(out);
                msg = Msg.msg(out);
                resultArea.append(msg + "\n");
                resultFrame.pack();
                resultFrame.setVisible(true);
                return out;
            } else {
                String errorMsg = "Error from Gemini API: " + response.statusCode() + " " + response.body();
                consoleMessage(errorMsg);
                msg = Msg.msg(errorMsg);
                resultArea.append(msg + "\n");
                resultFrame.pack();
                resultFrame.setVisible(true);
                System.err.println(errorMsg);
                return errorMsg;
            }

        } catch (IOException | InterruptedException e) {
            // logException("Failed to run Gemini analysis on the image.", e);
            System.err.println("Failed to process image for Gemini analysis: " + e.getMessage());
            Thread.currentThread().interrupt();
            return "Failed to process image for Gemini analysis.";
        }
    }

    /**
     * A helper to extract text from a simplified Gemini API JSON response.
     * For production, a proper JSON parsing library should be used.
     * @param jsonResponse The JSON string from the API.
     * @return The extracted text content.
     */
    private static String extractTextFromGeminiResponse1(String jsonResponse) {
        try {
            // This is a very brittle way to parse JSON. Avoid in production code.
            String searchText = "\"text\": \"";
            int startIndex = jsonResponse.indexOf(searchText);
            if (startIndex != -1) {
                startIndex += searchText.length();
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return jsonResponse.substring(startIndex, endIndex).replace("\\n", "\n");
                }
            }
            log_.warn("Could not parse text from Gemini response: " + jsonResponse);
            return "Could not parse text from Gemini response.";
        } catch (Exception e) {
            logException("Error parsing Gemini JSON response", e);
            return "Error parsing response.";
        }
    }

    /**
     *  Create the consoleText JTextArea if needed
     */
    private static void checkConsole() {
        if (consoleText == null) {
            consoleText = new JTextArea(10, 30);
            consoleText.setLineWrap(true);
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
        if(!LogUtil.getInteractiveMode()) {
            System.err.println(xmsg);
           // return;
        }
        else
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
            dialog.setVisible(true);
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
            JComponent msgComponent = getMessageComponent(msg);
            GuiUtils.addModalDialogComponent(msgComponent);
            javax.swing.JOptionPane.showMessageDialog(getCurrentWindow(),
                msgComponent);
            GuiUtils.removeModalDialogComponent(msgComponent);
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
        List<String>  lines = StringUtil.split(msg, "\n");
        StringBuilder sb    = new StringBuilder(msg.length() * 2);
        for (String line : lines) {
            line = StringUtil.breakText(line, "\n", 50);
            sb.append(line).append('\n');
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

