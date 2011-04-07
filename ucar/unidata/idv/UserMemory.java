package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import ucar.unidata.xml.XmlEncoder;

/**
 * Stand alone application to output user settings related to the JVM heap size
 * in megabytes.
 * 
 */
public class UserMemory {

	/**
	 * Default memory setting if not set by user.
	 */
	public static final int DEFAULT_MEMORY = 512;

	/**
	 * Getting the file separator for this platform.
	 */
	private static final String FS = System.getProperty("file.separator");

	/**
	 * Where the user prefs are located. TODO: There is probably a more official
	 * way to do this.
	 */
	private static final String IDV_USER_PREFS = System
			.getProperty("user.home")
			+ FS
			+ ".unidata"
			+ FS
			+ "idv"
			+ FS
			+ "DefaultIdv" + FS + "main.xml";

	/**
	 * Main app to get user memory settings in megabytes.
	 * 
	 * @param args
	 */
	public static void main(String... args) {
		String returnVal = DEFAULT_MEMORY + "";

		if (SystemMemory.isMemoryAvailable()) {
			try {
				final String xmlPrefs = FileUtils.readFileToString(new File(
						IDV_USER_PREFS));
				@SuppressWarnings("unchecked")
				final Map<Object, Object> userPrefMap = (Map<Object, Object>) new XmlEncoder()
						.toObject(xmlPrefs);
								
				final Object userMemoryPref = userPrefMap
						.get(IdvConstants.PREF_MEMORY);
				returnVal = (userMemoryPref == null) ? DEFAULT_MEMORY + ""
						: userMemoryPref.toString();
			} catch (Exception ignore) {
			}
		}
		System.out.println(returnVal);
	}
}
