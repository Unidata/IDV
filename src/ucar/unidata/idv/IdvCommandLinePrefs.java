/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;


import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


/**
 * This class has two purposes.
 *
 * 1. It helps supply command line arguments to the IDV via bash or the
 * Windows Batch Scripting environment. Ultimately, it dumps all the user
 * preferences defined in the user path main.xml into a format that can be read
 * by bash or the Windows Batch Scripting environment. Moreover, if some
 * variables are not present, it provides some sensible defaults.
 *
 * 2. It examines the version of the IDV running at present, and compares it
 * against the latest IDV version available from Unidata. If the users' IDV
 * version is out-of-date, the user will be presented with a dialog indicating
 * this problem and a link to the download page.
 */
public class IdvCommandLinePrefs {

    /** THE URL containing the currently running version of the IDV */
    private static final String IDV_VERSION_URL =
        "http://www.unidata.ucar.edu/software/idv/docs/javadoc/index.html";

    /** Default user pref file. */
    private static final String DEFAULT_USER_PREF_FILE = "main.xml";

    /** The IDV preference. */
    private static final String IDV_MEMORY = "idv.memory";

    /** Default user pref path. */
    private static final String DEFAULT_USER_PREF_PATH =
        IOUtil.joinDirs(System.getProperty("user.home"), ".unidata", "idv",
                        "DefaultIdv");

    /** The current version that can be downloaded from Unidata */
    private static final IDVVersion currentIDVVersion =
        getCurrentIDVVersion();

    /**
     * Main app to get user memory settings in megabytes.
     *
     * @param args
     *          the arguments
     */
    @SuppressWarnings("unchecked")
    public static void main(String... args) {
        final StringBuilder sb = new StringBuilder();

        try {
            final Map<Object, Object> userPrefMap = getPrefMap(args);
            String oldVersionKey = "idv_old_version_"
                                   + getIDVVersion().stringForShell() + "_"
                                   + currentIDVVersion.stringForShell()
                                   + "_dontwarn";

            Object oldVersionDontWarn = userPrefMap.get(oldVersionKey);
            boolean dontShowOldWarn =
                Boolean.parseBoolean((oldVersionDontWarn == null)
                                     ? false + ""
                                     : oldVersionDontWarn.toString());
            boolean dontShowOldWarnRsp = false;
            if (isIDVold() && !dontShowOldWarn) {
                dontShowOldWarnRsp = showOldVersionMessage();
            }

            for (Map.Entry<Object, Object> e : userPrefMap.entrySet()) {

                // Need to replace . with _ to make bash happy
                String s = e.getKey().toString().replace(".", "_") + "=\""
                           + e.getValue() + "\""
                           + System.getProperty("line.separator");

                sb.append(s);
            }
            //User said not to warn so must persist this information.
            if (dontShowOldWarnRsp) {
                final File f = new File(getPreferences(args));
                if (f.exists()) {
                    Element root = XmlUtil.getRoot(f.getPath(),
                                       XmlUtil.class);
                    Document document = root.getOwnerDocument();

                    ///Define the method element from the ground up.
                    XmlEncoder xmlEncoder = new XmlEncoder();
                    final Element warn = xmlEncoder.createElement(
                                             new Boolean(dontShowOldWarn));
                    final Element oldVersion =
                        xmlEncoder.createPrimitiveElement(
                            XmlEncoder.NAME_STRING, oldVersionKey);

                    Element methodElement =
                        xmlEncoder.createMethodElement(XmlEncoder.METHOD_PUT,
                            new ArrayList<Element>() {
                        {
                            add(oldVersion);
                            add(warn);
                        }
                    });

                    //Totally preserve existing XML except appending "methodElement"
                    root.appendChild(document.importNode(methodElement,
                            true));

                    BufferedWriter writer =
                        new BufferedWriter(new FileWriter(f));

                    writer.write(XmlUtil.toString(root, true));

                    writer.close();
                }
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        System.out.println(sb.toString());
    }

    /**
     * Gets the HTML for the old version message.
     *
     * @return the HTML
     */
    private static String getHTML() {
        return "<!doctype html>\n" + "<html lang=\"en\">\n" + "<head>\n"
               + "  <meta charset=\"utf-8\">\n"
               + "  <title>New IDV Version</title>\n"
               + "  <meta name=\"New IDV Version\" content=\"New IDV Version\">\n"
               + "</head>\n" + "<body>\n"
               + "<p>There is a new verion of the IDV (" + currentIDVVersion
               + ") available.</p>"
               + "<a href=\"http://www.unidata.ucar.edu/downloads/idv/current/index.jsp\">Download now.</a>"
               + "</body>\n" + "</html>";
    }

    /**
     * Show old version message.
     *
     * @return true, if the user does not want this message anymore
     */
    private static boolean showOldVersionMessage() {
        JEditorPane      ep  = new JEditorPane("text/html", getHTML());
        final JCheckBox  cb  = new JCheckBox("Don't show again");
        CheckboxListener cbl = new CheckboxListener(cb);
        cb.addItemListener(cbl);

        JPanel doLayout = LayoutUtil.doLayout(new Component[] { ep, cb }, 1,
                              LayoutUtil.WT_Y, LayoutUtil.WT_Y);

        ep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType().equals(
                        HyperlinkEvent.EventType.ACTIVATED)) {
                    Desktop desktop = Desktop.isDesktopSupported()
                                      ? Desktop.getDesktop()
                                      : null;
                    if ((desktop != null)
                            && desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            desktop.browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        ep.setEditable(false);
        ep.setBackground(new JLabel().getBackground());

        JOptionPane.showOptionDialog(null, doLayout, "New IDV Version",
                                     JOptionPane.OK_CANCEL_OPTION,
                                     JOptionPane.INFORMATION_MESSAGE, null,
                                     new String[] { "Close" }, "default");
        return cbl.show;
    }


    /**
     * Gets the pref map.
     *
     * @param args
     *          the args
     * @return the pref map
     * @throws IOException
     *           Signals that an I/O exception has occurred.
     * @throws Exception
     *           the exception
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> getPrefMap(String... args)
            throws IOException, Exception {
        final Map<Object, Object> userPrefMap = new HashMap<Object, Object>();
        final File                f           =
            new File(getPreferences(args));

        if (f.exists()) {
            userPrefMap.putAll(
                (Map<Object,
                     Object>) (new XmlEncoder().createObject(
                         XmlUtil.getRoot(f.getPath(), XmlUtil.class))));
        }

        vetSettings(userPrefMap);

        return userPrefMap;
    }

    /**
     * Herein lies code to check if any settings will make the IDV blow up. Will
     * fix parameters if necessary
     *
     * @param userPrefMap
     *          the user pref map
     */
    private static void vetSettings(final Map<Object, Object> userPrefMap) {
        checkMemory(userPrefMap);

        // Eventually check more stuff
        if ( !userPrefMap.containsKey(IdvConstants.PREF_MAX_PERMGENSIZE)) {
            userPrefMap.put(IdvConstants.PREF_MAX_PERMGENSIZE,
                            IdvConstants.DEFAULT_MAX_PERMGENSIZE);
        }
    }

    /**
     * Herein lies code to check if the memory setting will make the IDV blow up.
     * Will fix memory if necessary
     *
     * @param userPrefMap
     *          the user pref map
     */
    private static void checkMemory(final Map<Object, Object> userPrefMap) {
        if (userPrefMap.containsKey(IDV_MEMORY)) {
            userPrefMap.put(
                IDV_MEMORY,
                SystemMemoryManager.checkAndRepair(
                    (Long) userPrefMap.get(IDV_MEMORY)));
        } else {
            userPrefMap.put(IDV_MEMORY,
                            SystemMemoryManager.getDefaultMemory());
        }
    }

    /**
     * Gets the preferences.
     *
     * @param args
     *          the args
     * @return the preferences
     */
    private static String getPreferences(String... args) {
        String userPath = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i - 1].equals("-userpath")) {
                userPath = args[i];
            }
        }

        return (userPath == null)
               ? IOUtil.joinDir(DEFAULT_USER_PREF_PATH,
                                DEFAULT_USER_PREF_FILE)
               : IOUtil.joinDir(userPath, DEFAULT_USER_PREF_FILE);
    }

