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
    public static final long MINIMUM_MEMORY = 512;

    /** Max heap for a 32 bit OS. */
    private static final long OS_32_MAX = 1536;

    /** The INSTANCE. */
    private static final SystemMemoryManager INSTANCE = new SystemMemoryManager();

    /** Is the OS 32 bit. */
    private final boolean is32;

    /** The total available system memory in bytes. */
    private final long memory;

    /** Windows operating system? */
    private final boolean windows;

    /**
     * Private constructor.
     *
     * See this discussion:
     * http://stackoverflow.com/questions/1190837/java-xmx-max-memory-on-system
     *
     * Post condition: memory will either be -1, or the result of
     * getTotalPhysicalMemorySize
     */
    private SystemMemoryManager() {
        long mem = -1;

        try {
            final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            final Method                m      = osBean.getClass().getMethod("getTotalPhysicalMemorySize");

            m.setAccessible(true);
            mem = (Long) m.invoke(osBean);
        } catch (Exception ignore) {}

        this.memory  = (mem > -1)
                       ? Math.round(mem / 1024d / 1024d)
                       : mem;
        this.is32    = System.getProperty("os.arch").indexOf("64") < 0;
        this.windows = System.getProperty("os.name").contains("Windows");
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
     * Probably should call {@link #isMemoryAvailable} first. Must return at
     * least 512.
     *
     * @return Return the total memory in megabytes, if available.
     *
     */
    public static long getTotalMemory() {
        final long returnVal;

        if (!isMemoryAvailable()) {
            returnVal = INSTANCE.memory;
        } else {
            returnVal = INSTANCE.is32
                        ? Math.min(INSTANCE.memory - MINIMUM_MEMORY, OS_32_MAX)
                        : INSTANCE.memory - MINIMUM_MEMORY;
        }

        // If memory is available, must return at least MINIMUM_MEMORY.
        return isMemoryAvailable()
               ? Math.max(returnVal, MINIMUM_MEMORY)
               : INSTANCE.memory;
    }

	/**
	 * The default when the user first starts up should be to use 80% between
	 * the "low and high-water mark", but they should be allowed to increase
	 * that to 100% of the high-water mark. There are a couple of exceptions to
	 * the 80% heuristic. The amount of memory returned will never be < 512GB.
	 * Also, if on 32 bit OS be conservative and choose 70%. For example, if we
	 * have 32 bit windows with 1536 of memory, the result will be 1229.
	 * 
	 * @return the default memory
	 */
    public static long getDefaultMemory() {
        final double percent = INSTANCE.is32 ? 0.7 : 0.8;

        final long memory = Math.round(((getTotalMemory() - MINIMUM_MEMORY) * percent) + MINIMUM_MEMORY);

        return isMemoryAvailable()
               ? Math.max(memory, MINIMUM_MEMORY)
               : MINIMUM_MEMORY;
    }

    /**
     * Convenience method. Convert memory to percent.
     *
     * @param memory
     *            the memory
     * @return the percent or -1
     */
    public static float convertToPercent(final long memory) {
        float val;

        if (isMemoryAvailable()) {
            if (getTotalMemory() == MINIMUM_MEMORY) {
                val = memory * 100f / MINIMUM_MEMORY;
            } else {
                val = (memory - MINIMUM_MEMORY) * 100f / (getTotalMemory() - MINIMUM_MEMORY);
            }
        } else {
            val = -1;
        }

        return val;
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
               ? MINIMUM_MEMORY + Math.round((percent / 100f) * (getTotalMemory() - MINIMUM_MEMORY))
               : -1;
    }

    /**
     * Check and repair memory settings
     *
     * @param memory
     *            the memory setting
     *
     * @return the memory (fixed if necessary).
     */
    public static long checkAndRepair(final long memory) {
        long val = memory;

        if (memory < MINIMUM_MEMORY) {
            val = MINIMUM_MEMORY;
        }

        if (isMemoryAvailable() && (memory > getTotalMemory())) {
            val = getTotalMemory();
        }

        return val;
    }
}
