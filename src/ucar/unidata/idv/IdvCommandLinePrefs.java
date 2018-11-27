/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * Help supply command line arguments to the IDV via bash or the
 * Windows Batch Scripting environment. Ultimately, it dumps all the user
 * preferences defined in the user path main.xml into a format that can be read
 * by bash or the Windows Batch Scripting environment. Moreover, if some
 * variables are not present, it provides some sensible defaults.
 *
 */
public class IdvCommandLinePrefs {

    /** Default user pref file. */
    private static final String DEFAULT_USER_PREF_FILE = "main.xml";

    /** Default user pref path. */
    private static final String DEFAULT_USER_PREF_PATH =
        IOUtil.joinDirs(System.getProperty("user.home"), ".unidata", "idv",
                        "DefaultIdv");

    /** The IDV preference. */
    private static final String IDV_MEMORY = "idv.memory";

    /**
     * Main app to get user memory settings in megabytes.
     *
     * @param args
     *          the arguments
     */
    @SuppressWarnings("unchecked")
    public static void main(String... args) {
        final StringBuilder sb       = new StringBuilder();
        final File          prefFile = new File(getPreferences(args));

        try {
            final Map<Object, Object> userPrefMap = getPrefMap(args);

            for (Map.Entry<Object, Object> e : userPrefMap.entrySet()) {

                // Need to replace . with _ to make bash happy
                String s = e.getKey().toString().replace(".", "_") + "=\""
                           + e.getValue() + "\""
                           + System.getProperty("line.separator");

                sb.append(s);
            }

        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        System.out.println(sb.toString());
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
}