    /**
     * Checks if the presently running IDV is old.
     *
     * @return true, if this present version is old.
     */
    private static boolean isIDVold() {
        IDVVersion thisVersion    = getIDVVersion();
        IDVVersion currentVersion = currentIDVVersion;
        return thisVersion.compareTo(currentVersion) > 0;
    }

    /**
     * Gets the version of the IDV presently running.
     *
     * @return the IDV version now running
     */
    static IDVVersion getIDVVersion() {

        Properties properties = new Properties();
        Misc.readProperties("/ucar/unidata/idv/resources/version.properties",
                            properties, IdvCommandLinePrefs.class);

        return new IDVVersion(
            properties.getProperty("idv.version.major") + "."
            + properties.getProperty("idv.version.minor")
            + properties.getProperty("idv.version.revision"));
    }

    /**
     * Gets the official current version of the IDV from Unidata.
     *
     * @return the current IDV version
     */
    private static IDVVersion getCurrentIDVVersion() {
        StringBuilder response = new StringBuilder();

        try {
            URL           website    = new URL(IDV_VERSION_URL);
            URLConnection connection = website.openConnection();
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        connection.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
        } catch (MalformedURLException e) {
            return getIDVVersion();
        } catch (IOException e) {
            return getIDVVersion();
        }

        return parseOutVersion(response.toString());
    }

    /**
     * Parses the current available version of the IDV from Unidata.
     *
     * @param response the response
     * @return the IDV version
     */
    static IDVVersion parseOutVersion(String response) {
        Pattern p = Pattern.compile(
                        "Unidata IDV API v\\d+\\.\\d+([a-z][0-9a-zA-Z])?");
        String  version = "";
        Matcher matcher = p.matcher(response);
        while (matcher.find()) {
            version = matcher.group(0);
        }
        String[] parts = version.split("\\s+");

        if (parts.length == 0) {
            return getIDVVersion();
        } else {
            String first = parts[parts.length - 1];
            if (first.length() > 0) {
                String v = first.substring(1);
                return new IDVVersion(v);
            } else {
                return getIDVVersion();
            }
        }
    }

