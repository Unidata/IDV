package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps supply command line arguments to the IDV via bash.
 * Ultimately, it dumps all the user preferences defined in the user path
 * main.xml into a format that can be read by bash. Moreover, if some variables
 * are not present, it provides some sensible defaults.
 */
public class IdvCommandLinePrefs {

    /** Default user pref file. */
    private static final String DEFAULT_USER_PREF_FILE = "main.xml";

    /** Default user pref path. */
    private static final String DEFAULT_USER_PREF_PATH = IOUtil.joinDirs(System.getProperty("user.home"), ".unidata",
                                                             "idv", "DefaultIdv");

    /** The Constant defaultsMap. */
    private static final Map<Object, Object> defaultsMap = new HashMap<Object, Object>();

    static {
        defaultsMap.put("idv.memory", SystemMemory.isMemoryAvailable()
                                      ? SystemMemory.getMaxMemoryInMegabytes()
                                      : SystemMemory.DEFAULT_MEMORY);

        // There will be more of these soon.
    }

    /**
     * Main app to get user memory settings in megabytes.
     *
     * @param args
     *            the arguments
     */
    public static void main(String... args) {
        final StringBuilder sb = new StringBuilder();

        try {
            final Map<Object, Object> userPrefMap = getPrefMap(args);

            for (Map.Entry<Object, Object> e : userPrefMap.entrySet()) {

                // Need to replace . with _ to make bash happy
                String s = e.getKey().toString().replace(".", "_") + "=\"" + e.getValue() + "\""
                           + System.getProperty("line.separator");

                sb.append(s);
            }
        } catch (Exception ignore) {}

        System.out.println(sb.toString());
    }

    /**
     * Gets the pref map.
     *
     * @param args
     *            the args
     * @return the pref map
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> getPrefMap(String... args) throws IOException, Exception {
        final Map<Object, Object> userPrefMap = new HashMap<Object, Object>();
        final File                f           = new File(getPreferences(args));

        if (f.exists()) {
            userPrefMap.putAll((Map<Object,
                                    Object>) (new XmlEncoder().createObject(XmlUtil.getRoot(f.getPath(),
                                        XmlUtil.class))));
        }

        // Now adding defaults, if needed.
        for (Map.Entry<Object, Object> e : defaultsMap.entrySet()) {
            if (!userPrefMap.containsKey(e.getKey())) {
                userPrefMap.put(e.getKey(), e.getValue());
            }
        }

        return userPrefMap;
    }

    /**
     * Gets the preferences.
     *
     * @param args
     *            the args
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
               ? IOUtil.joinDir(DEFAULT_USER_PREF_PATH, DEFAULT_USER_PREF_FILE)
               : IOUtil.joinDir(userPath, DEFAULT_USER_PREF_FILE);
    }
}
