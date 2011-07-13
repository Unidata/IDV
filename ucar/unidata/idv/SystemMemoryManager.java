package ucar.unidata.idv;

//~--- JDK imports ------------------------------------------------------------

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * Global to deal with IDV command line memory settings.
 *
 */
public class SystemMemoryManager {

    /** Minimum memory for the IDV. */
    private static final long MINIMUM_MEMORY = 512;

    /** Max heap for a 32 bit OS. */
    private static final long OS_32_MAX = 1536;

    /** The INSTANCE. */
    private static final SystemMemoryManager INSTANCE = new SystemMemoryManager();

    /** The total available system memory in bytes. */
    private final long memory;

    /**
     * Private constructor.
     *
     * See this discussion: http://stackoverflow.com/questions/1190837/java-xmx-max-memory-on-system
     *
     * Post condition: memory will either be -1, or the result of getTotalPhysicalMemorySize
     */
    private SystemMemoryManager() {
        long mem = -1;

        try {
            final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            final Method                m      = osBean.getClass().getMethod("getTotalPhysicalMemorySize");

            m.setAccessible(true);
            mem = (Long) m.invoke(osBean);
        } catch (Exception ignore) {}

        this.memory = Math.round((mem > -1)
                                 ? mem / 1024d / 1024d
                                 : mem);
    }

    /**
     * Gets total available system memory in bytes. If none could be found
     * because the JVM does not implement this functionality, this method will
     * return -1. Probably should call {@link #isMemoryAvailable} first.
     *
     * @return the memory in bytes
     */
    private static long getMemory() {
        return INSTANCE.memory;
    }

    /**
     * Checks if is memory is available for this JVM.
     *
     * @return true, if memory is available
     */
    public static boolean isMemoryAvailable() {
        return INSTANCE.memory > 0;
    }

    /**
     * For 32 bit OS, the total should be the minimum of the (total system
     * memory - 512Mb) and 1536MB. On 64-bit systems, it should be the total
     * system memory-512Mb. If the total system memory is not available -1.
     * Probably should call {@link #isMemoryAvailable} first.
     *
     * @return Return the total memory in megabytes, if available.
     *
     */
    public static long getTotalMemory() {
        final long returnVal;

        if (!isMemoryAvailable()) {
            returnVal = INSTANCE.memory;
        } else {
            final boolean is32 = System.getProperty("os.arch").indexOf("64") < 0;

            returnVal = is32
                        ? Math.min(getMemory() - MINIMUM_MEMORY, OS_32_MAX)
                        : getMemory() - MINIMUM_MEMORY;
        }

        return returnVal;
    }

    /**
     * Gets the minimum memory.
     *
     * @return the minimum memory
     */
    public static long getMinimumMemory() {
        return MINIMUM_MEMORY;
    }

	/**
	 * The default when the user first starts up should be to use 80% of the
	 * total, but they should be allowed to increase that to 100% of the total
	 * 
	 * @return the default memory
	 */
	public static long getDefaultMemory() {
		return Math.round((isMemoryAvailable() ? getTotalMemory() * 0.8
				: MINIMUM_MEMORY));
	}

    /**
     * Convenience method. Convert memory to percent.
     *
     * @param number
     *            the number
     * @return the percent or -1 
     */
    public static float convertToPercent(final long number) {
        return isMemoryAvailable()
               ? (number - getMinimumMemory()) * 100f / (getTotalMemory() - getMinimumMemory())
               : -1;
    }

    /**
     * Convenience method. Convert memory percent to number. 
     *
     * @param percent
     *            the percent
     * @return the number or -1
     */
    public static long convertToNumber(final int percent) {
        return isMemoryAvailable()
               ? getMinimumMemory() + Math.round((percent / 100f) * (getTotalMemory() - getMinimumMemory()))
               : -1;
    }

    /**
     *     Check and repair memory settings
     *
     *     @param memory the memory setting
     *
     *     @return the memory (fixed if necessary).
     */
    public static long checkAndRepair(final long memory) {
        long val = memory;

        if (memory < getMinimumMemory()) {
            val = getMinimumMemory();
        }

        if (isMemoryAvailable() && (memory > getTotalMemory())) {
            val = getTotalMemory();
        }

        return val;
    }
}