    /**
     * The listener interface for receiving checkbox events.
     * The class that is interested in processing a checkbox
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addCheckboxListener<code> method. When
     * the checkbox event occurs, that object's appropriate
     * method is invoked.
     *
     * @see CheckboxEvent
     */
    private static final class CheckboxListener implements ItemListener {

        /** Don't show checkbox. */
        private JCheckBox cb;

        /** Show to user. */
        private boolean show = false;

        /**
         * Instantiates a new checkbox listener.
         *
         * @param cb the cb
         */
        CheckboxListener(JCheckBox cb) {
            this.cb = cb;
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            show = cb.isSelected();
        }
    }

    /**
     * The IDVVersion class.
     */
    static class IDVVersion implements Comparable<IDVVersion> {

        /** IDV version. */
        private int version;

        /** IDV version string. */
        private String versionStr;

        /**
         * Instantiates a new IDV version.
         *
         * @param major the major
         * @param minor the minor
         * @param revisionChar the revision character
         * @param revisionNum the revision number
         */
        IDVVersion(String major, String minor, String revisionChar,
                   String revisionNum) {
            idvVersionInternal(major, minor, revisionChar, revisionNum);
        }

        /**
         * Instantiates a new IDV version. Can take a string like "4.0u2" and parse it
         * into an IDVVersion object.
         *
         * @param idvversion
         *          the idv version, e.g., "4.0u2"
         */
        IDVVersion(String idvversion) {
            String[] parts        = idvversion.split("\\.");
            String   major        = parts[0];
            String[] parts2       = parts[1].split("[a-z]");
            String   minor        = parts2[0];
            String   revisionChar = null;
            String   revisionNum  = null;
            String[] rev          = idvversion.split(major + "." + minor);
            if (rev.length > 1) {
                String r = rev[1];
                revisionChar = r.substring(0, 1);
                revisionNum  = r.substring(1, r.length());
            }
            idvVersionInternal(major, minor, revisionChar, revisionNum);
        }

        /**
         * Helper method for the constructor
         *
         * @param major the major
         * @param minor the minor
         * @param revisionChar the revision character
         * @param revisionNum the revision number
         */
        private void idvVersionInternal(String major, String minor,
                                        String revisionChar,
                                        String revisionNum) {
            int    mjr = Integer.parseInt(major.trim());
            int    mnr = Integer.parseInt(minor.trim());
            String revStr;
            if ((revisionChar == null) || (revisionNum == null)) {
                revStr = "";
            } else {
                revStr = (revisionChar + revisionNum).trim();
            }
            int revChar = digitizeRevisionChar((revisionChar == null)
                    ? ""
                    : revisionChar.trim());
            int revNum  = digitizeRevisionNum((revisionNum == null)
                    ? ""
                    : revisionNum.trim());

            // Represent the IDV version as a int. 
            // The revision number takes the first eight bits
            // The revision character takes the second eight bits
            // The minor version number takes the third eight bits
            // The major version number takes the fourth eight bits
            this.version = revNum + (revChar << (8 * 1)) + (mnr << (8 * 2))
                           + (mjr << (8 * 3));
            this.versionStr = mjr + "." + mnr + revStr;
        }


        /**
         * {@inheritDoc}
         *
         */
        @Override
        public int compareTo(IDVVersion o) {
            int v1 = this.version;
            int v2 = o.version;
            if (v2 == v1) {
                return 0;
            } else if (v1 > v2) {
                return -1;
            } else if (this.equals(o)) {
                return 0;
            } else {
                return 1;
            }
        }

        /**
         * Digitize revision character. In short, ? > u > b > empty
         *
         * @param c the c
         * @return the revision character as a number
         */
        private static int digitizeRevisionChar(String c) {
            if (c.equals("")) {
                return 0;
            } else if (c.equals("b")) {
                return 1;
            } else if (c.equals("u")) {
                return 2;
            } else {
                return 3;
            }
        }

        /**
         * Assign a number to a revision number. They are not always numbers (e.g. 2.7uX)
         *
         * @param n the "number"
         * @return the revision number
         */
        private static int digitizeRevisionNum(String n) {
            try {
                return Integer.parseInt(n);
            } catch (NumberFormatException e) {
                //Could not find a number so default to something big.
                // 2.7uX comes after 2.7u2
                return 255;  //Highest number possible for first eight bits, see idvVersionInternal method
            }
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public String toString() {
            return versionStr;
        }

        /**
         * Normalized string for shell.
         *
         * @return the string
         */
        String stringForShell() {
            return toString().replaceAll("\\.|\\s+", "_");
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public int hashCode() {
            final int prime  = 31;
            int       result = 1;
            result = prime * result + version;
            return result;
        }

        /**
         * {@inheritDoc}
         *
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            IDVVersion other = (IDVVersion) obj;
            if (version != other.version) {
                return false;
            }
            return true;
        }
    }
}
